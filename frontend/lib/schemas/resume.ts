import { z } from "zod";

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
const ACCEPTED_MIME = "application/pdf";

export const jobDescriptionSchema = z.object({
    job_description: z
        .string()
        .min(50, "Job description must be at least 50 characters")
        .max(10_000, "Job description is too long (max 10,000 characters)"),
});

export const fileUploadSchema = z
    .instanceof(typeof window !== "undefined" ? File : Object)
    .refine(
        (file) => file instanceof File && file.type === ACCEPTED_MIME,
        "Only PDF files are accepted"
    )
    .refine(
        (file) => file instanceof File && file.size <= MAX_FILE_SIZE,
        "File size must be 5 MB or less"
    );

export type JobDescriptionValues = z.infer<typeof jobDescriptionSchema>;
