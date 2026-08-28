# Custom Habit Frequency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend habit tracking to support DAILY (unchanged), WEEKLY (once per week on a chosen day, with late-completion penalty), and CUSTOM (once every 7, 14, or 30 days) frequency types, with the radial circle adapting to show one segment per expected completion in the current month.

**Architecture:** A new `HabitPeriodCalculator` service centralises all period-boundary date math, keeping `HabitService` focused on orchestration. `buildMonthCompletions` is refactored to delegate to `HabitPeriodCalculator` and returns a list whose length equals expected completions (not always days-in-month). The frontend reads `monthCompletions.length` to determine segment count — no new frontend period logic needed.

**Tech Stack:** Java 25, Spring Boot 4.0.3, MapStruct, JUnit 5 + Mockito + AssertJ, React + TypeScript, Tailwind CSS.

**Spec:** `docs/superpowers/specs/2026-08-28-custom-habit-frequency-design.md`

## Global Constraints

- Java 25, Spring Boot 4.0.3, multi-module Maven (`model → repository → service → controller → app`).
- All enums stored as `EnumType.STRING` in JPA entities.
- Dev profile uses `ddl-auto: update` — new columns auto-created; prod uses `validate` and needs a migration script (out of scope for this plan, but noted).
- Lombok `@Data @Builder` on domain objects; `@Getter @Setter @Builder` on JPA entities.
- Immutable response DTOs use Java `record` + `@Builder`; mutable request DTOs use `@Builder` record.
- Tests: `./mvnw test -pl service -Dtest=ClassName`. All new service tests live in `StarList-Backend/service/src/test/java/service/`.
- `custom_interval_days` valid values: 7, 14, or 30 only.
- `scheduled_day_of_week` uses ISO values: 1 = Monday … 7 = Sunday.
- Time ranges: MORNING 00–11, AFTERNOON 12–16, EVENING 17–22, CUSTOM = stored `scheduled_hour`.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `model/enums/ScheduledTimeType.java` | **Create** | New enum: MORNING, AFTERNOON, EVENING, CUSTOM |
| `model/domain/Habit.java` | **Modify** | Add 4 new optional fields |
| `repository/entity/HabitEntity.java` | **Modify** | Add 4 new JPA columns |
| `repository/mapper/HabitMapper.java` | **Modify** | MapStruct auto-maps same-name fields; add explicit `@Mapping` only if needed |
| `repository/api/HabitCompletionRepository.java` | **Modify** | Add `existsByHabit_IdAndCompletedDateBetween` |
| `service/dto/AddHabitRequest.java` | **Modify** | Add optional frequency-config fields |
| `service/dto/UpdateHabitRequest.java` | **Modify** | Add optional frequency-config fields |
| `service/dto/HabitResponse.java` | **Modify** | Add new fields + `streakUnit` |
| `service/HabitPeriodCalculator.java` | **Create** | All period-boundary date math (currentPeriod, isLateCompletion, periodsForMonth) |
| `service/HabitCompletionService.java` | **Modify** | Add `existsForPeriod(habitId, start, end)` |
| `service/HabitService.java` | **Modify** | Update `completeHabit` + refactor `buildMonthCompletions` |
| `service/src/test/.../HabitPeriodCalculatorTest.java` | **Create** | Unit tests for all period math |
| `service/src/test/.../HabitServiceTest.java` | **Modify** | Add WEEKLY/CUSTOM completion + monthCompletions tests |
| `frontend/src/services/habitsApi.ts` | **Modify** | Add new fields to request/response types |
| `frontend/src/app/components/AddHabitModal.tsx` | **Modify** | Multi-step frequency config form |
| `frontend/src/app/components/EditHabitModal.tsx` | **Modify** | Add frequency-config fields |
| `frontend/src/app/components/HabitTracker.tsx` | **Modify** | Segment count from array length; streak label from `streakUnit`; segment labels |

---

## Task 1: ScheduledTimeType enum + model/entity fields + mapper

**Files:**
- Create: `StarList-Backend/model/src/main/java/model/enums/ScheduledTimeType.java`
- Modify: `StarList-Backend/model/src/main/java/model/domain/Habit.java`
- Modify: `StarList-Backend/repository/src/main/java/repository/entity/HabitEntity.java`
- Modify: `StarList-Backend/repository/src/main/java/repository/mapper/HabitMapper.java`

**Interfaces:**
- Produces: `ScheduledTimeType` enum used by all later tasks; `Habit` and `HabitEntity` with fields `scheduledDayOfWeek`, `scheduledTimeType`, `scheduledHour`, `customIntervalDays`.

- [ ] **Step 1: Create the ScheduledTimeType enum**

```java
// StarList-Backend/model/src/main/java/model/enums/ScheduledTimeType.java
package model.enums;

public enum ScheduledTimeType {
    /** 00:00 – 11:59 */
    MORNING,
    /** 12:00 – 16:59 */
    AFTERNOON,
    /** 17:00 – 22:59 */
    EVENING,
    /** User-specified hour stored in scheduledHour (0–23). */
    CUSTOM
}
```

- [ ] **Step 2: Add fields to the Habit domain object**

Open `StarList-Backend/model/src/main/java/model/domain/Habit.java`. After the `frequency` field add:

```java
    /**
     * ISO day of week (1 = Monday … 7 = Sunday) on which the habit is intended to be done.
     * Required for WEEKLY and CUSTOM frequency; null for DAILY.
     */
    private Integer scheduledDayOfWeek;

    /**
     * Preferred time of day. Informational only — does not gate completion.
     * Present for WEEKLY and CUSTOM; null for DAILY.
     */
    private ScheduledTimeType scheduledTimeType;

    /**
     * Hour of day (0–23) when {@code scheduledTimeType} is {@link ScheduledTimeType#CUSTOM}.
     * Null otherwise.
     */
    private Integer scheduledHour;

    /**
     * Interval in days for CUSTOM frequency. Valid values: 7, 14, 30.
     * Null for DAILY and WEEKLY.
     */
    private Integer customIntervalDays;
```

Also add the import: `import model.enums.ScheduledTimeType;`

- [ ] **Step 3: Add JPA columns to HabitEntity**

Open `StarList-Backend/repository/src/main/java/repository/entity/HabitEntity.java`. After the `frequency` column add:

```java
    @Column(name = "scheduled_day_of_week")
    private Integer scheduledDayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduled_time_type", length = 16)
    private ScheduledTimeType scheduledTimeType;

    @Column(name = "scheduled_hour")
    private Integer scheduledHour;

    @Column(name = "custom_interval_days")
    private Integer customIntervalDays;
```

Also add the import: `import model.enums.ScheduledTimeType;`

- [ ] **Step 4: Verify HabitMapper needs no changes**

MapStruct auto-maps fields with identical names. Open `HabitMapper.java` and confirm the interface has no explicit exclusions for the new fields. If the build fails in Step 5 due to unmapped fields, add:

```java
// Only needed if MapStruct warns about unmapped target properties:
@Mapping(target = "scheduledDayOfWeek", source = "entity.scheduledDayOfWeek")
@Mapping(target = "scheduledTimeType",  source = "entity.scheduledTimeType")
@Mapping(target = "scheduledHour",      source = "entity.scheduledHour")
@Mapping(target = "customIntervalDays", source = "entity.customIntervalDays")
```

- [ ] **Step 5: Build and confirm it compiles**

```bash
./mvnw clean install -DskipTests
```

Expected: BUILD SUCCESS. Fix any MapStruct compilation errors before proceeding.

- [ ] **Step 6: Commit**

```bash
git add StarList-Backend/model/src/main/java/model/enums/ScheduledTimeType.java \
        StarList-Backend/model/src/main/java/model/domain/Habit.java \
        StarList-Backend/repository/src/main/java/repository/entity/HabitEntity.java \
        StarList-Backend/repository/src/main/java/repository/mapper/HabitMapper.java
git commit -m "feat(model): add ScheduledTimeType enum and frequency-config fields to Habit/HabitEntity"
```

---

## Task 2: Repository — period existence query

**Files:**
- Modify: `StarList-Backend/repository/src/main/java/repository/api/HabitCompletionRepository.java`

**Interfaces:**
- Produces: `boolean existsByHabit_IdAndCompletedDateBetween(Long habitId, LocalDate start, LocalDate end)` — used by `HabitCompletionService.existsForPeriod` in Task 5.

- [ ] **Step 1: Add the query method**

Open `HabitCompletionRepository.java` and add after the existing `existsByHabit_IdAndCompletedDate` method:

```java
    /**
     * Returns true if the habit has any completion record with a date
     * in the inclusive range [{@code start}, {@code end}].
     * Used for WEEKLY and CUSTOM period duplicate checks.
     */
    boolean existsByHabit_IdAndCompletedDateBetween(Long habitId, LocalDate start, LocalDate end);
```

- [ ] **Step 2: Build**

```bash
./mvnw clean install -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add StarList-Backend/repository/src/main/java/repository/api/HabitCompletionRepository.java
git commit -m "feat(repository): add existsByHabit_IdAndCompletedDateBetween for period duplicate check"
```

---

## Task 3: DTOs — frequency config fields + streakUnit

**Files:**
- Modify: `StarList-Backend/service/src/main/java/service/dto/AddHabitRequest.java`
- Modify: `StarList-Backend/service/src/main/java/service/dto/UpdateHabitRequest.java`
- Modify: `StarList-Backend/service/src/main/java/service/dto/HabitResponse.java`

