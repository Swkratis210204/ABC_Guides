import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { CourseDetailView } from "@/components/CourseDetailView";
import { api } from "@/lib/api";
import { departmentBase, lookups, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

type Props = { params: Promise<{ uni: string; dept: string; code: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { uni, dept, code } = await params;
  const course = await api.course(uni, dept, decodeURIComponent(code));
  return { title: t(course.name) };
}

export default async function CoursePage({ params }: Props) {
  const { uni, dept, code } = await params;
  const [info, course] = await Promise.all([api.department(uni, dept), api.course(uni, dept, decodeURIComponent(code))]);
  const base = departmentBase(uni, dept);
  const look = lookups(info);

  // The trail follows how a student most likely arrived: semester courses sit under their semester,
  // free electives (no semester) under their level.
  const parent =
    course.semesters.length > 0
      ? { label: `${look.semesterLabel(course.semesters[0])} εξάμηνο`, href: `${base}/courses/semester/${course.semesters[0]}` }
      : { label: t(look.level(course.level)?.name), href: `${base}/courses/level/${encodeURIComponent(course.level)}` };

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Μαθήματα", href: `${base}/courses` },
          parent,
          { label: t(course.name) },
        ]}
      />
      <CourseDetailView course={course} info={info} base={base} />
    </>
  );
}
