import type { AppState } from "../core/types";
import type { Storage } from "./types";

const KEY = "cocina-app:v1";

export const localStorageAdapter: Storage = {
  async load() {
    try {
      const raw = window.localStorage.getItem(KEY);
      return raw ? (JSON.parse(raw) as AppState) : null;
    } catch {
      return null;
    }
  },
  async save(state) {
    try {
      window.localStorage.setItem(KEY, JSON.stringify(state));
    } catch {
      // no-op: storage unavailable
    }
  },
};
