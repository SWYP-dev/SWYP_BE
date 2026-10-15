CREATE TABLE job_feed_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    platform ENUM('DIRECT','EXTERNAL','JOBABA','PUBLIC','PUBLIC_PERSONNEL','ROCKETPUNCH','SARAMIN','WORKNET') NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    old_company_name VARCHAR(255) NOT NULL,
    new_company_name VARCHAR(255) NOT NULL,
    old_deadline DATE,
    new_deadline DATE,
    old_original_url VARCHAR(1000),
    new_original_url VARCHAR(1000),
    changed_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_job_feed_changes_pending (processed_at, changed_at, id)
) ENGINE=InnoDB;

ALTER TABLE notifications
    ADD COLUMN reason ENUM('DEADLINE_REMINDER','JOB_FEED_CHANGED') NOT NULL DEFAULT 'DEADLINE_REMINDER',
    ADD COLUMN job_feed_change_id BIGINT NULL,
    MODIFY COLUMN days_before_deadline INT NULL,
    MODIFY COLUMN message TEXT NOT NULL,
    MODIFY COLUMN status ENUM('FAILED','PENDING','SUCCESS') NOT NULL,
    ADD CONSTRAINT fk_notifications_job_feed_change FOREIGN KEY (job_feed_change_id)
        REFERENCES job_feed_changes (id),
    ADD CONSTRAINT uk_notifications_change_card_channel
        UNIQUE (job_feed_change_id, kanban_card_id, type);
