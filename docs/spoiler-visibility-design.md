# 스포일러 노출 설계 (확정)

## 배경

이슈 #15(대목/흔적 조회 API) 구현 당시, "스포 방지"라는 같은 표현으로 묶여 있던 **서로 다른 두 메커니즘**을 발견했다.

1. **읽기 상태(읽는 중/읽을 예정) 기반 페이지 범위 제한** — `UserBookStatus.status/currentPage`를 기준으로 아직 안 읽은 페이지의 대목 자체를 쿼리 결과에서 제외
2. **개별 대목의 스포일러 표기** — 작성 시 토글하는 `Passage.isSpoiler` 플래그로 프론트에서 블러 처리

둘 다 구현했지만 ①은 기획 확인 전까지 잠정 결정으로 남겨뒀다. 기획 확인 결과 **①은 존재하지 않는 기획**이었다 — 유저가 책을 어디까지 읽었는지를 서버가 저장하거나 그 값을 기준으로 노출 범위를 제한할 필요가 없다. 이슈 #42에서 ①을 완전히 제거하고, ②만 남기는 것으로 확정했다.

## 확정된 설계

스포일러 여부는 오직 대목(Passage) 하나 기준이다.

- **기준**: 작성자가 흔적을 남길 때 토글한 `Passage.isSpoiler` (`CreateOpinionRequest.isSpoiler` → `OpinionService.createNewPassage`)
- **서버는 항상 전체 데이터를 내려준다.** `PassageResponse.PassagesByPage.Detail`은 `isSpoiler` 값과 무관하게 `quotedText`와 병합된 `decorations`를 그대로 반환하고, `isSpoiler` 플래그를 함께 내려준다.
- **블러 처리와 확인은 전부 프론트 책임이다.** 서버 왕복 없이 `isSpoiler` 플래그만 보고 블러 처리하고, 버튼을 누르면 클라이언트에 이미 있는 데이터로 즉시 확인 가능하다.
- **읽기 상태(`UserBookStatus.status/currentPage`)는 대목 노출과 무관하다.** 로그인 여부와 관계없이 모든 사용자가 책의 모든 페이지/대목을 조회할 수 있다. `UserBookStatus` 도메인(엔티티/컨트롤러/API) 자체는 유지되지만, 다른 목적(예: 마이페이지에서 읽기 상태 표시)에만 쓰이고 대목 조회 API에는 관여하지 않는다.

## 이전 결정에서 바뀐 점 (이슈 #42)

| 항목 | 이전 (이슈 #15, 잠정) | 현재 (확정) |
|---|---|---|
| 대목 페이지 목록 조회 (`GET /api/books/{bookId}/passages`) | 읽기 상태에 따라 노출 페이지 범위가 달라짐 | 항상 책의 모든 페이지 번호를 반환 |
| 특정 페이지 대목 조회 (`GET /api/books/{bookId}/pages/{page}/passages`) | 노출 범위 밖이면 빈 배열, 비로그인이 첫 페이지 외 요청 시 401 | 인증 여부와 무관하게 항상 해당 페이지의 모든 대목 반환 |
| 스포일러 처리 | `isSpoiler` 플래그 기반, 프론트에서 블러 처리 | 변경 없음 (그대로 유지) |

제거된 컴포넌트: `PassageVisibilityFilter`, `PassageQueryRepository.findFirstVisiblePageNumber`, `PassageService`의 비로그인 401 분기.

## 관련 코드 위치

- `domain/passage/application/PassageService.java` — `getPageNumbers`, `getPassagesByPage` (읽기 상태 필터 없이 전체 조회)
- `domain/passage/infrastructure/PassageQueryRepository.java` — `findPageNumbers`, `findPassagesByPage`
- `domain/passage/presentation/response/PassageResponse.java` — `PassagesByPage.Detail`(`isSpoiler` 플래그 포함, 마스킹 없음)
- `domain/passage/presentation/docs/PassageControllerDocs.java` — 관련 Swagger 설명
- `domain/book/domain/UserBookStatus.java` 등 `UserBookStatus` 도메인 — 대목 노출과 무관하게 유지

## 관련 PR / 이슈

- #15 (본 로직이 처음 구현된 이슈, 잠정 결정을 남긴 곳)
- #42 (읽기 상태 기반 노출 제한 제거, 본 문서를 확정 버전으로 재작성)
- PR #30
