# B·C 화면과 Compose Preview

## 범위

Figma B·C의 10개 프레임을 Compose 화면 및 상태로 구현한다. 이 단계는 UI 구현이며 실제 GPS, 카메라, AR 렌더링, 갤러리 선택, 손그림 입력·이미지 합성, 파일 저장, API 업로드는 연결하지 않는다. 서버 응답을 흉내 내는 업로드 성공 흐름도 추가하지 않았다.

화면에서 사용하는 상태와 이벤트 콜백은 외부에서 전달한다. 촬영 배경과 편집 캔버스, 업로드 사진 영역은 Compose 슬롯으로 받으므로 다음 단계에서 실제 기기 화면이나 이미지 로더를 연결할 수 있다. ViewModel과 Repository는 아직 추가하지 않았다.

## 화면 확인 방법

앱 실행 → **B · C 화면 미리보기** → 원하는 화면을 선택한다. 모든 상세 화면 상단에 `UI 샘플`을 표시한다. 실제 연동이 필요한 버튼은 준비 중 안내를 표시하며, 다른 상태는 목록에서 직접 선택한다.

Android Studio에서 `app/src/main/java/com/wattagam/app/feature/showcase/BcScreenSamples.kt`를 열고 **Split** 또는 **Design**을 선택한다. `B · 촬영`, `C · 업로드` 그룹에 기본 Preview 10개가 있고, GPS 확인, 사진 꾸미기, 공개 업로드에는 320dp·글자 1.5배 Preview도 있다. 기본 Preview는 412 × 916dp이며 시스템 UI는 Android가 그린다.

| Figma 프레임 | 구현 | Preview |
| --- | --- | --- |
| [04 GPS 성공](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=218-1811) | capture/VisitCheckScreen · Available | VisitPreview |
| [04-E 범위 밖](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=226-1542) | capture/VisitCheckScreen · Outside | OutsidePreview |
| [05 촬영 선택](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=42-266) | capture/CaptureModeScreen | ModePreview |
| [05-A AR](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=5-31709) | capture/ArCaptureScreen | ArPreview |
| [05-B 일반 촬영](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=5-32078) | capture/PhotoCaptureScreen | CameraPreview |
| [06 사진 확인](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=5-32122) | capture/PhotoPreviewScreen | PhotoPreview |
| [06-A 꾸미기](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=5-32155) | guestbook/PhotoEditorScreen | EditorPreview |
| [07 공개 업로드](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=5-32226) | guestbook/PublishScreen · Ready | PublishPreview |
| [07-E 업로드 실패](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=226-1599) | guestbook/PublishScreen · Failed | FailurePreview |
| [08 성공·도장](https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=5-32525) | guestbook/PublishSuccessScreen | SuccessPreview |

## 공통 UI와 샘플 상태

- 기존 WattagamTheme, WattagamButton, 디자인 토큰을 재사용한다. 여러 화면이 쓰는 상단바, 단계 표시, 안내 카드, 작성자 정보는 ui/components/RecordingComponents.kt에 둔다.
- 메시지, 도장 수, 작성자, 위치 상태는 화면 입력이다. PublishStatus는 Ready / CheckingLocation / Uploading / Failed로 나뉜다. 전송 중에는 입력과 등록 버튼을 비활성화한다. 메시지 제한은 상태로 받으며 Preview의 80자는 디자인 기준 예시다.
- 샘플 화면에서 메시지 입력, 펜·지우개·색상 선택, 예시 그림 지우기, 플래시 선택 표시를 조작할 수 있다. 실제 그림 그리기나 플래시 제어는 아니다.
- 샘플별 로컬 UI 상태만 관리한다. 화면 간 영구 초안 공유, 프로세스 종료 후 복구, 이미지 합성·저장은 후속 연동 범위다. 실패 화면의 보관 문구는 디자인 상태를 표현한다.
- 지도 약도는 디자인용 안내 그림이며 GPS·지도 SDK와 연결되지 않았다. 카메라 배경과 푸앙이도 정적인 예시다.

