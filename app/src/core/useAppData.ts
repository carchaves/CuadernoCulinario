import { useCallback, useEffect, useState } from "react";
import type { AppState, DespensaFile, ListaFile, RecetasFile } from "./types";

const RAW_BASE = "https://raw.githubusercontent.com/carchaves/CuadernoCulinario/main/data";

const FILES = {
  despensa: "despensa.json",
  recetas: "recetas.json",
  lista: "lista-de-compra.json",
} as const;

/** raw.githubusercontent.com cachea unos minutos en el edge: rompemos la caché por carga. */
async function fetchJson<T>(file: string, signal: AbortSignal): Promise<T> {
  const res = await fetch(`${RAW_BASE}/${file}?t=${Date.now()}`, { signal, cache: "no-store" });
  if (!res.ok) throw new Error(`${file}: ${res.status} ${res.statusText}`);
  return (await res.json()) as T;
}

const idsToRecord = (ids: string[] | undefined): Record<string, true> =>
  Object.fromEntries((ids ?? []).map((id) => [id, true as const]));

const trueKeys = (map: Record<string, boolean> | undefined): Record<string, true> =>
  Object.fromEntries(
    Object.entries(map ?? {})
      .filter(([, v]) => v)
      .map(([k]) => [k, true as const])
  );

export function assembleState(despensa: DespensaFile, recetas: RecetasFile, lista: ListaFile): AppState {
  const pPages = despensa.pages ?? [];
  return {
    pPages,
    pActiveId: despensa.activePageId ?? (pPages.length ? pPages[0].id : null),
    lIncluded: idsToRecord(lista.includedIngredientIds),
    lDone: idsToRecord(lista.doneIngredientIds),
    recipes: { cocina: recetas.cocina ?? [], repo: recetas.repo ?? [] },
    rDone: trueKeys(recetas.stepDone),
  };
}

export interface AppData {
  state: AppState | null;
  loading: boolean;
  error: string | null;
  reload: () => void;
}

export function useAppData(): AppData {
  const [state, setState] = useState<AppState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [nonce, setNonce] = useState(0);

  const reload = useCallback(() => setNonce((n) => n + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    let cancelled = false;

    setLoading(true);
    setError(null);

    (async () => {
      try {
        const [despensa, recetas, lista] = await Promise.all([
          fetchJson<DespensaFile>(FILES.despensa, controller.signal),
          fetchJson<RecetasFile>(FILES.recetas, controller.signal),
          fetchJson<ListaFile>(FILES.lista, controller.signal),
        ]);
        if (cancelled) return;
        setState(assembleState(despensa, recetas, lista));
      } catch (e) {
        if (cancelled || controller.signal.aborted) return;
        setError(e instanceof Error ? e.message : "No se pudieron cargar los datos.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [nonce]);

  return { state, loading, error, reload };
}
