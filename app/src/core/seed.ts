import { uid } from "./logic";
import type { AppState, PantryPage, Recipe } from "./types";

export function seedPantry(): PantryPage[] {
  return [
    {
      id: uid(),
      name: "Granos y secos",
      ingredients: [
        { id: uid(), name: "Arroz", type: "peso", amount: 2, unit: "kg" },
        { id: uid(), name: "Lentejas", type: "peso", amount: 500, unit: "g" },
        { id: uid(), name: "Harina", type: "peso", amount: 1, unit: "kg" },
      ],
    },
    {
      id: uid(),
      name: "Frescos",
      ingredients: [
        { id: uid(), name: "Huevos", type: "unidad", amount: 12, unit: "u" },
        { id: uid(), name: "Leche", type: "peso", amount: 1, unit: "L" },
      ],
    },
    {
      id: uid(),
      name: "Especias",
      ingredients: [
        { id: uid(), name: "Sal", type: "peso", amount: 500, unit: "g" },
        { id: uid(), name: "Comino", type: "unidad", amount: 2, unit: "u" },
      ],
    },
    {
      id: uid(),
      name: "Carne deshebrada",
      ingredients: [
        { id: uid(), name: "Posta o pecho de res", type: "peso", amount: 1.3, unit: "kg" },
        { id: uid(), name: "Cebolla", type: "unidad", amount: 2, unit: "u" },
        { id: uid(), name: "Pimiento", type: "unidad", amount: 1, unit: "u" },
        { id: uid(), name: "Ajo", type: "unidad", amount: 4, unit: "u" },
        { id: uid(), name: "Tomate", type: "unidad", amount: 2, unit: "u" },
        { id: uid(), name: "Pasta de tomate", type: "peso", amount: 100, unit: "g" },
        { id: uid(), name: "Comino", type: "peso", amount: 20, unit: "g" },
        { id: uid(), name: "Orégano", type: "peso", amount: 20, unit: "g" },
        { id: uid(), name: "Laurel", type: "unidad", amount: 3, unit: "u" },
        { id: uid(), name: "Sal", type: "peso", amount: 200, unit: "g" },
        { id: uid(), name: "Pimienta", type: "peso", amount: 50, unit: "g" },
        { id: uid(), name: "Caldo", type: "peso", amount: 250, unit: "ml" },
      ],
    },
  ];
}

