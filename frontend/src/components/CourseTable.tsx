import Link from "next/link";
import { fmt, lookups, t } from "@/lib/labels";
import type { CourseSummary, DepartmentInfo } from "@/lib/types";
import { LevelBadge } from "./LevelBadge";

export type ShowColumn = "semester" | "level" | "cycles";

/**
 * A list of courses. Which extra columns appear depends on the view: a column that merely repeats the page's own filter
 * (the semester column on a semester page) is left out and shown as a group heading instead.
 *
 * @param excludeCycle  on a cycle page, the cycle being viewed: the "cycles" column then lists only the OTHER cycles
 * @param semesterTag   mark courses offered in two semesters (Ζ΄/Η΄) next to the name, for views without a semester column
 */
export function CourseTable({
  courses,
  info,
  base,
  show = ["semester", "level"],
  excludeCycle,
  semesterTag = false,
}: {
  courses: CourseSummary[];
  info: DepartmentInfo;
  base: string;
  show?: ShowColumn[];
  excludeCycle?: string;
  semesterTag?: boolean;
}) {
  const look = lookups(info);
  if (courses.length === 0) return <p className="muted">Δεν υπάρχουν μαθήματα σε αυτή την κατηγορία.</p>;

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Κωδ.</th>
            <th>Μάθημα</th>
            <th className="num">ECTS</th>
            {show.includes("semester") ? <th>Εξάμηνο</th> : null}
            {show.includes("level") ? <th>Επίπεδο</th> : null}
            {show.includes("cycles") ? <th>{excludeCycle ? "Μετρά και στους κύκλους" : "Κύκλοι"}</th> : null}
          </tr>
        </thead>
        <tbody>
          {courses.map((c) => {
            const cycles = c.cycles.filter((id) => id !== excludeCycle);
            return (
              <tr key={c.code}>
                <td>{c.code}</td>
                <td>
                  <Link href={`${base}/courses/${encodeURIComponent(c.code)}`}>{t(c.name)}</Link>
                  {c.offeredBy ? <span className="muted small"> · {c.offeredBy}</span> : null}
                  {c.language === "en" ? <span className="badge gray" style={{ marginLeft: 8 }}>EN</span> : null}
                  {semesterTag && c.semesters.length > 1 ? (
                    <span className="badge gray" style={{ marginLeft: 8 }} title="Προσφέρεται σε ένα από τα δύο εξάμηνα">
                      {look.semestersLabel(c.semesters)}
                    </span>
                  ) : null}
                </td>
                <td className="num">{fmt(c.ects)}</td>
                {show.includes("semester") ? <td>{look.semestersLabel(c.semesters) || "–"}</td> : null}
                {show.includes("level") ? (
                  <td>
                    <LevelBadge level={look.level(c.level)} />
                  </td>
                ) : null}
                {show.includes("cycles") ? (
                  <td>
                    {cycles.length === 0 ? (
                      <span className="muted">–</span>
                    ) : (
                      <span className="cycle-badges">
                        {cycles.map((id) => {
                          const cy = look.cycle(id);
                          return (
                            <Link key={id} className="badge gray" href={`${base}/courses/cycle/${encodeURIComponent(id)}`} title={cy ? t(cy.name) : id}>
                              {cy?.number ?? id}
                            </Link>
                          );
                        })}
                      </span>
                    )}
                  </td>
                ) : null}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
