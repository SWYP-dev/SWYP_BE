CREATE TABLE feedback (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NULL,
    type       ENUM('BUG_REPORT','ETC','FEATURE_SUGGESTION','SERVICE_INQUIRY') NOT NULL,
    content    TEXT         NOT NULL,
    email      VARCHAR(254) NULL,
    image_key  VARCHAR(500) NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;
