import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserPublic } from "@/types/api";

interface AuthState {
    /** Access token in memory only */
    accessToken: string | null;
    /** Refresh token — stored in localStorage via persist middleware */
    refreshToken: string | null;
    user: UserPublic | null;
    /**
     * True from app boot until the AuthInitializer has attempted to restore
     * the session (via refresh token). ProtectedRoute waits for this before
     * making any redirect decisions.
     */
    isInitializing: boolean;

    setTokens: (accessToken: string, refreshToken: string) => void;
    setAccessToken: (token: string) => void;
    setUser: (user: UserPublic) => void;
    clearAuth: () => void;
    setIsInitializing: (value: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            accessToken: null,
            refreshToken: null,
            user: null,
            isInitializing: true,

            setTokens: (accessToken, refreshToken) =>
                set({ accessToken, refreshToken }),
            setAccessToken: (accessToken) => set({ accessToken }),
            setUser: (user) => set({ user }),
            clearAuth: () => set({ accessToken: null, refreshToken: null, user: null }),
            setIsInitializing: (value) => set({ isInitializing: value }),
        }),
        {
            name: "resumetailor-auth",
            // Only persist the refresh token and user — access token is in-memory after hydration
            // isInitializing must NOT be persisted; it always starts as true on page load
            partialize: (state) => ({
                refreshToken: state.refreshToken,
                user: state.user,
                accessToken: null,
                isInitializing: true,
            }),
        }
    )
);
