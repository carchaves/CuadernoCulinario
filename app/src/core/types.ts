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
