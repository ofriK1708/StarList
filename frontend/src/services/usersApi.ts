import api from './api';

// --- Types / Interfaces --- //

export interface CreateUserRequest {
    email: string;
    cognitoUserId: string;
    displayName: string;
}

export interface UpdateUserRequest {
    displayName: string;
}

export interface UserResponse {
    id: number;
    email: string;
    displayName: string;
    totalCoins: number;
    lifetimeCoinsEarned: number;
    currentGalaxyCycle: number;
    galaxyResetDate: string; // LocalDate (YYYY-MM-DD)
    createdAt: string;       // Instant (ISO-8601)
}

// --- API Functions --- //

export const usersApi = {
    /**
     * Create a new user (Sign Up)
     */
    createUser: async (userData: CreateUserRequest): Promise<UserResponse> => {
        const response = await api.post<UserResponse>('/users', userData);
        return response.data;
    },

    /**
     * Update user details
     */
    updateUser: async (userId: number, updateData: UpdateUserRequest): Promise<UserResponse> => {
        const response = await api.put<UserResponse>(`/users/${userId}`, updateData);
        return response.data;
    },

    /**
     * Delete user and all associated data
     */
    deleteUser: async (userId: number): Promise<void> => {
        await api.delete(`/users/${userId}`);
    },

    /**
     * Look up an existing user by their Cognito sub (UUID).
     * Requires backend endpoint: GET /users/cognito/{sub}
     */
    getUserByCognitoSub: async (sub: string): Promise<UserResponse> => {
        const response = await api.get<UserResponse>(`/users/cognito/${sub}`);
        return response.data;
    },
};