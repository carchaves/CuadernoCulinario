import type { AppState } from "../core/types";
import type { Storage } from "./types";
import { tokenStore } from "./tokenStore";

const API_BASE = import.meta.env.VITE_API_URL || "";

async function tryRefresh(): Promise<boolean> {
  const refresh = tokenStore.getRefresh();
  if (!refresh) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: refresh }),
    });
    if (!res.ok) return false;
    const { accessToken } = await res.json();
    tokenStore.setAccess(accessToken);
    return true;
  } catch {
    return false;
  }
}

async function authFetch(path: string, init?: RequestInit): Promise<Response> {
  const doFetch = (token: string | null) =>
    fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { ...(init?.headers || {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    });

  let res = await doFetch(tokenStore.getAccess());
  if (res.status === 401) {
    const refreshed = await tryRefresh();
    if (!refreshed) {
      tokenStore.clear();
      throw new Error("No autenticado");
    }
    res = await doFetch(tokenStore.getAccess());
  }
  return res;
}

let currentRevision = 0;

export const serverStorage: Storage = {
  async load() {
    const res = await authFetch("/api/state");
    if (!res.ok) throw new Error(`No se pudo cargar el estado (${res.status})`);
    const body = await res.json();
    currentRevision = body.revision ?? 0;
    return (body.data as AppState | null) ?? null;
  },

  async save(state) {
    const attempt = (baseRevision: number) =>
      authFetch("/api/state", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ data: state, baseRevision }),
      });

    let res = await attempt(currentRevision);
    if (res.status === 409) {
      // Otra sesión (celular/web) guardó más reciente. Última escritura gana:
      // reintentamos nuestro estado sobre la revisión nueva del servidor.
      const conflict = await res.json();
      res = await attempt(conflict.revision);
    }
    if (!res.ok) throw new Error(`No se pudo guardar el estado (${res.status})`);
    const body = await res.json();
    currentRevision = body.revision;
  },
};
