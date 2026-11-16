import { RULE_KIND_GROUP, t } from "@/lib/labels";
import type { Requirements } from "@/lib/types";

/** Graduation rules, split into the ones the calculator can evaluate and the ones that are information only. */
export function RequirementList({ requirements }: { requirements: Requirements }) {
  const core = requirements.rules.filter((r) => RULE_KIND_GROUP[r.kind] === "core");
  const other = requirements.rules.filter((r) => RULE_KIND_GROUP[r.kind] !== "core");

  return (
    <>
      <h2>Προϋποθέσεις απόκτησης πτυχίου</h2>
      <ol>
        {core.map((r) => (
          <li key={r.id} id={r.id} style={{ marginBottom: 10 }}>
            <strong>{t(r.title)}</strong>
            <div>{t(r.description)}</div>
          </li>
        ))}
      </ol>

      <h2>Άλλες διατάξεις</h2>
      <div className="card-grid" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))" }}>
        {other.map((r) => (
          <div key={r.id} id={r.id} className="card">
            <div className="card-title">{t(r.title)}</div>
            <div className="card-meta">{t(r.description)}</div>
          </div>
        ))}
      </div>

      {requirements.transitionalProvisions.length > 0 ? (
        <>
          <h2>Μεταβατικές διατάξεις</h2>
          <ul>
            {requirements.transitionalProvisions.map((p, i) => (
              <li key={i}>
                {t(p.description)} <span className="muted small">(ισχύει έως {p.validUntil})</span>
              </li>
            ))}
          </ul>
        </>
      ) : null}

      {requirements.recentChanges.length > 0 ? (
        <>
          <h2>Πρόσφατες αλλαγές του οδηγού</h2>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Έτος</th><th>Αλλαγή</th></tr></thead>
              <tbody>
                {requirements.recentChanges.map((c, i) => (
                  <tr key={i}><td>{c.year}</td><td>{t(c.text)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </>
  );
}
