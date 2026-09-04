import { useState } from "react";

/** Input de etiquetas con autocompletado `contains` + opción explícita de agregar una nueva. */
export function TagAutocomplete({
  tags,
  onAdd,
  onRemove,
  allOptions,
  placeholder,
  accent,
}: {
  tags: string[];
  onAdd: (tag: string) => void;
  onRemove: (tag: string) => void;
  allOptions: string[];
  placeholder: string;
  accent: string;
}) {
  const [input, setInput] = useState("");
  const [focused, setFocused] = useState(false);

  const q = input.trim().toLowerCase();
  const options = allOptions.filter((t) => !tags.includes(t) && (!q || t.toLowerCase().includes(q))).slice(0, 8);
  const showAddNew = q.length > 0 && !allOptions.some((t) => t.toLowerCase() === q) && !tags.includes(input.trim());
  const showOptions = focused && (options.length > 0 || showAddNew);

  const commit = (tag: string) => {
    if (tag && !tags.includes(tag)) onAdd(tag);
    setInput("");
  };

  return (
    <div className="tagac">
      <div className="tagac-box">
        {tags.map((tag) => (
          <span key={tag} className="tagac-tag">
            {tag}
            <button type="button" onClick={() => onRemove(tag)} aria-label={`Quitar ${tag}`}>
              ×
            </button>
          </span>
        ))}
        <input
          placeholder={placeholder}
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
            <button
              type="button"
              className="tagac-addnew"
              style={{ color: accent }}
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => commit(input.trim())}
            >
              + Agregar "{input.trim()}"
            </button>
          )}
        </div>
      )}
    </div>
  );
}
