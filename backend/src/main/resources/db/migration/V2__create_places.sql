CREATE TABLE places (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_order INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    highlight VARCHAR(100) NOT NULL,
    directions VARCHAR(255) NOT NULL,
    visit_point_name VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    visit_radius_m INT NOT NULL,
    CONSTRAINT pk_places PRIMARY KEY (id),
    CONSTRAINT uk_places_course_order UNIQUE (course_order),
    CONSTRAINT ck_places_course_order CHECK (course_order > 0),
    CONSTRAINT ck_places_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_places_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_places_visit_radius CHECK (visit_radius_m > 0)
);
