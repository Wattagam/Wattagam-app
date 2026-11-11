# 백엔드 실행과 DB 연결

Docker를 실행한 상태에서 모든 Compose 명령을 저장소 루트에서 실행합니다.

처음 한 번 `.env`를 준비합니다. 기존 파일은 덮어쓰지 않습니다.

```bash
cp -n .env.example .env
```

`.env`의 `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`를 로컬 개발용 비밀번호로 설정합니다.

인증 서버 실행에 필요한 값도 `.env`에 설정합니다.

| 환경 변수 | 값 |
|---|---|
| `AUTH_JWT_SECRET` | 아래 명령으로 생성한 32바이트 이상 Base64 서명 키 |
| `KAKAO_APP_ID` | Android와 같은 카카오 Developers 앱의 **숫자 앱 ID**. 네이티브 앱 키·REST API 키와 다릅니다. |

```bash
openssl rand -base64 32
```

생성한 키를 `.env`에 저장해 사용합니다. 키를 바꾸면 기존 JWT는 사용할 수 없습니다. 설정이 없거나 잘못되면 서버가 시작되지 않습니다. 실제 키와 토큰은 Git·로그에 남기지 않습니다.

백엔드와 DB를 함께 실행합니다. 기본 백엔드 포트는 8080입니다.

```bash
docker compose up --build
```

임시로 8081 포트를 사용하려면 다음과 같이 실행합니다.

```bash
BACKEND_PORT=8081 docker compose up --build
```

상태 응답의 `status`가 `UP`이면 정상입니다. 8081로 실행했다면 아래 주소의 포트도 8081로 바꿉니다.

```bash
curl http://localhost:8080/actuator/health
```

DB 도구에서는 다음 정보로 접속합니다.

| 항목 | 값 |
|---|---|
| Host | `127.0.0.1` |
| Port | `13306` (`.env`의 `DB_PORT`) |
| Database | `.env`의 `MYSQL_DATABASE` — 기본 `wattagam` |
| User | `.env`의 `MYSQL_USER` — 기본 `wattagam` |
| Password | `.env`의 `MYSQL_PASSWORD` |

백엔드 컨테이너는 `db:3306`으로 자동 연결합니다.

IDE에서 실행하려면 DB만 먼저 준비하고, Java 21로 `BackendApplication`을 실행합니다.

```bash
docker compose up -d --wait db
```

IDE 실행 설정에 아래 환경 변수를 지정합니다. IDE는 `.env`를 자동으로 읽지 않습니다.

