"use client";

import { useEffect, useRef } from "react";

interface ATSScoreRingProps {
    score: number; // 0-100
    size?: number;
    /** Show the coloured label badge below the ring. Defaults to false. */
    showBadge?: boolean;
}

function getColor(score: number): string {
    // Mirrors Android SemanticSuccess / SemanticWarning / SemanticError tokens
    if (score >= 70) return "#4A7C59"; // SemanticSuccess
    if (score >= 40) return "#C08030"; // SemanticWarning
    return "#B04A3A"; // SemanticError
}

function getLabel(score: number): string {
    if (score >= 70) return "Great";
    if (score >= 40) return "Fair";
    return "Poor";
}

export function ATSScoreRing({ score, size = 160, showBadge = false }: ATSScoreRingProps) {
    const circleRef = useRef<SVGCircleElement>(null);
    const radius = (size - 16) / 2; // stroke-width = 8 each side
    const circumference = 2 * Math.PI * radius;
    const color = getColor(score);
    const label = getLabel(score);

    useEffect(() => {
        if (!circleRef.current) return;
        // Animate from 0 to target on mount
        circleRef.current.style.transition = "stroke-dashoffset 1s ease-out";
        const offset = circumference - (score / 100) * circumference;
        circleRef.current.style.strokeDashoffset = String(offset);
    }, [score, circumference]);

    return (
        <div className="flex flex-col items-center gap-3">
            <svg
                width={size}
                height={size}
                viewBox={`0 0 ${size} ${size}`}
                role="img"
                aria-label={`ATS score: ${score}%`}
            >
                {/* Background track */}
                <circle
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth={8}
                    className="text-muted"
                />
                {/* Score arc — starts full, animated to target via useEffect */}
                <circle
                    ref={circleRef}
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    fill="none"
                    stroke={color}
                    strokeWidth={8}
                    strokeLinecap="round"
                    strokeDasharray={circumference}
                    strokeDashoffset={circumference} // start at 0% (fully hidden)
                    transform={`rotate(-90 ${size / 2} ${size / 2})`}
                    style={{ filter: `drop-shadow(0 0 6px ${color}80)` }}
                />
                {/* Score text */}
                <text
                    x="50%"
                    y="46%"
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize="26"
                    fontWeight="bold"
                    fill={color}
                >
                    {score}
                </text>
                <text
                    x="50%"
                    y="62%"
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize="11"
                    fill="currentColor"
                    opacity={0.6}
                >
                    / 100
                </text>
            </svg>
            {showBadge && (
                <span
                    className="rounded-full px-3 py-1 text-xs font-semibold ring-1"
                    style={{
                        color,
                        backgroundColor: `${color}15`,
                        boxShadow: `0 0 0 1px ${color}30`,
                    }}
                >
                    {label} Match
                </span>
            )}
        </div>
    );
}
