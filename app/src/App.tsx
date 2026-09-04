import { useState } from "react";
import { useAppState } from "./core/useAppState";
import { Despensa } from "./screens/Despensa";
import { ListaDeCompra } from "./screens/ListaDeCompra";
import { Recetas } from "./screens/Recetas";
import { Settings } from "./screens/Settings";

type Section = "despensa" | "recetas" | "compra" | "ajustes";

const COLORS = {
  bg: "#161311",
  tile: "#1F1A16",
  border: "#8C8377",
  ink: "#EDE6D9",
  inkSoft: "#B7AE9F",
};

function PotIcon() {
  return (
    <svg viewBox="0 0 52 52" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M15 18 h22 v19 a3 3 0 0 1 -3 3 H18 a3 3 0 0 1 -3 -3 Z" />
      <path d="M13 16 h26" />
      <path d="M26 12 v4" />
      <path d="M15 24 a3 3 0 0 1 -4 0" />
      <path d="M37 24 a3 3 0 0 0 4 0" />
    </svg>
  );
}

function BookIcon() {
  return (
    <svg viewBox="0 0 52 52" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M26 15 C22 12 15 12 11 13 v25 c4 -1 11 -1 15 2" />
      <path d="M26 15 C30 12 37 12 41 13 v25 c-4 -1 -11 -1 -15 2" />
      <path d="M26 15 v27" />
    </svg>
  );
}

function CartIcon() {
  return (
    <svg viewBox="0 0 52 52" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 11 h4 l3 20 h20" />
      <path d="M18 16 h24 l-3 12 H20" />
      <circle cx="22" cy="38" r="2.6" />
      <circle cx="37" cy="38" r="2.6" />
    </svg>
  );
}

function GearIcon() {
  return (
    <svg viewBox="0 0 52 52" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="26" cy="26" r="6" />
      <path d="M26 10v5M26 37v5M10 26h5M37 26h5M15 15l3.5 3.5M33.5 33.5 37 37M37 15l-3.5 3.5M18.5 33.5 15 37" />
    </svg>
  );
}

const NAV = [
  { key: "despensa", label: "Despensa", Icon: PotIcon },
  { key: "recetas", label: "Recetas", Icon: BookIcon },
  { key: "compra", label: "Lista de Compra", Icon: CartIcon },
] as const;

function App() {
  const [section, setSection] = useState<Section>("despensa");
  const store = useAppState();
  const { state, loading, error, readOnly, syncing, pending } = store;

  return (
    <div className="app-shell">
      <style>{css}</style>
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark" aria-hidden>
            ◧
          </span>
          <span className="brand-name">Cocina</span>
        </div>
        <nav>
          {NAV.map(({ key, label, Icon }) => (
            <button
              key={key}
              type="button"
              className={"nav-item" + (section === key ? " on" : "")}
              onClick={() => setSection(key)}
            >
              <Icon />
              <span>{label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className={"sync " + (syncing ? "sync-on" : pending ? "sync-pending" : "sync-idle")}>
            {syncing ? "Sincronizando…" : pending ? "Cambios sin guardar" : readOnly ? "Solo lectura" : "Al día"}
          </div>
          <button
            type="button"
            className={"nav-item" + (section === "ajustes" ? " on" : "")}
            onClick={() => setSection("ajustes")}
          >
            <GearIcon />
            <span>Ajustes</span>
          </button>
          <button type="button" className="nav-item" onClick={store.sync} disabled={syncing}>
            <span className="nav-glyph" aria-hidden>
              ⟳
            </span>
            <span>Sincronizar</span>
          </button>
        </div>
      </aside>

      <main className="content">
        {readOnly && section !== "ajustes" && (
          <div className="banner">
            <span>Modo solo lectura: configurá un token de GitHub para poder editar.</span>
            <button type="button" onClick={() => setSection("ajustes")}>
              Ir a Ajustes
            </button>
          </div>
        )}
        {error && section !== "ajustes" && <div className="banner banner-err">{error}</div>}

        {section === "ajustes" ? (
          <Settings onSaved={store.refreshToken} />
        ) : loading && !state ? (
          <div className="placeholder">Cargando…</div>
        ) : !state ? (
          <div className="placeholder">
            <p>No se pudieron cargar los datos.</p>
            <button type="button" onClick={store.sync}>
              Reintentar
            </button>
          </div>
        ) : section === "despensa" ? (
          <Despensa store={store} state={state} />
        ) : section === "recetas" ? (
          <Recetas store={store} state={state} />
        ) : (
          <ListaDeCompra store={store} state={state} />
        )}
      </main>
    </div>
  );
}

export default App;

const css = `
.app-shell{position:fixed;inset:0;display:flex;min-width:960px;overflow:hidden;
  font-family:ui-sans-serif,system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;background:${COLORS.bg}}
.app-shell *{box-sizing:border-box}
.sidebar{flex:0 0 232px;background:${COLORS.bg};border-right:1.5px solid #2C2622;color:${COLORS.inkSoft};
  display:flex;flex-direction:column;padding:20px 14px;gap:6px}
.brand{display:flex;align-items:center;gap:10px;padding:4px 10px 20px;color:${COLORS.ink}}
.brand-mark{font-size:22px}
.brand-name{font-family:'Fraunces',Georgia,serif;font-size:21px;letter-spacing:.02em}
.sidebar nav{display:flex;flex-direction:column;gap:4px}
.nav-item{display:flex;align-items:center;gap:11px;width:100%;background:none;border:1.5px solid transparent;
  border-radius:11px;color:${COLORS.inkSoft};font-family:inherit;font-size:14px;font-weight:500;
  padding:10px 12px;cursor:pointer;text-align:left;transition:.15s}
.nav-item:hover{background:${COLORS.tile};color:${COLORS.ink}}
.nav-item:disabled{opacity:.5;cursor:default}
.nav-item.on{background:${COLORS.tile};border-color:${COLORS.border};color:${COLORS.ink}}
.nav-glyph{width:22px;text-align:center;font-size:17px}
.sidebar-foot{margin-top:auto;display:flex;flex-direction:column;gap:4px;padding-top:14px;border-top:1px solid #2C2622}
.sync{font-size:11px;letter-spacing:.1em;text-transform:uppercase;padding:6px 12px 10px}
.sync-idle{color:#7C7466}
.sync-on{color:#C7BFAF}
.sync-pending{color:#D6A85F}

.content{flex:1 1 auto;min-width:0;overflow-y:auto;position:relative;background:#FBF9F3}
.banner{display:flex;align-items:center;justify-content:space-between;gap:16px;background:#2A231C;color:#EDE6D9;
  padding:10px 24px;font-size:13px}
.banner button{background:#EDE6D9;color:#161311;border:none;border-radius:8px;padding:6px 12px;
  font-family:inherit;font-size:12.5px;font-weight:600;cursor:pointer}
.banner-err{background:#4A2A22;color:#F0CFC5}
.placeholder{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;
  height:60vh;color:#726C5C;font-size:14px}
.placeholder button{background:#24211A;color:#FBF9F3;border:none;border-radius:9px;padding:9px 16px;
  font-family:inherit;font-size:13px;font-weight:600;cursor:pointer}
`;
