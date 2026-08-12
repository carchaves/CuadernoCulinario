import { Filesystem, Directory, Encoding } from "@capacitor/filesystem";
import type { AppState } from "../core/types";
import type { Storage } from "./types";

const FILE = "cocina-data.json";

export const capacitorStorageAdapter: Storage = {
  async load() {
    try {
      const res = await Filesystem.readFile({
        path: FILE,
        directory: Directory.Data,
        encoding: Encoding.UTF8,
      });
      const raw = typeof res.data === "string" ? res.data : await (res.data as Blob).text();
      return JSON.parse(raw) as AppState;
    } catch {
      return null;
    }
  },
  async save(state) {
    await Filesystem.writeFile({
      path: FILE,
      directory: Directory.Data,
      encoding: Encoding.UTF8,
      data: JSON.stringify(state),
      recursive: true,
    });
  },
};
