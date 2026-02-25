"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuthStore } from "@/lib/stores/auth-store";

interface ProtectedRouteProps {
    children: React.ReactNode;
}

/**
 * Wraps page content that requires authentication.
 *
 * On page load, waits for AuthInitializer to finish the silent token-refresh
 * attempt (isInitializing = true) before deciding whether to redirect.
 * This prevents the flash-to-login that happens when accessToken is not
 * persisted but a valid refreshToken exists in localStorage.
 */
export function ProtectedRoute({ children }: ProtectedRouteProps) {
    const accessToken = useAuthStore((s) => s.accessToken);
    const isInitializing = useAuthStore((s) => s.isInitializing);
    const router = useRouter();

    useEffect(() => {
        // Only redirect once initialization is complete and there is no token
        if (!isInitializing && !accessToken) {
            router.replace("/login");
        }
    }, [accessToken, isInitializing, router]);

    // Show a full-screen spinner while the silent refresh is in progress
    if (isInitializing) {
        return (
            <div className="flex h-screen items-center justify-center bg-background">
                <div className="flex flex-col items-center gap-3">
                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    <p className="text-sm text-muted-foreground">Restoring session…</p>
                </div>
            </div>
        );
    }

    // Don't flash protected content before redirect completes
    if (!accessToken) return null;

    return <>{children}</>;
}
