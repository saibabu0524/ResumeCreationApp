import type { Metadata } from "next";
import { RegisterForm } from "@/components/auth/RegisterForm";

export const metadata: Metadata = {
    title: "Create Account",
    description: "Create a new ResumeAI account",
};

export default function RegisterPage() {
    return <RegisterForm />;
}
