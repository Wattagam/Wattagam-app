# Android 파일 구조와 2인 협업 가이드

## 목표와 현재 상태

왔다감은 중앙대학교 캠퍼스 투어 앱이다. Kotlin과 Jetpack Compose로 Figma의 공통 디자인을 구현한다. 현재는 API 연동 이전의 공통 컴포넌트와 개발용 샘플 화면 단계다. 알림 기능과 백그라운드 위치 수집은 범위에서 제외한다.

Gradle 모듈은 `app` 하나로 시작하고, 기능별 패키지로 작업을 분리한다. 모듈 분리는 빌드 시간이나 의존성 격리 문제가 실제로 생겼을 때 검토한다. 아래 트리는 확장 기준이며, 아직 필요하지 않은 폴더나 빈 클래스를 미리 만들지 않는다.

## 파일 구조

```text
Android/
├── AGENTS.md
├── README.md
├── docs/architecture-and-collaboration.md
├── gradle/libs.versions.toml
└── app/src/main/
    ├── java/com/wattagam/app/
    │   ├── MainActivity.kt
    │   ├── navigation/          # 앱 전체 화면 이동과 경로
    │   ├── ui/
    │   │   ├── theme/           # 색상·폰트·간격·모양
    │   │   └── components/      # 여러 기능이 사용하는 UI
    │   ├── feature/
    │   │   ├── auth/            # 로그인
    │   │   ├── map/             # 지도·장소 선택
    │   │   ├── place/           # 장소 상세
    │   │   ├── capture/         # 카메라·AR 촬영
    │   │   ├── guestbook/       # 방명록 작성·등록
    │   │   ├── gallery/         # 기록 목록·상세
    │   │   ├── stamps/          # 내 도장
    │   │   ├── mypage/          # 마이페이지(도장판·내 기록)·계정 설정
    │   │   └── showcase/        # 개발용 컴포넌트 확인 화면
    │   └── data/                # API 연결 시 기능별로 추가
    │       ├── auth/
    │       ├── place/
    │       └── guestbook/
    └── res/                     # 문자열·이미지·폰트
```

현재 구현된 패키지는 `navigation`, `ui/theme`, `ui/components`, `feature/showcase`, `feature/auth`, `feature/map`, `feature/place`, `feature/gallery`, `feature/mypage`, `feature/write`(05~07 촬영·꾸미기·업로드 연동), `data/sample`(API 연결 전 샘플 데이터), `data/location`, `data/auth`, `data/map`, `data/draft`(작성 초안 파일)이다. 나머지는 향후 구현 계획이다. `data`에는 실제 소비자가 생길 때 API, DTO, Repository 구현을 추가한다. 별도의 domain 계층이나 UseCase는 단순 전달만을 위해 만들지 않는다.

## 기능 내부의 책임

방명록을 예로 들면 다음과 같이 확장한다.

```text
feature/guestbook/
├── GuestbookRoute.kt
├── GuestbookScreen.kt
├── GuestbookUiState.kt
├── GuestbookViewModel.kt
└── components/
    └── PublicNotice.kt
```

| 파일 | 책임 | 추가 시점 |
| --- | --- | --- |
| Screen | 상태와 콜백을 받아 Compose 화면 표시 | 화면 구현 시작 |
| components | 해당 기능에서만 쓰는 UI | 화면 분리가 필요할 때 |
| UiState | 입력·로딩·오류 등 화면 상태 표현 | 상태가 복잡해질 때 |
| ViewModel | 화면 비즈니스 상태와 요청 처리 | 비즈니스 로직이나 데이터 연결 시 |
| Route | ViewModel 연결, 생명주기에 맞는 상태 수집, 이동 콜백 연결 | ViewModel 도입 시 |

Screen에서 API나 권한 요청을 직접 실행하지 않는다. 단순 입력은 작은 UI 상태로 관리하고, 화면 비즈니스 상태는 ViewModel이 소유한다. 공통 컴포넌트는 ViewModel을 받지 않고 상태, 이벤트 콜백, Modifier를 받는다.

기능 전용 UI는 먼저 해당 기능에 둔다. 다른 기능에서도 실제로 사용하게 되면 `ui/components`로 옮긴다. 공통 컴포넌트가 특정 feature 패키지를 참조하지 않도록 한다.

