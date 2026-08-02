-- Phase 4 (Contract): 레거시 연결 컬럼을 제거한다.
-- 주의: 새 ApplicationPosting 기반 애플리케이션 배포와 운영 검증이 끝난 뒤 별도로 실행한다.
-- 이 단계 전까지는 기존 코드로 되돌릴 수 있지만, 실행 후에는 DB 백업 없이는 되돌릴 수 없다.

DROP PROCEDURE IF EXISTS guard_drop_legacy_job_posting_columns;
DROP PROCEDURE IF EXISTS drop_foreign_keys_for_column;
DROP PROCEDURE IF EXISTS drop_indexes_for_column;
DROP PROCEDURE IF EXISTS drop_column_if_exists;

DELIMITER //

CREATE PROCEDURE guard_drop_legacy_job_posting_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE version = '20260801_03'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Required migration 20260801_03 has not been applied';
    END IF;
    IF EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE version = '20260801_04'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration 20260801_04 has already been applied';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM kanban_cards kc
        LEFT JOIN application_postings ap ON ap.id = kc.application_posting_id
        WHERE ap.id IS NULL
    ) OR EXISTS (
        SELECT 1
        FROM documents d
        LEFT JOIN application_postings ap ON ap.id = d.application_posting_id
        WHERE ap.id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Legacy columns cannot be dropped while invalid mappings exist';
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

CREATE PROCEDURE drop_indexes_for_column(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64)
)
BEGIN
    DECLARE finished INTEGER DEFAULT 0;
    DECLARE target_index_name VARCHAR(64);
    DECLARE target_indexes CURSOR FOR
        SELECT DISTINCT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
          AND index_name <> 'PRIMARY';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;

    OPEN target_indexes;
    drop_loop: LOOP
        FETCH target_indexes INTO target_index_name;
        IF finished = 1 THEN LEAVE drop_loop; END IF;
        SET @ddl = CONCAT(
            'ALTER TABLE `', target_table,
            '` DROP INDEX `', target_index_name, '`'
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END LOOP;
    CLOSE target_indexes;
END//

CREATE PROCEDURE drop_column_if_exists(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', target_table,
            '` DROP COLUMN `', target_column, '`'
        );
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

DELIMITER ;

CALL guard_drop_legacy_job_posting_columns();
CALL drop_foreign_keys_for_column('kanban_cards', 'job_posting_id');
CALL drop_foreign_keys_for_column('documents', 'job_posting_id');
CALL drop_indexes_for_column('kanban_cards', 'job_posting_id');
CALL drop_indexes_for_column('documents', 'job_posting_id');

CALL drop_column_if_exists('kanban_cards', 'job_posting_id');
CALL drop_column_if_exists('documents', 'job_posting_id');

INSERT INTO schema_migrations (`version`, `description`)
VALUES ('20260801_04', 'Drop legacy JobPosting relation columns');

SELECT `version`, `description`, `applied_at`
FROM schema_migrations
WHERE `version` LIKE '20260801_%'
ORDER BY `version`;

DROP PROCEDURE IF EXISTS drop_column_if_exists;
DROP PROCEDURE IF EXISTS drop_indexes_for_column;
DROP PROCEDURE IF EXISTS drop_foreign_keys_for_column;
DROP PROCEDURE IF EXISTS guard_drop_legacy_job_posting_columns;
