import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { usersApi, UserResponse } from '../services/usersApi';
import { setAuthToken } from '../services/api';
import { authService } from '../services/authService';

interface UserContextType {
    user: UserResponse | null;
    isLoading: boolean;
    /** Step 1 of sign-up: registers in Cognito and triggers a verification email. */
    register: (email: string, password: string, name: string) => Promise<void>;
    /** Step 2 of sign-up: confirms the email code, signs in, then creates the backend user. */
    confirmSignUp: (email: string, code: string, password: string, displayName: string) => Promise<void>;
    /** Signs in with email/password via Cognito and fetches the backend user. */
    login: (email: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    spendCoins: (amount: number) => boolean;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export function UserProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // Restore an existing Cognito session on page load.
        // Amplify manages its own session storage — no localStorage needed.
        const restoreSession = async () => {
            try {
                const token = await authService.getIdToken();
                const sub = await authService.getCognitoSub();
                if (token && sub) {
                    setAuthToken(token);
                    const email = await authService.getEmail();
                    setUser(await fetchOrCreateBackendUser(sub, email ?? ''));
                }
            } catch {
                // No active Cognito session — user needs to log in.
            } finally {
                setIsLoading(false);
            }
        };
        restoreSession();
    }, []);

    /**
     * Looks up the backend user for a confirmed Cognito sub, self-healing
     * if the row is missing (e.g. createUser failed right after signup
     * confirmation, leaving a valid Cognito account with no backend user).
     * Used by both session restore and login so neither path can leave a
     * confirmed Cognito user permanently stuck.
     */
    const fetchOrCreateBackendUser = async (sub: string, email: string): Promise<UserResponse> => {
        try {
            return await usersApi.getUserByCognitoSub(sub);
        } catch (e: any) {
            if (e?.response?.status === 404) {
                return await usersApi.createUser({
                    email,
                    cognitoUserId: sub,
                    displayName: email.split('@')[0],
                });
            }
            throw e;
        }
    };

    /**
     * Step 1 of registration: creates the Cognito account and triggers a
     * verification email. The backend user is NOT created yet.
     * After this, the UI should prompt the user for their confirmation code.
     */
    const register = async (email: string, password: string, name: string): Promise<void> => {
        await authService.handleSignUp(email, password, name);
    };

    /**
     * Step 2 of registration: confirms the email verification code,
     * signs in automatically, then creates the backend user using the
     * Cognito sub as cognitoUserId.
     */
    const confirmSignUp = async (email: string, code: string, password: string, displayName: string): Promise<void> => {
        await authService.handleConfirmSignUp(email, code);
        await authService.handleSignIn(email, password);
        const token = await authService.getIdToken();
        const sub = await authService.getCognitoSub();
        setAuthToken(token!);
        const newUser = await usersApi.createUser({
            email,
            cognitoUserId: sub!,
            displayName,
        });
        setUser(newUser);
    };

    /**
     * Signs in with email/password via Cognito, sets the JWT Bearer token
     * on the Axios instance, then fetches the corresponding backend user.
     * Requires backend endpoint: GET /users/cognito/{sub}
     */
    const login = async (email: string, password: string): Promise<void> => {
        await authService.handleSignOut().catch(() => {});
        await authService.handleSignIn(email, password);
        const token = await authService.getIdToken();
        const sub = await authService.getCognitoSub();
        setAuthToken(token!);

        setUser(await fetchOrCreateBackendUser(sub!, email));
    };

    const logout = async (): Promise<void> => {
        await authService.handleSignOut();
        setAuthToken(null);
        setUser(null);
    };

    const spendCoins = (amount: number) => {
        if (user && user.totalCoins >= amount) {
            setUser({ ...user, totalCoins: user.totalCoins - amount });
            return true;
        }
        return false;
    };

    return (
        <UserContext.Provider value={{ user, isLoading, register, confirmSignUp, login, logout, spendCoins }}>
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
