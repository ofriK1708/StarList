# Custom Habit Frequency — Design Spec

**Date:** 2026-08-28  
**Branch:** feature/custom-habit-frequency  
**Related issue:** https://github.com/ofriK1708/StarList/issues/71

---

## Overview

Extend habit tracking from daily-only to support three frequency types: **DAILY** (unchanged), **WEEKLY** (once per week on a chosen day), and **CUSTOM** (once every 7, 14, or 30 days). The radial circle visualization adapts to show one segment per expected completion in the current month rather than one per calendar day.

---

## Out of Scope

Multi-day-per-week patterns (e.g. Mon/Wed/Fri, Mon–Fri) are explicitly out of scope for this iteration. They require an array-of-days data model and independent per-slot completion logic that meaningfully increases complexity. Flag for a follow-up issue.

---

## 1. DB Schema Changes

New nullable columns on the `habits` table (added via Hibernate `update` in dev; a migration script for prod):

| Column | Type | Nullable | Purpose |
|---|---|---|---|
| `scheduled_day_of_week` | `SMALLINT` (1=Mon … 7=Sun) | yes | WEEKLY: which day the habit is due |
| `scheduled_time_type` | `VARCHAR(16)` | yes | WEEKLY & CUSTOM: `MORNING` / `AFTERNOON` / `EVENING` / `CUSTOM` |
| `scheduled_hour` | `SMALLINT` (0–23) | yes | Only when `scheduled_time_type = CUSTOM` |
| `custom_interval_days` | `SMALLINT` | yes | CUSTOM only: 7, 14, or 30 (enforced in service layer) |

**Constraints:**
- `scheduled_day_of_week` only present when `frequency = WEEKLY`
- `custom_interval_days` only present when `frequency = CUSTOM`; values outside {7, 14, 30} rejected with 400
- `scheduled_hour` only present when `scheduled_time_type = CUSTOM`
- Time fields are informational — they do not gate completion

New enum in `model.enums`:
```
ScheduledTimeType { MORNING, AFTERNOON, EVENING, CUSTOM }
```

---

## 2. Backend Logic

### 2.1 Completion Period

Each frequency type defines a **period** — the window within which exactly one completion is allowed:

| Frequency | Period | Already-completed check |
|---|---|---|
| DAILY | single calendar day | `existsByHabit_IdAndCompletedDate(habitId, today)` |
| WEEKLY | Mon–Sun ISO week containing today | `existsByHabit_IdAndCompletedDateBetween(habitId, weekStart, weekEnd)` |
| CUSTOM | interval window anchored to `createdAt` | `existsByHabit_IdAndCompletedDateBetween(habitId, periodStart, periodEnd)` |

**CUSTOM period calculation:**
```
periodIndex = floor(daysBetween(habit.createdAt, today) / intervalDays)
periodStart = createdAt + (periodIndex × intervalDays)
periodEnd   = periodStart + intervalDays - 1
```

### 2.2 Late-Completion Penalty (WEEKLY only)

When completing a WEEKLY habit:
- `today.dayOfWeek ≤ scheduledDayOfWeek` → on time → full reward, no penalty
- `today.dayOfWeek > scheduledDayOfWeek` → late → `net = max(0, reward - penalty)` coins awarded; completion still recorded; streak still advances

CUSTOM has no late penalty (no specific scheduled day within the period).

### 2.3 Streak Semantics

| Frequency | Streak unit | Increment condition |
|---|---|---|
| DAILY | days | completed yesterday |
| WEEKLY | weeks | completed in the immediately preceding ISO week |
| CUSTOM | periods | completed in the immediately preceding interval period |

The `currentStreak` field on `HabitEntity` stores the count; the unit is now frequency-dependent and communicated to the frontend via the response DTO.

### 2.4 `buildMonthCompletions` Refactor

`HabitService.buildMonthCompletions` currently iterates every calendar day. It must be refactored to:

