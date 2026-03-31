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

/** Tailor a resume using a stored resume ID or file upload. */
export async function tailorResume(params: {
  storedResumeId?: string;
  file?: File;
  jobDescription: string;
  provider?: string;
}): Promise<Blob> {
  const form = new FormData();
  form.append("job_description", params.jobDescription);
  form.append("provider", params.provider ?? "gemini");

  if (params.storedResumeId) {
    form.append("stored_resume_id", params.storedResumeId);
  } else if (params.file) {
    form.append("resume", params.file);
  }

  const resp = await apiClient.post("/resume/tailor", form, {
    headers: { "Content-Type": "multipart/form-data" },
    responseType: "blob",
  });

  return resp.data;
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
