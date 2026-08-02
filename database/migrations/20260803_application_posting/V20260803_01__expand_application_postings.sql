-- Phase 1 (Expand): 새 테이블과 nullable 연결 컬럼만 준비한다.
-- 애플리케이션을 중지하고 DB 스냅샷을 생성한 뒤 실행한다.

CREATE TABLE IF NOT EXISTS `schema_migrations` (
    `version` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `applied_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`version`)
) ENGINE = InnoDB;

DROP PROCEDURE IF EXISTS guard_expand_application_postings;
DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS drop_index_if_exists;
DROP PROCEDURE IF EXISTS drop_foreign_keys_for_column;

DELIMITER //

CREATE PROCEDURE guard_expand_application_postings()
BEGIN
    IF EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE version = '20260803_01'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration 20260803_01 has already been applied';
    END IF;
END//

CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', target_table,
            '` ADD COLUMN `', target_column, '` ', column_definition
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

CREATE PROCEDURE drop_index_if_exists(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = target_index
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', target_table,
            '` DROP INDEX `', target_index, '`'
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

CREATE PROCEDURE drop_foreign_keys_for_column(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64)
)
BEGIN
    DECLARE finished INTEGER DEFAULT 0;
    DECLARE foreign_key_name VARCHAR(64);
    DECLARE foreign_keys CURSOR FOR
        SELECT constraint_name
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
          AND referenced_table_name IS NOT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;

    OPEN foreign_keys;
    drop_loop: LOOP
        FETCH foreign_keys INTO foreign_key_name;
        IF finished = 1 THEN
            LEAVE drop_loop;
        END IF;

        SET @ddl = CONCAT(
            'ALTER TABLE `', target_table,
            '` DROP FOREIGN KEY `', foreign_key_name, '`'
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END LOOP;
    CLOSE foreign_keys;
END//

DELIMITER ;

CALL guard_expand_application_postings();

CREATE TABLE IF NOT EXISTS `application_postings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `source_job_posting_id` BIGINT NULL,
    `company_name` VARCHAR(255) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `deadline` DATE NULL,
    `thumbnail_url` VARCHAR(500) NULL,
    `original_url` VARCHAR(2048) NULL,
    `platform` VARCHAR(255) NOT NULL DEFAULT 'DIRECT',
    `career_type` VARCHAR(255) NULL,
    `category` VARCHAR(50) NULL,
    `region` VARCHAR(100) NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_application_postings_user_source`
        UNIQUE (`user_id`, `source_job_posting_id`),
    CONSTRAINT `fk_application_postings_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_application_postings_source`
        FOREIGN KEY (`source_job_posting_id`) REFERENCES `job_postings` (`id`)
        ON DELETE SET NULL,
    INDEX `idx_application_postings_user_id` (`user_id`),
    INDEX `idx_application_postings_source` (`source_job_posting_id`),
    INDEX `idx_application_postings_deadline` (`deadline`)
) ENGINE = InnoDB;

CALL add_column_if_missing('kanban_cards', 'application_posting_id', 'BIGINT NULL');
CALL add_column_if_missing('documents', 'application_posting_id', 'BIGINT NULL');

-- ddl-auto=update가 일부 적용된 상태에서도 백필할 수 있도록 새 FK와 유니크 인덱스를 제거한다.
CALL drop_foreign_keys_for_column('kanban_cards', 'application_posting_id');
CALL drop_foreign_keys_for_column('documents', 'application_posting_id');
CALL drop_index_if_exists('kanban_cards', 'uk_kanban_cards_application_posting');

ALTER TABLE `kanban_cards`
    MODIFY COLUMN `application_posting_id` BIGINT NULL;
ALTER TABLE `documents`
    MODIFY COLUMN `application_posting_id` BIGINT NULL;

INSERT INTO schema_migrations (`version`, `description`)
VALUES ('20260803_01', 'Expand ApplicationPosting schema');

DROP PROCEDURE IF EXISTS drop_foreign_keys_for_column;
DROP PROCEDURE IF EXISTS drop_index_if_exists;
DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS guard_expand_application_postings;
