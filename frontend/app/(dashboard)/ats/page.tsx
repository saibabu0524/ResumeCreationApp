import type { Metadata } from "next";
import { ATSUploader } from "@/components/ats/ATSUploader";

export const metadata: Metadata = {
    title: "ATS Analyzer",
    description: "Check how well your resume matches a job description",
};

export default function ATSPage() {
    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-foreground">ATS Analyzer</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Upload your resume and paste a job description to see your ATS compatibility score
                </p>
            </div>
            <ATSUploader />
        </div>
    );
}
