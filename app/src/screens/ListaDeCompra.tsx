import { useMemo, useState } from "react";
import type { AppState } from "../core/types";
import type { Actions } from "../core/store";
import { fmt } from "../core/logic";

const PALETTE = ["#C1494E", "#B07A2E", "#5A8F3C", "#C79A2A", "#4C7A93", "#8C5E9E", "#A65D8C"];
const UNITS = ["u", "g", "kg", "ml", "L", "dientes"];

export function ListaDeCompra({ state, actions, onBack }: { state: AppState; actions: Actions; onBack: () => void }) {
  const [view, setView] = useState<"lista" | "articulos">("lista");
  const [newName, setNewName] = useState("");
  const [newNameFocus, setNewNameFocus] = useState(false);
  const [newQty, setNewQty] = useState("");
  const [newUnit, setNewUnit] = useState("u");
  const [newPageId, setNewPageId] = useState<string | null>(null);

  const allIncluded = useMemo(() => state.pPages.flatMap((p) => p.ingredients).filter((i) => state.lIncluded[i.id]), [state]);
  const done = allIncluded.filter((i) => state.lDone[i.id]).length;
  const total = allIncluded.length;
  const pct = total ? Math.round((done / total) * 100) : 0;
  const footMsg = done === 0 ? "Marca cada cosa al ponerla en el carrito." : done === total ? "¡Todo en el carrito! 🛒" : `Faltan ${total - done} artículos.`;

  const pantryUnique = useMemo(() => {
    const seen = new Set<string>();
    const out: { name: string; pageId: string; unit: string }[] = [];
    state.pPages.forEach((p) => p.ingredients.forEach((i) => {
      const k = i.name.toLowerCase();
      if (!seen.has(k)) { seen.add(k); out.push({ name: i.name, pageId: p.id, unit: i.unit }); }
    }));
    return out;
  }, [state.pPages]);

  const lq = newName.trim().toLowerCase();
  const nameOptions = pantryUnique.filter((i) => !lq || i.name.toLowerCase().includes(lq));
  const showAddNew = lq.length > 0 && !pantryUnique.some((i) => i.name.toLowerCase() === lq);
  const showOptions = newNameFocus && (nameOptions.length > 0 || showAddNew);

  const effectivePageId = newPageId || (state.pPages[0] && state.pPages[0].id) || "";

  const commitAddItem = () => {
    const name = newName.trim();
    if (!name) return;
    const qty = parseFloat(newQty);
    actions.addShoppingItem({ name, amount: isNaN(qty) ? 0 : Math.max(0, qty), unit: newUnit || "u", pageId: effectivePageId });
    setNewName("");
    setNewQty("");
    setNewUnit("u");
  };

  const listaCategories = state.pPages
    .map((p, idx) => {
      const items = p.ingredients.filter((i) => state.lIncluded[i.id]);
      const d = items.filter((i) => state.lDone[i.id]).length;
      return { id: p.id, name: p.name, color: PALETTE[idx % PALETTE.length], countLabel: `${d}/${items.length}`, items };
    })
    .filter((cat) => cat.items.length > 0);

  return (
    <div className="lista-root">
      <style>{css}</style>
      <header className="header">
        <div className="wrap">
          <button className="back" onClick={onBack}>← Menú</button>
          <p className="eyebrow">Despensa · agrupada</p>
          <h1>Lista de compra</h1>
          <div className="meter-wrap">
            <div className="track"><div className="fill" style={{ width: `${pct}%` }} /></div>
            <div className="count">{done}/{total} en el carrito</div>
          </div>
          <div className="tabs">
            <button className={"tab" + (view === "lista" ? " active" : "")} onClick={() => setView("lista")}>Lista</button>
            <button className={"tab" + (view === "articulos" ? " active" : "")} onClick={() => setView("articulos")}>Artículos</button>
          </div>
        </div>
      </header>

      <main className="wrap">
        {view === "articulos" ? (
          <>
            <div className="addbox">
              <div className="addbox-title">+ Agregar artículo</div>
              <div className="addbox-row">
                <div className="autocomplete">
                  <input
                    placeholder="Buscar en despensa o agregar artículo"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    onFocus={() => setNewNameFocus(true)}
                    onBlur={() => setTimeout(() => setNewNameFocus(false), 150)}
                  />
                  {showOptions && (
                    <div className="options">
                      {nameOptions.map((opt) => (
                        <button key={opt.name} onMouseDown={(e) => e.preventDefault()} onClick={() => { setNewName(opt.name); setNewNameFocus(false); setNewPageId(opt.pageId); setNewUnit(opt.unit || "u"); }}>
                          {opt.name}
                        </button>
                      ))}
                      {showAddNew && <div className="add-hint">"{newName}" se agregará también a Despensa</div>}
                    </div>
                  )}
                </div>
                <input className="qty-input" type="number" min="0" placeholder="Cant. (opcional)" value={newQty} onChange={(e) => setNewQty(e.target.value)} />
                <select value={newUnit} onChange={(e) => setNewUnit(e.target.value)}>
                  {UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
                </select>
                <select value={effectivePageId} onChange={(e) => setNewPageId(e.target.value)}>
                  {state.pPages.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
                <button className="commit" onClick={commitAddItem}>Agregar</button>
              </div>
            </div>

            {state.pPages.map((p, idx) => (
              <section className="cat" key={p.id}>
                <div className="cat-head">
                  <span className="dot" style={{ background: PALETTE[idx % PALETTE.length] }} />
                  <span className="cat-name">{p.name}</span>
                  <span className="cat-count">{p.ingredients.filter((i) => state.lIncluded[i.id]).length}/{p.ingredients.length}</span>
                </div>
                <ul>
                  {p.ingredients.map((it) => (
                    <li key={it.id}>
                      <div className="row">
                        <span className="name">{it.name}</span>
                        {it.amount > 0 && <span className="qty">{fmt(it.amount, it.unit)} {it.unit}</span>}
                        <span className="in-list-label">En lista</span>
                        <span className={"sw" + (state.lIncluded[it.id] ? " on" : "")} onClick={() => actions.toggleIncluded(it.id)}><span className="sw-dot" /></span>
                        <button className="remove" onClick={() => actions.removeIngredient(p.id, it.id)}>✕</button>
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </>
        ) : listaCategories.length === 0 ? (
          <p className="empty">Nada en la lista todavía. Activa el switch "En lista" de un artículo en la pestaña Artículos.</p>
        ) : (
          listaCategories.map((cat) => (
            <section className="cat" key={cat.id}>
              <div className="cat-head">
                <span className="dot" style={{ background: cat.color }} />
                <span className="cat-name">{cat.name}</span>
                <span className="cat-count">{cat.countLabel}</span>
              </div>
              <ul>
                {cat.items.map((it) => (
                  <li key={it.id}>
                    <div className="row">
                      <span className={"name" + (state.lDone[it.id] ? " done" : "")} onClick={() => actions.toggleDone(it.id)}>{it.name}</span>
                      {it.amount > 0 && <span className="qty">{fmt(it.amount, it.unit)} {it.unit}</span>}
                      <span className={"sw big" + (state.lDone[it.id] ? " on" : "")} onClick={() => actions.toggleDone(it.id)}><span className="sw-dot" /></span>
                      <button className="remove" onClick={() => actions.removeFromLista(it.id)}>✕</button>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          ))
        )}
      </main>

      <div className="footbar">
        <div className="wrap footbar-inner">
          <span className="foot-msg">{footMsg}</span>
          <button className="reset" onClick={actions.resetLista}>Reiniciar</button>
        </div>
      </div>
    </div>
  );
}

const css = `
.lista-root{
  --paper:#F4F5EF;--card:#FBFCF8;--ink:#22271E;--ink-soft:#5C6353;--line:#E3E5DA;
  --cart:#2F9E6B;
  background:var(--paper);color:var(--ink);font-family:'Inter',system-ui,sans-serif;
  line-height:1.4;padding-bottom:96px;-webkit-font-smoothing:antialiased;min-height:100vh;
}
.lista-root *{box-sizing:border-box}
.lista-root .wrap{max-width:680px;margin:0 auto}
.lista-root .header{position:sticky;top:0;z-index:20;background:rgba(244,245,239,.92);backdrop-filter:blur(10px);
  border-bottom:1px solid var(--line);padding:14px 20px 14px}
.back{border:none;background:none;color:var(--ink-soft);font-family:inherit;font-size:12.5px;font-weight:600;
  letter-spacing:.06em;text-transform:uppercase;cursor:pointer;padding:0 0 10px}
.eyebrow{font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:var(--ink-soft);font-weight:600;margin:0 0 2px}
h1{font-family:'Fraunces',serif;font-weight:600;font-size:26px;line-height:1.05;margin:0;letter-spacing:-.01em}
.meter-wrap{margin-top:14px;display:flex;align-items:center;gap:14px}
.track{flex:1;height:10px;border-radius:20px;background:#E7E9DE;overflow:hidden}
.fill{height:100%;border-radius:20px;background:linear-gradient(90deg,#3FB27C,var(--cart));transition:width .3s}
.count{font-family:'Fraunces',serif;font-weight:600;font-size:15px;white-space:nowrap}
.tabs{display:flex;gap:4px;margin-top:14px;background:#E7E9DE;padding:4px;border-radius:12px}
.tab{flex:1;border:none;cursor:pointer;font-family:inherit;font-weight:600;font-size:14px;padding:9px 8px;
  border-radius:9px;background:transparent;color:var(--ink-soft)}
.tab.active{background:var(--card);color:var(--ink);box-shadow:0 1px 3px rgba(41,32,25,.12)}

main{padding:20px}
.addbox{background:var(--card);border:1px solid var(--line);border-radius:16px;margin-bottom:16px;padding:14px 16px}
.addbox-title{font-family:'Fraunces',serif;font-weight:600;font-size:15px;margin-bottom:10px}
.addbox-row{display:flex;gap:8px;flex-wrap:wrap}
.addbox-row input, .addbox-row select{border:1px solid var(--line);border-radius:9px;padding:9px 11px;
  font-family:inherit;font-size:14px;outline:none;background:var(--paper);color:var(--ink)}
.autocomplete{position:relative;flex:1 1 160px}
.autocomplete input{width:100%}
.qty-input{width:120px;font-family:'JetBrains Mono',monospace!important}
.options{position:absolute;left:0;right:0;top:calc(100% + 4px);background:var(--card);border:1px solid var(--line);
  border-radius:9px;box-shadow:0 6px 18px rgba(0,0,0,.08);z-index:5;max-height:160px;overflow:auto}
.options button{display:block;width:100%;text-align:left;border:none;background:none;padding:8px 12px;
  font-family:inherit;font-size:13px;cursor:pointer;color:var(--ink)}
.options button:hover{background:var(--paper)}
.add-hint{padding:8px 12px;font-size:12px;color:var(--ink-soft)}
.commit{background:var(--cart);color:#fff;border:none;border-radius:9px;padding:9px 16px;font-family:inherit;
  font-size:14px;font-weight:600;cursor:pointer}

.cat{background:var(--card);border:1px solid var(--line);border-radius:16px;margin-bottom:16px;overflow:hidden;
  box-shadow:0 1px 2px rgba(34,39,30,.05),0 8px 24px -16px rgba(34,39,30,.25)}
.cat-head{display:flex;align-items:center;gap:12px;padding:14px 16px;border-bottom:1px solid var(--line)}
.dot{width:12px;height:12px;border-radius:50%;flex:none}
.cat-name{font-family:'Fraunces',serif;font-weight:600;font-size:16px;flex:1}
.cat-count{font-size:12px;font-weight:600;color:var(--ink-soft);background:#EFF0E9;border-radius:20px;padding:3px 10px}

ul{list-style:none;margin:0;padding:4px 6px 8px}
.row{display:flex;align-items:center;gap:12px;padding:11px 10px;border-radius:11px}
.name{flex:1;font-size:15px;font-weight:500;cursor:pointer}
.name.done{color:var(--ink-soft);text-decoration:line-through;text-decoration-color:#B9BDAF}
.in-list-label{font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:.04em;color:var(--ink-soft)}
.qty{font-size:12px;font-weight:600;color:var(--ink-soft);background:#EEEFE8;border-radius:7px;padding:2px 8px;flex:none}
.remove{border:none;background:none;color:#A9AE9E;cursor:pointer;font-size:13px;padding:2px 4px}

.sw{--w:40px;--h:24px;--pad:2.5px;width:var(--w);height:var(--h);flex:none;border-radius:20px;
  background:#D2D5C8;position:relative;cursor:pointer;transition:background .22s}
.sw.big{--w:46px;--h:27px;--pad:3px}
.sw.on{background:#4C7A93}
.sw.big.on{background:var(--cart)}
.sw-dot{position:absolute;top:var(--pad);left:var(--pad);width:calc(var(--h) - var(--pad)*2);
  height:calc(var(--h) - var(--pad)*2);border-radius:50%;background:#fff;box-shadow:0 1px 3px rgba(0,0,0,.22);
  transition:transform .22s cubic-bezier(.4,.9,.4,1.2)}
.sw.on .sw-dot{transform:translateX(calc(var(--w) - var(--h)))}

.empty{color:#8C9180;font-size:13.5px;font-style:italic;text-align:center;padding:24px 0}

.footbar{position:fixed;left:0;right:0;bottom:0;z-index:20;background:rgba(251,252,248,.94);backdrop-filter:blur(10px);
  border-top:1px solid var(--line);padding:12px 20px}
.footbar-inner{display:flex;align-items:center;justify-content:space-between;gap:12px}
.foot-msg{font-size:13px;color:var(--ink-soft)}
.reset{font-family:'Inter',sans-serif;font-size:13px;font-weight:600;color:var(--ink-soft);background:transparent;
  border:1px solid var(--line);border-radius:10px;padding:8px 14px;cursor:pointer}
.reset:hover{background:#F1F2EB;color:var(--ink)}
`;
