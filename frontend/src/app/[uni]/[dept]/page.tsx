import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { SectionCard } from "@/components/SectionCard";
import { api } from "@/lib/api";
import { departmentBase, fmtDate, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

type Props = { params: Promise<{ uni: string; dept: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { uni, dept } = await params;
  const info = await api.department(uni, dept);
  return { title: t(info.department.name) };
}

export default async function DepartmentHome({ params }: Props) {
  const { uni, dept } = await params;
  const info = await api.department(uni, dept);
  const base = departmentBase(uni, dept);
  const x = info.info;

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name) },
        ]}
      />
      <h1>{t(info.department.name)}</h1>
      <p className="muted">
        {t(info.university.name)}
        {x.guideTitle ? ` · ${x.guideTitle}` : ""}
        {x.guideLastUpdated ? ` (τελευταία ενημέρωση ${fmtDate(x.guideLastUpdated)})` : ""}
      </p>

      <div className="card-grid">
        <SectionCard href={`${base}/courses`} title="Μαθήματα" meta={`${info.counts.courses} μαθήματα · ανά εξάμηνο, επίπεδο ή κύκλο`} />
        <SectionCard href={`${base}/faculty`} title="Διδάσκοντες" meta={`${info.counts.faculty} μέλη προσωπικού · ανά τομέα`} />
        <SectionCard href={`${base}/requirements`} title="Προϋποθέσεις πτυχίου" meta="Κανόνες και υπολογιστής ECTS" />
        <SectionCard href={`${base}/calendar`} title="Ακαδημαϊκό ημερολόγιο" meta="Εξάμηνα, εξεταστικές, αργίες" />
      </div>

      <h2>Στοιχεία τμήματος</h2>
      <div className="panel">
        <dl className="facts">
          {x.school ? (<><dt>Σχολή</dt><dd>{x.school}</dd></>) : null}
          {x.address ? (<><dt>Διεύθυνση</dt><dd>{x.address}</dd></>) : null}
          {x.phone ? (<><dt>Τηλέφωνο</dt><dd>{x.phone}</dd></>) : null}
          {x.email ? (<><dt>Email</dt><dd><a href={`mailto:${x.email}`}>{x.email}</a></dd></>) : null}
          {x.website ? (<><dt>Ιστότοπος</dt><dd><a href={x.website} rel="noopener noreferrer">{x.website}</a></dd></>) : null}
        </dl>
      </div>

      {x.administration?.length ? (
        <>
          <h2>Διοίκηση</h2>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Ρόλος</th><th>Όνομα</th></tr></thead>
              <tbody>
                {x.administration.map((a) => (
                  <tr key={a.role}><td>{a.role}</td><td>{a.name}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}

      {x.postgraduatePrograms?.length ? (
        <>
          <h2>Μεταπτυχιακά προγράμματα</h2>
          <ul>
            {x.postgraduatePrograms.map((p) => (<li key={p}>{p}</li>))}
          </ul>
        </>
      ) : null}
    </>
  );
}
