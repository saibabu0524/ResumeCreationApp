import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "@/lib/stores/auth-store";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

if (!BASE_URL) {
    throw new Error(
        "NEXT_PUBLIC_API_BASE_URL is not set. Copy .env.local.example to .env.local."
    );
}

export const client: AxiosInstance = axios.create({
    baseURL: BASE_URL,
    headers: { "Content-Type": "application/json" },
});

// ── Request interceptor — attach access token ─────────────────────────────────
client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// ── Response interceptor — handle 401 / token refresh ────────────────────────
let isRefreshing = false;
let pendingQueue: Array<{
    resolve: (value: unknown) => void;
    reject: (reason?: unknown) => void;
}> = [];

function drainQueue(error: unknown, token?: string) {
    pendingQueue.forEach(({ resolve, reject }) => {
        error ? reject(error) : resolve(token);
    });
    pendingQueue = [];
}

client.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & {
            _retried?: boolean;
        };

        if (error.response?.status === 401 && !originalRequest._retried) {
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    pendingQueue.push({ resolve, reject });
                }).then((token) => {
                    if (originalRequest.headers) {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                    }
                    return client(originalRequest);
                });
            }

            originalRequest._retried = true;
            isRefreshing = true;

            try {
                const refreshToken = useAuthStore.getState().refreshToken;
                if (!refreshToken) throw new Error("No refresh token");

                // Refresh token is in the request body (not HttpOnly cookie)
                const { data } = await client.post<{
                    data: { access_token: string; refresh_token: string };
                }>("/auth/refresh", { refresh_token: refreshToken });

                const newAccessToken = data.data.access_token;
                const newRefreshToken = data.data.refresh_token;

                useAuthStore.getState().setTokens(newAccessToken, newRefreshToken);
                drainQueue(null, newAccessToken);

                if (originalRequest.headers) {
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                }
                return client(originalRequest);
            } catch (refreshError) {
                drainQueue(refreshError);
                useAuthStore.getState().clearAuth();
                if (typeof window !== "undefined") {
                    window.location.href = "/login";
                }
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);