**Interfaces:**
- Produces: Updated request/response DTOs used by `HabitService` (Task 6) and the controller (no controller changes needed — it already deserialises JSON into these records).

- [ ] **Step 1: Update AddHabitRequest**

Replace the full file content:

```java
package service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.ScheduledTimeType;

@Builder
public record AddHabitRequest(
        @NotBlank String title,
        String description,
        @NotNull HabitFrequency frequency,
        @NotNull DifficultyLevel difficultyLevel,

        /** Required when frequency is WEEKLY or CUSTOM. ISO day: 1 = Monday … 7 = Sunday. */
        @Min(1) @Max(7) Integer scheduledDayOfWeek,

        /** Optional for WEEKLY and CUSTOM. Informational only. */
        ScheduledTimeType scheduledTimeType,

        /** Required when scheduledTimeType is CUSTOM. */
        @Min(0) @Max(23) Integer scheduledHour,

        /** Required when frequency is CUSTOM. Must be one of: 7, 14, 30. */
        Integer customIntervalDays
) {}
```

- [ ] **Step 2: Update UpdateHabitRequest**

Replace the full file content:

```java
package service.dto;

import lombok.Builder;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import model.enums.ScheduledTimeType;

/**
 * Partial-update request for a habit. All fields are optional — only non-null fields are applied.
 * At least one field should be provided; an all-null body is a no-op.
 */
@Builder
public record UpdateHabitRequest(
        String title,
        String description,
        HabitFrequency frequency,
        DifficultyLevel difficultyLevel,
        Integer scheduledDayOfWeek,
        ScheduledTimeType scheduledTimeType,
        Integer scheduledHour,
        Integer customIntervalDays
) {}
```

- [ ] **Step 3: Update HabitResponse — add new fields and streakUnit**

In `HabitResponse.java`, add these fields to the record parameters (after `isActive`):

```java
        /** ISO day of week (1=Mon…7=Sun). Present for WEEKLY and CUSTOM; null for DAILY. */
        Integer scheduledDayOfWeek,
        ScheduledTimeType scheduledTimeType,
        Integer scheduledHour,
        Integer customIntervalDays,
        /** "day" for DAILY, "week" for WEEKLY, "period" for CUSTOM. */
        String streakUnit,
```

Add the import: `import model.enums.ScheduledTimeType;`

Update both `from` factory methods to include the new fields. The single-arg factory (for mutation responses):

```java
    public static HabitResponse from(Habit habit) {
        return HabitResponse.builder()
                .habitId(habit.getId())
                .title(habit.getTitle())
                .description(habit.getDescription())
                .frequency(habit.getFrequency())
                .difficultyLevel(habit.getDifficultyLevel())
                .coinReward(habit.getCoinReward())
                .coinPenalty(habit.getCoinPenalty())
                .currentStreak(habit.getCurrentStreak())
                .bestStreak(habit.getBestStreak())
                .totalCompletions(habit.getTotalCompletions())
                .lastCompletedDate(habit.getLastCompletedDate())
                .createdAt(habit.getCreatedAt())
                .isActive(habit.getIsActive())
                .scheduledDayOfWeek(habit.getScheduledDayOfWeek())
                .scheduledTimeType(habit.getScheduledTimeType())
                .scheduledHour(habit.getScheduledHour())
                .customIntervalDays(habit.getCustomIntervalDays())
                .streakUnit(streakUnitFor(habit.getFrequency()))
                .build();
    }
```

The two-arg factory:

```java
    public static HabitResponse from(Habit habit, List<CompletionStatus> monthCompletions) {
        return HabitResponse.builder()
                .habitId(habit.getId())
                .title(habit.getTitle())
                .description(habit.getDescription())
                .frequency(habit.getFrequency())
                .difficultyLevel(habit.getDifficultyLevel())
                .coinReward(habit.getCoinReward())
                .coinPenalty(habit.getCoinPenalty())
                .currentStreak(habit.getCurrentStreak())
                .bestStreak(habit.getBestStreak())
                .totalCompletions(habit.getTotalCompletions())
                .lastCompletedDate(habit.getLastCompletedDate())
                .createdAt(habit.getCreatedAt())
                .isActive(habit.getIsActive())
                .scheduledDayOfWeek(habit.getScheduledDayOfWeek())
                .scheduledTimeType(habit.getScheduledTimeType())
                .scheduledHour(habit.getScheduledHour())
                .customIntervalDays(habit.getCustomIntervalDays())
                .streakUnit(streakUnitFor(habit.getFrequency()))
                .monthCompletions(monthCompletions)
                .build();
    }
```

Add the private helper at the bottom of the record:

```java
    private static String streakUnitFor(HabitFrequency frequency) {
        if (frequency == null) return "day";
        return switch (frequency) {
            case WEEKLY -> "week";
            case CUSTOM -> "period";
            default -> "day";
        };
    }
```

Also add the import: `import model.enums.HabitFrequency;`

- [ ] **Step 4: Build**

```bash
./mvnw clean install -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add StarList-Backend/service/src/main/java/service/dto/
git commit -m "feat(dto): add frequency-config fields and streakUnit to habit DTOs"
```

---

## Task 4: HabitPeriodCalculator — period math service

**Files:**
- Create: `StarList-Backend/service/src/main/java/service/HabitPeriodCalculator.java`
- Create: `StarList-Backend/service/src/test/java/service/HabitPeriodCalculatorTest.java`

**Interfaces:**
- Produces:
  - `LocalDate[] currentPeriod(HabitEntity habit, LocalDate today)` — returns `[start, end]` for the current completion period.
  - `boolean isLateCompletion(HabitEntity habit, LocalDate today)` — true when a WEEKLY habit's scheduled day has already passed this week.
  - `List<LocalDate[]> periodsForMonth(HabitEntity habit, YearMonth yearMonth)` — returns `[start, end]` pairs for every expected completion slot in the month. Length of list = segment count for the circle.

- [ ] **Step 1: Write the failing tests first**

Create `StarList-Backend/service/src/test/java/service/HabitPeriodCalculatorTest.java`:

