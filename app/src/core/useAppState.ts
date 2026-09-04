import { useCallback, useEffect, useRef, useState } from "react";
import * as gh from "./github";
import { getToken } from "./githubToken";
import type { AppState, DespensaFile, ListaFile, RecetasFile } from "./types";

/**
 * Fuente de verdad local (localStorage) + reconciliación con el repo de GitHub, que hace de
 * backend (ver `data/README.md`). Puerto web de `SyncRepository.kt`: offline-first, toda edición
 * se guarda de inmediato y se refleja en la UI; el push pasa después (debounce de 800ms).
 *
 * El estado en memoria es un único [AppState], pero se persiste como tres archivos
 * independientes: cada uno lleva su `sha` y su flag `dirty`, y se commitea por separado
 * (last-write-wins a nivel de archivo).
 */

const DESPENSA_PATH = "data/despensa.json";
const RECETAS_PATH = "data/recetas.json";
const LISTA_PATH = "data/lista-de-compra.json";

const CACHE_KEY = "cocina_cache_v1";
const PUSH_DEBOUNCE_MS = 800;

type Slice = "despensa" | "recetas" | "lista";
type Shas = Record<Slice, string | null>;
type Dirty = Record<Slice, boolean>;

interface Cache {
  data: AppState;
  shas: Shas;
  dirty: Dirty;
}

const emptyShas = (): Shas => ({ despensa: null, recetas: null, lista: null });
const emptyDirty = (): Dirty => ({ despensa: false, recetas: false, lista: false });

// ---- Traducción AppState <-> archivos --------------------------------------

export const toDespensaFile = (s: AppState): DespensaFile => ({
  pages: s.pPages,
  activePageId: s.pActiveId,
});

export const toRecetasFile = (s: AppState): RecetasFile => ({
  cocina: s.recipes.cocina,
  repo: s.recipes.repo,
  stepDone: s.rDone,
});

export const toListaFile = (s: AppState): ListaFile => ({
  stores: s.stores,
  lists: s.lists,
  priceHistory: s.priceHistory,
  boycottedBrands: s.boycottedBrands,
  receipts: s.receipts,
});

const trueKeys = (map: Record<string, boolean> | undefined): Record<string, true> =>
  Object.fromEntries(
    Object.entries(map ?? {})
      .filter(([, v]) => v)
      .map(([k]) => [k, true as const])
  );

export const emptyState = (): AppState => ({
  pPages: [],
  pActiveId: null,
  stores: [],
  lists: [],
  priceHistory: {},
  boycottedBrands: {},
  receipts: [],
  recipes: { cocina: [], repo: [] },
  rDone: {},
});

export function withDespensaFile(s: AppState, f: DespensaFile): AppState {
  const pPages = f.pages ?? [];
  return { ...s, pPages, pActiveId: f.activePageId ?? (pPages.length ? pPages[0].id : null) };
}

export function withRecetasFile(s: AppState, f: RecetasFile): AppState {
  return { ...s, recipes: { cocina: f.cocina ?? [], repo: f.repo ?? [] }, rDone: trueKeys(f.stepDone) };
}

export function withListaFile(s: AppState, f: ListaFile): AppState {
  return {
    ...s,
    stores: f.stores ?? [],
    lists: f.lists ?? [],
    priceHistory: f.priceHistory ?? {},
    boycottedBrands: f.boycottedBrands ?? {},
    receipts: f.receipts ?? [],
  };
}

/** Igualdad estructural barata: los archivos son JSON puro y se serializan igual. */
const sameSlice = (a: unknown, b: unknown) => JSON.stringify(a) === JSON.stringify(b);

// ---- Caché local -----------------------------------------------------------

function loadCache(): Cache | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<Cache>;
    if (!parsed.data) return null;
    return {
      data: { ...emptyState(), ...parsed.data },
      shas: { ...emptyShas(), ...(parsed.shas ?? {}) },
      dirty: { ...emptyDirty(), ...(parsed.dirty ?? {}) },
    };
  } catch {
    return null;
  }
}

