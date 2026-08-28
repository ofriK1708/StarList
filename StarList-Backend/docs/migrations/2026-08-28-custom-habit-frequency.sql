-- Migration: custom habit frequency
-- PR: #76  |  Issue: #71
-- Date: 2026-08-28
--
-- Adds four nullable columns to the `habits` table to support WEEKLY and
-- CUSTOM frequency scheduling.  All columns are nullable so existing DAILY
-- habits are unaffected.
--
-- Run this script against the production database BEFORE deploying the new
-- backend build.  Hibernate ddl-auto=validate will reject startup if these
-- columns are missing.

ALTER TABLE habits
    ADD COLUMN IF NOT EXISTS scheduled_day_of_week  INTEGER,
    ADD COLUMN IF NOT EXISTS scheduled_time_type    VARCHAR(16),
    ADD COLUMN IF NOT EXISTS scheduled_hour         INTEGER,
    ADD COLUMN IF NOT EXISTS custom_interval_days   INTEGER;

-- Optional: add check constraints to enforce valid values at the DB level.
-- These mirror the backend validation in HabitService.validateFrequencyConfig().
-- NOTE: PostgreSQL does not support IF NOT EXISTS for ADD CONSTRAINT,
-- so this block is safe to run only once.
ALTER TABLE habits
    ADD CONSTRAINT chk_scheduled_day_of_week
        CHECK (scheduled_day_of_week IS NULL OR scheduled_day_of_week BETWEEN 1 AND 7);

ALTER TABLE habits
    ADD CONSTRAINT chk_scheduled_hour
        CHECK (scheduled_hour IS NULL OR scheduled_hour BETWEEN 0 AND 23);

ALTER TABLE habits
    ADD CONSTRAINT chk_custom_interval_days
        CHECK (custom_interval_days IS NULL OR custom_interval_days IN (7, 14, 30));

ALTER TABLE habits
    ADD CONSTRAINT chk_scheduled_time_type
        CHECK (scheduled_time_type IS NULL
            OR scheduled_time_type IN ('MORNING', 'AFTERNOON', 'EVENING', 'CUSTOM'));
