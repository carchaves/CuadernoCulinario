# Datos de Cocina App

Estos tres archivos son la única fuente de verdad de la app: no hay servidor ni base de
datos, solo estos JSON versionados en git.

- **Quién escribe:** solo la app Android. Al abrir hace `pull` (si hay internet) de los tres
  archivos; cuando el usuario edita algo, hace `commit` del archivo que cambió vía la API de
  contenidos de GitHub (`PUT /repos/carchaves/CuadernoCulinario/contents/data/<archivo>`),
  usando el `sha` del blob como control de concurrencia (si otro commit se adelantó, reintenta
  con el `sha` nuevo — última escritura gana, a nivel de archivo).
- **Quién lee:** la app Android (con su propia copia local en caché) y el visor web
  (`app/`, publicado en GitHub Pages), que pide estos archivos directamente a
  `raw.githubusercontent.com` en cada carga — la web nunca escribe, solo muestra lo que haya
  en `main`.

## `despensa.json`

```json
{
  "pages": [
    { "id": "p1", "name": "Granos y secos", "ingredients": [
      { "id": "i1", "name": "Arroz", "type": "peso", "amount": 2, "unit": "kg" }
    ] }
  ],
  "activePageId": "p1"
}
```

`type` es `"peso"` (con `unit` en `g`/`kg`/`ml`/`L`) o `"unidad"` (con `unit: "u"`).

## `recetas.json`

```json
{
  "cocina": [
    { "id": "r1", "title": "...", "meta": ["..."], "tiempo": "45 min",
      "ingredientes": ["..."], "utensilios": ["..."],
      "prePrep": [ { "t": "...", "title": "...", "time": "...", "note": "..." } ],
      "prep": [] }
  ],
  "repo": [],
  "stepDone": { "cocina.r1.prep.0": true }
}
```

`cocina` y `repo` son las dos vistas de recetas de la app. `stepDone` marca qué pasos de
preparación quedaron completados, con clave `"{vista}.{idReceta}.{pre|prep}.{índice}"`.

## `lista-de-compra.json`

```json
{
  "includedIngredientIds": ["i1", "i3"],
  "doneIngredientIds": ["i3"]
}
```

Listas de ids de ingredientes (definidos en `despensa.json`): cuáles están agregados a la
lista de compra (`includedIngredientIds`) y cuáles ya se marcaron como comprados
(`doneIngredientIds`).
