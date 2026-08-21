# Cocina App

Despensa, recetas y lista de compra. Sin servidor ni base de datos: los datos viven como
archivos JSON versionados en este mismo repositorio (`data/`). La app nativa de Android es
la única que los edita — hace `pull` al abrir (si hay internet) y `commit` cuando hay
cambios, directo contra la API de GitHub. La página web es un visor de solo lectura,
publicado con GitHub Pages, que siempre muestra lo último que haya en `main`.

**Web:** https://carchaves.github.io/CuadernoCulinario/

## Estructura

- `data/` — los tres archivos que son la única fuente de verdad: `despensa.json`,
  `recetas.json`, `lista-de-compra.json`. Ver `data/README.md` para el schema de cada uno.
- `app/` — el visor web (React + TypeScript + Vite). Solo lectura: al cargar pide los tres
  archivos de `data/` directamente a `raw.githubusercontent.com` (repo público, sin auth) y
  los muestra. No tiene login ni forma de editar. Publicado en GitHub Pages vía
  `.github/workflows/deploy-pages.yml`.
- `android-native/` — app Android nativa (Kotlin + Jetpack Compose, sin WebView), la única
  editora. Room como copia local offline-first + un cliente de la API de contenidos de
  GitHub, con sync automático (debounce al editar, y al reabrir la app) y resolución de
  conflictos por `sha` de archivo (última escritura gana, por archivo).

## Comandos

```bash
npm install                     # instala app/
npm run dev                     # visor web local (Vite), contra los datos reales del repo
npm run build:app               # build de producción del visor web

npm run android:build           # compila el APK debug (android-native/app/build/outputs/apk/debug)
npm run android:install         # instala el último APK en el dispositivo conectado por adb
npm run android:run             # build + install + abre la app
```

## Cómo edita la app Android

`data/despensa.json`, `data/recetas.json` y `data/lista-de-compra.json` se leen/escriben vía
la API de contenidos de GitHub (`GET`/`PUT
repos/carchaves/CuadernoCulinario/contents/data/<archivo>`), usando el `sha` de cada blob
como control de concurrencia.

- Cada edición se guarda al instante en Room (offline-first, la UI nunca espera a la red).
- ~800ms después de la última edición, se hace `commit` (uno por archivo que realmente
  cambió) contra GitHub. Si no hay red, el cambio queda marcado como pendiente y se reintenta
  la próxima vez que la app se abre o vuelve a primer plano.
- Si el `sha` quedó desactualizado (otro commit se adelantó), se vuelve a pedir el archivo y
  se reintenta con el `sha` nuevo — última escritura gana, a nivel de archivo.

La web nunca escribe: en cada carga vuelve a pedir los tres archivos a
`raw.githubusercontent.com`, así que un commit hecho desde Android aparece ahí sin necesidad
de ningún redeploy.

## Configurar el token de GitHub en la app Android

Como no hay servidor, la app necesita un credential propio para poder comitear. La primera
vez que se abre (o desde el ícono de ajustes del menú) pide un **Personal Access Token** de
GitHub:

1. En GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens
   → Generate new token.
2. Repository access: **Only select repositories** → `carchaves/CuadernoCulinario`.
3. Permissions → Repository permissions → **Contents: Read and write**.
4. Pegar el token generado en la pantalla de Ajustes de la app. Se guarda cifrado en el
   dispositivo (`EncryptedSharedPreferences`), nunca en el repo.

## Despliegue de la web (GitHub Pages)

`.github/workflows/deploy-pages.yml` compila `app/` con Vite y publica `app/dist` en GitHub
Pages en cada push a `main` que toque `app/`. Los cambios en `data/` **no** disparan un
redeploy — no hace falta, la web pide los datos en vivo en cada carga.

Paso manual único (no se puede automatizar sin acceso a la cuenta): en el repo de GitHub, ir
a **Settings → Pages → Build and deployment → Source: GitHub Actions**.

## Historial

La app corrió antes con un servidor propio (Node/Express + Prisma/Postgres en Neon,
desplegado en Render) que migró primero desde Fly.io. Se eliminó por completo al pasar a
guardar los datos directamente en este repo.
