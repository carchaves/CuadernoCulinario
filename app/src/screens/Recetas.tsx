import { useMemo, useState } from "react";
import type { AppState, RecipeView } from "../core/types";
import { RecipeList } from "./recetas/RecipeList";
import { RecipeDetail } from "./recetas/RecipeDetail";

const ACCENT = "#C4562A";

export function Recetas({ state, onBack }: { state: AppState; onBack: () => void }) {
  const [view, setView] = useState<RecipeView>("cocina");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [servings, setServings] = useState(4);
  const [sortByTime, setSortByTime] = useState(false);
  const [filterByPantry, setFilterByPantry] = useState(false);
  const [pantryLinked, setPantryLinked] = useState(true);

  const pantryFlat = useMemo(() => state.pPages.flatMap((p) => p.ingredients), [state.pPages]);
  const pantryNames = useMemo(() => pantryFlat.map((i) => i.name.toLowerCase()), [pantryFlat]);

  const recipes = state.recipes[view] || [];
  const selectedRecipe = selectedId ? recipes.find((r) => r.id === selectedId) || null : null;

  const showList = selectedId == null;
  const showDetail = selectedId != null;

  let doneCount = 0;
  let totalCount = 0;
  if (selectedRecipe) {
    selectedRecipe.prePrep.forEach((_, i) => {
      totalCount++;
      if (state.rDone[`${view}.${selectedRecipe.id}.pre.${i}`]) doneCount++;
    });
    selectedRecipe.prep.forEach((_, i) => {
      totalCount++;
      if (state.rDone[`${view}.${selectedRecipe.id}.prep.${i}`]) doneCount++;
    });
  }
  const pct = totalCount ? Math.round((doneCount / totalCount) * 100) : 0;

  return (
    <div className="recetas-root">
      <style>{css}</style>
      <header className="header">
        <div className="wrap">
          <button className="back" onClick={onBack}>
            ← Menú
          </button>
          <div className="servings-row">
            <span className="servings-label">PARA</span>
            <button className="servings-btn" onClick={() => setServings((n) => Math.max(1, n - 1))}>
              −
            </button>
            <span className="servings-n">{servings}</span>
            <button className="servings-btn" onClick={() => setServings((n) => Math.min(24, n + 1))}>
              +
            </button>
            <span className="servings-label">personas</span>
          </div>
          <div className="title-row">
            <h1>Recetas</h1>
            <button
              className={"pantry-toggle" + (pantryLinked ? " on" : "")}
              style={pantryLinked ? { background: ACCENT, borderColor: ACCENT } : undefined}
              aria-label="Usar Despensa"
              onClick={() => {
                setPantryLinked((v) => !v);
                if (pantryLinked) setFilterByPantry(false);
              }}
            >
              <svg viewBox="0 0 52 52" width="20" height="20" fill="none" stroke={pantryLinked ? "#FDFBF6" : "#292019"} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                <path d="M15 18 h22 v19 a3 3 0 0 1 -3 3 H18 a3 3 0 0 1 -3 -3 Z" />
                <path d="M13 16 h26" />
                <path d="M26 12 v4" />
                <path d="M15 24 a3 3 0 0 1 -4 0" />
                <path d="M37 24 a3 3 0 0 0 4 0" />
                {!pantryLinked && <path d="M42 10 L10 42" />}
              </svg>
            </button>
          </div>

          {showList && (
            <div className="tabs">
              <button className={"tab" + (view === "cocina" ? " active" : "")} onClick={() => { setView("cocina"); setSelectedId(null); }}>
                🍲 Cocina
              </button>
              <button className={"tab" + (view === "repo" ? " active" : "")} onClick={() => { setView("repo"); setSelectedId(null); }}>
                🧁 Repostería
              </button>
            </div>
          )}

          {showDetail && selectedRecipe && (
            <>
              <h2 className="subtitle" style={{ color: ACCENT }}>
                {selectedRecipe.title}
              </h2>
              <div className="meter-wrap">
                <div className="track">
                  <div className="fill" style={{ width: `${pct}%`, background: ACCENT }} />
                </div>
                <div className="meter-count">
                  {doneCount} / {totalCount} pasos
                </div>
              </div>
            </>
          )}
        </div>
      </header>

      <main className="wrap">
        {showList && (
          <RecipeList
            recipes={recipes}
            rDone={state.rDone}
            view={view}
            sortByTime={sortByTime}
            filterByPantry={filterByPantry}
            pantryLinked={pantryLinked}
            pantryNames={pantryNames}
            accent={ACCENT}
            onOpen={setSelectedId}
            onToggleSort={() => setSortByTime((v) => !v)}
            onToggleFilter={() => setFilterByPantry((v) => !v)}
          />
        )}

        {showDetail && selectedRecipe && (
          <RecipeDetail
            view={view}
            recipe={selectedRecipe}
            servings={servings}
            pantryLinked={pantryLinked}
            pantryFlat={pantryFlat}
            rDone={state.rDone}
            accent={ACCENT}
            onClose={() => setSelectedId(null)}
          />
        )}
      </main>
    </div>
  );
}

