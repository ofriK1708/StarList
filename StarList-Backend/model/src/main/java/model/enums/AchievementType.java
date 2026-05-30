package model.enums;

public enum AchievementType {
    STARTER("Starter", "🌟", "Welcome to StarList!"),
    ARCHITECT("Architect", "📐", "Created your first 10 habits or tasks"),
    WEEK_WARRIOR("Week Warrior", "🔥", "Maintained a 7-day active streak"),
    UNSTOPPABLE("Unstoppable", "⚡", "Reached a 30-day active streak"),
    STREAK_CENTURION("The Centurion", "🛡️", "Hit a 100-day active streak"),
    PHOENIX("Phoenix", "🐦‍🔥", "Built a new streak immediately after breaking a previous one"),
    TASK_MASTER("Task Master", "🏆", "Completed 50 total tasks"),
    EARLY_BIRD("Early Bird", "🌅", "Completed a task before 8:00 AM"),
    NIGHT_OWL("Night Owl", "🦉", "Completed a task after 10:00 PM"),
    CLEAN_SWEEP("Clean Sweep", "🧹", "Completed 100% of scheduled daily items");

    private final String displayName;
    private final String icon;
    private final String description;

    AchievementType(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
}
