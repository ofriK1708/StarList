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
        // TODO(ofri): implement the notice policy described above.
        return Optional.empty();
    }
}
