import type { Metadata } from "next";
import { TailorWizard } from "@/components/resume/TailorWizard";

export const metadata: Metadata = {
    title: "Tailor Studio",
    description: "AI-powered resume tailoring for any job description",
};

export default function TailorPage() {
    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-foreground">Resume Tailor Studio</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    AI rewrites your resume to match any job description — optimised for ATS
                </p>
            </div>
            <TailorWizard />
        </div>
    );
}
