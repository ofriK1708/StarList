package service;

import model.enums.DifficultyLevel;
import org.springframework.stereotype.Component;

@Component
public class CoinCalculator {

    /**
     * Returns the base {@code [reward, penalty]} for a given difficulty level,
     * with no streak bonus applied.
     */
    public int[] computeBaseCoins(DifficultyLevel level) {
        return switch (level) {
            case NONE -> new int[]{0, 0};
            case EASY -> new int[]{10, 5};
            case MEDIUM -> new int[]{25, 10};
            case HARD -> new int[]{50, 25};
            case VERY_HARD -> new int[]{100, 50};
            case EXTREME -> new int[]{200, 100};
        };
    }

    /**
     * Returns the streak-boosted coin reward for completing a habit.
     * The penalty is not affected by streaks.
     *
     * <p>Multiplier tiers:
     * <ul>
     *   <li>streak &lt; 7  → 1.0×</li>
     *   <li>streak &lt; 14 → 1.25×</li>
     *   <li>streak ≥ 14 → 1.5×</li>
     * </ul>
     */
    public int computeHabitCompletionReward(DifficultyLevel level, int streak) {
        int baseReward = computeBaseCoins(level)[0];
        double multiplier = streak >= 14 ? 1.5 : streak >= 7 ? 1.25 : 1.0;
        return (int) Math.round(baseReward * multiplier);
    }
}
