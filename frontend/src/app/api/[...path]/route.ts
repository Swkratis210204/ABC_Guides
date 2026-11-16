import type { NextRequest } from "next/server";

// Forwards browser calls (/api/...) to the backend, which is not reachable from the internet. BACKEND_URL is read
// at runtime so one build works in every environment. Only GET and POST are exposed, and /api/admin is never
// forwarded: admin actions are done from inside the private network.
export const dynamic = "force-dynamic";

const BACKEND = process.env.BACKEND_URL ?? "http://localhost:8080";

type Ctx = { params: Promise<{ path: string[] }> };

async function proxy(req: NextRequest, ctx: Ctx): Promise<Response> {
  const { path } = await ctx.params;
  if (path[0] === "admin") {
    return Response.json({ error: { code: "NOT_FOUND", message: "No such resource" } }, { status: 404 });
  }

  const target = `${BACKEND}/api/${path.map(encodeURIComponent).join("/")}${req.nextUrl.search}`;
  const headers = new Headers({ Accept: "application/json" });
  const contentType = req.headers.get("content-type");
  if (contentType) headers.set("Content-Type", contentType);
  const forwardedFor = req.headers.get("x-forwarded-for") ?? req.headers.get("fly-client-ip");
  if (forwardedFor) headers.set("X-Forwarded-For", forwardedFor);

  try {
    const upstream = await fetch(target, {
      method: req.method,
      headers,
      body: req.method === "GET" ? undefined : await req.text(),
      cache: "no-store",
    });
    return new Response(upstream.body, {
      status: upstream.status,
      headers: { "Content-Type": upstream.headers.get("content-type") ?? "application/json" },
    });
  } catch {
    return Response.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Ο διακομιστής δεδομένων δεν είναι διαθέσιμος." } },
      { status: 503 },
    );
  }
}

export { proxy as GET, proxy as POST };
