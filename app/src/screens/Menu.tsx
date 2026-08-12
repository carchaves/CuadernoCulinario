import type { CSSProperties } from "react";

const COLORS = {
  bg: "#161311",
  tile: "#1F1A16",
  border: "#8C8377",
  ink: "#EDE6D9",
  inkSoft: "#B7AE9F",
};

const wrap: CSSProperties = {
  minHeight: "100vh",
  background: COLORS.bg,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: 24,
  boxSizing: "border-box",
  fontFamily: "ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, sans-serif",
};

const tileStyle: CSSProperties = {
  background: COLORS.tile,
  border: `1.5px solid ${COLORS.border}`,
  borderRadius: 14,
  aspectRatio: "1 / 1",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: 14,
  padding: 12,
  cursor: "pointer",
  color: COLORS.ink,
  outline: "none",
};

function PotIcon() {
  return (
    <svg viewBox="0 0 52 52" width="100%" height="100%" fill="none" stroke={COLORS.ink} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
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
    <svg viewBox="0 0 52 52" width="100%" height="100%" fill="none" stroke={COLORS.ink} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M26 15 C22 12 15 12 11 13 v25 c4 -1 11 -1 15 2" />
      <path d="M26 15 C30 12 37 12 41 13 v25 c-4 -1 -11 -1 -15 2" />
      <path d="M26 15 v27" />
      <path d="M15 20 h6 M15 25 h6" stroke={COLORS.inkSoft} strokeWidth="1.6" />
      <path d="M31 20 h6 M31 25 h6" stroke={COLORS.inkSoft} strokeWidth="1.6" />
    </svg>
  );
}

function CartIcon() {
  return (
    <svg viewBox="0 0 52 52" width="100%" height="100%" fill="none" stroke={COLORS.ink} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 11 h4 l3 20 h20" />
      <path d="M18 16 h24 l-3 12 H20" />
      <circle cx="22" cy="38" r="2.6" />
      <circle cx="37" cy="38" r="2.6" />
    </svg>
  );
}

const ITEMS = [
  { key: "despensa", title: "Despensa", Icon: PotIcon },
  { key: "recetas", title: "Recetas", Icon: BookIcon },
  { key: "compra", title: "Lista de Compra", Icon: CartIcon },
] as const;

export function Menu({ onGo }: { onGo: (key: "despensa" | "recetas" | "compra") => void }) {
  return (
    <div style={wrap}>
      <style>{`
        .menu-tile:hover { background: #26201B; border-color: ${COLORS.ink}; transform: translateY(-3px); }
        .menu-tile:focus-visible { border-color: ${COLORS.ink}; box-shadow: 0 0 0 3px rgba(237,230,217,.28); }
        .menu-tile { transition: border-color .18s ease, background-color .18s ease, transform .18s ease; }
        .menu-label { font-size: clamp(11px, 2.4vw, 15px); letter-spacing: .14em; text-transform: uppercase; text-align: center; line-height: 1.25; color: ${COLORS.ink}; }
        @media (prefers-reduced-motion: reduce) { .menu-tile { transition: none; } .menu-tile:hover { transform: none; } }
      `}</style>
      <div style={{ width: "100%", maxWidth: 720 }}>
        <div style={{ color: COLORS.inkSoft, textAlign: "center", fontSize: 12, letterSpacing: ".18em", textTransform: "uppercase", marginBottom: 18 }}>
          Tu cocina, en un solo lugar
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 18 }}>
          {ITEMS.map(({ key, title, Icon }) => (
            <button key={key} type="button" className="menu-tile" style={tileStyle} aria-label={title} onClick={() => onGo(key)}>
              <span style={{ width: "46%", maxWidth: 84 }}>
                <Icon />
              </span>
              <span className="menu-label">{title}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
