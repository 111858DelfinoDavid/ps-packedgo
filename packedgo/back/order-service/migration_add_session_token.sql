-- Migration: Add session_token column to multi_order_sessions table
-- Date: 2025-11-05
-- Purpose: Enable anonymous session recovery for checkout without JWT authentication
-- Database: PostgreSQL

-- Add session_token column
ALTER TABLE multi_order_sessions 
ADD COLUMN IF NOT EXISTS session_token VARCHAR(36) UNIQUE;

-- Generate UUIDs for existing sessions (if any)
UPDATE multi_order_sessions 
SET session_token = gen_random_uuid()::text
WHERE session_token IS NULL;

-- Add index for fast lookups
CREATE INDEX IF NOT EXISTS idx_session_token ON multi_order_sessions(session_token);

-- Verify migration
SELECT COUNT(*) as total_sessions, 
       COUNT(session_token) as sessions_with_token 
FROM multi_order_sessions;
