export type Unit = "g" | "kg" | "ml" | "L" | "u" | (string & {});
export type IngredientType = "peso" | "unidad";

export interface Ingredient {
  id: string;
  name: string;
  type: IngredientType;
  amount: number;
  unit: Unit;
}

export interface PantryPage {
  id: string;
  name: string;
  ingredients: Ingredient[];
}

export interface RecipeStep {
  t: string;
  title?: string;
  time?: string;
  note?: string;
}

export interface Recipe {
  id: string;
  title: string;
  meta: string[];
  tiempo: string;
  ingredientes: string[];
  utensilios: string[];
  prePrep: RecipeStep[];
  prep: RecipeStep[];
}

export type RecipeView = "cocina" | "repo";

export interface AppState {
  pPages: PantryPage[];
  pActiveId: string | null;
  lDone: Record<string, true>;
  lIncluded: Record<string, true>;
  recipes: Record<RecipeView, Recipe[]>;
  rDone: Record<string, true>;
}

// ---- Forma de los archivos en `data/` del repo ----------------------------
// Estos tipos describen el JSON tal cual vive en GitHub (ver `data/README.md`).
// `useAppData` los traduce al `AppState` en memoria de arriba.

export interface DespensaFile {
  pages: PantryPage[];
  activePageId: string | null;
}

export interface RecetasFile {
  cocina: Recipe[];
  repo: Recipe[];
  stepDone: Record<string, boolean>;
}

export interface ListaFile {
  includedIngredientIds: string[];
  doneIngredientIds: string[];
}
