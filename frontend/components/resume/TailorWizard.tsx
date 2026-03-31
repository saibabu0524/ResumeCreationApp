"use client";

import { useState, useCallback } from "react";
import { useDropzone, type FileRejection } from "react-dropzone";
import {
    Upload, FileText, X, Loader2, Wand2, CheckCircle, Download, ChevronRight
} from "lucide-react";
import { useTailorJob } from "@/lib/hooks/useTailorJob";

const STEPS = [
    { id: 1, label: "Upload" },
    { id: 2, label: "Describe" },
    { id: 3, label: "Tailor" },
    { id: 4, label: "Download" },
];

const MAX_SIZE = 5 * 1024 * 1024;

const MODEL_OPTIONS = [
    { id: "gemini-flash", label: "Gemini 2.0 Flash", sub: "Fast · Recommended", provider: "gemini", model: "gemini-2.0-flash" },
    { id: "gemini-pro", label: "Gemini 2.5 Pro", sub: "Powerful · Slower", provider: "gemini", model: "gemini-2.5-pro" },
    { id: "kimi-8k", label: "Kimi 8K", sub: "Moonshot · 8K context", provider: "cloud", model: "moonshot-v1-8k" },
    { id: "kimi-32k", label: "Kimi 32K", sub: "Moonshot · 32K context", provider: "cloud", model: "moonshot-v1-32k" },
    { id: "kimi-128k", label: "Kimi 128K", sub: "Moonshot · 128K context", provider: "cloud", model: "moonshot-v1-128k" },
    { id: "qwen-72b", label: "Qwen 2.5 72B", sub: "SiliconFlow · Powerful", provider: "qwen", model: "Qwen/Qwen2.5-72B-Instruct" },
    { id: "ollama", label: "Ollama", sub: "Local / Custom", provider: "ollama", model: null },
];

