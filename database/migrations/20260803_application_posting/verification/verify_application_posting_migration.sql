-- ApplicationPosting 마이그레이션 검증 스크립트
-- Phase 3 실행 후와 Phase 4 실행 후 각각 실행한다. 하나라도 불일치하면 SQLSTATE 45000으로 실패한다.

DROP PROCEDURE IF EXISTS verify_application_posting_migration;

DELIMITER //

CREATE PROCEDURE verify_application_posting_migration()
BEGIN
    DECLARE migration_count INT DEFAULT 0;
    DECLARE contract_applied INT DEFAULT 0;
    DECLARE matched_count INT DEFAULT 0;

    SELECT COUNT(*) INTO migration_count
    FROM schema_migrations
    WHERE version IN ('20260803_01', '20260803_02', '20260803_03');

    IF migration_count <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: migrations 01 through 03 are not complete';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'application_postings'
      AND column_name = 'source_job_posting_id'
      AND data_type = 'bigint'
      AND is_nullable = 'YES';
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: source_job_posting_id must be nullable BIGINT';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'application_postings'
          AND index_name = 'uk_application_postings_user_source'
          AND non_unique = 0
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'user_id,source_job_posting_id'
           AND COUNT(*) = 2
    ) exact_index;
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid uk_application_postings_user_source';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM information_schema.referential_constraints rc
    JOIN information_schema.key_column_usage kcu
      ON kcu.constraint_schema = rc.constraint_schema
     AND kcu.table_name = rc.table_name
     AND kcu.constraint_name = rc.constraint_name
    WHERE rc.constraint_schema = DATABASE()
      AND rc.table_name = 'application_postings'
      AND rc.constraint_name = 'fk_application_postings_source'
      AND rc.delete_rule = 'SET NULL'
      AND kcu.column_name = 'source_job_posting_id'
      AND kcu.referenced_table_name = 'job_postings'
      AND kcu.referenced_column_name = 'id';
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid fk_application_postings_source';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'kanban_cards'
      AND column_name = 'application_posting_id'
      AND data_type = 'bigint'
      AND is_nullable = 'NO';
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: kanban_cards.application_posting_id must be NOT NULL BIGINT';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'kanban_cards'
          AND index_name = 'uk_kanban_cards_application_posting'
          AND non_unique = 0
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'application_posting_id'
           AND COUNT(*) = 1
    ) exact_index;
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid uk_kanban_cards_application_posting';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'kanban_cards'
      AND constraint_name = 'fk_kanban_cards_application_posting'
      AND column_name = 'application_posting_id'
      AND referenced_table_name = 'application_postings'
      AND referenced_column_name = 'id';
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid fk_kanban_cards_application_posting';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'documents'
      AND column_name = 'application_posting_id'
      AND data_type = 'bigint'
      AND is_nullable = 'NO';
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: documents.application_posting_id must be NOT NULL BIGINT';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'documents'
      AND constraint_name = 'fk_documents_application_posting'
      AND column_name = 'application_posting_id'
      AND referenced_table_name = 'application_postings'
      AND referenced_column_name = 'id';
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid fk_documents_application_posting';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'documents'
          AND index_name = 'idx_documents_application_posting_id'
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'application_posting_id'
           AND COUNT(*) = 1
    ) exact_index;
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid idx_documents_application_posting_id';
    END IF;

    SELECT COUNT(*) INTO matched_count
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'documents'
          AND index_name = 'idx_documents_version_group'
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'application_posting_id,version_group'
           AND COUNT(*) = 2
    ) exact_index;
    IF matched_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid idx_documents_version_group';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM kanban_cards kc
        LEFT JOIN application_postings ap ON ap.id = kc.application_posting_id
        WHERE ap.id IS NULL OR kc.user_id <> ap.user_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid kanban card mapping';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM documents d
        LEFT JOIN application_postings ap ON ap.id = d.application_posting_id
        WHERE ap.id IS NULL OR d.user_id <> ap.user_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: invalid document mapping';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM application_postings ap
        LEFT JOIN kanban_cards kc ON kc.application_posting_id = ap.id
        WHERE ap.source_job_posting_id IS NOT NULL
          AND kc.id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: source-linked ApplicationPosting has no card';
    END IF;

    SELECT COUNT(*) INTO contract_applied
    FROM schema_migrations
    WHERE version = '20260803_04';

    IF contract_applied = 1 AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND column_name = 'job_posting_id'
          AND table_name IN ('kanban_cards', 'documents')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Verification failed: legacy job_posting_id column remains after Phase 4';
    END IF;
END//

DELIMITER ;

CALL verify_application_posting_migration();

SELECT
    (SELECT COUNT(*) FROM application_postings) AS application_posting_count,
    (SELECT COUNT(*) FROM kanban_cards) AS kanban_card_count,
    (SELECT COUNT(*) FROM documents) AS document_count,
    (SELECT COUNT(*) FROM application_postings WHERE source_job_posting_id IS NULL)
        AS detached_application_posting_count;

DROP PROCEDURE IF EXISTS verify_application_posting_migration;
