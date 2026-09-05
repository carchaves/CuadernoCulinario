# Cocina App

Despensa, recetas y lista de compra. Sin servidor ni base de datos: los datos viven como
archivos JSON versionados en este mismo repositorio (`data/`). Tanto la app Android como la
versión de escritorio de la web editan directo contra la API de GitHub — cada una con su
propia copia local offline-first, su Personal Access Token propio, y el mismo criterio de
concurrencia por `sha` de archivo (última escritura gana, por archivo), así que un cambio
hecho en una aparece en la otra sin pisarse.

**Web:** https://carchaves.github.io/CuadernoCulinario/

## Estructura

- `data/` — los tres archivos que son la única fuente de verdad: `despensa.json`,
  `recetas.json`, `lista-de-compra.json`. Ver `data/README.md` para el schema de cada uno.
- `app/` — versión de escritorio de la web (React + TypeScript + Vite), editable. Layout de
  sidebar fijo pensado para pantalla ancha, no una versión de teléfono estirada. Guarda su
  Personal Access Token en `localStorage`; sin token configurado sigue funcionando en modo
  solo lectura. Publicada en GitHub Pages vía `.github/workflows/deploy-pages.yml`.
- `android-native/` — app Android nativa (Kotlin + Jetpack Compose, sin WebView), la versión
  para el celular. Room como copia local offline-first + un cliente de la API de contenidos de
  GitHub, con sync automático (debounce al editar, y al reabrir la app).

## Comandos

```bash
npm install                     # instala app/
npm run dev                     # visor web local (Vite), contra los datos reales del repo
npm run build:app               # build de producción de la web

npm run android:build           # compila el APK debug (android-native/app/build/outputs/apk/debug)
npm run android:install         # instala el último APK en el dispositivo conectado por adb
npm run android:run             # build + install + abre la app
```

## Cómo sincronizan Android y la web

`data/despensa.json`, `data/recetas.json` y `data/lista-de-compra.json` se leen/escriben vía
la API de contenidos de GitHub (`GET`/`PUT
repos/carchaves/CuadernoCulinario/contents/data/<archivo>`), usando el `sha` de cada blob
como control de concurrencia. Android usa Room y la web usa `localStorage` como caché local,
pero el patrón es el mismo en las dos:

- Cada edición se guarda al instante en la caché local (offline-first, la UI nunca espera a
  la red).
- ~800ms después de la última edición, se hace `commit` (uno por archivo que realmente
  cambió) contra GitHub. Si no hay red, el cambio queda marcado como pendiente y se reintenta
  la próxima vez que se abre la app/pestaña o vuelve a primer plano.
- Si el `sha` quedó desactualizado (otro commit se adelantó, desde el mismo cliente o el
  otro), se vuelve a pedir el archivo y se reintenta con el `sha` nuevo — última escritura
  gana, a nivel de archivo.

## Configurar el token de GitHub

Como no hay servidor, cada cliente necesita su propio credential para poder comitear. Tanto
Android (pantalla de Ajustes, ícono de engranaje del menú) como la web (mismo nombre, en el
sidebar) piden un **Personal Access Token** de GitHub la primera vez:

1. En GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens
   → Generate new token.
2. Repository access: **Only select repositories** → `carchaves/CuadernoCulinario`.
3. Permissions → Repository permissions → **Contents: Read and write**.
4. Pegar el token generado en Ajustes. Android lo guarda cifrado en el dispositivo
   (`EncryptedSharedPreferences`); la web lo guarda en `localStorage` del navegador. Nunca se
   guarda en el repo — cada dispositivo/navegador necesita el suyo (se puede reusar el mismo
   token en varios).

## Despliegue de la web (GitHub Pages)

`.github/workflows/deploy-pages.yml` compila `app/` con Vite y publica `app/dist` en GitHub
Pages en cada push a `main` que toque `app/`. Los cambios en `data/` **no** disparan un
redeploy — no hace falta, la web pide los datos en vivo en cada carga.

Paso manual único (no se puede automatizar sin acceso a la cuenta): en el repo de GitHub, ir
a **Settings → Pages → Build and deployment → Source: GitHub Actions**.

## Historial

La app corrió antes con un servidor propio (Node/Express + Prisma/Postgres en Neon,
desplegado en Render) que migró primero desde Fly.io. Se eliminó por completo al pasar a
guardar los datos directamente en este repo. La web fue al principio un visor de solo
lectura y pasó a ser editable (versión de escritorio) más adelante, siguiendo el mismo diseño
de referencia que guió la reescritura de Android.
