-- Migration: Add description column to event_categories table
-- Date: 2025-11-20
-- Description: Adds description field to event categories to allow more detailed category information

-- Add description column if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'event_categories' 
        AND column_name = 'description'
    ) THEN
        ALTER TABLE event_categories 
        ADD COLUMN description VARCHAR(500);
        
        RAISE NOTICE 'Column description added to event_categories table';
    ELSE
        RAISE NOTICE 'Column description already exists in event_categories table';
    END IF;
END $$;

-- Update existing categories with default description if needed
UPDATE event_categories 
SET description = 'Categoría de evento'
WHERE description IS NULL;

-- Verification query
SELECT id, name, description, active, created_by 
FROM event_categories 
ORDER BY id;
