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

서비스 JWT 유효기간은 기본 3600초입니다. 만료되면 카카오 인증을 다시 진행합니다. 로그아웃 응답은 이미 발급된 JWT의 서버 즉시 폐기를 보장하지 않습니다.

닉네임 미설정 사용자는 닉네임 설정·로그아웃만 사용할 수 있습니다. 닉네임은 2~10자 한글·영문·숫자로 설정하며, 저장 후 같은 JWT를 계속 사용합니다. 로그인 응답의 `isNewUser`는 이번 계정 생성 여부, `needsNickname`은 현재 닉네임 미설정 여부입니다.

카카오 Developers 앱 등록과 SDK 로그인은 Android에서 진행합니다. 서버는 전달된 토큰의 유효성·앱 ID를 검증합니다. 테스트는 카카오 응답을 대체하고 실제 JWT·MySQL을 검증하므로, 실제 SDK 토큰을 사용한 로그인 연동 확인은 별도로 필요합니다.

카카오 사용자 식별자는 공식 API의 `Long` 값을 `BIGINT`로 저장합니다. 기존 ERD의 `VARCHAR(64)` 표기는 이 구현과 맞춰 갱신해야 합니다. 카카오 식별자가 포함될 수 있는 Hibernate JDBC 오류 원문 로그는 비활성화합니다.

```bash
cd backend
./gradlew test
./gradlew bootJar
```

테스트에는 Docker가 필요합니다. 테스트 전용 인증 설정과 격리된 MySQL을 사용하며 운영 `.env`나 DB를 변경하지 않습니다.
