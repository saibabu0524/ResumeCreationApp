import type { ATSAnalysisResponse, ApiWrapper } from "@/types/api";
import { client } from "./client";

/**
 * Analyse a resume PDF against a job description.
 * Field name is "resume" (not "file") per OpenAPI spec.
 */
export async function analyseResume(
    resume: File,
    jobDescription: string
): Promise<ATSAnalysisResponse> {
    const formData = new FormData();
    formData.append("resume", resume);
    formData.append("job_description", jobDescription);

    const res = await client.post<ApiWrapper<ATSAnalysisResponse>>(
        "/ats/analyse",
        formData,
        { headers: { "Content-Type": "multipart/form-data" } }
    );
    if (!res.data.data) throw new Error(res.data.message ?? "ATS analysis failed");
    return res.data.data;
}
