---
name: commit-convention
description: Use whenever creating a git commit (git commit -m) in this repository. Ensures the commit message follows this project's emoji + type convention, e.g. "✨ feat: 설명".
---

# 커밋 메시지 컨벤션

이 저장소의 커밋 메시지는 `{이모지} {타입}: {설명}` 형식을 따릅니다. `git log`에서 확인되는 실제 컨벤션입니다.

```
✨ feat: Comment 1-depth 제약을 정적 팩토리로 이동
🐛 fix: EnableJpaAuditing을 별도 Config 클래스로 분리
♻️ refactor: Policy 상수를 별도 클래스 대신 엔티티 상단으로 이동
🧹 chore: 핵심 도메인 개발을 위한 인프라 세팅
✅ test: Book 도메인 API 테스트 추가
```

## 타입 ↔ 이모지 매핑

| 타입 | 이모지 | 용도 |
|---|---|---|
| feat | ✨ | 새로운 기능 추가 |
| fix | 🐛 | 버그 수정 |
| refactor | ♻️ | 동작 변경 없는 구조 개선 |
| chore | 🧹 | 빌드/설정/패키지 등 자잘한 작업 (초기 커밋 일부는 🔧 chore도 사용됨 — 신규 커밋은 🧹 사용) |
| test | ✅ | 테스트 코드 추가/수정 |
| docs | 📝 | 문서/주석/API 명세 |
| rename | 🚚 | 파일/디렉터리 이동·이름 변경 |

이슈 라벨(✨ Feature, 🐛 Bug, ♻️ Refactor, 🧹 Chore, ✅ Test 등)과 타입을 맞추면 일관성이 유지됩니다.

## 작성 규칙

- 이모지는 유니코드 문자 그대로 사용한다 (`:sparkles:` 같은 gitmoji 코드 문자열이 아님 — 저장소 초기 커밋에는 코드 문자열이 남아 있지만 현재 컨벤션은 리터럴 이모지).
- `{타입}:` 뒤 설명은 한국어, 현재형 동사 없이 변경 내용을 간결히 요약한다 (예: "~ 추가", "~ 분리", "~ 이동").
- 이 저장소는 별도 CI 커밋 린트가 없으므로, 커밋을 생성할 때 이 컨벤션을 스스로 적용한다.
- PR 제목은 커밋 컨벤션과 달리 이모지 없이 `Feat/#이슈번호 제목` 형태를 사용한다 (기존 PR 제목 참고). 커밋 메시지와 PR 제목의 형식을 혼동하지 않는다.
