import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { Sidebar } from "@/components/navigation/Sidebar";

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <ProtectedRoute>
            <div className="flex h-screen overflow-hidden bg-background">
                <Sidebar />
                <main className="flex-1 overflow-y-auto">
                    <div className="mx-auto max-w-6xl px-6 py-8">{children}</div>
                </main>
            </div>
        </ProtectedRoute>
    );
}
