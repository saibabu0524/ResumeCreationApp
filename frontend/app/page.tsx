import Link from "next/link";
import { ArrowRight, Wand2, Target, ShieldCheck, FileText, Sparkles, Zap } from "lucide-react";

const FEATURES = [
  {
    icon: Wand2,
    title: "AI Resume Tailoring",
    description:
      "Upload your resume and a job description. Our AI rewrites every bullet point to match what the employer is looking for.",
    color: "text-primary",
    bg: "bg-primary/10",
    ring: "ring-primary/20",
  },
  {
    icon: Target,
    title: "ATS Score Analysis",
    description:
      "Instantly score your resume against any job description. See matched and missing keywords so you know exactly what to fix.",
    color: "text-emerald-600 dark:text-emerald-400",
    bg: "bg-emerald-500/10",
    ring: "ring-emerald-500/20",
  },
  {
    icon: ShieldCheck,
    title: "Privacy First",
    description:
      "Your resume is processed in-session and never stored without your permission. Your data stays yours.",
    color: "text-primary/70",
    bg: "bg-primary/8",
    ring: "ring-primary/15",
  },
];

const STATS = [
  { value: "3×", label: "More interview callbacks" },
  { value: "< 20s", label: "Average tailoring time" },
  { value: "ATS", label: "Optimised output" },
];

export default function HomePage() {
  return (
    <div className="relative min-h-screen overflow-x-hidden bg-background">
      {/*
       * Ambient background — single CSS radial-gradient.
       * Replaces three blurred divs that each created a GPU compositing
       * layer (major LCP/CLS penalty). This approach costs zero extra layers.
       */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 -z-10"
        style={{
          background:
            "radial-gradient(ellipse 80% 50% at 15% 5%, #D4A85310 0%, transparent 55%), " +
            "radial-gradient(ellipse 70% 45% at 85% 95%, #CFA05008 0%, transparent 55%)",
        }}
      />

      {/* Navigation */}
      <header className="sticky top-0 z-20 border-b border-border/40 glass">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/15 ring-1 ring-primary/30">
              <FileText className="h-4 w-4 text-primary" />
            </div>
            <span className="text-lg font-bold gradient-text">ResumeTailor</span>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/login"
              id="hero-login"
              className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
            >
              Sign in
            </Link>
            <Link
              href="/register"
              id="hero-register"
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground
                transition-all duration-150 hover:opacity-90 active:scale-[0.98]"
            >
              Get started free
            </Link>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-6">
        {/* Hero */}
        <section className="py-24 text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-4 py-1.5 text-xs font-medium text-primary mb-8">
            <Sparkles className="h-3 w-3" />
            AI-powered in seconds
          </div>

          <h1 className="mx-auto max-w-3xl text-5xl font-extrabold leading-tight tracking-tight text-foreground sm:text-6xl">
            Tailor your resume to{" "}
            <span className="gradient-text">every job</span>{" "}
            you apply for
          </h1>

          <p className="mx-auto mt-6 max-w-xl text-lg text-muted-foreground leading-relaxed">
            Stop submitting generic resumes. ResumeTailor analyses the job description and rewrites
            your resume to beat ATS filters and catch the recruiter&apos;s eye.
          </p>

          <div className="mt-10 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <Link
              href="/register"
              id="hero-cta-primary"
              className="flex items-center gap-2 rounded-xl bg-primary px-7 py-3.5 text-sm font-bold text-primary-foreground
                transition-all duration-150 hover:opacity-90 active:scale-[0.98] shadow-lg shadow-primary/20"
            >
              Start tailoring for free
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              href="/login"
              id="hero-cta-secondary"
              className="flex items-center gap-2 rounded-xl border border-border px-7 py-3.5 text-sm font-semibold text-foreground
                hover:bg-muted transition-colors duration-150"
            >
              Sign in
            </Link>
          </div>

          {/* Stats */}
          <div className="mt-16 grid grid-cols-3 gap-6 max-w-lg mx-auto">
            {STATS.map((stat) => (
              <div key={stat.label} className="text-center">
                <p className="text-2xl font-extrabold gradient-text">{stat.value}</p>
                <p className="mt-1 text-xs text-muted-foreground">{stat.label}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Features */}
        <section className="py-16">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground">Everything you need to land the job</h2>
            <p className="mt-3 text-muted-foreground">Two powerful tools, one streamlined workflow</p>
          </div>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map(({ icon: Icon, title, description, color, bg, ring }) => (
              <div
                key={title}
                className="glass rounded-2xl p-7 transition-all duration-200 hover:ring-1 hover:ring-primary/20 hover:-translate-y-1"
              >
                <div className={`mb-5 inline-flex h-12 w-12 items-center justify-center rounded-xl ${bg} ring-1 ${ring}`}>
                  <Icon className={`h-6 w-6 ${color}`} />
                </div>
                <h3 className="text-base font-bold text-foreground">{title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{description}</p>
              </div>
            ))}
          </div>
        </section>

        {/* How it works */}
        <section className="py-16">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground">How it works</h2>
          </div>
          <div className="grid gap-8 sm:grid-cols-3">
            {[
              { step: "1", icon: FileText, title: "Upload your resume", desc: "Upload your existing PDF resume — any format works." },
              { step: "2", icon: Zap, title: "Paste the job description", desc: "Copy the full job posting and paste it in. The more detail, the better." },
              { step: "3", icon: Wand2, title: "Download tailored resume", desc: "AI rewrites your resume in seconds. Download and apply." },
            ].map(({ step, icon: Icon, title, desc }) => (
              <div key={step} className="text-center">
                <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 ring-1 ring-primary/20">
                  <Icon className="h-7 w-7 text-primary" />
                </div>
                <div className="mb-2 text-xs font-bold uppercase tracking-widest text-primary">Step {step}</div>
                <h3 className="text-base font-bold text-foreground">{title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* CTA Banner */}
        <section className="py-16">
          <div className="glass rounded-3xl p-12 text-center ring-1 ring-primary/10">
            <Sparkles className="mx-auto mb-4 h-12 w-12 text-primary" />
            <h2 className="text-3xl font-extrabold text-foreground">
              Ready to land your next role?
            </h2>
            <p className="mt-3 text-muted-foreground">Create a free account and tailor your first resume in under 30 seconds.</p>
            <Link
              href="/register"
              id="bottom-cta"
              className="mx-auto mt-8 flex w-fit items-center gap-2 rounded-xl bg-primary px-8 py-3.5 text-sm font-bold text-primary-foreground
                transition-all duration-150 hover:opacity-90 active:scale-[0.98] shadow-lg shadow-primary/20"
            >
              Get started — it&apos;s free
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t border-border/40 py-8">
        <div className="mx-auto max-w-6xl px-6 text-center text-xs text-muted-foreground">
          © 2026 ResumeTailor. All rights reserved.
        </div>
      </footer>
    </div>
  );
}
