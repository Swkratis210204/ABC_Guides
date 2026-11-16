"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { calculateEcts } from "@/lib/client";
import { fmt, lookups, t } from "@/lib/labels";
import type { CalculatorResult, CourseSummary, DepartmentInfo } from "@/lib/types";
import { useCalculatorSelection } from "@/lib/useCalculatorSelection";
import { EctsResultPanel } from "./EctsResultPanel";
import { LevelBadge } from "./LevelBadge";

/** Lowercase and strip Greek accents so "αλγοριθμοι" finds "Αλγόριθμοι". */
const fold = (s: string) => s.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();

/**
 * Reads a number the user typed. Empty means "nothing entered" (undefined); anything that is not a sensible number
 * is invalid (null). Accepts a decimal comma, as Greek keyboards produce: "7,5".
 */
function parseEcts(raw: string | undefined, max: number, allowZero: boolean): number | undefined | null {
  const s = (raw ?? "").trim().replace(",", ".");
  if (s === "") return undefined;
  const n = Number(s);
  if (!Number.isFinite(n) || n < 0 || n > max || (!allowZero && n === 0)) return null;
  return n;
}

interface Group {
  key: string;
  title: string;
  courses: CourseSummary[];
}

export function EctsCalculatorForm({
  uni,
  dept,
  courses,
  info,
  base,
}: {
  uni: string;
  dept: string;
  courses: CourseSummary[];
  info: DepartmentInfo;
  base: string;
}) {
  const look = useMemo(() => lookups(info), [info]);
  const { selected, toggle, setMany, typed, setTyped, other, setOther, clear, loaded } = useCalculatorSelection(`ects-calculator:${uni}/${dept}`);
  const [filter, setFilter] = useState("");
  const [result, setResult] = useState<CalculatorResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const names = useMemo(() => new Map(courses.map((c) => [c.code, t(c.name)])), [courses]);
  const known = useMemo(() => new Set(courses.map((c) => c.code)), [courses]);
  const byCode = useMemo(() => new Map(courses.map((c) => [c.code, c])), [courses]);

  const groups = useMemo<Group[]>(() => {
    const q = fold(filter.trim());
    const visible = courses.filter((c) => !q || fold(`${c.code} ${t(c.name)}`).includes(q));
    const bySemester = new Map<number, CourseSummary[]>();
    const free: CourseSummary[] = [];
    for (const c of visible) {
      // A course offered in two semesters is listed once, under the first.
      if (c.semesters.length === 0) free.push(c);
      else {
        const first = Math.min(...c.semesters);
        bySemester.set(first, [...(bySemester.get(first) ?? []), c]);
      }
    }
    const out: Group[] = [...bySemester.entries()]
      .sort(([a], [b]) => a - b)
      .map(([n, list]) => ({ key: `s${n}`, title: `${look.semesterLabel(n)} εξάμηνο`, courses: list }));
    if (free.length) out.push({ key: "free", title: "Ελεύθερες επιλογές (ο οδηγός δεν αναφέρει ECTS· συμπληρώστε τα εσείς)", courses: free });
    return out;
  }, [courses, filter, look]);

  // Codes remembered from an older visit may no longer exist; only count the ones that do.
  const chosen = [...selected].filter((c) => known.has(c));

  const mandatoryCodes = useMemo(() => {
    const first = info.levels.find((l) => l.number === 1);
    return first ? courses.filter((c) => c.level === first.id).map((c) => c.code) : [];
  }, [courses, info]);

  // Only the newest request may update the screen, so a slow older answer cannot overwrite a newer one.
  const latestRequest = useRef(0);

  async function run() {
    // ECTS typed for ticked courses the guide gives none for, plus the free "other ECTS" amount.
    const overrides: Record<string, number> = {};
    let invalid = false;
    for (const code of chosen) {
      const course = byCode.get(code);
      if (!course || course.ects !== undefined) continue;
      const v = parseEcts(typed[code], 60, false);
      if (v === null) invalid = true;
      else if (v !== undefined) overrides[code] = v;
    }
    const otherEcts = parseEcts(other, 240, true);
    if (otherEcts === null) invalid = true;
    if (invalid) {
      // Keep showing the last good result; just say what is wrong with the number being typed.
      setError("Ελέγξτε τα ECTS που πληκτρολογήσατε: χρησιμοποιήστε αριθμό (π.χ. 6 ή 7,5).");
      return;
    }
    setError(null);

    const id = ++latestRequest.current;
    setBusy(true);
    try {
      const answer = await calculateEcts(uni, dept, chosen, overrides, otherEcts ?? 0);
      if (id === latestRequest.current) setResult(answer);
    } catch (e) {
      if (id === latestRequest.current) setError(e instanceof Error ? e.message : "Ο υπολογισμός δεν ήταν δυνατός.");
    } finally {
      if (id === latestRequest.current) setBusy(false);
    }
  }

  // The result follows the inputs: any change (ticking a course, typing ECTS) recalculates shortly after the last keystroke,
  // so the total on screen is never out of date.
  const inputKey = JSON.stringify([[...chosen].sort(), chosen.map((c) => typed[c] ?? ""), other]);
  useEffect(() => {
    if (!loaded) return;
    if (chosen.length === 0 && other.trim() === "") {
      latestRequest.current++;
      setResult(null);
      setError(null);
      setBusy(false);
      return;
    }
    const timer = setTimeout(() => void run(), 350);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputKey, loaded]);

  return (
    <div className="calc-layout">
      <section aria-label="Επιλογή μαθημάτων">
        <h2 style={{ marginTop: 0 }}>1. Επιλέξτε τα μαθήματα που έχετε περάσει</h2>
        <label className="visually-hidden" htmlFor="calc-filter">Αναζήτηση μαθήματος</label>
        <input id="calc-filter" type="search" placeholder="Αναζήτηση με όνομα ή κωδικό…" value={filter} onChange={(e) => setFilter(e.target.value)} />
        <p className="small" style={{ margin: "8px 0" }}>
          <button type="button" onClick={() => setMany(mandatoryCodes, true)}>Επιλογή όλων των υποχρεωτικών (ΥΠ)</button>{" "}
          <button type="button" onClick={clear} disabled={chosen.length === 0 && other === "" && Object.keys(typed).length === 0}>Καθαρισμός</button>
        </p>

        <div className="calc-picker">
          {groups.length === 0 ? <p className="muted">Κανένα μάθημα δεν ταιριάζει με την αναζήτηση.</p> : null}
          {groups.map((g) => {
            const codes = g.courses.map((c) => c.code);
            const allOn = codes.every((c) => selected.has(c));
            return (
              <div key={g.key}>
                <h3>
                  <span>{g.title}</span>
                  <button type="button" onClick={() => setMany(codes, !allOn)}>{allOn ? "Αποεπιλογή" : "Επιλογή όλων"}</button>
                </h3>
                {g.courses.map((c) => (
                  <label key={c.code} className="course-pick">
                    <input type="checkbox" checked={selected.has(c.code)} onChange={() => toggle(c.code)} />
                    <span className="code">{c.code}</span>
                    <span>
                      {t(c.name)} <LevelBadge level={look.level(c.level)} />
                      {c.semesters.length > 1 ? <span className="muted small"> ({look.semestersLabel(c.semesters)})</span> : null}
                    </span>
                    <span className="ects">
                      {c.ects !== undefined ? (
                        `${fmt(c.ects)} ECTS`
                      ) : selected.has(c.code) ? (
                        // The guide gives no ECTS for this course, so the user types them.
                        <input
                          className="ects-input"
                          type="text"
                          inputMode="decimal"
                          placeholder="ECTS"
                          aria-label={`ECTS για το μάθημα ${t(c.name)}`}
                          value={typed[c.code] ?? ""}
                          onChange={(e) => setTyped(c.code, e.target.value)}
                          onClick={(e) => e.stopPropagation()}
                        />
                      ) : (
                        "–"
                      )}
                    </span>
                  </label>
                ))}
              </div>
            );
          })}
        </div>

        <div className="other-ects">
          <label htmlFor="other-ects">Άλλα ECTS που δεν υπάρχουν στη λίστα</label>
          <input
            id="other-ects"
            type="text"
            inputMode="decimal"
            placeholder="0"
            value={other}
            onChange={(e) => setOther(e.target.value)}
          />
          <div className="small muted">
            π.χ. Πρακτική Άσκηση, μάθημα ξένης γλώσσας ή μάθημα άλλου τμήματος που δεν αναφέρεται. Προσμετρώνται μόνο στο σύνολο των ECTS, όχι στα ECTS ΕΠ + Ε.
          </div>
        </div>

        <div className="sticky-actions">
          <span className="muted small">{chosen.length} επιλεγμένα μαθήματα · το αποτέλεσμα ενημερώνεται αυτόματα</span>
        </div>
        {error ? <p className="notice bad" role="alert">{error}</p> : null}
      </section>

      <section aria-label="Αποτέλεσμα">
        <h2 style={{ marginTop: 0 }}>
          2. Αποτέλεσμα{busy ? <span className="muted small"> (ενημερώνεται…)</span> : null}
        </h2>
        {result ? (
          <EctsResultPanel result={result} info={info} base={base} names={names} />
        ) : (
          <p className="muted">
            Επιλέξτε τα μαθήματα που έχετε περάσει: το αποτέλεσμα εμφανίζεται εδώ και ενημερώνεται αυτόματα. Οι επιλογές σας διατηρούνται σε αυτόν τον περιηγητή.
          </p>
        )}
      </section>
    </div>
  );
}
