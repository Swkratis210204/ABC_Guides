import { t } from "@/lib/labels";
import type { QaPage } from "@/lib/types";

/** Questions grouped by section; each question opens to its answer (native <details>, works without JavaScript). */
export function QaSections({ page }: { page: QaPage }) {
  if (page.sections.length === 0) {
    return <p className="muted">Δεν υπάρχουν ακόμη διαθέσιμες πληροφορίες.</p>;
  }
  return (
    <>
      {page.sections.map((s) => (
        <section key={s.id} aria-labelledby={`qa-${s.id}`}>
          <h2 id={`qa-${s.id}`}>{t(s.title)}</h2>
          <div className="qa-list">
            {s.items.map((it) => (
              <details key={it.id} className="qa-item">
                <summary>{t(it.question)}</summary>
                <div className="qa-answer">
                  <p>{t(it.answer)}</p>
                  {it.note ? <p className="qa-note muted">{t(it.note)}</p> : null}
                  {it.links?.length ? (
                    <p>
                      {it.links.map((l, i) => (
                        <span key={l.url}>
                          {i > 0 ? " · " : ""}
                          <a href={l.url} rel="noopener noreferrer">{l.label}</a>
                        </span>
                      ))}
                    </p>
                  ) : null}
                </div>
              </details>
            ))}
          </div>
        </section>
      ))}
    </>
  );
}
