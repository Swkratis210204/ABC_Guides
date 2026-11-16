import Link from "next/link";

export interface Crumb {
  label: string;
  href?: string;
}

/** The trail University › Department › Section › Item › Detail. The last crumb is the current page and is not a link. */
export function Breadcrumbs({ items }: { items: Crumb[] }) {
  return (
    <nav className="breadcrumbs" aria-label="Διαδρομή πλοήγησης">
      <ol>
        {items.map((c, i) => (
          <li key={`${i}-${c.label}`} aria-current={i === items.length - 1 ? "page" : undefined}>
            {c.href && i < items.length - 1 ? <Link href={c.href}>{c.label}</Link> : c.label}
          </li>
        ))}
      </ol>
    </nav>
  );
}
