import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) throw new Error("Falta la variable de entorno JWT_SECRET");

const ACCESS_TOKEN_TTL = "15m";
const REFRESH_TOKEN_TTL = "30d";

export type TokenType = "access" | "refresh";

export interface TokenPayload {
  sub: string;
  type: TokenType;
}

export function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, 12);
}

export function verifyPassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

export function signAccessToken(userId: string): string {
  return jwt.sign({ sub: userId, type: "access" } satisfies TokenPayload, JWT_SECRET!, {
    expiresIn: ACCESS_TOKEN_TTL,
  });
}

export function signRefreshToken(userId: string): string {
  return jwt.sign({ sub: userId, type: "refresh" } satisfies TokenPayload, JWT_SECRET!, {
    expiresIn: REFRESH_TOKEN_TTL,
  });
}

export function verifyToken(token: string, expectedType: TokenType): TokenPayload {
  const payload = jwt.verify(token, JWT_SECRET!) as TokenPayload;
  if (payload.type !== expectedType) throw new Error(`Se esperaba un token de tipo ${expectedType}`);
  return payload;
}
