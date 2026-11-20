-- 왔다감 MVP 최소 ERD (MySQL 8 / InnoDB) — 설계 참고용. 정본은 Flyway 마이그레이션
-- ERDCloud: 상단 메뉴 Import -> SQL(DDL) 에서 이 파일 내용을 붙여넣기

CREATE TABLE users (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  kakao_user_id BIGINT       NOT NULL COMMENT '카카오 사용자 식별자',
  nickname      VARCHAR(10)  NULL     COMMENT '닉네임(2~10자, 설정 전 NULL)',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시(UTC)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_kakao (kakao_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자';

CREATE TABLE places (
  id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '장소 ID',
  course_order     TINYINT       NOT NULL COMMENT '코스 순서',
  name             VARCHAR(50)   NOT NULL COMMENT '장소명',
  description      TEXT          NOT NULL COMMENT '소개',
  highlight        VARCHAR(100)  NULL     COMMENT '관람 포인트',
  directions       VARCHAR(255)  NULL     COMMENT '찾아가는 길',
  visit_point_name VARCHAR(100)  NULL     COMMENT '방문 확인 지점명',
  latitude         DECIMAL(10,7) NOT NULL COMMENT '위도',
  longitude        DECIMAL(10,7) NOT NULL COMMENT '경도',
  visit_radius_m   INT           NOT NULL DEFAULT 30 COMMENT '방문 반경(m)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_places_order (course_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='투어 장소(3곳)';

CREATE TABLE guestbook_entries (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '방명록 ID',
  place_id          BIGINT       NOT NULL COMMENT '장소 ID',
  user_id           BIGINT       NOT NULL COMMENT '작성자 ID',
  image_object_key  VARCHAR(255) NOT NULL COMMENT 'OCI 객체 키(guestbook/<UUID>.jpg)',
  message           VARCHAR(80)  NULL     COMMENT '선택 메시지(0~80자)',
  client_request_id VARCHAR(64)  NOT NULL COMMENT '재시도 중복 방지 키',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시(UTC)',
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시(UTC)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_entry_request (user_id, client_request_id),
  KEY idx_entry_place_created (place_id, created_at, id),
  CONSTRAINT fk_entry_place FOREIGN KEY (place_id) REFERENCES places (id),
  CONSTRAINT fk_entry_user  FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공개 방명록(전체 공개만)';

CREATE TABLE stamps (
  id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '도장 ID',
  user_id        BIGINT   NOT NULL COMMENT '사용자 ID',
  place_id       BIGINT   NOT NULL COMMENT '장소 ID',
  first_entry_id BIGINT   NULL     COMMENT '도장을 받은 첫 방명록(삭제 시 NULL, 도장은 유지)',
  earned_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '획득일시(UTC)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stamp_user_place (user_id, place_id),
  CONSTRAINT fk_stamp_user  FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_stamp_place FOREIGN KEY (place_id) REFERENCES places (id),
  CONSTRAINT fk_stamp_entry FOREIGN KEY (first_entry_id) REFERENCES guestbook_entries (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='장소별 도장(장소당 1개)';
