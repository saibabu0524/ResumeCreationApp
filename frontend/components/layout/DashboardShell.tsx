"use client";

import { useState } from "react";
import { Menu, X, FileText } from "lucide-react";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { Sidebar } from "@/components/navigation/Sidebar";

/**
 * Client-side shell for all dashboard pages.
 * Handles the mobile sidebar drawer state so the server layout
 * can stay a plain server component.
 */
export function DashboardShell({ children }: { children: React.ReactNode }) {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    return (
        <ProtectedRoute>
            <div className="flex h-[100dvh] overflow-hidden bg-background">
                {/* Mobile backdrop */}
                {sidebarOpen && (
                    <div
                        aria-hidden="true"
                        className="fixed inset-0 z-20 bg-black/60 backdrop-blur-sm lg:hidden"
                        onClick={() => setSidebarOpen(false)}
                    />
                )}

                {/* Sidebar — fixed drawer on mobile, static column on desktop */}
                <div
                    className={`
                        fixed inset-y-0 left-0 z-30 transform transition-transform duration-200 ease-in-out
                        lg:static lg:translate-x-0 lg:z-auto
                        ${sidebarOpen ? "translate-x-0" : "-translate-x-full"}
                    `}
                >
                    <Sidebar onClose={() => setSidebarOpen(false)} />
                </div>

                {/* Main area */}
                <div className="flex flex-1 flex-col min-w-0 overflow-hidden">
                    {/* Mobile top bar */}
                    <div className="flex items-center gap-3 px-4 py-3 border-b border-border/40 bg-background/80 backdrop-blur-md lg:hidden shrink-0">
                        <button
                            type="button"
                            aria-label="Open menu"
                            onClick={() => setSidebarOpen(true)}
                            className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                        >
                            <Menu className="h-5 w-5" />
                        </button>
                        <div className="flex items-center gap-2">
                            <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary/15 ring-1 ring-primary/30">
                                <FileText className="h-3.5 w-3.5 text-primary" />
                            </div>
                            <span className="text-sm font-bold gradient-text">ResumeTailor</span>
                        </div>
                    </div>

                    <main className="flex-1 overflow-y-auto">
                        <div className="mx-auto max-w-6xl px-4 py-6 md:px-6 md:py-8">
                            {children}
                        </div>
                    </main>
                </div>
            </div>
        </ProtectedRoute>
    );
}
