import { getToken } from "./githubToken";

/**
 * Cliente de la Contents API de GitHub — el "backend" de la app (ver `data/README.md`).
 * Puerto TS de `GithubDataSource.kt` del Android: mismas rutas, mismo criterio de conflicto.
 */
export const OWNER = "carchaves";
export const REPO = "CuadernoCulinario";
export const BRANCH = "main";

const API = `https://api.github.com/repos/${OWNER}/${REPO}`;

/** El `sha` local quedó viejo (alguien commiteó ese archivo antes que nosotros). */
export class GithubConflictError extends Error {}
/** El archivo todavía no existe en el repo (404). */
export class GithubNotFoundError extends Error {}
/** No hay PAT configurado: la app está en modo solo lectura. */
export class NoTokenError extends Error {
  constructor() {
    super("No hay un token de GitHub configurado.");
  }
}

function headers(auth: boolean): Record<string, string> {
  const h: Record<string, string> = {
    Accept: "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
  };
  if (auth) {
    const token = getToken();
    if (!token) throw new NoTokenError();
    h.Authorization = `Bearer ${token}`;
  } else {
    const token = getToken();
    if (token) h.Authorization = `Bearer ${token}`;
  }
  return h;
}

/** GitHub devuelve el base64 cortado en líneas; `atob` no las tolera. */
function decodeBase64ToText(content: string): string {
  const binary = atob(content.replace(/\s/g, ""));
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

function encodeBytesToBase64(bytes: Uint8Array): string {
  let binary = "";
  const CHUNK = 0x8000;
  for (let i = 0; i < bytes.length; i += CHUNK) {
    binary += String.fromCharCode(...bytes.subarray(i, i + CHUNK));
  }
  return btoa(binary);
}

function encodeTextToBase64(text: string): string {
  return encodeBytesToBase64(new TextEncoder().encode(text));
}

export interface FetchedFile {
  json: unknown;
  sha: string;
}

/** @returns el JSON del archivo + el `sha` de su blob (necesario para escribirlo después). */
export async function fetchFile(path: string): Promise<FetchedFile> {
  const res = await fetch(`${API}/contents/${path}?ref=${BRANCH}&t=${Date.now()}`, {
    headers: headers(false),
    cache: "no-store",
  });
  if (res.status === 404) throw new GithubNotFoundError(`No existe ${path} en el repo`);
  if (!res.ok) throw new Error(`GitHub respondió ${res.status} al leer ${path}`);
  const body = (await res.json()) as { content?: string; sha: string };
  if (!body.content) throw new Error(`Respuesta sin contenido para ${path}`);
  return { json: JSON.parse(decodeBase64ToText(body.content)), sha: body.sha };
}

/** Solo el `sha` del blob, para reintentar un PUT que dio conflicto. */
export async function fetchSha(path: string): Promise<string> {
  const res = await fetch(`${API}/contents/${path}?ref=${BRANCH}&t=${Date.now()}`, {
    headers: headers(false),
    cache: "no-store",
  });
  if (res.status === 404) throw new GithubNotFoundError(`No existe ${path} en el repo`);
  if (!res.ok) throw new Error(`GitHub respondió ${res.status} al leer ${path}`);
  const body = (await res.json()) as { sha: string };
  return body.sha;
}

async function put(path: string, contentBase64: string, sha: string | null, message: string): Promise<string> {
  const res = await fetch(`${API}/contents/${path}`, {
    method: "PUT",
    headers: { ...headers(true), "Content-Type": "application/json" },
    body: JSON.stringify({ message, content: contentBase64, sha: sha ?? undefined, branch: BRANCH }),
  });
  if (!res.ok) {
    // 409: `sha` viejo. 422: GitHub también lo usa cuando mandamos sha vacío sobre un archivo
    // que ya existe (caché local borrada) — se arregla igual, releyendo el sha.
    if (res.status === 409 || res.status === 422) {
      throw new GithubConflictError(`Conflicto de sha al escribir ${path}`);
    }
    throw new Error(`GitHub respondió ${res.status} al escribir ${path}`);
  }
  const body = (await res.json()) as { content?: { sha?: string } };
  const newSha = body.content?.sha;
  if (!newSha) throw new Error(`Respuesta sin sha al escribir ${path}`);
  return newSha;
}

/** PUT con reintento en conflicto: releemos el archivo solo para tomar su `sha` fresco y
 * volvemos a escribir NUESTRA versión local (última escritura gana, sin merge). */
async function putWithRetry(path: string, contentBase64: string, sha: string | null, message: string): Promise<string> {
  try {
    return await put(path, contentBase64, sha, message);
  } catch (e) {
    if (!(e instanceof GithubConflictError)) throw e;
    const fresh = await fetchSha(path);
    return put(path, contentBase64, fresh, message);
  }
}

/** Commitea `json` en `path`. @returns el `sha` nuevo del blob. */
export function putFile(path: string, json: unknown, sha: string | null, message: string): Promise<string> {
  return putWithRetry(path, encodeTextToBase64(JSON.stringify(json, null, 2) + "\n"), sha, message);
}

/** Igual que [putFile] pero para binarios (fotos de recibo). */
export async function putBinaryFile(
  path: string,
  data: Uint8Array | Blob,
  sha: string | null,
  message: string
): Promise<string> {
  const bytes = data instanceof Blob ? new Uint8Array(await data.arrayBuffer()) : data;
  return putWithRetry(path, encodeBytesToBase64(bytes), sha, message);
}

/** Valida un PAT: 200 sobre el repo = sirve. */
export async function validateToken(pat: string): Promise<boolean> {
  try {
    const res = await fetch(API, {
      headers: {
        Accept: "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        Authorization: `Bearer ${pat}`,
      },
      cache: "no-store",
    });
    return res.ok;
  } catch {
    return false;
  }
}
