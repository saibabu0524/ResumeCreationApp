import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserPublic } from "@/types/api";

interface AuthState {
    /** Access token in memory only */
    accessToken: string | null;
    /** Refresh token — stored in localStorage via persist middleware */
    refreshToken: string | null;
    user: UserPublic | null;

    setTokens: (accessToken: string, refreshToken: string) => void;
    setAccessToken: (token: string) => void;
    setUser: (user: UserPublic) => void;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            accessToken: null,
            refreshToken: null,
            user: null,

            setTokens: (accessToken, refreshToken) =>
                set({ accessToken, refreshToken }),
            setAccessToken: (accessToken) => set({ accessToken }),
            setUser: (user) => set({ user }),
            clearAuth: () => set({ accessToken: null, refreshToken: null, user: null }),
        }),
        {
            name: "resumetailor-auth",
            // Only persist the refresh token and user — access token is in-memory after hydration
            partialize: (state) => ({
                refreshToken: state.refreshToken,
                user: state.user,
                // Access token is intentionally not persisted
                accessToken: null,
            }),
        }
    )
);
