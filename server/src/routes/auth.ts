import { Router } from "express";
import { z } from "zod";
import { prisma } from "../db.js";
import { signAccessToken, signRefreshToken, verifyPassword, verifyToken } from "../auth.js";

export const authRouter = Router();

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

authRouter.post("/login", async (req, res) => {
  const parsed = loginSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "email y password son requeridos" });
    return;
  }
  const { email, password } = parsed.data;
  const user = await prisma.user.findUnique({ where: { email } });
  if (!user || !(await verifyPassword(password, user.passwordHash))) {
    res.status(401).json({ error: "Credenciales inválidas" });
    return;
  }
  res.json({
    accessToken: signAccessToken(user.id),
    refreshToken: signRefreshToken(user.id),
  });
});

const refreshSchema = z.object({ refreshToken: z.string().min(1) });

authRouter.post("/refresh", (req, res) => {
  const parsed = refreshSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "refreshToken es requerido" });
    return;
  }
  try {
    const payload = verifyToken(parsed.data.refreshToken, "refresh");
    res.json({ accessToken: signAccessToken(payload.sub) });
  } catch {
    res.status(401).json({ error: "Refresh token inválido o vencido" });
  }
});
