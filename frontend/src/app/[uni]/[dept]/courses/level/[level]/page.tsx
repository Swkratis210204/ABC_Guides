import { notFound } from "next/navigation";
import { BrowseNav } from "@/components/BrowseNav";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { CourseGroups } from "@/components/CourseGroups";
import { LevelBadge } from "@/components/LevelBadge";
import { api } from "@/lib/api";
import { groupBySemester, rulesForLevel, totals } from "@/lib/grouping";
import { departmentBase, fmt, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

/** "What belongs to this level, and what does the department require of it?": courses by semester, plus the relevant rules. */
export default async function LevelPage({ params }: { params: Promise<{ uni: string; dept: string; level: string }> }) {
  const { uni, dept, level: levelId } = await params;
  const info = await api.department(uni, dept);
  const level = info.levels.find((l) => l.id === levelId);
  if (!level) notFound();

  const [courses, requirements] = await Promise.all([api.courses(uni, dept, { level: levelId }), api.requirements(uni, dept)]);
  const base = departmentBase(uni, dept);
  const sum = totals(courses);
  const rules = rulesForLevel(requirements, levelId, info);
  const isFree = levelId === "FREE";

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Μαθήματα", href: `${base}/courses` },
          { label: t(level.name) },
        ]}
      />
      <h1>
        <LevelBadge level={level} /> {t(level.name)}
      </h1>
      <BrowseNav base={base} info={info} mode="level" current={levelId} />
      <p className="muted">
        Επίπεδο {level.number} · {sum.count} μαθήματα{sum.ects > 0 ? ` · ${fmt(sum.ects)} ECTS συνολικά` : ""}
      </p>

      {rules.length > 0 ? (
        <div className="rule-note">
          <strong>Τι απαιτείται από αυτό το επίπεδο</strong>
          {rules.map((r) => (
            <p key={r.id}>{t(r.description)}</p>
          ))}
        </div>
      ) : null}

      {isFree ? (
        <div className="notice info">
          {info.info.freeElectivesNote}
          <br />
          Για τα μαθήματα άλλων τμημάτων ο οδηγός δεν αναφέρει ECTS.
        </div>
      ) : null}

      <CourseGroups groups={groupBySemester(courses, info)} info={info} base={base} show={["cycles"]} semesterTag />

      {isFree && info.info.freeElectivesFromDepartment?.length ? (
        <>
          <h2>Ελεύθερες επιλογές από το Τμήμα Πληροφορικής</h2>
          <ul>
            {info.info.freeElectivesFromDepartment.map((x) => (
              <li key={x}>{x}</li>
            ))}
          </ul>
        </>
      ) : null}
    </>
  );
}
