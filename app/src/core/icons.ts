/** Set fijo de íconos para las páginas (estanterías) de la despensa. */
export const PAGE_ICONS: { id: string; glyph: string; label: string }[] = [
  { id: "grano", glyph: "🌾", label: "Granos y secos" },
  { id: "verdura", glyph: "🥕", label: "Verduras" },
  { id: "fruta", glyph: "🍎", label: "Frutas" },
  { id: "carne", glyph: "🥩", label: "Carnes" },
  { id: "lacteo", glyph: "🧀", label: "Lácteos" },
  { id: "panificado", glyph: "🥖", label: "Panificados" },
  { id: "conserva", glyph: "🥫", label: "Conservas" },
  { id: "condimento", glyph: "🧂", label: "Condimentos" },
  { id: "bebida", glyph: "🧃", label: "Bebidas" },
  { id: "congelado", glyph: "🧊", label: "Congelados" },
  { id: "dulce", glyph: "🍯", label: "Dulces" },
  { id: "limpieza", glyph: "🧼", label: "Limpieza" },
];

export const iconGlyph = (iconId: string | undefined): string =>
  PAGE_ICONS.find((i) => i.id === iconId)?.glyph ?? "🗄️";

/** Colores sugeridos al crear un comercio. */
export const STORE_COLORS = ["#C1494E", "#B07A2E", "#5A8F3C", "#4C7A93", "#8C5E9E", "#A65D8C", "#8C8377"];
