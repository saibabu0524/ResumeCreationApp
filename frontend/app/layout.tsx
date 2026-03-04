import type { Metadata, Viewport } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers/Providers";

/*
 * Load only the weights actually used by the design system.
 * Removing Geist_Mono (was loaded but only referenced in the globals @theme
 * alias — no component uses font-mono at render time) saves ~30 KB of font
 * data from the critical path.
 */
const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
  preload: true,
});

export const metadata: Metadata = {
  title: {
    default: "ResumeTailor — AI-Powered Resume Tailoring",
    template: "%s | ResumeTailor",
  },
  description:
    "Tailor your resume to any job description with AI. Beat ATS systems and land more interviews.",
  keywords: ["resume", "ATS", "AI", "job application", "resume tailor"],
  openGraph: {
    title: "ResumeAI — AI-Powered Resume Tailoring",
    description: "Beat ATS systems and land more interviews.",
    type: "website",
  },
};

/**
 * Viewport config — exported separately per Next.js 14+ requirement.
 * Sets initial-scale and prevents content reflow during font load.
 */
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: [
    { media: "(prefers-color-scheme: dark)",  color: "#0E0D0B" },
    { media: "(prefers-color-scheme: light)", color: "#FAF7F0" },
  ],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        {/* Preconnect to Google Fonts origin so font requests are fast */}
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
      </head>
      <body className={`${inter.variable} font-sans antialiased`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
