import { SideNav } from "@/components/SideNav";
import { api } from "@/lib/api";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export default async function DepartmentLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ uni: string; dept: string }>;
}) {
  const { uni, dept } = await params;
  const info = await api.department(uni, dept);
  const base = departmentBase(uni, dept);

  // Each course view opens on its first option, so the three entries lead to three different pages.
  const courseModes = {
    semester: info.semesters[0] && `${base}/courses/semester/${info.semesters[0].number}`,
    level: info.levels[0] && `${base}/courses/level/${encodeURIComponent(info.levels[0].id)}`,
    cycle: info.cycles[0] && `${base}/courses/cycle/${encodeURIComponent(info.cycles[0].id)}`,
  };

  return (
    <div className="container">
      <div className="shell">
        <SideNav base={base} deptName={t(info.department.name)} courseModes={courseModes} />
        <main id="main">{children}</main>
      </div>
    </div>
  );
}
