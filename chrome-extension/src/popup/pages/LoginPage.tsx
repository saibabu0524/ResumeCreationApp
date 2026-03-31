import React, { useState } from "react";
import { login, register, fetchCurrentUser } from "../../lib/api-auth";
import { useAuthStore } from "../../stores/auth-store";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isRegister, setIsRegister] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const { setTokens, setUser } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      if (isRegister) {
        await register(email, password);
      }

      const resp = await login(email, password);
      if (resp.success && resp.data) {
        setTokens(resp.data.access_token, resp.data.refresh_token);

        const userResp = await fetchCurrentUser();
        if (userResp.data) {
          setUser(userResp.data);
        }
      } else {
        setError(resp.message || "Login failed");
      }
    } catch (err: any) {
      const detail = err?.response?.data?.detail;
      setError(typeof detail === "string" ? detail : "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6">
      <div className="text-center mb-6">
        <h1 className="text-xl font-bold text-gray-900">Resume Tailor</h1>
        <p className="text-sm text-gray-500 mt-1">
          {isRegister ? "Create your account" : "Sign in to continue"}
        </p>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Email
          </label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="you@example.com"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Password
          </label>
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Min. 8 characters"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2 px-4 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "Please wait..." : isRegister ? "Create Account" : "Sign In"}
        </button>
      </form>

      <button
        onClick={() => {
          setIsRegister(!isRegister);
          setError("");
        }}
        className="w-full mt-3 text-sm text-blue-600 hover:text-blue-800"
      >
        {isRegister ? "Already have an account? Sign in" : "Don't have an account? Register"}
      </button>
    </div>
  );
}
