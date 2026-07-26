-- opinion_likes INSERT/DELETE 트리거로 opinions.like_count를 DB에서 직접 동기화한다 (backend-plan.md §5.4).
-- H2는 트리거 본문에 순수 SQL을 쓸 수 없고 org.h2.api.Trigger를 구현한 컴파일된 클래스만 등록할 수 있어,
-- OpinionLikeCountH2Trigger가 schema-mysql.sql과 동일한 증감 로직을 재현한다.
-- 로컬(spring.profiles.active=local)과 테스트는 application-local.yaml의 spring.sql.init 설정으로
-- Hibernate가 테이블을 만든 뒤에 이 스크립트가 실행된다.
DROP TRIGGER IF EXISTS trg_opinion_likes_sync;

CREATE TRIGGER trg_opinion_likes_sync
    AFTER INSERT, DELETE ON opinion_likes
    FOR EACH ROW
    CALL "com.nexters.palang.domain.opinion.infrastructure.OpinionLikeCountH2Trigger";
