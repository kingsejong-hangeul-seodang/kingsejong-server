# 협업 규칙

fork 없이 이 레포에서 바로 브랜치를 파서 작업합니다.

## 브랜치

| 브랜치 | 용도 |
| --- | --- |
| `main` | 배포용. 직접 push하지 않고 `dev`에서만 머지 |
| `dev` | 기본 브랜치. 모든 작업 PR은 여기로 |
| `태그/#이슈번호` | 작업 브랜치. 예: `feat/#12`, `fix/#15`, `chore/#3` |

태그는 아래 5개 중 하나를 소문자로 씁니다.

## 태그

| 이모지 | 태그 | 언제 |
| --- | --- | --- |
| ✨ | `Feat` | 기능 추가 |
| 🐛 | `Fix` | 버그 수정 |
| ♻️ | `Refactor` | 동작은 그대로, 코드 구조 변경 |
| 📝 | `Docs` | 문서 |
| 🔧 | `Chore` | 의존성, 설정, 빌드, CI, 배포 |

## 제목 규칙

| | 형식 | 예 |
| --- | --- | --- |
| 이슈 | `이모지 [태그] 제목` | `✨ [Feat] 로그인 API` |
| 브랜치 | `태그/#이슈번호` | `feat/#12` |
| 커밋 | `태그: 내용` | `Feat: 로그인 API 추가` |
| PR | `이모지 태그 #이슈번호 - 제목` | `✨ Feat #12 - 로그인 API` |

- 커밋 메시지 끝에 이슈 번호를 붙이지 않습니다. 이슈 연결은 PR 본문의 `close #12`로 합니다.
- 커밋 하나에는 한 가지 변경만 담습니다.

## 작업 순서

```bash
# 1. 이슈를 만들고 번호를 확인 (템플릿 사용)

# 2. 최신 dev에서 브랜치 파기
git switch dev
git pull origin dev
git switch -c feat/#12

# 3. 작업 후 로컬 검증 (CI와 같은 명령)
./gradlew build

# 4. 커밋하고 push
git add .
git commit -m "Feat: 로그인 API 추가"
git push -u origin feat/#12

# 5. dev로 PR 올리기 (템플릿 채우기)
gh pr create --base dev
```

6. CI가 통과하고 리뷰 1명이 승인하면 **Merge commit**으로 머지합니다.
7. 머지 후 브랜치를 지우고, 다음 작업은 다시 2번부터 시작합니다.

```bash
git switch dev
git pull origin dev
git branch -d feat/#12
```

## 작업 중에 dev가 앞서 나갔을 때

내 브랜치에서 dev를 merge로 가져옵니다. PR이 열려 있을 때 rebase와 force push는 쓰지 않습니다.

```bash
git fetch origin
git merge origin/dev
git push
```
