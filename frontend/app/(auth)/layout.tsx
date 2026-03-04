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
            {/*
             * Ambient background — CSS radial-gradient (no filter: blur).
             * A single pseudo-element costs zero additional compositing layers
             * vs the previous three blurred divs that each created a GPU layer.
             */}
            <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-0 -z-10"
                style={{
                    background:
                        "radial-gradient(ellipse 80% 60% at 20% 10%, #D4A85314 0%, transparent 60%), " +
                        "radial-gradient(ellipse 70% 50% at 80% 90%, #CFA05010 0%, transparent 60%)",
                }}
            />

            {/* Content */}
            <div className="flex min-h-screen items-center justify-center px-4 py-16">
                <div className="w-full max-w-md">
                    {/* Logo */}
                    <div className="mb-8 text-center">
                        <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/15 ring-1 ring-primary/30">
                            <svg
                                className="h-6 w-6 text-primary"
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
