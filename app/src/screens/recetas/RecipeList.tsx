import type { Recipe } from "../../core/types";
import { hasAllIngredients, parseMinutes } from "../../core/logic";

interface CardInfo {
  n: number;
  recipe: Recipe;
  done: number;
  total: number;
  mins: number;
  available: boolean;
}

function Card({ info, accent, onOpen }: { info: CardInfo; accent: string; onOpen: () => void }) {
  const { recipe: r, n, done, total } = info;
  return (
    <button className="rcard" onClick={onOpen}>
      <div className="rcard-n" style={{ color: accent }}>
        {n}
      </div>
      <div className="rcard-body">
        <h2>{r.title}</h2>
        <div className="rcard-chips">
          {r.meta.map((m, i) => (
            <span className="chip" key={i}>
              {m}
            </span>
          ))}
          <span className="chip tint" style={{ background: `${accent}22`, color: accent }}>
            ⏱ {r.tiempo}
          </span>
        </div>
      </div>
      <div className="rcard-count" style={{ color: accent }}>
        {done}/{total}
      </div>
    </button>
  );
}

export function RecipeList({
  recipes,
  rDone,
  view,
  sortByTime,
  filterByPantry,
  pantryLinked,
  pantryNames,
  accent,
  onOpen,
  onToggleSort,
  onToggleFilter,
}: {
  recipes: Recipe[];
  rDone: Record<string, true>;
  view: string;
  sortByTime: boolean;
  filterByPantry: boolean;
  pantryLinked: boolean;
  pantryNames: string[];
  accent: string;
  onOpen: (id: string) => void;
  onToggleSort: () => void;
  onToggleFilter: () => void;
}) {
  let items: CardInfo[] = recipes.map((r, idx) => {
    let done = 0;
    const total = r.prePrep.length + r.prep.length;
    r.prePrep.forEach((_, i) => {
      if (rDone[`${view}.${r.id}.pre.${i}`]) done++;
    });
    r.prep.forEach((_, i) => {
      if (rDone[`${view}.${r.id}.prep.${i}`]) done++;
    });
    return { n: idx + 1, recipe: r, done, total, mins: parseMinutes(r.tiempo), available: hasAllIngredients(r.ingredientes, pantryNames) };
  });
  if (sortByTime) items = items.slice().sort((a, b) => a.mins - b.mins);

  const avail = filterByPantry ? items.filter((x) => x.available) : items;
  const rest = filterByPantry ? items.filter((x) => !x.available) : [];

  return (
    <>
      <div className="filters">
        <button className={"filter-btn" + (sortByTime ? " on" : "")} style={sortByTime ? { background: accent, borderColor: accent, color: "#fff" } : undefined} onClick={onToggleSort}>
          ⏱ Menor tiempo
        </button>
        {pantryLinked && (
          <button className={"filter-btn" + (filterByPantry ? " on" : "")} style={filterByPantry ? { background: accent, borderColor: accent, color: "#fff" } : undefined} onClick={onToggleFilter}>
            🍅 Ingredientes disponibles
          </button>
        )}
      </div>

      {avail.map((info) => (
        <Card key={info.recipe.id} info={info} accent={accent} onOpen={() => onOpen(info.recipe.id)} />
      ))}

      {rest.length > 0 && (
        <div className="divider">
          <span />
          Faltan ingredientes
          <span />
        </div>
      )}
      {rest.map((info) => (
        <div key={info.recipe.id} style={{ opacity: 0.6 }}>
          <Card info={info} accent={accent} onOpen={() => onOpen(info.recipe.id)} />
        </div>
      ))}
    </>
  );
}
