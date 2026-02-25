"use client";

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { Toaster } from "sonner";
import { AuthInitializer } from "@/components/auth/AuthInitializer";

export function Providers({ children }: { children: React.ReactNode }) {
    // Each browser session gets its own QueryClient — SSR-safe pattern
    const [queryClient] = useState(
        () =>
            new QueryClient({
                defaultOptions: {
                    queries: {
                        staleTime: 5 * 60 * 1000,  // 5 minutes — reduces refetch churn
                        gcTime: 10 * 60 * 1000,    // 10 minutes — keep data in memory
                        retry: 1,
                        refetchOnWindowFocus: false, // avoid unnecessary re-fetches
                    },
                },
            })
    );

    return (
        <QueryClientProvider client={queryClient}>
            <ThemeProvider
                attribute="class"
                defaultTheme="dark"
                enableSystem={false}
                disableTransitionOnChange
            >
                {/* Silently restores session from refreshToken on page load */}
                <AuthInitializer />
                {children}
                <Toaster richColors position="top-right" />
            </ThemeProvider>
        </QueryClientProvider>
    );
}
