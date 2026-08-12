import type { NextFunction, Request, Response } from "express";
import { verifyToken } from "../auth.js";

declare global {
  namespace Express {
    interface Request {
      userId?: string;
    }
  }
}

export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  const token = header?.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) {
    res.status(401).json({ error: "Falta el header Authorization" });
    return;
  }
  try {
    const payload = verifyToken(token, "access");
    req.userId = payload.sub;
    next();
  } catch {
    res.status(401).json({ error: "Token inválido o vencido" });
  }
}
