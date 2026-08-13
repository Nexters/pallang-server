---
name: pr-template
description: Use whenever creating or editing a pull request (gh pr create, gh pr edit --body, or drafting a PR description) in this repository. Ensures the PR body follows this project's standard template — the same structure used in PR #9.
---

# PR 템플릿 작성 가이드

이 저장소의 모든 PR 본문은 `.github/PULL_REQUEST_TEMPLATE.md`에 정의된 구조를 따릅니다.
`gh pr create` 또는 PR 본문을 작성/수정할 때는 아래 섹션을 **이 순서 그대로** 사용하세요.

## 템플릿 구조

```markdown
## 📌 관련 이슈
- Close #이슈번호

## 🚀 개요
> 이번 PR에서 변경된 핵심 내용을 요약해주세요.

## 📄 작업 내용
> 구체적인 작업 내용을 설명해주세요.
-
-
-

## 📸 스크린샷 / 테스트 결과 (선택)
> 결과물 확인을 위한 사진이나 테스트 로그를 첨부해주세요.

## ✅ 체크리스트
- [ ] 브랜치 전략(GitHub Flow)을 준수했나요?
- [ ] 메서드 단위로 코드가 잘 쪼개져 있나요?
- [ ] 테스트 통과 확인
- [ ] 서버 실행 확인
- [ ] API 동작 확인

---
## 🔍 리뷰 포인트 (Review Points)
> 리뷰어가 중점적으로 확인했으면 하는 부분을 적어주세요.
- (예: 이 로직이 최선일까요?)
- (예: 예외 처리 누락 여부 확인 부탁드립니다.)
```

## 작성 규칙

- 헤딩은 반드시 `##`로 시작한다 (일반 텍스트/굵은 글씨로만 쓰지 않기).
- 체크리스트 섹션과 리뷰 포인트 섹션 사이에는 `---` 구분선을 넣는다.
- `📌 관련 이슈`에는 브랜치명/이슈 번호에서 찾은 이슈를 `Close #N` 형식으로 적는다. 브랜치명이 `feat/#13`, `chore/#11`처럼 이슈 번호를 포함하면 그 번호를 사용한다.
- `📄 작업 내용`은 변경된 파일/기능을 불릿으로 나열한다. API가 있다면 표(Method/Path/설명)로 정리해도 좋다.
- `📸 스크린샷 / 테스트 결과`에는 최소한 `./gradlew test` 통과 여부를 적는다. 스크린샷이나 API 응답 예시가 있으면 첨부한다.
- `✅ 체크리스트`는 실제로 확인한 항목만 `[x]`로 표시하고, 확인하지 않았거나 해당 없는 항목은 `[ ]`로 남긴다 (임의로 전부 체크하지 않기).
- `🔍 리뷰 포인트`에는 설계 판단이 갈릴 수 있는 지점, 미해결 이슈, 리뷰어에게 확인받고 싶은 질문을 적는다. 비워두지 말고 최소 1개는 작성한다.
- 기존에 이 형식을 따르지 않는 PR 본문을 발견하면, 원래 내용을 보존하면서 위 섹션 구조로 재배치한다 (내용을 새로 지어내지 않는다).
- `gh pr create` 실행 시 사용자가 다른 담당자를 명시하지 않는 한 항상 `--assignee @me` 옵션을 포함해 작성자 본인에게 할당한다. (`gh pr create`는 기본적으로 담당자를 지정하지 않으므로 빠뜨리면 미할당으로 생성된다.) 이미 생성된 PR에 할당자를 추가/변경할 때는 `gh pr edit <번호> --add-assignee @me`를 사용한다.

## 참고

- 기준 예시: PR #9 (https://github.com/Nexters/pallang-server/pull/9)
- 템플릿 원본: `.github/PULL_REQUEST_TEMPLATE.md`
