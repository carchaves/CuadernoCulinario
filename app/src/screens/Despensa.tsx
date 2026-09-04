import { useMemo, useState } from "react";
import type { AppState, Ingredient, IngredientType, ShoppingList, Unit } from "../core/types";
import type { AppStore } from "../core/useAppState";
import { fmt, roundFor, stepFor, uid } from "../core/logic";
import { Modal } from "./ui";
import { PAGE_ICONS, STORE_COLORS, iconGlyph } from "../core/icons";

const UNITS: Unit[] = ["g", "kg", "ml", "L", "u"];

interface SendTarget {
  ingredient: Ingredient;
}

export function Despensa({ store, state }: { store: AppStore; state: AppState }) {
  const editable = !store.readOnly;
  const { mutate } = store;

  const [query, setQuery] = useState("");
  const [activeId, setActiveId] = useState<string | null>(state.pActiveId ?? state.pPages[0]?.id ?? null);
  const [newPage, setNewPage] = useState("");
  const [renaming, setRenaming] = useState<string | null>(null);
  const [renameText, setRenameText] = useState("");
  const [iconPicker, setIconPicker] = useState<string | null>(null);
  const [send, setSend] = useState<SendTarget | null>(null);
  const [draft, setDraft] = useState<{ name: string; type: IngredientType; amount: string; unit: Unit }>({
    name: "",
    type: "peso",
    amount: "",
    unit: "kg",
  });

  const active = state.pPages.find((p) => p.id === activeId) ?? state.pPages[0] ?? null;

  const q = query.trim().toLowerCase();
  const results = useMemo(
    () =>
      q
        ? state.pPages.flatMap((p) =>
            p.ingredients
              .filter((i) => i.name.toLowerCase().includes(q))
              .map((i) => ({ ...i, pageId: p.id, pageName: p.name }))
          )
        : [],
    [q, state.pPages]
  );

  // ---- Acciones sobre el estado -------------------------------------------

  const addPage = () => {
    const name = newPage.trim();
    if (!name) return;
    const page = { id: uid(), name, iconId: PAGE_ICONS[0].id, ingredients: [] };
    mutate((s) => ({ ...s, pPages: [...s.pPages, page], pActiveId: page.id }));
    setActiveId(page.id);
    setNewPage("");
  };

  const renamePage = (pageId: string, name: string) => {
    if (!name.trim()) return;
    mutate((s) => ({ ...s, pPages: s.pPages.map((p) => (p.id === pageId ? { ...p, name: name.trim() } : p)) }));
  };

  const setPageIcon = (pageId: string, iconId: string) =>
    mutate((s) => ({ ...s, pPages: s.pPages.map((p) => (p.id === pageId ? { ...p, iconId } : p)) }));

  const deletePage = (pageId: string) => {
    if (!confirm("¿Borrar esta estantería y todos sus ingredientes?")) return;
    mutate((s) => {
      const pPages = s.pPages.filter((p) => p.id !== pageId);
      return { ...s, pPages, pActiveId: s.pActiveId === pageId ? pPages[0]?.id ?? null : s.pActiveId };
    });
    setActiveId((cur) => (cur === pageId ? state.pPages.find((p) => p.id !== pageId)?.id ?? null : cur));
  };

  const selectPage = (pageId: string) => {
    setActiveId(pageId);
    mutate((s) => (s.pActiveId === pageId ? s : { ...s, pActiveId: pageId }));
  };

  const addIngredient = () => {
    if (!active) return;
    const name = draft.name.trim();
    if (!name) return;
    const ing: Ingredient = {
      id: uid(),
      name,
      type: draft.type,
      amount: parseFloat(draft.amount.replace(",", ".")) || 0,
      unit: draft.unit,
    };
    mutate((s) => ({
      ...s,
      pPages: s.pPages.map((p) => (p.id === active.id ? { ...p, ingredients: [...p.ingredients, ing] } : p)),
    }));
    setDraft({ name: "", type: draft.type, amount: "", unit: draft.unit });
  };

  const setAmount = (pageId: string, ingId: string, amount: number) =>
    mutate((s) => ({
      ...s,
      pPages: s.pPages.map((p) =>
        p.id !== pageId
          ? p
          : {
              ...p,
              ingredients: p.ingredients.map((i) =>
                i.id !== ingId ? i : { ...i, amount: roundFor(amount, i.unit) }
              ),
            }
      ),
    }));

  const adjust = (pageId: string, ing: Ingredient, dir: 1 | -1) =>
    setAmount(pageId, ing.id, ing.amount + dir * stepFor(ing.unit));

  const removeIngredient = (pageId: string, ingId: string) =>
    mutate((s) => ({
      ...s,
      pPages: s.pPages.map((p) => (p.id !== pageId ? p : { ...p, ingredients: p.ingredients.filter((i) => i.id !== ingId) })),
    }));

  return (
    <div className="pantry-root">
      <style>{css}</style>

      <header className="masthead">
        <div className="mast-left">
          <div className="mast-mark" aria-hidden>
            ◧
          </div>
          <div>
            <h1 className="mast-title">Despensa</h1>
            <p className="mast-sub">Inventario por peso y por unidad</p>
          </div>
        </div>
        <div className="search-bar">
          <span className="search-ico" aria-hidden>
            ⌕
          </span>
          <input
            className="search-input"
            placeholder="Buscar ingrediente en toda la despensa…"
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
                  {editable && (
                    <button className="cart-btn" title="Enviar a la lista de compra" onClick={() => setSend({ ingredient: r })}>
                      🛒
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <div className="cols">
        <section className="shelves">
          <h2 className="col-title">Repisas</h2>
          <div className="shelf-grid">
            {state.pPages.map((p) => (
              <div key={p.id} className={"shelf" + (p.id === active?.id ? " on" : "")}>
                <button className="shelf-main" onClick={() => selectPage(p.id)}>
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
                    <button onClick={() => deletePage(p.id)} title="Borrar">
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
                  placeholder="Nueva estantería…"
                  onChange={(e) => setNewPage(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && addPage()}
                />
                <button className="check" onClick={addPage} disabled={!newPage.trim()} aria-label="Agregar página">
                  ✓
                </button>
              </div>
            )}
          </div>
        </section>

        <section className="sheet">
          {!active ? (
            <div className="empty-card">
              <p className="empty-title">No hay estanterías</p>
              <p className="empty-body">Creá una para empezar a cargar ingredientes.</p>
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
                    placeholder="Ingrediente"
                    value={draft.name}
                    onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                    onKeyDown={(e) => e.key === "Enter" && addIngredient()}
                  />
                  <input
                    className="add-amount"
                    placeholder="0"
                    inputMode="decimal"
                    value={draft.amount}
                    onChange={(e) => setDraft({ ...draft, amount: e.target.value })}
                    onKeyDown={(e) => e.key === "Enter" && addIngredient()}
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
                  <button className="primary" onClick={addIngredient} disabled={!draft.name.trim()}>
                    Agregar
                  </button>
                </div>
              )}

              <ul className="list">
                {active.ingredients.length === 0 && <li className="row-empty">Esta estantería está vacía.</li>}
                {active.ingredients.map((ing) => (
                  <li key={ing.id} className="row">
                    <span className={"badge " + (ing.type === "peso" ? "badge-peso" : "badge-unidad")}>
                      {ing.type === "peso" ? "peso" : "unidad"}
                    </span>
                    <span className="row-name">{ing.name}</span>
                    <span className="leader" aria-hidden />
                    {editable ? (
                      <span className="stepper">
                        <button onClick={() => adjust(active.id, ing, -1)} aria-label="Restar">
                          −
                        </button>
                        <input
                          value={fmt(ing.amount, ing.unit)}
                          inputMode="decimal"
                          onChange={(e) => {
                            const v = parseFloat(e.target.value.replace(",", "."));
                            setAmount(active.id, ing.id, isNaN(v) ? 0 : v);
                          }}
                        />
                        <button onClick={() => adjust(active.id, ing, 1)} aria-label="Sumar">
                          +
                        </button>
                        <span className="qty-unit">{ing.unit}</span>
                      </span>
                    ) : (
                      <span className="qty-num">
                        {fmt(ing.amount, ing.unit)} <span className="qty-unit">{ing.unit}</span>
                      </span>
                    )}
                    {editable && (
                      <>
                        <button className="cart-btn" title="Enviar a la lista de compra" onClick={() => setSend({ ingredient: ing })}>
                          🛒
                        </button>
                        <button className="icon-btn" title="Quitar" onClick={() => removeIngredient(active.id, ing.id)}>
                          ×
                        </button>
                      </>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      </div>

      {renaming && (
        <Modal title="Renombrar estantería" onClose={() => setRenaming(null)}>
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
        <Modal title="Ícono de la estantería" onClose={() => setIconPicker(null)}>
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

      {send && (
        <SendToListModal store={store} state={state} ingredient={send.ingredient} onClose={() => setSend(null)} />
      )}
    </div>
  );
}

// ---- "Enviar a la lista" ---------------------------------------------------

function SendToListModal({
  store,
  state,
  ingredient,
  onClose,
}: {
  store: AppStore;
  state: AppState;
  ingredient: Ingredient;
  onClose: () => void;
}) {
  const openLists = state.lists.filter((l) => !l.finalizedAt);
  const [quantity, setQuantity] = useState("1");
  const [newStore, setNewStore] = useState("");

  const qty = parseFloat(quantity.replace(",", ".")) || null;

  const addTo = (listId: string) => {
    store.mutate((s) => ({
      ...s,
      lists: s.lists.map((l) =>
        l.id !== listId
          ? l
          : l.items.some((it) => it.ingredientId === ingredient.id)
            ? { ...l, items: l.items.map((it) => (it.ingredientId === ingredient.id ? { ...it, quantity: qty } : it)) }
            : {
                ...l,
                items: [
                  ...l.items,
                  { ingredientId: ingredient.id, quantity: qty, unit: ingredient.unit, bought: false, price: null },
                ],
              }
      ),
    }));
    onClose();
  };

  const createListAndAdd = (storeId: string) => {
    const list: ShoppingList = {
      id: uid(),
      storeId,
      createdAt: new Date().toISOString(),
      finalizedAt: null,
      items: [{ ingredientId: ingredient.id, quantity: qty, unit: ingredient.unit, bought: false, price: null }],
    };
    store.mutate((s) => ({ ...s, lists: [...s.lists, list] }));
    onClose();
  };

  const createStoreAndList = () => {
    const name = newStore.trim();
    if (!name) return;
    const newId = uid();
    const list: ShoppingList = {
      id: uid(),
      storeId: newId,
      createdAt: new Date().toISOString(),
      finalizedAt: null,
      items: [{ ingredientId: ingredient.id, quantity: qty, unit: ingredient.unit, bought: false, price: null }],
    };
    store.mutate((s) => ({
      ...s,
      stores: [...s.stores, { id: newId, name, color: STORE_COLORS[s.stores.length % STORE_COLORS.length], address: null }],
      lists: [...s.lists, list],
    }));
    onClose();
  };

  return (
    <Modal title={`Enviar «${ingredient.name}» a la lista`} onClose={onClose}>
      <label className="modal-label">Cantidad</label>
      <input className="modal-input" value={quantity} inputMode="decimal" onChange={(e) => setQuantity(e.target.value)} />

      <p className="modal-label">Listas abiertas</p>
      {openLists.length === 0 && <p className="muted-i">No hay listas abiertas.</p>}
      <ul className="pick-list">
        {openLists.map((l) => {
          const st = state.stores.find((s) => s.id === l.storeId);
          return (
            <li key={l.id}>
              <button onClick={() => addTo(l.id)}>
                <span className="dot" style={{ background: st?.color ?? "#8C8377" }} />
                {st?.name ?? "Sin comercio"} · {l.items.length} ítems
              </button>
            </li>
          );
        })}
      </ul>

      <p className="modal-label">Nueva lista en un comercio</p>
      <ul className="pick-list">
        {state.stores.map((s) => (
          <li key={s.id}>
            <button onClick={() => createListAndAdd(s.id)}>
              <span className="dot" style={{ background: s.color }} />
              {s.name}
            </button>
          </li>
        ))}
      </ul>

      <div className="inline-form">
        <input
          className="modal-input"
          placeholder="Comercio nuevo…"
          value={newStore}
          onChange={(e) => setNewStore(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && createStoreAndList()}
        />
        <button className="primary" onClick={createStoreAndList} disabled={!newStore.trim()}>
          Crear
        </button>
      </div>
    </Modal>
  );
}

const css = `
.pantry-root{
  --paper:#FBF9F3;--card:#FEFDFA;--ink:#24211A;--inkSoft:#726C5C;
  --inkFaint:#A29B89;--olive:#57682B;--oliveSoft:#E9EDD8;
  --amber:#8C5E12;--amberSoft:#F4E9CE;--line:#E5E0D2;--rule:#CDC7B5;
  min-height:100%;background:var(--paper);color:var(--ink);
  font-family:'Inter',ui-sans-serif,system-ui,sans-serif;padding:26px 32px 56px;
}
.pantry-root *{box-sizing:border-box}
.masthead{display:flex;align-items:center;justify-content:space-between;gap:24px;
  padding-bottom:16px;border-bottom:2px solid var(--ink)}
.mast-left{display:flex;align-items:center;gap:14px}
.mast-mark{font-size:28px;color:var(--olive);line-height:1}
.mast-title{font-family:'Fraunces','Fraunces',Georgia,serif;font-weight:600;font-size:31px;line-height:1;margin:0}
.mast-sub{margin:5px 0 0;color:var(--inkSoft);font-size:13px}
.search-bar{display:flex;align-items:center;gap:9px;background:var(--card);border:1px solid var(--line);
  border-radius:11px;padding:9px 12px;width:360px}
.search-ico{color:var(--inkFaint);font-size:16px}
.search-input{flex:1;border:none;background:transparent;outline:none;font-family:inherit;font-size:14px;color:var(--ink)}

.search-card{margin-top:16px;background:var(--card);border:1px solid var(--line);border-radius:14px;padding:12px 16px}
.search-results{list-style:none;margin:0;padding:0;max-height:280px;overflow-y:auto}
.search-results li{display:flex;align-items:center;gap:12px;padding:8px 2px}
.search-results li + li{border-top:1px dotted var(--line)}
.sresult-info{flex:1;display:flex;flex-direction:column;align-items:flex-start;gap:2px;background:none;border:none;
  cursor:pointer;font-family:inherit;text-align:left;padding:2px}
.sresult-name{font-size:14.5px;font-weight:500;color:var(--ink)}
.sresult-page{font-size:10.5px;text-transform:uppercase;letter-spacing:.07em;color:var(--inkFaint)}

.cols{display:grid;grid-template-columns:minmax(320px,42%) 1fr;gap:24px;margin-top:22px;align-items:start}
.col-title{font-size:11px;letter-spacing:.14em;text-transform:uppercase;color:var(--inkFaint);margin:0 0 10px}
.shelf-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:12px}
.shelf{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:12px;
  display:flex;flex-direction:column;gap:8px;box-shadow:0 10px 24px -22px rgba(36,33,26,.5)}
.shelf.on{border-color:var(--olive);box-shadow:inset 0 0 0 1px var(--olive)}
.shelf-main{background:none;border:none;cursor:pointer;font-family:inherit;text-align:left;padding:0;
  display:flex;flex-direction:column;gap:5px;color:var(--ink)}
.shelf-ico{font-size:22px}
.shelf-name{font-family:'Fraunces','Fraunces',Georgia,serif;font-size:16px;font-weight:600}
.shelf-count{font-size:11.5px;color:var(--inkFaint)}
.shelf-actions{display:flex;gap:4px;border-top:1px dashed var(--rule);padding-top:7px}
.shelf-actions button{flex:1;background:none;border:none;color:var(--inkSoft);font-size:13px;cursor:pointer;
  border-radius:6px;padding:3px}
.shelf-actions button:hover{background:var(--paper);color:var(--ink)}
.shelf-new{border-style:dashed;flex-direction:row;align-items:center;gap:6px}
.shelf-new input{flex:1;min-width:0;border:none;background:transparent;outline:none;font-family:inherit;font-size:13.5px;color:var(--ink)}
.check{border:none;background:#E9EDD8;color:var(--olive);border-radius:8px;width:28px;height:28px;cursor:pointer;font-size:14px}
.check:disabled{opacity:.4;cursor:default}

.sheet{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:6px 20px 18px;
  box-shadow:0 14px 30px -26px rgba(36,33,26,.4)}
.page-head{display:flex;align-items:center;gap:11px;padding:15px 2px 12px;border-bottom:1px solid var(--line)}
.page-ico{font-size:20px}
.page-title{font-family:'Fraunces','Fraunces',Georgia,serif;font-weight:600;font-size:22px;margin:0;flex:1}
.page-count{font-size:12px;font-weight:600;color:var(--olive);background:var(--oliveSoft);border-radius:20px;padding:3px 10px}

.add-row{display:flex;gap:8px;padding:12px 0 4px;border-bottom:1px dotted var(--line)}
.add-row input,.add-row select{border:1px solid var(--line);border-radius:9px;padding:8px 10px;font-family:inherit;
  font-size:13.5px;background:var(--paper);color:var(--ink);outline:none}
.add-name{flex:1}
.add-amount{width:80px}
.primary{background:var(--ink);color:var(--paper);border:none;border-radius:9px;padding:8px 14px;font-family:inherit;
  font-size:13px;font-weight:600;cursor:pointer}
.primary:disabled{opacity:.4;cursor:default}

.list{list-style:none;margin:0;padding:6px 0 0}
.row{display:flex;align-items:center;gap:10px;padding:11px 2px}
.row + .row{border-top:1px dotted var(--line)}
.badge{font-size:9.5px;text-transform:uppercase;letter-spacing:.07em;font-weight:600;padding:3px 7px;border-radius:5px;flex:none}
.badge-peso{background:var(--oliveSoft);color:var(--olive)}
.badge-unidad{background:var(--amberSoft);color:var(--amber)}
.row-name{font-size:15px;font-weight:500;overflow-wrap:anywhere}
.leader{flex:1 1 auto;min-width:14px;border-bottom:2px dotted var(--rule);transform:translateY(-3px)}
.stepper{display:inline-flex;align-items:center;gap:5px}
.stepper button{width:24px;height:24px;border:1px solid var(--line);background:var(--paper);border-radius:7px;
  cursor:pointer;color:var(--inkSoft);font-size:13px;line-height:1}
.stepper input{width:64px;text-align:right;border:1px solid var(--line);border-radius:7px;padding:4px 6px;
  font-family:'JetBrains Mono',ui-monospace,monospace;font-size:13.5px;background:var(--paper);color:var(--ink);outline:none}
.qty-num{font-family:'JetBrains Mono',ui-monospace,monospace;font-weight:600;font-size:14.5px;color:var(--ink)}
.qty-unit{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:11.5px;color:var(--inkFaint);min-width:18px}
.cart-btn,.icon-btn{border:none;background:none;cursor:pointer;font-size:14px;color:var(--inkSoft);
  border-radius:7px;padding:3px 6px}
.cart-btn:hover,.icon-btn:hover{background:var(--paper);color:var(--ink)}
.row-empty{padding:14px 2px;color:var(--inkFaint);font-size:13.5px;font-style:italic}
.muted-i{color:var(--inkFaint);font-size:13px;font-style:italic;margin:6px 0}

.empty-card{text-align:center;color:var(--inkSoft);padding:44px 24px}
.empty-title{font-family:'Fraunces',Georgia,serif;font-size:19px;font-weight:600;margin:0 0 6px;color:var(--ink)}
.empty-body{margin:0;font-size:13.5px}

.modal-input{width:100%;border:1px solid #E5E0D2;border-radius:9px;padding:9px 11px;font-family:inherit;
  font-size:14px;background:#FBF9F3;color:#24211A;outline:none}
.modal-label{font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:#A29B89;margin:16px 0 6px}
.modal-primary{margin-top:14px;width:100%}
.icon-grid{display:grid;grid-template-columns:repeat(6,1fr);gap:8px}
.icon-grid button{font-size:22px;background:#FBF9F3;border:1px solid #E5E0D2;border-radius:10px;padding:10px 0;cursor:pointer}
.pick-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:6px}
.pick-list button{width:100%;display:flex;align-items:center;gap:9px;background:#FBF9F3;border:1px solid #E5E0D2;
  border-radius:10px;padding:9px 12px;font-family:inherit;font-size:13.5px;cursor:pointer;color:#24211A;text-align:left}
.pick-list button:hover{border-color:#57682B}
.dot{width:10px;height:10px;border-radius:50%;flex:none}
.inline-form{display:flex;gap:8px;margin-top:10px}
`;
