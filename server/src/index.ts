import "dotenv/config";
import express from "express";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { authRouter } from "./routes/auth.js";
import { stateRouter } from "./routes/state.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const APP_DIST = path.resolve(__dirname, "../../app/dist");
const PORT = process.env.PORT ? Number(process.env.PORT) : 8080;

const app = express();
app.use(express.json({ limit: "5mb" }));

app.get("/health", (_req, res) => res.json({ ok: true }));
app.use("/auth", authRouter);
app.use("/api/state", stateRouter);

app.use(express.static(APP_DIST));
app.get(/.*/, (_req, res) => {
  res.sendFile(path.join(APP_DIST, "index.html"));
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Cocina App server escuchando en :${PORT}`);
});
