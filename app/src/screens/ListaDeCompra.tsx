import { useMemo, useRef, useState } from "react";
import type { AppState, Ingredient, ShoppingList, ShoppingListItem } from "../core/types";
import type { AppStore } from "../core/useAppState";
import { fmt, uid } from "../core/logic";
import { Modal } from "./ui";
import { STORE_COLORS } from "../core/icons";

type Tab = "lista" | "catalogo" | "precios";

const money = (n: number | null | undefined) =>
  n == null ? "—" : "$" + n.toLocaleString("es-AR", { maximumFractionDigits: 2 });

export function ListaDeCompra({ store, state }: { store: AppStore; state: AppState }) {
  const editable = !store.readOnly;
  const { mutate } = store;

  const [selectedId, setSelectedId] = useState<string | null>(state.lists.find((l) => !l.finalizedAt)?.id ?? null);
  const [tab, setTab] = useState<Tab>("lista");
  const [newListOpen, setNewListOpen] = useState(false);
  const [checkoutFor, setCheckoutFor] = useState<string | null>(null);
  const [boycottFor, setBoycottFor] = useState<string | null>(null);

  const pantry = useMemo(() => state.pPages.flatMap((p) => p.ingredients), [state.pPages]);
  const byId = useMemo(() => new Map(pantry.map((i) => [i.id, i])), [pantry]);
  const selected = state.lists.find((l) => l.id === selectedId) ?? null;
  const selectedStore = selected ? state.stores.find((s) => s.id === selected.storeId) ?? null : null;

  const priceOf = (ingredientId: string, storeId: string): number | null =>
    state.priceHistory[ingredientId]?.[storeId] ?? null;

  const subtotal = (list: ShoppingList): number =>
    list.items.reduce((acc, it) => {
      const p = it.price ?? priceOf(it.ingredientId, list.storeId);
      return p == null ? acc : acc + p * (it.quantity ?? 1);
    }, 0);

  // ---- Acciones -----------------------------------------------------------

  const patchList = (listId: string, fn: (l: ShoppingList) => ShoppingList) =>
    mutate((s) => ({ ...s, lists: s.lists.map((l) => (l.id === listId ? fn(l) : l)) }));

  const toggleItem = (listId: string, ingredientId: string, unit: string | null) =>
    patchList(listId, (l) =>
      l.items.some((it) => it.ingredientId === ingredientId)
        ? { ...l, items: l.items.filter((it) => it.ingredientId !== ingredientId) }
        : { ...l, items: [...l.items, { ingredientId, quantity: 1, unit, bought: false, price: null }] }
    );

  const setQuantity = (listId: string, ingredientId: string, raw: string) => {
    const q = raw.trim() === "" ? null : parseFloat(raw.replace(",", ".")) || null;
    patchList(listId, (l) => ({
      ...l,
      items: l.items.map((it) => (it.ingredientId === ingredientId ? { ...it, quantity: q } : it)),
    }));
  };

  const toggleBought = (listId: string, ingredientId: string) =>
    patchList(listId, (l) => ({
      ...l,
      items: l.items.map((it) => (it.ingredientId === ingredientId ? { ...it, bought: !it.bought } : it)),
    }));

  const removeItem = (listId: string, ingredientId: string) =>
    patchList(listId, (l) => ({ ...l, items: l.items.filter((it) => it.ingredientId !== ingredientId) }));

  const deleteList = (listId: string) => {
    if (!confirm("¿Borrar esta lista?")) return;
    mutate((s) => ({ ...s, lists: s.lists.filter((l) => l.id !== listId) }));
    setSelectedId(null);
  };

  const addToStoreList = (storeId: string, ingredientId: string, unit: string | null) =>
    mutate((s) => {
      const open = s.lists.find((l) => l.storeId === storeId && !l.finalizedAt);
      const item: ShoppingListItem = { ingredientId, quantity: 1, unit, bought: false, price: null };
      if (!open) {
        return {
          ...s,
          lists: [
            ...s.lists,
            { id: uid(), storeId, createdAt: new Date().toISOString(), finalizedAt: null, items: [item] },
          ],
        };
      }
      if (open.items.some((it) => it.ingredientId === ingredientId)) return s;
      return { ...s, lists: s.lists.map((l) => (l.id === open.id ? { ...l, items: [...l.items, item] } : l)) };
    });

  // ---- Render -------------------------------------------------------------

  const openLists = state.lists.filter((l) => !l.finalizedAt);
  const closedLists = state.lists.filter((l) => l.finalizedAt);

  const listRow = (l: ShoppingList) => {
    const st = state.stores.find((s) => s.id === l.storeId);
    const total = subtotal(l);
    return (
      <button
        key={l.id}
        className={"lrow" + (l.id === selectedId ? " on" : "")}
        onClick={() => {
          setSelectedId(l.id);
          setTab("lista");
        }}
      >
        <span className="dot" style={{ background: st?.color ?? "#8C8377" }} />
        <span className="lrow-body">
          <span className="lrow-name">{st?.name ?? "Sin comercio"}</span>
          {st?.address && <span className="lrow-addr">{st.address}</span>}
          <span className="lrow-meta">
            {l.items.length} ítems{total > 0 && ` · ~${money(total)}`}
          </span>
        </span>
      </button>
    );
  };

  return (
    <div className="lista-root">
      <style>{css}</style>

      <aside className="index">
        <h1>Lista de compra</h1>
        <p className="sub">Listas por comercio</p>
        {editable && (
          <button className="new-list" onClick={() => setNewListOpen(true)}>
            + Nueva lista
          </button>
        )}

        <p className="col-title">Abiertas</p>
        {openLists.length === 0 && <p className="muted-i">Ninguna lista abierta.</p>}
        {openLists.map(listRow)}

        {closedLists.length > 0 && (
          <>
            <p className="col-title">Finalizadas</p>
            {closedLists.map(listRow)}
          </>
        )}
      </aside>

      <section className="detail">
        {!selected ? (
          <p className="muted-i pad">Elegí o creá una lista.</p>
        ) : (
          <>
            <div className="detail-head">
              <div>
                <h2>
                  <span className="dot big" style={{ background: selectedStore?.color ?? "#8C8377" }} />
                  {selectedStore?.name ?? "Sin comercio"}
                </h2>
                <p className="sub">
                  {selectedStore?.address ? selectedStore.address + " · " : ""}
                  {selected.finalizedAt ? `Finalizada el ${selected.finalizedAt.slice(0, 10)}` : "Abierta"} ·{" "}
                  {selected.items.filter((i) => i.bought).length}/{selected.items.length} comprados · ~
                  {money(subtotal(selected))}
                </p>
              </div>
              {editable && (
                <div className="head-actions">
                  {!selected.finalizedAt && (
                    <button className="primary" onClick={() => setCheckoutFor(selected.id)}>
                      Finalizar compra
                    </button>
                  )}
                  <button className="ghost" onClick={() => deleteList(selected.id)}>
                    🗑 Borrar lista
                  </button>
                </div>
              )}
            </div>

            <div className="tabs">
              {(["lista", "catalogo", "precios"] as Tab[]).map((t) => (
                <button key={t} className={"tab" + (tab === t ? " active" : "")} onClick={() => setTab(t)}>
                  {t === "lista" ? "Lista" : t === "catalogo" ? "Catálogo" : "Comparar precios"}
                </button>
              ))}
            </div>

            {tab === "lista" && (
              <ul className="items">
                {selected.items.length === 0 && <li className="muted-i">Nada en esta lista todavía.</li>}
                {selected.items.map((it) => {
                  const ing = byId.get(it.ingredientId);
                  const brands = state.boycottedBrands[it.ingredientId] ?? [];
                  return (
                    <li key={it.ingredientId} className="item">
                      <button
                        className={"check" + (it.bought ? " on" : "")}
                        disabled={!editable}
                        onClick={() => toggleBought(selected.id, it.ingredientId)}
                        aria-label={it.bought ? "Marcar como pendiente" : "Marcar como comprado"}
                      >
                        {it.bought ? "✓" : ""}
                      </button>
                      <span className="item-body">
                        <span className={"item-name" + (it.bought ? " done" : "")}>
                          {ing?.name ?? it.ingredientId}
                        </span>
                        {brands.length > 0 && (
                          <span className="brands">
                            {brands.map((b) => (
                              <s key={b}>{b}</s>
                            ))}
                          </span>
                        )}
                      </span>
                      <span className="price-hint">
                        {money(it.price ?? priceOf(it.ingredientId, selected.storeId))}
                      </span>
                      {editable ? (
                        <>
                          <input
                            className="qty-input"
                            value={it.quantity ?? ""}
                            inputMode="decimal"
                            onChange={(e) => setQuantity(selected.id, it.ingredientId, e.target.value)}
                          />
                          <span className="unit">{it.unit ?? ing?.unit ?? ""}</span>
                          <button className="icon-btn" onClick={() => setBoycottFor(it.ingredientId)} title="Marcas boicoteadas">
                            🚫
                          </button>
                          <button className="icon-btn" onClick={() => removeItem(selected.id, it.ingredientId)} title="Quitar">
                            ×
                          </button>
                        </>
                      ) : (
                        <span className="unit">
                          {it.quantity != null ? fmt(it.quantity, it.unit ?? "u") : ""} {it.unit ?? ""}
                        </span>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}

            {tab === "catalogo" && (
              <div className="catalog">
                {state.pPages.map((p) => (
                  <section key={p.id} className="cat">
                    <div className="cat-head">{p.name}</div>
                    <ul>
                      {p.ingredients.map((ing) => {
                        const item = selected.items.find((it) => it.ingredientId === ing.id);
                        const brands = state.boycottedBrands[ing.id] ?? [];
                        return (
                          <li key={ing.id} className="item">
                            <button
                              className={"check" + (item ? " on" : "")}
                              disabled={!editable}
                              onClick={() => toggleItem(selected.id, ing.id, ing.unit)}
                              aria-label={item ? "Quitar de la lista" : "Agregar a la lista"}
                            >
                              {item ? "✓" : ""}
                            </button>
                            <span className="item-body">
                              <span className="item-name">{ing.name}</span>
                              {brands.length > 0 && (
                                <span className="brands">
                                  {brands.map((b) => (
                                    <s key={b}>{b}</s>
                                  ))}
                                </span>
                              )}
                            </span>
                            {item && editable && (
                              <input
                                className="qty-input"
                                value={item.quantity ?? ""}
                                inputMode="decimal"
                                onChange={(e) => setQuantity(selected.id, ing.id, e.target.value)}
                              />
                            )}
                            <span className="unit">{ing.unit}</span>
                            {editable && (
                              <button className="icon-btn" onClick={() => setBoycottFor(ing.id)} title="Marcas boicoteadas">
                                🚫
                              </button>
                            )}
                          </li>
                        );
                      })}
                      {p.ingredients.length === 0 && <li className="muted-i">Página vacía.</li>}
                    </ul>
                  </section>
                ))}
              </div>
            )}

            {tab === "precios" && (
              <ComparePanel
                state={state}
                list={selected}
                byId={byId}
                editable={editable}
                onAddTo={(storeId, ingredientId, unit) => addToStoreList(storeId, ingredientId, unit)}
              />
            )}
          </>
        )}
      </section>

      {newListOpen && (
        <NewListModal
          state={state}
          onClose={() => setNewListOpen(false)}
          onCreate={(list, newStore) => {
            mutate((s) => ({
              ...s,
              stores: newStore ? [...s.stores, newStore] : s.stores,
              lists: [...s.lists, list],
            }));
            setSelectedId(list.id);
            setTab("catalogo");
            setNewListOpen(false);
          }}
        />
      )}

      {boycottFor && (
        <BoycottModal
          ingredient={byId.get(boycottFor) ?? null}
          brands={state.boycottedBrands[boycottFor] ?? []}
          onClose={() => setBoycottFor(null)}
          onChange={(brands) =>
            mutate((s) => {
              const next = { ...s.boycottedBrands };
              if (brands.length === 0) delete next[boycottFor];
              else next[boycottFor] = brands;
              return { ...s, boycottedBrands: next };
            })
          }
        />
      )}

      {checkoutFor && selected && checkoutFor === selected.id && (
        <CheckoutModal store={store} state={state} list={selected} byId={byId} onClose={() => setCheckoutFor(null)} />
      )}
    </div>
  );
}

// ---- Comparación de precios entre comercios --------------------------------

function ComparePanel({
  state,
  list,
  byId,
  editable,
  onAddTo,
}: {
  state: AppState;
  list: ShoppingList;
  byId: Map<string, Ingredient>;
  editable: boolean;
  onAddTo: (storeId: string, ingredientId: string, unit: string | null) => void;
}) {
  if (list.items.length === 0) return <p className="muted-i pad">Agregá ítems para comparar precios.</p>;
  return (
    <div className="compare">
      {list.items.map((it) => {
        const prices = state.priceHistory[it.ingredientId] ?? {};
        const entries = Object.entries(prices).sort((a, b) => a[1] - b[1]);
        const cheapest = entries[0]?.[0];
        return (
          <section key={it.ingredientId} className="cmp-row">
            <div className="cmp-name">{byId.get(it.ingredientId)?.name ?? it.ingredientId}</div>
            {entries.length === 0 ? (
              <p className="muted-i">Sin precios registrados todavía.</p>
            ) : (
              <ul className="cmp-stores">
                {entries.map(([storeId, price]) => {
                  const st = state.stores.find((s) => s.id === storeId);
                  return (
                    <li key={storeId} className={storeId === cheapest ? "best" : undefined}>
                      <span className="dot" style={{ background: st?.color ?? "#8C8377" }} />
                      <span className="cmp-store">{st?.name ?? storeId}</span>
                      <span className="cmp-price">{money(price)}</span>
                      {editable && storeId !== list.storeId && (
                        <button
                          className="icon-btn"
                          title={`Agregar a la lista de ${st?.name ?? storeId}`}
                          onClick={() => onAddTo(storeId, it.ingredientId, it.unit)}
                        >
                          +
                        </button>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </section>
        );
      })}
    </div>
  );
}

// ---- Nueva lista -----------------------------------------------------------

function NewListModal({
  state,
  onClose,
  onCreate,
}: {
  state: AppState;
  onClose: () => void;
  onCreate: (list: ShoppingList, newStore: AppState["stores"][number] | null) => void;
}) {
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [color, setColor] = useState(STORE_COLORS[0]);

  const makeList = (storeId: string): ShoppingList => ({
    id: uid(),
    storeId,
    createdAt: new Date().toISOString(),
    finalizedAt: null,
    items: [],
  });

  return (
    <Modal title="Nueva lista" onClose={onClose}>
      <p className="modal-label">Comercio existente</p>
      {state.stores.length === 0 && <p className="muted-i">Todavía no hay comercios.</p>}
      <ul className="pick-list">
        {state.stores.map((s) => (
          <li key={s.id}>
            <button onClick={() => onCreate(makeList(s.id), null)}>
              <span className="dot" style={{ background: s.color }} />
              {s.name}
              {s.address ? ` · ${s.address}` : ""}
            </button>
          </li>
        ))}
      </ul>

      <p className="modal-label">Comercio nuevo</p>
      <input className="modal-input" placeholder="Nombre" value={name} onChange={(e) => setName(e.target.value)} />
      <input
        className="modal-input mt"
        placeholder="Dirección (opcional)"
        value={address}
        onChange={(e) => setAddress(e.target.value)}
      />
      <div className="colors">
        {STORE_COLORS.map((c) => (
          <button
            key={c}
            className={"swatch" + (c === color ? " on" : "")}
            style={{ background: c }}
            onClick={() => setColor(c)}
            aria-label={`Color ${c}`}
          />
        ))}
      </div>
      <button
        className="primary modal-primary"
        disabled={!name.trim()}
        onClick={() => {
          const storeId = uid();
          onCreate(makeList(storeId), { id: storeId, name: name.trim(), color, address: address.trim() || null });
        }}
      >
        Crear comercio y lista
      </button>
    </Modal>
  );
}

// ---- Marcas boicoteadas ----------------------------------------------------

function BoycottModal({
  ingredient,
  brands,
  onClose,
  onChange,
}: {
  ingredient: Ingredient | null;
  brands: string[];
  onClose: () => void;
  onChange: (brands: string[]) => void;
}) {
  const [text, setText] = useState("");
  return (
    <Modal title={`Marcas a evitar · ${ingredient?.name ?? ""}`} onClose={onClose}>
      <ul className="pick-list">
        {brands.length === 0 && <li className="muted-i">Sin marcas cargadas.</li>}
        {brands.map((b) => (
          <li key={b}>
            <button onClick={() => onChange(brands.filter((x) => x !== b))}>
              <s>{b}</s>
              <span className="grow" />×
            </button>
          </li>
        ))}
      </ul>
      <div className="inline-form">
        <input
          className="modal-input"
          placeholder="Marca…"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && text.trim()) {
              onChange([...brands, text.trim()]);
              setText("");
            }
          }}
        />
        <button
          className="primary"
          disabled={!text.trim()}
          onClick={() => {
            onChange([...brands, text.trim()]);
            setText("");
          }}
        >
          Agregar
        </button>
      </div>
    </Modal>
  );
}

// ---- Finalizar compra ------------------------------------------------------

function CheckoutModal({
  store,
  state,
  list,
  byId,
  onClose,
}: {
  store: AppStore;
  state: AppState;
  list: ShoppingList;
  byId: Map<string, Ingredient>;
  onClose: () => void;
}) {
  const [phase, setPhase] = useState<"foto" | "precios">("foto");
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [prices, setPrices] = useState<Record<string, string>>(() =>
    Object.fromEntries(list.items.map((it) => [it.ingredientId, it.price != null ? String(it.price) : ""]))
  );
  const fileRef = useRef<HTMLInputElement>(null);

  const uploadPhoto = async (file: File) => {
    setUploading(true);
    setError(null);
    const receiptId = uid();
    const path = `data/receipts/${receiptId}.jpg`;
    try {
      await store.putBinary(path, file, `Agregar foto de recibo desde la web`);
      store.mutate((s) => ({
        ...s,
        receipts: [...s.receipts, { id: receiptId, listId: list.id, photoPath: path, createdAt: new Date().toISOString() }],
      }));
      setPhase("precios");
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo subir la foto.");
    } finally {
      setUploading(false);
    }
  };

  const finalize = () => {
    const parsed: Record<string, number | null> = {};
    for (const it of list.items) {
      const raw = (prices[it.ingredientId] ?? "").trim();
      const v = raw === "" ? null : parseFloat(raw.replace(",", "."));
      parsed[it.ingredientId] = v == null || isNaN(v) ? null : v;
    }
    store.mutate((s) => {
      const priceHistory = { ...s.priceHistory };
      for (const [ingredientId, price] of Object.entries(parsed)) {
        if (price == null) continue;
        priceHistory[ingredientId] = { ...(priceHistory[ingredientId] ?? {}), [list.storeId]: price };
      }
      return {
        ...s,
        priceHistory,
        lists: s.lists.map((l) =>
          l.id !== list.id
            ? l
            : {
                ...l,
                finalizedAt: new Date().toISOString(),
                items: l.items.map((it) => ({ ...it, price: parsed[it.ingredientId] ?? it.price, bought: true })),
              }
        ),
      };
    });
    onClose();
  };

  const storeName = state.stores.find((s) => s.id === list.storeId)?.name ?? "";

  return (
    <Modal title={`Finalizar compra · ${storeName}`} onClose={onClose} width={560}>
      {phase === "foto" ? (
        <>
          <p className="muted-i">
            Sacá o elegí una foto del recibo: se guarda en el repo como referencia. Los precios se cargan a mano en
            el paso siguiente (no hay lectura automática del recibo).
          </p>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            capture="environment"
            className="modal-input mt"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) void uploadPhoto(f);
            }}
          />
          {uploading && <p className="muted-i">Subiendo la foto…</p>}
          {error && <p className="err">{error}</p>}
          <button className="ghost modal-primary" onClick={() => setPhase("precios")}>
            Seguir sin foto
          </button>
        </>
      ) : (
        <>
          <p className="muted-i">Cargá el precio pagado por cada ítem. Se guarda como último precio conocido en {storeName}.</p>
          <ul className="price-form">
            {list.items.map((it) => (
              <li key={it.ingredientId}>
                <span>{byId.get(it.ingredientId)?.name ?? it.ingredientId}</span>
                <input
                  className="modal-input narrow"
                  inputMode="decimal"
                  placeholder="$"
                  value={prices[it.ingredientId] ?? ""}
                  onChange={(e) => setPrices((p) => ({ ...p, [it.ingredientId]: e.target.value }))}
                />
              </li>
            ))}
          </ul>
          <button className="primary modal-primary" onClick={finalize}>
            Guardar precios y finalizar
          </button>
        </>
      )}
    </Modal>
  );
}

const css = `
.lista-root{display:grid;grid-template-columns:minmax(280px,30%) 1fr;height:100%;
  background:#F4F5EF;color:#22271E;font-family:'Inter',ui-sans-serif,system-ui,sans-serif;line-height:1.42}
.lista-root *{box-sizing:border-box}
.index{border-right:1px solid #E3E5DA;padding:22px 18px;overflow-y:auto;max-height:100%}
.index h1{font-family:'Fraunces','Fraunces',Georgia,serif;font-weight:600;font-size:25px;margin:0}
.sub{color:#5C6353;font-size:12.5px;margin:4px 0 0}
.col-title{font-size:10.5px;letter-spacing:.14em;text-transform:uppercase;color:#8C9180;margin:20px 0 8px}
.new-list{margin-top:14px;width:100%;border:1px dashed #A7B49B;background:#FBFCF8;color:#2F7A55;border-radius:10px;
  padding:9px;font-family:inherit;font-size:13px;font-weight:600;cursor:pointer}
.lrow{width:100%;display:flex;align-items:flex-start;gap:10px;background:#FBFCF8;border:1px solid #E3E5DA;
  border-radius:12px;padding:11px 13px;margin-bottom:8px;cursor:pointer;font-family:inherit;text-align:left}
.lrow.on{border-color:#2F9E6B;box-shadow:inset 0 0 0 1px #2F9E6B}
.lrow-body{display:flex;flex-direction:column;gap:2px;min-width:0}
.lrow-name{font-family:'Fraunces',Georgia,serif;font-size:15.5px;font-weight:600}
.lrow-addr{font-size:11.5px;color:#8C9180}
.lrow-meta{font-size:11.5px;color:#5C6353}
.dot{width:11px;height:11px;border-radius:50%;flex:none;margin-top:4px}
.dot.big{width:13px;height:13px;display:inline-block;margin:0 8px 0 0}

.detail{padding:22px 28px 56px;overflow-y:auto;max-height:100%}
.detail-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}
.detail-head h2{font-family:'Fraunces','Fraunces',Georgia,serif;font-weight:600;font-size:23px;margin:0;display:flex;align-items:center}
.head-actions{display:flex;gap:8px;flex:none}
.primary{background:#2F9E6B;color:#fff;border:none;border-radius:10px;padding:9px 15px;font-family:inherit;
  font-size:13px;font-weight:600;cursor:pointer}
.primary:disabled{opacity:.45;cursor:default}
.ghost{background:#FBFCF8;border:1px solid #E3E5DA;color:#5C6353;border-radius:10px;padding:9px 15px;
  font-family:inherit;font-size:13px;font-weight:600;cursor:pointer}

.tabs{display:flex;gap:4px;margin:18px 0 14px;background:#E7E9DE;padding:4px;border-radius:12px;max-width:520px}
.tab{flex:1;border:none;cursor:pointer;font-family:inherit;font-weight:600;font-size:13px;padding:8px;
  border-radius:9px;background:transparent;color:#5C6353}
.tab.active{background:#FBFCF8;color:#22271E;box-shadow:0 1px 3px rgba(34,39,30,.12)}

.items{list-style:none;margin:0;padding:0;background:#FBFCF8;border:1px solid #E3E5DA;border-radius:14px;
  padding:6px 12px}
.item{display:flex;align-items:center;gap:10px;padding:9px 2px}
.item + .item{border-top:1px dotted #E3E5DA}
.check{width:22px;height:22px;flex:none;border-radius:50%;border:1.5px solid #C3C8B8;background:#fff;
  cursor:pointer;color:#fff;font-size:12px;font-weight:700;padding:0}
.check.on{background:#2F9E6B;border-color:#2F9E6B}
.check:disabled{cursor:default}
.item-body{flex:1;min-width:0;display:flex;flex-direction:column;gap:2px}
.item-name{font-size:14.5px;font-weight:500}
.item-name.done{color:#8C9180;text-decoration:line-through}
.brands{font-size:11.5px;color:#A6432E;display:flex;gap:8px;flex-wrap:wrap}
.price-hint{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:12.5px;color:#5C6353;min-width:60px;text-align:right}
.qty-input{width:62px;text-align:right;border:1px solid #E3E5DA;border-radius:8px;padding:5px 7px;
  font-family:'JetBrains Mono',ui-monospace,monospace;font-size:13px;background:#F4F5EF;color:#22271E;outline:none}
.unit{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:11.5px;color:#8C9180;min-width:22px}
.icon-btn{border:none;background:none;cursor:pointer;font-size:14px;color:#5C6353;border-radius:7px;padding:3px 5px}
.icon-btn:hover{background:#EFF0E9}

.catalog{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:14px;align-items:start}
.cat{background:#FBFCF8;border:1px solid #E3E5DA;border-radius:14px;overflow:hidden}
.cat-head{padding:11px 14px;border-bottom:1px solid #E3E5DA;font-family:'Fraunces',Georgia,serif;font-weight:600;font-size:15px}
.cat ul{list-style:none;margin:0;padding:4px 12px 10px}

.compare{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:14px;align-items:start}
.cmp-row{background:#FBFCF8;border:1px solid #E3E5DA;border-radius:14px;padding:12px 14px}
.cmp-name{font-family:'Fraunces',Georgia,serif;font-weight:600;font-size:15px;margin-bottom:8px}
.cmp-stores{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:6px}
.cmp-stores li{display:flex;align-items:center;gap:8px;font-size:13.5px}
.cmp-stores li.best .cmp-price{color:#2F7A55;font-weight:700}
.cmp-store{flex:1}
.cmp-price{font-family:'JetBrains Mono',ui-monospace,monospace;font-size:13px}

.muted-i{color:#8C9180;font-size:13px;font-style:italic;margin:6px 0}
.pad{padding:24px 4px}
.err{color:#A6432E;font-size:12.5px}
.modal-input{width:100%;border:1px solid #E5E0D2;border-radius:9px;padding:9px 11px;font-family:inherit;
  font-size:14px;background:#FBF9F3;color:#24211A;outline:none}
.modal-input.mt{margin-top:8px}
.modal-input.narrow{width:110px;text-align:right;font-family:'JetBrains Mono',ui-monospace,monospace}
.modal-label{font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:#A29B89;margin:16px 0 6px}
.modal-primary{margin-top:14px;width:100%}
.pick-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:6px}
.pick-list button{width:100%;display:flex;align-items:center;gap:9px;background:#FBF9F3;border:1px solid #E5E0D2;
  border-radius:10px;padding:9px 12px;font-family:inherit;font-size:13.5px;cursor:pointer;color:#24211A;text-align:left}
.grow{flex:1}
.inline-form{display:flex;gap:8px;margin-top:10px}
.colors{display:flex;gap:8px;margin-top:10px}
.swatch{width:26px;height:26px;border-radius:50%;border:2px solid transparent;cursor:pointer}
.swatch.on{border-color:#24211A}
.price-form{list-style:none;margin:12px 0 0;padding:0;display:flex;flex-direction:column;gap:8px}
.price-form li{display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:13.5px}
`;
