# Cocina App

Despensa, recetas y lista de compra: app nativa de Android + página web, ambas conectadas a un
mismo servidor propio (accesible desde cualquier lado, no solo en casa). Basada en el diseño de
Claude Design "Integración de app de cocina".

**Producción:** https://cocina-app-server.fly.dev (login con el usuario creado vía `create-user`).

## Estructura

- `server/` — backend propio: Node.js + Express + Prisma/Postgres (Neon), auth con JWT, y sirve
  `app/dist` como página web en la misma URL. Desplegado en Fly.io (`fly.toml` en la raíz).
- `app/` — la página web (React + TypeScript + Vite). Habla con `server/` vía `fetch` con Bearer
  token (`src/storage/serverStorage.ts`); no depende de ningún wrapper nativo.
- `android-native/` — app Android nativa (Kotlin + Jetpack Compose, sin WebView). Room como
  copia local (offline-first) + Retrofit contra el mismo backend, con sync automático (debounce al
  editar, y al reabrir la app) y resolución de conflictos por `revision` (última escritura gana a
  nivel de documento completo).

Tanto la web como la app nativa comparten el mismo modelo de datos (`AppState`: despensa, recetas,
lista de compra) y el mismo backend — un cambio hecho en una se ve en la otra.

## Comandos

```bash
npm install                     # instala app/ y server/

npm run dev                     # app en el navegador contra localStorage (sin backend)
npm run server:dev              # servidor local en :8080 (necesita server/.env, ver abajo)

npm run android:build           # compila el APK debug (android-native/app/build/outputs/apk/debug)
npm run android:install         # instala el último APK en el dispositivo conectado por adb
npm run android:run             # build + install + abre la app

npm run deploy                  # despliega server/ a Fly.io (requiere flyctl autenticado)
```

### Variables de entorno de `server/`

Copiar `server/.env.example` a `server/.env`:

```
DATABASE_URL="postgresql://..."   # connection string de Neon
JWT_SECRET="..."                  # valor largo y aleatorio
PORT=8080
```

En producción estas viven como secrets de Fly (`flyctl secrets set ...`), no en el repo.

### Migraciones de base de datos

```bash
cd server
npx prisma migrate dev --name <nombre>   # genera y aplica una migración nueva contra DATABASE_URL
```

Nota: si `prisma migrate dev/status` falla con `P1001` (no llega al host) en esta red pero un
script Node con `pg` sí conecta, es un problema de resolución DNS IPv6 específico de esta
conexión (ver `src/db.ts`, que fuerza IPv4) — el motor de esquema de Prisma es un binario aparte
que no respeta ese workaround. En ese caso: generar el SQL con
`npx prisma migrate diff --from-empty --to-schema prisma/schema.prisma --script`, guardarlo en
`prisma/migrations/<timestamp>_<nombre>/migration.sql`, y aplicarlo a mano (ver
`scripts/bootstrap-db.mjs` como referencia). En Fly.io (`release_command` en `fly.toml`, hoy
comentado) esto no debería pasar — la red del datacenter no tiene el mismo problema.

### App Android nativa: apuntar a otro backend

Por variable de Gradle, para probar contra un servidor local en vez de producción:

```bash
./gradlew assembleDebug -PCOCINA_API_URL=http://127.0.0.1:8080
adb reverse tcp:8080 tcp:8080   # si el celular está conectado por USB
```

El valor por defecto (`https://cocina-app-server.fly.dev`) está en `android-native/app/build.gradle.kts`.

## Cómo sincroniza

`AppState` (despensa, recetas, lista de compra) vive como una fila `jsonb` por usuario en Postgres,
con un `revision` que se incrementa en cada `PUT /api/state` exitoso.

- **Página web**: cada cambio hace `PUT` de inmediato; si el servidor devuelve 409 (alguien más
  editó primero), reintenta con la revisión nueva — última escritura gana.
- **App nativa**: cada edición se guarda al instante en Room (offline-first, la UI nunca espera a
  la red) y se empuja al servidor con un debounce de ~800ms; si no hay red, queda marcada como
  pendiente y se reintenta la próxima vez que la app se abre o vuelve a primer plano. Probado a
  mano: editar sin conexión, confirmar que el servidor no cambia, reconectar, confirmar que llega.

## Agregar recetas con Claude Cowork

La pantalla "+ Crear nueva receta" de la página web (https://cocina-app-server.fly.dev, o
`http://localhost:PORT` si `server:dev` está corriendo local) es la misma que usarías vos — Claude
Cowork puede completarla manejando el navegador, sin necesidad de una API aparte.