function saveCache(cache: Cache): void {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
  } catch {
    /* cuota llena o almacenamiento bloqueado: seguimos solo en memoria */
  }
}

const COMMIT_MESSAGE: Record<Slice, string> = {
  despensa: "Actualizar despensa desde la web",
  recetas: "Actualizar recetas desde la web",
  lista: "Actualizar lista de compra desde la web",
};

const PATHS: Record<Slice, string> = {
  despensa: DESPENSA_PATH,
  recetas: RECETAS_PATH,
  lista: LISTA_PATH,
};

const SLICE_OF: Record<Slice, (s: AppState) => unknown> = {
  despensa: toDespensaFile,
  recetas: toRecetasFile,
  lista: toListaFile,
};

export interface AppStore {
  state: AppState | null;
  loading: boolean;
  error: string | null;
  /** true cuando no hay PAT configurado: la app funciona pero no puede escribir. */
  readOnly: boolean;
  syncing: boolean;
  /** Hay cambios locales sin commitear. */
  pending: boolean;
  mutate: (updater: (s: AppState) => AppState) => void;
  /** Push si hay cambios pendientes; si no, pull. */
  sync: () => void;
  /** Sube un archivo binario nuevo (foto de recibo). */
  putBinary: (path: string, data: Blob, message: string) => Promise<void>;
  /** Vuelve a evaluar si hay token (después de Ajustes). */
  refreshToken: () => void;
}

