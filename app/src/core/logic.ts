import type { Ingredient, Unit } from "./types";

export const uid = (): string =>
  Date.now().toString(36) + Math.random().toString(36).slice(2, 7);

export const stepFor = (unit: Unit): number => {
  switch (unit) {
    case "kg":
    case "L":
      return 0.1;
    case "g":
    case "ml":
      return 50;
    default:
      return 1;
  }
};

export const roundFor = (v: number, unit: Unit): number => {
  const r = unit === "kg" || unit === "L" ? Math.round(v * 100) / 100 : Math.round(v);
  return Math.max(0, r);
};

export const fmt = (n: number | null | undefined, unit: Unit): string => {
  if (n == null || isNaN(n)) return "0";
  const dec = unit === "kg" || unit === "L" ? 2 : 0;
  let s = Number(n).toFixed(dec);
  if (dec) s = s.replace(/\.?0+$/, "");
  return s;
};

const fmtNum = (n: number): string =>
  String(Math.round(n * 100) / 100)
    .replace(/\.00$/, "")
    .replace(/(\.\d)0$/, "$1");

export const cleanIngName = (name: string): string => {
  let n = (name || "").trim();
  while (/^u\s+de\s+/i.test(n)) n = n.replace(/^u\s+de\s+/i, "").trim();
  return n;
};

export interface SplitIngredient {
  name: string;
  num: string;
  unit: string;
}

export const splitIngredient = (str: string): SplitIngredient => {
  const m = str.match(
    /^([\d][\d.,/–-]*)\s*(kg|g|ml|l|cdas?|tazas?|docenas?|dientes?|claras?|u)?\b\s*(?:de\s+)?(.+)$/i
  );
  if (m) return { name: cleanIngName(m[3]), num: m[1].trim(), unit: (m[2] || "").trim() };
  return { name: cleanIngName(str), num: "", unit: "" };
};

export const repairIngredientStr = (str: string): string => {
  const p = splitIngredient(str);
  if (!p.num) return p.name;
  return p.unit ? `${p.num} ${p.unit} de ${p.name}` : `${p.num} de ${p.name}`;
};

export const findPantryUnit = (
  pantryFlat: Pick<Ingredient, "name" | "unit">[],
  name: string
): string => {
  const low = name.toLowerCase();
  const m = pantryFlat.find((pi) => {
    const pn = pi.name.toLowerCase();
    return low.includes(pn) || pn.includes(low);
  });
  return (m && m.unit) || "u";
};

export const parseIngToObj = (
  str: string,
  pantryFlat: Pick<Ingredient, "name" | "unit">[]
): { id: string; name: string; amount: number; unit: string } => {
  const p = splitIngredient(str);
  const unit = findPantryUnit(pantryFlat || [], p.name) || p.unit || "u";
  return {
    id: uid(),
    name: p.name,
    amount: p.num ? parseFloat(p.num.replace(",", ".")) : 0,
    unit,
  };
};

export const buildIngString = (ing: { name: string; amount: number; unit: string }): string => {
  if (!ing.amount) return ing.name;
  const amt = fmt(ing.amount, ing.unit);
  return ing.unit ? `${amt} ${ing.unit} de ${ing.name}` : `${amt} de ${ing.name}`;
};

export const parseMinutes = (txt: string | undefined): number => {
  if (!txt) return Infinity;
  let mins = 0;
  const hMatches = txt.matchAll(/(\d+(?:[.,]\d+)?)\s*h/gi);
  for (const m of hMatches) mins += parseFloat(m[1].replace(",", ".")) * 60;
  const minMatches = txt.matchAll(/(\d+)\s*min/gi);
  for (const m of minMatches) mins += parseFloat(m[1]);
  return mins || Infinity;
};

export const gramsFor = (u: string): number | null =>
  u === "kg" || u === "L" ? 1000 : u === "g" || u === "ml" ? 1 : null;

export const hasAllIngredients = (ingredientes: string[], pantryNames: string[]): boolean =>
  ingredientes.length > 0 &&
  ingredientes.every((ig) => {
    const low = ig.toLowerCase();
    return pantryNames.some((pn) => low.includes(pn) || pn.includes(low));
  });

export interface AvailabilityIngredient extends SplitIngredient {
  numDisplay: string;
  unitDisplay: string;
  available: "en" | "pocas" | "sin";
}

/**
 * Groups a recipe's ingredient strings by pantry availability, scaling quantities
 * by `servings / 4` (the design's base serving size). Ported from the dc-script's
 * `rRecipes` computation.
 */
export function computeIngredientAvailability(
  ingredientes: string[],
  pantryFlat: Ingredient[],
  servings: number
): { ingEn: AvailabilityIngredient[]; ingPocas: AvailabilityIngredient[]; ingSin: AvailabilityIngredient[] } {
  const ratio = servings / 4;
  const findPantryMatch = (name: string) => {
    const low = name.toLowerCase();
    return (
      pantryFlat.find((pi) => {
        const pn = pi.name.toLowerCase();
        return low.includes(pn) || pn.includes(low);
      }) || null
    );
  };

  const scaled = ingredientes.map(splitIngredient).map((ig) => {
    const match0 = findPantryMatch(ig.name);
    const num = ig.num
      ? fmtNum(parseFloat(ig.num.replace(",", ".")) * ratio)
      : ig.num;
    return { ...ig, num, unit: num ? ig.unit || (match0 && match0.unit) || "u" : "" };
  });

  const ingEn: AvailabilityIngredient[] = [];
  const ingPocas: AvailabilityIngredient[] = [];
  const ingSin: AvailabilityIngredient[] = [];

  scaled.forEach((ig) => {
    const match = findPantryMatch(ig.name);
    if (!match) {
      ingSin.push({ ...ig, numDisplay: ig.num, unitDisplay: ig.num ? ig.unit : "", available: "sin" });
      return;
    }
    const gU = gramsFor(ig.unit);
    const gP = gramsFor(match.unit);
    const needed = ig.num ? parseFloat(ig.num) : null;
    if (needed != null && gU && gP) {
      const have = (match.amount * gP) / gU;
      const label = `${fmtNum(have)}/${fmtNum(needed)}`;
      (have >= needed ? ingEn : ingPocas).push({
        ...ig,
        numDisplay: label,
        unitDisplay: match.unit,
        available: have >= needed ? "en" : "pocas",
      });
    } else if (needed != null && (ig.unit === "u" || !gU)) {
      const have = match.amount;
      const label = `${fmtNum(have)}/${fmtNum(needed)}`;
      (have >= needed ? ingEn : ingPocas).push({
        ...ig,
        numDisplay: label,
        unitDisplay: match.unit,
        available: have >= needed ? "en" : "pocas",
      });
    } else {
      ingEn.push({ ...ig, numDisplay: ig.num, unitDisplay: ig.num ? ig.unit : "", available: "en" });
    }
  });

  return { ingEn, ingPocas, ingSin };
}

export const stepId = (view: string, recId: string, section: "pre" | "prep", i: number): string =>
  `${view}.${recId}.${section}.${i}`;
