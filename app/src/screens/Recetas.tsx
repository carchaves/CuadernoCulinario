import { useMemo, useState } from "react";
import type { AppState, Recipe, RecipeStep, RecipeView } from "../core/types";
import type { AppStore } from "../core/useAppState";
import { hasAllIngredients, parseMinutes, stepId } from "../core/logic";
import { RecipeDetail } from "./recetas/RecipeDetail";
import { RecipeForm, type RecipeDraft } from "./recetas/RecipeForm";

const ACCENT = "#C4562A";

export function Recetas({ store, state }: { store: AppStore; state: AppState }) {
  const editable = !store.readOnly;
  const { mutate } = store;

  const [view, setView] = useState<RecipeView>("cocina");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [servings, setServings] = useState(4);
  const [sortByTime, setSortByTime] = useState(false);
  const [filterByPantry, setFilterByPantry] = useState(false);
  const [pantryLinked, setPantryLinked] = useState(true);

  const pantryFlat = useMemo(() => state.pPages.flatMap((p) => p.ingredients), [state.pPages]);
  const pantryNames = useMemo(() => pantryFlat.map((i) => i.name.toLowerCase()), [pantryFlat]);

  const recipes = state.recipes[view] ?? [];
  const selected = selectedId ? recipes.find((r) => r.id === selectedId) ?? null : null;
  const editingRecipe = editingId ? recipes.find((r) => r.id === editingId) ?? null : null;

  const allRecipes = [...state.recipes.cocina, ...state.recipes.repo];
  const allMetaTags = [...new Set(allRecipes.flatMap((r) => r.meta))].sort();
  const allUtensilios = [...new Set(allRecipes.flatMap((r) => r.utensilios))].sort();
  const allIngredientes = [
    ...new Set([...pantryFlat.map((i) => i.name), ...allRecipes.flatMap((r) => r.ingredientes)]),
  ].sort();

  // ---- Acciones ------------------------------------------------------------

  const saveRecipe = (id: string, draft: RecipeDraft) => {
    mutate((s) => {
      const list = s.recipes[view];
      const exists = list.some((r) => r.id === id);
      const next: Recipe = { id, ...draft };
      return {
        ...s,
        recipes: { ...s.recipes, [view]: exists ? list.map((r) => (r.id === id ? next : r)) : [...list, next] },
      };
    });
    setCreating(false);
    setEditingId(null);
    setSelectedId(id);
  };

  const deleteRecipe = (id: string) => {
    if (!confirm("¿Borrar esta receta?")) return;
    mutate((s) => ({ ...s, recipes: { ...s.recipes, [view]: s.recipes[view].filter((r) => r.id !== id) } }));
    setSelectedId(null);
  };

  const toggleStep = (id: string) =>
    mutate((s) => {
      const rDone = { ...s.rDone };
      if (rDone[id]) delete rDone[id];
      else rDone[id] = true;
      return { ...s, rDone };
    });

  const patchStep = (recId: string, section: "pre" | "prep", i: number, patch: Partial<RecipeStep>) => {
    const key = section === "pre" ? "prePrep" : "prep";
    mutate((s) => ({
      ...s,
      recipes: {
        ...s.recipes,
        [view]: s.recipes[view].map((r) =>
          r.id !== recId ? r : { ...r, [key]: r[key].map((st, idx) => (idx === i ? { ...st, ...patch } : st)) }
        ),
      },
    }));
  };

  // ---- Lista de la izquierda ----------------------------------------------

  let items = recipes.map((r, idx) => {
    let done = 0;
    const total = r.prePrep.length + r.prep.length;
    r.prePrep.forEach((_, i) => rDoneAt(state, view, r.id, "pre", i) && done++);
    r.prep.forEach((_, i) => rDoneAt(state, view, r.id, "prep", i) && done++);
    return {
      n: idx + 1,
      recipe: r,
      done,
      total,
      mins: parseMinutes(r.tiempo),
      available: hasAllIngredients(r.ingredientes, pantryNames),
    };
  });
  if (sortByTime) items = items.slice().sort((a, b) => a.mins - b.mins);
  const avail = filterByPantry ? items.filter((x) => x.available) : items;
  const rest = filterByPantry ? items.filter((x) => !x.available) : [];

  const card = (info: (typeof items)[number]) => (
    <button
      key={info.recipe.id}
      className={"rcard" + (info.recipe.id === selectedId ? " on" : "")}
      onClick={() => {
        setSelectedId(info.recipe.id);
        setCreating(false);
        setEditingId(null);
      }}
      style={info.recipe.id === selectedId ? { borderColor: ACCENT } : undefined}
    >
      <span className="rcard-n" style={{ color: ACCENT }}>
        {info.n}
      </span>
      <span className="rcard-body">
        <span className="rcard-title">{info.recipe.title}</span>
        <span className="rcard-chips">
          {info.recipe.meta.slice(0, 3).map((m, i) => (
            <span className="chip" key={i}>
              {m}
            </span>
          ))}
          <span className="chip" style={{ background: `${ACCENT}22`, color: ACCENT }}>
            ⏱ {info.recipe.tiempo}
          </span>
        </span>
      </span>
      <span className="rcard-count" style={{ color: ACCENT }}>
        {info.done}/{info.total}
      </span>
    </button>
  );

  return (
    <div className="recetas-root">
      <style>{css}</style>

      <aside className="rlist">
        <div className="rlist-head">
          <h1>Recetas</h1>
          <button
            className={"pantry-toggle" + (pantryLinked ? " on" : "")}
            style={pantryLinked ? { background: ACCENT, borderColor: ACCENT } : undefined}
            aria-label="Usar Despensa"
            title="Usar Despensa"
            onClick={() => {
              setPantryLinked((v) => !v);
              if (pantryLinked) setFilterByPantry(false);
            }}
          >
            <svg viewBox="0 0 52 52" width="20" height="20" fill="none" stroke={pantryLinked ? "#FDFBF6" : "#292019"} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 18 h22 v19 a3 3 0 0 1 -3 3 H18 a3 3 0 0 1 -3 -3 Z" />
              <path d="M13 16 h26" />
              <path d="M26 12 v4" />
              {!pantryLinked && <path d="M42 10 L10 42" />}
            </svg>
          </button>
        </div>

        <div className="tabs">
          {(["cocina", "repo"] as RecipeView[]).map((v) => (
            <button
              key={v}
              className={"tab" + (view === v ? " active" : "")}
              onClick={() => {
                setView(v);
                setSelectedId(null);
                setCreating(false);
                setEditingId(null);
              }}
            >
              {v === "cocina" ? "🍲 Cocina" : "🧁 Repostería"}
            </button>
          ))}
        </div>

        <div className="filters">
          <button
            className={"filter-btn" + (sortByTime ? " on" : "")}
            style={sortByTime ? { background: ACCENT, borderColor: ACCENT, color: "#fff" } : undefined}
            onClick={() => setSortByTime((v) => !v)}
          >
            ⏱ Menor tiempo
          </button>
          {pantryLinked && (
            <button
              className={"filter-btn" + (filterByPantry ? " on" : "")}
              style={filterByPantry ? { background: ACCENT, borderColor: ACCENT, color: "#fff" } : undefined}
              onClick={() => setFilterByPantry((v) => !v)}
            >
              🍅 Disponibles
            </button>
          )}
        </div>

        {editable && (
          <button
            className="new-recipe"
            onClick={() => {
              setCreating(true);
              setEditingId(null);
              setSelectedId(null);
            }}
          >
            + Nueva receta
          </button>
        )}

        <div className="rlist-scroll">
          {avail.map(card)}
          {rest.length > 0 && (
            <div className="divider">
              <span />
              Faltan ingredientes
              <span />
            </div>
          )}
          {rest.map((info) => (
            <div key={info.recipe.id} style={{ opacity: 0.6 }}>
              {card(info)}
            </div>
          ))}
          {items.length === 0 && <p className="muted-i">No hay recetas en esta vista.</p>}
        </div>
      </aside>

      <section className="rdetail">
        <div className="servings-row">
          <span className="servings-label">PARA</span>
          <button className="servings-btn" onClick={() => setServings((n) => Math.max(1, n - 1))}>
            −
          </button>
          <span className="servings-n">{servings}</span>
          <button className="servings-btn" onClick={() => setServings((n) => Math.min(24, n + 1))}>
            +
          </button>
          <span className="servings-label">personas</span>
        </div>

        {creating || editingRecipe ? (
          <RecipeForm
            view={view}
            editing={editingRecipe}
            allMetaTags={allMetaTags}
            allIngredientes={allIngredientes}
            allUtensilios={allUtensilios}
            accent={ACCENT}
            onSave={saveRecipe}
            onCancel={() => {
              setCreating(false);
              setEditingId(null);
            }}
          />
        ) : selected ? (
          <RecipeDetail
            view={view}
            recipe={selected}
            servings={servings}
            pantryLinked={pantryLinked}
            pantryFlat={pantryFlat}
            rDone={state.rDone}
            accent={ACCENT}
            editable={editable}
            onToggleStep={toggleStep}
            onPatchStep={(section, i, patch) => patchStep(selected.id, section, i, patch)}
            onEdit={() => setEditingId(selected.id)}
            onDelete={() => deleteRecipe(selected.id)}
          />
        ) : (
          <p className="muted-i pad">Elegí una receta de la izquierda.</p>
        )}
      </section>
    </div>
  );
}

