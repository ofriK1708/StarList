import api from './api';

export interface AchievementResponse {
    id: string;
    name: string;
    icon: string;
    description: string;
    unlocked: boolean;
    unlockedAt: string | null;
}

export const achievementsApi = {
    getMyAchievements: async (): Promise<AchievementResponse[]> => {
        const response = await api.get<AchievementResponse[]>('/achievements');
        return response.data;
    },
};
