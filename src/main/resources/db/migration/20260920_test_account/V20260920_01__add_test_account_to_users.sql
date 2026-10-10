DROP PROCEDURE IF EXISTS add_test_account_column;

DELIMITER //

CREATE PROCEDURE add_test_account_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'is_test_account'
    ) THEN
        ALTER TABLE users
            ADD COLUMN is_test_account BIT NOT NULL DEFAULT b'0';
    END IF;

    ALTER TABLE users
        MODIFY COLUMN provider ENUM('KAKAO', 'TEST') NOT NULL;
END//

DELIMITER ;

CALL add_test_account_column();
DROP PROCEDURE IF EXISTS add_test_account_column;
