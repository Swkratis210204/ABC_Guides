import { Breadcrumbs } from "@/components/Breadcrumbs";
import { FacultyList } from "@/components/FacultyList";
import { api } from "@/lib/api";
import { CATEGORY_LABEL, departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export default async function EdipPage({ params }: { params: Promise<{ uni: string; dept: string }> }) {
  const { uni, dept } = await params;
  const [info, members] = await Promise.all([api.department(uni, dept), api.faculty(uni, dept, { category: "EDIP" })]);
  const base = departmentBase(uni, dept);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Διδάσκοντες", href: `${base}/faculty` },
          { label: "Ε.ΔΙ.Π." },
        ]}
      />
      <h1>{CATEGORY_LABEL.EDIP}</h1>
      <FacultyList members={members} info={info} base={base} />
    </>
  );
}
