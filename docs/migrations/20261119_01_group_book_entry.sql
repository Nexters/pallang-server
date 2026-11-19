-- =====================================================================================================
-- #223 팔랑 모임 개선 1단계: 모임 책(bookEntry) 데이터 모델 전환
-- 대상: MySQL 8 (dev / prod)
--
-- 왜 수동 SQL인가
--   ddl-auto: update는 새 테이블/컬럼 추가만 하고, NOT NULL 해제·컬럼 삭제·기존 행 이전(backfill)은 하지 않는다.
--   기존 reading_groups.book_id / start_date / end_date / invite_code가 NOT NULL로 남아 있으면 새 코드의
--   모임 생성 INSERT가 실패하고, 기존 모임에는 group_book_entries 행이 없어 상세/목록 조회가 깨진다.
--
-- 실행 순서 (반드시 이 순서)
--   1) 서버 중지 (구버전 앱이 실행 중이면 마이그레이션과 새 앱 배포 사이에 만든 모임이 이전되지 않는다)
--   2) DB 백업 (mysqldump 등)
--   3) 이 파일 실행  ※ MySQL DDL은 자동 커밋이라 트랜잭션으로 묶이지 않는다. 중간에 실패하면 실패 지점부터
--      확인 후 이어서 실행한다(INSERT ... WHERE NOT EXISTS로 재실행해도 중복 이전되지 않게 작성했다).
--   4) 맨 아래 "검증" 쿼리로 건수 확인
--   5) 새 버전 앱 배포
--   6) 안정화 확인 후 20261119_02_group_book_entry_cleanup.sql 로 옛 컬럼 정리 (선택, 되돌리기 어려움)
--
-- 되돌리기
--   옛 컬럼(book_id/start_date/end_date/invite_code)은 이 단계에서 지우지 않고 값도 그대로 둔다. 구버전 앱으로
--   롤백해야 하면 새 앱이 만든 모임만 옛 컬럼이 비어 있으므로, 롤백 전에 그 모임들을 확인해야 한다
--   (SELECT id FROM reading_groups WHERE book_id IS NULL).
-- =====================================================================================================


-- -----------------------------------------------------------------------------------------------------
-- 1. reading_groups: 상태/합치기/낙관적 락 컬럼 추가, 이름 길이 확장, 옛 컬럼 NULL 허용
-- -----------------------------------------------------------------------------------------------------
-- name: 15 grapheme 기준으로 검증하므로(이모지 ZWJ 시퀀스 등은 코드 포인트가 훨씬 많다) 컬럼은 100자로 넓힌다.
-- 옛 CHECK(capacity BETWEEN 2 AND 10 AND end_date >= start_date)는 그대로 둬도 된다 — 날짜가 NULL이면
-- 비교 결과가 NULL이라 MySQL CHECK는 위반으로 보지 않는다. 정리는 cleanup 파일에서 한다.
ALTER TABLE reading_groups
    ADD COLUMN status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN merged_into_id BIGINT      NULL,
    ADD COLUMN merge_job_id   BIGINT      NULL,
    ADD COLUMN merged_at      DATETIME(6) NULL,
    ADD COLUMN version        BIGINT      NOT NULL DEFAULT 0,
    MODIFY COLUMN name        VARCHAR(100) NOT NULL,
    MODIFY COLUMN book_id     BIGINT      NULL,
    MODIFY COLUMN start_date  DATE        NULL,
    MODIFY COLUMN end_date    DATE        NULL,
    MODIFY COLUMN invite_code VARCHAR(32) NULL;


-- -----------------------------------------------------------------------------------------------------
-- 2. group_book_entries: 모임 책 테이블 생성 + 기존 모임의 책/기간 이전
-- -----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS group_book_entries
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    group_id      BIGINT      NOT NULL,
    book_id       BIGINT      NOT NULL,
    start_date    DATE        NULL,
    end_date      DATE        NULL,
    status_mode   VARCHAR(20) NOT NULL,
    manual_status VARCHAR(20) NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_group_book_entries_group (group_id, created_at),
    KEY idx_group_book_entries_book (book_id),
    KEY idx_group_book_entries_end_date (end_date),
    CONSTRAINT fk_group_book_entries_group FOREIGN KEY (group_id) REFERENCES reading_groups (id),
    CONSTRAINT fk_group_book_entries_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT chk_group_book_entries_period CHECK (
        (start_date IS NULL AND end_date IS NULL)
            OR (start_date IS NOT NULL AND end_date IS NOT NULL AND start_date <= end_date))
);

