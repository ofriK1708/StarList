import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { usersApi, UserResponse, CreateUserRequest } from '../services/usersApi';
import { setAuthToken } from '../services/api';

interface UserContextType {
    user: UserResponse | null;
    isLoading: boolean;
    register: (data: CreateUserRequest) => Promise<UserResponse>;

    // TODO: AWS Cognito - This function will be replaced by a real login function:
    // login: (email, password) => Promise<void>
    loginByDevId: (userId: number) => Promise<void>;

    logout: () => void;
    spendCoins: (amount: number) => boolean;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export function UserProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // TODO: AWS Cognito - Instead of reading a fake user ID from local storage,
        // you will check for a valid JWT Token from AWS Cognito here.
        const savedUserId = localStorage.getItem('starlist_user_id');
        if (savedUserId) {
            loginByDevId(Number(savedUserId)).catch(() => {
                // Session is stale (e.g. DB was reset); logout() already called inside loginByDevId
            });
        } else {
            setIsLoading(false);
        }
    }, []);

    const register = async (userData: CreateUserRequest): Promise<UserResponse> => {
        setIsLoading(true);
        try {
            // TODO: AWS Cognito - Later, you will first register the user in AWS Cognito,
            // get their generated Cognito sub/ID, and THEN save them to your Spring Boot DB.
            const newUser = await usersApi.createUser(userData);

            // FIXED: Returning the new user so Login.tsx can display the ID
            return newUser;
        } catch (error) {
            console.error("Registration failed:", error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    // TODO: AWS Cognito - This entire function is for DEV MODE only.
    // In production, you will send email & password to AWS Cognito, receive a JWT token,
    // and then fetch the user details from your backend using that token.
    const loginByDevId = async (userId: number) => {
        setIsLoading(true);
        try {
            const existingUser = await usersApi.getUser(userId);
            setUser(existingUser);

            // TODO: AWS Cognito - Here you will save the real JWT Token, not the numeric ID.
            setAuthToken(existingUser.id);
            localStorage.setItem('starlist_user_id', existingUser.id.toString());
        } catch (error) {
            console.error("Login failed:", error);
            logout();
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    const logout = () => {
        setUser(null);
        // TODO: AWS Cognito - Here you will also call AWS Cognito's logout function
        // to clear their cookies/sessions.
        setAuthToken(null);
        localStorage.removeItem('starlist_user_id');
    };

    const spendCoins = (amount: number) => {
        if (user && user.totalCoins >= amount) {
            setUser({ ...user, totalCoins: user.totalCoins - amount });
            return true;
        }
        return false;
    };

    return (
        <UserContext.Provider value={{ user, isLoading, register, loginByDevId, logout, spendCoins }}>
            {children}
        </UserContext.Provider>
    );
}

export function useUser() {
    const context = useContext(UserContext);
    if (context === undefined) {
        throw new Error('useUser must be used within a UserProvider');
    }
    return context;
}