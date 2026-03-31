import React, { useEffect, useState, useRef } from "react";
import { listResumes, uploadResume, deleteResume } from "../../lib/api-resume";
import type { StoredResume } from "../../types";

export function ResumesPage() {
  const [resumes, setResumes] = useState<StoredResume[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);

  const loadResumes = async () => {
    try {
      const resp = await listResumes();
      if (resp.data) setResumes(resp.data);
    } catch {
      setError("Failed to load resumes");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadResumes();
  }, []);

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setError("");

    try {
      await uploadResume(file);
      await loadResumes();
    } catch (err: any) {
      const detail = err?.response?.data?.detail;
      setError(typeof detail === "string" ? detail : "Upload failed");
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = "";
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteResume(id);
      setResumes((prev) => prev.filter((r) => r.id !== id));
    } catch {
      setError("Failed to delete resume");
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="p-4 space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-lg font-semibold text-gray-900">My Resumes</h2>
        <label className="cursor-pointer text-xs px-3 py-1 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          {uploading ? "Uploading..." : "Upload PDF"}
          <input
            ref={fileRef}
            type="file"
            accept=".pdf"
            onChange={handleUpload}
            className="hidden"
            disabled={uploading}
          />
        </label>
      </div>

      {error && (
        <div className="p-2 bg-red-50 text-red-700 text-xs rounded">{error}</div>
      )}

      {loading ? (
        <div className="text-center text-sm text-gray-400 py-8">Loading...</div>
      ) : resumes.length === 0 ? (
        <div className="text-center text-sm text-gray-400 py-8">
          No resumes uploaded yet. Upload a PDF to get started.
        </div>
      ) : (
        <div className="space-y-2">
          {resumes.map((r) => (
            <div
              key={r.id}
              className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
            >
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {r.original_filename}
                </p>
                <p className="text-xs text-gray-400">
                  {formatSize(r.file_size_bytes)} &middot;{" "}
                  {new Date(r.created_at).toLocaleDateString()}
                </p>
              </div>
              <button
                onClick={() => handleDelete(r.id)}
                className="ml-2 text-xs text-red-500 hover:text-red-700"
              >
                Delete
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
