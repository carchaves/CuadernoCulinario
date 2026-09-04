/**
 * Personal Access Token de GitHub con el que la web commitea los archivos de `data/`.
 * Equivalente web de `GithubTokenStore.kt` del Android: no hay backend ni sesión, solo esta
 * credencial guardada en el navegador del usuario.
 */
const KEY = "cocina_github_pat";

export function getToken(): string | null {
  try {
    const t = localStorage.getItem(KEY);
    return t && t.trim() ? t : null;
  } catch {
    return null;
  }
}

export function setToken(pat: string): void {
  try {
    localStorage.setItem(KEY, pat.trim());
  } catch {
    /* almacenamiento bloqueado: la sesión queda en solo lectura */
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* nada que hacer */
  }
}

export function hasToken(): boolean {
  return getToken() != null;
}
