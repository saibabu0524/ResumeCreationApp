"use client";

import { useEffect, useRef } from "react";
import { useAuthStore } from "@/lib/stores/auth-store";
import * as authApi from "@/lib/api/auth";

/**
 * Mounted once at the root of the app (inside Providers).
 *
 * Waits for Zustand's persist middleware to finish rehydrating from
 * localStorage, then – if a refreshToken exists but no accessToken –
 * performs a silent token refresh before ProtectedRoute makes any
 * redirect decisions.
 *
 * Sets isInitializing = false when done.
 */
export function AuthInitializer() {
    const { setTokens, clearAuth, setIsInitializing } = useAuthStore();
    const initialized = useRef(false);

    useEffect(() => {
        // Guard against StrictMode double-invocation
        if (initialized.current) return;
        initialized.current = true;

        async function restoreSession() {
            // Wait for the persist middleware to finish loading from localStorage.
            // This prevents the race where useEffect fires before rehydration.
            await useAuthStore.persist.rehydrate();

            const { accessToken, refreshToken } = useAuthStore.getState();

            // Already have an access token in memory — nothing to do
            if (accessToken) {
                setIsInitializing(false);
                return;
            }

            // No refresh token either — user is definitely not logged in
            if (!refreshToken) {
                setIsInitializing(false);
                return;
            }

            // Attempt silent refresh
            try {
                const tokens = await authApi.refresh(refreshToken);
                setTokens(tokens.access_token, tokens.refresh_token);
            } catch {
                // Refresh token is expired or invalid — clear everything
                clearAuth();
            } finally {
                setIsInitializing(false);
            }
        }

        restoreSession();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
}
