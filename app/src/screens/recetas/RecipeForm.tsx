import { useState } from "react";
import type { Ingredient, Recipe, RecipeStep, RecipeView } from "../../core/types";
import type { Actions } from "../../core/store";
import { buildIngString, findPantryUnit, parseIngToObj, roundFor, stepFor, uid } from "../../core/logic";
import { TagAutocomplete } from "./TagAutocomplete";
import { IngredientTagInput, type IngredientTagItem } from "./IngredientTagInput";
import { StepsEditor } from "./StepsEditor";

export function RecipeForm({
  view,
  editingId,
  editingRecipe,
  pantryFlat,
  allMetaTags,
  allUtensilios,
  accent,
  actions,
  onCancel,
  onSaved,
}: {
  view: RecipeView;
  editingId: string | null;
  editingRecipe: Recipe | null;
  pantryFlat: Ingredient[];
  allMetaTags: string[];
  allUtensilios: string[];
  accent: string;
  actions: Actions;
  onCancel: () => void;
  onSaved: (id: string) => void;
}) {
  const [title, setTitle] = useState(editingRecipe?.title || "");
  const [metaTags, setMetaTags] = useState<string[]>(editingRecipe?.meta ? editingRecipe.meta.slice() : []);
  const [tiempo, setTiempo] = useState(editingRecipe?.tiempo && editingRecipe.tiempo !== "—" ? editingRecipe.tiempo : "");
  const [ingList, setIngList] = useState<IngredientTagItem[]>(
    editingRecipe ? editingRecipe.ingredientes.map((s) => parseIngToObj(s, pantryFlat)) : []
  );
  const [utnList, setUtnList] = useState<string[]>(editingRecipe?.utensilios ? editingRecipe.utensilios.slice() : []);
  const [prePrepSteps, setPrePrepSteps] = useState<RecipeStep[]>(editingRecipe ? editingRecipe.prePrep.map((s) => ({ ...s })) : []);
  const [prepSteps, setPrepSteps] = useState<RecipeStep[]>(editingRecipe ? editingRecipe.prep.map((s) => ({ ...s })) : []);

  const pantryIngredientNames = [...new Set(pantryFlat.map((i) => i.name))];

  const addIngredientTag = (name: string) => {
    const unit = findPantryUnit(pantryFlat, name);
    setIngList((list) => (list.some((o) => o.name === name) ? list : [...list, { id: uid(), name, amount: 0, unit }]));
    actions.addIngredientToPantry(name);
  };

  const moveStep = (section: "pre" | "prep", i: number, dir: 1 | -1) => {
    const pre = prePrepSteps.slice();
    const prep = prepSteps.slice();
    const arr = section === "pre" ? pre : prep;
    const j = i + dir;
    if (j >= 0 && j < arr.length) {
      [arr[i], arr[j]] = [arr[j], arr[i]];
    } else if (dir === 1 && section === "pre") {
      const [item] = pre.splice(i, 1);
      prep.unshift(item);
    } else if (dir === -1 && section === "prep") {
      const [item] = prep.splice(i, 1);
      pre.push(item);
    }
    setPrePrepSteps(pre);
    setPrepSteps(prep);
  };

  const commit = () => {
    const t = title.trim();
    if (!t) return;
    const cleanSteps = (arr: RecipeStep[]) => arr.map((s) => ({ ...s, t: (s.t || "").trim() })).filter((s) => s.t);
    const draft = {
      title: t,
      meta: metaTags.slice(),
      tiempo: tiempo.trim() || "—",
      ingredientes: ingList.filter((o) => o.name.trim()).map(buildIngString),
      utensilios: utnList.slice(),
      prePrep: cleanSteps(prePrepSteps),
      prep: cleanSteps(prepSteps),
    };
    const id = actions.saveRecipe(view, editingId, draft);
    onSaved(id);
  };

  return (
    <>
      <button className="link-back" onClick={onCancel}>
        ← Recetas
      </button>

      <div className="card">
        <div className="card-head">
          <h2>Información</h2>
        </div>
        <div className="card-body">
          <input className="field" placeholder="Título" value={title} onChange={(e) => setTitle(e.target.value)} style={{ marginBottom: 8 }} />
          <div style={{ marginBottom: 8 }}>
            <TagAutocomplete tags={metaTags} onAdd={(t) => setMetaTags((m) => [...m, t])} onRemove={(t) => setMetaTags((m) => m.filter((x) => x !== t))} allOptions={allMetaTags} placeholder="Buscar o agregar etiqueta" accent={accent} />
          </div>
          <input className="field" placeholder="Tiempo estimado total (ej: 30 min)" value={tiempo} onChange={(e) => setTiempo(e.target.value)} />
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <span className="card-emoji">{view === "repo" ? "🍫" : "🍅"}</span>
          <h2>Ingredientes</h2>
        </div>
        <div className="card-body">
          <IngredientTagInput
            items={ingList}
            onChangeName={(i, name) => setIngList((l) => l.map((o, idx) => (idx === i ? { ...o, name } : o)))}
            onAdjust={(i, dir) => setIngList((l) => l.map((o, idx) => (idx !== i ? o : { ...o, amount: roundFor((o.amount || 0) + dir * stepFor(o.unit), o.unit) })))}
            onRemove={(i) => setIngList((l) => l.filter((_, idx) => idx !== i))}
            onAdd={addIngredientTag}
            pantryNames={pantryIngredientNames}
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
          <TagAutocomplete tags={utnList} onAdd={(t) => setUtnList((l) => [...l, t])} onRemove={(t) => setUtnList((l) => l.filter((x) => x !== t))} allOptions={allUtensilios} placeholder="Buscar o agregar utensilio" accent={accent} />
        </div>
      </div>

      <h3 className="section-title">Pre-preparación</h3>
      <StepsEditor
        steps={prePrepSteps}
        offset={0}
        onChangeText={(i, t) => setPrePrepSteps((s) => s.map((st, idx) => (idx === i ? { ...st, t } : st)))}
        onChangeTitle={(i, title2) => setPrePrepSteps((s) => s.map((st, idx) => (idx === i ? { ...st, title: title2 } : st)))}
        onRemove={(i) => setPrePrepSteps((s) => s.filter((_, idx) => idx !== i))}
        onMoveUp={(i) => moveStep("pre", i, -1)}
        onMoveDown={(i) => moveStep("pre", i, 1)}
        onAdd={() => setPrePrepSteps((s) => [...s, { t: "", title: "" }])}
        accent={accent}
      />

      <h3 className="section-title">Preparación</h3>
      <StepsEditor
        steps={prepSteps}
        offset={prePrepSteps.length}
        onChangeText={(i, t) => setPrepSteps((s) => s.map((st, idx) => (idx === i ? { ...st, t } : st)))}
        onChangeTitle={(i, title2) => setPrepSteps((s) => s.map((st, idx) => (idx === i ? { ...st, title: title2 } : st)))}
        onRemove={(i) => setPrepSteps((s) => s.filter((_, idx) => idx !== i))}
        onMoveUp={(i) => moveStep("prep", i, -1)}
        onMoveDown={(i) => moveStep("prep", i, 1)}
        onAdd={() => setPrepSteps((s) => [...s, { t: "", title: "" }])}
        accent={accent}
      />

      <div className="form-actions">
        <button className="save" style={{ background: accent }} onClick={commit}>
          {editingId ? "Guardar cambios" : "Guardar receta"}
        </button>
        <button className="cancel" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </>
  );
}
