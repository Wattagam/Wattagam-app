# API 목록

공통: 인증이 필요한 API는 `Authorization: Bearer {서비스 JWT}`를 사용합니다. 응답은 `isSuccess / status / code / message / result / timeStamp` envelope입니다(이미지 조회 성공 응답 제외).

| 도메인 | 메서드 | 엔드포인트 | 담당 BE | 명세 |
| --- | --- | --- | --- | --- |
| auth | POST | `/api/auth/kakao` | 고태영 | Notion API 명세 DB |
| auth | POST | `/api/auth/logout` | 고태영 | Notion API 명세 DB |
| user | PATCH | `/api/me/nickname` | 고태영 | Notion API 명세 DB |
| user | DELETE | `/api/me` | 고태영 | Notion API 명세 DB |
| place | GET | `/api/places` | 고태영 | Notion API 명세 DB |
| place | GET | `/api/places/{id}` | 고태영 | Notion API 명세 DB |
| place | POST | `/api/places/{id}/visits` | 고태영 | Notion API 명세 DB |
| stamp | GET | `/api/me/stamps` | 고태영 | Notion API 명세 DB |
| guestbook | POST | `/api/places/{id}/guestbook` | 이시형 | [01](guestbook/01_방명록_작성.md) |
| guestbook | GET | `/api/places/{id}/guestbook` | 이시형 | [02](guestbook/02_장소별_갤러리_조회.md) |
| guestbook | GET | `/api/guestbook/{id}` | 이시형 | [03](guestbook/03_방명록_상세_조회.md) |
| guestbook | PATCH | `/api/guestbook/{id}` | 이시형 | [04](guestbook/04_방명록_수정.md) |
| guestbook | DELETE | `/api/guestbook/{id}` | 이시형 | [05](guestbook/05_방명록_삭제.md) |
| guestbook | GET | `/api/me/guestbook` | 이시형 | [06](guestbook/06_내_방명록_목록_조회.md) |
| guestbook | GET | `/api/guestbook/{id}/image` | 이시형 | [07](guestbook/07_방명록_이미지_조회.md) |

## 방명록 에러 코드 (`ErrorCode`에 추가됨)

| code | HTTP | 의미 |
| --- | --- | --- |
| GUESTBOOK_400_1 | 400 | 메시지 80자 초과 |
| GUESTBOOK_403_1 | 403 | 방문 반경 밖 (OUTSIDE_RADIUS) |
| GUESTBOOK_403_2 | 403 | 위치 정확도 낮음 (LOW_ACCURACY) |
| GUESTBOOK_403_3 | 403 | 위치 측정 후 시간 경과 (STALE_LOCATION) |
| GUESTBOOK_404_1 | 404 | 존재하지 않는 방명록 |
| GUESTBOOK_503_1 | 503 | 이미지 저장소 처리 실패 (저장소가 꺼진 환경 포함) |

## 방명록 성공 코드

| code | HTTP | API |
| --- | --- | --- |
| GUESTBOOK_201_1 | 201 | 방명록 작성(새로 저장) |
| GUESTBOOK_200_1 | 200 | 방명록 작성(같은 `clientRequestId` 재요청) |
| GUESTBOOK_200_2 | 200 | 장소별 갤러리 조회 |
| GUESTBOOK_200_3 | 200 | 방명록 상세 조회 |
| GUESTBOOK_200_4 | 200 | 방명록 수정 |
| GUESTBOOK_200_5 | 200 | 방명록 삭제 |
| GUESTBOOK_200_6 | 200 | 내 방명록 목록 조회 |

방명록 API 7개는 2026-11-20 기준 백엔드 구현과 로컬 통합 테스트를 마쳤습니다(배포·Android 연동은 미확인). 미확정 정책(수정은 메시지만, 삭제 후 이미지 정리 실패는 200 + 재시도)은 기본값으로 구현했습니다.
