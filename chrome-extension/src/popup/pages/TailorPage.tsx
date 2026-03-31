import React, { useEffect, useState, useRef } from "react";
import { tailorResume } from "../../lib/api-resume";
import type { StoredResume, ExtractionResult } from "../../types";
import { listResumes } from "../../lib/api-resume";

const MODEL_OPTIONS = [
  { id: "gemini-flash", label: "Gemini 2.0 Flash", provider: "gemini", model: "gemini-2.0-flash" },
  { id: "gemini-pro", label: "Gemini 2.5 Pro", provider: "gemini", model: "gemini-2.5-pro" },
  { id: "kimi-8k", label: "Kimi 8K", provider: "cloud", model: "moonshot-v1-8k" },
  { id: "kimi-32k", label: "Kimi 32K", provider: "cloud", model: "moonshot-v1-32k" },
  { id: "kimi-128k", label: "Kimi 128K", provider: "cloud", model: "moonshot-v1-128k" },
  { id: "qwen-72b", label: "Qwen 2.5 72B", provider: "qwen", model: "Qwen/Qwen2.5-72B-Instruct" },
  { id: "ollama", label: "Ollama (local/custom)", provider: "ollama", model: null },
];

export function TailorPage() {
  const [jobDescription, setJobDescription] = useState("");
  const [extractionSource, setExtractionSource] = useState("");
  const [resumes, setResumes] = useState<StoredResume[]>([]);
  const [selectedResumeId, setSelectedResumeId] = useState("");
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [useUpload, setUseUpload] = useState(false);
  const [selectedModelId, setSelectedModelId] = useState("gemini-flash");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [progressMsg, setProgressMsg] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);

  // Load extracted JD + stored resumes on mount
  useEffect(() => {
    chrome.storage.session.get("lastExtraction", (result) => {
      const extraction = result.lastExtraction as ExtractionResult | undefined;
      if (extraction?.jobDescription) {
        setJobDescription(extraction.jobDescription);
        setExtractionSource(extraction.source);
      }
    });

    listResumes().then((resp) => {
      if (resp.data) {
        setResumes(resp.data);
        if (resp.data.length > 0) setSelectedResumeId(resp.data[0].id);
      }
    });
  }, []);

  const handleExtractFromPage = () => {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      const tabId = tabs[0]?.id;
      if (!tabId) return;
      chrome.tabs.sendMessage(tabId, { type: "EXTRACT_JD" }, (response) => {
        if (response?.jobDescription) {
          setJobDescription(response.jobDescription);
          setExtractionSource(response.source);
        } else {
          setError("No job description found on this page");
        }
      });
    });
  };

  const handleTailor = async () => {
    const selected = MODEL_OPTIONS.find((m) => m.id === selectedModelId) ?? MODEL_OPTIONS[0];
    if (!jobDescription.trim()) {
      setError("Job description is required");
      return;
    }
    if (!useUpload && !selectedResumeId) {
      setError("Select a resume or upload one");
      return;
    }
    if (useUpload && !uploadFile) {
      setError("Select a PDF file to upload");
      return;
    }

    setLoading(true);
    setError("");
    setSuccess("");
    setProgressMsg("Uploading resume\u2026");

    try {
      const blob = await tailorResume({
        storedResumeId: !useUpload ? selectedResumeId : undefined,
        file: useUpload ? uploadFile! : undefined,
        jobDescription,
        provider: selected.provider,
        model: selected.model,
        onProgress: setProgressMsg,
      });

      // Download the tailored PDF
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "tailored_resume.pdf";
      a.click();
      URL.revokeObjectURL(url);
      setSuccess("Tailored resume downloaded!");
    } catch (err: any) {
      const detail = err?.response?.data?.detail;
      setError(typeof detail === "string" ? detail : err?.message ?? "Tailoring failed. Please try again.");
    } finally {
      setLoading(false);
      setProgressMsg("");
    }
  };

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-semibold text-gray-900">Tailor Resume</h2>

      {error && (
        <div className="p-2 bg-red-50 text-red-700 text-xs rounded">{error}</div>
      )}
      {success && (
        <div className="p-2 bg-green-50 text-green-700 text-xs rounded">{success}</div>
      )}

      {/* Job Description */}
      <div>
        <div className="flex justify-between items-center mb-1">
          <label className="text-sm font-medium text-gray-700">Job Description</label>
          <button
            onClick={handleExtractFromPage}
            className="text-xs text-blue-600 hover:text-blue-800"
          >
            Extract from page
          </button>
        </div>
        {extractionSource && (
          <p className="text-xs text-gray-400 mb-1">Source: {extractionSource}</p>
        )}
        <textarea
          value={jobDescription}
          onChange={(e) => setJobDescription(e.target.value)}
          rows={5}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-xs resize-y focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          placeholder="Paste or extract job description..."
        />
      </div>

      {/* Resume Selection */}
      <div>
        <label className="text-sm font-medium text-gray-700 block mb-1">Resume</label>
        <div className="flex gap-2 mb-2">
          <button
            onClick={() => setUseUpload(false)}
            className={`text-xs px-3 py-1 rounded-full ${
              !useUpload ? "bg-blue-600 text-white" : "bg-gray-100 text-gray-600"
            }`}
          >
            Stored ({resumes.length})
          </button>
          <button
            onClick={() => setUseUpload(true)}
            className={`text-xs px-3 py-1 rounded-full ${
              useUpload ? "bg-blue-600 text-white" : "bg-gray-100 text-gray-600"
            }`}
          >
            Upload New
          </button>
        </div>

        {!useUpload ? (
          <select
            value={selectedResumeId}
            onChange={(e) => setSelectedResumeId(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-xs"
          >
            {resumes.length === 0 && <option value="">No stored resumes</option>}
            {resumes.map((r) => (
              <option key={r.id} value={r.id}>
                {r.original_filename}
              </option>
            ))}
          </select>
        ) : (
          <input
            ref={fileRef}
            type="file"
            accept=".pdf"
            onChange={(e) => setUploadFile(e.target.files?.[0] ?? null)}
            className="w-full text-xs"
          />
        )}
      </div>

      {/* AI Model */}
      <div>
        <label className="text-sm font-medium text-gray-700 block mb-1">AI Model</label>
        <select
          value={selectedModelId}
          onChange={(e) => setSelectedModelId(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-xs"
        >
          {MODEL_OPTIONS.map((opt) => (
            <option key={opt.id} value={opt.id}>
              {opt.label} — {opt.provider}
            </option>
          ))}
        </select>
      </div>

      {loading && progressMsg && (
        <div className="p-2 bg-blue-50 text-blue-700 text-xs rounded text-center">
          {progressMsg}
        </div>
      )}

      <button
        onClick={handleTailor}
        disabled={loading}
        className="w-full py-2 px-4 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {loading ? "Processing…" : "Tailor Resume"}
      </button>
    </div>
  );
}
