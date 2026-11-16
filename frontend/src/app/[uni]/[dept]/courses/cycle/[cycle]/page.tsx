import Link from "next/link";
import { notFound } from "next/navigation";
import { BrowseNav } from "@/components/BrowseNav";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { CourseGroups } from "@/components/CourseGroups";
import { api } from "@/lib/api";
import { groupByLevel, rulesOfKind } from "@/lib/grouping";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

/**
 * "Which courses make up this study cycle, and what else do they count for?" A course counts for only one cycle,
 * so each row shows the other cycles it could count for instead.
 */
export default async function CyclePage({ params }: { params: Promise<{ uni: string; dept: string; cycle: string }> }) {
  const { uni, dept, cycle: cycleId } = await params;
  const info = await api.department(uni, dept);
  const cycle = info.cycles.find((c) => c.id === cycleId);
  if (!cycle) notFound();

  const [courses, requirements] = await Promise.all([api.courses(uni, dept, { cycle: cycleId }), api.requirements(uni, dept)]);
  const base = departmentBase(uni, dept);
  const rule = rulesOfKind(requirements, "CYCLES")[0];
  const needed = typeof rule?.params.minCourses === "number" ? rule.params.minCourses : undefined;
  const shared = courses.filter((c) => c.cycles.length > 1).length;

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Μαθήματα", href: `${base}/courses` },
          { label: `Κύκλος ${cycle.number}: ${t(cycle.name)}` },
        ]}
      />
      <h1>
        Κύκλος {cycle.number}: {t(cycle.name)}
      </h1>
      <BrowseNav base={base} info={info} mode="cycle" current={cycleId} />
      <p className="muted">
        {courses.length} μαθήματα{needed ? ` · χρειάζονται τουλάχιστον ${needed} για να ολοκληρωθεί ο κύκλος` : ""}
        {shared > 0 ? ` · ${shared} από αυτά μπορούν να μετρήσουν και σε άλλον κύκλο` : ""}
      </p>

      {rule ? (
        <div className="rule-note">
          <p>{t(rule.description)}</p>
          <p className="small">
            Δείτε πόσους κύκλους μπορείτε να ολοκληρώσετε με τα μαθήματά σας στον{" "}
            <Link href={`${base}/requirements/calculator`}>υπολογιστή ECTS</Link>.
          </p>
        </div>
      ) : null}

      <CourseGroups groups={groupByLevel(courses, info)} info={info} base={base} show={["semester", "cycles"]} excludeCycle={cycleId} />
    </>
  );
}
