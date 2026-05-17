CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    club_id BIGINT NULL,
    club_name VARCHAR(200) NULL,
    publisher_id BIGINT NOT NULL,
    publisher_name VARCHAR(100) NOT NULL,
    publisher_role VARCHAR(50) NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_announcements_scope ON announcements (scope_type, club_id);
CREATE INDEX idx_announcements_publisher ON announcements (publisher_id);

