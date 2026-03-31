/**
 * Auth store using Zustand, persisted to chrome.storage.local.
 */
import { create } from "zustand";
import { getFromStorage, setInStorage, removeFromStorage } from "../lib/storage";
import type { UserPublic } from "../types";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserPublic | null;
  isLoggedIn: boolean;
  isInitialized: boolean;

  setTokens: (access: string, refresh: string) => void;
  setUser: (user: UserPublic) => void;
  clearAuth: () => void;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isLoggedIn: false,
  isInitialized: false,

  setTokens: (access, refresh) => {
    set({ accessToken: access, refreshToken: refresh, isLoggedIn: true });
    setInStorage("refreshToken", refresh);
    setInStorage("accessToken", access);
  },

  setUser: (user) => {
    set({ user });
    setInStorage("user", user);
  },

  clearAuth: () => {
    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isLoggedIn: false,
    });
    removeFromStorage("refreshToken");
    removeFromStorage("accessToken");
    removeFromStorage("user");
  },

  initialize: async () => {
    const accessToken = await getFromStorage<string>("accessToken");
    const refreshToken = await getFromStorage<string>("refreshToken");
    const user = await getFromStorage<UserPublic>("user");

    set({
      accessToken,
      refreshToken,
      user,
      isLoggedIn: !!accessToken && !!refreshToken,
      isInitialized: true,
    });
  },
}));
