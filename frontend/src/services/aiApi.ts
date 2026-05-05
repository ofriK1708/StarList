import api from './api';

export type ConversationType = 'TASK_CREATION' | 'STUDY_PLAN' | 'HABIT_SUGGESTION' | 'GENERAL_CHAT';

export interface AiChatRequest {
    message: string;
    newConversation?: boolean;
}

export interface AiChatResponse {
    conversationId: number;
    aiMessage: string;
    conversationType: ConversationType;
    tasksCreated: number;
    createdAt: string;
}

export interface AiConversationHistoryItem {
    conversationId: number;
    userMessage: string;
    aiResponse: string;
    conversationType: ConversationType;
    tasksCreated: number;
    createdAt: string;
}

export const aiApi = {
    sendMessage: async (request: AiChatRequest): Promise<AiChatResponse> => {
        const response = await api.post<AiChatResponse>('/ai/chat', request);
        return response.data;
    },

    getDailyBriefing: async (): Promise<AiChatResponse> => {
        const response = await api.get<AiChatResponse>('/ai/briefing');
        return response.data;
    },

    getHistory: async (): Promise<AiConversationHistoryItem[]> => {
        const response = await api.get<AiConversationHistoryItem[]>('/ai/history');
        return response.data;
    },
};
