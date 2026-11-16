import Link from "next/link";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { LevelBadge } from "@/components/LevelBadge";
import { api } from "@/lib/api";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export const metadata = { title: "Μαθήματα" };

export default async function CoursesHub({ params }: { params: Promise<{ uni: string; dept: string }> }) {
  const { uni, dept } = await params;
  const info = await api.department(uni, dept);
  const base = departmentBase(uni, dept);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Μαθήματα" },
        ]}
      />
      <h1>Μαθήματα</h1>
      <p className="muted">Επιλέξτε πώς θέλετε να περιηγηθείτε στα {info.counts.courses} μαθήματα του τμήματος.</p>

      <h2 id="semester">Ανά εξάμηνο</h2>
      <ul className="link-list">
        {info.semesters.map((s) => (
          <li key={s.number}>
            <Link className="chip" href={`${base}/courses/semester/${s.number}`}>
              {s.label} εξάμηνο <span className="count">{s.courseCount}</span>
            </Link>
          </li>
        ))}
      </ul>
      <p className="muted small">Τα μαθήματα ελεύθερης επιλογής (ΕΛ) δεν ανήκουν σε συγκεκριμένο εξάμηνο· βρείτε τα στο «Ανά επίπεδο».</p>

      <h2 id="level">Ανά επίπεδο</h2>
      <ul className="link-list">
        {info.levels.map((l) => (
          <li key={l.id}>
            <Link className="chip" href={`${base}/courses/level/${encodeURIComponent(l.id)}`}>
              <LevelBadge level={l} /> {t(l.name)} <span className="count">{l.courseCount}</span>
            </Link>
          </li>
        ))}
      </ul>

      <h2 id="cycle">Ανά κύκλο σπουδών</h2>
      <ul className="link-list">
        {info.cycles.map((c) => (
          <li key={c.id}>
            <Link className="chip" href={`${base}/courses/cycle/${encodeURIComponent(c.id)}`}>
              {c.number}. {t(c.name)} <span className="count">{c.courseCount}</span>
            </Link>
          </li>
        ))}
      </ul>
      <p className="muted small">Οι κύκλοι σπουδών είναι προαιρετικοί και αναγράφονται στο πτυχίο αν ολοκληρωθούν (τουλάχιστον 5 μαθήματα του κύκλου).</p>
    </>
  );
}
