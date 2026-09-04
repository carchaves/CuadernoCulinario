import { useState } from "react";
import { validateToken, OWNER, REPO } from "../core/github";
import { clearToken, hasToken, setToken } from "../core/githubToken";

/**
 * Ajustes: el token de GitHub con el que la web commitea los archivos de `data/`. No es un
 * login — no hay usuarios ni sesión, solo esta credencial guardada en este navegador.
 */
export function Settings({ onSaved }: { onSaved: () => void }) {
  const [pat, setPat] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);
  const [had, setHad] = useState(hasToken);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const token = pat.trim();
    if (!token || loading) return;
    setLoading(true);
    setError(null);
    setSaved(false);
    const ok = await validateToken(token);
    if (ok) {
      setToken(token);
      setPat("");
      setHad(true);
      setSaved(true);
      onSaved();
    } else {
      setError(`El token no sirve para ${OWNER}/${REPO}. Revisalo (y tu conexión) e intentá de nuevo.`);
    }
    setLoading(false);
  }

  return (
    <div className="settings-root">
      <style>{css}</style>
      <form className="card" onSubmit={submit}>
        <div className="mark" aria-hidden>
          ◧
        </div>
        <h1>Ajustes</h1>
        <p>
          Tus datos viven en el repo <code>{OWNER}/{REPO}</code>. Para poder guardar cambios, la web
          necesita un token de GitHub.
        </p>
        <p>
          En GitHub: Settings → Developer settings → Personal access tokens → Fine-grained tokens →
          Generate new token. Elegí solo el repositorio <code>{REPO}</code> y dale el permiso
          «Contents: Read and write». Copiá el token y pegalo acá abajo.
        </p>
        {had && <p className="note">Ya hay un token guardado en este navegador. Pegá uno nuevo para reemplazarlo.</p>}
        <label className="field">
          <span>Token de GitHub</span>
          <input
            type="password"
            autoComplete="off"
            value={pat}
            onChange={(e) => setPat(e.target.value)}
            placeholder="github_pat_…"
          />
        </label>
        {error && <p className="error">{error}</p>}
        {saved && <p className="ok">Token guardado. Ya podés editar.</p>}
        <button type="submit" className="primary" disabled={loading || !pat.trim()}>
          {loading ? "Validando…" : "Guardar"}
        </button>
        {had && (
          <button
            type="button"
            className="ghost"
            onClick={() => {
              clearToken();
              setHad(false);
              setSaved(false);
              onSaved();
            }}
          >
            Borrar el token de este navegador
          </button>
        )}
      </form>
    </div>
  );
}

const css = `
.settings-root{min-height:100%;background:#161311;color:#B7AE9F;padding:48px 24px;
  font-family:ui-sans-serif,system-ui,sans-serif;display:flex;justify-content:center;align-items:flex-start}
.settings-root *{box-sizing:border-box}
.settings-root .card{width:100%;max-width:560px;background:#1F1A16;border:1.5px solid #8C8377;border-radius:14px;
  padding:28px;display:flex;flex-direction:column;gap:12px}
.settings-root .mark{font-size:26px;color:#EDE6D9;text-align:center}
.settings-root h1{font-family:'Fraunces',Georgia,serif;font-weight:600;font-size:24px;color:#EDE6D9;margin:0;text-align:center}
.settings-root p{margin:0;font-size:13px;line-height:1.5}
.settings-root code{font-family:ui-monospace,monospace;font-size:12px;color:#EDE6D9}
.settings-root .note{font-size:12.5px;color:#EDE6D9}
.settings-root .field{display:flex;flex-direction:column;gap:6px;margin-top:6px}
.settings-root .field span{font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#B7AE9F}
.settings-root input{background:#161311;border:1.5px solid #8C8377;border-radius:10px;color:#EDE6D9;
  font-family:inherit;font-size:14px;padding:10px 12px;outline:none}
.settings-root input:focus{border-color:#EDE6D9}
.settings-root .error{color:#E39182;font-size:12.5px}
.settings-root .ok{color:#9BC08A;font-size:12.5px}
.settings-root .primary{background:#EDE6D9;color:#161311;border:none;border-radius:10px;padding:11px 16px;
  font-family:inherit;font-size:14px;font-weight:600;cursor:pointer}
.settings-root .primary:disabled{opacity:.5;cursor:default}
.settings-root .ghost{background:none;border:none;color:#B7AE9F;font-family:inherit;font-size:12.5px;
  cursor:pointer;text-decoration:underline;padding:4px}
`;
