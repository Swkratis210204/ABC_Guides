import Link from "next/link";
import { redirect } from "next/navigation";
import { api } from "@/lib/api";
import { t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export default async function Home() {
  const universities = await api.universities();
  // With a single university there is nothing to choose.
  if (universities.length === 1) redirect(`/${encodeURIComponent(universities[0].slug)}`);

  return (
    <main className="container">
      <h1>Επιλέξτε πανεπιστήμιο</h1>
      <div className="card-grid">
        {universities.map((u) => (
          <Link key={u.slug} href={`/${encodeURIComponent(u.slug)}`} className="card">
            <div className="card-title">{t(u.name)}</div>
            {u.shortName ? <div className="card-meta">{u.shortName}</div> : null}
          </Link>
        ))}
      </div>
      {universities.length === 0 ? <p className="muted">Δεν υπάρχουν διαθέσιμα δεδομένα.</p> : null}
    </main>
  );
}
