import { useState } from "react";
import type { Ingredient, Recipe, RecipeView } from "../../core/types";
import type { Actions } from "../../core/store";
import { computeIngredientAvailability, splitIngredient, stepId } from "../../core/logic";

function StepList({
  steps,
  view,
  recId,
  section,
  offset,
  rDone,
  accent,
  actions,
}: {
  steps: Recipe["prep"];
  view: RecipeView;
  recId: string;
  section: "pre" | "prep";
  offset: number;
  rDone: Record<string, true>;
  accent: string;
  actions: Actions;
}) {
  const [editingIdx, setEditingIdx] = useState<number | null>(null);
  const [noteInput, setNoteInput] = useState("");

  return (
    <div className="steps-list">
      {steps.map((st, i) => {
        const id = stepId(view, recId, section, i);
        const done = !!rDone[id];
        const n = offset + i + 1;
        const isEditing = editingIdx === i;
        return (
          <div key={i} className={"step-card" + (done ? " done" : "")} onClick={() => actions.toggleStep(id)}>
            <div className="step-card-head">
              <div className="step-circle" style={{ background: done ? accent : undefined, borderColor: done ? accent : undefined, color: done ? "#fff" : undefined }}>
                {done ? "✓" : n}
              </div>
              <h2 className="step-title">{st.title && st.title.trim() ? st.title : `Paso ${n}`}</h2>
              <button
                type="button"
                className="note-icon"
                title="Editar paso"
                onClick={(e) => {
                  e.stopPropagation();
                  setEditingIdx(i);
                  setNoteInput(st.t);
                }}
              >
                🗒
              </button>
            </div>
            <div className="step-card-body">
              {!isEditing && <p className={"step-text" + (done ? " done" : "")}>{st.t}</p>}
              {st.time && <div className="step-time" style={{ color: accent }}>⏱ {st.time}</div>}
              {isEditing && (
                <div onClick={(e) => e.stopPropagation()} style={{ marginTop: 10 }}>
                  <textarea className="note-textarea" value={noteInput} onChange={(e) => setNoteInput(e.target.value)} placeholder="Editar paso…" />
                  <div className="note-actions">
                    <button
                      type="button"
                      style={{ background: accent }}
                      onClick={() => {
                        actions.saveStepText(view, recId, section, i, noteInput.trim());
                        setEditingIdx(null);
                      }}
                    >
                      Guardar
                    </button>
                    <button type="button" className="note-cancel" onClick={() => setEditingIdx(null)}>
                      Cancelar
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

export function RecipeDetail({
  view,
  recipe,
  servings,
  pantryLinked,
  pantryFlat,
  rDone,
  accent,
  actions,
  onClose,
  onEdit,
}: {
  view: RecipeView;
  recipe: Recipe;
  servings: number;
  pantryLinked: boolean;
  pantryFlat: Ingredient[];
  rDone: Record<string, true>;
  accent: string;
  actions: Actions;
  onClose: () => void;
  onEdit: () => void;
}) {
  const { ingEn, ingPocas, ingSin } = computeIngredientAvailability(recipe.ingredientes, pantryFlat, servings);
  const plainIngredientes = recipe.ingredientes.map(splitIngredient);
  const ratio = servings / 4;

  return (
    <>
      <button className="link-back" onClick={onClose}>
        ← Recetas
      </button>

      <div className="card">
        <div className="card-head">
          <span className="card-emoji">{view === "repo" ? "🍫" : "🍅"}</span>
          <h2>Ingredientes</h2>
        </div>
        <div className="card-body">
          {!pantryLinked ? (
            <ul className="ing-grid">
              {plainIngredientes.map((ig, i) => {
                const num = ig.num ? String(Math.round(parseFloat(ig.num.replace(",", ".")) * ratio * 100) / 100) : ig.num;
                return (
                  <li key={i}>
                    <span className="ing-name">
                      <span className="dot" style={{ background: accent }} />
                      {ig.name}
                    </span>
                    <span className="ing-num">{num}</span>
                    <span className="ing-unit">{num ? ig.unit : ""}</span>
                  </li>
                );
              })}
            </ul>
          ) : (
            <>
              {ingEn.length > 0 && (
                <>
                  <div className="avail-label" style={{ color: "#5A8F3C" }}>
                    En Despensa
                  </div>
                  <ul className="ing-grid">
                    {ingEn.map((ig, i) => (
                      <li key={i}>
                        <span className="ing-name">
                          <span className="dot" style={{ background: "#5A8F3C" }} />
                          {ig.name}
                        </span>
                        <span className="ing-num">{ig.numDisplay}</span>
                        <span className="ing-unit">{ig.unitDisplay}</span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
              {ingPocas.length > 0 && (
                <>
                  <div className="avail-label" style={{ color: "#B07A2E" }}>
                    Pocas unidades
                  </div>
                  <ul className="ing-grid">
                    {ingPocas.map((ig, i) => (
                      <li key={i}>
                        <span className="ing-name">
                          <span className="dot" style={{ background: "#B07A2E" }} />
                          {ig.name}
                        </span>
                        <span className="ing-num">{ig.numDisplay}</span>
                        <span className="ing-unit">{ig.unitDisplay}</span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
              {ingSin.length > 0 && (
                <>
                  <div className="avail-label" style={{ color: "#C1494E" }}>
                    Sin unidades
                  </div>
                  <ul className="ing-grid">
                    {ingSin.map((ig, i) => (
                      <li key={i}>
                        <span className="ing-name">
                          <span className="dot" style={{ background: "#C1494E" }} />
                          {ig.name}
                        </span>
                        <span className="ing-num">{ig.numDisplay}</span>
                        <span className="ing-unit">{ig.unitDisplay}</span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <span className="card-emoji">🔪</span>
          <h2>Utensilios</h2>
        </div>
        <div className="card-body">
          <ul className="utn-list">
            {recipe.utensilios.map((u, i) => (
              <li key={i}>
                <span className="dot" style={{ background: accent }} />
                {u}
              </li>
            ))}
          </ul>
        </div>
      </div>

      {recipe.prePrep.length > 0 && (
        <>
          <h3 className="section-title">Pre-preparación</h3>
          <StepList steps={recipe.prePrep} view={view} recId={recipe.id} section="pre" offset={0} rDone={rDone} accent={accent} actions={actions} />
        </>
      )}

      <h3 className="section-title">
        Preparación <span className="section-time" style={{ color: accent }}>⏱ {recipe.tiempo}</span>
      </h3>
      <StepList steps={recipe.prep} view={view} recId={recipe.id} section="prep" offset={recipe.prePrep.length} rDone={rDone} accent={accent} actions={actions} />

      <div className="detail-actions">
        <button className="edit-recipe" style={{ background: accent }} onClick={onEdit}>
          ✎ Editar receta
        </button>
        <button
          className="delete-recipe"
          onClick={() => {
            actions.deleteRecipe(view, recipe.id);
            onClose();
          }}
        >
          Eliminar receta
        </button>
      </div>
    </>
  );
}
