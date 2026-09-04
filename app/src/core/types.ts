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
  /** Clave del set fijo de íconos (ver `PAGE_ICONS` en `screens/Despensa.tsx`). */
  iconId?: string;
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

// ---- Lista de compra: administrador de listas por comercio -----------------

export interface Store {
  id: string;
  name: string;
  color: string;
  address: string | null;
}

export interface ShoppingListItem {
  ingredientId: string;
  quantity: number | null;
  unit: string | null;
  bought: boolean;
  price: number | null;
}

export interface ShoppingList {
  id: string;
  storeId: string;
  createdAt: string;
  finalizedAt: string | null;
  items: ShoppingListItem[];
}

export interface Receipt {
  id: string;
  listId: string;
  photoPath: string;
  createdAt: string;
}

/** Último precio conocido por ingrediente y comercio: `priceHistory[ingredientId][storeId]`. */
export type PriceHistory = Record<string, Record<string, number>>;
/** Marcas a evitar por ingrediente; se muestran tachadas. */
export type BoycottedBrands = Record<string, string[]>;

export interface AppState {
  pPages: PantryPage[];
  pActiveId: string | null;
  stores: Store[];
  lists: ShoppingList[];
  priceHistory: PriceHistory;
  boycottedBrands: BoycottedBrands;
  receipts: Receipt[];
  recipes: Record<RecipeView, Recipe[]>;
  rDone: Record<string, true>;
}

// ---- Forma de los archivos en `data/` del repo ----------------------------
// Estos tipos describen el JSON tal cual vive en GitHub (ver `data/README.md`).
// `useAppState` los traduce al `AppState` en memoria de arriba.

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
  stores: Store[];
  lists: ShoppingList[];
  priceHistory: PriceHistory;
  boycottedBrands: BoycottedBrands;
  receipts: Receipt[];
}
