import { useMemo, useState } from "react";
import type { AppState, Ingredient, IngredientType, Unit } from "../core/types";
import type { AppStore } from "../core/useAppState";
import { fmt, roundFor, stepFor, uid } from "../core/logic";
import { Modal } from "./ui";
import { PAGE_ICONS, iconGlyph } from "../core/icons";

const UNITS: Unit[] = ["g", "kg", "ml", "L", "u"];

/**
 * Inventario de utensilios y equipamiento. Misma mecánica que `Despensa` (cajones editables +
 * ítems con cantidad), sobre `mPages`/`mActiveId` y sin la acción de enviar a la lista de compra.
 */
export function Menaje({ store, state }: { store: AppStore; state: AppState }) {
  const editable = !store.readOnly;
  const { mutate } = store;

  const [query, setQuery] = useState("");
  const [activeId, setActiveId] = useState<string | null>(state.mActiveId ?? state.mPages[0]?.id ?? null);
  const [newPage, setNewPage] = useState("");
  const [renaming, setRenaming] = useState<string | null>(null);
  const [renameText, setRenameText] = useState("");
  const [iconPicker, setIconPicker] = useState<string | null>(null);
  const [draft, setDraft] = useState<{ name: string; type: IngredientType; amount: string; unit: Unit }>({
    name: "",
    type: "unidad",
    amount: "",
    unit: "u",
  });

  const active = state.mPages.find((p) => p.id === activeId) ?? state.mPages[0] ?? null;

  const q = query.trim().toLowerCase();
  const results = useMemo(
    () =>
      q
        ? state.mPages.flatMap((p) =>
            p.ingredients
              .filter((i) => i.name.toLowerCase().includes(q))
              .map((i) => ({ ...i, pageId: p.id, pageName: p.name }))
          )
        : [],
    [q, state.mPages]
  );

  // ---- Acciones sobre el estado -------------------------------------------

  const addPage = () => {
    const name = newPage.trim();
    if (!name) return;
    const page = { id: uid(), name, iconId: PAGE_ICONS[0].id, ingredients: [] };
    mutate((s) => ({ ...s, mPages: [...s.mPages, page], mActiveId: page.id }));
    setActiveId(page.id);
    setNewPage("");
  };

  const renamePage = (pageId: string, name: string) => {
    if (!name.trim()) return;
    mutate((s) => ({ ...s, mPages: s.mPages.map((p) => (p.id === pageId ? { ...p, name: name.trim() } : p)) }));
  };

  const setPageIcon = (pageId: string, iconId: string) =>
    mutate((s) => ({ ...s, mPages: s.mPages.map((p) => (p.id === pageId ? { ...p, iconId } : p)) }));

  const deletePage = (pageId: string) => {
    if (!confirm("¿Borrar este cajón y todo su menaje?")) return;
    mutate((s) => {
      const mPages = s.mPages.filter((p) => p.id !== pageId);
      return { ...s, mPages, mActiveId: s.mActiveId === pageId ? mPages[0]?.id ?? null : s.mActiveId };
    });
    setActiveId((cur) => (cur === pageId ? state.mPages.find((p) => p.id !== pageId)?.id ?? null : cur));
  };

  const selectPage = (pageId: string) => {
    setActiveId(pageId);
    mutate((s) => (s.mActiveId === pageId ? s : { ...s, mActiveId: pageId }));
  };

  const addItem = () => {
    if (!active) return;
    const name = draft.name.trim();
    if (!name) return;
    const item: Ingredient = {
      id: uid(),
      name,
      type: draft.type,
      amount: parseFloat(draft.amount.replace(",", ".")) || 0,
      unit: draft.unit,
    };
    mutate((s) => ({
      ...s,
      mPages: s.mPages.map((p) => (p.id === active.id ? { ...p, ingredients: [...p.ingredients, item] } : p)),
    }));
    setDraft({ name: "", type: draft.type, amount: "", unit: draft.unit });
  };

  const setAmount = (pageId: string, itemId: string, amount: number) =>
    mutate((s) => ({
      ...s,
      mPages: s.mPages.map((p) =>
        p.id !== pageId
          ? p
          : {
              ...p,
              ingredients: p.ingredients.map((i) =>
                i.id !== itemId ? i : { ...i, amount: roundFor(amount, i.unit) }
              ),
            }
      ),
    }));

  const adjust = (pageId: string, item: Ingredient, dir: 1 | -1) =>
    setAmount(pageId, item.id, item.amount + dir * stepFor(item.unit));

  const removeItem = (pageId: string, itemId: string) =>
    mutate((s) => ({
      ...s,
      mPages: s.mPages.map((p) => (p.id !== pageId ? p : { ...p, ingredients: p.ingredients.filter((i) => i.id !== itemId) })),
    }));

  return (
    <div className="menaje-root">
      <style>{css}</style>

      <header className="masthead">
        <div className="mast-left">
          <div className="mast-mark" aria-hidden>
            ◧
          </div>
          <div>
            <h1 className="mast-title">Menaje</h1>
            <p className="mast-sub">Inventario de utensilios y equipamiento</p>
          </div>
        </div>
        <div className="search-bar">
          <span className="search-ico" aria-hidden>
            ⌕
          </span>
          <input
            className="search-input"
            placeholder="Buscar utensilio en todo el menaje…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          {query && (
            <button className="icon-btn" onClick={() => setQuery("")} aria-label="Limpiar">
              ×
            </button>
          )}
        </div>
      </header>

      {q && (
        <section className="search-card">
          {results.length === 0 ? (
            <p className="muted-i">Sin coincidencias para "{query.trim()}".</p>
          ) : (
            <ul className="search-results">
              {results.map((r) => (
                <li key={r.pageId + r.id}>
                  <button className="sresult-info" onClick={() => selectPage(r.pageId)}>
                    <span className="sresult-name">{r.name}</span>
                    <span className="sresult-page">{r.pageName}</span>
                  </button>
                  <span className="qty-num">
                    {fmt(r.amount, r.unit)} {r.unit}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <div className="cols">
        <section className="shelves">
          <h2 className="col-title">Cajones</h2>
          <div className="shelf-grid">
            {state.mPages.map((p) => (
              <div key={p.id} className={"shelf" + (p.id === active?.id ? " on" : "")}>
                <button className="shelf-main" onClick={() => selectPage(p.id)}>
                  <span className="drawer-pull" aria-hidden />
                  <span className="shelf-ico" aria-hidden>
                    {iconGlyph(p.iconId)}
                  </span>
                  <span className="shelf-name">{p.name}</span>
                  <span className="shelf-count">{p.ingredients.length} ítems</span>
                </button>
                {editable && (
                  <div className="shelf-actions">
                    <button onClick={() => setIconPicker(p.id)} title="Cambiar ícono">
                      ◆
                    </button>
                    <button
                      onClick={() => {
                        setRenaming(p.id);
                        setRenameText(p.name);
                      }}
                      title="Renombrar"
                    >
                      ✎
                    </button>
                    <button onClick={() => deletePage(p.id)} title="Eliminar cajón">
                      🗑
                    </button>
                  </div>
                )}
              </div>
            ))}
            {editable && (
              <div className="shelf shelf-new">
                <input
                  value={newPage}
                  placeholder="Nombre del cajón…"
                  onChange={(e) => setNewPage(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && addPage()}
                />
                <button className="check" onClick={addPage} disabled={!newPage.trim()} aria-label="Agregar cajón">
                  ✓
                </button>
              </div>
            )}
          </div>
        </section>

        <section className="sheet">
          {!active ? (
            <div className="empty-card">
              <p className="empty-title">No hay cajones</p>
              <p className="empty-body">Creá tu primer cajón para empezar a cargar utensilios.</p>
            </div>
          ) : (
            <>
              <div className="page-head">
                <span className="page-ico" aria-hidden>
                  {iconGlyph(active.iconId)}
                </span>
                <h2 className="page-title">{active.name}</h2>
                <span className="page-count">{active.ingredients.length}</span>
              </div>

              {editable && (
                <div className="add-row">
                  <input
                    className="add-name"
                    placeholder="Utensilio"
                    value={draft.name}
                    onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                    onKeyDown={(e) => e.key === "Enter" && addItem()}
                  />
                  <input
                    className="add-amount"
                    placeholder="0"
                    inputMode="decimal"
                    value={draft.amount}
                    onChange={(e) => setDraft({ ...draft, amount: e.target.value })}
                    onKeyDown={(e) => e.key === "Enter" && addItem()}
                  />
                  <select
                    value={draft.unit}
                    onChange={(e) => {
                      const unit = e.target.value as Unit;
                      setDraft({ ...draft, unit, type: unit === "u" ? "unidad" : "peso" });
                    }}
                  >
                    {UNITS.map((u) => (
                      <option key={u} value={u}>
                        {u}
                      </option>
                    ))}
                  </select>
                  <button className="primary" onClick={addItem} disabled={!draft.name.trim()}>
                    Agregar
                  </button>
                </div>
              )}

              <ul className="list">
                {active.ingredients.length === 0 && <li className="row-empty">Este cajón está vacío.</li>}
                {active.ingredients.map((item) => (
                  <li key={item.id} className="row">
                    <span className={"badge " + (item.type === "peso" ? "badge-peso" : "badge-unidad")}>
                      {item.type === "peso" ? "peso" : "unidad"}
                    </span>
                    <span className="row-name">{item.name}</span>
                    <span className="leader" aria-hidden />
                    {editable ? (
                      <span className="stepper">
                        <button onClick={() => adjust(active.id, item, -1)} aria-label="Restar">
                          −
                        </button>
                        <input
                          value={fmt(item.amount, item.unit)}
                          inputMode="decimal"
                          onChange={(e) => {
                            const v = parseFloat(e.target.value.replace(",", "."));
                            setAmount(active.id, item.id, isNaN(v) ? 0 : v);
                          }}
                        />
                        <button onClick={() => adjust(active.id, item, 1)} aria-label="Sumar">
                          +
                        </button>
                        <span className="qty-unit">{item.unit}</span>
                      </span>
                    ) : (
                      <span className="qty-num">
                        {fmt(item.amount, item.unit)} <span className="qty-unit">{item.unit}</span>
                      </span>
                    )}
                    {editable && (
                      <button className="icon-btn" title="Quitar" onClick={() => removeItem(active.id, item.id)}>
                        ×
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      </div>

      {renaming && (
        <Modal title="Renombrar cajón" onClose={() => setRenaming(null)}>
          <input
            className="modal-input"
            autoFocus
            value={renameText}
            onChange={(e) => setRenameText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                renamePage(renaming, renameText);
                setRenaming(null);
              }
            }}
          />
          <button
            className="primary modal-primary"
            onClick={() => {
              renamePage(renaming, renameText);
              setRenaming(null);
            }}
          >
            Guardar
          </button>
        </Modal>
      )}

      {iconPicker && (
        <Modal title="Ícono del cajón" onClose={() => setIconPicker(null)}>
          <div className="icon-grid">
            {PAGE_ICONS.map((ic) => (
              <button
                key={ic.id}
                title={ic.label}
                onClick={() => {
                  setPageIcon(iconPicker, ic.id);
                  setIconPicker(null);
                }}
              >
                {ic.glyph}
              </button>
            ))}
          </div>
        </Modal>
      )}
    </div>
  );
}

const css = `
.menaje-root{
  --paper:#EEF1F3;--card:#FAFBFC;--ink:#23282E;--inkSoft:#667079;
  --inkFaint:#9CA6AD;--steel:#3E5C6E;--steelSoft:#DCE6EA;
  --copper:#B5651D;--copperSoft:#F2E1D2;--line:#D7DDE1;--rule:#C7D0D5;
  min-height:100%;background:var(--paper);color:var(--ink);
  font-family:'Inter',ui-sans-serif,system-ui,sans-serif;padding:26px 32px 56px;
}
.menaje-root *{box-sizing:border-box}
.menaje-root .masthead{display:flex;align-items:center;justify-content:space-between;gap:24px;
  padding-bottom:16px;border-bottom:2px solid var(--ink)}
.menaje-root .mast-left{display:flex;align-items:center;gap:14px}
.menaje-root .mast-mark{font-size:28px;color:var(--steel);line-height:1}
.menaje-root .mast-title{font-family:'JetBrains Mono',ui-monospace,monospace;font-weight:600;font-size:28px;line-height:1;margin:0;
  text-transform:uppercase;letter-spacing:.03em}
.menaje-root .mast-sub{margin:5px 0 0;color:var(--inkSoft);font-size:13px}
.menaje-root .search-bar{display:flex;align-items:center;gap:9px;background:var(--card);border:1px solid var(--line);
  border-radius:11px;padding:9px 12px;width:360px}
.menaje-root .search-ico{color:var(--inkFaint);font-size:16px}
.menaje-root .search-input{flex:1;border:none;background:transparent;outline:none;font-family:inherit;font-size:14px;color:var(--ink)}

.menaje-root .search-card{margin-top:16px;background:var(--card);border:1px solid var(--line);border-radius:14px;padding:12px 16px}
.menaje-root .search-results{list-style:none;margin:0;padding:0;max-height:280px;overflow-y:auto}
.menaje-root .search-results li{display:flex;align-items:center;gap:12px;padding:8px 2px}
.menaje-root .search-results li + li{border-top:1px solid var(--line)}
.menaje-root .sresult-info{flex:1;display:flex;flex-direction:column;align-items:flex-start;gap:2px;background:none;border:none;
  cursor:pointer;font-family:inherit;text-align:left;padding:2px}
.menaje-root .sresult-name{font-size:14.5px;font-weight:500;color:var(--ink)}
.menaje-root .sresult-page{font-size:10.5px;text-transform:uppercase;letter-spacing:.07em;color:var(--inkFaint)}

.menaje-root .cols{display:grid;grid-template-columns:minmax(320px,42%) 1fr;gap:24px;margin-top:22px;align-items:start}
.menaje-root .col-title{font-size:11px;letter-spacing:.14em;text-transform:uppercase;color:var(--inkFaint);margin:0 0 10px}
.menaje-root .shelf-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:12px}
.menaje-root .shelf{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:12px;
  display:flex;flex-direction:column;gap:8px;box-shadow:0 10px 24px -22px rgba(35,40,46,.5)}
.menaje-root .shelf.on{border-color:var(--steel);box-shadow:inset 0 0 0 1px var(--steel)}
.menaje-root .shelf-main{background:none;border:none;cursor:pointer;font-family:inherit;text-align:left;padding:0;
  display:flex;flex-direction:column;gap:5px;color:var(--ink)}
.menaje-root .drawer-pull{display:block;width:28px;height:4px;border-radius:2px;background:var(--copper);margin:0 auto 8px}
.menaje-root .shelf-ico{font-size:22px}
.menaje-root .shelf-name{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:14.5px;font-weight:600}
.menaje-root .shelf-count{font-size:11.5px;color:var(--inkFaint)}
.menaje-root .shelf-actions{display:flex;gap:4px;border-top:1px solid var(--rule);padding-top:7px}
.menaje-root .shelf-actions button{flex:1;background:none;border:none;color:var(--inkSoft);font-size:13px;cursor:pointer;
  border-radius:6px;padding:3px}
.menaje-root .shelf-actions button:hover{background:var(--paper);color:var(--ink)}
.menaje-root .shelf-new{border-style:dashed;border-color:var(--steel);flex-direction:row;align-items:center;gap:6px}
.menaje-root .shelf-new input{flex:1;min-width:0;border:none;background:transparent;outline:none;font-family:inherit;font-size:13.5px;color:var(--ink)}
.menaje-root .check{border:none;background:var(--copper);color:#FFF;border-radius:8px;width:28px;height:28px;cursor:pointer;font-size:14px}
.menaje-root .check:disabled{opacity:.4;cursor:default}

.menaje-root .sheet{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:6px 20px 18px;
  box-shadow:0 14px 30px -26px rgba(35,40,46,.4)}
.menaje-root .page-head{display:flex;align-items:center;gap:11px;padding:15px 2px 12px;border-bottom:1px solid var(--line)}
.menaje-root .page-ico{font-size:20px}
.menaje-root .page-title{font-family:'JetBrains Mono',ui-monospace,monospace;font-weight:600;font-size:19px;margin:0;flex:1;
  text-transform:uppercase;letter-spacing:.03em}
.menaje-root .page-count{font-size:12px;font-weight:600;color:var(--steel);background:var(--steelSoft);border-radius:20px;padding:3px 10px}

.menaje-root .add-row{display:flex;gap:8px;padding:12px 0 4px;border-bottom:1px solid var(--line)}
.menaje-root .add-row input,.menaje-root .add-row select{border:1px solid var(--line);border-radius:9px;padding:8px 10px;font-family:inherit;
  font-size:13.5px;background:var(--paper);color:var(--ink);outline:none}
.menaje-root .add-name{flex:1}
.menaje-root .add-amount{width:80px}
.menaje-root .primary{background:var(--ink);color:var(--paper);border:none;border-radius:9px;padding:8px 14px;font-family:inherit;
  font-size:13px;font-weight:600;cursor:pointer}
.menaje-root .primary:disabled{opacity:.4;cursor:default}

.menaje-root .list{list-style:none;margin:0;padding:6px 0 0}
.menaje-root .row{display:flex;align-items:center;gap:10px;padding:11px 2px}
.menaje-root .row + .row{border-top:1px solid var(--line)}
.menaje-root .badge{font-size:9.5px;text-transform:uppercase;letter-spacing:.07em;font-weight:600;padding:3px 7px;border-radius:5px;flex:none}
.menaje-root .badge-peso{background:var(--steelSoft);color:var(--steel)}
.menaje-root .badge-unidad{background:var(--copperSoft);color:var(--copper)}
.menaje-root .row-name{font-size:15px;font-weight:500;overflow-wrap:anywhere}
.menaje-root .leader{flex:1 1 auto;min-width:14px;border-bottom:1px solid var(--line);transform:translateY(-2px)}
.menaje-root .stepper{display:inline-flex;align-items:center;gap:5px}
.menaje-root .stepper button{width:24px;height:24px;border:1px solid var(--line);background:var(--paper);border-radius:7px;
  cursor:pointer;color:var(--inkSoft);font-size:13px;line-height:1}
.menaje-root .stepper input{width:64px;text-align:right;border:1px solid var(--line);border-radius:7px;padding:4px 6px;
  font-family:'JetBrains Mono',ui-monospace,monospace;font-size:13.5px;font-weight:600;background:var(--paper);color:var(--copper);outline:none}
.menaje-root .qty-num{font-family:'JetBrains Mono',ui-monospace,monospace;font-weight:600;font-size:14.5px;color:var(--copper)}
.menaje-root .qty-unit{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:11.5px;color:var(--inkFaint);min-width:18px}
.menaje-root .icon-btn{border:none;background:none;cursor:pointer;font-size:14px;color:var(--inkSoft);
  border-radius:7px;padding:3px 6px}
.menaje-root .icon-btn:hover{background:var(--paper);color:var(--ink)}
.menaje-root .row-empty{padding:14px 2px;color:var(--inkFaint);font-size:13.5px;font-style:italic}
.menaje-root .muted-i{color:var(--inkFaint);font-size:13px;font-style:italic;margin:6px 0}

.menaje-root .empty-card{text-align:center;color:var(--inkSoft);padding:44px 24px}
.menaje-root .empty-title{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:19px;font-weight:600;margin:0 0 6px;color:var(--ink)}
.menaje-root .empty-body{margin:0;font-size:13.5px}

.menaje-root .modal-input{width:100%;border:1px solid var(--line);border-radius:9px;padding:9px 11px;font-family:inherit;
  font-size:14px;background:var(--paper);color:var(--ink);outline:none}
.menaje-root .modal-primary{margin-top:14px;width:100%}
.menaje-root .icon-grid{display:grid;grid-template-columns:repeat(6,1fr);gap:8px}
.menaje-root .icon-grid button{font-size:22px;background:var(--paper);border:1px solid var(--line);border-radius:10px;padding:10px 0;cursor:pointer}
`;
