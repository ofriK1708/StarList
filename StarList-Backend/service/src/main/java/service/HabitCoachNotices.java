package service;

import java.time.LocalDate;
import java.util.Optional;
import service.dto.HabitPeriodStatus;
import service.dto.HabitResponse;

/**
 * Decides whether a habit deserves a nudge in the AI assistant's system prompt, and what it says.
 *
 * <p>Deliberately a pure static function with no Spring wiring: {@code AiService} cannot be
 * instantiated without an OpenAI client, so keeping this policy separate is what makes it testable.
 *
 * <p>The reminder cadence is: once when the period opens, the day before the habit is due, the day
 * it is due, and then every day after the due date until the period closes.
 */
final class HabitCoachNotices {

    /**
     * Days past due before the wording escalates. Deliberately not 1 — a single missed day gets a
     * plain statement of fact, so the assistant does not scold the user over one slip.
     */
    private static final int ESCALATE_AFTER_DAYS = 3;

    private HabitCoachNotices() {
    }

    /**
     * Returns the notice for one habit, or empty when it should stay quiet.
     *
     * <p>Stay quiet when the habit has no period status, is not scheduled today, has already been
     * completed this period, is inactive, or is simply mid-period with nothing due soon. That last
     * case matters: without it a 30-day CUSTOM habit emits a notice every single day and floods
     * the prompt.
     *
     * <p>Callers rely on this contract, and {@code HabitCoachNoticesTest} enforces it:
     * <ul>
     *   <li>every returned notice contains the habit's title</li>
     *   <li>an overdue notice contains the number of days late</li>
     *   <li>a due-today notice contains "today"; a due-tomorrow notice contains "tomorrow"</li>
     *   <li>wording escalates — a habit 3 days late must not read identically to one 1 day late</li>
     * </ul>
     * Wording is otherwise free; it sets the assistant's tone.
     *
     * @param habit the habit, whose {@link HabitPeriodStatus} carries all the dates already computed
     * @param today the user's local date, matching the zone the period status was computed in
     * @return the notice text, or {@link Optional#empty()} to say nothing about this habit
     */
    static Optional<String> forHabit(HabitResponse habit, LocalDate today) {
        HabitPeriodStatus status = habit.periodStatus();

        // Only an explicit false silences a habit: if isActive somehow arrives null we would rather
        // over-remind than go silently quiet and hide the bug.
        if (status == null
                || !status.scheduledToday()
                || status.completedThisPeriod()
                || Boolean.FALSE.equals(habit.isActive())) {
            return Optional.empty();
        }

        String title = habit.title();
        String streak = " (streak: " + habit.currentStreak() + ")";
        int daysLate = status.daysLate();

        if (daysLate > 0) {
            String late = daysLate + (daysLate == 1 ? " day" : " days");

            // Most urgent first: the period closes tonight, so this is the final chance to log it.
            if (today.equals(status.periodEnd())) {
                return Optional.of("Last chance on \"" + title + "\" — it was due " + late
                        + " ago and this period ends today. Logging it now still counts." + streak);
            }
            if (daysLate >= ESCALATE_AFTER_DAYS) {
                return Optional.of("\"" + title + "\" is now " + late + " past due. Completing it late "
                        + "restarts the streak, but it still counts — worth picking back up." + streak);
            }
            return Optional.of("\"" + title + "\" is " + late + " past due. Completing it late "
                    + "will restart the streak." + streak);
        }

        if (status.daysUntilDue() == 0) {
            return Optional.of("\"" + title + "\" is due today." + streak);
        }
        if (status.daysUntilDue() == 1) {
            return Optional.of("\"" + title + "\" is due tomorrow." + streak);
        }
        if (today.equals(status.periodStart())) {
            return Optional.of("New period started for \"" + title + "\" — due in "
                    + status.daysUntilDue() + " days." + streak);
        }

        // Mid-period with nothing due soon. Staying quiet here is what stops a 30-day CUSTOM habit
        // from emitting a notice every single day and flooding the prompt.
        return Optional.empty();
    }
}
