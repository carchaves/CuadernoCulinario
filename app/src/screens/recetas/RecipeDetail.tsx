import { useState } from "react";
import type { Ingredient, Recipe, RecipeStep, RecipeView } from "../../core/types";
import { computeIngredientAvailability, splitIngredient, stepId } from "../../core/logic";

type Section = "pre" | "prep";

function StepCard({
  step,
  n,
  done,
  editable,
  accent,
  onToggle,
  onPatch,
}: {
  step: RecipeStep;
  n: number;
  done: boolean;
  editable: boolean;
  accent: string;
  onToggle: () => void;
  onPatch: (patch: Partial<RecipeStep>) => void;
}) {
  const [editing, setEditing] = useState(false);

  return (
    <div className={"step-card" + (done ? " done" : "")}>
      <div className="step-card-head">
        <button
          type="button"
          className="step-circle"
          style={done ? { background: accent, borderColor: accent, color: "#fff" } : undefined}
          onClick={onToggle}
          aria-label={done ? "Marcar como pendiente" : "Marcar como hecho"}
        >
          {done ? "✓" : n}
        </button>
        {editing ? (
          <input
            className="step-title-input"
            value={step.title ?? ""}
            placeholder={`Paso ${n}`}
            onChange={(e) => onPatch({ title: e.target.value })}
          />
        ) : (
          <h4 className="step-title">{step.title?.trim() ? step.title : `Paso ${n}`}</h4>
        )}
        {step.time && !editing && <span className="step-time">⏱ {step.time}</span>}
        {editable && (
          <button type="button" className="step-move" onClick={() => setEditing((v) => !v)} aria-label="Editar paso">
            {editing ? "✓" : "✎"}
          </button>
        )}
      </div>
      <div className="step-card-body">
        {editing ? (
          <>
            <textarea
              className="step-text-input"
              rows={2}
              value={step.t}
              placeholder="Texto del paso"
              onChange={(e) => onPatch({ t: e.target.value })}
            />
            <textarea
              className="step-note-input"
              rows={1}
              value={step.note ?? ""}
              placeholder="Nota (opcional)"
              onChange={(e) => onPatch({ note: e.target.value })}
            />
            <input
              className="step-time-input wide"
              value={step.time ?? ""}
              placeholder="Tiempo del paso (ej: 10 min)"
              onChange={(e) => onPatch({ time: e.target.value })}
            />
          </>
        ) : (
          <>
            <p className={"step-text" + (done ? " done" : "")}>{step.t}</p>
            {step.note && <p className="step-note">{step.note}</p>}
          </>
        )}
      </div>
    </div>
  );
}

function IngGroup({ label, color, items }: { label: string; color: string; items: { name: string; numDisplay: string; unitDisplay: string }[] }) {
  if (items.length === 0) return null;
  return (
    <>
      <div className="avail-label" style={{ color }}>
        {label}
      </div>
      <ul className="ing-grid">
        {items.map((ig, i) => (
          <li key={i}>
            <span className="ing-name">
              <span className="dot" style={{ background: color }} />
              {ig.name}
            </span>
            <span className="ing-num">{ig.numDisplay}</span>
            <span className="ing-unit">{ig.unitDisplay}</span>
          </li>
        ))}
      </ul>
    </>
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
  editable,
  onToggleStep,
  onPatchStep,
  onEdit,
  onDelete,
}: {
  view: RecipeView;
  recipe: Recipe;
  servings: number;
  pantryLinked: boolean;
  pantryFlat: Ingredient[];
  rDone: Record<string, true>;
  accent: string;
  editable: boolean;
  onToggleStep: (id: string) => void;
  onPatchStep: (section: Section, i: number, patch: Partial<RecipeStep>) => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { ingEn, ingPocas, ingSin } = computeIngredientAvailability(recipe.ingredientes, pantryFlat, servings);
  const ratio = servings / 4;

  const total = recipe.prePrep.length + recipe.prep.length;
  let done = 0;
  recipe.prePrep.forEach((_, i) => rDone[stepId(view, recipe.id, "pre", i)] && done++);
  recipe.prep.forEach((_, i) => rDone[stepId(view, recipe.id, "prep", i)] && done++);
  const pct = total ? Math.round((done / total) * 100) : 0;

  const renderSteps = (steps: RecipeStep[], section: Section, offset: number) => (
    <div className="steps-list">
      {steps.map((st, i) => {
        const id = stepId(view, recipe.id, section, i);
        return (
          <StepCard
            key={i}
            step={st}
            n={offset + i + 1}
            done={!!rDone[id]}
            editable={editable}
            accent={accent}
            onToggle={() => onToggleStep(id)}
            onPatch={(patch) => onPatchStep(section, i, patch)}
          />
        );
      })}
    </div>
  );

  return (
    <div className="detail">
      <div className="detail-head">
        <div>
          <h2 className="detail-title" style={{ color: accent }}>
            {recipe.title}
          </h2>
          <div className="rcard-chips">
            {recipe.meta.map((m, i) => (
              <span className="chip" key={i}>
                {m}
              </span>
            ))}
            <span className="chip" style={{ background: `${accent}22`, color: accent }}>
              ⏱ {recipe.tiempo}
            </span>
          </div>
        </div>
        {editable && (
          <div className="detail-actions">
            <button onClick={onEdit}>✎ Editar</button>
            <button onClick={onDelete}>🗑 Borrar</button>
          </div>
        )}
      </div>

      <div className="meter-wrap">
        <div className="track">
          <div className="fill" style={{ width: `${pct}%`, background: accent }} />
        </div>
        <div className="meter-count">
          {done} / {total} pasos
        </div>
      </div>

      <div className="detail-cols">
        <div className="card">
          <div className="card-head">
            <span className="card-emoji">{view === "repo" ? "🍫" : "🍅"}</span>
            <h2>Ingredientes</h2>
          </div>
          <div className="card-body">
            {!pantryLinked ? (
              <ul className="ing-grid">
                {recipe.ingredientes.map(splitIngredient).map((ig, i) => {
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
                <IngGroup label="En Despensa" color="#5A8F3C" items={ingEn} />
                <IngGroup label="Pocas unidades" color="#B07A2E" items={ingPocas} />
                <IngGroup label="Sin unidades" color="#C1494E" items={ingSin} />
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
              {recipe.utensilios.length === 0 && <li className="muted-i">Sin utensilios cargados.</li>}
              {recipe.utensilios.map((u, i) => (
                <li key={i}>
                  <span className="dot" style={{ background: accent }} />
                  {u}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {recipe.prePrep.length > 0 && (
        <>
          <h3 className="section-title">Pre-preparación</h3>
          {renderSteps(recipe.prePrep, "pre", 0)}
        </>
      )}

      <h3 className="section-title">
        Preparación{" "}
        <span className="section-time" style={{ color: accent }}>
          ⏱ {recipe.tiempo}
        </span>
      </h3>
      {renderSteps(recipe.prep, "prep", recipe.prePrep.length)}
    </div>
  );
}
