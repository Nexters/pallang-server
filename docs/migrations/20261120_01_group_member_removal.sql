-- =====================================================================================================
-- #226 팔랑 모임 개선 3단계: 멤버 내보내기
-- 대상: MySQL 8 (dev / prod)
-- 선행: 20261119_01_group_book_entry.sql (group_members.status / removed_at 컬럼이 있어야 한다)
--
-- 실행 시점: 새 버전 앱 배포 "전". 두 작업 모두 기존 데이터를 바꾸지 않고 추가만 하므로 서버를 멈추지 않아도 된다.
-- =====================================================================================================


-- -----------------------------------------------------------------------------------------------------
-- 1. notifications.type 에 GROUP_MEMBER_REMOVED 추가 (필수)
-- -----------------------------------------------------------------------------------------------------
-- notifications.type은 Hibernate가 만든 MySQL ENUM이라 ddl-auto: update가 새 값을 넣어주지 않는다. 안 하면 내보내기는
-- 성공하지만 대상에게 보내는 "모임 참여 종료" 알림 저장이 실패한다(PR #209의 CONTENT_REMOVED와 같은 상황).
--
-- (a) 현재 ENUM 값 목록 확인 — 아래 (b)는 이 목록에 GROUP_MEMBER_REMOVED만 덧붙여야 한다.
SHOW COLUMNS FROM notifications LIKE 'type';

-- (b) 2026-11-20 기준 코드의 NotificationType 전체. (a)의 결과에 이 외의 값이 있으면 지우지 말고 함께 적는다.
ALTER TABLE notifications
    MODIFY COLUMN type ENUM (
        'OPINION_LIKED',
        'OPINION_COMMENTED',
        'BOOK_NEW_OPINIONS',
        'GROUP_JOINED',
        'GROUP_MEMBER_JOINED',
        'GROUP_SCHEDULE_CHANGED',
        'GROUP_ENDING_SOON',
        'CONTENT_REMOVED',
        'GROUP_MEMBER_REMOVED'
        ) NOT NULL;


-- -----------------------------------------------------------------------------------------------------
-- 2. group_member_removals: 내보내기 감사 이력
-- -----------------------------------------------------------------------------------------------------
-- 사유 코드는 문자열로 저장한다(ENUM/CHECK 없음) — 사유가 늘어도 ALTER가 필요 없다. 회원/모임 삭제와 FK로 얽히지
-- 않도록 FK 없이 id 값만 둔다. ddl-auto가 만들어도 되지만, ENUM/CHECK 없이 만들기 위해 직접 만든다.
CREATE TABLE IF NOT EXISTS group_member_removals
(
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    group_id       BIGINT        NOT NULL,
    actor_user_id  BIGINT        NOT NULL,
    target_user_id BIGINT        NOT NULL,
    reason_code    VARCHAR(40)   NOT NULL,
    other_text     VARCHAR(2000) NULL,
    request_key    VARCHAR(100)  NULL,
    created_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_group_member_removals_group (group_id),
    KEY idx_group_member_removals_target (target_user_id)
);


-- -----------------------------------------------------------------------------------------------------
-- 검증
-- -----------------------------------------------------------------------------------------------------
-- GROUP_MEMBER_REMOVED가 목록에 있어야 한다
SHOW COLUMNS FROM notifications LIKE 'type';
-- 테이블이 있어야 한다
SHOW CREATE TABLE group_member_removals;
