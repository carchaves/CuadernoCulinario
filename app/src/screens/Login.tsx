import { useState } from "react";
import { tokenStore } from "../storage/tokenStore";

const API_BASE = import.meta.env.VITE_API_URL || "";

export function Login({ onSuccess }: { onSuccess: () => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    if (!email.trim() || !password) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });
      if (!res.ok) {
        setError("Email o contraseña incorrectos.");
        return;
      }
      const { accessToken, refreshToken } = await res.json();
      tokenStore.set(accessToken, refreshToken);
      onSuccess();
    } catch {
      setError("No se pudo conectar con el servidor.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-root">
      <style>{css}</style>
      <div className="login-card">
        <div className="login-mark" aria-hidden>
          ◧
        </div>
        <h1>Cocina App</h1>
        <p className="login-sub">Iniciá sesión para ver tu despensa, recetas y lista de compra.</p>
        <input
          className="login-input"
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          autoFocus
        />
        <input
          className="login-input"
          type="password"
          placeholder="Contraseña"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
        />
        {error && <p className="login-error">{error}</p>}
        <button className="login-btn" onClick={submit} disabled={loading}>
          {loading ? "Entrando…" : "Entrar"}
        </button>
      </div>
    </div>
  );
}

const css = `
.login-root{
  min-height:100vh;background:#161311;display:flex;align-items:center;justify-content:center;
  padding:24px;box-sizing:border-box;font-family:'Inter',ui-sans-serif,system-ui,sans-serif;
}
.login-root *{box-sizing:border-box}
.login-card{width:100%;max-width:340px;background:#1F1A16;border:1.5px solid #8C8377;border-radius:14px;
  padding:32px 28px;display:flex;flex-direction:column;gap:12px;align-items:stretch}
.login-mark{font-size:28px;color:#EDE6D9;text-align:center}
.login-card h1{font-family:'Fraunces',serif;font-weight:640;font-size:24px;color:#EDE6D9;margin:0;text-align:center}
.login-sub{color:#B7AE9F;font-size:13px;text-align:center;margin:0 0 8px;line-height:1.4}
.login-input{background:#161311;border:1px solid #8C8377;border-radius:9px;padding:11px 13px;
  color:#EDE6D9;font-family:inherit;font-size:14px;outline:none}
.login-input:focus{border-color:#EDE6D9}
.login-error{color:#E68A3C;font-size:12.5px;margin:0;text-align:center}
.login-btn{background:#EDE6D9;color:#161311;border:none;border-radius:9px;padding:12px;
  font-family:inherit;font-size:14px;font-weight:600;cursor:pointer;margin-top:6px}
.login-btn:disabled{opacity:.6;cursor:default}
`;
