import { useState } from "react";
import { fmt } from "../../core/logic";

export interface IngredientTagItem {
  id: string;
  name: string;
  amount: number;
  unit: string;
}

export function IngredientTagInput({
  items,
  onChangeName,
  onAdjust,
  onRemove,
  onAdd,
  pantryNames,
  accent,
}: {
  items: IngredientTagItem[];
  onChangeName: (i: number, name: string) => void;
  onAdjust: (i: number, dir: 1 | -1) => void;
  onRemove: (i: number) => void;
  onAdd: (name: string) => void;
  pantryNames: string[];
  accent: string;
}) {
  const [input, setInput] = useState("");
  const [focused, setFocused] = useState(false);

  const q = input.trim().toLowerCase();
  const options = pantryNames.filter((n) => !items.some((o) => o.name === n) && (!q || n.toLowerCase().includes(q)));
  const showAddNew = q.length > 0 && !pantryNames.some((n) => n.toLowerCase() === q) && !items.some((o) => o.name === input.trim());
  const showOptions = focused && (options.length > 0 || showAddNew);

  const commit = (name: string) => {
    if (name) onAdd(name);
    setInput("");
  };

  return (
    <div>
      {items.length > 0 && (
        <ul className="ingtag-list">
          {items.map((it, i) => (
            <li key={it.id}>
              <span className="dot" style={{ background: accent }} />
              <input className="ingtag-name" value={it.name} onChange={(e) => onChangeName(i, e.target.value)} />
              <span className="ingtag-stepper">
                <button type="button" onClick={() => onAdjust(i, -1)}>
                  −
                </button>
                <span className="ingtag-amt">{fmt(it.amount, it.unit)}</span>
                <button type="button" onClick={() => onAdjust(i, 1)}>
                  +
                </button>
                <span className="ingtag-unit">{it.unit}</span>
              </span>
              <button type="button" className="ingtag-remove" onClick={() => onRemove(i)}>
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
      <div className="tagac">
        <div className="tagac-box">
          <input
            placeholder="Buscar en despensa o agregar ingrediente"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onFocus={() => setFocused(true)}
            onBlur={() => setTimeout(() => setFocused(false), 150)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                commit(input.trim());
              }
            }}
          />
        </div>
        {showOptions && (
          <div className="tagac-options">
            {options.map((opt) => (
              <button key={opt} type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => commit(opt)}>
                {opt}
              </button>
            ))}
            {showAddNew && (
              <button type="button" className="tagac-addnew" style={{ color: accent }} onMouseDown={(e) => e.preventDefault()} onClick={() => commit(input.trim())}>
                + Agregar "{input.trim()}" (y a despensa)
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
