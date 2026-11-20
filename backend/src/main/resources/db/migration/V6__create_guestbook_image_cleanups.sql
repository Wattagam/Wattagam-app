-- 방명록 행은 지웠지만 OCI 이미지 삭제를 끝내지 못한 객체 키. 정리가 끝나면 행을 지운다.
CREATE TABLE guestbook_image_cleanups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    image_object_key VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_guestbook_image_cleanups PRIMARY KEY (id),
    CONSTRAINT uk_guestbook_image_cleanups_key UNIQUE (image_object_key)
);
