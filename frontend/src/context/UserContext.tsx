import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { usersApi, UserResponse, CreateUserRequest } from '../services/usersApi';
import { setAuthToken } from '../services/api';

interface UserContextType {
    user: UserResponse | null;
    isLoading: boolean;
    register: (data: CreateUserRequest) => Promise<void>;
    loginByDevId: (userId: number) => Promise<void>;
    logout: () => void;
    spendCoins: (amount: number) => boolean;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export function UserProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const savedUserId = localStorage.getItem('starlist_user_id');
        if (savedUserId) {
            loginByDevId(Number(savedUserId));
        } else {
            setIsLoading(false);
        }
    }, []);

    const register = async (userData: CreateUserRequest) => {
        setIsLoading(true);
        try {
            const newUser = await usersApi.createUser(userData);
            setUser(newUser);
            setAuthToken(newUser.id); // שותל את ה-ID בכל הבקשות הבאות
            localStorage.setItem('starlist_user_id', newUser.id.toString());
        } catch (error) {
            console.error("Registration failed:", error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    const loginByDevId = async (userId: number) => {
        setIsLoading(true);
        try {
            const existingUser = await usersApi.getUser(userId);
            setUser(existingUser);
            setAuthToken(existingUser.id);
            localStorage.setItem('starlist_user_id', existingUser.id.toString());
        } catch (error) {
            console.error("Login failed:", error);
            logout();
        } finally {
            setIsLoading(false);
        }
    };

    const logout = () => {
        setUser(null);
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