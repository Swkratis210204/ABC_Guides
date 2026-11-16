import { fmt } from "@/lib/labels";
import { totals, type CourseGroup } from "@/lib/grouping";
import type { DepartmentInfo } from "@/lib/types";
import { CourseTable, type ShowColumn } from "./CourseTable";
import { LevelBadge } from "./LevelBadge";

/** Course lists split into titled sections, each with its course count and ECTS total. */
export function CourseGroups({
  groups,
  info,
  base,
  show,
  excludeCycle,
  semesterTag,
}: {
  groups: CourseGroup[];
  info: DepartmentInfo;
  base: string;
  show: ShowColumn[];
  excludeCycle?: string;
  semesterTag?: boolean;
}) {
  if (groups.length === 0) return <p className="muted">Δεν υπάρχουν μαθήματα σε αυτή την κατηγορία.</p>;

  return (
    <>
      {groups.map((g) => {
        const sum = totals(g.courses);
        return (
          <section key={g.key} aria-labelledby={`g-${g.key}`}>
            <h2 id={`g-${g.key}`} className="group-title">
              {g.level ? <LevelBadge level={g.level} /> : null} {g.title}
              <span className="group-meta">
                {sum.count} {sum.count === 1 ? "μάθημα" : "μαθήματα"}
                {sum.ects > 0 ? ` · ${fmt(sum.ects)} ECTS` : ""}
                {sum.unknownEcts > 0 && sum.ects > 0 ? ` (+${sum.unknownEcts} χωρίς γνωστά ECTS)` : ""}
              </span>
            </h2>
            <CourseTable courses={g.courses} info={info} base={base} show={show} excludeCycle={excludeCycle} semesterTag={semesterTag} />
          </section>
        );
      })}
    </>
  );
}
