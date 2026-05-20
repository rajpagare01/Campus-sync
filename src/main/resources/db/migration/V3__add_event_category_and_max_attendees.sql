-- Add category and max_attendees to event table
ALTER TABLE event ADD COLUMN category VARCHAR(255);
ALTER TABLE event ADD COLUMN max_attendees INT;

-- Ensure engagement counts exist on post (some legacy posts might not have them if they were added later)
-- Actually, the DTO handles them by querying services, so no need for columns on post table for now.
-- But we should ensure the event columns are added.
