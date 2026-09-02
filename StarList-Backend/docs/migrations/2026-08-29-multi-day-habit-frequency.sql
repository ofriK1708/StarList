-- Migration: multi-day habit frequency + HABIT_MISS transaction type
-- Issue: #75
-- Date: 2026-08-29
--
-- Adds the scheduled_days_of_week column to support MULTI_DAY habits
-- (e.g. Mon/Wed/Fri).  Stored as a comma-separated list of ISO day numbers
-- (1=Mon … 7=Sun), e.g. "1,3,5".  Max 6 days, max 13 characters ("1,2,3,4,5,6,7").
--
-- Run BEFORE deploying the new backend build.

ALTER TABLE habits
    ADD COLUMN IF NOT EXISTS scheduled_days_of_week VARCHAR(20);

-- MULTI_DAY is a new valid value for the frequency column.
--
-- CORRECTION (2026-09-02): the note that previously stood here — that frequency is a free
-- VARCHAR validated only in the application layer — was wrong. HabitEntity.frequency is
-- @Enumerated(EnumType.STRING), and Hibernate 6 emits a CHECK constraint listing the enum
-- values present when the column was created. ddl-auto=update never widens it, so inserting
-- a MULTI_DAY habit fails with:
--   ERROR: new row for relation "habits" violates check constraint "habits_frequency_check"
-- See 2026-09-02-multi-day-frequency-check-constraint.sql, which must also be run.
