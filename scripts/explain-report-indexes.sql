SHOW INDEX FROM psychological_reports;

EXPLAIN
SELECT id, risk, emotion, confidence, created_at
FROM psychological_reports
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 20;

EXPLAIN
SELECT id, user_id, risk, emotion, created_at
FROM psychological_reports
WHERE risk = 'high'
  AND created_at >= CURRENT_DATE - INTERVAL 30 DAY
ORDER BY created_at DESC
LIMIT 100;
