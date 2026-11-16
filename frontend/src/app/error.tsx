"use client";

export default function ErrorPage({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <main className="container">
      <h1>Κάτι πήγε στραβά</h1>
      <p className="notice bad">
        Δεν ήταν δυνατή η φόρτωση των δεδομένων. Ο διακομιστής μπορεί να ξεκινά ή να μην είναι διαθέσιμος αυτή τη στιγμή.
      </p>
      <p>
        <button className="primary" onClick={() => reset()}>
          Δοκιμάστε ξανά
        </button>
      </p>
      {error.digest ? <p className="muted small">Κωδικός σφάλματος: {error.digest}</p> : null}
    </main>
  );
}
