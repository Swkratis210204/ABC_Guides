import Link from "next/link";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { api } from "@/lib/api";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export const metadata = { title: "Διδάσκοντες" };

export default async function FacultyHub({ params }: { params: Promise<{ uni: string; dept: string }> }) {
  const { uni, dept } = await params;
  const [info, edip] = await Promise.all([api.department(uni, dept), api.faculty(uni, dept, { category: "EDIP" })]);
  const base = departmentBase(uni, dept);
  const depTotal = info.sectors.reduce((n, s) => n + s.memberCount, 0);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Διδάσκοντες" },
        ]}
      />
      <h1>Διδάσκοντες</h1>
      <p className="muted">{depTotal} μέλη ΔΕΠ σε {info.sectors.length} ερευνητικούς τομείς, και {edip.length} μέλη Ε.ΔΙ.Π.</p>

      <h2>Ερευνητικοί τομείς</h2>
      <div className="card-grid">
        {info.sectors.map((s) => (
          <Link key={s.id} href={`${base}/faculty/sector/${encodeURIComponent(s.id)}`} className="card">
            <div className="card-title">{t(s.name)}</div>
            <div className="card-meta">{s.memberCount} μέλη</div>
            {s.topics ? <div className="card-meta">{s.topics}</div> : null}
          </Link>
        ))}
      </div>

      <h2>Εργαστηριακό Διδακτικό Προσωπικό</h2>
      <div className="card-grid">
        <Link href={`${base}/faculty/edip`} className="card">
          <div className="card-title">Ε.ΔΙ.Π.</div>
          <div className="card-meta">{edip.length} μέλη</div>
        </Link>
      </div>
    </>
  );
}
