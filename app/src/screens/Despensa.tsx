import { useState } from "react";
import type { AppState } from "../core/types";
import type { Actions } from "../core/store";
import { fmt } from "../core/logic";

const PESO_UNITS = ["g", "kg", "ml", "L"];

const Icon = ({ d, size = 16, stroke = 1.7 }: { d: string; size?: number; stroke?: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
    <path d={d} />
  </svg>
);
const P = {
  plus: "M12 5v14M5 12h14",
  minus: "M5 12h14",
  x: "M18 6 6 18M6 6l12 12",
  check: "M20 6 9 17l-5-5",
  search: "M21 21l-4.3-4.3M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Z",
};

export function Despensa({ state, actions, onBack }: { state: AppState; actions: Actions; onBack: () => void }) {
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [addingPage, setAddingPage] = useState(false);
  const [newPageNameVal, setNewPageNameVal] = useState("");

  const active = state.pPages.find((p) => p.id === state.pActiveId) || null;

  const commitNewPage = () => {
    const name = newPageNameVal.trim();
    if (name) actions.addPage(name);
    setNewPageNameVal("");
    setAddingPage(false);
  };

  const q = query.trim().toLowerCase();
  const results = q
    ? state.pPages.flatMap((p) => p.ingredients.filter((i) => i.name.toLowerCase().includes(q)).map((i) => ({ ...i, pageId: p.id, pageName: p.name })))
    : [];

  return (
    <div className="pantry-root">
      <style>{css}</style>
      <div className="wrap">
        <button className="back" onClick={onBack}>
          ← Menú
        </button>
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
          <div className="mast-right">
            <button
              className={"search-btn" + (searchOpen ? " search-on" : "")}
              onClick={() => {
                setSearchOpen((o) => !o);
                if (searchOpen) setQuery("");
              }}
              aria-label={searchOpen ? "Cerrar búsqueda" : "Buscar ingrediente"}
            >
              <Icon d={searchOpen ? P.x : P.search} size={18} />
            </button>
            {active && (
              <div className="mast-tally">
                <span className="tally-num">{active.ingredients.length}</span>
                <span className="tally-label">
                  ítems en
                  <br />
                  esta página
                </span>
              </div>
            )}
          </div>
        </header>

        {searchOpen && (
          <div className="search">
            <div className="search-bar">
              <span className="search-ico">
                <Icon d={P.search} size={16} />
              </span>
              <input
                autoFocus
                className="search-input"
                placeholder="Buscar ingrediente en toda la despensa…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Escape") {
                    setQuery("");
                    setSearchOpen(false);
                  }
                }}
              />
              {query && (
                <button className="search-clear" onClick={() => setQuery("")} aria-label="Limpiar">
                  <Icon d={P.x} size={15} />
                </button>
              )}
            </div>
            {q &&
              (results.length === 0 ? (
                <p className="search-empty">Sin coincidencias para "{query.trim()}".</p>
              ) : (
                <ul className="search-results">
                  {results.map((r) => (
                    <li key={r.pageId + r.id} className="sresult">
                      <button
                        className="sresult-info"
                        onClick={() => {
                          actions.setActivePage(r.pageId);
                          setSearchOpen(false);
                          setQuery("");
                        }}
                        title="Ir a esta página"
                      >
                        <span className="sresult-name">{r.name}</span>
                        <span className="sresult-page">{r.pageName}</span>
                      </button>
                      <span className="sresult-qty">
                        {fmt(r.amount, r.unit)} {r.unit}
                      </span>
                    </li>
                  ))}
                </ul>
              ))}
          </div>
        )}

        <nav className="tabs" aria-label="Páginas por tipo de ingrediente">
          {state.pPages.map((p) => (
            <button key={p.id} className={"tab" + (p.id === state.pActiveId ? " tab-on" : "")} onClick={() => actions.setActivePage(p.id)}>
              <span className="tab-name">{p.name}</span>
              <span className="tab-count">{p.ingredients.length}</span>
            </button>
          ))}
          {addingPage ? (
            <span className="tab tab-adding">
              <input
                autoFocus
                className="tab-input"
                placeholder="Nombre de la página"
                value={newPageNameVal}
                onChange={(e) => setNewPageNameVal(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") commitNewPage();
                  if (e.key === "Escape") {
                    setAddingPage(false);
                    setNewPageNameVal("");
                  }
                }}
              />
              <button className="mini ok" onClick={commitNewPage} aria-label="Crear página">
                <Icon d={P.check} size={15} />
              </button>
            </span>
          ) : (
            <button className="tab tab-add" onClick={() => setAddingPage(true)}>
              <Icon d={P.plus} size={15} /> Página
            </button>
          )}
        </nav>

        {!active ? (
          <div className="empty-card">
            <p className="empty-title">No hay páginas</p>
            <p className="empty-body">Crea tu primera categoría de ingredientes para empezar.</p>
            <button className="primary" onClick={() => setAddingPage(true)}>
              <Icon d={P.plus} size={16} /> Crear página
            </button>
          </div>
        ) : (
          <section className="sheet">
            <div className="page-head">
              <h2 className="page-title">{active.name}</h2>
              <div className="page-actions">
                <button className="ghost danger" onClick={() => actions.deletePage(active.id)} aria-label="Eliminar página">
                  <Icon d={P.x} size={15} />
                </button>
              </div>
            </div>

            <ul className="list">
              {active.ingredients.length === 0 && <li className="row-empty">Esta página está vacía. Agrega tu primer ingrediente abajo.</li>}
              {active.ingredients.map((ing) => (
                <li key={ing.id} className="row">
                  <div className="row-head">
                    <span className={"badge " + (ing.type === "peso" ? "badge-peso" : "badge-unidad")}>{ing.type === "peso" ? "peso" : "unidad"}</span>
                    <span className="row-name">{ing.name}</span>
                  </div>
                  <div className="row-actions">
                    <span className="stepper">
                      <button className="step" onClick={() => actions.adjustIngredient(active.id, ing.id, -1)} aria-label="Restar">
                        <Icon d={P.minus} size={14} />
                      </button>
                      <span className="qty">
                        <span className="qty-num">{fmt(ing.amount, ing.unit)}</span>
                        <span className="qty-unit">{ing.unit}</span>
                      </span>
                      <button className="step" onClick={() => actions.adjustIngredient(active.id, ing.id, 1)} aria-label="Sumar">
                        <Icon d={P.plus} size={14} />
                      </button>
                    </span>
                    <span className="leader" aria-hidden />
                    <button className="ghost danger" onClick={() => actions.removeIngredient(active.id, ing.id)} aria-label="Eliminar">
                      <Icon d={P.x} size={14} />
                    </button>
                  </div>
                </li>
              ))}
            </ul>

            <AddForm onAdd={(ing) => actions.addIngredient(active.id, ing)} />
          </section>
        )}
      </div>
    </div>
  );
}