const css = `
.recetas-root{
  background:#F6F2EA;color:#292019;font-family:'DM Sans',system-ui,sans-serif;line-height:1.45;
  padding-bottom:60px;-webkit-font-smoothing:antialiased;min-height:100vh;
}
.recetas-root *{box-sizing:border-box}
.recetas-root .wrap{max-width:640px;margin:0 auto}
.recetas-root .header{position:sticky;top:0;z-index:30;background:rgba(246,242,234,.93);backdrop-filter:blur(10px);
  border-bottom:1px solid #E7D8BC;padding:14px 20px 12px}
.back{border:none;background:none;color:#7A7062;font-family:inherit;font-size:12.5px;font-weight:600;
  letter-spacing:.06em;text-transform:uppercase;cursor:pointer;padding:0 0 10px}
.servings-row{display:flex;align-items:center;gap:8px;margin:0 0 1px}
.servings-label{font-size:11px;letter-spacing:.2em;text-transform:uppercase;color:#7A7062;font-weight:600}
.servings-btn{width:20px;height:20px;border:1px solid #E7D8BC;background:#FDFBF6;border-radius:6px;
  display:grid;place-items:center;cursor:pointer;color:#7A7062;font-size:12px;padding:0}
.servings-n{font-size:12px;font-weight:700;color:#292019;min-width:14px;text-align:center}
.title-row{display:flex;align-items:center;justify-content:space-between;gap:12px}
h1{font-family:'Instrument Serif',serif;font-weight:400;font-size:30px;line-height:1;margin:0}
.pantry-toggle{width:40px;height:40px;flex:none;border-radius:50%;border:1px solid #E7D8BC;background:#FDFBF6;
  display:grid;place-items:center;cursor:pointer}
.subtitle{font-family:'Instrument Serif',serif;font-weight:400;font-size:24px;line-height:1;margin:6px 0 0}

.tabs{display:flex;gap:4px;margin-top:14px;background:#EBE5D9;padding:4px;border-radius:12px}
.tab{flex:1;border:none;background:transparent;cursor:pointer;font-family:'DM Sans',sans-serif;font-weight:600;
  font-size:14px;color:#7A7062;padding:9px 8px;border-radius:9px;display:flex;align-items:center;justify-content:center;gap:7px}
.tab.active{background:#FDFBF6;color:#292019;box-shadow:0 1px 3px rgba(41,32,25,.12)}

.meter-wrap{margin-top:12px;display:flex;align-items:center;gap:12px}
.track{flex:1;height:8px;border-radius:20px;background:#E4DDD0;overflow:hidden}
.fill{height:100%;border-radius:20px;transition:width .3s}
.meter-count{font-size:13px;font-weight:600;color:#7A7062;white-space:nowrap}

main{padding:18px 20px}
.filters{display:flex;align-items:center;justify-content:center;gap:8px;margin-bottom:14px;flex-wrap:wrap}
.filter-btn{border:1px solid #E7D8BC;background:#FDFBF6;color:#7A7062;border-radius:9px;padding:8px 13px;
  font-family:inherit;font-size:13px;font-weight:600;cursor:pointer}

.rcard{width:100%;text-align:left;background:#FDFBF6;border:1px solid #E7D8BC;border-radius:16px;margin-bottom:12px;
  padding:14px 16px;cursor:pointer;font-family:inherit;display:flex;align-items:flex-start;gap:12px}
.rcard-n{font-family:'Instrument Serif',serif;font-size:22px;line-height:1;width:24px;flex:none;font-style:italic}
.rcard-body{flex:1;min-width:0}
.rcard-body h2{font-family:'Instrument Serif',serif;font-weight:400;font-size:19px;margin:0;color:#292019}
.rcard-chips{display:flex;flex-wrap:wrap;gap:6px;margin-top:6px}
.chip{font-size:11px;font-weight:600;padding:3px 9px;border-radius:20px;background:#EFEADF;color:#7A7062}
.rcard-count{font-size:12px;font-weight:700;padding-top:2px}
.divider{display:flex;align-items:center;gap:10px;margin:6px 0 14px;color:#A99A82;font-size:12px;font-weight:600;
  letter-spacing:.04em;text-transform:uppercase}
.divider span{flex:1;border-top:1px solid #E7D8BC}

.link-back{border:none;background:none;color:#7A7062;font-family:inherit;font-size:12.5px;font-weight:600;
  letter-spacing:.06em;text-transform:uppercase;cursor:pointer;padding:0 0 12px}

.card{background:#FDFBF6;border:1px solid #E7D8BC;border-radius:16px;margin-bottom:16px}
.card-head{padding:15px 16px 13px;border-bottom:1px solid #E7D8BC;display:flex;align-items:flex-start;gap:12px}
.card-head h2{font-family:'Instrument Serif',serif;font-weight:400;font-size:20px;line-height:1.08;margin:0}
.card-emoji{font-size:22px;line-height:1;width:26px;flex:none}
.card-body{padding:14px 16px}

.ing-grid{list-style:none;margin:0 0 4px;padding:0;display:grid;grid-template-columns:1fr auto 62px;
  column-gap:10px;row-gap:7px;align-items:center}
.ing-grid li{display:contents}
.ing-name{font-size:14px;font-weight:500;display:flex;gap:8px;align-items:baseline;color:#292019}
.dot{width:5px;height:5px;border-radius:50%;flex:none;transform:translateY(-2px)}
.ing-num{font-family:'JetBrains Mono',monospace;font-size:13px;font-weight:600;color:#292019;text-align:right}
.ing-unit{font-family:'JetBrains Mono',monospace;font-size:11.5px;font-weight:600;color:#8A5A34;background:#F1E9E0;
  border-radius:7px;padding:3px 6px;white-space:nowrap;text-align:center}
.avail-label{font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.06em;margin:0 0 6px}

.utn-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:7px}
.utn-list li{font-size:14px;font-weight:500;display:flex;gap:8px;align-items:baseline;color:#292019}

.section-title{font-family:'Instrument Serif',serif;font-style:italic;font-weight:400;font-size:15px;margin:0 0 8px;
  color:#7A7062}
.section-time{font-family:'DM Sans',sans-serif;font-style:normal;font-size:12px;font-weight:600;background:#F3EAD9;
  padding:2px 8px;border-radius:7px;margin-left:4px}

.steps-list{display:flex;flex-direction:column;gap:12px;margin-bottom:16px}
.step-card{background:#FDFBF6;border:1px solid #E7D8BC;border-radius:16px}
.step-card-head{padding:15px 16px 13px;border-bottom:1px solid #E7D8BC;display:flex;align-items:flex-start;gap:12px}
.step-circle{width:24px;height:24px;border-radius:50%;background:#EDE7DA;border:2px solid #E2DBCD;display:flex;
  align-items:center;justify-content:center;font-size:12px;font-weight:700;color:#7A7062;flex:none}
.step-title{font-family:'Instrument Serif',serif;font-weight:400;font-size:20px;line-height:1.08;margin:0;flex:1;
  min-width:0;color:#292019}
.step-card.done .step-title{color:#7A7062}
.step-card-body{padding:14px 16px}
.step-text{font-size:15px;font-weight:500;line-height:1.5;margin:0;color:#5C6353}
.step-text.done{color:#A99A82;text-decoration:line-through;text-decoration-color:#C6BCA9}
.step-time{display:inline-flex;margin-top:8px;font-size:12px;font-weight:600;background:#F3EAD9;padding:2px 8px;
  border-radius:7px}
.step-note{margin:10px 0 0;font-size:13px;line-height:1.5;color:#7A7062;font-style:italic;border-left:2px solid #E7D8BC;
  padding-left:10px}
`;
