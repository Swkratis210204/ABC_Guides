import Link from "next/link";
import { t } from "@/lib/labels";
import type { DepartmentInfo } from "@/lib/types";

export type BrowseMode = "semester" | "level" | "cycle";

/**
 * The switcher shown at the top of every course list: first choose how to look at the courses (by semester, level
 * or cycle), then pick an option; the current choice is highlighted. It makes the three views feel like one place.
 */
export function BrowseNav({ base, info, mode, current }: { base: string; info: DepartmentInfo; mode: BrowseMode; current: string }) {
  const options: { id: string; label: string; count: number; title?: string }[] =
    mode === "semester"
      ? info.semesters.map((s) => ({ id: String(s.number), label: s.label, count: s.courseCount, title: `${s.label} εξάμηνο` }))
      : mode === "level"
        ? info.levels.map((l) => ({ id: l.id, label: l.short, count: l.courseCount, title: t(l.name) }))
        : info.cycles.map((c) => ({ id: c.id, label: String(c.number), count: c.courseCount, title: t(c.name) }));

  const first = {
    semester: info.semesters[0] && `${base}/courses/semester/${info.semesters[0].number}`,
    level: info.levels[0] && `${base}/courses/level/${encodeURIComponent(info.levels[0].id)}`,
    cycle: info.cycles[0] && `${base}/courses/cycle/${encodeURIComponent(info.cycles[0].id)}`,
  };
  const modes: { id: BrowseMode; label: string }[] = [
    { id: "semester", label: "Ανά εξάμηνο" },
    { id: "level", label: "Ανά επίπεδο" },
    { id: "cycle", label: "Ανά κύκλο" },
  ];

  return (
    <nav className="browse-nav" aria-label="Περιήγηση μαθημάτων">
      <div className="seg" role="group" aria-label="Τρόπος περιήγησης">
        {modes.map((m) => (
          <Link key={m.id} href={first[m.id] ?? `${base}/courses`} aria-current={m.id === mode ? "true" : undefined}>
            {m.label}
          </Link>
        ))}
      </div>
      <ul className="link-list">
        {options.map((o) => (
          <li key={o.id}>
            <Link
              className={`chip${o.id === current ? " active" : ""}`}
              href={`${base}/courses/${mode}/${encodeURIComponent(o.id)}`}
              aria-current={o.id === current ? "page" : undefined}
              title={o.title}
            >
              {o.label} <span className="count">{o.count}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