-- 기존 모임마다 entry 1개. 등록 시각은 모임 생성 시각으로 둬서 책 목록 정렬이 자연스럽게 하고, 기존 모임은
-- 날짜 기준 자동 판정(AUTO)으로 시작한다. 과거 날짜도 그대로 옮긴다(새로 입력한 값이 아니므로 검증 대상 아님).
INSERT INTO group_book_entries (group_id, book_id, start_date, end_date, status_mode, manual_status, created_at, updated_at)
SELECT g.id, g.book_id, g.start_date, g.end_date, 'AUTO', NULL, g.created_at, g.created_at
FROM reading_groups g
WHERE g.book_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM group_book_entries e WHERE e.group_id = g.id)
ORDER BY g.id;


-- -----------------------------------------------------------------------------------------------------
-- 3. group_members: 상태/참여일/내보낸 시각 추가
-- -----------------------------------------------------------------------------------------------------
-- 지금까지는 탈퇴/내보내기 기능이 없어 모든 기존 멤버십은 ACTIVE다. 참여일은 기존 가입 시각(created_at)으로 채운다.
ALTER TABLE group_members
    ADD COLUMN status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN joined_at  DATETIME(6) NULL,
    ADD COLUMN removed_at DATETIME(6) NULL;

UPDATE group_members SET joined_at = created_at WHERE joined_at IS NULL;

ALTER TABLE group_members MODIFY COLUMN joined_at DATETIME(6) NOT NULL;


-- -----------------------------------------------------------------------------------------------------
-- 4. group_invitations: 초대 테이블 생성 + 기존 invite_code 이전
-- -----------------------------------------------------------------------------------------------------
-- issued_by는 회원 삭제와 FK로 얽히지 않도록 FK 없이 값만 둔다(엔티티도 연관관계 없음).
CREATE TABLE IF NOT EXISTS group_invitations
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    group_id   BIGINT      NOT NULL,
    code       VARCHAR(32) NOT NULL,
    issued_by  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_group_invitations_code (code),
    KEY idx_group_invitations_group_status (group_id, status),
    CONSTRAINT fk_group_invitations_group FOREIGN KEY (group_id) REFERENCES reading_groups (id)
);

-- 기존 초대 코드는 재발급 이력이 남아 있지 않아 실제 발급 시각을 알 수 없다. 마이그레이션 시각에 모임장이 발급한
-- 것으로 간주한다(아직 내보내기 기능이 없어 "내보낸 시각 이전 발급 초대" 판단에는 영향이 없다).
INSERT INTO group_invitations (group_id, code, issued_by, status, revoked_at, created_at, updated_at)
SELECT g.id, g.invite_code, g.host_id, 'ACTIVE', NULL, NOW(6), NOW(6)
FROM reading_groups g
WHERE g.invite_code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM group_invitations i WHERE i.code = g.invite_code)
ORDER BY g.id;


-- -----------------------------------------------------------------------------------------------------
-- 5. idempotency_records: 요청 키 처리 결과 저장 테이블
-- -----------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS idempotency_records
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    operation   VARCHAR(50)  NOT NULL,
    request_key VARCHAR(100) NOT NULL,
    resource_id BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_idempotency_records_user_operation_key (user_id, operation, request_key)
);


-- -----------------------------------------------------------------------------------------------------
-- 검증 (모두 기대값과 같아야 새 앱을 배포한다)
-- -----------------------------------------------------------------------------------------------------
-- (a) 모든 모임에 entry가 정확히 1개 → 0
SELECT COUNT(*) AS groups_without_single_entry
FROM reading_groups g
WHERE (SELECT COUNT(*) FROM group_book_entries e WHERE e.group_id = g.id) <> 1;

-- (b) 옮긴 책/기간이 원본과 같다 → 0
SELECT COUNT(*) AS mismatched_entries
FROM reading_groups g
         JOIN group_book_entries e ON e.group_id = g.id
WHERE e.book_id <> g.book_id
   OR NOT (e.start_date <=> g.start_date)
   OR NOT (e.end_date <=> g.end_date);

-- (c) 모든 모임에 ACTIVE 초대가 1개 → 0
SELECT COUNT(*) AS groups_without_single_active_invitation
FROM reading_groups g
WHERE (SELECT COUNT(*) FROM group_invitations i WHERE i.group_id = g.id AND i.status = 'ACTIVE') <> 1;

-- (d) 참여일이 비어 있는 멤버십 → 0
SELECT COUNT(*) AS members_without_joined_at FROM group_members WHERE joined_at IS NULL;
