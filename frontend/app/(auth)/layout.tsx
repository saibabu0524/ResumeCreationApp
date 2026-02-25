import type { Metadata } from "next";

export const metadata: Metadata = {
    title: "Auth",
};

export default function AuthLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <div className="relative min-h-screen overflow-hidden bg-background">
            {/* Animated background gradient */}
            <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-0 -z-10"
            >
                <div className="absolute -top-40 -left-40 h-[700px] w-[700px] rounded-full bg-blue-500/10 blur-3xl" />
                <div className="absolute -bottom-40 -right-40 h-[600px] w-[600px] rounded-full bg-violet-500/10 blur-3xl" />
                <div className="absolute top-1/2 left-1/2 h-[400px] w-[400px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-blue-500/5 blur-3xl" />
            </div>

            {/* Content */}
            <div className="flex min-h-screen items-center justify-center px-4 py-16">
                <div className="w-full max-w-md">
                    {/* Logo */}
                    <div className="mb-8 text-center">
                        <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-500/20 ring-1 ring-blue-500/30">
                            <svg
                                className="h-6 w-6 text-blue-400"
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                                strokeWidth={2}
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                                />
                            </svg>
                        </div>
                        <span className="text-2xl font-bold gradient-text">ResumeTailor</span>
                    </div>

                    {children}
                </div>
            </div>
        </div>
    );
}