export function seedRecipes(): Record<"cocina" | "repo", Recipe[]> {
  return {
    cocina: [
      {
        id: "baleadas-armadas",
        title: "Baleadas con carne, frijol, huevo y aguacate",
        meta: ["Ensamblado", "Individual"],
        tiempo: "≈ 10 min",
        ingredientes: [
          "4 tortillas de harina",
          "2 tazas de carne deshebrada",
          "1 taza de frijol refrito",
          "4 huevos",
          "1 aguacate",
          "Sal",
        ],
        utensilios: ["Sartén", "Comal", "Espátula"],
        prePrep: [{ t: "Calentar por separado la carne deshebrada y el frijol refrito." }],
        prep: [
          { t: "Freír los huevos." },
          { t: "Calentar las tortillas en el comal." },
          { t: "Rellenar cada tortilla con frijol, carne, huevo frito y aguacate." },
        ],
      },
      {
        id: "pepian-recalentado",
        title: "Pepián recalentado con arroz",
        meta: ["Recalentado", "Rápido"],
        tiempo: "≈ 10 min",
        ingredientes: ["Pepián (sobrante)", "Arroz cocido"],
        utensilios: ["Olla pequeña", "Cuchara de madera"],
        prePrep: [{ t: "Sacar el pepián y el arroz del refrigerador." }],
        prep: [
          { t: "Calentar el pepián a fuego lento, agregando agua o caldo si está muy espeso." },
          { t: "Calentar el arroz." },
          { t: "Servir el pepián sobre el arroz." },
        ],
      },
      {
        id: "pupusas-freezer",
        title: "Pupusas del freezer al comal + curtido",
        meta: ["Del freezer", "Rápido"],
        tiempo: "≈ 15 min",
        ingredientes: ["Pupusas congeladas", "Curtido"],
        utensilios: ["Comal", "Tenazas"],
        prePrep: [{ t: "Sacar las pupusas congeladas y el curtido del refrigerador." }],
        prep: [
          {
            t: "Cocinar las pupusas directo del freezer en comal a fuego medio-bajo, sin descongelar.",
            time: "5–6 min por lado",
          },
          { t: "Servir calientes con curtido." },
        ],
      },
      {
        id: "bowl-gallo-pinto",
        title: "Bowl de gallo pinto, carne, huevo y plátano",
        meta: ["Bowl", "Desayuno o cena"],
        tiempo: "≈ 15 min",
        ingredientes: [
          "Gallo pinto (sobrante)",
          "1 taza de carne deshebrada",
          "2 huevos",
          "1 plátano maduro",
          "Aceite",
        ],
        utensilios: ["Sartén", "Espátula"],
        prePrep: [{ t: "Cortar el plátano maduro en rodajas." }],
        prep: [
          { t: "Freír el plátano maduro hasta dorar." },
          { t: "Calentar el gallo pinto y la carne deshebrada." },
          { t: "Freír los huevos." },
          { t: "Armar el bowl con gallo pinto, carne, huevo y plátano." },
        ],
      },
      {
        id: "tacos-carne-deshebrada",
        title: "Tacos de carne deshebrada, chimol, aguacate y queso",
        meta: ["Tacos o plato", "Ensamblado"],
        tiempo: "≈ 15 min",
        ingredientes: [
          "Tortillas de maíz o harina",
          "2 tazas de carne deshebrada",
          "Chimol (tomate, cebolla, culantro, limón)",
          "1 aguacate",
          "Queso que derrita",
        ],
        utensilios: ["Comal", "Tabla y cuchillo"],
        prePrep: [
          { t: "Preparar el chimol: picar tomate, cebolla y culantro; sazonar con limón y sal." },
        ],
        prep: [
          { t: "Calentar la carne deshebrada y las tortillas." },
          { t: "Armar tacos o plato con carne, chimol, aguacate y queso." },
        ],
      },
    ],
    repo: [
      {
        id: "barras-datil",
        title: "Barras de dátil, avena y maní",
        meta: ["Sin horno", "~12"],
        tiempo: "≈ 20 min + 1 h refrigeración",
        ingredientes: [
          "Avena",
          "Nueces",
          "250 g de dátiles",
          "Mantequilla de maní",
          "Miel",
          "Leche en polvo",
          "Sal",
          "Chocolate para cubrir",
        ],
        utensilios: ["Procesador de alimentos", "Molde rectangular", "Refrigerador"],
        prePrep: [
          { t: "Tostar avena y nueces.", time: "3–4 min" },
          { t: "Procesar los dátiles hasta formar una pasta." },
        ],
        prep: [
          { t: "Sumar mantequilla de maní, miel, leche en polvo y sal a los dátiles." },
          { t: "Incorporar la avena y nueces en pulsos." },
          { t: "Presionar en el molde y cubrir con chocolate." },
          { t: "Refrigerar y cortar en frío.", time: "1 h" },
        ],
      },
      {
        id: "bolitas",
        title: "Bolitas de avena y cacao",
        meta: ["Sin horno", "~20"],
        tiempo: "≈ 20 min",
        ingredientes: [
          "Avena",
          "Mantequilla de maní",
          "Leche en polvo",
          "Cacao amargo",
          "Miel",
          "Chía",
          "Coco rallado",
        ],
        utensilios: ["Bowl", "Refrigerador"],
        prePrep: [
          { t: "Mezclar avena, mantequilla de maní, leche en polvo, cacao, miel y chía." },
          { t: "Refrigerar la mezcla.", time: "15 min" },
        ],
        prep: [{ t: "Bolear la mezcla." }, { t: "Rebozar en coco rallado." }],
      },
      {
        id: "barras-platano",
        title: "Barras de avena con plátano",
        meta: ["Horno", "~12"],
        tiempo: "≈ 30 min",
        ingredientes: [
          "Plátanos maduros",
          "Huevos",
          "Yogur griego",
          "Miel",
          "Avena",
          "Leche en polvo",
          "Polvo de hornear",
          "Canela",
          "Nueces",
          "Chispas de chocolate",
        ],
        utensilios: ["Horno", "Molde rectangular", "Bowl"],
        prePrep: [{ t: "Pisar los plátanos con huevos, yogur y miel." }],
        prep: [
          { t: "Sumar avena, leche en polvo, polvo de hornear, canela, nueces y chispas." },
          { t: "Hornear a 180 °C.", time: "22–25 min · 180°" },
          { t: "Enfriar antes de cortar." },
        ],
      },
      {
        id: "granola",
        title: "Granola en clusters",
        meta: ["Horno"],
        tiempo: "≈ 30 min",
        ingredientes: [
          "1 clara de huevo",
          "Avena",
          "Frutos secos",
          "Semillas",
          "Leche en polvo",
          "Canela",
          "Miel",
          "Aceite de coco",
          "Coco rallado",
        ],
        utensilios: ["Horno", "Bandeja para hornear", "Bowl"],
        prePrep: [
          { t: "Batir la clara de huevo." },
          {
            t: "Mezclar avena, frutos secos, semillas, leche en polvo y canela con la clara, miel y aceite de coco.",
          },
        ],
        prep: [
          { t: "Hornear sin revolver los primeros 20 min.", time: "25–30 min · 150°" },
          { t: "Sumar el coco al final." },
          { t: "Enfriar sin tocar para que queden clusters." },
        ],
      },
    ],
  };
}

export function seedState(): AppState {
  const pPages = seedPantry();
  return {
    pPages,
    pActiveId: pPages[0].id,
    lDone: {},
    lIncluded: {},
    recipes: seedRecipes(),
    rDone: {},
  };
}
