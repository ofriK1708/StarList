import api from './api';

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';
export type TaskStatus = 'PENDING' | 'COMPLETED' | 'DELETED';

export interface TaskResponse {
    taskId: number;
    title: string;
    description: string;
    difficultyLevel: DifficultyLevel;
    durationMinutes: number | null;
    coinReward: number;
    coinPenalty: number | null;
    status: TaskStatus;
    dueDate: string | null;
    createdAt: string;
}

export interface AddTaskRequest {
    title: string;
    description?: string;
    difficultyLevel: DifficultyLevel;
    durationMinutes?: number;
    dueDate?: string | null;
}

/** Partial-update payload — only the provided fields are applied. */
export interface UpdateTaskRequest {
    title?: string;
    description?: string;
    difficultyLevel?: DifficultyLevel;
    durationMinutes?: number;
    dueDate?: string | null;
}

export interface MarkTaskDoneResponse {
    taskId: number;
    coinsEarned: number;
    newTotalCoins: number;
}

export const tasksApi = {
    getTasks: async (): Promise<TaskResponse[]> => {
        const response = await api.get('/tasks');
        return response.data;
    },
    createTask: async (task: AddTaskRequest): Promise<TaskResponse> => {
        const response = await api.post('/tasks', task);
        return response.data;
    },
    updateTask: async (taskId: number, task: UpdateTaskRequest): Promise<TaskResponse> => {
        const response = await api.put(`/tasks/${taskId}`, task);
        return response.data;
    },
    completeTask: async (taskId: number): Promise<MarkTaskDoneResponse> => {
        const response = await api.post(`/tasks/${taskId}/complete`);
        return response.data;
    },
    deleteTask: async (taskId: number): Promise<void> => {
        await api.delete(`/tasks/${taskId}`);
    }
};