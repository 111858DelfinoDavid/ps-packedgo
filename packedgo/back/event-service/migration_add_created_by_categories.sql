-- Migration: Add created_by column to categories tables for multitenant support
-- Date: 2025-10-29
-- Description: Adds created_by field to event_categories and consumption_categories tables

-- Add created_by to event_categories
ALTER TABLE event_categories 
ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- Add created_by to consumption_categories
ALTER TABLE consumption_categories 
ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- Create indexes for better performance on filtering by created_by
CREATE INDEX IF NOT EXISTS idx_event_categories_created_by 
ON event_categories(created_by);

CREATE INDEX IF NOT EXISTS idx_consumption_categories_created_by 
ON consumption_categories(created_by);

-- Optional: Update existing records to assign them to a default admin
-- Uncomment and modify the user_id if needed:
-- UPDATE event_categories SET created_by = 1 WHERE created_by IS NULL;
-- UPDATE consumption_categories SET created_by = 1 WHERE created_by IS NULL;
