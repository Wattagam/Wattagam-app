CREATE TABLE guestbook_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    place_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    image_object_key VARCHAR(255) NOT NULL,
    message VARCHAR(80) NULL,
    client_request_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_guestbook_entries PRIMARY KEY (id),
    CONSTRAINT uk_guestbook_entries_request UNIQUE (user_id, client_request_id),
    CONSTRAINT uk_guestbook_entries_image_key UNIQUE (image_object_key),
    CONSTRAINT fk_guestbook_entries_place FOREIGN KEY (place_id) REFERENCES places (id),
    CONSTRAINT fk_guestbook_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_guestbook_entries_place_created (place_id, created_at, id),
    INDEX idx_guestbook_entries_user_created (user_id, created_at, id)
);

-- 방명록이 없던 시기의 임시 값은 외래 키를 걸 수 없으므로 비운다.
UPDATE stamps SET first_entry_id = NULL WHERE first_entry_id IS NOT NULL;

-- 첫 방명록을 삭제해도 도장은 유지하고 연결만 끊는다.
ALTER TABLE stamps
    ADD CONSTRAINT fk_stamps_first_entry FOREIGN KEY (first_entry_id) REFERENCES guestbook_entries (id) ON DELETE SET NULL;
