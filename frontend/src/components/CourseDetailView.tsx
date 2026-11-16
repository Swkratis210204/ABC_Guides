import Link from "next/link";
import { fmt, lookups, t } from "@/lib/labels";
import type { CourseDetail, DepartmentInfo } from "@/lib/types";
import { LevelBadge } from "./LevelBadge";

/** Everything the guide says about one course. Fields the guide does not give are simply left out. */
export function CourseDetailView({ course, info, base }: { course: CourseDetail; info: DepartmentInfo; base: string }) {
  const look = lookups(info);
  const level = look.level(course.level);
  const courseHref = (code: string) => `${base}/courses/${encodeURIComponent(code)}`;
  const legend = info.info.offeredByLegend ?? {};

  return (
    <>
      <h1>{t(course.name)}</h1>
      <p>
        <LevelBadge level={level} /> <span className="muted">Κωδικός {course.code}</span>
        {course.language === "en" ? <span className="badge gray" style={{ marginLeft: 8 }}>Διδάσκεται στα Αγγλικά</span> : null}
      </p>

      <div className="panel">
        <dl className="facts">
          <dt>ECTS</dt>
          <dd>{course.ects !== undefined ? fmt(course.ects) : <span className="muted">Δεν αναφέρονται στον οδηγό</span>}</dd>

          {course.semesters.length > 0 ? (
            <>
              <dt>Εξάμηνο</dt>
              <dd>
                {course.semesters.map((n, i) => (
                  <span key={n}>
                    {i > 0 ? " ή " : ""}
                    <Link href={`${base}/courses/semester/${n}`}>{look.semesterLabel(n)}</Link>
                  </span>
                ))}{" "}
                εξάμηνο
              </dd>
            </>
          ) : null}

          <dt>Επίπεδο</dt>
          <dd>
            <Link href={`${base}/courses/level/${encodeURIComponent(course.level)}`}>{level ? t(level.name) : course.level}</Link>
          </dd>

          {course.offeredBy ? (
            <>
              <dt>Προσφέρεται από</dt>
              <dd>{legend[course.offeredBy] ? `${legend[course.offeredBy]} (${course.offeredBy})` : course.offeredBy}</dd>
            </>
          ) : null}

          <dt>Κύκλοι σπουδών</dt>
          <dd>
            {course.cycles.length > 0 ? (
              <ul className="link-list" style={{ margin: 0 }}>
                {course.cycles.map((id) => {
                  const c = look.cycle(id);
                  return (
                    <li key={id}>
                      <Link className="chip" href={`${base}/courses/cycle/${encodeURIComponent(id)}`}>
                        {c ? `${c.number}. ${t(c.name)}` : id}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <span className="muted">{course.cyclesNote ? `Κανένας (${course.cyclesNote})` : "Κανένας"}</span>
            )}
          </dd>

          <dt>Προαπαιτούμενα</dt>
          <dd>
            {course.prerequisites.length === 0 ? (
              <span className="muted">Κανένα</span>
            ) : (
              <>
                <ul style={{ margin: 0, paddingLeft: "1.1rem" }}>
                  {course.prerequisites.map((group, gi) => (
                    <li key={gi}>
                      {group.map((p, i) => (
                        <span key={p.code}>
                          {i > 0 ? " ή " : ""}
                          <Link href={courseHref(p.code)}>{p.name ? t(p.name) : p.code}</Link>
                        </span>
                      ))}
                    </li>
                  ))}
                </ul>
                <div className="muted small">Απαιτείται κάθε γραμμή· όπου αναγράφεται «ή» αρκεί ένα από τα μαθήματα.</div>
              </>
            )}
            {course.prerequisitesText ? (
              <div className="muted small">Όπως αναγράφεται στον οδηγό: «{course.prerequisitesText}»</div>
            ) : null}
          </dd>

          <dt>Διδάσκων</dt>
          <dd>
            {course.instructors.length === 0 ? (
              <span className="muted">Δεν αναφέρεται στον οδηγό</span>
            ) : (
              course.instructors.map((i, idx) => (
                <span key={i.name}>
                  {idx > 0 ? ", " : ""}
                  {i.facultyId ? <Link href={`${base}/faculty/${encodeURIComponent(i.facultyId)}`}>{i.name}</Link> : i.name}
                </span>
              ))
            )}
          </dd>

          {course.aliases.length > 0 ? (
            <>
              <dt>Συντομογραφίες</dt>
              <dd>{course.aliases.join(", ")}</dd>
            </>
          ) : null}
        </dl>

        {course.eclassUrl ? (
          <p>
            <a className="button primary" href={course.eclassUrl} target="_blank" rel="noopener noreferrer">
              Άνοιγμα στο eClass
            </a>
          </p>
        ) : null}
      </div>

      {course.description ? (
        <>
          <h2>Σύντομη περιγραφή</h2>
          <p>{t(course.description)}</p>
        </>
      ) : null}

      {course.requiredBy.length > 0 ? (
        <>
          <h2>Απαιτείται για</h2>
          <ul className="link-list">
            {course.requiredBy.map((r) => (
              <li key={r.code}>
                <Link className="chip" href={courseHref(r.code)}>
                  {r.name ? t(r.name) : r.code}
                </Link>
              </li>
            ))}
          </ul>
        </>
      ) : null}
    </>
  );
}
