import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "Πληροφορίες Τμημάτων", template: "%s · Πληροφορίες Τμημάτων" },
  description: "Περιήγηση στις πληροφορίες των τμημάτων: μαθήματα, διδάσκοντες, προϋποθέσεις πτυχίου και ακαδημαϊκό ημερολόγιο.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="el">
      <body>
        <header className="site-header">
          <div className="inner">
            <Link href="/" className="site-brand">
              <img src="/logo.png" alt="ABC Aueb Gen" width={44} height={44} className="site-logo" />
              <span className="site-title">Πληροφορίες Τμημάτων</span>
            </Link>
            <span className="site-sub">Μαθήματα · Διδάσκοντες · Προϋποθέσεις πτυχίου · Ημερολόγιο</span>
          </div>
        </header>
        {children}
      </body>
    </html>
  );
}
