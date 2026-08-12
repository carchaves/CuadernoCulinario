import { useEffect, useState } from "react";
import { Capacitor } from "@capacitor/core";
import { useAppState } from "./core/store";
import { Menu } from "./screens/Menu";
import { Despensa } from "./screens/Despensa";
import { ListaDeCompra } from "./screens/ListaDeCompra";
import { Recetas } from "./screens/Recetas";

type Screen = "menu" | "despensa" | "recetas" | "compra";

function App() {
  const [screen, setScreen] = useState<Screen>("menu");
  const { state, loading, reload, actions } = useAppState();

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    let remove: (() => void) | undefined;
    import("@capacitor/app").then(({ App: CapApp }) => {
      CapApp.addListener("resume", () => reload()).then((h) => {
        remove = () => h.remove();
      });
    });
    return () => remove?.();
  }, [reload]);

  if (loading || !state) {
    return (
      <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#161311", color: "#B7AE9F", fontFamily: "system-ui, sans-serif", fontSize: 13 }}>
        Cargando…
      </div>
    );
  }

  if (screen === "despensa") return <Despensa state={state} actions={actions} onBack={() => setScreen("menu")} />;
  if (screen === "recetas") return <Recetas state={state} actions={actions} onBack={() => setScreen("menu")} />;
  if (screen === "compra") return <ListaDeCompra state={state} actions={actions} onBack={() => setScreen("menu")} />;
  return <Menu onGo={(key) => setScreen(key)} />;
}

export default App;
