import { Capacitor } from "@capacitor/core";
import type { Storage } from "./types";
import { localStorageAdapter } from "./localStorage";
import { httpStorageAdapter } from "./httpStorage";
import { capacitorStorageAdapter } from "./capacitorStorage";

let cached: Promise<Storage> | null = null;

async function detectStorage(): Promise<Storage> {
  if (Capacitor.isNativePlatform()) return capacitorStorageAdapter;
  try {
    const res = await fetch("/api/status", { headers: { Accept: "application/json" } });
    const ct = res.headers.get("content-type") || "";
    if (res.ok && ct.includes("application/json")) return httpStorageAdapter;
  } catch {
    // no desktop-server reachable
  }
  return localStorageAdapter;
}

export function getStorage(): Promise<Storage> {
  if (!cached) cached = detectStorage();
  return cached;
}