```java
package service;

import model.enums.HabitFrequency;
import org.junit.jupiter.api.Test;
import repository.entity.HabitEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HabitPeriodCalculatorTest {

    private final HabitPeriodCalculator calc = new HabitPeriodCalculator();

    // ── currentPeriod — DAILY ──────────────────────────────────────────────────

    @Test
    void currentPeriod_daily_returnsTodayToToday() {
        HabitEntity h = dailyHabit();
        LocalDate today = LocalDate.of(2026, 8, 15);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(today);
        assertThat(period[1]).isEqualTo(today);
    }

    // ── currentPeriod — WEEKLY ─────────────────────────────────────────────────

    @Test
    void currentPeriod_weekly_returnsMondayToSundayOfCurrentWeek() {
        // Aug 10 2026 is a Monday; Aug 16 is Sunday
        HabitEntity h = weeklyHabit(3); // Wednesday
        LocalDate wednesday = LocalDate.of(2026, 8, 12);

        LocalDate[] period = calc.currentPeriod(h, wednesday);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 10)); // Monday
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 16)); // Sunday
    }

    @Test
    void currentPeriod_weekly_onSundayReturnsCorrectWeek() {
        HabitEntity h = weeklyHabit(7); // Sunday
        LocalDate sunday = LocalDate.of(2026, 8, 16);

        LocalDate[] period = calc.currentPeriod(h, sunday);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 16));
    }

    // ── currentPeriod — CUSTOM ─────────────────────────────────────────────────

    @Test
    void currentPeriod_custom14days_period0() {
        // createdAt Aug 1; interval 14; today Aug 10 → period 0: Aug 1–14
        HabitEntity h = customHabit(14, LocalDate.of(2026, 8, 1));
        LocalDate today = LocalDate.of(2026, 8, 10);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    void currentPeriod_custom14days_period1() {
        // createdAt Aug 1; interval 14; today Aug 20 → period 1: Aug 15–28
        HabitEntity h = customHabit(14, LocalDate.of(2026, 8, 1));
        LocalDate today = LocalDate.of(2026, 8, 20);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    void currentPeriod_custom30days_onlyOnePeriodInMonth() {
        HabitEntity h = customHabit(30, LocalDate.of(2026, 8, 1));
        LocalDate today = LocalDate.of(2026, 8, 25);

        LocalDate[] period = calc.currentPeriod(h, today);

        assertThat(period[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(period[1]).isEqualTo(LocalDate.of(2026, 8, 30));
    }

    // ── isLateCompletion ───────────────────────────────────────────────────────

    @Test
    void isLateCompletion_daily_alwaysFalse() {
        HabitEntity h = dailyHabit();
        assertThat(calc.isLateCompletion(h, LocalDate.of(2026, 8, 15))).isFalse();
    }

    @Test
    void isLateCompletion_weekly_completedOnScheduledDay_false() {
        HabitEntity h = weeklyHabit(3); // Wednesday = ISO 3
        LocalDate wednesday = LocalDate.of(2026, 8, 12);
        assertThat(calc.isLateCompletion(h, wednesday)).isFalse();
    }

    @Test
    void isLateCompletion_weekly_completedBeforeScheduledDay_false() {
        HabitEntity h = weeklyHabit(5); // Friday = ISO 5
        LocalDate tuesday = LocalDate.of(2026, 8, 11);
        assertThat(calc.isLateCompletion(h, tuesday)).isFalse();
    }

    @Test
    void isLateCompletion_weekly_completedAfterScheduledDay_true() {
        HabitEntity h = weeklyHabit(3); // Wednesday = ISO 3
        LocalDate friday = LocalDate.of(2026, 8, 14); // ISO 5
        assertThat(calc.isLateCompletion(h, friday)).isTrue();
    }

    @Test
    void isLateCompletion_custom_alwaysFalse() {
        HabitEntity h = customHabit(14, LocalDate.of(2026, 8, 1));
        assertThat(calc.isLateCompletion(h, LocalDate.of(2026, 8, 20))).isFalse();
    }

    // ── periodsForMonth — DAILY ────────────────────────────────────────────────

    @Test
    void periodsForMonth_daily_returnsOnePeriodPerDay() {
        HabitEntity h = dailyHabit();
        YearMonth feb2026 = YearMonth.of(2026, 2);

        List<LocalDate[]> periods = calc.periodsForMonth(h, feb2026);

        assertThat(periods).hasSize(28);
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(periods.get(27)[0]).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    // ── periodsForMonth — WEEKLY ───────────────────────────────────────────────

    @Test
    void periodsForMonth_weekly_returnsOneEntryPerOccurrenceOfScheduledDayInMonth() {
        // Mondays in August 2026: 3, 10, 17, 24, 31 → 5 Mondays
        HabitEntity h = weeklyHabit(1); // Monday
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(5);
        // First Monday Aug 3 → period is the full ISO week: Jul 27 – Aug 2
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 8, 2));
        // Second Monday Aug 10 → Aug 4–10
        assertThat(periods.get(1)[0]).isEqualTo(LocalDate.of(2026, 8, 4));
        assertThat(periods.get(1)[1]).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void periodsForMonth_weekly_4occurrencesWhenScheduledDayNotInFirstWeek() {
        // Sundays in Feb 2026: 1, 8, 15, 22 → 4 Sundays
        HabitEntity h = weeklyHabit(7); // Sunday
        YearMonth feb2026 = YearMonth.of(2026, 2);

        List<LocalDate[]> periods = calc.periodsForMonth(h, feb2026);

        assertThat(periods).hasSize(4);
    }

    // ── periodsForMonth — CUSTOM ───────────────────────────────────────────────

    @Test
    void periodsForMonth_custom14days_returnsTwoPeriodsInAugust() {
        // createdAt Aug 1; interval 14 → periods: Aug 1–14, Aug 15–28
        HabitEntity h = customHabit(14, LocalDate.of(2026, 8, 1));
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(2);
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 8, 14));
        assertThat(periods.get(1)[0]).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(periods.get(1)[1]).isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    void periodsForMonth_custom30days_returnsOnePeriod() {
        HabitEntity h = customHabit(30, LocalDate.of(2026, 8, 1));
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(1);
        assertThat(periods.get(0)[0]).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(periods.get(0)[1]).isEqualTo(LocalDate.of(2026, 8, 30));
    }

    @Test
    void periodsForMonth_custom7days_returnsFourOrFivePeriods() {
        // createdAt Aug 1; interval 7 → Aug 1–7, Aug 8–14, Aug 15–21, Aug 22–28 (4 periods whose start is in Aug)
        HabitEntity h = customHabit(7, LocalDate.of(2026, 8, 1));
        YearMonth aug2026 = YearMonth.of(2026, 8);

        List<LocalDate[]> periods = calc.periodsForMonth(h, aug2026);

        assertThat(periods).hasSize(4);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HabitEntity dailyHabit() {
        return HabitEntity.builder()
                .id(1L)
                .frequency(HabitFrequency.DAILY)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
    }

    private HabitEntity weeklyHabit(int scheduledDayOfWeek) {
        return HabitEntity.builder()
                .id(1L)
                .frequency(HabitFrequency.WEEKLY)
                .scheduledDayOfWeek(scheduledDayOfWeek)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
    }

    private HabitEntity customHabit(int intervalDays, LocalDate createdDate) {
        return HabitEntity.builder()
                .id(1L)
                .frequency(HabitFrequency.CUSTOM)
                .customIntervalDays(intervalDays)
                .createdAt(createdDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();
    }
}
```

- [ ] **Step 2: Run the tests — verify they all fail**

```bash
./mvnw test -pl service -Dtest=HabitPeriodCalculatorTest
```

Expected: FAIL with `ClassNotFoundException: service.HabitPeriodCalculator`.

- [ ] **Step 3: Implement HabitPeriodCalculator**

Create `StarList-Backend/service/src/main/java/service/HabitPeriodCalculator.java`:

```java
package service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import model.enums.HabitFrequency;
import org.springframework.stereotype.Component;
import repository.entity.HabitEntity;

@Component
public class HabitPeriodCalculator {

    /**
     * Returns the start and end of the current completion period for the given habit.
     * <ul>
     *   <li>DAILY — {@code [today, today]}</li>
     *   <li>WEEKLY — {@code [Monday of today's ISO week, Sunday of today's ISO week]}</li>
     *   <li>CUSTOM — interval window anchored to the habit's {@code createdAt} date</li>
     * </ul>
     *
     * @return a two-element array {@code [periodStart, periodEnd]} (inclusive on both ends)
     */
    public LocalDate[] currentPeriod(HabitEntity habit, LocalDate today) {
        return switch (habit.getFrequency()) {
            case DAILY -> new LocalDate[]{today, today};
            case WEEKLY -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                yield new LocalDate[]{monday, monday.plusDays(6)};
            }
            case CUSTOM -> customPeriodContaining(habit, today);
        };
    }

    /**
     * Returns true if the habit is WEEKLY and today falls after the scheduled day of week
     * in the current ISO week — meaning the completion is "late" and incurs a penalty.
     * Always returns false for DAILY and CUSTOM habits.
     */
    public boolean isLateCompletion(HabitEntity habit, LocalDate today) {
        if (habit.getFrequency() != HabitFrequency.WEEKLY) return false;
        int todayIso = today.getDayOfWeek().getValue(); // 1=Mon … 7=Sun
        return todayIso > habit.getScheduledDayOfWeek();
    }

    /**
     * Returns a list of {@code [periodStart, periodEnd]} pairs for every expected completion
     * slot whose start date falls within {@code yearMonth}. The length of this list equals
     * the number of circle segments shown in the frontend.
     *
     * <ul>
     *   <li>DAILY — one pair per calendar day in the month</li>
     *   <li>WEEKLY — one pair per occurrence of {@code scheduledDayOfWeek} in the month;
     *       each pair spans the full ISO week containing that day</li>
     *   <li>CUSTOM — one pair per interval period whose start date falls in the month</li>
     * </ul>
     */
    public List<LocalDate[]> periodsForMonth(HabitEntity habit, YearMonth yearMonth) {
        return switch (habit.getFrequency()) {
            case DAILY -> dailyPeriodsForMonth(yearMonth);
            case WEEKLY -> weeklyPeriodsForMonth(habit, yearMonth);
            case CUSTOM -> customPeriodsForMonth(habit, yearMonth);
        };
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private LocalDate[] customPeriodContaining(HabitEntity habit, LocalDate today) {
        LocalDate anchor = createdDate(habit);
        long daysSince = ChronoUnit.DAYS.between(anchor, today);
        long periodIndex = daysSince / habit.getCustomIntervalDays();
        LocalDate start = anchor.plusDays(periodIndex * habit.getCustomIntervalDays());
        LocalDate end = start.plusDays(habit.getCustomIntervalDays() - 1);
        return new LocalDate[]{start, end};
    }

    private List<LocalDate[]> dailyPeriodsForMonth(YearMonth yearMonth) {
        List<LocalDate[]> result = new ArrayList<>(yearMonth.lengthOfMonth());
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate d = yearMonth.atDay(day);
            result.add(new LocalDate[]{d, d});
        }
        return result;
    }

    private List<LocalDate[]> weeklyPeriodsForMonth(HabitEntity habit, YearMonth yearMonth) {
        List<LocalDate[]> result = new ArrayList<>(5);
        DayOfWeek targetDay = DayOfWeek.of(habit.getScheduledDayOfWeek());
        // First occurrence of the scheduled day in the month
        LocalDate first = yearMonth.atDay(1).with(TemporalAdjusters.nextOrSame(targetDay));
        while (!first.isAfter(yearMonth.atEndOfMonth())) {
            LocalDate monday = first.with(DayOfWeek.MONDAY);
            result.add(new LocalDate[]{monday, monday.plusDays(6)});
            first = first.plusWeeks(1);
        }
        return result;
    }

    private List<LocalDate[]> customPeriodsForMonth(HabitEntity habit, YearMonth yearMonth) {
        List<LocalDate[]> result = new ArrayList<>(4);
        LocalDate anchor = createdDate(habit);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // Advance anchor by multiples of intervalDays until we reach periods in the month
        long intervalDays = habit.getCustomIntervalDays();
        long startIndex = 0;
        if (anchor.isBefore(monthStart)) {
            long daysBehind = ChronoUnit.DAYS.between(anchor, monthStart);
            startIndex = daysBehind / intervalDays;
        }

        LocalDate periodStart = anchor.plusDays(startIndex * intervalDays);
        while (!periodStart.isAfter(monthEnd)) {
            LocalDate periodEnd = periodStart.plusDays(intervalDays - 1);
            result.add(new LocalDate[]{periodStart, periodEnd});
            periodStart = periodStart.plusDays(intervalDays);
        }
        return result;
    }

    private LocalDate createdDate(HabitEntity habit) {
        Instant createdAt = habit.getCreatedAt() != null ? habit.getCreatedAt() : Instant.now();
        return createdAt.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
```

