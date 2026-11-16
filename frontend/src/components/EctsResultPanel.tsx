import Link from "next/link";
import { fmt, t } from "@/lib/labels";
import type { CalculatorResult, DepartmentInfo } from "@/lib/types";

function Bar({ value, max }: { value: number; max: number }) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
  return (
    <div className={`progress${value >= max ? " done" : ""}`} role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
      <span style={{ width: `${pct}%` }} />
    </div>
  );
}

/** Shows what the calculator found. Everything here is an estimate from the courses the user ticked. */
export function EctsResultPanel({
  result,
  info,
  base,
  names,
}: {
  result: CalculatorResult;
  info: DepartmentInfo;
  base: string;
  names: Map<string, string>;
}) {
  const href = (code: string) => `${base}/courses/${encodeURIComponent(code)}`;
  const label = (code: string) => names.get(code) ?? code;
  const levelName = new Map(info.levels.map((l) => [l.id, l]));
  const unmet = result.rules.filter((r) => !r.satisfied).length;

  return (
    <div aria-live="polite">
      <div className="panel">
        <h2 style={{ marginTop: 0 }}>
          {fmt(result.totals.earned)} / {fmt(result.totals.required)} ECTS
        </h2>
        <Bar value={result.totals.earned} max={result.totals.required} />
        <p style={{ margin: 0 }}>
          {result.totals.remaining > 0 ? (
            <>Σας λείπουν <strong>{fmt(result.totals.remaining)} ECTS</strong> για το σύνολο.</>
          ) : (
            <>Το σύνολο ECTS καλύπτεται.</>
          )}
        </p>
        {result.totals.userProvided > 0 ? (
          <p className="small muted" style={{ marginBottom: 0 }}>
            Από αυτά, {fmt(result.totals.userProvided)} ECTS τα δηλώσατε εσείς (ο οδηγός δεν τα αναφέρει).
          </p>
        ) : null}
      </div>

      {result.allCalculableRulesMet ? (
        <div className="notice ok">Με βάση τα μαθήματα που επιλέξατε, πληρούνται όλες οι προϋποθέσεις που μπορεί να ελέγξει ο υπολογιστής.</div>
      ) : (
        <div className="notice info">
          {unmet} από {result.rules.length} προϋποθέσεις δεν πληρούνται ακόμη. Δείτε παρακάτω τι λείπει.
        </div>
      )}

      <h3>Προϋποθέσεις</h3>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th /><th>Προϋπόθεση</th><th className="num">Τώρα</th><th className="num">Απαιτείται</th><th className="num">Λείπουν</th></tr>
          </thead>
          <tbody>
            {result.rules.map((r) => (
              <tr key={r.id}>
                <td>{r.satisfied ? <span className="check" aria-label="Πληρούται">✓</span> : <span className="cross" aria-label="Δεν πληρούται">✗</span>}</td>
                <td>
                  {t(r.title)}
                  {r.missing.length > 0 ? (
                    <details>
                      <summary className="small muted">Δείτε τα {r.missing.length} μαθήματα που λείπουν</summary>
                      <ul className="small" style={{ margin: "4px 0 0", paddingLeft: "1.1rem" }}>
                        {r.missing.map((m) => (
                          <li key={m.code}>
                            <Link href={href(m.code)}>{m.name ? t(m.name) : m.code}</Link>
                          </li>
                        ))}
                      </ul>
                    </details>
                  ) : null}
                </td>
                <td className="num">{fmt(r.current)}</td>
                <td className="num">{fmt(r.required)}</td>
                <td className="num">{r.remaining > 0 ? fmt(r.remaining) : "–"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h3>Ανά επίπεδο</h3>
      <div className="table-wrap">
        <table>
          <thead><tr><th>Επίπεδο</th><th className="num">Μαθήματα</th><th className="num">ECTS</th></tr></thead>
          <tbody>
            {result.levels.map((l) => (
              <tr key={l.level}>
                <td>{levelName.get(l.level) ? `${levelName.get(l.level)!.short} · ${t(levelName.get(l.level)!.name)}` : l.level}</td>
                <td className="num">{l.coursesPassed}</td>
                <td className="num">{fmt(l.ectsEarned)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {result.cycles ? (
        <>
          <h3>Κύκλοι σπουδών</h3>
          <p>
            Με τα μαθήματα που επιλέξατε μπορείτε να ολοκληρώσετε έως <strong>{result.cycles.maxCompletable}</strong> κύκλους
            (χρειάζονται {result.cycles.minCourses} μαθήματα ανά κύκλο και κάθε μάθημα μετρά σε έναν μόνο κύκλο).
          </p>
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {result.cycles.items.map((c) => (
              <li key={c.id} className="panel" style={{ margin: "8px 0", padding: "10px 14px" }}>
                <div>
                  <strong>
                    {c.number}. {t(c.name)}
                  </strong>{" "}
                  {c.completedInBestPlan ? <span className="badge green">Ολοκληρώνεται</span> : null}
                </div>
                <Bar value={Math.min(c.passedCodes.length, result.cycles!.minCourses)} max={result.cycles!.minCourses} />
                <div className="small muted">
                  {c.passedCodes.length} από {result.cycles!.minCourses} μαθήματα
                  {c.missingToComplete > 0 ? ` · λείπουν ${c.missingToComplete}` : ""}
                </div>
                {c.completedInBestPlan ? (
                  <div className="small">
                    Μετρούν εδώ:{" "}
                    {c.assignedCodes.map((code, i) => (
                      <span key={code}>
                        {i > 0 ? ", " : ""}
                        <Link href={href(code)}>{label(code)}</Link>
                      </span>
                    ))}
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        </>
      ) : null}

      {result.manualChecks.length > 0 ? (
        <>
          <h3>Δεν ελέγχονται από τον υπολογιστή</h3>
          <p className="small muted">Αυτές οι προϋποθέσεις πρέπει να τις επιβεβαιώσετε μόνοι σας ή με τη γραμματεία:</p>
          <ul className="small">
            {result.manualChecks.map((m) => (
              <li key={m.id}>
                <Link href={`${base}/requirements#${m.id}`}>{t(m.title)}</Link>
              </li>
            ))}
          </ul>
        </>
      ) : null}

      {result.warnings.length > 0 || result.unknownCodes.length > 0 ? (
        <details style={{ marginTop: 16 }}>
          <summary>Παρατηρήσεις ({result.warnings.length + result.unknownCodes.length})</summary>
          <ul className="small">
            {result.unknownCodes.map((c) => (
              <li key={`u-${c}`}>Ο κωδικός «{c}» δεν αντιστοιχεί σε μάθημα του τμήματος και αγνοήθηκε.</li>
            ))}
            {result.warnings.map((w, i) => (
              <li key={`${w.code}-${i}`}>{warningText(w, label)}</li>
            ))}
          </ul>
        </details>
      ) : null}

      <p className="muted small" style={{ marginTop: 16 }}>
        Ο υπολογισμός είναι ενδεικτικός: βασίζεται στα μαθήματα που δηλώσατε, δεν ελέγχει βαθμούς, απαλλαγές ή μεταβατικές διατάξεις.
        Για επίσημη επιβεβαίωση απευθυνθείτε στη γραμματεία.
      </p>
    </div>
  );
}

function warningText(w: CalculatorResult["warnings"][number], label: (code: string) => string): string {
  const course = w.courseCode ? label(w.courseCode) : "";
  switch (w.code) {
    case "ECTS_UNKNOWN":
      return `Το μάθημα «${course}» θεωρείται περασμένο, αλλά ο οδηγός δεν αναφέρει ECTS γι' αυτό και δεν πληκτρολογήσατε. Συμπληρώστε τα ECTS του στη λίστα για να προσμετρηθούν.`;
    case "DUPLICATE_CODE":
      return `Το μάθημα «${course}» επιλέχθηκε περισσότερες από μία φορές και μετρήθηκε μία.`;
    case "PREREQUISITE_NOT_PASSED":
      return `Το μάθημα «${course}» έχει προαπαιτούμενο που δεν υπάρχει στη λίστα σας (ενημερωτικό: ο οδηγός έχει χαλαρώσει προϋποθέσεις).`;
    default:
      return w.message;
  }
}