## 화면 이동과 데이터 경계

- 기능 화면은 `onWriteClick(placeId)`처럼 의미 있는 이벤트를 전달하고, 실제 경로 연결은 `navigation`에서 처리한다.
- 화면 사이에는 장소 ID, 기록 ID, 임시 이미지 URI 등 필요한 최소 값만 전달한다. ViewModel이나 Bitmap, API 응답 전체를 넘기지 않는다.
- 기능끼리 상대 기능의 ViewModel을 참조하지 않는다. 공통 데이터가 필요하면 해당 데이터를 소유하는 Repository를 통해 공유한다.
- 작성 시작과 업로드 시 현재 위치를 확인하고, 최종 방문 가능 여부와 도장 지급은 서버 응답을 따른다. 사진 촬영만으로 도장을 지급하지 않는다.
- 실패·인증 만료·위치 이탈 시 초안을 유지한다. 임시 이미지는 파일로 관리하며, 프로세스 종료 후 복구는 별도로 구현하고 검증한다.

## 두 사람의 역할 분담

UI 담당과 API 담당으로 고정 분리하지 않고 사용자 흐름별로 맡는다. 각 담당자가 자신의 화면, 상태 처리, 이후 API 연결과 테스트까지 완료한다. 아래 A/B는 담당자 확정 전의 제안이다.

| 담당 | 주 담당 기능 |
| --- | --- |
| A | 로그인 → 지도 → 장소 상세 → 도장 |
| B | 촬영·AR → 방명록 작성·등록 → 갤러리 |
| 함께 합의 | 공통 디자인, 화면 이동 계약, 공유 데이터 형태 |

촬영·AR 작업량이 커지면 갤러리를 A에게 옮긴다. 작업 시작 전 이슈에 담당 기능과 수정할 공통 파일을 적는다. 공통 파일마다 해당 작업의 편집 담당자를 정하되, 영구적인 독점 소유로 만들지는 않는다.

## 충돌을 줄이는 작업 규칙

1. 기능별 짧은 브랜치와 PR을 사용한다. 두 사람이 같은 작업 브랜치에서 동시에 수정하지 않는다.
2. `navigation`, 테마, 공통 컴포넌트, 버전 카탈로그를 수정할 때 PR에 영향과 변경한 사용처를 적는다.
3. 여러 작업이 같은 공통 파일을 필요로 하면 필요한 공통 변경을 먼저 작은 PR로 반영한다. 다른 담당자는 합의된 콜백과 샘플 상태로 화면을 진행한다.
4. 다른 사람이 작업 중인 파일을 통째로 교체하거나 무관한 포맷 변경을 섞지 않는다.
5. 문자열 키는 새 기능부터 `guestbook_`, `capture_`처럼 기능 접두어를 사용한다. 문자열 충돌이 반복되면 기능별 values XML로 분리하되 기존 키를 일괄 변경하지 않는다.
6. 의존성은 `gradle/libs.versions.toml`에서 관리하고, 실제 사용하는 기능을 추가할 때 도입한다.
7. PR에는 바뀐 사용자 동작, 실제 수행한 검증, 미구현 범위를 적는다. 샘플 상태를 실연동으로 표현하지 않는다.

## 구현과 검증 순서

1. 공통 테마와 컴포넌트를 Figma 및 기기 화면과 비교한다.
2. 샘플 상태와 Preview로 각 기능 화면을 구현한다. 좁은 화면·큰 글자·빈 화면·로딩·오류 상태를 확인한다.
3. 화면 이동, 시스템 뒤로가기, 입력과 초안의 유지 범위를 연결한다.
4. 실제 API와 기기 기능을 연결하면서 ViewModel과 데이터 계층을 추가한다.
5. 기능별 수락 조건을 검증하고 PR을 올린다. 버튼 중복 입력 차단, 실패 시 초안 유지, 위치 재확인처럼 관찰 가능한 동작을 기준으로 한다.

공통 빌드 및 검사 명령은 [README](../README.md)의 검증 절차를 따른다. 순수 로직 테스트는 `app/src/test`, 기기에서 실행하는 UI 테스트는 `app/src/androidTest`에 실제 패키지 구조를 따라 둔다. 문서만 수정한 경우 앱 테스트를 불필요하게 반복하지 않고 링크와 코드 일치 여부를 확인한다.
