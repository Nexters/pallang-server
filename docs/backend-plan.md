# 여백이들과 교환독서 — 백엔드 구현 계획

## Context

`docs/requirements.md`(Figma 전수 분석 + 기획 노트 기반 v2 요구사항 명세)를 바탕으로 백엔드 코드 계획을 구체화한다. 이번 개정(v2)에서는 다음 두 가지를 추가로 반영했다:

- **`docs/conventions.md`** — 팀의 Java 코드 컨벤션(레이어별 메서드 명명, 디렉토리 구조, 에러 코드 포맷, DTO 필드명 규칙, 테스트 스타일)을 그대로 채택.
- **PR #6 (`Feat/#3 1차 MVP 주요 Entity 클래스 작성`)** — 이미 병합 대기 중인 엔티티 초안을 베이스라인으로 삼고, 리뷰에서 나온 개선점을 반영.

**우선순위 변경**: PR #6을 머지하고 바로 작업을 시작하면서, **인증(카카오 로그인/JWT)과 OCR은 우선순위를 낮춰 후순위로 미룬다.** 도서/대목/흔적/꾸밈/댓글/좋아요 등 핵심 도메인 로직과 API부터 만들고, 인증·OCR은 별도 Phase로 뒤로 뺀다 (§6 Phase 표 참고). 대신 "인증 필요"로 표시된 엔드포인트들을 지금 당장 개발·테스트할 수 있도록 임시 인증 스탠드인 전략을 둔다 (§4.3).

인증/OCR/읽기상태 관련 세부 설계(이전 논의 결과)는 후순위로 미뤄진 것과 별개로 그대로 유지한다 — 나중에 착수할 때 바로 쓸 수 있도록 설계만 먼저 남겨둔다:

- **인증 방식**: JWT (Access + Refresh)
- **소셜 로그인 범위**: 카카오만 우선 구현 (Provider 확장 가능한 구조로)
- **OCR 처리 위치**: 서버 프록시, 메모리 처리 (디스크 미기록, 로그 제외)
- **읽기상태 미설정 시 정책**: PLANNED와 동일 취급 (첫 페이지만 노출)
- 유사 문장 판정: **정규화 후 해시 일치**, 꾸밈 노출 3순위 랜덤: **Decoration.id 기반 결정적 셔플**, 도서 검색: **알라딘 OpenAPI**

