import { Breadcrumbs } from "@/components/Breadcrumbs";
import { EctsCalculatorForm } from "@/components/EctsCalculatorForm";
import { api } from "@/lib/api";
import { departmentBase, t } from "@/lib/labels";

export const dynamic = "force-dynamic";

export const metadata = { title: "Υπολογιστής ECTS" };

export default async function CalculatorPage({ params }: { params: Promise<{ uni: string; dept: string }> }) {
  const { uni, dept } = await params;
  const [info, courses] = await Promise.all([api.department(uni, dept), api.courses(uni, dept)]);
  const base = departmentBase(uni, dept);

  return (
    <>
      <Breadcrumbs
        items={[
          { label: info.university.shortName ?? t(info.university.name), href: `/${encodeURIComponent(uni)}` },
          { label: t(info.department.name), href: base },
          { label: "Προϋποθέσεις πτυχίου", href: `${base}/requirements` },
          { label: "Υπολογιστής ECTS" },
        ]}
      />
      <h1>Υπολογιστής ECTS</h1>
      <p className="muted">
        Δηλώστε τα μαθήματα που έχετε περάσει και δείτε πόσα ECTS σας λείπουν, ποια υποχρεωτικά μαθήματα απομένουν και ποιους κύκλους σπουδών μπορείτε να ολοκληρώσετε.
      </p>
      <EctsCalculatorForm uni={uni} dept={dept} courses={courses} info={info} base={base} />
    </>
  );
}
