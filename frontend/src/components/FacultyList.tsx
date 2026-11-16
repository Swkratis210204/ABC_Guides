import Link from "next/link";
import { STATUS_LABEL, lookups, t } from "@/lib/labels";
import type { DepartmentInfo, FacultyView } from "@/lib/types";

export function FacultyList({ members, info, base }: { members: FacultyView[]; info: DepartmentInfo; base: string }) {
  const look = lookups(info);
  if (members.length === 0) return <p className="muted">Δεν υπάρχουν μέλη σε αυτή την κατηγορία.</p>;

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Όνομα</th>
            <th>Βαθμίδα</th>
            <th>Τομέας</th>
            <th className="num">Μαθήματα</th>
          </tr>
        </thead>
        <tbody>
          {members.map((m) => (
            <tr key={m.id}>
              <td>
                <Link href={`${base}/faculty/${encodeURIComponent(m.id)}`}>{t(m.name)}</Link>
                {STATUS_LABEL[m.status] ? <span className="badge warn" style={{ marginLeft: 8 }}>{STATUS_LABEL[m.status]}</span> : null}
              </td>
              <td>{look.rankLabel(m.rank) || <span className="muted">–</span>}</td>
              <td>{t(look.sector(m.sector)?.name) || <span className="muted">–</span>}</td>
              <td className="num">{m.courses.length || <span className="muted">–</span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
