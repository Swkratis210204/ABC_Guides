import Link from "next/link";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { RequirementList } from "@/components/RequirementList";
import { api } from "@/lib/api";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export const metadata = { title: "Προϋποθέσεις πτυχίου" };

export default async function RequirementsPage({ params }: { params: Promise<{ uni: string; dept: string }> }) {
  const { uni, dept } = await params;
  const [info, requirements] = await Promise.all([api.department(uni, dept), api.requirements(uni, dept)]);
  const base = departmentBase(uni, dept);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Προϋποθέσεις πτυχίου" },
        ]}
      />
      <h1>Προϋποθέσεις πτυχίου</h1>
      <p className="muted">Συνολικά απαιτούνται τουλάχιστον {requirements.totalEcts} ECTS.</p>
      <p>
        <Link href={`${base}/requirements/calculator`} className="button primary">
          Υπολογιστής ECTS: δείτε τι σας λείπει
        </Link>
      </p>
      <RequirementList requirements={requirements} />
    </>
  );
}
