CREATE TABLE stamps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    first_entry_id BIGINT NULL,
    earned_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_stamps PRIMARY KEY (id),
    CONSTRAINT uk_stamps_user_place UNIQUE (user_id, place_id),
    CONSTRAINT fk_stamps_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_stamps_place FOREIGN KEY (place_id) REFERENCES places (id)
);
