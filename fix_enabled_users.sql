-- Fix users with enabled = null or false
UPDATE users SET enabled = true WHERE enabled IS NULL OR enabled = false;
 
-- Check the results
SELECT id, email, enabled, deleted FROM users; 