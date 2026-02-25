import type { Metadata } from "next";
import { Inter, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers/Providers";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
  display: "swap",
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

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        className={`${inter.variable} ${geistMono.variable} font-sans antialiased`}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
