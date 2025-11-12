-- Migration: Add robust session tracking fields
-- Date: 2025-11-05
-- Description: Add fields for backend state authority pattern

ALTER TABLE multi_order_sessions 
ADD COLUMN IF NOT EXISTS last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS client_info VARCHAR(500),
ADD COLUMN IF NOT EXISTS attempt_count INTEGER DEFAULT 0;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_last_accessed ON multi_order_sessions(last_accessed_at);

-- Update existing sessions
UPDATE multi_order_sessions 
SET last_accessed_at = created_at 
WHERE last_accessed_at IS NULL;

-- Verification query
SELECT 
    COUNT(*) as total_sessions,
    COUNT(last_accessed_at) as with_last_access,
    COUNT(client_info) as with_client_info
FROM multi_order_sessions;
