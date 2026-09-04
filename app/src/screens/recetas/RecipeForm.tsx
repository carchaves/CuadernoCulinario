import { useState } from "react";
import type { Recipe, RecipeStep, RecipeView } from "../../core/types";
import { uid } from "../../core/logic";
import { TagAutocomplete } from "./TagAutocomplete";
import { StepsEditor } from "./StepsEditor";

export interface RecipeDraft {
  title: string;
  meta: string[];
  tiempo: string;
  ingredientes: string[];
  utensilios: string[];
  prePrep: RecipeStep[];
  prep: RecipeStep[];
}

export function RecipeForm({
  view,
  editing,
  allMetaTags,
  allIngredientes,
  allUtensilios,
  accent,
  onSave,
  onCancel,
}: {
  view: RecipeView;
  editing: Recipe | null;
  allMetaTags: string[];
  allIngredientes: string[];
  allUtensilios: string[];
  accent: string;
  onSave: (id: string, draft: RecipeDraft) => void;
  onCancel: () => void;
}) {
  const [title, setTitle] = useState(editing?.title ?? "");
  const [metaTags, setMetaTags] = useState<string[]>(editing ? [...editing.meta] : []);
  const [tiempo, setTiempo] = useState(editing && editing.tiempo !== "—" ? editing.tiempo : "");
  const [ingList, setIngList] = useState<string[]>(editing ? [...editing.ingredientes] : []);
  const [utnList, setUtnList] = useState<string[]>(editing ? [...editing.utensilios] : []);
  const [prePrepSteps, setPrePrepSteps] = useState<RecipeStep[]>(editing ? editing.prePrep.map((s) => ({ ...s })) : []);
  const [prepSteps, setPrepSteps] = useState<RecipeStep[]>(editing ? editing.prep.map((s) => ({ ...s })) : []);

  const moveStep = (section: "pre" | "prep", i: number, dir: 1 | -1) => {
    const pre = prePrepSteps.slice();
    const prep = prepSteps.slice();
    const arr = section === "pre" ? pre : prep;
    const j = i + dir;
    if (j >= 0 && j < arr.length) {
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    setPrePrepSteps(pre);
    setPrepSteps(prep);
  };

  const commit = () => {
    const t = title.trim();
    if (!t) return;
    const clean = (arr: RecipeStep[]) =>
      arr
        .map((s) => ({
          ...s,
          t: (s.t || "").trim(),
          title: s.title?.trim() || undefined,
          time: s.time?.trim() || undefined,
          note: s.note?.trim() || undefined,
        }))
        .filter((s) => s.t);
    onSave(editing?.id ?? uid(), {
      title: t,
      meta: [...metaTags],
      tiempo: tiempo.trim() || "—",
      ingredientes: ingList.filter((s) => s.trim()),
      utensilios: [...utnList],
      prePrep: clean(prePrepSteps),
      prep: clean(prepSteps),
    });
  };

  return (
    <div className="recipe-form">
      <div className="card">
        <div className="card-head">
          <h2>{editing ? "Editar receta" : "Nueva receta"}</h2>
        </div>
        <div className="card-body">
          <input className="field" placeholder="Título" value={title} onChange={(e) => setTitle(e.target.value)} />
          <TagAutocomplete
            tags={metaTags}
            onAdd={(t) => setMetaTags((m) => [...m, t])}
            onRemove={(t) => setMetaTags((m) => m.filter((x) => x !== t))}
            allOptions={allMetaTags}
            placeholder="Buscar o agregar etiqueta"
            accent={accent}
          />
          <input
            className="field"
            placeholder="Tiempo estimado total (ej: 30 min)"
            value={tiempo}
            onChange={(e) => setTiempo(e.target.value)}
          />
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <span className="card-emoji">{view === "repo" ? "🍫" : "🍅"}</span>
          <h2>Ingredientes</h2>
        </div>
        <div className="card-body">
          <TagAutocomplete
            tags={ingList}
            onAdd={(t) => setIngList((l) => [...l, t])}
            onRemove={(t) => setIngList((l) => l.filter((x) => x !== t))}
            allOptions={allIngredientes}
            placeholder="ej: 2 kg de papas"
            accent={accent}
          />
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <span className="card-emoji">🔪</span>
          <h2>Utensilios</h2>
        </div>
        <div className="card-body">
          <TagAutocomplete
            tags={utnList}
            onAdd={(t) => setUtnList((l) => [...l, t])}
            onRemove={(t) => setUtnList((l) => l.filter((x) => x !== t))}
            allOptions={allUtensilios}
            placeholder="Buscar o agregar utensilio"
            accent={accent}
          />
        </div>
      </div>

      <h3 className="section-title">Pre-preparación</h3>
      <StepsEditor
        steps={prePrepSteps}
        offset={0}
        onChange={(i, patch) => setPrePrepSteps((s) => s.map((st, idx) => (idx === i ? { ...st, ...patch } : st)))}
        onRemove={(i) => setPrePrepSteps((s) => s.filter((_, idx) => idx !== i))}
        onMoveUp={(i) => moveStep("pre", i, -1)}
        onMoveDown={(i) => moveStep("pre", i, 1)}
        onAdd={() => setPrePrepSteps((s) => [...s, { t: "" }])}
        accent={accent}
      />

      <h3 className="section-title">Preparación</h3>
      <StepsEditor
        steps={prepSteps}
        offset={prePrepSteps.length}
        onChange={(i, patch) => setPrepSteps((s) => s.map((st, idx) => (idx === i ? { ...st, ...patch } : st)))}
        onRemove={(i) => setPrepSteps((s) => s.filter((_, idx) => idx !== i))}
        onMoveUp={(i) => moveStep("prep", i, -1)}
        onMoveDown={(i) => moveStep("prep", i, 1)}
        onAdd={() => setPrepSteps((s) => [...s, { t: "" }])}
        accent={accent}
      />

      <div className="form-actions">
        <button className="save" style={{ background: accent }} onClick={commit} disabled={!title.trim()}>
          {editing ? "Guardar cambios" : "Guardar receta"}
        </button>
        <button className="cancel" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </div>
  );
}
