import { Router } from "express";
import { z } from "zod";
import { prisma } from "../db.js";
import { requireAuth } from "../middleware/requireAuth.js";

export const stateRouter = Router();
stateRouter.use(requireAuth);

stateRouter.get("/", async (req, res) => {
  const row = await prisma.appStateRow.findUnique({ where: { userId: req.userId! } });
  if (!row) {
    res.json({ data: null, revision: 0, updatedAt: null });
    return;
  }
  res.json({ data: row.data, revision: row.revision, updatedAt: row.updatedAt });
});

const putSchema = z.object({
  data: z.record(z.string(), z.unknown()),
  baseRevision: z.number().int().nonnegative(),
});

stateRouter.put("/", async (req, res) => {
  const parsed = putSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "Body inválido: se espera { data, baseRevision }" });
    return;
  }
  const { data, baseRevision } = parsed.data;
  const userId = req.userId!;

  const existing = await prisma.appStateRow.findUnique({ where: { userId } });

  if (!existing) {
    if (baseRevision !== 0) {
      res.status(409).json({ error: "conflicto", data: null, revision: 0 });
      return;
    }
    const created = await prisma.appStateRow.create({
      data: { userId, data: data as object, revision: 1 },
    });
    res.json({ revision: created.revision, updatedAt: created.updatedAt });
    return;
  }

  const updated = await prisma.appStateRow.updateMany({
    where: { userId, revision: baseRevision },
    data: { data: data as object, revision: { increment: 1 } },
  });

  if (updated.count === 0) {
    const current = await prisma.appStateRow.findUnique({ where: { userId } });
    res.status(409).json({ error: "conflicto", data: current!.data, revision: current!.revision });
    return;
  }

  const fresh = await prisma.appStateRow.findUnique({ where: { userId } });
  res.json({ revision: fresh!.revision, updatedAt: fresh!.updatedAt });
});
