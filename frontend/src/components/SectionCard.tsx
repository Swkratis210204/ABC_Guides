import Link from "next/link";

export function SectionCard({ href, title, meta }: { href: string; title: string; meta?: string }) {
  return (
    <Link href={href} className="card">
      <div className="card-title">{title}</div>
      {meta ? <div className="card-meta">{meta}</div> : null}
    </Link>
  );
}