1. Compute the list of **expected completion dates** for the month given the habit's frequency config.
   - DAILY: every calendar day from max(createdAt, monthStart) to min(today, monthEnd)
   - WEEKLY: dates of the scheduled day-of-week falling in the month
   - CUSTOM: the `periodStart` date of each interval period that overlaps the month
2. For each expected date, check whether the completions set contains any date within that period.
3. Map each slot to `DONE` / `MISSED` / `NA` using the same rules as today.

The return type stays `List<CompletionStatus>` but length is now variable (not always 28–31).

### 2.5 New / Changed Files (Backend)

| File | Change |
|---|---|
| `model/enums/ScheduledTimeType.java` | New enum |
| `model/domain/Habit.java` | Add 4 new fields |
| `repository/entity/HabitEntity.java` | Add 4 new JPA columns |
| `repository/mapper/HabitMapper.java` | Map new fields |
| `repository/api/HabitCompletionRepository.java` | Add `existsByHabit_IdAndCompletedDateBetween` |
| `service/dto/AddHabitRequest.java` | Add optional frequency-config fields |
| `service/dto/UpdateHabitRequest.java` | Add optional frequency-config fields |
| `service/dto/HabitResponse.java` | Expose new fields + `streakUnit` string |
| `service/HabitService.java` | Update `completeHabit` + `buildMonthCompletions` |
| `service/HabitCompletionService.java` | Add `existsForPeriod(habitId, start, end)` |

---

## 3. Frontend Design

### 3.1 Add / Edit Habit Modal

Frequency selection becomes a multi-step form:

1. **Frequency type** — radio/tab: Daily | Weekly | Custom
2. **If Weekly:**
   - Day-of-week selector: Mon Tue Wed Thu Fri Sat Sun (single select)
   - Time-of-day: Morning | Afternoon | Evening | Custom (if Custom → hour picker 00–23)
3. **If Custom:**
   - Interval: Every week | Every 2 weeks | Every month (hard cap — no options beyond monthly)
   - Same time-of-day picker as Weekly

### 3.2 Habit Card Circle

The radial SVG adapts segment count to frequency:

| Frequency | Segments | Segment labels |
|---|---|---|
| DAILY | days in month (28–31) | day number |
| WEEKLY | occurrences of scheduled day in month (4–5) | date of that day (e.g. "4", "11", "18", "25") |
| CUSTOM | number of periods overlapping the month (1–4) | period start date |

Backend drives this via `monthCompletions` array length — frontend only needs to render N segments regardless of what N is. No frontend period-calculation logic needed.

### 3.3 Streak Label

```
DAILY  → "{n} day streak"
WEEKLY → "{n} week streak"
CUSTOM → "{n} period streak"
```

`HabitResponse` gains a `streakUnit: 'day' | 'week' | 'period'` field to drive this.

### 3.4 New / Changed Files (Frontend)

| File | Change |
|---|---|
| `services/habitsApi.ts` | Add new fields to request/response types |
| `components/HabitTracker.tsx` | Pass `streakUnit` to card; adapt segment label |
| `components/AddHabitModal.tsx` | New — multi-step frequency form (currently inline or missing) |
| `components/EditHabitModal.tsx` | Add frequency-config fields |

---

## 4. Validation Rules

- CUSTOM `intervalDays` must be one of {7, 14, 30}; anything else → 400
- `scheduledDayOfWeek` required when `frequency = WEEKLY`; forbidden otherwise
- `scheduledHour` required when `scheduledTimeType = CUSTOM`; ignored otherwise
- A habit cannot be completed more than once per period (any frequency)
- Min frequency = once per month (30 days); no interval > 30 supported

---

## 5. What Is Not Changing

- Coin reward/penalty amounts (still difficulty-based)
- Soft-delete behavior
- `HabitCompletion` table structure (no new columns needed)
- Achievement triggers (fire on the same events as today)
- Daily habits — zero behavior change
