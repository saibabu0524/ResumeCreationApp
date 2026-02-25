"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { analyseResume } from "@/lib/api/ats";
import type { ATSAnalysisResponse } from "@/types/api";

export function useATSAnalysis() {
    return useMutation<ATSAnalysisResponse, Error, { file: File; jobDescription: string }>({
        mutationFn: ({ file, jobDescription }) => analyseResume(file, jobDescription),
        onError: (error) => {
            toast.error(error.message ?? "ATS analysis failed");
        },
    });
}
