"use client";

import { useQuery } from "@tanstack/react-query";
import { FileText } from "lucide-react";
import { getResumeHistory } from "@/lib/api/resume";
import { format } from "date-fns";
import { Download, Clock, CheckCircle } from "lucide-react";

function ScoreChip({ score }: { score: number }) {
    const color =
        score >= 70
            ? "text-[#4A7C59] dark:text-[#6AAF80] bg-[#4A7C59]/10 ring-[#4A7C59]/20"
            : score >= 40
                ? "text-[#C08030] bg-[#C08030]/10 ring-[#C08030]/20"
                : "text-[#B04A3A] dark:text-[#E06050] bg-[#B04A3A]/10 ring-[#B04A3A]/20";
    return (
        <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${color}`}>
            <CheckCircle className="h-3 w-3" />
            {score}% ATS
        </span>
    );
}

function SkeletonCard() {
    return (
        <div className="glass rounded-xl p-5">
            <div className="flex items-start gap-3">
                <div className="skeleton h-9 w-9 rounded-lg" />
                <div className="flex-1 space-y-2">
                    <div className="skeleton h-4 w-3/4 rounded" />
                    <div className="skeleton h-3 w-full rounded" />
                </div>
            </div>
            <div className="mt-4 flex items-center justify-between">
                <div className="skeleton h-3 w-24 rounded" />
                <div className="skeleton h-7 w-24 rounded-lg" />
            </div>
        </div>
    );
}

// The history API returns a list of raw dicts — we read keys dynamically
function HistoryItem({ item }: { item: Record<string, unknown> }) {
    const filename = (item.tailored_filename ?? item.original_filename ?? "Resume") as string;
    const createdAt = item.created_at as string | undefined;
    const atsScore = item.ats_score as number | undefined | null;
    const downloadUrl = item.download_url as string | undefined;

    return (
        <div className="glass rounded-xl p-5 transition-all duration-200 hover:ring-1 hover:ring-primary/30 hover:-translate-y-0.5">
            <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3 min-w-0">
                    <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 ring-1 ring-primary/20">
                        <FileText className="h-4 w-4 text-primary" />
                    </div>
                    <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-foreground">{filename}</p>
                        {typeof item.job_description_preview === "string" && (
                            <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">
                                {item.job_description_preview}
                            </p>
                        )}
                    </div>
                </div>
                {typeof atsScore === "number" && <ScoreChip score={atsScore} />}
            </div>

            <div className="mt-4 flex items-center justify-between">
                <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Clock className="h-3 w-3" />
                    {createdAt ? format(new Date(createdAt), "MMM d, yyyy") : "—"}
                </span>
                {downloadUrl && (
                    <a
                        href={downloadUrl}
                        download={filename}
                        className="flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary
              ring-1 ring-primary/20 hover:bg-primary/20 transition-colors duration-150"
                    >
                        <Download className="h-3 w-3" />
                        Download
                    </a>
                )}
            </div>
        </div>
    );
}

export function ResumeHistoryList() {
    const { data, isLoading, isError, error } = useQuery({
        queryKey: ["resume-history"],
        queryFn: () => getResumeHistory(),
    });

    if (isLoading) {
        return (
            <div className="grid gap-4 sm:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3">
                {Array.from({ length: 6 }).map((_, i) => (
                    <SkeletonCard key={i} />
                ))}
            </div>
        );
    }

    if (isError) {
        return (
            <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-center">
                <p className="text-sm font-medium text-destructive">Failed to load resume history</p>
                <p className="mt-1 text-xs text-muted-foreground">
                    {error instanceof Error ? error.message : "Unknown error"}
                </p>
            </div>
        );
    }

    if (!data || data.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border py-20 text-center">
                <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-muted">
                    <FileText className="h-7 w-7 text-muted-foreground" />
                </div>
                <p className="text-base font-medium text-foreground">No resumes yet</p>
                <p className="mt-1 text-sm text-muted-foreground">
                    Use the Tailor Studio to create your first tailored resume
                </p>
            </div>
        );
    }

    return (
        <div className="grid gap-4 sm:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3">
            {data.map((item, idx) => (
                <HistoryItem key={(item.id as string) ?? idx} item={item} />
            ))}
        </div>
    );
}
