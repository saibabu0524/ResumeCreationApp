import type { Metadata } from "next";
import { Wand2 } from "lucide-react";
import { ResumeHistoryList } from "@/components/resume/ResumeHistoryList";

export const metadata: Metadata = {
    title: "Dashboard",
    description: "Your resume history and activity",
};

export default function DashboardPage() {
    return (
        <div className="space-y-8">
            {/* Page header */}
            <div>
                <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    All your previously tailored resumes
                </p>
            </div>

            {/* Quick action cards */}
            <div className="grid gap-4 sm:grid-cols-2">
                <a
                    href="/tailor"
                    id="cta-tailor"
                    className="group glass rounded-xl p-5 hover:ring-1 hover:ring-primary/30 transition-all duration-200 hover:-translate-y-0.5"
                >
                    <div className="flex items-start gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 ring-1 ring-blue-500/20">
                            <Wand2 className="h-5 w-5 text-blue-400" />
                        </div>
                        <div>
                            <p className="font-semibold text-foreground group-hover:text-primary transition-colors">
                                Tailor a Resume
                            </p>
                            <p className="mt-0.5 text-sm text-muted-foreground">
                                Upload PDF + job description, get AI-tailored output
                            </p>
                        </div>
                    </div>
                </a>

                <a
                    href="/ats"
                    id="cta-ats"
                    className="group glass rounded-xl p-5 hover:ring-1 hover:ring-emerald-500/30 transition-all duration-200 hover:-translate-y-0.5"
                >
                    <div className="flex items-start gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-500/10 ring-1 ring-emerald-500/20">
                            <svg className="h-5 w-5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                                <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                            </svg>
                        </div>
                        <div>
                            <p className="font-semibold text-foreground group-hover:text-emerald-400 transition-colors">
                                ATS Score Check
                            </p>
                            <p className="mt-0.5 text-sm text-muted-foreground">
                                Scan resume against a job description instantly
                            </p>
                        </div>
                    </div>
                </a>
            </div>

            {/* Resume history */}
            <div>
                <h2 className="mb-4 text-lg font-semibold text-foreground">Resume History</h2>
                <ResumeHistoryList />
            </div>
        </div>
    );
}
