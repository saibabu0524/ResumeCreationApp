"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useState } from "react";
import { tailorResume } from "@/lib/api/resume";

export function useTailorJob() {
    const [progressMessage, setProgressMessage] = useState<string | null>(null);

    const mutation = useMutation<ArrayBuffer, Error, { file: File; jobDescription: string }>({
        mutationFn: ({ file, jobDescription }) =>
            tailorResume(file, jobDescription, setProgressMessage),
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