function AddForm({ onAdd }: { onAdd: (i: { name: string; type: "peso" | "unidad"; amount: number; unit: string }) => void }) {
  const [name, setName] = useState("");
  const [type, setType] = useState<"peso" | "unidad">("peso");
  const [amount, setAmount] = useState("");
  const [unit, setUnit] = useState("g");

  const submit = () => {
    const n = name.trim();
    if (!n) return;
    const amt = parseFloat(amount);
    onAdd({ name: n, type, amount: isNaN(amt) ? 0 : Math.max(0, amt), unit: type === "unidad" ? "u" : unit });
    setName("");
    setAmount("");
  };

  return (
    <div className="add">
      <div className="add-line">
        <input className="add-name" placeholder="Nombre del ingrediente" value={name} onChange={(e) => setName(e.target.value)} onKeyDown={(e) => e.key === "Enter" && submit()} />
      </div>
      <div className="add-line add-controls">
        <div className="toggle" role="tablist" aria-label="Tipo de medida">
          <button className={"tg " + (type === "peso" ? "tg-on" : "")} onClick={() => setType("peso")}>
            Peso
          </button>
          <button className={"tg " + (type === "unidad" ? "tg-on" : "")} onClick={() => setType("unidad")}>
            Unidad
          </button>
        </div>
        <input className="add-amount" type="number" min="0" step="any" inputMode="decimal" placeholder="0" value={amount} onChange={(e) => setAmount(e.target.value)} onKeyDown={(e) => e.key === "Enter" && submit()} />
        {type === "peso" ? (
          <select className="add-unit" value={unit} onChange={(e) => setUnit(e.target.value)}>
            {PESO_UNITS.map((u) => (
              <option key={u} value={u}>
                {u}
              </option>
            ))}
          </select>
        ) : (
          <span className="add-unit-fixed">u</span>
        )}
        <button className="primary add-btn" onClick={submit}>
          <Icon d={P.plus} size={16} /> Agregar
        </button>
      </div>
    </div>
  );
}

