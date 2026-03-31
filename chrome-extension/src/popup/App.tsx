import React, { useEffect, useState } from "react";
import { useAuthStore } from "../stores/auth-store";
import { LoginPage } from "./pages/LoginPage";
import { TailorPage } from "./pages/TailorPage";
import { AtsPage } from "./pages/AtsPage";
import { ResumesPage } from "./pages/ResumesPage";

type Tab = "tailor" | "ats" | "resumes";

const TAB_CONFIG: { key: Tab; label: string }[] = [
  { key: "tailor", label: "Tailor" },
  { key: "ats", label: "ATS" },
  { key: "resumes", label: "Resumes" },
];

export default function App() {
  const { isLoggedIn, isInitialized, user, initialize, clearAuth } = useAuthStore();
  const [activeTab, setActiveTab] = useState<Tab>("tailor");

  useEffect(() => {
    initialize();
  }, [initialize]);

  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-sm text-gray-400">Loading...</div>
      </div>
    );
  }

  if (!isLoggedIn) {
    return <LoginPage />;
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
        <div className="text-sm font-semibold text-gray-900">Resume Tailor</div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-gray-500 truncate max-w-[140px]">
            {user?.email}
          </span>
          <button
            onClick={clearAuth}
            className="text-xs text-red-500 hover:text-red-700"
          >
            Logout
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200">
        {TAB_CONFIG.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex-1 py-2 text-sm font-medium text-center transition-colors ${
              activeTab === tab.key
                ? "text-blue-600 border-b-2 border-blue-600"
                : "text-gray-500 hover:text-gray-700"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        {activeTab === "tailor" && <TailorPage />}
        {activeTab === "ats" && <AtsPage />}
        {activeTab === "resumes" && <ResumesPage />}
      </div>
    </div>
  );
}
