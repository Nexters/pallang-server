# 스포일러/읽기상태 노출 설계 — 기획 확인 필요 (이슈 #15)

## 배경

이슈 #15(대목/흔적 조회 API) 구현 중, 스포일러 방지와 관련된 문서 문구가 **서로 다른 두 메커니즘**을 가리키고 있다는 걸 발견했다. `requirements.md`가 둘 다 "(스포 방지)"라는 표현을 써서 같은 것처럼 보이지만, 실제로는 기준 축과 UX 흐름이 다르다. 백엔드 구현은 일단 두 메커니즘을 구분해 진행했고, 그중 하나(②)는 기획 확인 전까지 **잠정 결정**으로 두고 작업을 계속한다.

## 두 가지 메커니즘

### ① 읽기 상태 기반 페이지 범위 제한

`requirements.md` FR-WRITE-08:
> 읽는 중: 어디까지 읽었는지 페이지 표시. **그 이후 페이지는 내용이 보이지 않음**
> 읽을 예정: **첫 부분만 보여주고 이후는 보이지 않음** (스포 방지)

`backend-plan.md` §5.7:
```
READING  → passage.pageNumber <= userBookStatus.currentPage
PLANNED  → passage.pageNumber == MIN(pageNumber) (해당 책의 첫 대목만)
미설정   → PLANNED와 동일 취급
```

- **기준 축**: 이 유저가 이 책을 어디까지 읽었다고 밝혔는가 (`UserBookStatus`)
- **문서에 리빌(reveal) 버튼 언급 없음**. "보이지 않음"이라고만 되어 있고, FR-OPINION-08은 비로그인 사용자가 범위 밖 페이지를 시도하면 "로그인 유도"라고만 명시 — 클릭 한 번으로 보이는 흐름이 아니라 별도 행동(로그인 또는 읽기상태 갱신)을 요구하는 것으로 읽힘.
- **현재 구현 (하드 블록, 확정)**: `PassageVisibilityFilter`(`domain/passage/application/PassageVisibilityFilter.java`)가 QueryDSL 조건으로 이 범위를 벗어난 Passage를 **쿼리 결과에서 완전히 제외**한다. 비로그인 사용자가 첫 페이지가 아닌 페이지를 요청하면 `PassageService.getVisiblePassagesByPage`가 `LoginRequiredException`(401)을 던진다.
- **왜 하드 블록으로 확정했는가**: 여기에 "그냥 눌러서 보기" 버튼을 두면 `READING`/`currentPage`를 서버에 저장하고 필터링하는 로직 자체가 장식이 된다. 진행 상태를 실제로 갱신해야만 범위가 넓어지는 게 이 기능의 존재 이유이므로, 이 부분은 재확인 없이 하드 블록으로 간다.

### ② 개별 대목의 스포일러 표기 (작성 시 토글)

`requirements.md` FR-VIEW-03:
> 스포일러 표기된 대목은 **해당 페이지에서** 블러 처리. **[버튼]을 눌러야** 대목 + 흔적을 확인 가능

- **기준 축**: 이 문장(Passage) 자체가 작성자에 의해 스포일러로 표시됐는가 (`Passage.isSpoiler`, 흔적 작성 시 토글 — `CreateOpinionRequest.isSpoiler` → `OpinionService.createNewPassage`)
- **전제**: "해당 페이지에서"라는 표현대로, 이건 **①을 이미 통과해서 볼 수 있는 페이지 안**에서의 이야기다. 아직 못 읽은 페이지의 스포일러 여부와는 무관.
- **문서에 버튼이 명시적으로 있음** — 그런데 버튼을 눌렀을 때 서버 재요청이 있는지, 즉시 반응해야 하는지는 명시돼 있지 않음.

## 현재 구현 (잠정, 기획 확인 전)

`PassageResponse.PassagesByPage.Detail`(`domain/passage/presentation/response/PassageResponse.java`)은 `isSpoiler` 값과 무관하게 `quotedText`와 병합된 `decorations`를 **항상 그대로** 반환하고, `isSpoiler` 플래그만 함께 내려준다. 블러 처리와 버튼 클릭 시 확인은 **프론트에서 서버 재요청 없이** `isSpoiler` 플래그만으로 처리한다고 가정했다.

이렇게 정한 이유:
- 스포일러는 접근 제어가 아니라 UX 배려용이라 네트워크상 노출 자체는 문제되지 않는다고 판단.
- "버튼을 누르면 즉시 확인 가능해야 한다"는 요구(로딩 없이 바로 보임)를 만족하려면 데이터가 이미 클라이언트에 있어야 한다.

## 기획 확인 필요 — 두 가지 질문

**① 읽기 상태 기반 제한**
"읽는 중은 현재 페이지까지, 읽을 예정은 첫 페이지까지만 노출"은 아직 안 읽은 페이지를 **아예 못 보게 막는** 건지, 아니면 스포일러처럼 "블러 처리 후 버튼 누르면 볼 수 있는" 형태인지? 예를 들어 "읽을 예정" 상태 유저가 페이지 목록을 볼 때, 아직 못 보는 페이지들이 "여기 흔적 있음(잠김)"처럼 존재는 보이는지, 아니면 그 페이지가 있다는 사실조차 안 보이는지.

**② 개별 대목의 스포일러 표기**
- 유저는 그 페이지에 스포일러 대목이 **존재한다는 사실**은 알 수 있어야 하는가? (예: "스포일러 흔적 2개 있음")
- 블러 처리된 카드에 페이지 번호나 대략적인 분량 같은 **메타 정보**는 보여도 되는가, 아니면 완전히 아무것도 안 보이다가 버튼 눌러야 전부 나타나야 하는가?
- 버튼을 누르면 **그 즉시(서버 요청 없이)** 바로 보여야 하는가, 아니면 눌렀을 때 한 번 더 로딩되는 정도는 괜찮은가?

## 답변에 따라 달라지는 백엔드 설계

②의 세 번째 질문(즉시 반응 vs 로딩 허용)이 실제 구현을 가르는 핵심 분기다.

| 답변 | 백엔드 설계 |
|---|---|
| 즉시 반응 필요 (로딩 없이 바로 확인) | **현재 구현 유지**: 항상 전체 데이터 + `isSpoiler` 플래그 전송, 클라이언트가 블러/리빌 처리 |
| 로딩 한 번 정도 허용 | 서버가 `isSpoiler=true`인 대목은 `quotedText`/`decorations`를 마스킹해서 응답하고, 버튼 클릭 시 호출할 별도 "리빌" API(또는 쿼리 파라미터)를 새로 설계 |
| 존재/개수만 알면 됨, 내용은 몰라야 함 | 위 "로딩 허용" 안에 더해, `GET /api/books/{bookId}/passages`(페이지 번호 목록) 응답에도 스포일러 개수/여부를 별도로 포함할지 결정 필요 |

## 관련 코드 위치

- `domain/passage/application/PassageVisibilityFilter.java` — 읽기상태 기반 페이지 범위 필터 (①, 하드 블록, 확정)
- `domain/passage/application/PassageService.java` — `getVisiblePassagesByPage`(비로그인 401 분기), `getMergedDecorationsByPassageId`
- `domain/passage/presentation/response/PassageResponse.java` — `PassagesByPage.Detail`(②, 현재 마스킹 없음, 잠정)
- `domain/passage/presentation/docs/PassageControllerDocs.java` — 관련 Swagger 설명

## 관련 PR / 이슈

- #15 (본 문서가 다루는 조회 API 전체)
- PR #30
