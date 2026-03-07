package model.enums;

public enum DifficultyLevel {
    NONE(0),
    EASY(1),
    MEDIUM(2),
    HARD(3),
    VERY_HARD(4),
    EXTREME(5);

    private final int level;

    DifficultyLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

