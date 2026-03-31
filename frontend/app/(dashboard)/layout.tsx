import { DashboardShell } from "@/components/layout/DashboardShell";
import { ErrorBoundary } from "@/components/ui/ErrorBoundary";

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <DashboardShell>
            <ErrorBoundary>{children}</ErrorBoundary>
        </DashboardShell>
    );
}
