export const dynamic = "force-dynamic";

// Liveness check for the hosting platform. It does not call the backend, so a backend outage does not restart the frontend.
export function GET() {
  return new Response("ok", { status: 200, headers: { "Content-Type": "text/plain" } });
}
