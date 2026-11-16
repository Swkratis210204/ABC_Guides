"use client";

import { usePathname } from "next/navigation";

// Where reports go. Override at build time with NEXT_PUBLIC_ISSUES_URL (e.g. for a fork or another tenant).
const ISSUES_URL = process.env.NEXT_PUBLIC_ISSUES_URL ?? "https://github.com/Swkratis210204/ABC_Guides/issues/new";

/** A link that opens a pre-filled GitHub issue (the "Λάθος σε πληροφορία" form) naming the page it was clicked on. */
export function ReportIssue() {
  const pathname = usePathname();
  const q = new URLSearchParams({ template: "data-error.yml", title: `[Λάθος] ${pathname}`, page: pathname, labels: "data-error" });
  return (
    <p className="report-issue">
      Βρήκατε λάθος σε αυτή τη σελίδα;{" "}
      <a href={`${ISSUES_URL}?${q.toString()}`} target="_blank" rel="noopener noreferrer">
        Αναφορά λάθους στο GitHub
      </a>
    </p>
  );
}
