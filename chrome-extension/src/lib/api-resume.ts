/**
 * Resume & ATS API calls.
 */
import { apiClient } from "./api-client";
import type { ApiResponse, AtsResult, StoredResume } from "../types";

/** Upload a base resume to the library. */
export async function uploadResume(file: File): Promise<ApiResponse<StoredResume>> {
  const form = new FormData();
  form.append("file", file);
  const resp = await apiClient.post<ApiResponse<StoredResume>>("/resumes/upload", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return resp.data;
}

/** List user's stored resumes. */
export async function listResumes(): Promise<ApiResponse<StoredResume[]>> {
  const resp = await apiClient.get<ApiResponse<StoredResume[]>>("/resumes/");
  return resp.data;
}

/** Delete a stored resume. */
export async function deleteResume(id: string): Promise<void> {
  await apiClient.delete(`/resumes/${id}`);
}

interface TailorJobResponseData {
  job_id: string;
  status: string;
}

interface JobStatusData {
  job_id: string;
  status: string;
  error?: string | null;
}

const POLL_INTERVAL_MS = 4000;
const POLL_MAX_ATTEMPTS = 150; // 10 minutes

/** Tailor a resume using a stored resume ID or file upload. */
export async function tailorResume(params: {
  storedResumeId?: string;
  file?: File;
  jobDescription: string;
  provider?: string;
  model?: string | null;
  onProgress?: (message: string) => void;
}): Promise<Blob> {
  const form = new FormData();
  form.append("job_description", params.jobDescription);
  form.append("provider", params.provider ?? "gemini");
  if (params.model) {
    form.append("model", params.model);
  }

  if (params.storedResumeId) {
    form.append("stored_resume_id", params.storedResumeId);
  } else if (params.file) {
    form.append("resume", params.file);
  }

  // Step 1: Submit job → 202 Accepted
  const submitResp = await apiClient.post<ApiResponse<TailorJobResponseData>>(
    "/resume/tailor",
    form,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  const jobId = submitResp.data.data?.job_id;
  if (!jobId) throw new Error("Server did not return a job ID");

  params.onProgress?.("Resume uploaded — processing…");

  // Step 2: Poll until completed or failed
  for (let attempt = 0; attempt < POLL_MAX_ATTEMPTS; attempt++) {
    await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));

    const statusResp = await apiClient.get<ApiResponse<JobStatusData>>(
      `/resume/jobs/${jobId}`,
    );
    const job = statusResp.data.data;
    if (!job) continue;

    const elapsed = Math.round(((attempt + 1) * POLL_INTERVAL_MS) / 1000);

    if (job.status === "completed") {
      params.onProgress?.("Downloading tailored resume…");
      // Step 3: Download PDF
      const dlResp = await apiClient.get(`/resume/jobs/${jobId}/download`, {
        responseType: "blob",
      });
      return dlResp.data as Blob;
    }

    if (job.status === "failed") {
      throw new Error(job.error ?? "Resume processing failed");
    }

    params.onProgress?.(
      elapsed < 30
        ? "AI is tailoring your resume…"
        : `Still processing… (${elapsed}s)`,
    );
  }

  throw new Error("Job timed out after 10 minutes. Please try again.");
}

/** Run ATS analysis using a stored resume ID or file upload. */
export async function analyseAts(params: {
  storedResumeId?: string;
  file?: File;
  jobDescription: string;
  provider?: string;
}): Promise<ApiResponse<AtsResult>> {
  const form = new FormData();
  form.append("job_description", params.jobDescription);
  form.append("provider", params.provider ?? "gemini");

  if (params.storedResumeId) {
    form.append("stored_resume_id", params.storedResumeId);
  } else if (params.file) {
    form.append("resume", params.file);
  }

  const resp = await apiClient.post<ApiResponse<AtsResult>>("/ats/analyse", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });

  return resp.data;
}
