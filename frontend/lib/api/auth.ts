import type {
    TokenResponse,
    ApiWrapper,
    LoginRequest,
    RegisterRequest,
    UserPublic,
} from "@/types/api";
import { client } from "./client";

export async function login(data: LoginRequest): Promise<TokenResponse> {
    const res = await client.post<ApiWrapper<TokenResponse>>("/auth/login", data);
    if (!res.data.data) throw new Error(res.data.message ?? "Login failed");
    return res.data.data;
}

export async function register(data: RegisterRequest): Promise<TokenResponse> {
    const res = await client.post<ApiWrapper<TokenResponse>>("/auth/register", data);
    if (!res.data.data) throw new Error(res.data.message ?? "Registration failed");
    return res.data.data;
}

export async function refresh(refreshToken: string): Promise<TokenResponse> {
    // Backend expects the refresh token in the request body
    const res = await client.post<ApiWrapper<TokenResponse>>("/auth/refresh", {
        refresh_token: refreshToken,
    });
    if (!res.data.data) throw new Error(res.data.message ?? "Token refresh failed");
    return res.data.data;
}

export async function logout(refreshToken: string): Promise<void> {
    await client.post("/auth/logout", { refresh_token: refreshToken });
}

export async function getProfile(): Promise<UserPublic> {
    const res = await client.get<ApiWrapper<UserPublic>>("/users/me");
    if (!res.data.data) throw new Error("Failed to load profile");
    return res.data.data;
}
