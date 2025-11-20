-- Migration: Fix inactive categories and events
-- Date: 2025-11-20
-- Description: Updates existing categories and events that were created as inactive due to the bug

-- 1. Add description column to event_categories if it doesn't exist
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
    END IF;
END $$;

-- 2. Update all event_categories to active=true
UPDATE event_categories 
SET active = true 
WHERE active = false OR active IS NULL;

RAISE NOTICE 'Updated % event categories to active', (SELECT COUNT(*) FROM event_categories WHERE active = true);

-- 3. Verify event_categories
SELECT 
    id, 
    name, 
    description,
    active, 
    created_by 
FROM event_categories 
ORDER BY id;

-- 4. Update all events to active=true and status='ACTIVE'
UPDATE events 
SET 
    active = true,
    status = 'ACTIVE'
WHERE active = false OR status != 'ACTIVE';

RAISE NOTICE 'Updated % events to active', (SELECT COUNT(*) FROM events WHERE active = true);

-- 5. Verify events
SELECT 
    id, 
    name, 
    active, 
    status,
    category_id,
    created_by 
FROM events 
ORDER BY id;
