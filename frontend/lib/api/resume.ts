import type { ApiWrapper } from "@/types/api";
import { client } from "./client";

/**
 * Upload a PDF + job description to get a tailored resume back.
 * Field name is "resume" per OpenAPI spec. Returns binary PDF.
 */
export async function tailorResume(
    resume: File,
    jobDescription: string
): Promise<ArrayBuffer> {
    const formData = new FormData();
    formData.append("resume", resume);
    formData.append("job_description", jobDescription);

    const res = await client.post<ArrayBuffer>("/resume/tailor", formData, {
        headers: { "Content-Type": "multipart/form-data" },
        responseType: "arraybuffer",
    });
    return res.data;
}

/**
 * Fetch history of tailored resumes for the current user.
 * Returns raw dict array from the API.
 */
export async function getResumeHistory(): Promise<Record<string, unknown>[]> {
    const res = await client.get<ApiWrapper<Record<string, unknown>[]>>(
        "/resume/history"
    );
    return res.data.data ?? [];
}
