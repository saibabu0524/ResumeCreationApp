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
                    // SemanticSuccess #4A7C59
                    ? "bg-[#4A7C59]/10 text-[#4A7C59] dark:text-[#6AAF80] ring-[#4A7C59]/25"
                    // SemanticError #B04A3A
                    : "bg-[#B04A3A]/10 text-[#B04A3A] dark:text-[#E06050] ring-[#B04A3A]/25"
                }`}
        >
            {variant === "matched" ? "✓ " : "✗ "}
            {word}
        </span>
    );
}

export function KeywordPills({ matched, missing }: KeywordPillsProps) {
    return (
        <div className="space-y-4">
            {matched.length > 0 && (
                <div>
                    <p className="mb-2.5 text-sm font-semibold text-[#4A7C59] dark:text-[#6AAF80]">
                        Matched Keywords
                        <span className="ml-1.5 text-xs font-normal text-muted-foreground">({matched.length})</span>
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
                    <p className="mb-2.5 text-sm font-semibold text-[#B04A3A] dark:text-[#E06050]">
                        Missing Keywords
                        <span className="ml-1.5 text-xs font-normal text-muted-foreground">({missing.length})</span>
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
