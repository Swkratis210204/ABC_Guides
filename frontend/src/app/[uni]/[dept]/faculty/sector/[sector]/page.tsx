import { notFound } from "next/navigation";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { FacultyList } from "@/components/FacultyList";
import { api } from "@/lib/api";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export default async function SectorPage({ params }: { params: Promise<{ uni: string; dept: string; sector: string }> }) {
  const { uni, dept, sector: sectorId } = await params;
  const info = await api.department(uni, dept);
  const sector = info.sectors.find((s) => s.id === sectorId);
  if (!sector) notFound();

  const members = await api.faculty(uni, dept, { sector: sectorId });
  const base = departmentBase(uni, dept);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Διδάσκοντες", href: `${base}/faculty` },
          { label: t(sector.name) },
        ]}
      />
      <h1>{t(sector.name)}</h1>
      {sector.topics ? <p className="muted">{sector.topics}</p> : null}
      <FacultyList members={members} info={info} base={base} />
    </>
  );
}
