-- opinion_likes INSERT/DELETE 트리거로 opinions.like_count를 DB에서 직접 동기화한다 (backend-plan.md §5.4).
-- 운영 프로파일은 ddl-auto: validate로 스키마를 자동 반영하지 않으므로(application-prod.yaml 참고),
-- 마이그레이션 도구(Flyway/Liquibase) 도입 전까지는 배포 전에 이 스크립트를 운영 MySQL에 수동으로 반영해야 한다.
-- 로컬에서 spring.sql.init.mode=always, platform=mysql로 MySQL을 직접 붙여 검증할 때는 자동으로 실행된다.
DROP TRIGGER IF EXISTS trg_opinion_likes_after_insert;
DROP TRIGGER IF EXISTS trg_opinion_likes_after_delete;

CREATE TRIGGER trg_opinion_likes_after_insert
AFTER INSERT ON opinion_likes
FOR EACH ROW
UPDATE opinions SET like_count = like_count + 1 WHERE id = NEW.opinion_id;

CREATE TRIGGER trg_opinion_likes_after_delete
AFTER DELETE ON opinion_likes
FOR EACH ROW
UPDATE opinions SET like_count = like_count - 1 WHERE id = OLD.opinion_id;
