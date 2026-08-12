import type { RecipeStep } from "../../core/types";

export function StepsEditor({
  steps,
  offset,
  onChangeText,
  onChangeTitle,
  onRemove,
  onMoveUp,
  onMoveDown,
  onAdd,
  accent,
}: {
  steps: RecipeStep[];
  offset: number;
  onChangeText: (i: number, text: string) => void;
  onChangeTitle: (i: number, title: string) => void;
  onRemove: (i: number) => void;
  onMoveUp: (i: number) => void;
  onMoveDown: (i: number) => void;
  onAdd: () => void;
  accent: string;
}) {
  return (
    <div className="steps-editor">
      {steps.map((st, i) => (
        <div className="step-card" key={i}>
          <div className="step-card-head">
            <div className="step-n" style={{ color: accent }}>
              {offset + i + 1}
            </div>
            <input className="step-title" value={st.title || ""} onChange={(e) => onChangeTitle(i, e.target.value)} placeholder={`Paso ${offset + i + 1}`} />
            <button type="button" disabled={i === 0} onClick={() => onMoveUp(i)} className="step-move">
              ↑
            </button>
            <button type="button" disabled={i === steps.length - 1} onClick={() => onMoveDown(i)} className="step-move">
              ↓
            </button>
            <button type="button" className="step-remove" onClick={() => onRemove(i)}>
              ×
            </button>
          </div>
          <div className="step-card-body">
            <input value={st.t} onChange={(e) => onChangeText(i, e.target.value)} placeholder="Descripción del paso" />
          </div>
        </div>
      ))}
      <button type="button" className="step-add" style={{ color: accent }} onClick={onAdd}>
        + Agregar paso
      </button>
    </div>
  );
}
