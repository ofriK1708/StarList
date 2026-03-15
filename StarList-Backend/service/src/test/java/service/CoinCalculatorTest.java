package service;

import model.enums.DifficultyLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CoinCalculatorTest {

    // Real object — no mocks needed, this class has zero dependencies
    private final CoinCalculator coinCalculator = new CoinCalculator();

    // ── computeBaseCoins ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → reward={1}, penalty={2}")
    @CsvSource({
        "NONE,       0,   0",
        "EASY,      10,   5",
        "MEDIUM,    25,  10",
        "HARD,      50,  25",
        "VERY_HARD,100,  50",
        "EXTREME,  200, 100"
    })
    void computeBaseCoins_returnsCorrectRewardAndPenalty(DifficultyLevel level, int expectedReward, int expectedPenalty) {
        int[] result = coinCalculator.computeBaseCoins(level);

        assertThat(result[0]).as("reward").isEqualTo(expectedReward);
        assertThat(result[1]).as("penalty").isEqualTo(expectedPenalty);
    }

    // ── computeHabitCompletionReward ──────────────────────────────────────────

    @Test
    void computeHabitCompletionReward_streak1_noMultiplier() {
        // EASY base = 10, streak < 7 → 1.0x → 10
        assertThat(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 1)).isEqualTo(10);
    }

    @Test
    void computeHabitCompletionReward_streak6_stillNoMultiplier() {
        // boundary: 6 is still < 7 → 1.0x
        assertThat(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 6)).isEqualTo(25);
    }

    @Test
    void computeHabitCompletionReward_streak7_onePointTwoFiveMultiplier() {
        // EASY base = 10, streak 7–13 → 1.25x → 12.5 → rounds to 13
        assertThat(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 7)).isEqualTo(13);
    }

    @Test
    void computeHabitCompletionReward_streak13_stillOnePointTwoFive() {
        // boundary: 13 is still < 14 → 1.25x → 25 * 1.25 = 31.25 → rounds to 31
        assertThat(coinCalculator.computeHabitCompletionReward(DifficultyLevel.MEDIUM, 13)).isEqualTo(31);
    }

    @Test
    void computeHabitCompletionReward_streak14_onePointFiveMultiplier() {
        // EASY base = 10, streak ≥ 14 → 1.5x → 15
        assertThat(coinCalculator.computeHabitCompletionReward(DifficultyLevel.EASY, 14)).isEqualTo(15);
    }
}
