import type { ResumeHistoryItem } from "@/types/api";
import { FileText, Download, Clock, CheckCircle } from "lucide-react";
import { format } from "date-fns";

interface ResumeCardProps {
    item: ResumeHistoryItem;
}

function ScoreChip({ score }: { score: number }) {
    // Uses Android semantic colour tokens directly
    const color =
        score >= 70
            ? "text-[#4A7C59] dark:text-[#6AAF80] bg-[#4A7C59]/10 ring-[#4A7C59]/20"   // SemanticSuccess
            : score >= 40
                ? "text-[#C08030] bg-[#C08030]/10 ring-[#C08030]/20"                     // SemanticWarning
                : "text-[#B04A3A] dark:text-[#E06050] bg-[#B04A3A]/10 ring-[#B04A3A]/20"; // SemanticError

    return (
        <span
            className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${color}`}
        >
            <CheckCircle className="h-3 w-3" />
            {score}% ATS
        </span>
    );
}

export function ResumeCard({ item }: ResumeCardProps) {
    return (
        <div className="glass rounded-xl p-5 transition-all duration-200 hover:ring-1 hover:ring-primary/30 hover:-translate-y-0.5">
            <div className="flex items-start justify-between gap-4">
                {/* Icon + name */}
                <div className="flex items-start gap-3 min-w-0">
                    <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 ring-1 ring-primary/20">
                        <FileText className="h-4 w-4 text-primary" />
                    </div>
                    <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-foreground">
                            {item.tailored_filename}
                        </p>
                        <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">
                            {item.job_description_preview}
                        </p>
                    </div>
                </div>

                {/* Score chip */}
                {item.ats_score !== null && <ScoreChip score={item.ats_score} />}
            </div>

            <div className="mt-4 flex items-center justify-between">
                <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Clock className="h-3 w-3" />
                    {format(new Date(item.created_at), "MMM d, yyyy")}
                </span>

                <a
                    href={item.download_url}
                    download={item.tailored_filename}
                    id={`download-resume-${item.id}`}
                    className="flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary
            ring-1 ring-primary/20 hover:bg-primary/20 transition-colors duration-150"
                >
                    <Download className="h-3 w-3" />
                    Download
                </a>
            </div>
        </div>
    );
}
