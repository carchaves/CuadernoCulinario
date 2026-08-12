import { useEffect, useState } from "react";
import { useAppState } from "./core/store";
import { Menu } from "./screens/Menu";
import { Despensa } from "./screens/Despensa";
import { ListaDeCompra } from "./screens/ListaDeCompra";
import { Recetas } from "./screens/Recetas";
import { Login } from "./screens/Login";
import { tokenStore } from "./storage/tokenStore";

type Screen = "menu" | "despensa" | "recetas" | "compra";

const API_BASE = import.meta.env.VITE_API_URL || "";

const loadingScreen = (
  <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#161311", color: "#B7AE9F", fontFamily: "system-ui, sans-serif", fontSize: 13 }}>
    Cargando…
  </div>
);

function AuthenticatedApp() {
  const [screen, setScreen] = useState<Screen>("menu");
  const { state, loading, actions } = useAppState();

  if (loading || !state) return loadingScreen;

  if (screen === "despensa") return <Despensa state={state} actions={actions} onBack={() => setScreen("menu")} />;
  if (screen === "recetas") return <Recetas state={state} actions={actions} onBack={() => setScreen("menu")} />;
  if (screen === "compra") return <ListaDeCompra state={state} actions={actions} onBack={() => setScreen("menu")} />;
  return <Menu onGo={(key) => setScreen(key)} />;
}

function App() {
  const [checkingServer, setCheckingServer] = useState(true);
  const [requiresLogin, setRequiresLogin] = useState(false);
  const [authed, setAuthed] = useState(tokenStore.hasSession());

  useEffect(() => tokenStore.subscribe(() => setAuthed(tokenStore.hasSession())), []);

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(`${API_BASE}/health`);
        setRequiresLogin(res.ok);
      } catch {
        setRequiresLogin(false);
      } finally {
        setCheckingServer(false);
      }
    })();
  }, []);

  if (checkingServer) return loadingScreen;
  if (requiresLogin && !authed) return <Login onSuccess={() => setAuthed(true)} />;
  return <AuthenticatedApp />;
}

export default App;
