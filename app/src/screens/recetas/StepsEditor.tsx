import type { RecipeStep } from "../../core/types";

/** Editor de pasos: título editable, texto del paso y nota, como campos separados. */
export function StepsEditor({
  steps,
  offset,
  onChange,
  onRemove,
  onMoveUp,
  onMoveDown,
  onAdd,
  accent,
}: {
  steps: RecipeStep[];
  offset: number;
  onChange: (i: number, patch: Partial<RecipeStep>) => void;
  onRemove: (i: number) => void;
  onMoveUp: (i: number) => void;
  onMoveDown: (i: number) => void;
  onAdd: () => void;
  accent: string;
}) {
  return (
    <div className="steps-editor">
      {steps.map((st, i) => (
        <div className="step-edit" key={i}>
          <div className="step-edit-head">
            <span className="step-n" style={{ color: accent }}>
              {offset + i + 1}
            </span>
            <input
              className="step-title-input"
              value={st.title ?? ""}
              onChange={(e) => onChange(i, { title: e.target.value })}
              placeholder={`Paso ${offset + i + 1}`}
            />
            <input
              className="step-time-input"
              value={st.time ?? ""}
              onChange={(e) => onChange(i, { time: e.target.value })}
              placeholder="⏱"
            />
            <button type="button" disabled={i === 0} onClick={() => onMoveUp(i)} className="step-move" aria-label="Subir">
              ↑
            </button>
            <button
              type="button"
              disabled={i === steps.length - 1}
              onClick={() => onMoveDown(i)}
              className="step-move"
              aria-label="Bajar"
            >
              ↓
            </button>
            <button type="button" className="step-move" onClick={() => onRemove(i)} aria-label="Quitar paso">
              ×
            </button>
          </div>
          <textarea
            className="step-text-input"
            rows={2}
            value={st.t}
            onChange={(e) => onChange(i, { t: e.target.value })}
            placeholder="Texto del paso"
          />
          <textarea
            className="step-note-input"
            rows={1}
            value={st.note ?? ""}
            onChange={(e) => onChange(i, { note: e.target.value })}
            placeholder="Nota (opcional)"
          />
        </div>
      ))}
      <button type="button" className="step-add" style={{ color: accent }} onClick={onAdd}>
        + Agregar paso
      </button>
    </div>
  );
}
