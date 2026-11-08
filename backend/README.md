# 백엔드 실행과 DB 연결

Docker를 실행한 상태에서 모든 Compose 명령을 저장소 루트에서 실행합니다.

처음 한 번 `.env`를 준비합니다. 기존 파일은 덮어쓰지 않습니다.

```bash
cp -n .env.example .env
```

`.env`의 `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`를 로컬 개발용 비밀번호로 설정합니다.

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

DB 포트·이름을 바꿨다면 JDBC URL도 맞춥니다. Docker 백엔드와 동시에 실행할 때는 IDE의 `SERVER_PORT`를 8081 등 다른 포트로 지정합니다.

종료는 `docker compose down`으로 합니다. DB 데이터는 볼륨에 보존됩니다. 이미 초기화된 DB의 비밀번호는 `.env`만 바꿔도 변경되지 않습니다.
