import { notFound } from "next/navigation";
import { BrowseNav } from "@/components/BrowseNav";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { CourseGroups } from "@/components/CourseGroups";
import { api } from "@/lib/api";
import { groupByLevel, totals } from "@/lib/grouping";
import { departmentBase, fmt, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

/** "What do I take this semester?": the semester's courses, split by level, with the cycles each one counts for. */
export default async function SemesterPage({ params }: { params: Promise<{ uni: string; dept: string; semester: string }> }) {
  const { uni, dept, semester } = await params;
  const number = Number(semester);
  const info = await api.department(uni, dept);
  const found = Number.isInteger(number) ? info.semesters.find((s) => s.number === number) : undefined;
  if (!found) notFound();

  const courses = await api.courses(uni, dept, { semester: number });
  const base = departmentBase(uni, dept);
  const sum = totals(courses);
  const either = courses.filter((c) => c.semesters.length > 1).length;

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Μαθήματα", href: `${base}/courses` },
          { label: `${found.label} εξάμηνο` },
        ]}
      />
      <h1>{found.label} εξάμηνο</h1>
      <BrowseNav base={base} info={info} mode="semester" current={semester} />
      <p className="muted">
        {sum.count} μαθήματα · {fmt(sum.ects)} ECTS αν τα παρακολουθήσετε όλα
        {either > 0 ? ` · ${either} από αυτά προσφέρονται και στο άλλο εξάμηνο (Ζ΄/Η΄)` : ""}
      </p>
      <CourseGroups groups={groupByLevel(courses, info)} info={info} base={base} show={["cycles"]} />
    </>
  );
}
