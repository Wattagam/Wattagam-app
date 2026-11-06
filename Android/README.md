# 왔다감 Android

Kotlin + Jetpack Compose 네이티브 앱. Android Studio에서 이 `Android/` 폴더를 엽니다. 백엔드와 Gradle 프로젝트를 분리합니다.

## 개발 가이드

- [파일 구조와 2인 협업 가이드](docs/architecture-and-collaboration.md)
- [Android 에이전트 작업 규칙](AGENTS.md)
- [B·C 화면 및 Preview 사용법](docs/bc-screen-preview.md)

## 현재 범위

- [공통 디자인 Figma](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=247-101)의 색상·타이포그래피·크기 기준
- Noto Sans KR 로컬 가변 폰트 (400/500/700), 라이선스: `licenses/NotoSansKR-OFL.txt`
- 버튼·입력창·상단바·위치 상태·도장판의 첫 구현
- Navigation 3 기반 개발용 부품 목록과 상세 화면, 입력 상태 보존, 좁은 화면·큰 글자 Preview
- B·C의 방문 확인·촬영·사진 확인·편집·업로드 화면과 정상/범위 밖/실패/성공 상태 Preview
- 샘플 데이터 화면 흐름 (부품 목록의 '앱 화면 흐름 보기'에서 시작): 로그인 → 닉네임 → 위치 권한 → 지도 → 장소 설명 → 장소 갤러리 → 기록 보기, 마이페이지 → 설정
- 작성 시작 위치 확인 (실제 GPS): 장소 설명·갤러리의 '방명록 남기기'를 누를 때마다 현재 위치를 새로 측정해 장소 반경 30 m, 정확도 50 m 이내인지 판정 (`data/location`). 범위 안이면 B 의 04 방문 확인 화면으로 넘어가고, 거기서 '다시 확인'도 실제 위치로 판정합니다 (범위 밖이면 04-E).
- 촬영·업로드 연동 (`feature/write`): 05 촬영 선택 → 05-B 일반 촬영(CameraX) 또는 갤러리(Photo Picker) → 06 사진 확인 → 06-A 손그림·글자 꾸미기(사진과 합쳐 저장) → 07 공개 업로드. 07 '등록'은 현재 위치를 다시 재서 범위 안일 때만 진행하고, 업로드 API 전이라 '아직 등록되지 않음'을 알립니다.
- 초안 보존 (`data/draft`): 사진 원본·꾸민 사진·메시지를 장소별로 앱 내부 저장소에 둡니다. 실패·범위 밖·앱 종료에도 남고, 05 에 다시 들어오면 이어서 쓸지 묻습니다.
- AR(05-A)은 푸앙이 3D 모델·AR 연결 전이라 일반 촬영으로 안내합니다.
- 지도 (`feature/map`): 앱 키가 있고 ARM 기기면 카카오맵에 네 장소 라벨을 띄웁니다. 키가 없거나 x86 에뮬레이터(카카오맵 SDK 미지원)·지도 오류면 안내 지도 그림으로 대신합니다.
- 장소 좌표는 지도 검색의 건물 위치입니다. 현장의 확인 지점 기준으로 `SampleData` 의 위도·경도를 다시 맞춰야 하고, 반경·정확도 기준도 현장 검증 전입니다.
- 카카오 로그인 (SDK): `local.properties` 에 앱 키가 있으면 실제 카카오 로그인, 없으면 샘플 흐름으로 바로 넘어갑니다. 서버 로그인 API 전이라 받은 카카오 토큰은 아직 어디에도 보내거나 저장하지 않습니다.
- 서버·업로드·도장·AR 연결 없음. 화면의 기록·도장은 샘플입니다.

## 환경

- Android SDK Platform 37, Build Tools 36.0.0, JDK 17 이상 (Gradle 호환 JDK)
- Gradle 데몬은 `gradle/gradle-daemon-jvm.properties`에서 JDK 25로 지정합니다. Java/Kotlin 바이트코드 타깃은 17입니다.
- AGP 9.3.2, Gradle 9.5.1, Kotlin/Compose compiler 2.4.10
- 최소 Android 8.0 / API 26: 가변 폰트 지원을 포함한 초기 기준. 지원 기기는 실제 기능 검증 시 확인합니다.
- SDK 경로는 개인 `local.properties`의 `sdk.dir` 또는 `ANDROID_HOME`으로 지정합니다. 저장소에는 경로를 커밋하지 않습니다.
- AGP의 built-in Kotlin을 사용하므로 `org.jetbrains.kotlin.android` 플러그인을 중복 적용하지 않습니다.
- 카카오 로그인·카카오맵을 쓰려면 `local.properties` 에 `KAKAO_NATIVE_APP_KEY=네이티브앱키` 를 넣습니다 (Git 에 올리지 않음). 카카오 개발자 콘솔에 패키지명 `com.wattagam.app` 과 각자 디버그 키 해시를 등록하고, 카카오 로그인과 카카오맵 사용 설정을 켜야 합니다.

## 검증

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

기기 연결 후 UI 동작 검증:

```sh
./gradlew :app:connectedDebugAndroidTest
```

테스트는 로딩 중 중복 등록 차단, 일반 버튼 콜백, 상세 화면 진입 후 복귀, 닉네임 규칙, 샘플 화면 흐름을 검증합니다. JVM 테스트 `VisitRuleTest` 는 반경·정확도 판정을 검증합니다. 연결 기기가 없으면 실행 성공을 주장하지 않습니다.

## 다음 작업

1. 실기기에서 카카오 로그인·카카오맵·촬영·꾸미기·위치 재확인 확인
2. 현장에서 네 장소 좌표 측정, 반경·정확도 기준 검증
3. AR(05-A) 방식 결정 후 연결
4. 백엔드 API 명세가 나오면 Retrofit/OkHttp, 카카오 토큰 → 서버 로그인, 화면 상태를 ViewModel/StateFlow 로 이동. 이미지 로딩 단계에 Coil

버전은 `gradle/libs.versions.toml`에서 관리합니다. 사용하지 않는 라이브러리는 미리 추가하지 않습니다.

폰트 원본: https://github.com/google/fonts/tree/main/ofl/notosanskr
