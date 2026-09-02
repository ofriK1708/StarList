-- Migration: widen the habits.frequency check constraint to allow MULTI_DAY
-- Date: 2026-09-02
--
-- Fixes a 500 on POST /habits when creating a MULTI_DAY habit:
--   ERROR: new row for relation "habits" violates check constraint "habits_frequency_check"
--
-- WHY THIS IS NEEDED
-- The 2026-08-29 migration assumed frequency was a plain VARCHAR validated only in the
-- application layer. That is wrong. HabitEntity.frequency is @Enumerated(EnumType.STRING),
-- and Hibernate 6 generates a CHECK constraint listing the enum values in force at the time
-- the column was created. `ddl-auto=update` adds new columns but never widens an existing
-- check constraint, so every database created before MULTI_DAY existed still rejects it.
--
-- Affects any environment whose habits table predates the MULTI_DAY enum value — local dev
-- and the AWS RDS instance alike. Run it once per database.

ALTER TABLE habits DROP CONSTRAINT IF EXISTS habits_frequency_check;

ALTER TABLE habits
    ADD CONSTRAINT habits_frequency_check
        CHECK (frequency IN ('DAILY', 'WEEKLY', 'CUSTOM', 'MULTI_DAY'));

-- Verify:
--   SELECT conname, pg_get_constraintdef(oid)
--   FROM pg_constraint
--   WHERE conrelid = 'habits'::regclass AND contype = 'c';
