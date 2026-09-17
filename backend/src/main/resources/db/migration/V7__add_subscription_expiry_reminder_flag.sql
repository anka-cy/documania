ALTER TABLE subscriptions
    ADD COLUMN expiry_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;