// Bootstrap de un solo uso: aplica la migración inicial directamente vía `pg`
// (forzando IPv4) porque el motor de esquema de Prisma es un binario aparte que no
// respeta el resolver DNS de Node, y en esta red el AAAA de Neon no es alcanzable.
// Después de esto, `prisma migrate deploy` en producción (Fly.io) no debería
// necesitar este workaround.
import "dotenv/config";
import { readFileSync, readdirSync } from "node:fs";
import { createHash } from "node:crypto";
import dns from "node:dns";
import { Pool } from "pg";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const migrationsDir = path.join(__dirname, "../prisma/migrations");
const [migrationName] = readdirSync(migrationsDir).filter((f) => !f.startsWith("."));
const sqlPath = path.join(migrationsDir, migrationName, "migration.sql");
const sql = readFileSync(sqlPath, "utf8");
const checksum = createHash("sha256").update(sql).digest("hex");

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  lookup: (hostname, options, callback) => dns.lookup(hostname, { family: 4 }, callback),
});

const client = await pool.connect();
try {
  await client.query("BEGIN");
  await client.query(sql);
  await client.query(
    `CREATE TABLE IF NOT EXISTS "_prisma_migrations" (
      id VARCHAR(36) PRIMARY KEY,
      checksum VARCHAR(64) NOT NULL,
      finished_at TIMESTAMPTZ,
      migration_name VARCHAR(255) NOT NULL,
      logs TEXT,
      rolled_back_at TIMESTAMPTZ,
      started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      applied_steps_count INTEGER NOT NULL DEFAULT 0
    )`
  );
  await client.query(
    `INSERT INTO "_prisma_migrations" (id, checksum, finished_at, migration_name, started_at, applied_steps_count)
     VALUES (gen_random_uuid()::text, $1, now(), $2, now(), 1)`,
    [checksum, migrationName]
  );
  await client.query("COMMIT");
  console.log(`Migración "${migrationName}" aplicada y registrada.`);
} catch (e) {
  await client.query("ROLLBACK");
  throw e;
} finally {
  client.release();
  await pool.end();
}
