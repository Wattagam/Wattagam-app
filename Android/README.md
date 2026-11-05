# 왔다감 Android

Kotlin + Jetpack Compose 네이티브 앱. Android Studio에서 이 `Android/` 폴더를 엽니다. 백엔드와 Gradle 프로젝트를 분리합니다.

## 현재 범위

- [공통 디자인 Figma](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=247-101)의 색상·타이포그래피·크기 기준
- Noto Sans KR 로컬 가변 폰트 (400/500/700), 라이선스: `licenses/NotoSansKR-OFL.txt`
- 버튼·입력창·상단바·위치 상태·도장판의 첫 구현
- Navigation 3 기반 개발용 부품 목록과 상세 화면, 입력 상태 보존, 좁은 화면·큰 글자 Preview
- 실제 로그인·GPS·카메라·AR·API 연결 없음. 샘플 상태를 실제 기능으로 간주하지 않습니다.

## 환경

- Android SDK Platform 37, Build Tools 36.0.0, JDK 17 이상 (Gradle 호환 JDK)
- AGP 9.3.2, Gradle 9.5.1, Kotlin/Compose compiler 2.4.10
- 최소 Android 8.0 / API 26: 가변 폰트 지원을 포함한 초기 기준. 지원 기기는 실제 기능 검증 시 확인합니다.
- SDK 경로는 개인 `local.properties`의 `sdk.dir` 또는 `ANDROID_HOME`으로 지정합니다. 저장소에는 경로를 커밋하지 않습니다.
- AGP의 built-in Kotlin을 사용하므로 `org.jetbrains.kotlin.android` 플러그인을 중복 적용하지 않습니다.

## 검증

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

기기 연결 후 UI 동작 검증:

```sh
./gradlew :app:connectedDebugAndroidTest
```

테스트는 로딩 중 중복 등록 차단, 일반 버튼 콜백, 상세 화면 진입 후 복귀를 검증합니다. 연결 기기가 없으면 실행 성공을 주장하지 않습니다. 현재 순수 JVM 테스트는 없습니다.

## 다음 작업

1. 공통 컴포넌트를 Figma와 기기에서 비교하고 하단 탐색·장소 항목·방명록 카드 보완
2. 샘플 데이터로 로그인·지도·편집·등록·갤러리 화면 조립
3. 화면 비즈니스 상태가 생기는 시점에 ViewModel/StateFlow와 lifecycle-aware 수집 추가
4. API 단계에 Retrofit/OkHttp, 이미지 로딩 단계에 Coil 추가. Hilt·DataStore·지도·카카오·카메라·AR은 실제 사용 시 추가

버전은 `gradle/libs.versions.toml`에서 관리합니다. 사용하지 않는 라이브러리는 미리 추가하지 않습니다.

폰트 원본: https://github.com/google/fonts/tree/main/ofl/notosanskr
