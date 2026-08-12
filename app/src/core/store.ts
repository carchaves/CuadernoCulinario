import { useCallback, useEffect, useRef, useState } from "react";
import { getStorage } from "../storage";
import type { Storage } from "../storage/types";
import { uid, roundFor, stepFor } from "./logic";
import { seedState } from "./seed";
import type { AppState, Ingredient, IngredientType, Recipe, RecipeStep, RecipeView, Unit } from "./types";

export interface NewIngredientInput {
  name: string;
  type: IngredientType;
  amount: number;
  unit: Unit;
}

export interface RecipeDraft {
  title: string;
  meta: string[];
  tiempo: string;
  ingredientes: string[];
  utensilios: string[];
  prePrep: RecipeStep[];
  prep: RecipeStep[];
}

export function useAppState() {
  const [state, setState] = useState<AppState | null>(null);
  const storageRef = useRef<Storage | null>(null);

  const load = useCallback(async () => {
    const storage = storageRef.current ?? (await getStorage());
    storageRef.current = storage;
    const loaded = await storage.load();
    setState(loaded ?? seedState());
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const mutate = useCallback((updater: (s: AppState) => AppState) => {
    setState((prev) => {
      if (!prev) return prev;
      const next = updater(prev);
      storageRef.current?.save(next);
      return next;
    });
  }, []);

  // ---- Despensa -----------------------------------------------------------
  const addPage = useCallback(
    (name: string) => {
      const page = { id: uid(), name, ingredients: [] };
      mutate((s) => ({ ...s, pPages: [...s.pPages, page], pActiveId: page.id }));
      return page.id;
    },
    [mutate]
  );

  const deletePage = useCallback(
    (pageId: string) => {
      mutate((s) => {
        const next = s.pPages.filter((p) => p.id !== pageId);
        return {
          ...s,
          pPages: next,
          pActiveId: s.pActiveId === pageId ? (next.length ? next[0].id : null) : s.pActiveId,
        };
      });
    },
    [mutate]
  );

  const setActivePage = useCallback(
    (pageId: string) => {
      mutate((s) => ({ ...s, pActiveId: pageId }));
    },
    [mutate]
  );

  const addIngredient = useCallback(
    (pageId: string, input: NewIngredientInput) => {
      const ing: Ingredient = { id: uid(), ...input };
      mutate((s) => ({
        ...s,
        pPages: s.pPages.map((p) => (p.id === pageId ? { ...p, ingredients: [...p.ingredients, ing] } : p)),
      }));
    },
    [mutate]
  );

  const adjustIngredient = useCallback(
    (pageId: string, ingId: string, dir: 1 | -1) => {
      mutate((s) => ({
        ...s,
        pPages: s.pPages.map((p) =>
          p.id !== pageId
            ? p
            : {
                ...p,
                ingredients: p.ingredients.map((i) =>
                  i.id !== ingId ? i : { ...i, amount: roundFor(i.amount + dir * stepFor(i.unit), i.unit) }
                ),
              }
        ),
      }));
    },
    [mutate]
  );

  const removeIngredient = useCallback(
    (pageId: string, ingId: string) => {
      mutate((s) => ({
        ...s,
        pPages: s.pPages.map((p) => (p.id !== pageId ? p : { ...p, ingredients: p.ingredients.filter((i) => i.id !== ingId) })),
      }));
    },
    [mutate]
  );

  const addIngredientToPantry = useCallback(
    (name: string) => {
      mutate((s) => {
        const pantryFlat = s.pPages.flatMap((p) => p.ingredients);
        if (pantryFlat.some((i) => i.name.toLowerCase() === name.toLowerCase())) return s;
        const ing: Ingredient = { id: uid(), name, type: "unidad", amount: 0, unit: "u" };
        const otros = s.pPages.find((p) => p.name === "Otros");
        const pPages = otros
          ? s.pPages.map((p) => (p.id === otros.id ? { ...p, ingredients: [...p.ingredients, ing] } : p))
          : [...s.pPages, { id: uid(), name: "Otros", ingredients: [ing] }];
        return { ...s, pPages };
      });
    },
    [mutate]
  );

  // ---- Lista de compra ------------------------------------------------------
  const addShoppingItem = useCallback(
    (input: { name: string; amount: number; unit: Unit; pageId: string }) => {
      mutate((s) => {
        const pantryFlat = s.pPages.flatMap((p) => p.ingredients);
        const existing = pantryFlat.find((i) => i.name.toLowerCase() === input.name.toLowerCase());
        if (existing) return s;
        const ing: Ingredient = {
          id: uid(),
          name: input.name,
          type: input.unit === "u" ? "unidad" : "peso",
          amount: input.amount,
          unit: input.unit,
        };
        return {
          ...s,
          pPages: s.pPages.map((p) => (p.id === input.pageId ? { ...p, ingredients: [...p.ingredients, ing] } : p)),
        };
      });
    },
    [mutate]
  );

  const toggleIncluded = useCallback(
    (ingId: string) => {
      mutate((s) => {
        const lIncluded = { ...s.lIncluded };
        if (lIncluded[ingId]) delete lIncluded[ingId];
        else lIncluded[ingId] = true;
        return { ...s, lIncluded };
      });
    },
    [mutate]
  );

  const toggleDone = useCallback(
    (ingId: string) => {
      mutate((s) => {
        const lDone = { ...s.lDone };
        if (lDone[ingId]) delete lDone[ingId];
        else lDone[ingId] = true;
        return { ...s, lDone };
      });
    },
    [mutate]
  );

  const removeFromLista = useCallback(
    (ingId: string) => {
      mutate((s) => {
        const lIncluded = { ...s.lIncluded };
        delete lIncluded[ingId];
        return { ...s, lIncluded };
      });
    },
    [mutate]
  );

  const resetLista = useCallback(() => {
    mutate((s) => ({ ...s, lDone: {} }));
  }, [mutate]);

  // ---- Recetas ----------------------------------------------------------
  const saveRecipe = useCallback(
    (view: RecipeView, editingId: string | null, draft: RecipeDraft) => {
      let savedId = editingId ?? uid();
      mutate((s) => {
        const list = s.recipes[view];
        const recipes = editingId
          ? { ...s.recipes, [view]: list.map((r) => (r.id === editingId ? { ...r, ...draft } : r)) }
          : { ...s.recipes, [view]: [...list, { id: savedId, ...draft }] };
        return { ...s, recipes };
      });
      return savedId;
    },
    [mutate]
  );

  const deleteRecipe = useCallback(
    (view: RecipeView, id: string) => {
      mutate((s) => ({ ...s, recipes: { ...s.recipes, [view]: s.recipes[view].filter((r) => r.id !== id) } }));
    },
    [mutate]
  );

  const toggleStep = useCallback(
    (id: string) => {
      mutate((s) => {
        const rDone = { ...s.rDone };
        if (rDone[id]) delete rDone[id];
        else rDone[id] = true;
        return { ...s, rDone };
      });
    },
    [mutate]
  );

  const saveStepText = useCallback(
    (view: RecipeView, recId: string, section: "pre" | "prep", i: number, text: string) => {
      const key = section === "pre" ? "prePrep" : "prep";
      mutate((s) => ({
        ...s,
        recipes: {
          ...s.recipes,
          [view]: s.recipes[view].map((r) =>
            r.id !== recId
              ? r
              : { ...r, [key]: r[key].map((st, idx) => (idx !== i ? st : { ...st, t: text })) }
          ),
        },
      }));
    },
    [mutate]
  );

  const resetRecetas = useCallback(
    (view: RecipeView) => {
      mutate((s) => {
        const rDone = { ...s.rDone };
        (s.recipes[view] || []).forEach((r) => {
          r.prePrep.forEach((_, i) => delete rDone[`${view}.${r.id}.pre.${i}`]);
          r.prep.forEach((_, i) => delete rDone[`${view}.${r.id}.prep.${i}`]);
        });
        return { ...s, rDone };
      });
    },
    [mutate]
  );

  return {
    state,
    loading: state === null,
    reload: load,
    actions: {
      addPage,
      deletePage,
      setActivePage,
      addIngredient,
      adjustIngredient,
      removeIngredient,
      addIngredientToPantry,
      addShoppingItem,
      toggleIncluded,
      toggleDone,
      removeFromLista,
      resetLista,
      saveRecipe,
      deleteRecipe,
      toggleStep,
      saveStepText,
      resetRecetas,
    },
  };
}

export type Actions = ReturnType<typeof useAppState>["actions"];
export type { Recipe };
