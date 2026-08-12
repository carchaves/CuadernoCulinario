# Build context is the repo root: docker build -f server/Dockerfile .

FROM node:22-slim AS app-build
WORKDIR /app/app
COPY app/package.json app/package-lock.json* ./
RUN npm install
COPY app/ ./
RUN npm run build

FROM node:22-slim AS server-build
WORKDIR /app/server
COPY server/package.json server/package-lock.json* ./
RUN npm install
COPY server/ ./
RUN npx prisma generate
RUN npm run build

FROM node:22-slim
ENV NODE_ENV=production
WORKDIR /app/server
COPY server/package.json server/package-lock.json* ./
COPY server/prisma ./prisma
COPY server/prisma.config.ts ./
RUN npm install --omit=dev
COPY --from=server-build /app/server/dist ./dist
COPY --from=server-build /app/server/node_modules/.prisma ./node_modules/.prisma
COPY --from=app-build /app/app/dist /app/app/dist

EXPOSE 8080
CMD ["node", "dist/index.js"]
