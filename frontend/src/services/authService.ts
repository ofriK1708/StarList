import { signUp, confirmSignUp, signIn, signOut, fetchAuthSession } from 'aws-amplify/auth';

export const authService = {
    /**
     * Registers a new user in Cognito. Triggers a verification email.
     * Does NOT create the backend user — that happens after confirmation.
     */
    handleSignUp: (email: string, password: string, name: string) =>
        signUp({
            username: email,
            password,
            options: { userAttributes: { email, name } },
        }),

    /** Confirms the user's email with the code sent by Cognito. */
    handleConfirmSignUp: (email: string, code: string) =>
        confirmSignUp({ username: email, confirmationCode: code }),

    /** Signs the user in and establishes an authenticated Cognito session. */
    handleSignIn: (email: string, password: string) =>
        signIn({ username: email, password }),

    /** Signs the user out and clears the Cognito session. */
    handleSignOut: () => signOut(),

    /**
     * Returns the raw JWT idToken string from the current session,
     * or null if there is no active session.
     * Include this as `Authorization: Bearer <token>` in backend requests.
     */
    getIdToken: async (): Promise<string | null> => {
        const session = await fetchAuthSession();
        return session.tokens?.idToken?.toString() ?? null;
    },

    /**
     * Returns the Cognito sub (UUID) from the current session's idToken,
     * or null if there is no active session.
     * This sub is stored as cognitoUserId in the Spring Boot backend.
     */
    getCognitoSub: async (): Promise<string | null> => {
        const session = await fetchAuthSession();
        return (session.tokens?.idToken?.payload?.sub as string) ?? null;
    },
};
