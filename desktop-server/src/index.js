import express from "express";
import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { listDevices, pullData, pushData } from "./adbBridge.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const shared = JSON.parse(readFileSync(path.join(ROOT, "shared.config.json"), "utf8"));
const APP_ID = shared.appId;
const PORT = process.env.PORT ? Number(process.env.PORT) : shared.desktopPort;
const APP_DIST = path.join(ROOT, "app/dist");
const DATA_DIR = path.join(__dirname, "../data");
if (!existsSync(DATA_DIR)) mkdirSync(DATA_DIR, { recursive: true });

let currentSerial = null;
let connected = false;
let lastSync = null;
let cachedData = null;
let pushTimer = null;

function cacheFile(serial) {
  return path.join(DATA_DIR, `${serial}.json`);
}

function loadDiskCache(serial) {
  const f = cacheFile(serial);
  if (!existsSync(f)) return null;
  try {
    return JSON.parse(readFileSync(f, "utf8"));
  } catch {
    return null;
  }
}

function saveDiskCache(serial, data) {
  writeFileSync(cacheFile(serial), JSON.stringify(data), "utf8");
}

async function handleDeviceChange(serial) {
  if (serial === currentSerial) return;
  currentSerial = serial;
  if (serial) {
    connected = true;
    console.log(`[adb] dispositivo conectado: ${serial} — sincronizando desde el celular…`);
    try {
      const fromPhone = await pullData(serial, APP_ID);
      cachedData = fromPhone ?? loadDiskCache(serial) ?? null;
      if (cachedData) saveDiskCache(serial, cachedData);
      lastSync = Date.now();
      console.log(`[adb] datos cargados desde el celular (${fromPhone ? "archivo en el dispositivo" : "sin archivo aún, usando cache local"}).`);
    } catch (err) {
      console.error("[adb] error al leer datos del celular:", err.message);
    }
  } else {
    connected = false;
    console.log("[adb] dispositivo desconectado. Los últimos cambios enviados quedan en el celular.");
  }
}

async function pollLoop() {
  try {
    const devices = await listDevices();
    await handleDeviceChange(devices[0] || null);
  } catch (err) {
    console.error("[adb] error listando dispositivos:", err.message);
  }
  setTimeout(pollLoop, 1500);
}
pollLoop();

function schedulePush() {
  if (pushTimer) clearTimeout(pushTimer);
  pushTimer = setTimeout(async () => {
    if (!connected || !currentSerial || !cachedData) return;
    try {
      await pushData(currentSerial, APP_ID, cachedData);
      lastSync = Date.now();
      console.log("[adb] cambios enviados al celular.");
    } catch (err) {
      console.error("[adb] error al enviar datos al celular:", err.message);
    }
  }, 600);
}

const app = express();
app.use(express.json({ limit: "5mb" }));

app.get("/api/status", (_req, res) => {
  res.json({ connected, serial: currentSerial, lastSync, appId: APP_ID });
});

app.get("/api/data", (_req, res) => {
  res.json(cachedData || {});
});

app.put("/api/data", (req, res) => {
  cachedData = req.body;
  if (currentSerial) saveDiskCache(currentSerial, cachedData);
  schedulePush();
  res.json({ ok: true });
});

app.use(express.static(APP_DIST));
app.get(/.*/, (_req, res) => {
  res.sendFile(path.join(APP_DIST, "index.html"));
});

app.listen(PORT, () => {
  console.log(`Cocina App (escritorio) en http://localhost:${PORT}`);
  console.log(`Esperando al celular por USB (appId: ${APP_ID})…`);
});
