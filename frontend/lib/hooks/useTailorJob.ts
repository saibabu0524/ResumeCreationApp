"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { tailorResume } from "@/lib/api/resume";

export function useTailorJob() {
    return useMutation<ArrayBuffer, Error, { file: File; jobDescription: string }>({
        mutationFn: ({ file, jobDescription }) => tailorResume(file, jobDescription),
        onError: (error) => {
            toast.error(error.message ?? "Resume tailoring failed");
        },
        onSuccess: () => {
            toast.success("Resume tailored successfully!");
        },
    });
}
