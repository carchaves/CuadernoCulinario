import "dotenv/config";
import { defineConfig } from "prisma/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { Pool } from "pg";
import dns from "node:dns";

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  lookup: (hostname: string, options: unknown, callback: (...args: any[]) => void) =>
    dns.lookup(hostname, { family: 4 }, callback),
});

export default defineConfig({
  schema: "prisma/schema.prisma",
  datasource: {
    url: process.env.DATABASE_URL,
  },
  migrations: {
    adapter: async () => new PrismaPg(pool),
  },
});
