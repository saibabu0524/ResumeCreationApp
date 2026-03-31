import type { ApiWrapper } from "@/types/api";
import { client } from "./client";

interface TailorJobResponse {
    job_id: string;
    status: string;
}

interface JobStatusResponse {
    job_id: string;
    status: string;
    download_url?: string | null;
    error?: string | null;
}

const POLL_INTERVAL_MS = 4000;
const POLL_MAX_ATTEMPTS = 150; // 10 minutes

/**
 * Submits a resume-tailoring job, polls for completion, then downloads the PDF.
 * Returns the tailored PDF as an ArrayBuffer.
 */
export async function tailorResume(
    resume: File,
    jobDescription: string,
    onProgress?: (message: string) => void,
    provider: string = "gemini",
    model?: string | null,
): Promise<ArrayBuffer> {
    const formData = new FormData();
    formData.append("resume", resume);
    formData.append("job_description", jobDescription);
    formData.append("provider", provider);
    if (model) formData.append("model", model);

    // Step 1: Submit job → 202 Accepted
    const submitRes = await client.post<ApiWrapper<TailorJobResponse>>(
        "/resume/tailor",
        formData,
        { headers: { "Content-Type": "multipart/form-data" } },
    );
    const jobId = submitRes.data.data?.job_id;
    if (!jobId) throw new Error("Server did not return a job ID");

    onProgress?.("Resume uploaded — processing…");

    // Step 2: Poll until completed or failed
    for (let attempt = 0; attempt < POLL_MAX_ATTEMPTS; attempt++) {
        await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));

        const statusRes = await client.get<ApiWrapper<JobStatusResponse>>(
            `/resume/jobs/${jobId}`,
        );
        const job = statusRes.data.data;
        if (!job) continue;

        const elapsed = Math.round(((attempt + 1) * POLL_INTERVAL_MS) / 1000);

        if (job.status === "completed") {
            onProgress?.("Downloading tailored resume…");
            // Step 3: Download PDF
            const dlRes = await client.get<ArrayBuffer>(
                `/resume/jobs/${jobId}/download`,
                { responseType: "arraybuffer" },
            );
            return dlRes.data;
        }

        if (job.status === "failed") {
            throw new Error(job.error ?? "Resume processing failed");
        }

        onProgress?.(
            elapsed < 30
                ? "AI is tailoring your resume…"
                : `Still processing… (${elapsed}s)`,
        );
    }

    throw new Error("Job timed out after 10 minutes. Please try again.");
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