| 환경 변수 | 값 |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:13306/wattagam?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` |
| `SPRING_DATASOURCE_USERNAME` | `.env`의 `MYSQL_USER` |
| `SPRING_DATASOURCE_PASSWORD` | `.env`의 `MYSQL_PASSWORD` |
| `AUTH_JWT_SECRET` | `.env`의 `AUTH_JWT_SECRET` |
| `KAKAO_APP_ID` | `.env`의 `KAKAO_APP_ID` |

DB 포트·이름을 바꿨다면 JDBC URL도 맞춥니다. Docker 백엔드와 동시에 실행할 때는 IDE의 `SERVER_PORT`를 8081 등 다른 포트로 지정합니다.

종료는 `docker compose down`으로 합니다. DB 데이터는 볼륨에 보존됩니다. 이미 초기화된 DB의 비밀번호는 `.env`만 바꿔도 변경되지 않습니다.

## 인증 API

| 메서드 | 경로 | 동작 |
|---|---|---|
| POST | `/api/auth/kakao` | `{ "kakaoAccessToken": "..." }` 검증 후 서비스 JWT와 사용자·닉네임 설정 상태 반환 |
| PATCH | `/api/me/nickname` | 서비스 Bearer JWT와 `{ "nickname": "푸앙이" }`로 닉네임 설정·변경 |
| POST | `/api/auth/logout` | 서비스 Bearer JWT로 로그아웃 요청 확인. Android가 저장된 서비스 토큰 삭제 |
| DELETE | `/api/me` | 서비스 Bearer JWT로 본인 회원 탈퇴 요청 |

서비스 JWT 유효기간은 기본 3600초입니다. 만료되면 카카오 인증을 다시 진행합니다. 로그아웃 응답은 이미 발급된 JWT의 서버 즉시 폐기를 보장하지 않습니다.

닉네임 미설정 사용자는 닉네임 설정·로그아웃·회원 탈퇴를 사용할 수 있습니다. 닉네임은 2~10자 한글·영문·숫자로 설정하며, 저장 후 같은 JWT를 계속 사용합니다. 로그인 응답의 `isNewUser`는 이번 계정 생성 여부, `needsNickname`은 현재 닉네임 미설정 여부입니다.

카카오 Developers 앱 등록과 SDK 로그인은 Android에서 진행합니다. 서버는 전달된 토큰의 유효성·앱 ID를 검증합니다. 테스트는 카카오 응답을 대체하고 실제 JWT·MySQL을 검증하므로, 실제 SDK 토큰을 사용한 로그인 연동 확인은 별도로 필요합니다.

카카오 사용자 식별자는 공식 API의 `Long` 값을 `BIGINT`로 저장합니다. 기존 ERD의 `VARCHAR(64)` 표기는 이 구현과 맞춰 갱신해야 합니다. 카카오 식별자가 포함될 수 있는 Hibernate JDBC 오류 원문 로그는 비활성화합니다.

## 회원 탈퇴

`DELETE /api/me`는 JWT로 확인한 본인만 요청할 수 있으며 닉네임 설정 전에도 허용합니다. 탈퇴 정책은 계정·방명록·OCI 이미지·도장의 즉시 영구 삭제입니다. 현재 브랜치는 실제 방명록·OCI 정리 기능이 연결되기 전까지 `503 USER_503_1`을 반환합니다. 연결 후에는 필수 정리와 DB 커밋이 끝나야 `200 USER_200_2`를 반환합니다. 탈퇴 완료 후 기존 JWT는 보호 API에서 `401`로 거절되며, 같은 카카오 계정으로 다시 가입하면 새 계정을 만듭니다.

방명록·OCI 구현은 `com.wattagam.user.WithdrawalContentCleanup.deleteForUser(userId: Long): Boolean`을 제공해야 합니다. 탈퇴와 같은 DB 트랜잭션에서 호출하며, `true`는 방명록·이미지 정리 완료이고 `false`는 재시도 가능한 실패입니다. 모든 OCI 삭제가 성공할 때까지 키 조회에 필요한 DB 레코드를 유지합니다. 일부 이미지가 먼저 삭제되면 재시도에서 이미 없는 키를 정리 완료로 처리합니다. 도장은 사용자 외래 키의 `ON DELETE CASCADE`로 정리합니다.

방명록 등록·이미지 쓰기는 첫 OCI 또는 방명록 쓰기 전에 `UserRepository.findByIdForUpdate(userId)`로 회원 탈퇴와 같은 사용자 행 락을 얻어 DB 트랜잭션이 종료될 때까지 유지합니다. 사용자 조회 결과가 없으면 작업을 중단합니다. OCI 통신에는 연결·응답 시간 제한을 설정합니다.

## 장소 조회와 방문 확인

| 메서드 | 경로 | 동작 |
|---|---|---|
| GET | `/api/places` | 장소 목록과 좌표·장소별 방문 반경 조회 |
| GET | `/api/places/{id}` | 장소 소개·관람 포인트·찾아가는 길·방문 확인 지점 조회 |
| POST | `/api/places/{id}/visits` | 현재 GPS로 선택한 장소의 작성 가능 여부 확인 |

서비스 Bearer JWT와 닉네임 설정 완료가 필요합니다. 장소 조회에는 위치 정보가 필요하지 않습니다.

`GET /api/places`의 `stampEarned`는 로그인한 사용자의 실제 도장 획득 여부입니다. `hasGuestbook`은 방명록 조회를 연동할 때 추가합니다.

초기 대상은 310관(75m), 도서관(40m), 청룡연못(30m)입니다. 서버는 중심점과 현재 위치 사이의 거리가 해당 장소 반경 이하인지 판정합니다. Android 연동 시 기존 30m 고정값 대신 API의 `visitRadiusMeters`를 사용해야 합니다.

초기 데이터는 Flyway로 등록하며, 방문 판정 중심 좌표는 다음과 같습니다.

| 장소 | 위도 | 경도 | 반경 |
|---|---|---|---|
| 310관 | 37.503645 | 126.956032 | 75m |
| 도서관 | 37.504779 | 126.957959 | 40m |
| 청룡연못 | 37.505625 | 126.957321 | 30m |

GPS 정보는 예상 위치 오차 20m 이하, 측정 후 60초 이내일 때 사용하며 기기 시계가 서버보다 앞서는 오차는 10초까지 허용합니다. 위치 오차를 방문 반경에 더하지 않습니다. 기준은 `application.properties`의 `app.visit` 설정에서 관리합니다.

방문 확인 요청은 JSON 숫자인 `latitude`, `longitude`, `accuracyMeters`와 위치 측정 시각인 `measuredAt`(UTC ISO 8601 문자열)을 전달합니다. 작성 가능 여부는 응답의 `result.canWrite`로 판단합니다. 범위 밖·정확도 부족·오래된 위치도 판정 자체가 성공하면 HTTP 200이며, `reasonCode`가 각각 `OUTSIDE_RADIUS`, `LOW_ACCURACY`, `STALE_LOCATION`입니다. 잘못된 입력이나 허용 오차를 넘는 미래 시각은 HTTP 400입니다.

방문 확인만으로 방문 기록을 저장하거나 도장을 지급하지 않습니다. 방명록 최종 등록에서도 현재 위치를 다시 검증하며, 도장은 해당 장소의 첫 공개 저장 성공 시 지급합니다.

## 스탬프 조회와 방명록 연동

`GET /api/me/stamps`는 닉네임 설정을 마친 사용자의 서비스 Bearer JWT로 본인 스탬프를 조회합니다. `result.stamps`는 미획득 장소를 포함한 전체 장소를 코스 순서로 담습니다. `earnedCount`는 획득 수, `totalCount`는 등록된 전체 장소 수입니다. `earnedAt`은 UTC ISO 8601 시각이고, `firstEntryId`는 첫 발급에 사용한 방명록 ID입니다. 미획득 장소의 두 값은 `null`입니다.

방명록 등록 서비스는 현재 GPS 재검증과 이미지 처리 조건을 충족해 엔트리를 저장한 뒤, 커밋 전에 `com.wattagam.stamp.StampService.grantForPublishedEntry(userId: Long, placeId: Long, entryId: Long): Unit`을 호출합니다. 이 메서드는 `MANDATORY` 전파를 사용하므로 기존 쓰기 트랜잭션이 필요하며, 호출자 롤백 시 발급도 함께 롤백됩니다.

중복 발급 요청은 정상 처리하며 최초 `earnedAt`과 `firstEntryId`를 보존합니다. DB의 `(user_id, place_id)` 고유 제약은 동시 요청에서도 사용자별·장소별 발급을 한 건으로 제한합니다.

사용자 삭제 시 `users` 외래 키의 `ON DELETE CASCADE`로 스탬프를 정리합니다. 방명록 구현 후 별도 마이그레이션에서 `first_entry_id` 외래 키에 `ON DELETE SET NULL`을 적용해, 첫 방명록이 삭제돼도 스탬프를 보존합니다.

## 방명록 이미지 저장소

### Android·방명록 연결 규칙

- Android가 사진과 꾸미기를 합성한 **JPEG 한 장**을 보냅니다. 형식은 `image/jpeg`이며 최대 5 MiB(5,242,880바이트), 가로·세로 각각 최대 4,096px입니다. 업로드 API에서 요청 크기를 먼저 제한하고, 저장 모듈에서 JPEG 바이트와 크기를 검증합니다.
- 백엔드가 `GuestbookImageStorage.createKey()`로 `guestbook/<UUID>.jpg`를 만듭니다. 방명록 DB에는 `image_object_key`를 저장합니다. 원본 파일명, 사용자 입력 키, OCI 주소나 임시 URL을 저장하지 않습니다.
- 방명록 조회 응답의 이미지 주소는 백엔드의 방명록 ID 기반 이미지 경로로 정합니다. Android는 서비스 JWT로 요청하고, 백엔드는 방명록을 볼 수 있는지 확인한 뒤 `image/jpeg`와 `Cache-Control: private, no-store`로 응답합니다. 키를 인자로 받는 공개 API는 제공하지 않습니다.
- `put(key, jpeg)`는 기존 객체를 덮어쓰지 않습니다. OCI가 조건 불일치(412)를 반환해 `false`가 되면 기존 객체를 보존하고 새 키를 사용합니다. `read(key)`는 객체가 없을 때 `null`을 반환합니다. `delete(key)`는 객체가 이미 없는 경우에도 완료로 처리합니다. 권한·버킷·통신 오류는 정리 성공으로 처리하지 않습니다.
- 방명록 등록은 먼저 사용자 잠금·작성 권한·GPS·중복 게시 키를 확인합니다. 업로드 응답을 받지 못해도 객체가 저장됐을 수 있으므로, 시도한 키를 정리 재시도에 사용할 수 있게 보존합니다. 이미지 저장 후 방명록과 도장 저장이 실패하면 이번 요청에서 만든 이미지를 정리합니다. 수정은 새 키로 저장하고 DB 커밋 후 이전 객체를 정리합니다. 탈퇴도 모든 대상 이미지 삭제를 확인할 때까지 DB에 키를 보존합니다.

이 모듈은 서버 내부 저장소만 제공합니다. 방명록 API의 요청·응답, DB 컬럼과 탈퇴 정리 연결은 방명록 구현에서 적용합니다.

### 개발용 OCI 준비와 실제 저장 검증

OCI 콘솔에서 프로젝트 컴파트먼트의 **Object Storage → Buckets**로 이동해 `wattagam-images-dev`를 생성합니다. 저장소 계층은 Standard, 공개 접근은 Private, 버전 관리는 Disabled로 둡니다. 삭제를 막는 보존 규칙은 적용하지 않습니다. 콘솔에서 해당 버킷의 리전과 Object Storage 네임스페이스를 확인합니다. [버킷 생성](https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/managingbuckets_topic-To_create_a_bucket.htm) · [버전 관리](https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/usingversioning.htm)

개발용 OCI API 서명 프로필을 로컬 `~/.oci/config`에 설정합니다. 개인키는 저장소 밖에 두고 Git, Docker 이미지와 로그에 남기지 않습니다. 앱 프로필에는 지정 버킷의 `OBJECT_CREATE`, `OBJECT_READ`, `OBJECT_DELETE`만 부여합니다. 버킷을 만드는 권한은 콘솔 운영 계정에 둡니다. [SDK 인증 설정](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm) · [OCI Object Storage 권한](https://docs.oracle.com/en-us/iaas/Content/Identity/policyreference/objectstoragepolicyreference.htm)

`<개발용 그룹>`과 `<컴파트먼트>`를 실제 이름으로 바꿔 프로젝트 컴파트먼트의 IAM 정책에 적용합니다.

```text
Allow group <개발용 그룹> to manage objects in compartment <컴파트먼트> where all {target.bucket.name = 'wattagam-images-dev', any {request.permission = 'OBJECT_CREATE', request.permission = 'OBJECT_READ', request.permission = 'OBJECT_DELETE'}}
```

기본 실행과 로컬 테스트에서는 OCI 연결이 꺼져 있습니다. IDE나 호스트에서 백엔드와 연결하려면 `OCI_STORAGE_ENABLED=true`와 아래 환경 변수를 설정합니다. Compose는 기본 로컬 DB 실행에 그대로 사용할 수 있습니다.

| 환경 변수 | 값 |
|---|---|
| `OCI_STORAGE_REGION` | 버킷 리전 ID |
| `OCI_STORAGE_NAMESPACE` | Object Storage 네임스페이스 |
| `OCI_STORAGE_BUCKET` | 개발 버킷 `wattagam-images-dev` |
| `OCI_CONFIG_FILE` | 선택 사항. 생략하면 `~/.oci/config` |
| `OCI_CONFIG_PROFILE` | API 서명 프로필 이름. 기본 `DEFAULT` |

실제 OCI 연동 검증은 위 버킷과 프로필을 설정한 쉘에서 명시적으로 실행합니다. 테스트가 만든 임의 키의 합성 JPEG만 업로드·조회·삭제하고, 삭제 재시도와 최종 객체 부재를 확인합니다.

```bash
cd backend
OCI_STORAGE_SMOKE_TEST=true ./gradlew test --tests '*OciImageStorageSmokeTest' --rerun-tasks
```

## 테스트와 빌드

```bash
cd backend
./gradlew test
./gradlew bootJar
```

테스트에는 Docker가 필요합니다. 테스트 전용 인증 설정과 격리된 MySQL을 사용하며 운영 `.env`나 DB를 변경하지 않습니다.
