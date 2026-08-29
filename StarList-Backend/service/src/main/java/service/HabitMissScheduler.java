package service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import repository.api.HabitCompletionRepository;
import repository.api.HabitRepository;
import repository.entity.HabitEntity;

/**
 * Runs once per day (00:05 UTC) and deducts coins for every habit period that expired
 * without a completion.
 *
 * <p>No {@code HabitCompletion} record is created on a miss — only a {@link TransactionType#HABIT_MISS}
 * coin transaction is written and the user's coin balance is reduced.
 *
 * <p>Per-frequency expiry rules:
 * <ul>
 *   <li>DAILY  — checks yesterday's single-day period (runs every day)</li>
 *   <li>WEEKLY — checks last week's ISO week (runs only on Mondays)</li>
 *   <li>CUSTOM — checks the period whose end date was yesterday (runs only on period-boundary days)</li>
 * </ul>
 */
@Slf4j
@Component
public class HabitMissScheduler {

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final HabitPeriodCalculator habitPeriodCalculator;
    private final CoinTransactionService coinTransactionService;
    private final UserService userService;

    public HabitMissScheduler(HabitRepository habitRepository,
                               HabitCompletionRepository habitCompletionRepository,
                               HabitPeriodCalculator habitPeriodCalculator,
                               CoinTransactionService coinTransactionService,
                               UserService userService) {
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.habitPeriodCalculator = habitPeriodCalculator;
        this.coinTransactionService = coinTransactionService;
        this.userService = userService;
    }

    /**
     * Cron: {@code 0 5 0 * * *} = every day at 00:05 UTC.
     * All time arithmetic uses UTC so the boundary never drifts with DST.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    @Transactional
    public void processMisses() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        log.info("HabitMissScheduler running for date boundary: yesterday={}, today={}", yesterday, today);

        List<HabitEntity> habits = habitRepository.findAllByDeletedAtIsNull();
        log.debug("Processing {} active habits", habits.size());

        int penaltiesApplied = 0;
        for (HabitEntity habit : habits) {
            LocalDate[] expiredPeriod = resolveExpiredPeriod(habit, today, yesterday);
            if (expiredPeriod == null) {
                // No period expired for this frequency/day combination — skip silently
                continue;
            }

            boolean wasCompleted = habitCompletionRepository
                    .existsByHabit_IdAndCompletedDateBetween(habit.getId(), expiredPeriod[0], expiredPeriod[1]);

            if (!wasCompleted) {
                applyMissPenalty(habit, expiredPeriod);
                penaltiesApplied++;
            }
        }

        log.info("HabitMissScheduler finished: {} miss penalties applied out of {} habits", penaltiesApplied, habits.size());
    }

    // ── private helpers ────────────────────────────────────────────────────────

    /**
     * Returns the period {@code [start, end]} that just expired as of {@code today},
     * or {@code null} if no period expired for this habit on this day.
     *
     * <p>The scheduler calls this for every habit on every run. Returning {@code null}
     * is the signal to skip — it is not an error.
     */
    private LocalDate[] resolveExpiredPeriod(HabitEntity habit, LocalDate today, LocalDate yesterday) {
        return switch (habit.getFrequency()) {
            // DAILY: a new period starts every day, so yesterday's single-day period always just expired.
            case DAILY -> new LocalDate[]{yesterday, yesterday};

            // WEEKLY: the ISO week runs Mon→Sun. A week expires the moment Monday begins.
            // We only check on Mondays to avoid re-penalising on Tue–Sun of the same week.
            case WEEKLY -> {
                if (today.getDayOfWeek() != DayOfWeek.MONDAY) yield null;
                LocalDate lastMonday = today.minusDays(7);
                LocalDate lastSunday = yesterday; // yesterday is always Sunday when today is Monday
                yield new LocalDate[]{lastMonday, lastSunday};
            }

            // CUSTOM: intervals are anchored to the habit's creation date.
            // We ask: did the period containing yesterday end yesterday?
            // If yes, that period just expired. If no, yesterday was mid-period — skip.
            case CUSTOM -> {
                LocalDate[] period = habitPeriodCalculator.currentPeriod(habit, yesterday);
                yield period[1].equals(yesterday) ? period : null;
            }
        };
    }

    /**
     * Deducts {@code coinPenalty} coins from the habit owner and records a
     * {@link TransactionType#HABIT_MISS} transaction. Skips habits with no penalty configured.
     */
    private void applyMissPenalty(HabitEntity habit, LocalDate[] expiredPeriod) {
        Integer penalty = habit.getCoinPenalty();
        if (penalty == null || penalty <= 0) {
            log.debug("Habit {} has no penalty configured — skipping miss deduction", habit.getId());
            return;
        }

        log.info("Applying miss penalty: habit={} '{}', period=[{},{}], penalty={}",
                habit.getId(), habit.getTitle(), expiredPeriod[0], expiredPeriod[1], penalty);

        coinTransactionService.record(
                habit.getUser(),
                -penalty,
                TransactionType.HABIT_MISS,
                ReferenceType.HABIT,
                habit.getId(),
                "Missed habit: " + habit.getTitle());

        // spendCoins deducts from totalCoins only; lifetimeCoinsEarned is never reduced by penalties.
        userService.spendCoins(habit.getUser(), penalty);
    }
}
