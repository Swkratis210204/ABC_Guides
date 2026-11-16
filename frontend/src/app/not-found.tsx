import Link from "next/link";

export default function NotFound() {
  return (
    <main className="container">
      <h1>Η σελίδα δεν βρέθηκε</h1>
      <p className="muted">Ο σύνδεσμος μπορεί να είναι λάθος ή το στοιχείο να έχει αφαιρεθεί.</p>
      <p>
        <Link href="/" className="button primary">
          Επιστροφή στην αρχική
        </Link>
      </p>
    </main>
  );
}
