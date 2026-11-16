import { Breadcrumbs } from "@/components/Breadcrumbs";
import { api } from "@/lib/api";
import { departmentBase, fmtDate, t } from "@/lib/labels";
import type { CalendarItem } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata = { title: "Ακαδημαϊκό ημερολόγιο" };

type Row = CalendarItem & { type: string };

export default async function CalendarPage({ params }: { params: Promise<{ uni: string; dept: string }> }) {
  const { uni, dept } = await params;
  const [info, cal] = await Promise.all([api.department(uni, dept), api.calendar(uni, dept)]);
  const base = departmentBase(uni, dept);

  const rows: Row[] = [
    ...cal.terms.map((x) => ({ ...x, type: "Εξάμηνο" })),
    ...cal.examPeriods.map((x) => ({ ...x, type: "Εξεταστική" })),
    ...cal.holidays.map((x) => ({ ...x, type: x.kind === "BREAK" ? "Διακοπές" : "Αργία" })),
  ].sort((a, b) => (a.start ?? "9999").localeCompare(b.start ?? "9999") || (a.end ?? "").localeCompare(b.end ?? ""));

  // "Today" in the server's calendar day; used only to highlight what is happening now.
  const today = new Date().toISOString().slice(0, 10);
  const isNow = (r: Row) => !!r.start && r.start <= today && today <= (r.end ?? r.start);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Ακαδημαϊκό ημερολόγιο" },
        ]}
      />
      <h1>Ακαδημαϊκό ημερολόγιο {cal.academicYear}</h1>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>Περίοδος</th><th>Είδος</th><th>Ημερομηνίες</th></tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={`${r.type}-${r.id}`} className={isNow(r) ? "current" : undefined}>
                <td>
                  {t(r.name)}
                  {isNow(r) ? <span className="badge warn" style={{ marginLeft: 8 }}>Τώρα</span> : null}
                </td>
                <td className="muted">{r.type}</td>
                <td>
                  {r.start ? (
                    r.end && r.end !== r.start ? `${fmtDate(r.start)} – ${fmtDate(r.end)}` : fmtDate(r.start)
                  ) : (
                    <span className="muted">{r.note ? t(r.note) : "Θα ανακοινωθεί"}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="timeline-note">Οι ημερομηνίες είναι όπως αναγράφονται στον οδηγό σπουδών. Ο οδηγός δίνει μόνο τις ημερομηνίες των αργιών· οι ονομασίες συμπληρώθηκαν από τις γνωστές εθνικές και θρησκευτικές εορτές.</p>
    </>
  );
}
