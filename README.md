# Cocina App

Despensa, recetas y lista de compra, en el celular y sincronizadas con la compu cuando la
conectás por cable. Basada en el diseño de Claude Design "Integración de app de cocina".

## Estructura

- `app/` — la aplicación (React + TypeScript + Vite). Es el mismo código que corre en el celular
  (empaquetado con Capacitor) y el que sirve `desktop-server` en la compu.
- `app/android/` — proyecto nativo Android generado por Capacitor.
- `desktop-server/` — servidor local que detecta el celular por `adb`, trae sus datos, sirve la
  misma app en `http://localhost:4787` y devuelve los cambios al celular mientras esté conectado.
- `shared.config.json` — appId y puerto compartidos entre `app/` y `desktop-server/`.

## Comandos

```bash
npm install                # instala todo (raíz, app, desktop-server)

npm run dev                 # servidor de desarrollo de la app en el navegador (localStorage)

npm run android:run         # build + instala + abre la app en el celular conectado por adb
npm run android:build       # solo compila el APK debug (app/android/app/build/outputs/apk/debug)
npm run android:install     # solo instala el último APK compilado

npm run desktop             # build de la app + levanta el servidor de escritorio en :4787
```

## Cómo sincroniza

Los datos de la app viven en un único archivo (`cocina-data.json`) dentro del almacenamiento
privado de la app en el celular. `desktop-server` sondea `adb devices`; cuando detecta el celular:

1. Trae ese archivo con `adb exec-out run-as <appId> cat files/cocina-data.json`.
2. Lo sirve en `http://localhost:4787` (misma UI que el celular, pero hablando con este servidor
   en vez de con el filesystem del teléfono).
3. Cada cambio se guarda localmente y, con un pequeño debounce, se devuelve al celular con
   `run-as ... sh -c 'cat > files/cocina-data.json'`.

Al desconectar el cable, el servidor deja de intentar enviar cambios — lo último que se envió
queda en el celular. La app del celular vuelve a leer su archivo cada vez que pasa a primer plano,
así que cualquier cambio hecho desde la compu se ve apenas se reabre la app.

Este mecanismo no necesita root: `run-as` funciona porque el APK se instala en modo debug
(`gradlew assembleDebug`, `adb install`).

## Agregar recetas con Claude Cowork

Con el celular conectado y `npm run desktop` corriendo, abrí `http://localhost:4787`, entrá a
Recetas y usá "+ Crear nueva receta" — es la misma pantalla que usarías vos, así que Claude Cowork
puede completarla manejando el navegador.
