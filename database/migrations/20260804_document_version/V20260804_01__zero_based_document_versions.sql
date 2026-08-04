-- FILE 문서 버전을 파일명 표기 규칙과 동일한 0-based 체계로 변환한다.
-- 애플리케이션 배포 전에 한 번만 실행해야 한다.

DROP PROCEDURE IF EXISTS migrate_document_versions_to_zero_based;

DELIMITER //

CREATE PROCEDURE migrate_document_versions_to_zero_based()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    IF EXISTS (
        SELECT 1
        FROM schema_migrations
        WHERE version = '20260804_01'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration 20260804_01 has already been applied';
    END IF;

    UPDATE documents
    SET `version` = `version` - 1
    WHERE doc_type = 'FILE'
      AND `version` > 0;

    INSERT INTO schema_migrations (`version`, `description`)
    VALUES ('20260804_01', 'Convert FILE document versions to zero-based numbering');

    COMMIT;
END//

DELIMITER ;

CALL migrate_document_versions_to_zero_based();
DROP PROCEDURE IF EXISTS migrate_document_versions_to_zero_based;
