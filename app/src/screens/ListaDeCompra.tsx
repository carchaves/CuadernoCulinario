import { useMemo, useState } from "react";
import type { AppState } from "../core/types";
import { fmt } from "../core/logic";

const PALETTE = ["#C1494E", "#B07A2E", "#5A8F3C", "#C79A2A", "#4C7A93", "#8C5E9E", "#A65D8C"];

export function ListaDeCompra({ state, onBack }: { state: AppState; onBack: () => void }) {
  const [view, setView] = useState<"lista" | "articulos">("lista");

  const allIncluded = useMemo(() => state.pPages.flatMap((p) => p.ingredients).filter((i) => state.lIncluded[i.id]), [state]);
  const done = allIncluded.filter((i) => state.lDone[i.id]).length;
  const total = allIncluded.length;
  const pct = total ? Math.round((done / total) * 100) : 0;
  const footMsg = total === 0 ? "La lista está vacía." : done === total ? "¡Todo en el carrito! 🛒" : `Faltan ${total - done} artículos.`;

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
          state.pPages.map((p, idx) => (
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
                      {state.lIncluded[it.id] && <span className="in-list-label">En lista</span>}
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          ))
        ) : listaCategories.length === 0 ? (
          <p className="empty">Nada en la lista todavía.</p>
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
                      <span className={"name" + (state.lDone[it.id] ? " done" : "")}>{it.name}</span>
                      {it.amount > 0 && <span className="qty">{fmt(it.amount, it.unit)} {it.unit}</span>}
                      {state.lDone[it.id] && <span className="check" aria-label="En el carrito">✓</span>}
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

.cat{background:var(--card);border:1px solid var(--line);border-radius:16px;margin-bottom:16px;overflow:hidden;
  box-shadow:0 1px 2px rgba(34,39,30,.05),0 8px 24px -16px rgba(34,39,30,.25)}
.cat-head{display:flex;align-items:center;gap:12px;padding:14px 16px;border-bottom:1px solid var(--line)}
.dot{width:12px;height:12px;border-radius:50%;flex:none}
.cat-name{font-family:'Fraunces',serif;font-weight:600;font-size:16px;flex:1}
.cat-count{font-size:12px;font-weight:600;color:var(--ink-soft);background:#EFF0E9;border-radius:20px;padding:3px 10px}

ul{list-style:none;margin:0;padding:4px 6px 8px}
.row{display:flex;align-items:center;gap:12px;padding:11px 10px;border-radius:11px}
.name{flex:1;font-size:15px;font-weight:500}
.name.done{color:var(--ink-soft);text-decoration:line-through;text-decoration-color:#B9BDAF}
.in-list-label{font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:.04em;color:var(--cart);
  background:#E4F2EA;border-radius:20px;padding:3px 9px;flex:none}
.qty{font-size:12px;font-weight:600;color:var(--ink-soft);background:#EEEFE8;border-radius:7px;padding:2px 8px;flex:none}
.check{width:24px;height:24px;flex:none;border-radius:50%;background:var(--cart);color:#fff;font-size:13px;
  font-weight:700;display:grid;place-items:center}

.empty{color:#8C9180;font-size:13.5px;font-style:italic;text-align:center;padding:24px 0}

.footbar{position:fixed;left:0;right:0;bottom:0;z-index:20;background:rgba(251,252,248,.94);backdrop-filter:blur(10px);
  border-top:1px solid var(--line);padding:12px 20px}
.footbar-inner{display:flex;align-items:center;justify-content:center;gap:12px}
.foot-msg{font-size:13px;color:var(--ink-soft)}
`;
