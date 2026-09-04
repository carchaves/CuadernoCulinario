# Datos de Cocina App

Estos tres archivos son la única fuente de verdad de la app: no hay servidor ni base de
datos, solo estos JSON versionados en git.

- **Quién escribe:** la app Android y la web de escritorio (`app/`, publicada en GitHub
  Pages), ambas con su propia copia local en caché. Al abrir hacen `pull` (si hay internet) de
  los archivos; cuando el usuario edita algo, hacen `commit` del archivo que cambió vía la API
  de contenidos de GitHub (`PUT /repos/carchaves/CuadernoCulinario/contents/data/<archivo>`),
  usando el `sha` del blob como control de concurrencia (si otro commit se adelantó — desde el
  otro cliente o el mismo — reintenta con el `sha` nuevo: última escritura gana, a nivel de
  archivo). Ambas necesitan un Personal Access Token de GitHub propio (`Contents: Read and
  write` sobre este repo) configurado localmente; ninguna lo guarda en el repo.
- Las fotos de recibo de compra viven en `data/receipts/*.jpg`, comiteadas igual que los JSON
  (como base64) y referenciadas por `photoPath` desde `lista-de-compra.json`.

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
  "stores": [
    { "id": "s1", "name": "Coto Devoto", "color": "#8C8377", "address": "Av. Ejemplo 123" }
  ],
  "lists": [
    { "id": "l1", "storeId": "s1", "createdAt": "2026-08-21T00:00:00Z", "finalizedAt": null,
      "items": [
        { "ingredientId": "i1", "quantity": 2, "unit": "kg", "bought": false, "price": null }
      ] }
  ],
  "priceHistory": { "i1": { "s1": 1200 } },
  "boycottedBrands": { "i1": ["MarcaX"] },
  "receipts": [
    { "id": "r1", "listId": "l1", "photoPath": "data/receipts/r1.jpg", "createdAt": "..." }
  ]
}
```

Administrador de listas por comercio:
- `stores`: comercios donde se compra (nombre, color identificador, dirección opcional).
- `lists`: una lista de compra por comercio. Cada `item` referencia un ingrediente de
  `despensa.json` por id, con cantidad/unidad a comprar, si ya se marcó comprado (`bought`) y
  el precio pagado (`price`, se completa al finalizar la compra).
- `priceHistory`: último precio conocido de cada ingrediente, por comercio — alimenta la
  comparación de precios entre comercios.
- `boycottedBrands`: marcas a evitar por ingrediente; se muestran tachadas en el catálogo y el
  detalle de la lista.
- `receipts`: fotos de recibos de compras finalizadas (ver más arriba), con la lista a la que
  pertenecen. Los precios se cargan a mano después de sacar la foto — no hay reconocimiento
  automático del recibo.