export function TailorWizard() {
    const [step, setStep] = useState(1);
    const [file, setFile] = useState<File | null>(null);
    const [fileError, setFileError] = useState<string | null>(null);
    const [jobDescription, setJobDescription] = useState("");
    const [jdError, setJdError] = useState<string | null>(null);
    const [downloadUrl, setDownloadUrl] = useState<string | null>(null);
    const [selectedModelId, setSelectedModelId] = useState("gemini-flash");

    const { mutate, isPending, isError, error, progressMessage } = useTailorJob();

    const selectedModel = MODEL_OPTIONS.find((m) => m.id === selectedModelId) ?? MODEL_OPTIONS[0];

    const onDrop = useCallback((accepted: File[], rejected: FileRejection[]) => {
        setFileError(null);
        if (rejected.length > 0) {
            setFileError(rejected[0].errors[0]?.message ?? "Invalid file");
            return;
        }
        if (accepted[0]) { setFile(accepted[0]); }
    }, []);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        accept: { "application/pdf": [".pdf"] },
        maxFiles: 1,
        maxSize: MAX_SIZE,
    });

    function goToStep2() {
        if (!file) { setFileError("Please upload a PDF resume"); return; }
        setFileError(null);
        setStep(2);
    }

    function goToStep3() {
        if (jobDescription.trim().length < 50) {
            setJdError("Job description must be at least 50 characters");
            return;
        }
        setJdError(null);
        setStep(3);
        // Kick off tailoring
        mutate(
            { file: file!, jobDescription, provider: selectedModel.provider, model: selectedModel.model },
            {
                onSuccess: (arrayBuffer) => {
                    const blob = new Blob([arrayBuffer], { type: "application/pdf" });
                    const url = URL.createObjectURL(blob);
                    setDownloadUrl(url);
                    setStep(4);
                },
            }
        );
    }

    function reset() {
        setStep(1);
        setFile(null);
        setJobDescription("");
        setFileError(null);
        setJdError(null);
        if (downloadUrl) URL.revokeObjectURL(downloadUrl);
        setDownloadUrl(null);
    }

    return (
        <div className="space-y-8">
            {/* Step progress */}
            <div className="flex items-center gap-2">
                {STEPS.map((s, idx) => (
                    <div key={s.id} className="flex items-center gap-2">
                        <div
                            className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold transition-colors duration-300
                ${step > s.id ? "bg-emerald-500 text-white" :
                                    step === s.id ? "bg-primary text-white" :
                                        "bg-muted text-muted-foreground"}`}
                        >
                            {step > s.id ? <CheckCircle className="h-4 w-4" /> : s.id}
                        </div>
                        <span className={`text-sm font-medium hidden sm:block ${step === s.id ? "text-foreground" : "text-muted-foreground"}`}>
                            {s.label}
                        </span>
                        {idx < STEPS.length - 1 && (
                            <ChevronRight className="h-4 w-4 text-muted-foreground mx-1" />
                        )}
                    </div>
                ))}
            </div>

            {/* Step 1 — Upload */}
            {step === 1 && (
                <div className="glass rounded-xl p-8 space-y-5">
                    <div>
                        <h2 className="text-lg font-semibold text-foreground">Upload Your Resume</h2>
                        <p className="text-sm text-muted-foreground mt-1">PDF format only, max 5 MB</p>
                    </div>
                    <div
                        {...getRootProps()}
                        id="tailor-dropzone"
                        className={`cursor-pointer rounded-xl border-2 border-dashed p-12 text-center transition-all duration-200
              ${isDragActive ? "border-primary bg-primary/5" : "border-border hover:border-primary/50 hover:bg-muted/30"}
              ${fileError ? "border-destructive/50" : ""}`}
                    >
                        <input {...getInputProps()} id="tailor-file-input" />
                        {file ? (
                            <div className="flex items-center justify-center gap-3">
                                <FileText className="h-6 w-6 text-primary" />
                                <span className="text-sm font-medium text-foreground">{file.name}</span>
                                <button
                                    type="button"
                                    onClick={(e) => { e.stopPropagation(); setFile(null); setFileError(null); }}
                                    className="ml-2 text-muted-foreground hover:text-destructive"
                                    aria-label="Remove file"
                                >
                                    <X className="h-4 w-4" />
                                </button>
                            </div>
                        ) : (
                            <>
                                <Upload className="mx-auto mb-3 h-10 w-10 text-muted-foreground" />
                                <p className="text-sm font-medium text-foreground">
                                    {isDragActive ? "Drop it here!" : "Drag & drop your resume PDF"}
                                </p>
                                <p className="mt-1 text-xs text-muted-foreground">or click to browse</p>
                            </>
                        )}
                    </div>
                    {fileError && <p className="text-xs text-destructive">{fileError}</p>}
                    <div className="flex justify-end">
                        <button
                            id="tailor-next-1"
                            type="button"
                            onClick={goToStep2}
                            className="rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground
                transition-all duration-150 hover:opacity-90 active:scale-[0.98] flex items-center gap-2"
                        >
                            Next <ChevronRight className="h-4 w-4" />
                        </button>
                    </div>
                </div>
            )}

            {/* Step 2 — Job Description */}
            {step === 2 && (
                <div className="glass rounded-xl p-8 space-y-5">
                    <div>
                        <h2 className="text-lg font-semibold text-foreground">Paste Job Description</h2>
                        <p className="text-sm text-muted-foreground mt-1">The more detail you provide, the better the tailoring</p>
                    </div>
                    <div className="space-y-1.5">
                        <textarea
                            id="tailor-jd"
                            rows={12}
                            placeholder="Paste the full job description here — responsibilities, requirements, skills…"
                            value={jobDescription}
                            onChange={(e) => setJobDescription(e.target.value)}
                            className={`w-full resize-none rounded-lg border bg-background px-3 py-2.5 text-sm outline-none
                transition-all duration-200 focus:ring-2 focus:ring-primary/50 focus:border-primary font-mono
                ${jdError ? "border-destructive" : "border-border"}`}
                        />
                        <div className="flex justify-between">
                            {jdError
                                ? <p className="text-xs text-destructive">{jdError}</p>
                                : <span className="text-xs text-muted-foreground">Minimum 50 characters</span>}
                            <span className="text-xs text-muted-foreground">{jobDescription.length} chars</span>
                        </div>
                    </div>

                    <div className="space-y-3 rounded-xl border border-border bg-muted/40 p-4">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-semibold text-foreground">AI Model</p>
                                <p className="text-xs text-muted-foreground">Choose provider + model</p>
                            </div>
                            <span className="rounded-full bg-primary/10 px-3 py-1 text-[11px] font-semibold text-primary">{selectedModel.label}</span>
                        </div>
                        <div className="space-y-2">
                            {MODEL_OPTIONS.map((opt) => (
                                <label
                                    key={opt.id}
                                    className={`flex cursor-pointer items-start gap-3 rounded-lg border px-3 py-2 text-sm transition-colors ${
                                        selectedModelId === opt.id
                                            ? "border-primary bg-primary/5"
                                            : "border-border hover:border-primary/40"
                                    }`}
                                >
                                    <input
                                        type="radio"
                                        name="model"
                                        value={opt.id}
                                        checked={selectedModelId === opt.id}
                                        onChange={() => setSelectedModelId(opt.id)}
                                        className="mt-1 h-4 w-4 accent-primary"
                                    />
                                    <div className="flex flex-col gap-0.5">
                                        <span className="font-semibold text-foreground">{opt.label}</span>
                                        <span className="text-xs text-muted-foreground">{opt.sub}</span>
                                        <span className="text-[11px] font-mono text-primary">{opt.provider.toUpperCase()} {opt.model ? `· ${opt.model}` : ""}</span>
                                    </div>
                                </label>
                            ))}
                        </div>
                    </div>
                    <div className="flex justify-between">
                        <button
                            type="button"
                            onClick={() => setStep(1)}
                            className="rounded-lg border border-border px-5 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted transition-colors"
                        >
                            Back
                        </button>
                        <button
                            id="tailor-next-2"
                            type="button"
                            onClick={goToStep3}
                            className="rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground
                transition-all duration-150 hover:opacity-90 active:scale-[0.98] flex items-center gap-2"
                        >
                            Tailor Resume <Wand2 className="h-4 w-4" />
                        </button>
                    </div>
                </div>
            )}

            {/* Step 3 — Processing */}
            {step === 3 && (
                <div className="glass rounded-xl p-12 text-center space-y-6">
                    {isPending && (
                        <>
                            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-primary/10">
                                <Loader2 className="h-10 w-10 animate-spin text-primary" />
                            </div>
                            <div>
                                <p className="text-lg font-semibold text-foreground">AI is tailoring your resume…</p>
                                <p className="mt-1 text-sm text-muted-foreground">
                                    {progressMessage ?? "This usually takes 10–20 seconds"}
                                </p>
                            </div>
                            <div className="mx-auto max-w-sm space-y-2">
                                <div className="skeleton h-3 w-full rounded" />
                                <div className="skeleton h-3 w-4/5 rounded mx-auto" />
                                <div className="skeleton h-3 w-3/5 rounded mx-auto" />
                            </div>
                        </>
                    )}
                    {isError && (
                        <div className="space-y-4">
                            <p className="text-base font-medium text-destructive">Tailoring failed</p>
                            <p className="text-sm text-muted-foreground">{error?.message}</p>
                            <button
                                type="button"
                                onClick={reset}
                                className="rounded-lg border border-border px-6 py-2.5 text-sm font-medium text-foreground hover:bg-muted"
                            >
                                Start over
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* Step 4 — Download */}
            {step === 4 && downloadUrl && (
                <div className="glass rounded-xl p-12 text-center space-y-6">
                    <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-emerald-500/10 ring-1 ring-emerald-500/20">
                        <CheckCircle className="h-10 w-10 text-emerald-400" />
                    </div>
                    <div>
                        <p className="text-lg font-semibold text-foreground">Your resume is ready!</p>
                        <p className="mt-1 text-sm text-muted-foreground">Download your AI-tailored resume below</p>
                    </div>
                    <div className="flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
                        <a
                            id="tailor-download-btn"
                            href={downloadUrl}
                            download="tailored-resume.pdf"
                            className="flex items-center gap-2 rounded-lg bg-emerald-500 px-6 py-2.5 text-sm font-semibold text-white
                transition-all duration-150 hover:bg-emerald-600 active:scale-[0.98]"
                        >
                            <Download className="h-4 w-4" />
                            Download PDF
                        </a>
                        <button
                            type="button"
                            onClick={reset}
                            className="rounded-lg border border-border px-6 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted transition-colors"
                        >
                            Tailor another
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
