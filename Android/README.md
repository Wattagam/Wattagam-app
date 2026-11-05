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
- 작성 시작 위치 확인 (실제 GPS): 장소 설명·갤러리의 '방명록 남기기'를 누를 때마다 현재 위치를 새로 측정해 장소 반경 30 m, 정확도 50 m 이내인지 판정 (`data/location`). 범위 안이면 작성 화면으로 넘기는 연결 지점(`onWriteAllowed`)만 있고, 작성 화면은 아직 없습니다.
- 장소 좌표는 지도에서 잡은 **임시 값**입니다. 현장에서 측정해 `SampleData` 의 위도·경도를 고쳐야 합니다. 반경·정확도 기준도 현장 검증 전입니다.
- 카카오 로그인 (SDK): `local.properties` 에 앱 키가 있으면 실제 카카오 로그인, 없으면 샘플 흐름으로 바로 넘어갑니다. 서버 로그인 API 전이라 받은 카카오 토큰은 아직 어디에도 보내거나 저장하지 않습니다.
- 서버·업로드·도장·카메라·AR 연결 없음. 화면의 기록·도장은 샘플입니다.

## 환경

- Android SDK Platform 37, Build Tools 36.0.0, JDK 17 이상 (Gradle 호환 JDK)
- Gradle 데몬은 `gradle/gradle-daemon-jvm.properties`에서 JDK 25로 지정합니다. Java/Kotlin 바이트코드 타깃은 17입니다.
- AGP 9.3.2, Gradle 9.5.1, Kotlin/Compose compiler 2.4.10
- 최소 Android 8.0 / API 26: 가변 폰트 지원을 포함한 초기 기준. 지원 기기는 실제 기능 검증 시 확인합니다.
- SDK 경로는 개인 `local.properties`의 `sdk.dir` 또는 `ANDROID_HOME`으로 지정합니다. 저장소에는 경로를 커밋하지 않습니다.
- AGP의 built-in Kotlin을 사용하므로 `org.jetbrains.kotlin.android` 플러그인을 중복 적용하지 않습니다.
- 카카오 로그인을 쓰려면 `local.properties` 에 `KAKAO_NATIVE_APP_KEY=네이티브앱키` 를 넣습니다 (Git 에 올리지 않음). 카카오 개발자 콘솔에 패키지명 `com.wattagam.app` 과 디버그 키 해시를 등록해야 합니다.

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

1. 빌드·린트·테스트 실행, 기기에서 위치 확인과 카카오 로그인 확인 (아직 한 번도 빌드하지 않음)
2. 현장에서 네 장소 좌표 측정, 반경·정확도 기준 검증
3. 촬영·작성 화면(B)을 `onWriteAllowed` 에 연결, 등록 직전 위치 재확인
4. 백엔드 API 명세가 나오면 Retrofit/OkHttp, 카카오 토큰 → 서버 로그인, 화면 상태를 ViewModel/StateFlow 로 이동. 이미지 로딩 단계에 Coil

버전은 `gradle/libs.versions.toml`에서 관리합니다. 사용하지 않는 라이브러리는 미리 추가하지 않습니다.

폰트 원본: https://github.com/google/fonts/tree/main/ofl/notosanskr
