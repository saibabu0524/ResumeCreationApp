"use client";

import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import * as authApi from "@/lib/api/auth";
import { useAuthStore } from "@/lib/stores/auth-store";
import type { LoginRequest, RegisterRequest } from "@/types/api";

export function useAuth() {
    const { user, accessToken, setTokens, setUser, clearAuth } = useAuthStore();
    const router = useRouter();

    const isAuthenticated = !!accessToken;

    const loginMutation = useMutation({
        mutationFn: (data: LoginRequest) => authApi.login(data),
        onSuccess: async (tokens) => {
            setTokens(tokens.access_token, tokens.refresh_token);
            // Fetch user profile after login
            try {
                const profile = await authApi.getProfile();
                setUser(profile);
                toast.success(`Welcome back!`);
            } catch {
                toast.success("Signed in successfully!");
            }
            router.push("/dashboard");
        },
        onError: (error: unknown) => {
            const message =
                error instanceof Error ? error.message : "Invalid email or password";
            toast.error(message);
        },
    });

    const registerMutation = useMutation({
        mutationFn: (data: RegisterRequest) => authApi.register(data),
        onSuccess: async (tokens) => {
            setTokens(tokens.access_token, tokens.refresh_token);
            try {
                const profile = await authApi.getProfile();
                setUser(profile);
                toast.success("Account created! Welcome to ResumeTailor!");
            } catch {
                toast.success("Account created successfully!");
            }
            router.push("/dashboard");
        },
        onError: (error: unknown) => {
            const message =
                error instanceof Error ? error.message : "Registration failed";
            toast.error(message);
        },
    });

    function logout() {
        clearAuth();
        toast.info("You've been signed out");
        router.push("/login");
    }

    return {
        user,
        isAuthenticated,
        login: loginMutation.mutate,
        loginIsPending: loginMutation.isPending,
        loginError: loginMutation.error,
        register: registerMutation.mutate,
        registerIsPending: registerMutation.isPending,
        registerError: registerMutation.error,
        logout,
    };
}