const css = `
.pantry-root{
  --paper:#FBF9F3;--card:#FEFDFA;--ink:#24211A;--inkSoft:#726C5C;
  --inkFaint:#A29B89;--olive:#57682B;--oliveSoft:#E9EDD8;
  --amber:#8C5E12;--amberSoft:#F4E9CE;--line:#E5E0D2;--rule:#CDC7B5;
  --danger:#A6432E;--dangerSoft:#F2E1DB;
  min-height:100vh;background:var(--paper);color:var(--ink);
  font-family:'Inter',system-ui,sans-serif;
  padding:20px 16px 48px;box-sizing:border-box;
  -webkit-font-smoothing:antialiased;
}
.pantry-root *{box-sizing:border-box}
.wrap{max-width:680px;margin:0 auto}
.back{border:none;background:none;color:var(--inkSoft);font-family:inherit;font-size:12.5px;font-weight:600;letter-spacing:.06em;text-transform:uppercase;cursor:pointer;padding:0 0 14px;}

.masthead{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;
  padding-bottom:18px;border-bottom:2px solid var(--ink)}
.mast-left{display:flex;align-items:center;gap:14px}
.mast-mark{font-size:30px;color:var(--olive);line-height:1;margin-top:2px}
.mast-title{font-family:'Fraunces',serif;font-weight:640;font-size:34px;line-height:1;margin:0;
  letter-spacing:-.01em}
.mast-sub{margin:5px 0 0;color:var(--inkSoft);font-size:13.5px;letter-spacing:.02em}
.mast-tally{display:flex;align-items:center;gap:9px;text-align:right}
.mast-right{display:flex;align-items:center;gap:14px}
.search-btn{width:40px;height:40px;flex:0 0 auto;border:1px solid var(--line);background:var(--card);
  border-radius:11px;display:grid;place-items:center;cursor:pointer;color:var(--inkSoft);transition:.15s}
.search-btn:hover{border-color:var(--olive);color:var(--olive)}
.search-on{background:var(--ink);border-color:var(--ink);color:var(--paper)}
.search-on:hover{color:var(--paper)}

.search{margin-top:16px;background:var(--card);border:1px solid var(--line);border-radius:14px;
  padding:12px;box-shadow:0 14px 30px -24px rgba(36,33,26,.4)}
.search-bar{display:flex;align-items:center;gap:9px;background:var(--paper);border:1px solid var(--line);
  border-radius:10px;padding:9px 12px}
.search-bar:focus-within{border-color:var(--olive)}
.search-ico{color:var(--inkFaint);display:grid;place-items:center}
.search-input{flex:1;border:none;background:transparent;outline:none;font-family:inherit;font-size:15px;color:var(--ink)}
.search-clear{border:none;background:transparent;color:var(--inkFaint);cursor:pointer;display:grid;
  place-items:center;padding:2px;border-radius:6px}
.search-clear:hover{color:var(--ink)}
.search-empty{margin:12px 4px 4px;color:var(--inkFaint);font-size:13.5px;font-style:italic}
.search-results{list-style:none;margin:8px 0 0;padding:0;max-height:300px;overflow-y:auto}
.sresult{display:flex;align-items:center;gap:12px;padding:9px 4px}
.sresult + .sresult{border-top:1px dotted var(--line)}
.sresult-info{flex:1 1 auto;min-width:0;display:flex;flex-direction:column;gap:2px;align-items:flex-start;
  background:none;border:none;cursor:pointer;text-align:left;font-family:inherit;padding:2px 4px;border-radius:7px}
.sresult-info:hover{background:var(--paper)}
.sresult-name{font-size:15px;font-weight:500;color:var(--ink);overflow-wrap:anywhere}
.sresult-page{font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:var(--inkFaint)}
.sresult-qty{font-family:'JetBrains Mono',monospace;font-weight:600;font-size:15px;color:var(--ink)}
.tally-num{font-family:'JetBrains Mono',monospace;font-weight:600;font-size:30px;color:var(--olive);line-height:1}
.tally-label{font-size:10.5px;text-transform:uppercase;letter-spacing:.09em;color:var(--inkFaint);line-height:1.25}

.tabs{display:flex;gap:7px;overflow-x:auto;padding:18px 2px 16px;scrollbar-width:thin}
.tab{flex:0 0 auto;display:flex;align-items:center;gap:8px;border:1px solid var(--line);
  background:var(--card);color:var(--inkSoft);border-radius:999px;padding:8px 14px;
  font-family:inherit;font-size:13.5px;font-weight:500;cursor:pointer;transition:.15s;white-space:nowrap}
.tab:hover{border-color:var(--rule);color:var(--ink)}
.tab-on{background:var(--ink);border-color:var(--ink);color:var(--paper)}
.tab-on .tab-name{color:var(--paper)}
.tab-on .tab-count{background:rgba(255,255,255,.18);color:var(--paper)}
.tab-name{font-weight:600}
.tab-count{font-family:'JetBrains Mono',monospace;font-size:11px;background:var(--oliveSoft);
  color:var(--olive);border-radius:999px;padding:1px 7px;min-width:20px;text-align:center}
.tab-add{color:var(--olive);border-style:dashed;font-weight:600}
.tab-adding{padding:4px 6px 4px 12px;background:var(--card);border-color:var(--olive)}
.tab-input{border:none;background:transparent;font-family:inherit;font-size:13.5px;outline:none;
  width:150px;color:var(--ink)}

.sheet{background:var(--card);border:1px solid var(--line);border-radius:16px;
  padding:8px 20px 20px;box-shadow:0 1px 0 rgba(36,33,26,.03),0 14px 30px -22px rgba(36,33,26,.25)}
.page-head{display:flex;align-items:center;justify-content:space-between;gap:12px;
  padding:16px 2px 12px;border-bottom:1px solid var(--line)}
.page-title{font-family:'Fraunces',serif;font-weight:560;font-size:23px;margin:0;letter-spacing:-.01em}
.page-actions{display:flex;gap:4px}

.list{list-style:none;margin:0;padding:6px 0 4px}
.row{display:flex;flex-direction:column;gap:9px;padding:13px 2px}
.row + .row{border-top:1px dotted var(--line)}
.row-head{display:flex;align-items:flex-start;gap:9px}
.badge{font-size:9.5px;text-transform:uppercase;letter-spacing:.07em;font-weight:600;
  padding:3px 7px;border-radius:5px;flex:0 0 auto;margin-top:2px}
.badge-peso{background:var(--oliveSoft);color:var(--olive)}
.badge-unidad{background:var(--amberSoft);color:var(--amber)}
.row-name{font-size:15.5px;font-weight:500;color:var(--ink);line-height:1.35;
  overflow-wrap:anywhere;flex:1 1 auto}
.row-actions{display:flex;align-items:center;gap:11px;padding-left:2px}
.leader{flex:1 1 auto;min-width:14px;border-bottom:2px dotted var(--rule);
  transform:translateY(-3px);margin:0 2px}

.stepper{display:flex;align-items:center;gap:6px;flex:0 0 auto}
.step{width:24px;height:24px;border:1px solid var(--line);background:var(--paper);border-radius:7px;
  display:grid;place-items:center;cursor:pointer;color:var(--inkSoft);transition:.13s;padding:0}
.step:hover{border-color:var(--olive);color:var(--olive);background:var(--oliveSoft)}
.qty{display:inline-flex;align-items:baseline;gap:4px;min-width:70px;justify-content:flex-end}
.qty-num{font-family:'JetBrains Mono',monospace;font-weight:600;font-size:16px;color:var(--ink);
  font-variant-numeric:tabular-nums}
.qty-unit{font-family:'JetBrains Mono',monospace;font-size:11.5px;color:var(--inkFaint);font-weight:500}

.ghost{width:28px;height:28px;border:none;background:transparent;border-radius:7px;color:var(--inkFaint);
  display:grid;place-items:center;cursor:pointer;transition:.13s;padding:0}
.ghost:hover{background:var(--paper);color:var(--ink)}
.ghost.danger:hover{background:var(--dangerSoft);color:var(--danger)}

.row-empty{padding:12px 2px;color:var(--inkFaint);font-size:13.5px;font-style:italic}

.add{margin-top:16px;padding-top:16px;border-top:2px solid var(--ink)}
.add-line{display:flex;gap:9px;align-items:center;flex-wrap:wrap}
.add-line + .add-line{margin-top:10px}
.add-name{flex:1 1 auto;border:1px solid var(--line);border-radius:9px;padding:10px 12px;
  font-family:inherit;font-size:15px;outline:none;background:var(--paper);color:var(--ink)}
.toggle{display:inline-flex;border:1px solid var(--line);border-radius:9px;overflow:hidden;background:var(--card)}
.tg{border:none;background:transparent;padding:9px 14px;font-family:inherit;font-size:13.5px;font-weight:500;
  cursor:pointer;color:var(--inkSoft);transition:.13s}
.tg + .tg{border-left:1px solid var(--line)}
.tg-on{background:var(--olive);color:#fff}
.add-amount{width:88px;border:1px solid var(--line);border-radius:9px;padding:10px 11px;
  font-family:'JetBrains Mono',monospace;font-size:15px;outline:none;background:var(--paper);color:var(--ink)}
.add-unit{border:1px solid var(--line);border-radius:9px;padding:10px 8px;font-family:inherit;
  font-size:13.5px;background:var(--card);color:var(--ink);outline:none;cursor:pointer}
.add-unit-fixed{font-family:'JetBrains Mono',monospace;color:var(--inkFaint);font-size:14px;
  padding:0 4px;min-width:22px;text-align:center}
.add-btn{margin-left:auto}

.primary{display:inline-flex;align-items:center;gap:7px;background:var(--olive);color:#fff;border:none;
  border-radius:9px;padding:10px 16px;font-family:inherit;font-size:14px;font-weight:600;cursor:pointer;
  transition:.15s}
.primary:hover{background:#485824}
.mini{width:30px;height:30px;border:1px solid var(--line);background:var(--card);border-radius:8px;
  display:grid;place-items:center;cursor:pointer;color:var(--inkSoft);transition:.13s;padding:0}
.mini:hover{border-color:var(--rule);color:var(--ink)}
.mini.ok{background:var(--olive);border-color:var(--olive);color:#fff}
.mini.ok:hover{background:#485824}

.empty-card{text-align:center;color:var(--inkSoft);background:var(--card);border:1px dashed var(--rule);
  border-radius:16px;padding:44px 24px;display:flex;flex-direction:column;align-items:center;gap:8px}
.empty-title{font-family:'Fraunces',serif;font-size:20px;font-weight:560;margin:0;color:var(--ink)}
.empty-body{margin:0 0 8px;font-size:14px}

:focus-visible{outline:2px solid var(--olive);outline-offset:2px;border-radius:6px}

@media (max-width:520px){
  .mast-title{font-size:28px}
  .qty{min-width:58px}
  .row-name{font-size:14.5px}
  .add-btn{margin-left:0;width:100%;justify-content:center}
  .tally-label{display:none}
}
@media (prefers-reduced-motion:reduce){
  .pantry-root *{transition:none!important}
}
`;
