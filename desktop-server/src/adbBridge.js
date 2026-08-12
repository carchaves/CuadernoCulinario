import { spawn } from "node:child_process";
import { existsSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";

const FALLBACK_ADB_PATHS = [
  path.join(homedir(), "development/android-sdk/platform-tools/adb"),
  process.env.ANDROID_HOME ? path.join(process.env.ANDROID_HOME, "platform-tools/adb") : null,
  process.env.ANDROID_SDK_ROOT ? path.join(process.env.ANDROID_SDK_ROOT, "platform-tools/adb") : null,
].filter(Boolean);

function resolveAdbBin() {
  for (const p of FALLBACK_ADB_PATHS) {
    if (existsSync(p)) return p;
  }
  return "adb";
}

const ADB_BIN = resolveAdbBin();

function run(args, input) {
  return new Promise((resolve, reject) => {
    const child = spawn(ADB_BIN, args, { stdio: ["pipe", "pipe", "pipe"] });
    const chunks = [];
    const errChunks = [];
    child.stdout.on("data", (c) => chunks.push(c));
    child.stderr.on("data", (c) => errChunks.push(c));
    child.on("error", reject);
    child.on("close", (code) => {
      const stdout = Buffer.concat(chunks);
      const stderr = Buffer.concat(errChunks).toString("utf8");
      if (code !== 0) {
        reject(new Error(`adb ${args.join(" ")} exited ${code}: ${stderr}`));
        return;
      }
      resolve(stdout);
    });
    if (input != null) {
      child.stdin.write(input);
      child.stdin.end();
    } else {
      child.stdin.end();
    }
  });
}

export async function listDevices() {
  const out = (await run(["devices"])).toString("utf8");
  return out
    .split("\n")
    .slice(1)
    .map((l) => l.trim())
    .filter((l) => l.endsWith("\tdevice"))
    .map((l) => l.split("\t")[0]);
}

export async function pullData(serial, appId) {
  const out = await run(["-s", serial, "exec-out", "run-as", appId, "cat", "files/cocina-data.json"]);
  const text = out.toString("utf8");
  if (!text.trim() || text.startsWith("cat:") || text.includes("No such file")) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export async function pushData(serial, appId, data) {
  const json = JSON.stringify(data);
  await run(["-s", serial, "exec-in", "run-as", appId, "sh", "-c", "cat > files/cocina-data.json"], json);
}
