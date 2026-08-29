package service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
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
            List<LocalDate[]> expiredPeriods = resolveExpiredPeriods(habit, today, yesterday);
            for (LocalDate[] expiredPeriod : expiredPeriods) {

                boolean wasCompleted = habitCompletionRepository
                        .existsByHabit_IdAndCompletedDateBetween(habit.getId(), expiredPeriod[0], expiredPeriod[1]);

                if (!wasCompleted) {
                    applyMissPenalty(habit, expiredPeriod);
                    penaltiesApplied++;
                }
            }
        }

        log.info("HabitMissScheduler finished: {} miss penalties applied out of {} habits", penaltiesApplied, habits.size());
    }

    // ── private helpers ────────────────────────────────────────────────────────

    /**
     * Returns all periods that expired as of {@code today} for this habit.
     * Most frequencies return zero or one period; MULTI_DAY can return multiple
     * (one per scheduled day that falls on yesterday, which is at most one in practice,
     * but the list contract keeps the call-site loop simple).
     */
    private List<LocalDate[]> resolveExpiredPeriods(HabitEntity habit, LocalDate today, LocalDate yesterday) {
        return switch (habit.getFrequency()) {
            // DAILY: yesterday's single-day slot always just expired.
            case DAILY -> {
                List<LocalDate[]> r = new ArrayList<>();
                r.add(new LocalDate[]{yesterday, yesterday});
                yield r;
            }

            // WEEKLY: the ISO week expires the moment Monday begins.
            // Only check on Mondays to avoid re-penalising on Tue–Sun.
            case WEEKLY -> {
                if (today.getDayOfWeek() != DayOfWeek.MONDAY) yield List.<LocalDate[]>of();
                LocalDate lastMonday = today.minusDays(7);
                LocalDate lastSunday = yesterday; // always Sunday when today is Monday
                List<LocalDate[]> r = new ArrayList<>();
                r.add(new LocalDate[]{lastMonday, lastSunday});
                yield r;
            }

            // CUSTOM: check if the period containing yesterday ended yesterday.
            case CUSTOM -> {
                LocalDate[] period = habitPeriodCalculator.currentPeriod(habit, yesterday);
                if (period != null && period[1].equals(yesterday)) {
                    List<LocalDate[]> r = new ArrayList<>();
                    r.add(period);
                    yield r;
                }
                yield List.<LocalDate[]>of();
            }

            // MULTI_DAY: each selected weekday is an independent single-day slot.
            // If yesterday was one of the scheduled days, that slot just expired.
            case MULTI_DAY -> {
                List<Integer> days = habit.getScheduledDaysOfWeek();
                int yesterdayIso = yesterday.getDayOfWeek().getValue();
                if (days != null && days.contains(yesterdayIso)) {
                    List<LocalDate[]> r = new ArrayList<>();
                    r.add(new LocalDate[]{yesterday, yesterday});
                    yield r;
                }
                yield List.<LocalDate[]>of();
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
