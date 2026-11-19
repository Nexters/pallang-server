-- =====================================================================================================
-- #223 팔랑 모임 개선 1단계: 옛 컬럼 정리 (선택, 되돌리기 어려움)
-- 대상: MySQL 8 (dev / prod)
--
-- 20261119_01_group_book_entry.sql 실행 + 새 앱 배포 후, 구버전으로 롤백할 일이 없다고 판단되면 실행한다.
-- 실행하면 reading_groups의 옛 단일 책/기간/초대 코드 값이 사라져 구버전 앱으로 돌아갈 수 없다.
-- 실행 전 DB 백업.
-- =====================================================================================================

-- 1. 옛 CHECK 제약 이름 확인 (Hibernate가 이름 없이 만든 제약이라 MySQL이 reading_groups_chk_1 등으로 이름을 붙였다)
SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS tc
         JOIN information_schema.CHECK_CONSTRAINTS cc
              ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
WHERE tc.TABLE_SCHEMA = DATABASE()
  AND tc.TABLE_NAME = 'reading_groups'
  AND tc.CONSTRAINT_TYPE = 'CHECK';

-- 2. book_id FK 이름 확인
SELECT CONSTRAINT_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reading_groups'
  AND COLUMN_NAME = 'book_id'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 3. 위에서 확인한 이름으로 바꿔 실행한다 (<...> 부분).
--    end_date/start_date를 참조하는 CHECK가 남아 있으면 컬럼 삭제가 막히므로 먼저 지우고, 정원 CHECK만 다시 건다.
ALTER TABLE reading_groups DROP CHECK <reading_groups_check_constraint_name>;
ALTER TABLE reading_groups ADD CONSTRAINT chk_reading_groups_capacity CHECK (capacity BETWEEN 2 AND 10);

ALTER TABLE reading_groups DROP FOREIGN KEY <reading_groups_book_id_fk_name>;
ALTER TABLE reading_groups DROP INDEX uq_reading_groups_invite_code;

ALTER TABLE reading_groups
    DROP COLUMN book_id,
    DROP COLUMN start_date,
    DROP COLUMN end_date,
    DROP COLUMN invite_code;
