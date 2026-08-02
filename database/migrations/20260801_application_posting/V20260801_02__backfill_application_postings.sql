-- Phase 2 (Migrate): 기존 JobPosting 스냅샷과 연결 ID를 백필한다.
-- 이 단계에서는 기존 job_posting_id 컬럼을 제거하지 않는다.

DROP PROCEDURE IF EXISTS guard_backfill_application_postings;
DROP PROCEDURE IF EXISTS assert_application_posting_backfill;

DELIMITER //

CREATE PROCEDURE guard_backfill_application_postings()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE version = '20260801_01'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Required migration 20260801_01 has not been applied';
    END IF;

    IF EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE version = '20260801_02'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration 20260801_02 has already been applied';
    END IF;
END//

CREATE PROCEDURE assert_application_posting_backfill()
BEGIN
    DECLARE invalid_card_count BIGINT DEFAULT 0;
    DECLARE invalid_document_count BIGINT DEFAULT 0;
    DECLARE duplicate_card_count BIGINT DEFAULT 0;
    DECLARE mismatched_card_user_count BIGINT DEFAULT 0;
    DECLARE mismatched_document_user_count BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO invalid_card_count
    FROM kanban_cards kc
    LEFT JOIN application_postings ap ON ap.id = kc.application_posting_id
    WHERE kc.application_posting_id IS NULL OR ap.id IS NULL;

    SELECT COUNT(*) INTO invalid_document_count
    FROM documents d
    LEFT JOIN application_postings ap ON ap.id = d.application_posting_id
    WHERE d.application_posting_id IS NULL OR ap.id IS NULL;

    SELECT COUNT(*) INTO duplicate_card_count
    FROM (
        SELECT application_posting_id
        FROM kanban_cards
        GROUP BY application_posting_id
        HAVING COUNT(*) > 1
    ) duplicated;

    SELECT COUNT(*) INTO mismatched_card_user_count
    FROM kanban_cards kc
    JOIN application_postings ap ON ap.id = kc.application_posting_id
    WHERE kc.user_id <> ap.user_id;

    SELECT COUNT(*) INTO mismatched_document_user_count
    FROM documents d
    JOIN application_postings ap ON ap.id = d.application_posting_id
    WHERE d.user_id <> ap.user_id;

    IF invalid_card_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Backfill failed: kanban_cards contains an unmapped row';
    END IF;
    IF invalid_document_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Backfill failed: documents contains an unmapped row';
    END IF;
    IF duplicate_card_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Backfill failed: multiple cards share one ApplicationPosting';
    END IF;
    IF mismatched_card_user_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Backfill failed: card and ApplicationPosting users differ';
    END IF;
    IF mismatched_document_user_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Backfill failed: document and ApplicationPosting users differ';
    END IF;
END//

DELIMITER ;

CALL guard_backfill_application_postings();

INSERT INTO `application_postings` (
    `user_id`, `source_job_posting_id`, `company_name`, `title`, `deadline`,
    `thumbnail_url`, `original_url`, `platform`, `career_type`, `category`,
    `region`, `created_at`, `updated_at`
)
SELECT DISTINCT
    jp.user_id,
    jp.id,
    jp.company_name,
    jp.title,
    jp.deadline,
    jp.thumbnail_url,
    jp.original_url,
    jp.platform,
    jp.career_type,
    jp.category,
    jp.region,
    COALESCE(jp.created_at, CURRENT_TIMESTAMP(6)),
    COALESCE(jp.updated_at, CURRENT_TIMESTAMP(6))
FROM job_postings jp
JOIN (
    SELECT job_posting_id FROM kanban_cards WHERE job_posting_id IS NOT NULL
    UNION
    SELECT job_posting_id FROM documents WHERE job_posting_id IS NOT NULL
) referenced_postings ON referenced_postings.job_posting_id = jp.id
ON DUPLICATE KEY UPDATE
    company_name = VALUES(company_name),
    title = VALUES(title),
    deadline = VALUES(deadline),
    thumbnail_url = VALUES(thumbnail_url),
    original_url = VALUES(original_url),
    platform = VALUES(platform),
    career_type = VALUES(career_type),
    category = VALUES(category),
    region = VALUES(region),
    updated_at = VALUES(updated_at);

-- 기존 job_posting_id를 기준으로 모든 레거시 행의 매핑을 확정한다.
-- UPDATE 전에 실제 변경 대상 건수를 확인할 수 있도록 출력한다.
SELECT COUNT(*) AS kanban_card_rows_to_update
FROM kanban_cards kc
JOIN application_postings ap
    ON ap.user_id = kc.user_id
   AND ap.source_job_posting_id = kc.job_posting_id
WHERE kc.id IS NOT NULL
  AND (
      kc.application_posting_id IS NULL
      OR kc.application_posting_id <> ap.id
  );

UPDATE kanban_cards kc
JOIN application_postings ap
    ON ap.user_id = kc.user_id
   AND ap.source_job_posting_id = kc.job_posting_id
SET kc.application_posting_id = ap.id
WHERE kc.id IS NOT NULL
  AND (
      kc.application_posting_id IS NULL
      OR kc.application_posting_id <> ap.id
  );

SELECT COUNT(*) AS document_rows_to_update
FROM documents d
JOIN application_postings ap
    ON ap.user_id = d.user_id
   AND ap.source_job_posting_id = d.job_posting_id
WHERE d.id IS NOT NULL
  AND (
      d.application_posting_id IS NULL
      OR d.application_posting_id <> ap.id
  );

UPDATE documents d
JOIN application_postings ap
    ON ap.user_id = d.user_id
   AND ap.source_job_posting_id = d.job_posting_id
SET d.application_posting_id = ap.id
WHERE d.id IS NOT NULL
  AND (
      d.application_posting_id IS NULL
      OR d.application_posting_id <> ap.id
  );

CALL assert_application_posting_backfill();

-- 레거시 카드가 이미 삭제되고 FILE 문서만 남은 지원 건은 ApplicationPosting을 유지하되
-- 원본 공고 연결을 해제한다. 그래야 같은 공고를 다시 카드로 등록할 때
-- uk_application_postings_user_source와 충돌하지 않는다.
UPDATE application_postings ap
LEFT JOIN kanban_cards kc ON kc.application_posting_id = ap.id
SET ap.source_job_posting_id = NULL
WHERE ap.source_job_posting_id IS NOT NULL
  AND kc.id IS NULL;

INSERT INTO schema_migrations (`version`, `description`)
VALUES ('20260801_02', 'Backfill ApplicationPosting data');

SELECT
    (SELECT COUNT(*) FROM application_postings) AS application_posting_count,
    (SELECT COUNT(*) FROM kanban_cards) AS kanban_card_count,
    (SELECT COUNT(*) FROM documents) AS document_count;

DROP PROCEDURE IF EXISTS assert_application_posting_backfill;
DROP PROCEDURE IF EXISTS guard_backfill_application_postings;
