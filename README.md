# 📚 Palang — 낯선 사람과의 교환독서

> 직접 쓰지 않아도, 아는 사람이 아니어도, 종이책·전자책 경계 없이
> 같은 대목 앞에서 서로의 생각을 비교하며 확장하는 교환독서 서비스

---

## 💡 서비스 소개

*"다른 사람들은 이 책을 읽고 무슨 생각이 들었을까?"*

같은 책이어도 사람마다 다르게 느끼고, 낯선 사람과 같은 문장에서 비슷한 울림을 받기도 한다.

Palang이 지향하는 건 단순한 감상 공유가 아니라,
**누군가 남긴 흔적을 같은 대목에서 읽은 다음 사람이 마주하고,
공감하거나 새로운 관점을 만나 생각을 넓혀가는 것**이다.

### 🚧 기존 독서모임의 한계

교환독서·독서모임에 관심은 있어도 참여를 막는 장벽이 존재한다.

- 📉 "내가 책을 많이 읽는 것도 아닌데" 하는 위축감
- 😶 감상이 "헉 좋다" 수준이라는 막막함, 낯가림
- ✏️ 책에 직접 쓰기 망설여짐 · 함께할 친구가 없음 · e북은 참여 불가

욕구는 있는데 기존 방식이 이 중 일부만 수용한다.

### ✨ Palang의 해답

> 같은 책을 읽는 낯선 사람들이, 같은 대목 앞에서 생각을 비교하고 확장해가는
> **온라인 교환독서 서비스**

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build Tool | Gradle |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL (prod) / H2 (test) |
| API Docs | Swagger (springdoc) |

---

## 📁 패키지 구조

도메인형 계층 아키텍처를 채택한다.
역할 기반으로 명확히 구분해 애매한 위치의 클래스 생성을 방지하고, 협업 시 일관성과 유지보수성을 높인다.

```
com.nexters.palang
└── {domain}
    ├── presentation    # Controller, Request/Response DTO
    ├── application     # Service (비즈니스 로직)
    ├── domain          # Entity, VO
    ├── infrastructure  # JPA Repository, 외부 연동
    └── common          # 공통 설정, 예외 처리
```

---

## 🌿 브랜치 전략 (Git Flow)

```
main       ─── 실서버 배포 브랜치 (직접 커밋 금지)
  └── develop  ─── 개발 통합 브랜치
        └── feat/#이슈번호  ─── 기능 개발 브랜치
```

| 브랜치 | 역할 |
|--------|------|
| `main` | 언제나 배포 가능한 상태 유지 |
| `develop` | feature 브랜치 병합 및 빌드 |
| `feat/#n` | 이슈 단위 기능 개발 후 develop에 머지 후 삭제 |
| `hotfix/#n` | 긴급 버그 수정 후 머지 후 삭제 |

---

## 💬 커밋 컨벤션

```
<Emoji> <type>: <subject>

예) ✨ feat: 로그인 기능 구현
    🐛 fix: 토큰 만료 오류 수정
    🔧 chore: Gradle 의존성 추가
```

| Type | Emoji | 설명 |
|------|-------|------|
| feat | ✨ | 새로운 기능 추가 |
| fix | 🐛 | 버그 수정 |
| docs | 📝 | 문서 수정 |
| style | 🎨 | 코드 포맷팅 (기능 변경 없음) |
| refactor | ♻️ | 코드 리팩토링 |
| test | ✅ | 테스트 코드 추가/수정 |
| chore | 🔧 | 빌드 설정, 패키지 매니저 수정 |
| wip | 🚧 | 미완성 임시 커밋 (지양) |
| rename | 🚚 | 파일/폴더 이름 변경 또는 이동 |

**Subject 규칙**
- 50자 이내, 마침표·특수기호 없음
- 영문 시작 시 동사 원형 + 대문자 (과거형 금지)
- 개조식으로 작성

---

## 📐 코드 컨벤션

### 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 변수 | camelCase | `userEmail` |
| 패키지 | 소문자 | `useremail` |
| URL/파일명 | kebab-case | `/user-email-page` |
| Enum/상수 | UPPER_SNAKE_CASE | `NORMAL_STATUS` |
| 메서드 | 소문자 시작 동사 | `getUserId()` |
| 클래스 | UpperCamelCase 명사 | `UserEmail` |
| 컬렉션 필드 | 복수형 (타입명 미포함) | `List<Long> ids` |

### 레이어별 메서드명

| 작업 | Controller | Service | Repository |
|------|-----------|---------|------------|
| 목록 조회 | `readXXX` | `getXXXs` / `findXXX` | `findByXXX` |
| 단건 조회 | `readXXX` | `getXXX` / `findXXX` | `findByXXX` |
| 등록 | `createXXX` | `addXXX` | `insertXXX` |
| 수정 | `updateXXX` | `modifyXXX` | `updateXXX` |
| 삭제 | `deleteXXX` | `removeXXX` | `deleteXXX` |

### 정적 팩토리 메서드

- `from` : 파라미터 1개
- `of` : 파라미터 2개 이상

### Lombok 제한

| 허용 | 금지 |
|------|------|
| `@Getter`, `@Builder`, `@RequiredArgsConstructor` 등 | `@Setter`, `@Data` |

---

## ⚠️ 예외 처리

### 에러 코드 형식

```
DOMAIN_HTTPSTATUSCODE_N
예) MEMBER_404_1, AUTH_401_2
```

```java
@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404_1", "해당 회원을 찾을 수 없습니다."),
    ;
    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
```

### 에러 응답 형식

```json
{
  "type": "/요청/uri",
  "title": "에러 제목",
  "status": 400,
  "detail": "에러 상세 메시지"
}
```

---

## 🧪 테스트 전략

| 레이어 | 방식 |
|--------|------|
| Controller | RestAssured 인수 테스트 (API당 해피케이스 + 상태코드별 대표 케이스) |
| Service | H2 DB 통합 테스트 (Mocking 지양, 리팩터링 내성 확보) |
| Repository | `@Query` 직접 작성 시에만 테스트 |

**BDD 스타일 (Given / When / Then)**

```java
@Test
@DisplayName("음료를 추가하면 주문 목록에 담긴다.")
void addBeverage_thenAddedToOrder() {
    // given
    // when
    // then
}
```

---

## 📋 PR / Issue 템플릿

`.github/` 디렉토리에 포함되어 있습니다.

| 템플릿 | 설명 |
|--------|------|
| `PULL_REQUEST_TEMPLATE.md` | PR 작성 양식 |
| `ISSUE_TEMPLATE/feat.md` | 기능 구현 이슈 |
| `ISSUE_TEMPLATE/fix.md` | 버그 수정 이슈 |
| `ISSUE_TEMPLATE/refactor.md` | 리팩토링 이슈 |
| `ISSUE_TEMPLATE/chore.md` | 단순 작업 이슈 |
| `ISSUE_TEMPLATE/deploy.md` | 배포/인프라 이슈 |
