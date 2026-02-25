"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, Loader2, Mail, Lock, AlertCircle } from "lucide-react";
import { registerSchema, type RegisterFormValues } from "@/lib/schemas/auth";
import { useAuth } from "@/lib/hooks/useAuth";

export function RegisterForm() {
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const { register: registerUser, registerIsPending } = useAuth();

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
    });

    const onSubmit = (data: RegisterFormValues) => {
        registerUser({
            email: data.email,
            password: data.password,
        });
    };

    return (
        <div className="glass rounded-2xl p-8">
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-foreground">Create account</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Start tailoring resumes with AI — it&apos;s free
                </p>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
                {/* Email */}
                <div className="space-y-1.5">
                    <label htmlFor="reg-email" className="block text-sm font-medium text-foreground">
                        Email address
                    </label>
                    <div className="relative">
                        <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                        <input
                            id="reg-email"
                            type="email"
                            autoComplete="email"
                            placeholder="you@example.com"
                            {...register("email")}
                            className={`w-full rounded-lg border bg-background py-2.5 pl-9 pr-4 text-sm outline-none transition-all duration-200
                focus:ring-2 focus:ring-primary/50 focus:border-primary
                ${errors.email ? "border-destructive ring-1 ring-destructive/30" : "border-border"}`}
                        />
                    </div>
                    {errors.email && (
                        <p className="flex items-center gap-1 text-xs text-destructive">
                            <AlertCircle className="h-3 w-3" />{errors.email.message}
                        </p>
                    )}
                </div>

                {/* Password */}
                <div className="space-y-1.5">
                    <label htmlFor="reg-password" className="block text-sm font-medium text-foreground">
                        Password
                    </label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                        <input
                            id="reg-password"
                            type={showPassword ? "text" : "password"}
                            autoComplete="new-password"
                            placeholder="Min. 8 characters"
                            {...register("password")}
                            className={`w-full rounded-lg border bg-background py-2.5 pl-9 pr-12 text-sm outline-none transition-all duration-200
                focus:ring-2 focus:ring-primary/50 focus:border-primary
                ${errors.password ? "border-destructive ring-1 ring-destructive/30" : "border-border"}`}
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword((v) => !v)}
                            aria-label={showPassword ? "Hide password" : "Show password"}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                        >
                            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                    </div>
                    {errors.password && (
                        <p className="flex items-center gap-1 text-xs text-destructive">
                            <AlertCircle className="h-3 w-3" />{errors.password.message}
                        </p>
                    )}
                </div>

                {/* Confirm Password */}
                <div className="space-y-1.5">
                    <label htmlFor="reg-confirm" className="block text-sm font-medium text-foreground">
                        Confirm password
                    </label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                        <input
                            id="reg-confirm"
                            type={showConfirm ? "text" : "password"}
                            autoComplete="new-password"
                            placeholder="••••••••"
                            {...register("confirmPassword")}
                            className={`w-full rounded-lg border bg-background py-2.5 pl-9 pr-12 text-sm outline-none transition-all duration-200
                focus:ring-2 focus:ring-primary/50 focus:border-primary
                ${errors.confirmPassword ? "border-destructive ring-1 ring-destructive/30" : "border-border"}`}
                        />
                        <button
                            type="button"
                            onClick={() => setShowConfirm((v) => !v)}
                            aria-label={showConfirm ? "Hide confirm password" : "Show confirm password"}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                        >
                            {showConfirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                    </div>
                    {errors.confirmPassword && (
                        <p className="flex items-center gap-1 text-xs text-destructive">
                            <AlertCircle className="h-3 w-3" />{errors.confirmPassword.message}
                        </p>
                    )}
                </div>

                {/* Submit */}
                <button
                    id="register-submit"
                    type="submit"
                    disabled={registerIsPending}
                    className="mt-2 w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-primary-foreground
            transition-all duration-150 hover:opacity-90 active:scale-[0.98]
            disabled:cursor-not-allowed disabled:opacity-60 flex items-center justify-center gap-2"
                >
                    {registerIsPending ? (
                        <>
                            <Loader2 className="h-4 w-4 animate-spin" />
                            Creating account…
                        </>
                    ) : (
                        "Create account"
                    )}
                </button>
            </form>

            <p className="mt-6 text-center text-sm text-muted-foreground">
                Already have an account?{" "}
                <Link href="/login" className="font-medium text-primary hover:underline">
                    Sign in
                </Link>
            </p>
        </div>
    );
}
