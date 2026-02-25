"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Wand2, Target, LogOut, FileText } from "lucide-react";
import { useTheme } from "next-themes";
import { Sun, Moon } from "lucide-react";
import { useAuth } from "@/lib/hooks/useAuth";

const NAV_ITEMS = [
    {
        href: "/dashboard",
        icon: LayoutDashboard,
        label: "Dashboard",
        id: "nav-dashboard",
    },
    {
        href: "/ats",
        icon: Target,
        label: "ATS Analyzer",
        id: "nav-ats",
    },
    {
        href: "/tailor",
        icon: Wand2,
        label: "Tailor Studio",
        id: "nav-tailor",
    },
];

export function Sidebar() {
    const pathname = usePathname();
    const { logout, user } = useAuth();
    const { theme, setTheme } = useTheme();

    return (
        <aside className="flex h-screen w-64 flex-col glass-strong border-r border-border/60">
            {/* Logo */}
            <div className="flex items-center gap-2.5 px-5 py-6 border-b border-border/40">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-500/20 ring-1 ring-blue-500/30">
                    <FileText className="h-4 w-4 text-blue-400" />
                </div>
                <span className="text-lg font-bold gradient-text">ResumeTailor</span>
            </div>

            {/* Nav links */}
            <nav className="flex-1 space-y-1 px-3 py-4">
                {NAV_ITEMS.map(({ href, icon: Icon, label, id }) => {
                    const isActive = pathname === href || pathname.startsWith(href + "/");
                    return (
                        <Link
                            key={href}
                            id={id}
                            href={href}
                            className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150
                ${isActive
                                    ? "bg-primary/10 text-primary ring-1 ring-primary/20"
                                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                                }`}
                        >
                            <Icon className="h-4 w-4 shrink-0" />
                            {label}
                        </Link>
                    );
                })}
            </nav>

            {/* Bottom section — user + theme toggle */}
            <div className="border-t border-border/40 px-3 py-4 space-y-2">
                {/* Theme toggle */}
                <button
                    id="theme-toggle"
                    onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
                    className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground
            hover:bg-muted hover:text-foreground transition-all duration-150"
                    aria-label="Toggle theme"
                >
                    {theme === "dark" ? (
                        <Sun className="h-4 w-4 shrink-0" />
                    ) : (
                        <Moon className="h-4 w-4 shrink-0" />
                    )}
                    {theme === "dark" ? "Light mode" : "Dark mode"}
                </button>

                {/* User info + logout */}
                {user && (
                    <div className="rounded-lg border border-border/40 bg-muted/30 px-3 py-2.5">
                        <p className="text-xs font-medium text-foreground truncate">{user.email}</p>
                        <p className="text-xs text-muted-foreground truncate">
                            {user.is_superuser ? "Admin" : "User"}
                        </p>
                    </div>
                )}

                <button
                    id="nav-logout"
                    onClick={logout}
                    className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground
            hover:bg-destructive/10 hover:text-destructive transition-all duration-150"
                >
                    <LogOut className="h-4 w-4 shrink-0" />
                    Sign out
                </button>
            </div>
        </aside>
    );
}
