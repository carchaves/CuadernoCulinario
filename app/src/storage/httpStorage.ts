import type { AppState } from "../core/types";
import type { Storage } from "./types";

export const httpStorageAdapter: Storage = {
  async load() {
    const res = await fetch("/api/data");
    if (!res.ok) return null;
    const body = await res.json();
    return body && Object.keys(body).length ? (body as AppState) : null;
  },
  async save(state) {
    await fetch("/api/data", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(state),
    });
  },
};
