import api from './api';

export type HabitFrequency = 'DAILY' | 'WEEKLY' | 'CUSTOM' | 'MULTI_DAY';
export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';
/** Informational time-of-day preference; does not gate completion. */
export type ScheduledTimeType = 'MORNING' | 'AFTERNOON' | 'EVENING' | 'CUSTOM';
/** Unit used for streak display: "day" (DAILY), "week" (WEEKLY), "period" (CUSTOM). */
export type StreakUnit = 'day' | 'week' | 'period';

/**
 * Frequency-config fields shared by AddHabitRequest and UpdateHabitRequest.
 * All fields are optional at the type level; backend validation enforces
 * which are required depending on the chosen frequency.
 */
export interface FrequencyConfig {
    /** ISO day-of-week (1=Mon…7=Sun). Required for WEEKLY and CUSTOM. */
    scheduledDayOfWeek?: number;
    /** Time-of-day preference (informational only). */
    scheduledTimeType?: ScheduledTimeType;
    /** Hour (0–23) when scheduledTimeType is CUSTOM. */
    scheduledHour?: number;
    /** Interval in days (7, 14, or 30). Required for CUSTOM. */
    customIntervalDays?: number;
    /** ISO days of week (1=Mon…7=Sun); 2–6 values. Required for MULTI_DAY. */
    scheduledDaysOfWeek?: number[];
}

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
    /** ISO day-of-week (1=Mon…7=Sun). Present for WEEKLY and CUSTOM. */
    scheduledDayOfWeek: number | null;
    scheduledTimeType: ScheduledTimeType | null;
    /** Hour (0–23) when scheduledTimeType is CUSTOM. */
    scheduledHour: number | null;
    /** Interval in days (7, 14, or 30). Present for CUSTOM. */
    customIntervalDays: number | null;
    /** ISO days of week (1=Mon…7=Sun). Present for MULTI_DAY. */
    scheduledDaysOfWeek: number[] | null;
    /** Unit for streak display. */
    streakUnit: StreakUnit;
    /**
     * Per-period completion statuses for the requested month.
     * Length equals the number of radial segments (days for DAILY,
     * scheduled-day occurrences for WEEKLY, interval windows for CUSTOM).
     */
    monthCompletions?: Array<'DONE' | 'MISSED' | 'NA'>;
}

export interface AddHabitRequest extends FrequencyConfig {
    title: string;
    description?: string;
    frequency: HabitFrequency;
    difficultyLevel: DifficultyLevel;
}

export interface MarkHabitDoneResponse {
    habitId: number;
    coinsEarned: number;
    newTotalCoins: number;
    currentStreak: number;
    bestStreak: number;
}

export interface UpdateHabitRequest extends FrequencyConfig {
    title?: string;
    description?: string;
    frequency?: HabitFrequency;
    difficultyLevel?: DifficultyLevel;
}

/**
 * The browser's IANA timezone (e.g. "Asia/Jerusalem"). The backend uses it to decide which
 * calendar day a completion lands on, and therefore whether the habit was completed late.
 */
const userTimezone = (): string | undefined => {
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || undefined;
    } catch {
        return undefined; // backend falls back to UTC
    }
};

export const habitsApi = {
    getHabits: async (year?: number, month?: number): Promise<HabitResponse[]> => {
        const params = {
            ...(year && month ? { year, month } : {}),
            userTimezone: userTimezone(),
        };
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
        const response = await api.post(`/habits/${habitId}/complete`, null, {
            params: { userTimezone: userTimezone() },
        });
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