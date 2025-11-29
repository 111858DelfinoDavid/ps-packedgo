-- Migration: Create event_consumptions junction table
-- Description: Creates a many-to-many relationship between events and consumptions
-- Date: 2025-11-29

-- Create the junction table
CREATE TABLE IF NOT EXISTS event_consumptions (
    event_id BIGINT NOT NULL,
    consumption_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, consumption_id),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (consumption_id) REFERENCES consumptions(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_event_consumptions_event_id ON event_consumptions(event_id);
CREATE INDEX idx_event_consumptions_consumption_id ON event_consumptions(consumption_id);

-- Optional: Populate with existing data (all consumptions for each event based on createdBy)
-- This makes existing events keep showing all their consumptions
INSERT INTO event_consumptions (event_id, consumption_id)
SELECT DISTINCT e.id, c.id
FROM events e
INNER JOIN consumptions c ON c.created_by = e.created_by AND c.active = true
WHERE NOT EXISTS (
    SELECT 1 FROM event_consumptions ec 
    WHERE ec.event_id = e.id AND ec.consumption_id = c.id
);

-- Verification query
-- SELECT e.name AS event_name, COUNT(ec.consumption_id) AS consumption_count
-- FROM events e
-- LEFT JOIN event_consumptions ec ON e.id = ec.event_id
-- GROUP BY e.id, e.name
-- ORDER BY e.name;