const rDoneAt = (state: AppState, view: RecipeView, recId: string, section: "pre" | "prep", i: number) =>
  !!state.rDone[stepId(view, recId, section, i)];

const css = `
.recetas-root{display:grid;grid-template-columns:minmax(300px,34%) 1fr;height:100%;
  background:#F6F2EA;color:#292019;font-family:'DM Sans',ui-sans-serif,system-ui,sans-serif;line-height:1.45}
.recetas-root *{box-sizing:border-box}
.rlist{border-right:1px solid #E7D8BC;padding:22px 18px;display:flex;flex-direction:column;gap:12px;
  max-height:100%;overflow:hidden}
.rlist-head{display:flex;align-items:center;justify-content:space-between;gap:12px}
.rlist-head h1{font-family:'Instrument Serif','Instrument Serif',Georgia,serif;font-weight:400;font-size:29px;line-height:1;margin:0}
.pantry-toggle{width:38px;height:38px;flex:none;border-radius:50%;border:1px solid #E7D8BC;background:#FDFBF6;
  display:grid;place-items:center;cursor:pointer}
.tabs{display:flex;gap:4px;background:#EBE5D9;padding:4px;border-radius:12px}
.tab{flex:1;border:none;background:transparent;cursor:pointer;font-family:inherit;font-weight:600;font-size:13.5px;
  color:#7A7062;padding:8px;border-radius:9px}
.tab.active{background:#FDFBF6;color:#292019;box-shadow:0 1px 3px rgba(41,32,25,.12)}
.filters{display:flex;gap:8px;flex-wrap:wrap}
.filter-btn{border:1px solid #E7D8BC;background:#FDFBF6;color:#7A7062;border-radius:9px;padding:7px 12px;
  font-family:inherit;font-size:12.5px;font-weight:600;cursor:pointer}
.new-recipe{border:1px dashed #C89A78;background:#FDFBF6;color:#C4562A;border-radius:10px;padding:9px;
  font-family:inherit;font-size:13px;font-weight:600;cursor:pointer}
.rlist-scroll{flex:1;overflow-y:auto;padding-right:4px}
.rcard{width:100%;text-align:left;background:#FDFBF6;border:1px solid #E7D8BC;border-radius:14px;margin-bottom:10px;
  padding:12px 14px;cursor:pointer;font-family:inherit;display:flex;align-items:flex-start;gap:10px}
.rcard-n{font-family:'Instrument Serif',Georgia,serif;font-style:italic;font-size:19px;line-height:1;width:22px;flex:none}
.rcard-body{flex:1;min-width:0;display:flex;flex-direction:column;gap:5px}
.rcard-title{font-family:'Instrument Serif','Instrument Serif',Georgia,serif;font-size:17px;color:#292019}
.rcard-chips{display:flex;flex-wrap:wrap;gap:5px}
.chip{font-size:10.5px;font-weight:600;padding:3px 8px;border-radius:20px;background:#EFEADF;color:#7A7062}
.rcard-count{font-size:12px;font-weight:700}
.divider{display:flex;align-items:center;gap:10px;margin:8px 0 12px;color:#A99A82;font-size:11px;font-weight:600;
  letter-spacing:.06em;text-transform:uppercase}
.divider span{flex:1;border-top:1px solid #E7D8BC}

.rdetail{padding:22px 28px 60px;overflow-y:auto;max-height:100%}
.servings-row{display:flex;align-items:center;gap:8px;margin-bottom:14px}
.servings-label{font-size:10.5px;letter-spacing:.2em;text-transform:uppercase;color:#7A7062;font-weight:600}
.servings-btn{width:22px;height:22px;border:1px solid #E7D8BC;background:#FDFBF6;border-radius:6px;
  display:grid;place-items:center;cursor:pointer;color:#7A7062;font-size:12px;padding:0}
.servings-n{font-size:13px;font-weight:700;min-width:16px;text-align:center}

.detail-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}
.detail-title{font-family:'Instrument Serif','Instrument Serif',Georgia,serif;font-weight:400;font-size:27px;line-height:1.05;margin:0 0 8px}
.detail-actions{display:flex;gap:6px;flex:none}
.detail-actions button{border:1px solid #E7D8BC;background:#FDFBF6;color:#7A7062;border-radius:9px;padding:6px 11px;
  font-family:inherit;font-size:12.5px;font-weight:600;cursor:pointer}
.meter-wrap{margin:14px 0 18px;display:flex;align-items:center;gap:12px}
.track{flex:1;height:8px;border-radius:20px;background:#E4DDD0;overflow:hidden}
.fill{height:100%;border-radius:20px;transition:width .3s}
.meter-count{font-size:12.5px;font-weight:600;color:#7A7062;white-space:nowrap}
.detail-cols{display:grid;grid-template-columns:1fr 1fr;gap:16px}

.card{background:#FDFBF6;border:1px solid #E7D8BC;border-radius:16px;margin-bottom:16px}
.card-head{padding:13px 16px;border-bottom:1px solid #E7D8BC;display:flex;align-items:center;gap:10px}
.card-head h2{font-family:'Instrument Serif','Instrument Serif',Georgia,serif;font-weight:400;font-size:18px;margin:0}
.card-emoji{font-size:19px}
.card-body{padding:14px 16px;display:flex;flex-direction:column;gap:10px}

.ing-grid{list-style:none;margin:0;padding:0;display:grid;grid-template-columns:1fr auto 62px;
  column-gap:10px;row-gap:7px;align-items:center}
.ing-grid li{display:contents}
.ing-name{font-size:14px;font-weight:500;display:flex;gap:8px;align-items:baseline}
.dot{width:5px;height:5px;border-radius:50%;flex:none;transform:translateY(-2px)}
.ing-num{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:13px;font-weight:600;text-align:right}
.ing-unit{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:11.5px;font-weight:600;color:#8A5A34;background:#F1E9E0;
  border-radius:7px;padding:3px 6px;text-align:center}
.avail-label{font-size:10.5px;font-weight:700;text-transform:uppercase;letter-spacing:.06em;margin:0}
.utn-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:7px}
.utn-list li{font-size:14px;font-weight:500;display:flex;gap:8px;align-items:baseline}

.section-title{font-family:'Instrument Serif',Georgia,serif;font-style:italic;font-weight:400;font-size:15px;margin:8px 0;color:#7A7062}
.section-time{font-style:normal;font-size:12px;font-weight:600;background:#F3EAD9;padding:2px 8px;border-radius:7px;margin-left:4px}
.steps-list{display:flex;flex-direction:column;gap:10px;margin-bottom:16px}
.step-card{background:#FDFBF6;border:1px solid #E7D8BC;border-radius:14px}
.step-card-head{padding:11px 14px;border-bottom:1px solid #E7D8BC;display:flex;align-items:center;gap:10px}
.step-circle{width:24px;height:24px;border-radius:50%;background:#EDE7DA;border:2px solid #E2DBCD;
  font-size:12px;font-weight:700;color:#7A7062;flex:none;cursor:pointer;padding:0}
.step-title{font-family:'Instrument Serif','Instrument Serif',Georgia,serif;font-weight:400;font-size:17px;margin:0;flex:1;min-width:0}
.step-card.done .step-title{color:#7A7062}
.step-time{font-size:11.5px;font-weight:600;background:#F3EAD9;padding:2px 8px;border-radius:7px;color:#8A5A34}
.step-move{border:none;background:none;color:#7A7062;font-size:14px;cursor:pointer;padding:2px 5px;border-radius:6px}
.step-move:disabled{opacity:.3;cursor:default}
.step-card-body{padding:12px 14px;display:flex;flex-direction:column;gap:8px}
.step-text{font-size:14.5px;font-weight:500;line-height:1.5;margin:0;color:#5C6353}
.step-text.done{color:#A99A82;text-decoration:line-through}
.step-note{margin:0;font-size:13px;line-height:1.5;color:#7A7062;font-style:italic;border-left:2px solid #E7D8BC;padding-left:10px}

.recipe-form .field,.step-title-input,.step-time-input,.step-text-input,.step-note-input{
  border:1px solid #E7D8BC;border-radius:9px;padding:8px 10px;font-family:inherit;font-size:14px;
  background:#FBF9F3;color:#292019;outline:none;width:100%;resize:vertical}
.step-title-input{font-family:'Instrument Serif',Georgia,serif;font-size:16px}
.step-time-input{width:76px;flex:none;text-align:center}
.step-time-input.wide{width:100%;text-align:left}
.step-note-input{font-style:italic;font-size:13px}
.steps-editor{display:flex;flex-direction:column;gap:10px;margin-bottom:16px}
.step-edit{background:#FDFBF6;border:1px solid #E7D8BC;border-radius:14px;padding:12px 14px;
  display:flex;flex-direction:column;gap:8px}
.step-edit-head{display:flex;align-items:center;gap:8px}
.step-n{font-family:'Instrument Serif',Georgia,serif;font-style:italic;font-size:17px;width:20px;flex:none}
.step-add{border:1px dashed #C89A78;background:#FDFBF6;border-radius:10px;padding:9px;font-family:inherit;
  font-size:13px;font-weight:600;cursor:pointer}
.form-actions{display:flex;gap:10px;margin-bottom:24px}
.save{border:none;color:#fff;border-radius:10px;padding:10px 18px;font-family:inherit;font-size:13.5px;
  font-weight:600;cursor:pointer}
.save:disabled{opacity:.5;cursor:default}
.cancel{border:1px solid #E7D8BC;background:#FDFBF6;color:#7A7062;border-radius:10px;padding:10px 18px;
  font-family:inherit;font-size:13.5px;font-weight:600;cursor:pointer}

.tagac{position:relative}
.tagac-box{display:flex;flex-wrap:wrap;gap:6px;border:1px solid #E7D8BC;border-radius:9px;padding:7px 8px;background:#FBF9F3}
.tagac-box input{flex:1;min-width:140px;border:none;background:transparent;outline:none;font-family:inherit;font-size:13.5px}
.tagac-tag{display:inline-flex;align-items:center;gap:5px;background:#EFEADF;color:#5C5346;border-radius:20px;
  padding:3px 6px 3px 10px;font-size:12.5px}
.tagac-tag button{border:none;background:none;color:#7A7062;cursor:pointer;font-size:14px;line-height:1;padding:0 2px}
.tagac-options{position:absolute;top:calc(100% + 4px);left:0;right:0;z-index:20;background:#FDFBF6;
  border:1px solid #E7D8BC;border-radius:10px;box-shadow:0 16px 30px -20px rgba(41,32,25,.5);
  max-height:220px;overflow-y:auto;display:flex;flex-direction:column}
.tagac-options button{border:none;background:none;text-align:left;font-family:inherit;font-size:13.5px;
  padding:8px 12px;cursor:pointer;color:#292019}
.tagac-options button:hover{background:#F3EEE3}
.tagac-addnew{font-weight:600}

.muted-i{color:#A99A82;font-size:13.5px;font-style:italic}
.pad{padding:24px 4px}
`;
