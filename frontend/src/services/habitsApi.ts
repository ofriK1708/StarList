import api from './api';

export type HabitFrequency = 'DAILY' | 'WEEKLY' | 'CUSTOM';
export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';

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
}

export interface AddHabitRequest {
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

export interface UpdateHabitRequest {
    title?: string;
    description?: string;
    frequency?: HabitFrequency;
    difficultyLevel?: DifficultyLevel;
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