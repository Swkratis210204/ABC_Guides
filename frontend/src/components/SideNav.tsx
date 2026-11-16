"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

/** Where each "browse courses by…" entry leads: the first option of that view (a real page, not a spot on another page). */
export interface CourseModeLinks {
  semester?: string;
  level?: string;
  cycle?: string;
}

/** The department's section tree. A section is highlighted while any page below it is open. */
export function SideNav({ base, deptName, courseModes }: { base: string; deptName: string; courseModes: CourseModeLinks }) {
  const pathname = usePathname();
  const isCurrent = (href: string, exact = false) =>
    exact ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);

  const link = (href: string, label: string, exact = false) => (
    <Link href={href} aria-current={isCurrent(href, exact) ? "page" : undefined}>
      {label}
    </Link>
  );

  // The three course views are different pages, so each is highlighted while any page of that view is open.
  const mode = (kind: "semester" | "level" | "cycle", label: string) => {
    const target = courseModes[kind];
    if (!target) return null;
    return (
      <li>
        <Link href={target} aria-current={isCurrent(`${base}/courses/${kind}`) ? "page" : undefined}>
          {label}
        </Link>
      </li>
    );
  };

  return (
    <nav className="sidenav" aria-label="Ενότητες τμήματος">
      <div className="dept">{deptName}</div>
      <ul>
        <li>{link(base, "Αρχική τμήματος", true)}</li>
        <li>
          {link(`${base}/courses`, "Μαθήματα")}
          <ul>
            {mode("semester", "Ανά εξάμηνο")}
            {mode("level", "Ανά επίπεδο")}
            {mode("cycle", "Ανά κύκλο σπουδών")}
          </ul>
        </li>
        <li>{link(`${base}/faculty`, "Διδάσκοντες")}</li>
        <li>
          {link(`${base}/requirements`, "Προϋποθέσεις πτυχίου")}
          <ul>
            <li>{link(`${base}/requirements/calculator`, "Υπολογιστής ECTS")}</li>
          </ul>
        </li>
        <li>{link(`${base}/calendar`, "Ακαδημαϊκό ημερολόγιο")}</li>
        <li>{link(`${base}/internship`, "Πρακτική Άσκηση")}</li>
        <li>{link(`${base}/faq`, "Συχνές ερωτήσεις")}</li>
      </ul>
    </nav>
  );
}
