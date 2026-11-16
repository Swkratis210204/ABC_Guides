import { Breadcrumbs } from "@/components/Breadcrumbs";
import { QaSections } from "@/components/QaSections";
import { api } from "@/lib/api";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export const metadata = { title: "Συχνές ερωτήσεις" };

export default async function FaqPage({ params }: { params: Promise<{ uni: string; dept: string }> }) {
  const { uni, dept } = await params;
  const [info, page] = await Promise.all([api.department(uni, dept), api.faq(uni, dept)]);
  const base = departmentBase(uni, dept);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Συχνές ερωτήσεις" },
        ]}
      />
      <h1>Συχνές ερωτήσεις</h1>
      {page.intro ? <p className="muted">{t(page.intro)}</p> : null}
      <QaSections page={page} />
    </>
  );
}