**설계 스타일**: 도메인 로직 배치는 **서비스 계층 중심(Anemic Domain Model)** 을 유지한다 — 엔티티는 데이터 홀더, 규칙은 각 도메인의 `application` 계층(Service)에 둔다. 단, 다음 두 경우는 예외로 엔티티에 둔다:
- **단순 상태 전이** (`completeOnboarding()`, `withdraw()` 등, PR #6 패턴 유지)
- **생성 시점 불변식** — 그 엔티티 하나만 보고 판단 가능하고, 다른 엔티티/트랜잭션 조율이 필요 없는 규칙(예: 댓글 1-depth 제약)은 정적 팩토리 메서드로 "애초에 잘못된 객체를 만들 수 없게" 막는다 (§5.3).

반대로 **여러 엔티티에 걸친 조율이 필요한 규칙**(예: 좋아요 생성/삭제에 맞춰 Opinion의 캐시된 좋아요 수를 갱신하는 것)은 단일 엔티티의 생성자/팩토리로 표현할 수 없으므로 서비스 계층에 남긴다 (§5.4).

목표는 (인증/OCR을 제외한) 핵심 도메인부터 실제 코드를 짤 수 있는 수준의 패키지 구조, 엔티티 설계, API 명세, 핵심 알고리즘 설계, 그리고 팀 컨벤션에 맞는 명명/에러코드/테스트 규칙까지 확정하는 것이다.

---

## 1. build.gradle 추가 의존성

**지금 바로 추가 (핵심 도메인 작업에 필요)**
```gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-webclient' // 알라딘 외부 API 호출용 WebClient (Spring Boot 4는 WebClient 자동설정이 starter-webflux가 아닌 별도 모듈로 분리됨)
implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0' // API 문서, 프론트 협업용 — 컨벤션.md의 XxxApi 인터페이스 + @Operation 패턴 전제
```

**인증 Phase 착수 시 추가 (지금은 제외)**
```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

- **Redis는 도입하지 않는다.** Refresh Token은 DB 테이블(`RefreshToken`)로 관리 (인증 Phase에서).
- **QueryDSL**은 흔적 목록(정렬 2종 + 스포일러/읽기상태 필터 조합), 꾸밈 병합 조회처럼 동적 조건 쿼리가 많아 지금 단계부터 채택.
- `spring-boot-starter-security`를 아직 넣지 않으므로, 지금 단계에서는 모든 엔드포인트가 기본적으로 열려 있다. §4.3의 임시 인증 스탠드인으로 "로그인한 사용자"를 흉내 낸다.

---

## 2. 패키지 구조 — `컨벤션.md` + PR #6 실제 구조 기준

PR #6이 이미 `presentation / application / domain / infrastructure / common` 5분할 구조를 도메인별로 만들어뒀고, 이는 `컨벤션.md`의 "디렉토리 구조(도메인형)" 후보 1과 일치한다. 이 구조를 그대로 확정한다 (기존 계획의 `service/controller/repository/dto` 3분류안은 폐기).

```
com.nexters.palang
├─ global
│   ├─ config       (SecurityConfig, JpaConfig, QuerydslConfig, WebClientConfig, SwaggerConfig)
│   ├─ security
│   │   └─ jwt      (JwtTokenProvider, JwtAuthenticationFilter, SecurityUserPrincipal)
│   ├─ exception    (GlobalExceptionHandler, BaseErrorCode, CommonErrorCode)
│   ├─ response     (ApiResponse<T>, Swagger 커스텀 어노테이션: @ApiResponseExplanations 등)
│   ├─ util
│   └─ common       (BaseEntity — PR #6에서 이미 사용 중)
└─ domain
    ├─ auth         (application, domain, infrastructure, presentation, common)
    ├─ user
    ├─ book
    ├─ passage
    ├─ opinion      (Opinion, OpinionLike)
    ├─ decoration
    ├─ comment
    ├─ ocr
    └─ notice       (Phase 4, 낮은 우선순위)
```

각 도메인 내부 규칙 (컨벤션.md 기준):
- `presentation`: Controller + `XxxApi` 인터페이스(Swagger `@Operation` 명세) + request/response DTO
- `application`: Service(비즈니스 로직) + DTO ↔ Entity mapper
- `domain`: Entity, VO, 도메인 전용 상수(enum)
- `infrastructure`: JPA Repository, 외부 API 클라이언트(카카오/알라딘/OCR)
- `common`: 해당 도메인의 커스텀 예외 + `DomainErrorCode` enum

---

## 3. 엔티티 설계 — PR #6 베이스라인 + 보완 필요 항목

PR #6에서 이미 확정된 좋은 패턴은 그대로 따른다: `Like` 대신 `OpinionLike`(댓글엔 좋아요 없음을 스키마로 표현), `Book.source(API/MANUAL)`, `@Check` DB 제약(페이지 수, offset 범위), `BaseEntity + @EnableJpaAuditing`.

| 엔티티 | PR #6 현재 상태 | 보완 필요 |
|---|---|---|
| `User` | nickname(15,unique), profileImageUrl, backgroundColor, snsProvider(KAKAO/APPLE), snsId, termsAgreedAt(NOT NULL), hasCompletedOnboarding, isWithdrawn, withdrawnAt | **`nicknameUpdatedAt` 추가** (1일 1회 변경 제한 검증용) / **`withdraw()`에 닉네임 익명화 포함** (`"탈퇴한 사용자" + id`로 치환해 unique 제약 해제) / `termsAgreedAt`을 nullable로 바꿀지, 가입 시점에 즉시 값이 채워지는 플로우로 갈지 인증 담당자와 확정 |
| `RefreshToken` | 없음 | 신규 필요 (JWT 재발급용: user_id, tokenHash, expiresAt, revoked) |
| `UserBookStatus` | user, book, status(READING/PLANNED), currentPage, unique(user,book) | 변경 없음 (그대로 사용) |
| `Book` | title, author, publisher, pageCount(`@Check`>0), isbn(nullable), coverImageUrl(nullable), source(API/MANUAL) | 변경 없음 |
| `Passage` | book, creator(User), pageNumber, quotedText(150), isSpoiler | **`normalizedHash` 추가** + `(book_id, normalized_hash)` 인덱스 (유사 문장 판정, FR-WRITE-07) / 150자 제한은 `PassagePolicy.QUOTED_TEXT_MAX_LENGTH` 상수 참조 (§3.1) |
| `Opinion` | passage, user, content(500), likeCount(int) | **`deletedAt` 추가** (수정/삭제) / **`likeCount` 동기화는 `opinion_likes` INSERT/DELETE DB 트리거로 처리** (이유는 §5.4 참고) / `(passage_id, created_at)` 인덱스 추가 (최신순 정렬 전용, 기존 `idx_opinions_passage_sort`는 좋아요순 전용) / `@OneToMany(mappedBy="opinion", cascade=ALL, orphanRemoval=true) decorations` 추가 (§5.5) |
| `Decoration` | opinion, startOffset, endOffset(`@Check`), effectType(**String**), color(**"#PRIMARY" 문자열**) | **`effectType` → `enum EffectType{UNDERLINE,WAVY,HIGHLIGHT}`** / **`color`를 저장 시점에 실제 hex로 resolve**하거나 `ColorToken` enum 도입 / 독립 테이블·엔티티는 유지하되 `Opinion` 쪽 연관관계에 `cascade=ALL, orphanRemoval=true`로 "Opinion 없이 존재 불가"를 표현 (§5.5) |
| `Comment` | opinion, user, parentComment(self FK), content(1000) | **`deletedAt` 추가** (수정/삭제) / **1-depth 제약은 정적 팩토리(`Comment.root()`/`Comment.reply()`)에서 생성 시점에 차단** (DB 트리거 방식 채택 안 함 — §5.3) |
| `OpinionLike` | user, opinion, unique(user,opinion) | 변경 없음 |
| `Notice` | 없음 | 신규, Phase 4 |
| `Report` | 없음 | MVP 범위 밖, 백로그 |

**인덱스 최종 목록**
- `idx_passages_book_page (book_id, page_number)`
- `idx_passages_book_hash (book_id, normalized_hash)` — 신규
- `idx_opinions_passage_sort (passage_id, like_count, created_at)` — 좋아요순 (PR #6 기존)
- `idx_opinions_passage_created (passage_id, created_at)` — 신규, 최신순 default
- `idx_likes_user_created (user_id, created_at)` — PR #6 기존

**SnsProvider 관련 주의**: PR #6은 `SnsProvider{KAKAO,APPLE}`를 DB `ENUM('kakao','apple')`과 대소문자 다르게 매핑하고 콜레이션의 대소문자 무시에 의존한다. DB `ENUM` 정의를 대문자로 맞추거나 `VARCHAR + @Enumerated(STRING)`으로 바꾸는 걸 다음 PR에서 논의.

### 3.1 도메인 상수 (매직넘버 제거)

VO로 감싸지 않고 컴파일 타임 상수로만 뺀다 (Anemic 방향과 충돌 없음). `@Column(length=)`, `@Size(max=)` 등 애노테이션 속성에 그대로 쓸 수 있도록 `static final` 상수로 선언하고, 엔티티·요청 DTO 양쪽에서 같은 상수를 참조한다. 위치는 컨벤션.md 디렉토리 구조의 `domain/constant`에 대응해 `domain/{feature}/domain/{Feature}Policy.java`로 둔다.

| 상수 클래스 | 상수명 | 값 | 사용처 |
|---|---|---|---|
| `PassagePolicy` | `QUOTED_TEXT_MAX_LENGTH` | 150 | `Passage.quotedText` 컬럼 길이 + 발췌 요청 DTO 검증 |
| `OpinionPolicy` | `CONTENT_MAX_LENGTH` | 500 | `Opinion.content` + 흔적 작성 요청 DTO 검증 |
| `CommentPolicy` | `CONTENT_MAX_LENGTH` | 500 (DB 컬럼은 1000으로 여유를 둠) | 댓글 작성 요청 DTO 검증 |
| `UserPolicy` | `NICKNAME_MAX_LENGTH` | 15 | `User.nickname` 컬럼 길이 + 닉네임 변경 요청 DTO 검증 |

```java
public final class PassagePolicy {
    public static final int QUOTED_TEXT_MAX_LENGTH = 150;
    private PassagePolicy() {}
}
```

---

## 4. 인증/인가 아키텍처

> ⏸ **후순위 (Phase 3, §6 참고).** 4.1~4.2는 나중에 착수할 때 바로 쓰도록 설계만 먼저 남겨둔 것이고, 지금 당장 구현하지 않는다. 그 사이 "인증 필요" 엔드포인트를 개발/테스트하는 방법은 §4.3.

### 4.1 로그인 플로우 (카카오)
모바일 앱이 카카오 SDK로 로그인 후 **카카오 액세스 토큰**을 백엔드로 전달하는 방식 (`domain/auth/infrastructure/KakaoOAuthClient`가 WebClient로 카카오 유저 정보 API 직접 검증 호출).

```
POST /api/auth/kakao   { kakaoAccessToken }
 → KakaoOAuthClient.getUserInfo(token) 로 카카오 사용자 식별자 확보
 → snsProvider+snsId로 User 조회, 없으면 신규 생성
     → NicknameGenerator: 형용사×명사 랜덤 조합 → save 시도
       → DataIntegrityViolationException 시 숫자 접미사(1~100) 붙여 재시도
 → JWT AccessToken(짧은 만료) + RefreshToken(DB 저장, 긴 만료) 발급
 → 응답: { accessToken, refreshToken, isNewUser, termsAgreed, hasCompletedOnboarding }
```

- `POST /api/auth/terms`, `POST /api/auth/refresh`, `POST /api/auth/logout` 함께 구현.
- Swagger 문서화는 컨벤션.md 패턴대로 `AuthApi` 인터페이스에 `@Operation` + `@ApiResponseExplanations`를 선언하고, `AuthController`가 구현.

### 4.2 "지연 로그인" 경계 — Soft Authentication
FR-AUTH-01의 4개 트리거(2페이지 이상 열람, 흔적 작성, 댓글 작성, 마이페이지)는 **같은 엔드포인트 안에서 페이지 순번에 따라** 갈리는 경우가 있다. 따라서:

- `JwtAuthenticationFilter`는 토큰이 없어도 요청을 막지 않고 통과시키되(**OptionalAuthentication**), 있으면 `SecurityContext`에 인증 정보를 채운다.
- 세밀한 차단은 서비스 레이어에서: 비로그인 사용자가 2페이지 이상/댓글 작성/좋아요를 시도하면 `LoginRequiredException` → **401** (컨벤션.md 기준 "인증되지 않은 접근"에 해당하는 정확한 상태 코드), `AuthErrorCode.LOGIN_REQUIRED("AUTH_401_x", "로그인이 필요합니다.")` 형태로 응답.
- `permitAll` 대상: 홈 캐러셀, 책 상세, 책 검색(내부/외부), 대목/흔적 목록 GET(응답 내용은 로그인 여부로 서비스 레이어가 제한), 댓글 목록 GET.
- 인증 필수: 흔적 작성/수정/삭제, 댓글 작성/수정/삭제, 좋아요, 마이페이지 전체, 읽기상태 설정.

### 4.3 임시 인증 스탠드인 — 인증 Phase 전까지 개발을 막지 않기 위한 장치
실제 JWT가 없어도 "인증 필요" 엔드포인트(흔적 작성, 좋아요, 댓글, 마이페이지 등)를 지금 바로 개발·테스트할 수 있어야 한다. 그래서 컨트롤러가 "현재 유저"를 얻는 지점을 처음부터 인터페이스로 추상화해둔다:

```java
// global/security/CurrentUserProvider.java
public interface CurrentUserProvider {
    Long getCurrentUserId(); // 인증 안 되어 있으면 LoginRequiredException
}
```

- **지금 (인증 Phase 이전)**: `@Profile("local")`인 `HeaderCurrentUserProvider`가 `X-Debug-User-Id` 요청 헤더 값을 그대로 유저 ID로 사용한다. 헤더가 없으면 `LoginRequiredException`을 던져서 "비로그인 시 401" 케이스도 지금부터 동일하게 검증 가능.
- **나중 (인증 Phase)**: `JwtCurrentUserProvider`가 `SecurityContext`에서 유저 ID를 꺼내는 걸로 구현체만 교체한다. **컨트롤러/서비스 코드는 한 줄도 안 바뀐다** — `CurrentUserProvider`에만 의존하기 때문.
- 테스트 코드도 이 인터페이스를 목(mock)으로 대체하면 되므로, JWT가 없다는 이유로 `@WebMvcTest`가 막히지 않는다.

---

## 5. 핵심 도메인 로직

### 5.1 닉네임 자동 생성 (FR-AUTH-04)
`NicknameGenerator` (domain/user/application): 형용사 30 × 명사 30 랜덤 조합 → `User` 저장 시도 → unique 제약 위반 시 1~100 숫자 접미사 붙여 재시도(낙관적 방식). 단어 목록은 별도 상수 클래스로 분리.

### 5.2 유사 문장 판정 (FR-WRITE-07)
```
Passage.normalizedHash = SHA-256( quotedText에서 공백·구두점 제거 후 정규화 )
저장 전: SimilarPassageFinder.find(bookId, pageNumber, normalizedHash)
  → WHERE book_id = ? AND page_number BETWEEN page-1 AND page+1 AND normalized_hash = ?
```

### 5.3 댓글 1-depth 제약 — 정적 팩토리로 생성 시점에 차단
PR #6은 `Comment.parentComment`에 "대댓글에는 답글 불가(DB 트리거로 강제)"라는 주석을 남겼다. 이 규칙은 **댓글 하나만 보고 판단 가능한 생성 시점 불변식**이라 서비스 레이어나 DB 트리거까지 갈 필요 없이, 정적 팩토리로 "애초에 잘못된 객체를 만들 수 없게" 막는다:

```java
public class Comment extends BaseEntity {
    ...
    public static Comment root(Opinion opinion, User user, String content) {
        return new Comment(opinion, user, null, content);
    }

    public static Comment reply(Comment parent, User user, String content) {
        if (parent.isReply()) {
            throw new NestedReplyNotAllowedException();
        }
        return new Comment(parent.opinion, user, parent, content);
    }

    private boolean isReply() {
        return parentComment != null;
    }
}
```

`NestedReplyNotAllowedException`은 `domain/comment/common`에 두고 `COMMENT_400_1`(§8)과 매핑한다. DB 트리거는 최후 방어선으로 남겨둘 수는 있지만, 1차 방어는 이 팩토리 메서드가 맡는다 — 트리거 없이도 H2/MySQL 어디서든 동일하게 JUnit으로 검증 가능하다는 게 핵심 이점.

### 5.4 좋아요 카운트 동기화 — DB 트리거로 처리
`Opinion.likeCount`는 `OpinionLike`라는 **다른 엔티티의 생성/삭제**에 맞춰 갱신되어야 하는 값이라, 댓글 1-depth와 달리 단일 엔티티의 정적 팩토리로는 표현할 수 없다. PR #6 코드가 이미 `opinion_likes` INSERT/DELETE 트리거를 전제로 작성돼 있었고, 이번 구현(#16)에서 DB 트리거 방식으로 확정했다.

- `OpinionLikeService.toggleLike()`가 `OpinionLike`를 생성/삭제만 하고, `opinions.like_count` 증감은 DB 트리거가 담당한다.
  - MySQL(운영): `src/main/resources/schema-mysql.sql` — `AFTER INSERT`/`AFTER DELETE` 트리거 2개, 마이그레이션 도구 도입 전까지 배포 전 수동 반영 필요 (`ddl-auto: validate`).
  - H2(로컬/테스트): `src/main/resources/schema-h2.sql` + `OpinionLikeCountH2Trigger`(`org.h2.api.Trigger` 구현체, 컴파일된 클래스만 등록 가능해 인라인 SQL 트리거 본문은 쓸 수 없음). `application.yaml`의 `local` 프로파일 문서(`spring.sql.init.mode=always`, `platform=h2`) + `spring.jpa.defer-datasource-initialization=true`로 Hibernate가 테이블을 만든 뒤 자동 등록된다. (`application-local.yaml`은 개인 설정용으로 `.gitignore` 대상이라 여기 두지 않는다.)
- **stale read 대응**: 트리거는 별도 UPDATE로 DB 값을 바꾸지만, 같은 트랜잭션의 영속성 컨텍스트가 들고 있는 `Opinion` 엔티티는 이 변경을 모른다. `OpinionLikeService`는 `OpinionLike` 저장/삭제 후 `flush()` → `entityManager.refresh(opinion)` 순서로 다시 읽어 응답에 정확한 `likeCount`를 담는다.
- **트레이드오프**: H2/MySQL에 트리거를 각각 유지해야 해서 테스트 비용이 늘고(H2는 컴파일된 Java 클래스, MySQL은 SQL 트리거로 로직이 이중화됨), MySQL 트리거는 마이그레이션 도구가 없어 배포 전 수동 반영에 의존한다. 이 비용을 감수하고 트리거를 선택한 이유는 PR #6 베이스라인과의 일치, 그리고 동시성 상황에서 `likeCount` 증감을 애플리케이션 레이어의 낙관적 락/재시도 없이 DB가 원자적으로 보장하기 때문이다.

### 5.5 Decoration 집계 경계 — cascade + orphanRemoval
`Decoration`은 `Opinion` 없이 존재할 이유가 없는 컴포지션 관계지만, 독립 엔티티/테이블 자체는 유지한다. 이유는 §5.6의 꾸밈 병합 알고리즘이 **여러 Opinion에 걸친 Decoration을 가로질러 조회하고, `Decoration.id`를 정렬 tiebreaker로 사용**하기 때문 — `@ElementCollection`으로 바꾸면 own PK가 없어져서 이 정렬 자체가 불가능해진다.

대신 "Opinion 없이 존재 불가"라는 제약은 연관관계 옵션으로 표현한다 (Opinion을 Aggregate Root, Decoration을 그 안의 자식 엔티티로 취급):

```java
// Opinion.java
@OneToMany(mappedBy = "opinion", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Decoration> decorations = new ArrayList<>();
```

단, `Opinion`은 소프트 삭제(`deletedAt`)로 처리하기로 했으므로 실제 물리 삭제가 일어나는 경우는 드물다. `orphanRemoval`은 하드 삭제/관리자 삭제 같은 예외 상황을 위한 안전장치이고, 평상시 노출 제어는 `WHERE o.deletedAt IS NULL` 조인 필터로 충분하다.

### 5.6 꾸밈 병합 노출 (FR-VIEW-03, 최우선 난이도 항목)
`DecorationMergeSelector.select(passageId)` (domain/decoration/application):
```
1. 해당 Passage의 모든 Opinion → Decoration 조회, JOIN으로 likeCount 함께 로드
2. 정렬: likeCount DESC, opinion.createdAt DESC, decoration.id ASC (3순위 랜덤 대신 결정적 tiebreak)
3. 정렬된 리스트를 순회하며 [startOffset, endOffset) 구간이
   이미 채택된 구간과 겹치는지 검사 (interval overlap)
4. 겹치지 않으면 채택, 겹치면 skip, 3개 채택되면 종료
```
순수 함수로 작성해 단위 테스트 용이하게 분리.

### 5.7 읽기 상태 기반 노출 필터 (FR-WRITE-08)
```
READING  → passage.pageNumber <= userBookStatus.currentPage
PLANNED  → passage.pageNumber == MIN(pageNumber) (해당 책의 첫 대목만)
미설정   → PLANNED와 동일 취급
```
`isSpoiler` 조건이 AND로 추가 적용. 비로그인 사용자는 항상 PLANNED와 동일 로직. QueryDSL predicate로 구성해 재사용.

---

## 6. API 엔드포인트 (컨벤션.md 필드/파라미터 규칙 적용)

**공통 규칙**
- 페이지네이션 파라미터: `page`, `size`, `sortType` (`sort` 아님)
- 응답의 PK는 도메인명 접두 (`opinionId`, `passageId`, 단순 `id` 아님)
- 리스트 응답 필드명은 `{도메인명}s` (예: `opinions`, `comments`)
- 요청 객체는 URL에 도메인이 이미 명시되면 필드에서 도메인명 생략
- 응답은 `ApiResponse<T>` 공통 래퍼로 감싸 반환
- **`필요*`** 로 표시된 항목은 실제 로그인이 아니라 §4.3의 임시 스탠드인(`X-Debug-User-Id` 헤더)으로 지금 단계에서 개발·테스트한다.

### Phase 1 — 도서 · 흔적 핵심 도메인 (지금 착수)
| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | /api/books/search?keyword= | - | 알라딘 외부 검색 |
| GET | /api/books/internal-search?keyword= | - | 내부 DB 검색 (FR-HOME-03) |
| POST | /api/books | - | 직접 등록 (누구나 등록 가능, 인증 불필요) |
| GET | /api/home/books | - | 홈 캐러셀 (FR-HOME-01,02) |
| GET | /api/books/recent | 필요* | 내가 최근 남긴 책 목록 (FR-WRITE-01) |
| GET | /api/books/popular | - | 흔적 많은 책 순 |
| POST | /api/passages/similar-check | 필요* | 유사 문장 후보 조회 (FR-WRITE-07) |
| PUT | /api/users/me/book-status | 필요* | 읽기상태/현재페이지 설정 |
| POST | /api/opinions | 필요* | Passage(신규/병합)+Opinion+Decoration 원자적 생성 — **직접 입력만, OCR 제외** |
| GET | /api/books/{bookId}/passages?page=&size= | soft | 페이지 목록(오름차순), 스포일러/읽기상태 필터 |
| GET | /api/books/{bookId}/pages/{page}/passages | soft | 대목 전환 + 꾸밈 병합 결과 포함 |
| GET | /api/passages/{passageId}/opinions?sortType=&page=&size= | soft | 흔적 목록 (최신/좋아요순) |
| GET | /api/opinions/{opinionId} | soft | 흔적 상세 + 작성자 꾸밈(FR-OPINION-05) |
| PATCH/DELETE | /api/opinions/{opinionId} | 필요*(본인) | 수정/삭제 |
| POST | /api/opinions/{opinionId}/like | 필요* | 좋아요 토글 |
| GET | /api/opinions/{opinionId}/comments?page=&size= | - | 댓글 목록 (원댓글+답글, 5개 더보기) |
| POST | /api/opinions/{opinionId}/comments | 필요* | 댓글/답글 작성 (1-depth 검증) |
| PATCH/DELETE | /api/comments/{commentId} | 필요*(본인) | 수정/삭제 |

### Phase 2 — 마이페이지
| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | /api/users/me | 필요* | 프로필 |
| PATCH | /api/users/me/nickname | 필요* | 닉네임 변경 (1일 1회 검증, nicknameUpdatedAt) |
| PATCH | /api/users/me/background-color | 필요* | 배경색 변경 |
| DELETE | /api/users/me | 필요* | 회원탈퇴 (소프트 삭제 + 닉네임 익명화) |
| GET | /api/users/me/opinions?page=&size= | 필요* | 내가 남긴 흔적 |
| GET | /api/users/me/likes?page=&size= | 필요* | 좋아요 누른 흔적 (liked_at desc) |
| GET | /api/notices, /api/notices/{id} | - | 공지사항 |

### Phase 3 (후순위) — 인증/로그인
| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | /api/auth/kakao | - | 카카오 로그인/가입 |
| POST | /api/auth/terms | 필요 | 약관 동의 기록 |
| POST | /api/auth/refresh | - | 토큰 재발급 |
| POST | /api/auth/logout | 필요 | 로그아웃 |
| PATCH | /api/users/me/onboarding-complete | 필요 | 온보딩 완료 |

이 Phase 착수 시점에 `HeaderCurrentUserProvider`(§4.3)를 `JwtCurrentUserProvider`로 교체하고, `X-Debug-User-Id` 헤더 지원은 제거(또는 local 프로파일 전용으로만 유지)한다.

### Phase 4 (후순위) — OCR
| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | /api/ocr/extract | 필요 | 이미지→텍스트, 서버 메모리 처리, 미저장 |

OCR이 붙기 전까지 흔적 작성은 "직접 입력" 경로만 지원한다 (FR-WRITE-06). 바텀시트 UI 자체는 프론트 재량이나, 백엔드는 OCR 옵션을 노출하지 않는다.

---

## 7. 명명 규칙 (컨벤션.md 1-1 적용)

| 계층 | 조회 | 생성 | 삭제 | 수정 |
|---|---|---|---|---|
| Controller/Service | `getOpinion` | `createOpinion` | `removeOpinion` | `modifyOpinion` |
| Repository | `findByOpinion` / `countByOpinion` / `existsByOpinion` | `insertOpinion` | `deleteOpinion` | `updateOpinion` |

기능을 더 명확히 설명할 수 있는 이름이 있으면 get/create/remove 대신 그 이름을 우선한다 (예: `signInWithKakao`, `mergePassage`).

---

## 8. 에러 코드 설계 (컨벤션.md 기준)

도메인별로 `DomainErrorCode implements BaseErrorCode` enum을 `domain/{feature}/common`에 둔다. 포맷: `{DOMAIN}_{4xx}_{n}`.

| 에러 코드 | HTTP | 메시지 |
|---|---|---|
| `AUTH_401_1` | 401 | 로그인이 필요합니다. |
| `AUTH_401_2` | 401 | 토큰이 만료되었습니다. |
| `USER_400_1` | 400 | 닉네임은 하루에 한 번만 변경할 수 있습니다. |
| `USER_409_1` | 409 | 이미 사용 중인 닉네임입니다. |
| `PASSAGE_400_1` | 400 | 발췌 문장은 150자를 초과할 수 없습니다. |
| `OPINION_403_1` | 403 | 본인이 작성한 흔적만 수정/삭제할 수 있습니다. |
| `OPINION_404_1` | 404 | 해당 흔적을 찾을 수 없습니다. |
| `COMMENT_400_1` | 400 | 답글에는 답글을 남길 수 없습니다. |
| `BOOK_404_1` | 404 | 해당 도서를 찾을 수 없습니다. |

사용 가능한 HTTP 상태는 컨벤션.md 기준 **400/401/403/404/409만** — 그 외 상태 코드는 사용하지 않는다.

---

## 9. 후속 확인 필요 (백엔드 착수를 막지는 않음)

- **OCR 벤더 미정**: `OcrClient` 인터페이스로 추상화, Phase 4(OCR) 착수 시점에 벤더 선정 — 지금 당장은 필요 없음.
- **댓글 1-depth(정적 팩토리)·좋아요 카운트(DB 트리거) 방식**: 댓글 1-depth는 정적 팩토리로 확정(§5.3). 좋아요 카운트 동기화는 초안에서 "서비스 계층 처리"로 검토했으나, PR #6 코드가 이미 DB 트리거를 전제로 작성돼 있었고 #16에서 DB 트리거 방식으로 최종 확정했다(§5.4).
- Q-06(병합 거부 시 별도 Passage), Q-08(탈퇴 닉네임 포맷), Q-09(같은 Opinion 내 Decoration 겹침 허용 여부)는 Phase 1 흔적 작성 로직을 짤 때 재확인.
- **`NicknameGenerator`(§5.1)는 Phase 3(인증)에서만 쓰이지만, 지금 단계에 미리 만들어 단위 테스트로 검증해두면** 인증 착수 시 바로 회원가입 플로우에 꽂아 넣을 수 있다. 급하지 않으면 스킵해도 무방.

---

## 10. 검증 방법

- 각 도메인에 `@DataJpaTest`(Repository, 인덱스/제약 검증) + 순수 단위 테스트(`DecorationMergeSelector`, `NicknameGenerator`, `SimilarPassageFinder`) + `@WebMvcTest`(Controller, permitAll/인증 경계 검증) 3단 구성.
- 테스트명은 컨벤션.md 1-2 규칙 준수: `@DisplayName`은 문장형("~테스트" 지양), BDD Given/When/Then 스타일.
  - 예: `"좋아요를 이미 누른 흔적에 다시 누르면 좋아요가 취소된다"`
- `DecorationMergeSelector`는 겹침/동점자 케이스를 표로 만들어 파라미터화 테스트로 검증.
- H2 프로파일로 `./gradlew test` 실행, MySQL은 로컬 docker-compose로 별도 프로파일 검증.
- 인증 플로우는 카카오 개발자 콘솔의 테스트 앱 키로 실제 로그인 E2E 1회 수동 검증.
