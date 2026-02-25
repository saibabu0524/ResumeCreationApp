"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/stores/auth-store";

interface ProtectedRouteProps {
    children: React.ReactNode;
}

/**
 * Wraps page content that requires authentication.
 * Redirects to /login if no access token is present in the auth store.
 * Must be a Client Component because it reads Zustand state.
 */
export function ProtectedRoute({ children }: ProtectedRouteProps) {
    const accessToken = useAuthStore((s) => s.accessToken);
    const router = useRouter();

    useEffect(() => {
        if (!accessToken) {
            router.replace("/login");
        }
    }, [accessToken, router]);

    // Don't flash protected content before redirect completes
    if (!accessToken) return null;

    return <>{children}</>;
}
