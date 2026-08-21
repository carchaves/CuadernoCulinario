import { useState } from "react";
import { useAppData } from "./core/useAppData";
import { Menu } from "./screens/Menu";
import { Despensa } from "./screens/Despensa";
import { ListaDeCompra } from "./screens/ListaDeCompra";
import { Recetas } from "./screens/Recetas";

type Screen = "menu" | "despensa" | "recetas" | "compra";

const shellStyle = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column" as const,
  alignItems: "center",
  justifyContent: "center",
  gap: 16,
  padding: 24,
  boxSizing: "border-box" as const,
  background: "#161311",
  color: "#B7AE9F",
  fontFamily: "system-ui, sans-serif",
  fontSize: 13,
  textAlign: "center" as const,
};

const loadingScreen = <div style={shellStyle}>Cargando…</div>;

function App() {
  const [screen, setScreen] = useState<Screen>("menu");
  const { state, loading, error, reload } = useAppData();

  if (loading) return loadingScreen;

  if (error || !state) {
    return (
      <div style={shellStyle}>
        <div style={{ color: "#EDE6D9", fontSize: 15 }}>No se pudieron cargar los datos</div>
        <div style={{ maxWidth: 420, lineHeight: 1.5 }}>{error ?? "Respuesta vacía del repositorio."}</div>
        <button
          type="button"
          onClick={reload}
          style={{
            marginTop: 4,
            background: "#1F1A16",
            border: "1.5px solid #8C8377",
            borderRadius: 10,
            color: "#EDE6D9",
            fontFamily: "inherit",
            fontSize: 13,
            fontWeight: 600,
            padding: "10px 18px",
            cursor: "pointer",
          }}
        >
          Reintentar
        </button>
      </div>
    );
  }

  if (screen === "despensa") return <Despensa state={state} onBack={() => setScreen("menu")} />;
  if (screen === "recetas") return <Recetas state={state} onBack={() => setScreen("menu")} />;
  if (screen === "compra") return <ListaDeCompra state={state} onBack={() => setScreen("menu")} />;
  return <Menu onGo={(key) => setScreen(key)} />;
}

export default App;