- [ ] **Step 4: Run the tests — verify they all pass**

```bash
./mvnw test -pl service -Dtest=HabitPeriodCalculatorTest
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add StarList-Backend/service/src/main/java/service/HabitPeriodCalculator.java \
        StarList-Backend/service/src/test/java/service/HabitPeriodCalculatorTest.java
git commit -m "feat(service): add HabitPeriodCalculator with period math for DAILY/WEEKLY/CUSTOM"
```

---

## Task 5: HabitCompletionService — existsForPeriod

**Files:**
- Modify: `StarList-Backend/service/src/main/java/service/HabitCompletionService.java`

**Interfaces:**
- Consumes: `HabitCompletionRepository.existsByHabit_IdAndCompletedDateBetween` (Task 2)
- Produces: `boolean existsForPeriod(Long habitId, LocalDate start, LocalDate end)` — used by `HabitService.completeHabit` (Task 6)

- [ ] **Step 1: Add the method**

In `HabitCompletionService.java`, add after `existsToday`:

```java
    /** Returns true if the habit has any completion record in the inclusive date range [{@code start}, {@code end}]. */
    public boolean existsForPeriod(Long habitId, LocalDate start, LocalDate end) {
        return habitCompletionRepository.existsByHabit_IdAndCompletedDateBetween(habitId, start, end);
    }
```

- [ ] **Step 2: Build**

```bash
./mvnw clean install -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add StarList-Backend/service/src/main/java/service/HabitCompletionService.java
git commit -m "feat(service): add HabitCompletionService.existsForPeriod for WEEKLY/CUSTOM duplicate check"
```

---

## Task 6: HabitService — completeHabit for WEEKLY/CUSTOM + validation

**Files:**
- Modify: `StarList-Backend/service/src/main/java/service/HabitService.java`
- Modify: `StarList-Backend/service/src/test/java/service/HabitServiceTest.java`

**Interfaces:**
- Consumes: `HabitPeriodCalculator.currentPeriod`, `HabitPeriodCalculator.isLateCompletion`, `HabitCompletionService.existsForPeriod`

- [ ] **Step 1: Inject HabitPeriodCalculator into HabitService**

