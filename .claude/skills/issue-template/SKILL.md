---
name: issue-template
description: Use whenever creating or editing a GitHub issue (gh issue create, gh issue edit --body, or drafting an issue description) in this repository. Ensures the issue body follows this project's type-specific template under .github/ISSUE_TEMPLATE/ — the same structure used in issue #7.
---

# 이슈 템플릿 작성 가이드

이 저장소의 이슈는 `.github/ISSUE_TEMPLATE/`에 정의된 작업 유형별 템플릿을 따릅니다.
`gh issue create` 또는 이슈 본문을 작성/수정할 때는 먼저 작업 유형을 고른 뒤, 그 유형의 구조를 그대로 사용하세요.

## 유형 선택

| 유형 | 파일 | 제목 접두사 | 라벨 |
|---|---|---|---|
| 기능 구현 | `feat.md` | `feat: 기능명 - 세부 작업 내용` | ✨ Feature (엔티티/DB 중심이면 🗄️ DB, 배포/인프라면 🚀 Deploy 등 성격에 맞는 라벨 추가 가능) |
| 버그 수정 | `fix.md` | `fix: 오류명 - 요약` | 🐛 Bug |
| 단순 작업 | `chore.md` | `chore: 작업 내용` | 🧹 Chore |
| 코드 개선 | `refactor.md` | `refactor: 개선 대상 - 요약` | ♻️ Refactor |
| 배포/인프라 | `deploy.md` | `deploy: 배포 버전/환경 - 요약` | 🚀 Deploy |

제목이 이미 `feat:`/`fix:`/`chore:`/`refactor:`/`deploy:` 중 하나로 시작하면 그 유형의 템플릿을 쓰고, 접두사가 없다면 내용에 맞는 접두사를 붙인다.

## 공통 구조 (feat 예시)

```markdown
## 🎯 구현 목적
- 이 기능을 구현하려는 이유와 최종 목표를 설명합니다.

## 📝 작업 내용
> 작업을 완료하기 위한 세부 업무를 쪼개어 작성합니다.
- [ ]
- [ ]
- [ ]

## 💡 참고 사항
- 작업 시 주의사항, 참고할 만한 아키텍처나 라이브러리 정보 등이 있다면 적어주세요.
```

다른 유형은 첫 번째/두 번째 섹션 제목만 다르다 (`.github/ISSUE_TEMPLATE/<type>.md` 원본 참고):
- `fix.md`: `## 🎯 수정 목표` → `## 🚨 오류 상세 및 재현 방법`(현상/재현) → `## 📝 수정 계획` → `## 💡 참고 사항`
- `chore.md`: `## 🎯 작업 목적` → `## 📝 작업 내용` → `## 💡 참고 사항`
- `refactor.md`: `## 🎯 리팩토링 목적` → `## 🏗 개선 범위 및 작업 내용` → `## 💡 참고 사항`
- `deploy.md`: `## 🎯 배포 목표` → `## 🌐 배포 환경 및 변경 사항`(환경 명시) → `## 💡 참고 사항`

## 작성 규칙

- 헤딩은 반드시 `##`로 시작하고, 이모지를 원본 템플릿과 동일하게 유지한다.
- `📝 작업 내용`(또는 해당 유형의 두 번째 섹션)은 체크박스(`- [ ]`)로 세부 업무를 쪼갠다. 이미 완료된 항목이 있으면 `- [x]`로 표시한다.
- `💡 참고 사항`에는 관련 문서(예: `backend_plan.md` 섹션 번호), 선행/후속 이슈 번호, 미확정 사항을 적는다.
- 기존에 이 형식을 따르지 않는 이슈 본문(예: `## 배경` / `## 작업 내용` / `## 참고` 형태)을 발견하면, 원래 내용을 보존하면서 유형에 맞는 섹션 구조로 재배치한다 (내용을 새로 지어내지 않는다).
- 라벨이 없거나 제목 접두사와 라벨이 어긋나면, 저장소 라벨 목록(`gh label list`)에서 맞는 라벨을 찾아 붙인다.

## 참고

- 기준 예시: 이슈 #7 (https://github.com/Nexters/pallang-server/issues/7)
- 템플릿 원본: `.github/ISSUE_TEMPLATE/*.md`
