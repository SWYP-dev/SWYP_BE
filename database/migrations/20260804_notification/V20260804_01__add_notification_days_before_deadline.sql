-- 인앱 알림을 나중에 조회해도 생성 당시의 D-7/D-3/D-1/D-Day 배지가 유지되도록
-- 마감일까지 남은 일수를 알림 레코드에 스냅샷으로 저장한다.

ALTER TABLE notifications
    ADD COLUMN days_before_deadline INT NULL AFTER message;

-- 기존 알림은 현재 메시지에 저장된 D-Day 문구를 기준으로 백필한다.
-- 예상하지 못한 형식의 기존 메시지가 있으면 NOT NULL 변경 단계에서 실패시켜
-- 잘못된 배지 값으로 조용히 변환되지 않도록 한다.
UPDATE notifications
SET days_before_deadline = CASE
    WHEN message LIKE '%D-Day%' THEN 0
    WHEN message LIKE '%D-1%' THEN 1
    WHEN message LIKE '%D-3%' THEN 3
    WHEN message LIKE '%D-7%' THEN 7
    ELSE NULL
END;

ALTER TABLE notifications
    MODIFY COLUMN days_before_deadline INT NOT NULL;
