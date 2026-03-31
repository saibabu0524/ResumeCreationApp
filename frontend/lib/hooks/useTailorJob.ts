"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useState } from "react";
import { tailorResume } from "@/lib/api/resume";

export function useTailorJob() {
    const [progressMessage, setProgressMessage] = useState<string | null>(null);

    const mutation = useMutation<ArrayBuffer, Error, { file: File; jobDescription: string; provider: string; model?: string | null }>({
        mutationFn: ({ file, jobDescription, provider, model }) =>
            tailorResume(file, jobDescription, setProgressMessage, provider, model),
        onMutate: () => {
            setProgressMessage("Uploading resume\u2026");
        },
        onError: (error) => {
            setProgressMessage(null);
            toast.error(error.message ?? "Resume tailoring failed");
        },
        onSuccess: () => {
            setProgressMessage(null);
            toast.success("Resume tailored successfully!");
        },
    });

    return { ...mutation, progressMessage };
}
