CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kakao_user_id BIGINT NOT NULL,
    nickname VARCHAR(10) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_kakao_user_id UNIQUE (kakao_user_id)
);
