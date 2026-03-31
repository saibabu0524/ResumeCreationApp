/**
 * Auth API calls.
 */
import { apiClient } from "./api-client";
import type { ApiResponse, TokenResponse, UserPublic } from "../types";

export async function login(
  email: string,
  password: string,
): Promise<ApiResponse<TokenResponse>> {
  const resp = await apiClient.post<ApiResponse<TokenResponse>>("/auth/login", {
    email,
    password,
  });
  return resp.data;
}

export async function register(
  email: string,
  password: string,
): Promise<ApiResponse<UserPublic>> {
  const resp = await apiClient.post<ApiResponse<UserPublic>>("/auth/register", {
    email,
    password,
  });
  return resp.data;
}

export async function fetchCurrentUser(): Promise<ApiResponse<UserPublic>> {
  const resp = await apiClient.get<ApiResponse<UserPublic>>("/users/me");
  return resp.data;
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post("/auth/logout", { refresh_token: refreshToken });
}