In `HabitService.java`, add `HabitPeriodCalculator` as a constructor parameter and field (it's a `@Component` so Spring will inject it):

```java
    private final HabitPeriodCalculator habitPeriodCalculator;

    public HabitService(UserService userService, HabitCompletionService habitCompletionService,
                        CoinTransactionService coinTransactionService, AchievementService achievementService,
                        HabitRepository habitRepository, HabitMapper habitMapper, CoinCalculator coinCalculator,
                        HabitPeriodCalculator habitPeriodCalculator) {
        this.userService = userService;
        this.habitCompletionService = habitCompletionService;
        this.coinTransactionService = coinTransactionService;
        this.achievementService = achievementService;
        this.habitRepository = habitRepository;
        this.habitMapper = habitMapper;
        this.coinCalculator = coinCalculator;
        this.habitPeriodCalculator = habitPeriodCalculator;
    }
```

- [ ] **Step 2: Update addHabit to persist frequency-config fields**

In `addHabit`, update the `Habit.builder()` call to include the new fields:

```java
        Habit habit = Habit.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .frequency(request.frequency())
                .difficultyLevel(request.difficultyLevel())
                .scheduledDayOfWeek(request.scheduledDayOfWeek())
                .scheduledTimeType(request.scheduledTimeType())
                .scheduledHour(request.scheduledHour())
                .customIntervalDays(request.customIntervalDays())
                .coinReward(coins[0])
                .coinPenalty(coins[1])
                .isActive(true)
                .build();
```

Also add service-layer validation before the builder — reject invalid `customIntervalDays` and missing `scheduledDayOfWeek`:

```java
        validateFrequencyConfig(request.frequency(), request.scheduledDayOfWeek(), request.customIntervalDays());
```

Add the private validation method:

```java
    private void validateFrequencyConfig(HabitFrequency frequency, Integer scheduledDayOfWeek, Integer customIntervalDays) {
        if (frequency == HabitFrequency.WEEKLY || frequency == HabitFrequency.CUSTOM) {
            if (scheduledDayOfWeek == null || scheduledDayOfWeek < 1 || scheduledDayOfWeek > 7) {
                throw new IllegalArgumentException("scheduledDayOfWeek (1–7) is required for WEEKLY and CUSTOM habits");
            }
        }
        if (frequency == HabitFrequency.CUSTOM) {
            if (customIntervalDays == null || (customIntervalDays != 7 && customIntervalDays != 14 && customIntervalDays != 30)) {
                throw new IllegalArgumentException("customIntervalDays must be 7, 14, or 30");
            }
        }
    }
```

Add the import: `import model.enums.HabitFrequency;`

- [ ] **Step 3: Rewrite completeHabit for all frequency types**

Replace the body of `completeHabit` with:

```java
    @Transactional
    public MarkHabitDoneResponse completeHabit(Long habitId) {
        log.info("About to complete habit {}", habitId);

        HabitEntity entity = loadActiveHabit(habitId);
        LocalDate today = LocalDate.now();
        LocalDate[] period = habitPeriodCalculator.currentPeriod(entity, today);
        LocalDate periodStart = period[0];
        LocalDate periodEnd = period[1];

        boolean alreadyDone = entity.getFrequency() == HabitFrequency.DAILY
                ? habitCompletionService.existsToday(habitId)
                : habitCompletionService.existsForPeriod(habitId, periodStart, periodEnd);

        if (alreadyDone) {
            throw new HabitAlreadyCompletedTodayException(habitId);
        }

        boolean late = habitPeriodCalculator.isLateCompletion(entity, today);
        int oldBestStreak = entity.getBestStreak();
        int newStreak = isStreakContinued(entity, today, periodStart) ?
                entity.getCurrentStreak() + 1 : 1;
        int newBestStreak = Math.max(newStreak, oldBestStreak);

        int baseCoins = coinCalculator.computeHabitCompletionReward(entity.getDifficultyLevel(), newStreak);
        int penalty = late ? (entity.getCoinPenalty() != null ? entity.getCoinPenalty() : 0) : 0;
        int coinsEarned = Math.max(0, baseCoins - penalty);

        log.debug("Habit {} '{}' completed: streak {} -> {}, best={}, late={}, coins={}",
                habitId, entity.getTitle(), entity.getCurrentStreak(), newStreak, newBestStreak, late, coinsEarned);

        UserEntity user = entity.getUser();
        try {
            habitCompletionService.record(entity, user, today, coinsEarned, newStreak);
        } catch (DataIntegrityViolationException e) {
            throw new HabitAlreadyCompletedTodayException(habitId);
        }

        entity.setCurrentStreak(newStreak);
        entity.setBestStreak(newBestStreak);
        entity.setTotalCompletions(entity.getTotalCompletions() + 1);
        entity.setLastCompletedDate(today);
        habitRepository.save(entity);
        coinTransactionService.record(user, coinsEarned, TransactionType.HABIT_COMPLETION,
                ReferenceType.HABIT, habitId, "Completed habit: " + entity.getTitle());
        userService.addCoins(user, coinsEarned);
        achievementService.onHabitCompleted(user.getId(), newStreak, oldBestStreak, Instant.now());

        return MarkHabitDoneResponse.builder()
                .habitId(habitId)
                .coinsEarned(coinsEarned)
                .newTotalCoins(user.getTotalCoins())
                .currentStreak(newStreak)
                .bestStreak(newBestStreak)
                .build();
    }
```

Add the private streak-continuation helper:

```java
    /**
     * Returns true if the user's last completion falls within the period immediately preceding the current one.
     *
     * <ul>
     *   <li>DAILY — last completed yesterday</li>
     *   <li>WEEKLY — last completed anywhere in the previous ISO week (i.e. between Monday-7days and periodStart-1)</li>
     *   <li>CUSTOM — last completed within the period that ends one day before the current period starts</li>
     * </ul>
     */
    private boolean isStreakContinued(HabitEntity entity, LocalDate today, LocalDate currentPeriodStart) {
        LocalDate last = entity.getLastCompletedDate();
        if (last == null) return false;
        return switch (entity.getFrequency()) {
            case DAILY -> today.minusDays(1).equals(last);
            case WEEKLY -> {
                LocalDate prevWeekStart = currentPeriodStart.minusWeeks(1);
                LocalDate prevWeekEnd = currentPeriodStart.minusDays(1);
                yield !last.isBefore(prevWeekStart) && !last.isAfter(prevWeekEnd);
            }
            case CUSTOM -> {
                LocalDate prevPeriodEnd = currentPeriodStart.minusDays(1);
                LocalDate prevPeriodStart = prevPeriodEnd.minusDays(entity.getCustomIntervalDays() - 1);
                yield !last.isBefore(prevPeriodStart) && !last.isAfter(prevPeriodEnd);
            }
        };
    }
```

- [ ] **Step 4: Write new tests in HabitServiceTest**

Add these test methods to `HabitServiceTest.java`. Add `@Mock HabitPeriodCalculator habitPeriodCalculator;` to the mock list, and `@Mock` it. Update existing tests that construct `HabitService` via `@InjectMocks` — `@InjectMocks` picks up the new mock automatically.

```java
    @Mock HabitPeriodCalculator habitPeriodCalculator;

    // ── completeHabit WEEKLY — on time ────────────────────────────────────────

    @Test
    void completeHabit_weekly_onTime_fullReward() {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(10L).title("Weekly run").difficultyLevel(DifficultyLevel.MEDIUM)
                .frequency(HabitFrequency.WEEKLY).scheduledDayOfWeek(5) // Friday
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null).user(user).build();

        when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(10L, periodStart, periodEnd)).thenReturn(false);
        when(habitPeriodCalculator.isLateCompletion(habit, today)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 1)).thenReturn(20);

        MarkHabitDoneResponse response = habitService.completeHabit(10L);

        assertThat(response.coinsEarned()).isEqualTo(20); // no penalty
        assertThat(response.currentStreak()).isEqualTo(1);
    }

    @Test
    void completeHabit_weekly_late_penaltyDeducted() {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(10L).title("Weekly run").difficultyLevel(DifficultyLevel.MEDIUM)
                .frequency(HabitFrequency.WEEKLY).scheduledDayOfWeek(3) // Wednesday
                .coinPenalty(10)
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null).user(user).build();

        when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(10L, periodStart, periodEnd)).thenReturn(false);
        when(habitPeriodCalculator.isLateCompletion(habit, today)).thenReturn(true);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 1)).thenReturn(20);

        MarkHabitDoneResponse response = habitService.completeHabit(10L);

        assertThat(response.coinsEarned()).isEqualTo(10); // 20 reward - 10 penalty
        assertThat(response.currentStreak()).isEqualTo(1); // streak still counts
    }

    @Test
    void completeHabit_weekly_alreadyDoneThisPeriod_throws() {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        HabitEntity habit = HabitEntity.builder()
                .id(10L).frequency(HabitFrequency.WEEKLY).currentStreak(1).bestStreak(1).totalCompletions(1).build();

        when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(10L, periodStart, periodEnd)).thenReturn(true);

        assertThatThrownBy(() -> habitService.completeHabit(10L))
                .isInstanceOf(HabitAlreadyCompletedTodayException.class);
        verifyNoInteractions(coinTransactionService, userService);
    }

    @Test
    void completeHabit_custom_withinPeriod_allowed() {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = periodStart.plusDays(29);

        UserEntity user = UserEntity.builder().id(1L).totalCoins(0).lifetimeCoinsEarned(0).build();
        HabitEntity habit = HabitEntity.builder()
                .id(20L).title("Monthly review").difficultyLevel(DifficultyLevel.HARD)
                .frequency(HabitFrequency.CUSTOM).customIntervalDays(30)
                .currentStreak(0).bestStreak(0).totalCompletions(0)
                .lastCompletedDate(null).user(user).build();

        when(habitRepository.findById(20L)).thenReturn(Optional.of(habit));
        when(habitPeriodCalculator.currentPeriod(habit, today)).thenReturn(new LocalDate[]{periodStart, periodEnd});
        when(habitCompletionService.existsForPeriod(20L, periodStart, periodEnd)).thenReturn(false);
        when(habitPeriodCalculator.isLateCompletion(habit, today)).thenReturn(false);
        when(coinCalculator.computeHabitCompletionReward(DifficultyLevel.HARD, 1)).thenReturn(40);

        MarkHabitDoneResponse response = habitService.completeHabit(20L);

        assertThat(response.coinsEarned()).isEqualTo(40);
        assertThat(response.currentStreak()).isEqualTo(1);
    }
```

- [ ] **Step 5: Run all service tests**

```bash
./mvnw test -pl service
```

Expected: ALL tests PASS (including the pre-existing DAILY tests — they now route through `existsToday` as before).

- [ ] **Step 6: Commit**

```bash
git add StarList-Backend/service/src/main/java/service/HabitService.java \
        StarList-Backend/service/src/test/java/service/HabitServiceTest.java
git commit -m "feat(service): update completeHabit for WEEKLY/CUSTOM periods and late-completion penalty"
```

---

## Task 7: HabitService — refactor buildMonthCompletions

**Files:**
- Modify: `StarList-Backend/service/src/main/java/service/HabitService.java`
- Modify: `StarList-Backend/service/src/test/java/service/HabitServiceTest.java`

**Interfaces:**
- Consumes: `HabitPeriodCalculator.periodsForMonth`

- [ ] **Step 1: Refactor buildMonthCompletions**

Replace the existing `buildMonthCompletions` private method in `HabitService.java`:

```java
    /**
     * Builds a per-period {@link CompletionStatus} list for the given month.
     *
     * <p>For DAILY habits the list has one entry per calendar day (same as before).
     * For WEEKLY and CUSTOM habits the list has one entry per expected completion period
     * in the month — this is the segment count rendered by the frontend circle.
     *
     * <ul>
     *   <li>{@code DONE} — the period's end is in the past or today, on/after {@code habitCreatedDate},
     *   and any completion date falls within [periodStart, periodEnd].</li>
     *   <li>{@code MISSED} — the period has ended, on/after {@code habitCreatedDate}, and no completion
     *   exists in [periodStart, periodEnd].</li>
     *   <li>{@code NA} — the period hasn't ended yet, OR the period is before the habit was created.</li>
     * </ul>
     */
    private List<CompletionStatus> buildMonthCompletions(
            Set<LocalDate> completedDates, YearMonth yearMonth,
            LocalDate habitCreatedDate, HabitEntity entity) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<LocalDate[]> periods = habitPeriodCalculator.periodsForMonth(entity, yearMonth);
        List<CompletionStatus> result = new ArrayList<>(periods.size());

        for (LocalDate[] period : periods) {
            LocalDate periodStart = period[0];
            LocalDate periodEnd = period[1];

            // Periods entirely before the habit was created → NA
            if (periodEnd.isBefore(habitCreatedDate)) {
                result.add(CompletionStatus.NA);
                continue;
            }

            boolean done = completedDates.stream()
                    .anyMatch(d -> !d.isBefore(periodStart) && !d.isAfter(periodEnd));

            if (done) {
                result.add(CompletionStatus.DONE);
            } else if (periodEnd.isBefore(today)) {
                // Period fully in the past with no completion → MISSED
                result.add(CompletionStatus.MISSED);
            } else {
                // Period is current or future → NA
                result.add(CompletionStatus.NA);
            }
        }
        return result;
    }
```

- [ ] **Step 2: Update all callers of buildMonthCompletions**

There are two callers in `HabitService`: `getHabit` and `getUserHabits`. Both must pass the entity. 

In `getHabit`, change:
```java
        return HabitResponse.from(habit,
                buildMonthCompletions(
                        completionsMap.getOrDefault(habitId, Collections.emptySet()),
                        yearMonth,
                        habitCreatedDate));
```
to:
```java
        HabitEntity entity = loadActiveHabit(habitId);
        return HabitResponse.from(habit,
                buildMonthCompletions(
                        completionsMap.getOrDefault(habitId, Collections.emptySet()),
                        yearMonth,
                        habitCreatedDate,
                        entity));
```

Note: `loadActiveHabit` is already called earlier in `getHabit` to get the habit; reuse that reference by storing it:
```java
        HabitEntity entity = loadActiveHabit(habitId);
        Habit habit = habitMapper.toDomain(entity);
        // ... rest unchanged, pass entity to buildMonthCompletions
```

In `getUserHabits`, the entity loop already has `entity` available:
```java
        return entities.stream()
                .map(entity -> {
                    Habit habit = habitMapper.toDomain(entity);
                    LocalDate habitCreatedDate = habit.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                    return HabitResponse.from(habit,
                            buildMonthCompletions(
                                    completionsMap.getOrDefault(habit.getId(), Collections.emptySet()),
                                    yearMonth,
                                    habitCreatedDate,
                                    entity));
                })
                .toList();
```

- [ ] **Step 3: Add tests for WEEKLY and CUSTOM monthCompletions**

Add to `HabitServiceTest.java`:

```java
    // ── getHabit / monthCompletions — WEEKLY ──────────────────────────────────

    @Test
    void getHabit_weekly_completedThisWeek_markedDone() {
        YearMonth aug2026 = YearMonth.of(2026, 8);
        Instant createdAt = LocalDate.of(2026, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate completedDate = LocalDate.of(2026, 8, 12); // Wednesday Aug 12

        HabitEntity entity = HabitEntity.builder()
                .id(1L).frequency(HabitFrequency.WEEKLY).scheduledDayOfWeek(3).build();
        Habit habit = Habit.builder().id(1L).frequency(HabitFrequency.WEEKLY)
                .scheduledDayOfWeek(3).createdAt(createdAt).build();

        // 5 Mondays in August 2026: Aug 3, 10, 17, 24, 31
        List<LocalDate[]> fivePeriods = List.of(
                new LocalDate[]{LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2)},
                new LocalDate[]{LocalDate.of(2026, 8, 3),  LocalDate.of(2026, 8, 9)},
                new LocalDate[]{LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16)},
                new LocalDate[]{LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23)},
                new LocalDate[]{LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 30)}
        );

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), aug2026))
                .thenReturn(Map.of(1L, Set.of(completedDate)));
        when(habitPeriodCalculator.periodsForMonth(entity, aug2026)).thenReturn(fivePeriods);

        HabitResponse response = habitService.getHabit(1L, aug2026);

        assertThat(response.monthCompletions()).hasSize(5);
        // completedDate Aug 12 falls in period Aug 10–16 → index 2
        assertThat(response.monthCompletions().get(2)).isEqualTo(CompletionStatus.DONE);
    }

    @Test
    void getHabit_custom30days_onePeriod_notDone_markedNA_whenCurrentPeriod() {
        YearMonth aug2026 = YearMonth.of(2026, 8);
        Instant createdAt = LocalDate.of(2026, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        HabitEntity entity = HabitEntity.builder()
                .id(1L).frequency(HabitFrequency.CUSTOM).customIntervalDays(30).build();
        Habit habit = Habit.builder().id(1L).frequency(HabitFrequency.CUSTOM)
                .customIntervalDays(30).createdAt(createdAt).build();

        // One period in August: Aug 1–30
        List<LocalDate[]> onePeriod = List.of(
                new LocalDate[]{LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30)}
        );

        when(habitRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(habitMapper.toDomain(entity)).thenReturn(habit);
        when(habitCompletionService.getCompletedDatesForHabits(List.of(1L), aug2026))
                .thenReturn(Map.of());
        when(habitPeriodCalculator.periodsForMonth(entity, aug2026)).thenReturn(onePeriod);

        HabitResponse response = habitService.getHabit(1L, aug2026);

        assertThat(response.monthCompletions()).hasSize(1);
        // Period end Aug 30 is not before today (still in the future or today) → NA
        assertThat(response.monthCompletions().get(0)).isIn(CompletionStatus.NA, CompletionStatus.MISSED);
    }
```

- [ ] **Step 4: Run all service tests**

```bash
./mvnw test -pl service
```

Expected: ALL tests PASS, including the pre-existing DAILY `monthCompletions` tests.

> **Note:** The pre-existing `getHabit_arrayLength_matchesMonthDays` test asserts `hasSize(28)` for February — it will now route through `habitPeriodCalculator.periodsForMonth`. You must add a mock expectation for `periodsForMonth` in that test. Add this mock setup to that test and any other `getHabit_*` test that uses a DAILY habit:
> ```java
> List<LocalDate[]> febPeriods = IntStream.rangeClosed(1, 28)
>     .mapToObj(d -> new LocalDate[]{LocalDate.of(2026, 2, d), LocalDate.of(2026, 2, d)})
>     .collect(Collectors.toList());
> when(habitPeriodCalculator.periodsForMonth(entity, feb2026)).thenReturn(febPeriods);
> ```
> Apply a similar `when(habitPeriodCalculator.periodsForMonth(...))` stub to every other `getHabit_*` and `getUserHabits_*` test that currently exists. The `HabitPeriodCalculatorTest` already proves the daily math is correct — these mocks just satisfy Mockito.

- [ ] **Step 5: Commit**

```bash
git add StarList-Backend/service/src/main/java/service/HabitService.java \
        StarList-Backend/service/src/test/java/service/HabitServiceTest.java
git commit -m "feat(service): refactor buildMonthCompletions to support WEEKLY/CUSTOM period segments"
```

---

## Task 8: Frontend — API types

**Files:**
- Modify: `frontend/src/services/habitsApi.ts`

**Interfaces:**
- Produces: Updated TypeScript types used by AddHabitModal, EditHabitModal, HabitTracker (Tasks 9–10).

- [ ] **Step 1: Update habitsApi.ts**

Replace the file with:

```typescript
import api from './api';

export type HabitFrequency = 'DAILY' | 'WEEKLY' | 'CUSTOM';
export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';
export type ScheduledTimeType = 'MORNING' | 'AFTERNOON' | 'EVENING' | 'CUSTOM';
export type StreakUnit = 'day' | 'week' | 'period';

export interface HabitResponse {
    habitId: number;
    title: string;
    description: string | null;
    frequency: HabitFrequency;
    difficultyLevel: DifficultyLevel;
    coinReward: number;
    coinPenalty: number | null;
    currentStreak: number;
    bestStreak: number;
    totalCompletions: number;
    lastCompletedDate: string | null;
    createdAt: string;
    isActive: boolean;
    monthCompletions?: string[];
    // Frequency config — null for DAILY
    scheduledDayOfWeek: number | null;  // 1=Mon … 7=Sun
    scheduledTimeType: ScheduledTimeType | null;
    scheduledHour: number | null;
    customIntervalDays: number | null;
    streakUnit: StreakUnit;
}

export interface FrequencyConfig {
    scheduledDayOfWeek?: number;      // Required for WEEKLY and CUSTOM
    scheduledTimeType?: ScheduledTimeType;
    scheduledHour?: number;           // Required when scheduledTimeType = 'CUSTOM'
    customIntervalDays?: 7 | 14 | 30; // Required for CUSTOM
}

export interface AddHabitRequest {
    title: string;
    description?: string;
    frequency: HabitFrequency;
    difficultyLevel: DifficultyLevel;
    scheduledDayOfWeek?: number;
    scheduledTimeType?: ScheduledTimeType;
    scheduledHour?: number;
    customIntervalDays?: number;
}

export interface UpdateHabitRequest {
    title?: string;
    description?: string;
    frequency?: HabitFrequency;
    difficultyLevel?: DifficultyLevel;
    scheduledDayOfWeek?: number;
    scheduledTimeType?: ScheduledTimeType;
    scheduledHour?: number;
    customIntervalDays?: number;
}

export interface MarkHabitDoneResponse {
    habitId: number;
    coinsEarned: number;
    newTotalCoins: number;
    currentStreak: number;
    bestStreak: number;
}

export const habitsApi = {
    getHabits: async (year?: number, month?: number): Promise<HabitResponse[]> => {
        const params = year && month ? { year, month } : {};
        const response = await api.get('/habits', { params });
        return response.data.map((h: any) => ({
            ...h,
            habitId: h.habitId || h.id
        }));
    },
    createHabit: async (habit: AddHabitRequest): Promise<HabitResponse> => {
        const response = await api.post('/habits', habit);
        const h = response.data;
        return { ...h, habitId: h.habitId || h.id };
    },
    completeHabit: async (habitId: number): Promise<MarkHabitDoneResponse> => {
        const response = await api.post(`/habits/${habitId}/complete`);
        return response.data;
    },
    updateHabit: async (habitId: number, habit: UpdateHabitRequest): Promise<HabitResponse> => {
        const response = await api.put(`/habits/${habitId}`, habit);
        const h = response.data;
        return { ...h, habitId: h.habitId || h.id };
    },
    deleteHabit: async (habitId: number): Promise<void> => {
        await api.delete(`/habits/${habitId}`);
    }
};
```

- [ ] **Step 2: Confirm TypeScript compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/services/habitsApi.ts
git commit -m "feat(frontend): update habitsApi types for frequency config and streakUnit"
```

---

## Task 9: Frontend — frequency config UI in AddHabitModal and EditHabitModal

**Files:**
- Modify: `frontend/src/app/components/AddHabitModal.tsx`
- Modify: `frontend/src/app/components/EditHabitModal.tsx`

**Interfaces:**
- Consumes: `AddHabitRequest`, `UpdateHabitRequest`, `FrequencyConfig`, `ScheduledTimeType` from `habitsApi.ts`

- [ ] **Step 1: Replace AddHabitModal with multi-step frequency form**

Replace the full file content of `AddHabitModal.tsx`:

```tsx
import { useState } from "react";
import { X, Flame, Plus } from "lucide-react";
import { AddHabitRequest, DifficultyLevel, HabitFrequency, ScheduledTimeType } from "@/services/habitsApi.ts";

interface AddHabitModalProps {
    isOpen: boolean;
    onClose: () => void;
    onAdd: (habitData: AddHabitRequest) => void;
}

const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export function AddHabitModal({ isOpen, onClose, onAdd }: AddHabitModalProps) {
    const [title, setTitle] = useState("");
    const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM');
    const [frequency, setFrequency] = useState<HabitFrequency>('DAILY');
    const [scheduledDay, setScheduledDay] = useState<number>(1); // 1=Mon
    const [timeType, setTimeType] = useState<ScheduledTimeType>('MORNING');
    const [customHour, setCustomHour] = useState<number>(9);
    const [intervalDays, setIntervalDays] = useState<7 | 14 | 30>(7);

    if (!isOpen) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        const request: AddHabitRequest = {
            title,
            difficultyLevel: difficulty,
            frequency,
            ...(frequency !== 'DAILY' && {
                scheduledDayOfWeek: scheduledDay,
                scheduledTimeType: timeType,
                ...(timeType === 'CUSTOM' && { scheduledHour: customHour }),
            }),
            ...(frequency === 'CUSTOM' && { customIntervalDays: intervalDays }),
        };

        onAdd(request);
        resetForm();
    };

    const resetForm = () => {
        setTitle("");
        setDifficulty("MEDIUM");
        setFrequency("DAILY");
        setScheduledDay(1);
        setTimeType("MORNING");
        setCustomHour(9);
        setIntervalDays(7);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
                <div className="flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800/50">
                    <h2 className="text-lg text-white font-medium flex items-center gap-2">
                        <Flame className="w-5 h-5 text-orange-400" />
                        New Habit
                    </h2>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-5 space-y-4">
                    {/* Title */}
                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Habit Title</label>
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="e.g., Morning run"
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                            required
                        />
                    </div>

                    {/* Difficulty */}
                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Difficulty</label>
                        <select
                            value={difficulty}
                            onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                        >
                            <option value="EASY">Easy</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HARD">Hard</option>
                        </select>
                    </div>

                    {/* Frequency type */}
                    <div>
                        <label className="block text-sm text-slate-300 mb-2">Frequency</label>
                        <div className="grid grid-cols-3 gap-2">
                            {(['DAILY', 'WEEKLY', 'CUSTOM'] as HabitFrequency[]).map((f) => (
                                <button
                                    key={f}
                                    type="button"
                                    onClick={() => setFrequency(f)}
                                    className={`py-2 rounded-lg text-sm font-medium transition-colors ${
                                        frequency === f
                                            ? 'bg-indigo-600 text-white'
                                            : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                    }`}
                                >
                                    {f.charAt(0) + f.slice(1).toLowerCase()}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Weekly & Custom: day of week */}
                    {frequency !== 'DAILY' && (
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">Scheduled Day</label>
                            <div className="grid grid-cols-7 gap-1">
                                {DAY_NAMES.map((name, i) => (
                                    <button
                                        key={i}
                                        type="button"
                                        onClick={() => setScheduledDay(i + 1)}
                                        className={`py-1.5 rounded text-xs font-medium transition-colors ${
                                            scheduledDay === i + 1
                                                ? 'bg-indigo-600 text-white'
                                                : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                        }`}
                                    >
                                        {name}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Custom: interval */}
                    {frequency === 'CUSTOM' && (
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">Repeat Every</label>
                            <div className="grid grid-cols-3 gap-2">
                                {([7, 14, 30] as const).map((days) => (
                                    <button
                                        key={days}
                                        type="button"
                                        onClick={() => setIntervalDays(days)}
                                        className={`py-2 rounded-lg text-sm font-medium transition-colors ${
                                            intervalDays === days
                                                ? 'bg-indigo-600 text-white'
                                                : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                        }`}
                                    >
                                        {days === 7 ? 'Every week' : days === 14 ? 'Every 2 weeks' : 'Every month'}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Weekly & Custom: time of day */}
                    {frequency !== 'DAILY' && (
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">Time of Day</label>
                            <div className="grid grid-cols-2 gap-2">
                                {(['MORNING', 'AFTERNOON', 'EVENING', 'CUSTOM'] as ScheduledTimeType[]).map((t) => (
                                    <button
                                        key={t}
                                        type="button"
                                        onClick={() => setTimeType(t)}
                                        className={`py-2 rounded-lg text-xs font-medium transition-colors ${
                                            timeType === t
                                                ? 'bg-indigo-600 text-white'
                                                : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                        }`}
                                    >
                                        {t === 'MORNING' ? '🌅 Morning (until 12)' :
                                         t === 'AFTERNOON' ? '☀️ Afternoon (12–17)' :
                                         t === 'EVENING' ? '🌙 Evening (17–23)' :
                                         '🕐 Custom hour'}
                                    </button>
                                ))}
                            </div>
                            {timeType === 'CUSTOM' && (
                                <div className="mt-2 flex items-center gap-2">
                                    <input
                                        type="number"
                                        min={0}
                                        max={23}
                                        value={customHour}
                                        onChange={(e) => setCustomHour(parseInt(e.target.value))}
                                        className="w-20 bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
                                    />
                                    <span className="text-slate-400 text-sm">:00</span>
                                </div>
                            )}
                        </div>
                    )}

                    <div className="pt-4 flex gap-3">
                        <button type="button" onClick={onClose} className="flex-1 py-2 rounded-lg font-medium bg-slate-700 hover:bg-slate-600 text-white transition-colors">
                            Cancel
                        </button>
                        <button type="submit" className="flex-1 py-2 rounded-lg font-medium bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2 transition-colors">
                            <Plus className="w-4 h-4" />
                            Add Habit
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
```

- [ ] **Step 2: Update EditHabitModal with the same frequency-config UI**

Replace the full file content of `EditHabitModal.tsx`:

```tsx
import { useState, useEffect } from "react";
import { X, Edit2, Check } from "lucide-react";
import {
    HabitResponse, UpdateHabitRequest, DifficultyLevel,
    HabitFrequency, ScheduledTimeType
} from "@/services/habitsApi.ts";

interface EditHabitModalProps {
    isOpen: boolean;
    onClose: () => void;
    habitToEdit: HabitResponse | null;
    onUpdate: (habitId: number, habitData: UpdateHabitRequest) => void;
}

const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export function EditHabitModal({ isOpen, onClose, habitToEdit, onUpdate }: EditHabitModalProps) {
    const [title, setTitle] = useState("");
    const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM');
    const [frequency, setFrequency] = useState<HabitFrequency>('DAILY');
    const [scheduledDay, setScheduledDay] = useState<number>(1);
    const [timeType, setTimeType] = useState<ScheduledTimeType>('MORNING');
    const [customHour, setCustomHour] = useState<number>(9);
    const [intervalDays, setIntervalDays] = useState<7 | 14 | 30>(7);

    useEffect(() => {
        if (habitToEdit) {
            setTitle(habitToEdit.title);
            setDifficulty(habitToEdit.difficultyLevel);
            setFrequency(habitToEdit.frequency);
            setScheduledDay(habitToEdit.scheduledDayOfWeek ?? 1);
            setTimeType(habitToEdit.scheduledTimeType ?? 'MORNING');
            setCustomHour(habitToEdit.scheduledHour ?? 9);
            setIntervalDays((habitToEdit.customIntervalDays as 7 | 14 | 30) ?? 7);
        }
    }, [habitToEdit]);

    if (!isOpen || !habitToEdit) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        const request: UpdateHabitRequest = {
            title,
            difficultyLevel: difficulty,
            frequency,
            ...(frequency !== 'DAILY' && {
                scheduledDayOfWeek: scheduledDay,
                scheduledTimeType: timeType,
                ...(timeType === 'CUSTOM' && { scheduledHour: customHour }),
            }),
            ...(frequency === 'CUSTOM' && { customIntervalDays: intervalDays }),
        };

        onUpdate(habitToEdit.habitId, request);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
                <div className="flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800/50">
                    <h2 className="text-lg text-white font-medium flex items-center gap-2">
                        <Edit2 className="w-5 h-5 text-blue-400" />
                        Edit Habit
                    </h2>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-5 space-y-4">
                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Habit Title</label>
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Difficulty</label>
                        <select
                            value={difficulty}
                            onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                        >
                            <option value="EASY">Easy</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HARD">Hard</option>
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm text-slate-300 mb-2">Frequency</label>
                        <div className="grid grid-cols-3 gap-2">
                            {(['DAILY', 'WEEKLY', 'CUSTOM'] as HabitFrequency[]).map((f) => (
                                <button
                                    key={f}
                                    type="button"
                                    onClick={() => setFrequency(f)}
                                    className={`py-2 rounded-lg text-sm font-medium transition-colors ${
                                        frequency === f
                                            ? 'bg-indigo-600 text-white'
                                            : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                    }`}
                                >
                                    {f.charAt(0) + f.slice(1).toLowerCase()}
                                </button>
                            ))}
                        </div>
                    </div>

                    {frequency !== 'DAILY' && (
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">Scheduled Day</label>
                            <div className="grid grid-cols-7 gap-1">
                                {DAY_NAMES.map((name, i) => (
                                    <button
                                        key={i}
                                        type="button"
                                        onClick={() => setScheduledDay(i + 1)}
                                        className={`py-1.5 rounded text-xs font-medium transition-colors ${
                                            scheduledDay === i + 1
                                                ? 'bg-indigo-600 text-white'
                                                : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                        }`}
                                    >
                                        {name}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {frequency === 'CUSTOM' && (
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">Repeat Every</label>
                            <div className="grid grid-cols-3 gap-2">
                                {([7, 14, 30] as const).map((days) => (
                                    <button
                                        key={days}
                                        type="button"
                                        onClick={() => setIntervalDays(days)}
                                        className={`py-2 rounded-lg text-sm font-medium transition-colors ${
                                            intervalDays === days
                                                ? 'bg-indigo-600 text-white'
                                                : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                        }`}
                                    >
                                        {days === 7 ? 'Every week' : days === 14 ? 'Every 2 weeks' : 'Every month'}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {frequency !== 'DAILY' && (
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">Time of Day</label>
                            <div className="grid grid-cols-2 gap-2">
                                {(['MORNING', 'AFTERNOON', 'EVENING', 'CUSTOM'] as ScheduledTimeType[]).map((t) => (
                                    <button
                                        key={t}
                                        type="button"
                                        onClick={() => setTimeType(t)}
                                        className={`py-2 rounded-lg text-xs font-medium transition-colors ${
                                            timeType === t
                                                ? 'bg-indigo-600 text-white'
                                                : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white border border-slate-700'
                                        }`}
                                    >
                                        {t === 'MORNING' ? '🌅 Morning (until 12)' :
                                         t === 'AFTERNOON' ? '☀️ Afternoon (12–17)' :
                                         t === 'EVENING' ? '🌙 Evening (17–23)' :
                                         '🕐 Custom hour'}
                                    </button>
                                ))}
                            </div>
                            {timeType === 'CUSTOM' && (
                                <div className="mt-2 flex items-center gap-2">
                                    <input
                                        type="number"
                                        min={0}
                                        max={23}
                                        value={customHour}
                                        onChange={(e) => setCustomHour(parseInt(e.target.value))}
                                        className="w-20 bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
                                    />
                                    <span className="text-slate-400 text-sm">:00</span>
                                </div>
                            )}
                        </div>
                    )}

                    <div className="pt-4 flex gap-3">
                        <button type="button" onClick={onClose} className="flex-1 py-2 rounded-lg font-medium bg-slate-700 hover:bg-slate-600 text-white transition-colors">
                            Cancel
                        </button>
                        <button type="submit" className="flex-1 py-2 rounded-lg font-medium bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2 transition-colors">
                            <Check className="w-4 h-4" />
                            Save Changes
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
```

- [ ] **Step 3: Confirm TypeScript compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/components/AddHabitModal.tsx \
        frontend/src/app/components/EditHabitModal.tsx
git commit -m "feat(frontend): add multi-step frequency config UI to Add/Edit habit modals"
```

---

## Task 10: Frontend — HabitTracker circle and streak label

**Files:**
- Modify: `frontend/src/app/components/HabitTracker.tsx`

**Interfaces:**
- Consumes: `HabitResponse.monthCompletions`, `HabitResponse.streakUnit`, `HabitResponse.scheduledDayOfWeek`, `HabitResponse.frequency`

- [ ] **Step 1: Update HabitRadialCard to use monthCompletions.length for segment count**

The current implementation hard-codes `daysInMonth` as the segment count and rebuilds streak logic on the frontend. After this task, the segment count comes directly from `monthCompletions.length` (the backend controls this), and streak label uses `streakUnit`.

Replace the `HabitRadialCard` function in `HabitTracker.tsx`:

```tsx
function HabitRadialCard({ habit, onCheck, onEdit, onDelete, index }: {
    habit: HabitResponse; onCheck: () => void; onEdit: () => void; onDelete: () => void; index: number
}) {
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    const isCompletedToday = Boolean(habit.lastCompletedDate && habit.lastCompletedDate.startsWith(todayStr));

    // monthCompletions drives the segment count — length varies by frequency.
    const completions = habit.monthCompletions ?? [];
    const segmentCount = completions.length;

    const radius = 90;
    const center = 120;
    const circumference = 2 * Math.PI * radius;
    const segmentLength = segmentCount > 0 ? circumference / segmentCount : circumference;
    const gap = segmentCount > 10 ? 4 : segmentCount > 4 ? 6 : 10; // wider gaps for few segments
    const strokeWidth = segmentCount > 10 ? 12 : 16; // thicker arcs for few segments

    const colors = [
        { stroke: 'text-blue-400', glow: 'rgba(96, 165, 250, 0.5)', bg: 'bg-blue-400' },
        { stroke: 'text-purple-400', glow: 'rgba(192, 132, 252, 0.5)', bg: 'bg-purple-400' },
        { stroke: 'text-emerald-400', glow: 'rgba(52, 211, 153, 0.5)', bg: 'bg-emerald-400' },
        { stroke: 'text-orange-400', glow: 'rgba(251, 146, 60, 0.5)', bg: 'bg-orange-400' },
    ];
    const color = colors[index % colors.length];

    const streakLabel = `${habit.currentStreak} ${habit.streakUnit ?? 'day'} streak`;

    // Segment label: for DAILY show day numbers; for WEEKLY/CUSTOM show segment index
    const getSegmentLabel = (i: number): string => {
        if (habit.frequency === 'DAILY') return String(i + 1);
        if (habit.frequency === 'WEEKLY') {
            // Show the date of the scheduled day in that week
            // We approximate: find the i-th occurrence of scheduledDayOfWeek in the current month
            const now = new Date();
            const target = habit.scheduledDayOfWeek ?? 1; // 1=Mon ISO
            let count = 0;
            for (let d = 1; d <= new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate(); d++) {
                const dow = new Date(now.getFullYear(), now.getMonth(), d).getDay(); // 0=Sun
                const iso = dow === 0 ? 7 : dow; // convert to ISO 1=Mon..7=Sun
                if (iso === target) {
                    if (count === i) return String(d);
                    count++;
                }
            }
            return String(i + 1);
        }
        // CUSTOM: show period number
        return String(i + 1);
    };

    return (
        <div className="bg-slate-800/40 border border-slate-700/50 rounded-[2.5rem] p-8 flex flex-col items-center group hover:bg-slate-800/60 transition-all duration-300 relative overflow-hidden">

            <div className="absolute top-6 left-6 flex gap-2 z-20 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                    onClick={(e) => { e.stopPropagation(); onEdit(); }}
                    className="p-1.5 bg-slate-700/50 hover:bg-slate-600 rounded-lg text-slate-300 hover:text-white transition-colors"
                    title="Edit Habit"
                >
                    <Edit2 className="w-4 h-4" />
                </button>
                <button
                    onClick={(e) => { e.stopPropagation(); onDelete(); }}
                    className="p-1.5 bg-red-900/30 hover:bg-red-500/80 rounded-lg text-red-400 hover:text-white transition-colors"
                    title="Delete Habit"
                >
                    <Trash2 className="w-4 h-4" />
                </button>
            </div>

            <div className="text-center mb-6 z-10">
                <h3 className="text-xl font-bold text-white group-hover:text-blue-200 transition-colors">{habit.title}</h3>
                <div className="flex items-center justify-center gap-1.5 text-orange-400 text-sm mt-1 font-medium">
                    <Flame className="w-4 h-4 fill-orange-400/20" />
                    <span>{streakLabel}</span>
                </div>
            </div>

            <div className="relative w-64 h-64 flex items-center justify-center z-10 my-4">
                <svg viewBox="0 0 240 240" className="w-full h-full transform -rotate-90 drop-shadow-2xl">
                    {completions.map((status, i) => {
                        const isDone = status === 'DONE';
                        const isMissed = status === 'MISSED';
                        const segActualLength = segmentLength - gap;
                        return (
                            <circle
                                key={i}
                                cx={center}
                                cy={center}
                                r={radius}
                                fill="transparent"
                                stroke="currentColor"
                                strokeWidth={strokeWidth}
                                strokeDasharray={`${segActualLength} ${circumference - segActualLength}`}
                                strokeDashoffset={-(i * segmentLength)}
                                className={isDone ? color.stroke : isMissed ? 'text-red-900/60' : 'text-slate-700'}
                                style={isDone ? { filter: `drop-shadow(0 0 6px ${color.glow})` } : undefined}
                            />
                        );
                    })}
                </svg>

                {/* Segment labels — unrotated */}
                <svg viewBox="0 0 240 240" className="w-full h-full absolute inset-0 pointer-events-none">
                    {completions.map((status, i) => {
                        const theta = (i + 0.5) / segmentCount * 2 * Math.PI;
                        const x = center + radius * Math.sin(theta);
                        const y = center - radius * Math.cos(theta);
                        const isDone = status === 'DONE';
                        const isNA = status === 'NA';
                        return (
                            <text
                                key={i}
                                x={x}
                                y={y}
                                textAnchor="middle"
                                dominantBaseline="central"
                                fill={isDone ? 'white' : isNA ? '#1e293b' : '#475569'}
                                fontSize={segmentCount > 20 ? "7" : "9"}
                                fontWeight={isDone ? '700' : '400'}
                            >
                                {getSegmentLabel(i)}
                            </text>
                        );
                    })}
                </svg>

                <div className="absolute inset-0 flex items-center justify-center">
                    <button
                        onClick={(e) => { e.stopPropagation(); onCheck(); }}
                        disabled={isCompletedToday}
                        className={`w-32 h-32 rounded-full flex flex-col items-center justify-center transition-all duration-500 relative overflow-hidden ${
                            isCompletedToday
                                ? 'bg-transparent text-green-400 cursor-default'
                                : 'bg-slate-900 text-white hover:scale-105 border border-slate-700 hover:border-slate-500 shadow-2xl'
                        }`}
                    >
                        {isCompletedToday ? (
                            <>
                                <Check className="w-12 h-12 animate-in zoom-in" />
                                <span className="text-[11px] mt-2 uppercase font-black tracking-tighter">Logged</span>
                            </>
                        ) : (
                            <>
                                <Plus className="w-12 h-12 group-hover:rotate-90 transition-transform duration-300" />
                                <span className="text-[11px] mt-2 uppercase font-black tracking-tighter">Complete</span>
                            </>
                        )}
                    </button>
                </div>
            </div>

            <div className="absolute top-6 right-8 flex items-center gap-1 opacity-40">
                <span className="text-xs font-bold text-yellow-500">+{habit.coinReward}</span>
            </div>

            <div className={`absolute -bottom-10 -left-10 w-40 h-40 rounded-full blur-[80px] opacity-10 ${color.bg}`} />
        </div>
    );
}
```

- [ ] **Step 2: Confirm TypeScript compiles**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Spot-check visually**

Start the app and open the Habits page. Verify:
- A DAILY habit shows ~28–31 segments with day numbers.
- A WEEKLY habit (after creating one via the new modal) shows 4–5 segments.
- A CUSTOM / 30-day habit shows 1 segment.
- Streak label shows "X day streak", "X week streak", or "X period streak" appropriately.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/components/HabitTracker.tsx
git commit -m "feat(frontend): adapt habit circle to monthCompletions length; add frequency-aware streak label"
```

---

## Final Checklist

- [ ] `./mvnw clean install` passes (all modules, all tests)
- [ ] DAILY habits behave exactly as before — no regression
- [ ] Creating a WEEKLY habit with `scheduledDayOfWeek` missing returns 400
- [ ] Creating a CUSTOM habit with `customIntervalDays = 60` returns 400
- [ ] Completing a WEEKLY habit late deducts the penalty (net ≥ 0)
- [ ] Completing a WEEKLY habit twice in the same week throws `HabitAlreadyCompletedTodayException`
- [ ] CUSTOM habit once-a-month shows 1 segment in the circle
- [ ] WEEKLY habit shows 4–5 segments depending on the month
- [ ] Streak label uses the correct unit per frequency type
- [ ] Add/Edit modal shows frequency-config fields only when WEEKLY or CUSTOM is selected
