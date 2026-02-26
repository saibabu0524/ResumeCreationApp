import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // ── Bundle optimisation ───────────────────────────────────────────────────
  // Tree-shake large packages so only imported icons/utilities are bundled.
  experimental: {
    optimizePackageImports: [
      "lucide-react",
      "date-fns",
      "@radix-ui/react-dropdown-menu",
      "@radix-ui/react-label",
      "@radix-ui/react-slot",
    ],
  },

  // Output standalone for Docker deployment
  output: "standalone",

  // ── Build Memory Optimizations for EC2 ────────────────────────────────────
  // We disable type checking & eslint during the production build 
  // because the t3.micro runs out of memory (1GB RAM) trying to compile.
  // We rely on local development and CI to catch these errors instead!
  eslint: {
    ignoreDuringBuilds: true,
  },
  typescript: {
    ignoreBuildErrors: true,
  },

  // Gzip/Brotli compression (improves TTFB on static assets)
  compress: true,

  // ── Image optimisation ────────────────────────────────────────────────────
  images: {
    formats: ["image/avif", "image/webp"],
    minimumCacheTTL: 60 * 60 * 24 * 30, // 30 days
  },

  // ── Security Headers ──────────────────────────────────────────────────────
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          {
            key: "Content-Security-Policy",
            value: [
              "default-src 'self'",
              "script-src 'self' 'unsafe-eval' 'unsafe-inline'",
              "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
              "font-src 'self' https://fonts.gstatic.com",
              "img-src 'self' data: blob:",
              // connect-src must be origin-level — browsers ignore path prefixes
              "connect-src 'self' http://resumetailor.in https://resumetailor.in",
              "frame-src 'self' blob:",
              "worker-src blob:",
            ].join("; "),
          },
          {
            key: "X-Content-Type-Options",
            value: "nosniff",
          },
          {
            key: "X-Frame-Options",
            value: "DENY",
          },
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=()",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
