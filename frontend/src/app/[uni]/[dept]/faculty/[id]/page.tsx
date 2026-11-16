import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { api } from "@/lib/api";
import { CATEGORY_LABEL, STATUS_LABEL, departmentBase, lookups, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

type Props = { params: Promise<{ uni: string; dept: string; id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { uni, dept, id } = await params;
  return { title: t((await api.facultyMember(uni, dept, id)).name) };
}

export default async function FacultyMemberPage({ params }: Props) {
  const { uni, dept, id } = await params;
  const [info, member] = await Promise.all([api.department(uni, dept), api.facultyMember(uni, dept, id)]);
  const base = departmentBase(uni, dept);
  const look = lookups(info);
  const sector = look.sector(member.sector);

  const parent = sector
    ? { label: t(sector.name), href: `${base}/faculty/sector/${encodeURIComponent(sector.id)}` }
    : { label: "Ε.ΔΙ.Π.", href: `${base}/faculty/edip` };

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Διδάσκοντες", href: `${base}/faculty` },
          parent,
          { label: t(member.name) },
        ]}
      />
      <h1>{t(member.name)}</h1>
      {STATUS_LABEL[member.status] ? (
        <p>
          <span className="badge warn">{STATUS_LABEL[member.status]}</span>
          {member.statusNote ? <span className="muted"> ({member.statusNote})</span> : null}
        </p>
      ) : null}

      <div className="panel">
        <dl className="facts">
          <dt>Κατηγορία</dt>
          <dd>{CATEGORY_LABEL[member.category]}</dd>
          {member.rank ? (<><dt>Βαθμίδα</dt><dd>{look.rankLabel(member.rank)}</dd></>) : null}
          {sector ? (
            <>
              <dt>Τομέας</dt>
              <dd><Link href={`${base}/faculty/sector/${encodeURIComponent(sector.id)}`}>{t(sector.name)}</Link></dd>
            </>
          ) : null}
          {member.email ? (<><dt>Email</dt><dd><a href={`mailto:${member.email}`}>{member.email}</a></dd></>) : null}
          {member.homepageUrl ? (
            <>
              <dt>Ιστοσελίδα</dt>
              <dd><a href={member.homepageUrl} target="_blank" rel="noopener noreferrer">{member.homepageUrl}</a></dd>
            </>
          ) : null}
        </dl>
      </div>

      <h2>Μαθήματα που διδάσκει</h2>
      {member.courses.length === 0 ? (
        <p className="muted">Ο οδηγός δεν αναφέρει μαθήματα για αυτό το μέλος.</p>
      ) : (
        <ul className="link-list">
          {member.courses.map((c) => (
            <li key={c.code}>
              <Link className="chip" href={`${base}/courses/${encodeURIComponent(c.code)}`}>
                {t(c.name)}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
