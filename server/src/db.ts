import { PrismaClient } from "@prisma/client";
import { PrismaPg } from "@prisma/adapter-pg";
import { Pool, type PoolConfig } from "pg";
import dns from "node:dns";
import type { LookupFunction } from "node:net";

// Fuerza resolución IPv4: en algunas redes el AAAA de Neon resuelve pero la ruta IPv6
// no es alcanzable, y el motor por defecto intenta IPv6 primero y falla.
type PoolConfigWithLookup = PoolConfig & { lookup?: LookupFunction };

const lookupIpv4First: LookupFunction = (hostname, options, callback) =>
  dns.lookup(hostname, { family: 4 }, callback as (err: NodeJS.ErrnoException | null, address: string, family: number) => void);

const poolConfig: PoolConfigWithLookup = {
  connectionString: process.env.DATABASE_URL,
  lookup: lookupIpv4First,
};

const pool = new Pool(poolConfig);

export const prisma = new PrismaClient({ adapter: new PrismaPg(pool) });
