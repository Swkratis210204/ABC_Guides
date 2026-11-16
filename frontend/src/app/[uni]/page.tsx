import Link from "next/link";
import { notFound } from "next/navigation";
import { Breadcrumbs } from "@/components/Breadcrumbs";
import { api } from "@/lib/api";
import { t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export default async function UniversityPage({ params }: { params: Promise<{ uni: string }> }) {
  const { uni } = await params;
  const [departments, universities] = await Promise.all([api.departments(uni), api.universities()]);
  const university = universities.find((u) => u.slug === uni);
  if (!university) notFound();

  return (
    <main className="container">
      <Breadcrumbs items={[{ label: university.shortName ?? t(university.name) }]} />
      <h1>{t(university.name)}</h1>
      <h2>Τμήματα</h2>
      <div className="card-grid">
        {departments.map((d) => (
          <Link key={d.slug} href={`/${encodeURIComponent(uni)}/${encodeURIComponent(d.slug)}`} className="card">
            <div className="card-title">{t(d.name)}</div>
          </Link>
        ))}
      </div>
    </main>
  );
}