export function useAppState(): AppStore {
  const [state, setState] = useState<AppState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [readOnly, setReadOnly] = useState(() => getToken() == null);
  const [syncing, setSyncing] = useState(false);
  const [pending, setPending] = useState(false);

  const stateRef = useRef<AppState | null>(null);
  const shasRef = useRef<Shas>(emptyShas());
  const dirtyRef = useRef<Dirty>(emptyDirty());
  const pushTimer = useRef<number | null>(null);
  const busy = useRef(false);

  const anyDirty = () => dirtyRef.current.despensa || dirtyRef.current.recetas || dirtyRef.current.lista;

  const commitLocal = useCallback((next: AppState) => {
    stateRef.current = next;
    setState(next);
    setPending(anyDirty());
    saveCache({ data: next, shas: shasRef.current, dirty: dirtyRef.current });
  }, []);

  /** GET de los tres archivos en paralelo. No se pisa un archivo con cambios locales sin
   * commitear: ese se resuelve al subirlo (409 → reintento con el sha fresco). */
  const pull = useCallback(async () => {
    const [despensa, recetas, lista] = await Promise.all([
      gh.fetchFile(DESPENSA_PATH),
      gh.fetchFile(RECETAS_PATH),
      gh.fetchFile(LISTA_PATH),
    ]);
    let next = stateRef.current ?? emptyState();
    if (!dirtyRef.current.despensa) {
      next = withDespensaFile(next, despensa.json as DespensaFile);
      shasRef.current.despensa = despensa.sha;
    }
    if (!dirtyRef.current.recetas) {
      next = withRecetasFile(next, recetas.json as RecetasFile);
      shasRef.current.recetas = recetas.sha;
    }
    if (!dirtyRef.current.lista) {
      next = withListaFile(next, lista.json as ListaFile);
      shasRef.current.lista = lista.sha;
    }
    commitLocal(next);
  }, [commitLocal]);

  /** Un PUT (commit) por archivo sucio. Si uno falla se intentan igual los otros y solo ese
   * queda pendiente. */
  const push = useCallback(async () => {
    const current = stateRef.current;
    if (!current) return;
    if (getToken() == null) throw new gh.NoTokenError();
    let failure: unknown = null;
    for (const slice of ["despensa", "recetas", "lista"] as Slice[]) {
      if (!dirtyRef.current[slice]) continue;
      try {
        shasRef.current[slice] = await gh.putFile(
          PATHS[slice],
          SLICE_OF[slice](current),
          shasRef.current[slice],
          COMMIT_MESSAGE[slice]
        );
        dirtyRef.current[slice] = false;
      } catch (e) {
        failure = e;
      }
    }
    commitLocal(stateRef.current ?? current);
    if (failure) throw failure;
  }, [commitLocal]);

  const runSync = useCallback(async () => {
    if (busy.current) return;
    busy.current = true;
    setSyncing(true);
    try {
      if (anyDirty()) await push();
      else await pull();
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo sincronizar con GitHub.");
    } finally {
      busy.current = false;
      setSyncing(false);
    }
  }, [pull, push]);

  // Carga inicial: caché primero (arranque instantáneo), después reconciliación con el repo.
  useEffect(() => {
    let cancelled = false;
    const cached = loadCache();
    if (cached) {
      stateRef.current = cached.data;
      shasRef.current = cached.shas;
      dirtyRef.current = cached.dirty;
      setState(cached.data);
      setPending(anyDirty());
      setLoading(false);
    }

    (async () => {
      try {
        if (anyDirty() && getToken() != null) await push();
        else await pull();
      } catch (e) {
        if (cancelled) return;
        if (!stateRef.current) setError(e instanceof Error ? e.message : "No se pudieron cargar los datos.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
    // Solo al montar: el resto de la sincronización pasa por runSync().
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Equivalente web del ON_RESUME de Android: al volver a la pestaña, traemos lo que se haya
  // editado desde el celular (o subimos lo que quedó pendiente acá).
  useEffect(() => {
    const onFocus = () => {
      if (document.visibilityState === "hidden") return;
      void runSync();
    };
    window.addEventListener("focus", onFocus);
    document.addEventListener("visibilitychange", onFocus);
    return () => {
      window.removeEventListener("focus", onFocus);
      document.removeEventListener("visibilitychange", onFocus);
    };
  }, [runSync]);

  const schedulePush = useCallback(() => {
    if (pushTimer.current != null) window.clearTimeout(pushTimer.current);
    pushTimer.current = window.setTimeout(() => {
      pushTimer.current = null;
      if (!anyDirty()) return;
      void (async () => {
        if (busy.current) return;
        busy.current = true;
        setSyncing(true);
        try {
          await push();
          setError(null);
        } catch (e) {
          setError(e instanceof Error ? e.message : "No se pudo guardar en GitHub.");
        } finally {
          busy.current = false;
          setSyncing(false);
        }
      })();
    }, PUSH_DEBOUNCE_MS);
  }, [push]);

  const mutate = useCallback(
    (updater: (s: AppState) => AppState) => {
      const current = stateRef.current;
      if (!current) return;
      if (getToken() == null) return; // solo lectura: la UI ya oculta los controles de edición
      const next = updater(current);
      if (next === current) return;
      // Solo se ensucia el archivo cuya rebanada del estado cambió realmente; una mutación puede
      // tocar dos (ej. mandar un ingrediente a la lista) y son dos commits. Se acumulan con OR:
      // lo que quedó sucio antes sigue sucio.
      dirtyRef.current = {
        despensa: dirtyRef.current.despensa || !sameSlice(toDespensaFile(current), toDespensaFile(next)),
        recetas: dirtyRef.current.recetas || !sameSlice(toRecetasFile(current), toRecetasFile(next)),
        lista: dirtyRef.current.lista || !sameSlice(toListaFile(current), toListaFile(next)),
      };
      commitLocal(next);
      schedulePush();
    },
    [commitLocal, schedulePush]
  );

  const putBinary = useCallback(async (path: string, data: Blob, message: string) => {
    let sha: string | null = null;
    try {
      sha = await gh.fetchSha(path);
    } catch {
      sha = null; // archivo nuevo
    }
    await gh.putBinaryFile(path, data, sha, message);
  }, []);

  const refreshToken = useCallback(() => setReadOnly(getToken() == null), []);

  return {
    state,
    loading,
    error,
    readOnly,
    syncing,
    pending,
    mutate,
    sync: () => void runSync(),
    putBinary,
    refreshToken,
  };
}
