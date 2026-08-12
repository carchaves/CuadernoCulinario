import type { Storage } from "./types";
import { localStorageAdapter } from "./localStorage";
import { serverStorage } from "./serverStorage";

let cached: Promise<Storage> | null = null;

async function detectStorage(): Promise<Storage> {
  try {
    const res = await fetch(`${import.meta.env.VITE_API_URL || ""}/health`);
    if (res.ok) return serverStorage;
  } catch {
    // sin servidor accesible: modo local para iterar la UI sin backend
  }
  return localStorageAdapter;
}

export function getStorage(): Promise<Storage> {
  if (!cached) cached = detectStorage();
  return cached;
}
