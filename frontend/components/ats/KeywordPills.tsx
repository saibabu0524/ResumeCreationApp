interface KeywordPillsProps {
    matched: string[];
    missing: string[];
}

function Pill({
    word,
    variant,
}: {
    word: string;
    variant: "matched" | "missing";
}) {
    return (
        <span
            className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ring-1 ${variant === "matched"
                    ? "bg-emerald-500/10 text-emerald-400 ring-emerald-500/20"
                    : "bg-red-500/10 text-red-400 ring-red-500/20"
                }`}
        >
            {variant === "matched" ? "✓ " : "✗ "}
            {word}
        </span>
    );
}

export function KeywordPills({ matched, missing }: KeywordPillsProps) {
    return (
        <div className="space-y-5">
            {matched.length > 0 && (
                <div>
                    <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-emerald-400">
                        Matched Keywords ({matched.length})
                    </p>
                    <div className="flex flex-wrap gap-2">
                        {matched.map((kw) => (
                            <Pill key={kw} word={kw} variant="matched" />
                        ))}
                    </div>
                </div>
            )}
            {missing.length > 0 && (
                <div>
                    <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-red-400">
                        Missing Keywords ({missing.length})
                    </p>
                    <div className="flex flex-wrap gap-2">
                        {missing.map((kw) => (
                            <Pill key={kw} word={kw} variant="missing" />
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