## 디자인 적용 기준과 리소스

Figma에서 내려받은 사진·푸앙이 원본은 drawable-nodpi에, SVG 아이콘·손그림·공간 링은 원본을 4배 PNG로 렌더링하여 drawable-xxxhdpi에 보관한다. 임시 Figma 다운로드 URL을 런타임에 사용하지 않는다. 동일한 푸앙이 이미지는 한 리소스를 재사용한다. 사진은 샘플에서만 주입하며 향후 촬영 결과로 교체한다.

성공 화면의 수정·삭제·더보기는 현재 MVP 제외 결정에 따라 생략한다. 공개 범위 문구는 로그인한 앱 사용자에게 공개한다는 현재 정책을 따른다. 상태 표시줄·제스처 바는 피그마 이미지를 복제하지 않고 시스템 인셋을 사용한다. 작은 화면과 큰 글자는 스크롤로 대응하고 48dp 터치 영역을 확보한다.

## 검증

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

BcScreensTest는 범위 밖 작성 진입 차단, 전송 중 중복 요청 차단, 실패 시 메시지 표시와 재시도 콜백, 샘플 촬영 안내를 검증한다. renderAllDesignStatesForVisualReview는 기본 크기와 320 × 720dp·글자 1.5배로 각 화면을 렌더링하고 앱 캐시의 bc-review에 검토용 PNG를 저장한다. 자동 픽셀 비교 테스트는 아니다.

실제 GPS 판정, 이미지 파일 보존, 업로드 재시도와 중복 저장 방지, AR 결과물은 이 UI 검증으로 검증됐다고 간주하지 않는다.

이번 구현 검증: assembleDebug, assembleDebugAndroidTest, lintDebug 통과. 에뮬레이터에서 기존 공통 컨트롤 3개와 B·C 화면 7개를 포함한 UI 테스트 10개 통과. 기본/작은 화면 총 20회 렌더링을 수행하고 캡처를 검토했다. Lint는 오류 0개, 기존 의존성 버전 안내 5개이며 순수 JVM 테스트는 NO-SOURCE다. 실기기 및 Android Studio Preview 창 자체의 렌더링은 별도로 검증하지 않았다.

## 포커스 스튜디오 적용 범위

사용자가 선택한 포커스 스튜디오 방향은 사진 꾸미기와 공개 업로드(Ready/CheckingLocation/Uploading)에만 적용한다. 검정 배경, 가로 폭을 채우는 사진, 흰색 주요 버튼, 간결한 공개·위치 안내를 사용한다. 실패 및 완료 화면, B 촬영 화면은 기존 표현을 유지한다.

`feature/guestbook/FocusStudioComponents.kt`의 내부 스캐폴드와 색상은 두 화면의 composition 안에서만 사용한다. `ui/theme`와 `ui/components`는 이번 포커스 스튜디오 적용에서 수정하지 않는다. 브랜치의 이전 커밋에 포함된 공통 단계 표시 개선과는 별개다.

사진 꾸미기의 canvas 슬롯은 `(Modifier) -> Unit`을 받아 화면이 지정한 비율을 따른다. 공개 업로드는 기존 상태/콜백을 유지하고, 위치 범위 밖·글자 수 초과 시 제출을 막으며 위치 확인/업로드 중 입력 및 재요청을 막는다. 다크 화면의 시스템 바 아이콘은 showcase 진입/이탈 때 복원된다. 실제 드로잉 엔진과 API는 아직 연결하지 않는다.

포커스 스튜디오 적용 후에도 촬영·방문·사진 확인·실패·완료의 나머지 8개 기본 화면 PNG가 적용 전과 파일 단위로 동일함을 수동 비교했다. 새 UI 테스트는 위치 범위 밖의 제출 차단/재확인 허용과 80자 초과 입력의 제출 차단/문구 유지를 검증한다.
