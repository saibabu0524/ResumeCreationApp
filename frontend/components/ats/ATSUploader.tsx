"use client";

import { useCallback, useState } from "react";
import { useDropzone, type FileRejection } from "react-dropzone";
import {
    Upload,
    FileText,
    X,
    Loader2,
    Sparkles,
    CheckCircle2,
    AlertCircle,
    Lightbulb,
    TrendingUp,
} from "lucide-react";
import { useATSAnalysis } from "@/lib/hooks/useATSAnalysis";
import { ATSScoreRing } from "./ATSScoreRing";
import { KeywordPills } from "./KeywordPills";

const MAX_SIZE = 5 * 1024 * 1024;

function getScoreLabel(score: number): { label: string; color: string } {
    // Mirrors Android SemanticSuccess / SemanticWarning / SemanticError
    if (score >= 80) return { label: "Excellent Match", color: "text-[#4A7C59] dark:text-[#6AAF80]" };
    if (score >= 60) return { label: "Good Match",      color: "text-[#C08030]" };
    if (score >= 40) return { label: "Fair Match",      color: "text-[#C08030]" };
    return             { label: "Poor Match",       color: "text-[#B04A3A] dark:text-[#E06050]" };
}

export function ATSUploader() {
    const [file, setFile] = useState<File | null>(null);
    const [jobDescription, setJobDescription] = useState("");
    const [fileError, setFileError] = useState<string | null>(null);
    const [jdError, setJdError] = useState<string | null>(null);

    const { mutate, isPending, data, isError, error } = useATSAnalysis();

    const onDrop = useCallback((accepted: File[], rejected: FileRejection[]) => {
        setFileError(null);
        if (rejected.length > 0) {
            setFileError(rejected[0].errors[0]?.message ?? "Invalid file");
            return;
        }
        if (accepted[0]) setFile(accepted[0]);
    }, []);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        accept: { "application/pdf": [".pdf"] },
        maxFiles: 1,
        maxSize: MAX_SIZE,
    });

    function handleAnalyse() {
        setJdError(null);
        if (!file) { setFileError("Please upload a PDF resume"); return; }
        if (jobDescription.trim().length < 50) {
            setJdError("Job description must be at least 50 characters");
            return;
        }
        mutate({ file, jobDescription });
    }

    return (
        <div className="grid gap-6 lg:grid-cols-2 items-start">
            {/* ── LEFT — Upload panel ───────────────────────────────────────── */}
            <div className="space-y-4">
                {/* Dropzone */}
                <div
                    {...getRootProps()}
                    id="ats-dropzone"
                    className={`relative cursor-pointer rounded-xl border-2 border-dashed p-8 text-center transition-all duration-200
            ${isDragActive ? "border-primary bg-primary/5" : "border-border hover:border-primary/50 hover:bg-muted/30"}
            ${fileError ? "border-destructive/50" : ""}`}
                >
                    <input {...getInputProps()} id="ats-file-input" />
                    {file ? (
                        <div className="flex items-center justify-between gap-3">
                            <div className="flex items-center gap-2 min-w-0">
                                <FileText className="h-5 w-5 text-primary shrink-0" />
                                <span className="truncate text-sm font-medium text-foreground">{file.name}</span>
                            </div>
                            <button
                                type="button"
                                onClick={(e) => { e.stopPropagation(); setFile(null); setFileError(null); }}
                                className="shrink-0 text-muted-foreground hover:text-destructive transition-colors"
                                aria-label="Remove file"
                            >
                                <X className="h-4 w-4" />
                            </button>
                        </div>
                    ) : (
                        <>
                            <Upload className="mx-auto mb-3 h-8 w-8 text-muted-foreground" />
                            <p className="text-sm font-medium text-foreground">
                                {isDragActive ? "Drop your PDF here" : "Drag PDF here or click to browse"}
                            </p>
                            <p className="mt-1 text-xs text-muted-foreground">PDF only · Max 5 MB</p>
                        </>
                    )}
                </div>
                {fileError && <p className="text-xs text-destructive">{fileError}</p>}

                {/* Job Description */}
                <div className="space-y-1.5">
                    <label htmlFor="ats-jd" className="block text-sm font-medium text-foreground">
                        Job Description
                    </label>
                    <textarea
                        id="ats-jd"
                        rows={8}
                        placeholder="Paste the full job description here…"
                        value={jobDescription}
                        onChange={(e) => setJobDescription(e.target.value)}
                        className={`w-full resize-none rounded-lg border bg-background px-3 py-2.5 text-sm outline-none transition-all duration-200
              focus:ring-2 focus:ring-primary/50 focus:border-primary font-mono
              ${jdError ? "border-destructive" : "border-border"}`}
                    />
                    <div className="flex items-center justify-between">
                        {jdError
                            ? <p className="text-xs text-destructive">{jdError}</p>
                            : <span />}
                        <span className="text-xs text-muted-foreground">{jobDescription.length} chars</span>
                    </div>
                </div>

                <button
                    id="ats-analyse-btn"
                    type="button"
                    onClick={handleAnalyse}
                    disabled={isPending}
                    className="w-full rounded-lg bg-primary py-3 text-sm font-semibold text-primary-foreground
            transition-all duration-150 hover:opacity-90 active:scale-[0.98]
            disabled:cursor-not-allowed disabled:opacity-60 flex items-center justify-center gap-2"
                >
                    {isPending ? (
                        <><Loader2 className="h-4 w-4 animate-spin" />Analysing…</>
                    ) : (
                        <><Sparkles className="h-4 w-4" />Analyse Resume</>
                    )}
                </button>
            </div>

            {/* ── RIGHT — Results panel ─────────────────────────────────────── */}
            <div className="glass rounded-xl overflow-hidden">
                {/* Loading skeleton */}
                {isPending && (
                    <div className="flex flex-col items-center justify-center py-16 px-6 gap-5">
                        <div className="skeleton h-44 w-44 rounded-full" />
                        <div className="space-y-2 w-full max-w-[220px]">
                            <div className="skeleton h-4 w-full rounded" />
                            <div className="skeleton h-4 w-3/4 rounded mx-auto" />
                        </div>
                        <div className="skeleton h-3 w-36 rounded" />
                    </div>
                )}

                {/* Empty state */}
                {!isPending && !data && !isError && (
                    <div className="flex flex-col items-center justify-center py-16 px-6 text-center text-muted-foreground">
                        <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-muted/60">
                            <Sparkles className="h-8 w-8 opacity-40" />
                        </div>
                        <p className="text-base font-medium text-foreground/70">No results yet</p>
                        <p className="mt-1 text-sm">Upload a resume and paste a job description to check your ATS compatibility score.</p>
                    </div>
                )}

                {/* Error state */}
                {isError && (
                    <div className="m-4 flex items-start gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-4">
                        <AlertCircle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
                        <div>
                            <p className="text-sm font-medium text-destructive">Analysis failed</p>
                            <p className="mt-0.5 text-sm text-destructive/80">{error?.message ?? "Please try again."}</p>
                        </div>
                    </div>
                )}

                {/* Results */}
                {data && !isPending && (() => {
                    const { label, color } = getScoreLabel(data.overall_score);
                    return (
                        <div className="divide-y divide-border/50">
                            {/* Score section */}
                            <div className="flex flex-col items-center gap-2 px-6 py-8">
                                <ATSScoreRing score={data.overall_score} size={180} />
                                <span className={`text-base font-semibold ${color}`}>{label}</span>
                                <p className="text-sm text-muted-foreground text-center">
                                    Your resume scored <strong className={color}>{data.overall_score}%</strong> against this job description
                                </p>
                            </div>

                            {/* Summary */}
                            {data.summary && (
                                <div className="px-5 py-4">
                                    <div className="flex items-center gap-2 mb-2">
                                        <TrendingUp className="h-4 w-4 text-primary shrink-0" />
                                        <p className="text-sm font-semibold text-foreground">Summary</p>
                                    </div>
                                    <p className="text-sm text-muted-foreground leading-relaxed">{data.summary}</p>
                                </div>
                            )}

                            {/* Strengths */}
                            {data.strengths && data.strengths.length > 0 && (
                                <div className="px-5 py-4">
                                    <div className="flex items-center gap-2 mb-3">
                                        <CheckCircle2 className="h-4 w-4 text-[#4A7C59] dark:text-[#6AAF80] shrink-0" />
                                        <p className="text-sm font-semibold text-foreground">
                                            Strengths
                                            <span className="ml-1.5 text-xs font-normal text-muted-foreground">({data.strengths.length})</span>
                                        </p>
                                    </div>
                                    <ul className="space-y-2">
                                        {data.strengths.map((s, i) => (
                                            <li key={i} className="flex items-start gap-2 text-sm text-foreground">
                                                <span className="mt-0.5 text-[#4A7C59] dark:text-[#6AAF80] shrink-0">✓</span>
                                                <span>{s}</span>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            {/* Suggestions */}
                            {data.suggestions && data.suggestions.length > 0 && (
                                <div className="px-5 py-4">
                                    <div className="flex items-center gap-2 mb-3">
                                        <Lightbulb className="h-4 w-4 text-[#C08030] shrink-0" />
                                        <p className="text-sm font-semibold text-foreground">
                                            Suggestions
                                            <span className="ml-1.5 text-xs font-normal text-muted-foreground">({data.suggestions.length})</span>
                                        </p>
                                    </div>
                                    <ul className="space-y-2">
                                        {data.suggestions.map((s, i) => (
                                            <li key={i} className="flex items-start gap-2 text-sm text-foreground/80">
                                                <span className="mt-0.5 text-[#C08030] shrink-0">•</span>
                                                <span>{s}</span>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            {/* Keywords */}
                            {((data.keywords_present?.length ?? 0) > 0 || (data.keywords_missing?.length ?? 0) > 0) && (
                                <div className="px-5 py-4">
                                    <KeywordPills
                                        matched={data.keywords_present ?? []}
                                        missing={data.keywords_missing ?? []}
                                    />
                                </div>
                            )}
                        </div>
                    );
                })()}
            </div>
        </div>
    );
}

