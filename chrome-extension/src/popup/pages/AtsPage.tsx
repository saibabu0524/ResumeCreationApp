import React, { useEffect, useState, useRef } from "react";
import { analyseAts, listResumes } from "../../lib/api-resume";
import type { AtsResult, StoredResume, ExtractionResult } from "../../types";

function ScoreRing({ score, label }: { score: number; label: string }) {
  const color =
    score >= 85
      ? "text-green-500"
      : score >= 70
        ? "text-blue-500"
        : score >= 50
          ? "text-yellow-500"
          : "text-red-500";

  return (
    <div className="flex flex-col items-center">
      <div className={`text-3xl font-bold ${color}`}>{score}</div>
      <div className="text-xs text-gray-500">{label}</div>
    </div>
  );
}

export function AtsPage() {
  const [jobDescription, setJobDescription] = useState("");
  const [resumes, setResumes] = useState<StoredResume[]>([]);
  const [selectedResumeId, setSelectedResumeId] = useState("");
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [useUpload, setUseUpload] = useState(false);
  const [provider, setProvider] = useState("gemini");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<AtsResult | null>(null);

  useEffect(() => {
    chrome.storage.session.get("lastExtraction", (res) => {
      const extraction = res.lastExtraction as ExtractionResult | undefined;
      if (extraction?.jobDescription) {
        setJobDescription(extraction.jobDescription);
      }
    });

    listResumes().then((resp) => {
      if (resp.data) {
        setResumes(resp.data);
        if (resp.data.length > 0) setSelectedResumeId(resp.data[0].id);
      }
    });
  }, []);

  const handleAnalyse = async () => {
    if (!jobDescription.trim()) {
      setError("Job description is required");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    try {
      const resp = await analyseAts({
        storedResumeId: !useUpload ? selectedResumeId : undefined,
        file: useUpload ? uploadFile! : undefined,
        jobDescription,
        provider,
      });

      if (resp.data) {
        setResult(resp.data);
      } else {
        setError(resp.message || "Analysis failed");
      }
    } catch (err: any) {
      const detail = err?.response?.data?.detail;
      setError(typeof detail === "string" ? detail : "ATS analysis failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-semibold text-gray-900">ATS Analysis</h2>

      {error && (
        <div className="p-2 bg-red-50 text-red-700 text-xs rounded">{error}</div>
      )}

      {!result ? (
        <>
          {/* Job Description */}
          <div>
            <label className="text-sm font-medium text-gray-700 block mb-1">
              Job Description
            </label>
            <textarea
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-xs resize-y focus:ring-2 focus:ring-blue-500"
              placeholder="Paste job description..."
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
                type="file"
                accept=".pdf"
                onChange={(e) => setUploadFile(e.target.files?.[0] ?? null)}
                className="w-full text-xs"
              />
            )}
          </div>

          <button
            onClick={handleAnalyse}
            disabled={loading}
            className="w-full py-2 px-4 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? "Analysing..." : "Analyse ATS Score"}
          </button>
        </>
      ) : (
        /* Results View */
        <div className="space-y-4">
          <div className="flex justify-center">
            <ScoreRing score={result.overall_score} label={result.score_label} />
          </div>

          {/* Section Scores */}
          <div className="grid grid-cols-2 gap-2">
            {Object.entries(result.section_scores).map(([key, val]) => (
              <div key={key} className="bg-gray-50 p-2 rounded text-center">
                <div className="text-sm font-medium">{val}</div>
                <div className="text-xs text-gray-500">
                  {key.replace(/_/g, " ")}
                </div>
              </div>
            ))}
          </div>

          {/* Keywords */}
          <div>
            <p className="text-xs font-medium text-gray-700 mb-1">Keywords Found</p>
            <div className="flex flex-wrap gap-1">
              {result.keywords_present.map((kw) => (
                <span key={kw} className="px-2 py-0.5 bg-green-100 text-green-700 rounded-full text-xs">
                  {kw}
                </span>
              ))}
            </div>
          </div>

          <div>
            <p className="text-xs font-medium text-gray-700 mb-1">Keywords Missing</p>
            <div className="flex flex-wrap gap-1">
              {result.keywords_missing.map((kw) => (
                <span key={kw} className="px-2 py-0.5 bg-red-100 text-red-700 rounded-full text-xs">
                  {kw}
                </span>
              ))}
            </div>
          </div>

          {/* Suggestions */}
          {result.suggestions.length > 0 && (
            <div>
              <p className="text-xs font-medium text-gray-700 mb-1">Suggestions</p>
              <ul className="space-y-1">
                {result.suggestions.map((s, i) => (
                  <li key={i} className="text-xs text-gray-600">• {s}</li>
                ))}
              </ul>
            </div>
          )}

          <p className="text-xs text-gray-500">{result.summary}</p>

          <button
            onClick={() => setResult(null)}
            className="w-full py-2 text-sm text-blue-600 hover:text-blue-800"
          >
            Analyse Again
          </button>
        </div>
      )}
    </div>
  );
}
