-- Phase 3 (Constrain): 백필 검증 후 NOT NULL, 인덱스와 FK를 적용한다.
-- 성공 후 새 애플리케이션을 배포할 수 있다. 기존 job_posting_id는 계속 유지한다.

DROP PROCEDURE IF EXISTS guard_constrain_application_postings;
DROP PROCEDURE IF EXISTS assert_application_posting_links;
DROP PROCEDURE IF EXISTS drop_index_if_exists;
DROP PROCEDURE IF EXISTS drop_foreign_keys_for_column;

DELIMITER //

CREATE PROCEDURE guard_constrain_application_postings()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE version = '20260801_02'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Required migration 20260801_02 has not been applied';
    END IF;
    IF EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE version = '20260801_03'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration 20260801_03 has already been applied';
    END IF;
END//

CREATE PROCEDURE assert_application_posting_links()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM kanban_cards kc
        LEFT JOIN application_postings ap ON ap.id = kc.application_posting_id
        WHERE kc.application_posting_id IS NULL OR ap.id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Constraint migration aborted: invalid kanban card mapping';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM documents d
        LEFT JOIN application_postings ap ON ap.id = d.application_posting_id
        WHERE d.application_posting_id IS NULL OR ap.id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Constraint migration aborted: invalid document mapping';
    END IF;
    IF EXISTS (
        SELECT application_posting_id
        FROM kanban_cards
        GROUP BY application_posting_id
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Constraint migration aborted: duplicate card mapping';
    END IF;
END//

CREATE PROCEDURE drop_index_if_exists(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = target_index
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', target_table, '` DROP INDEX `', target_index, '`'
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
        IF finished = 1 THEN LEAVE drop_loop; END IF;
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

CALL guard_constrain_application_postings();
CALL assert_application_posting_links();

-- ddl-auto=update의 부분 적용 흔적과 기존 job_posting_id 기반 버전 인덱스를 정리한다.
CALL drop_foreign_keys_for_column('kanban_cards', 'application_posting_id');
CALL drop_foreign_keys_for_column('documents', 'application_posting_id');
CALL drop_index_if_exists('kanban_cards', 'uk_kanban_cards_application_posting');
CALL drop_index_if_exists('documents', 'idx_documents_application_posting_id');
CALL drop_index_if_exists('documents', 'idx_documents_version_group');

ALTER TABLE `kanban_cards`
    MODIFY COLUMN `application_posting_id` BIGINT NOT NULL,
    ADD CONSTRAINT `uk_kanban_cards_application_posting`
        UNIQUE (`application_posting_id`),
    ADD CONSTRAINT `fk_kanban_cards_application_posting`
        FOREIGN KEY (`application_posting_id`) REFERENCES `application_postings` (`id`);

ALTER TABLE `documents`
    MODIFY COLUMN `application_posting_id` BIGINT NOT NULL,
    ADD INDEX `idx_documents_application_posting_id` (`application_posting_id`),
    ADD INDEX `idx_documents_version_group` (`application_posting_id`, `version_group`),
    ADD CONSTRAINT `fk_documents_application_posting`
        FOREIGN KEY (`application_posting_id`) REFERENCES `application_postings` (`id`);

INSERT INTO schema_migrations (`version`, `description`)
VALUES ('20260801_03', 'Add ApplicationPosting constraints');

DROP PROCEDURE IF EXISTS drop_foreign_keys_for_column;
DROP PROCEDURE IF EXISTS drop_index_if_exists;
DROP PROCEDURE IF EXISTS assert_application_posting_links;
DROP PROCEDURE IF EXISTS guard_constrain_application_postings;
