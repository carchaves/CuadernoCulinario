import type { AppState } from "../core/types";

export interface Storage {
  load(): Promise<AppState | null>;
  save(state: AppState): Promise<void>;
}
