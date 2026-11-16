// Server-side API client. Used by server components only: it talks to the backend directly using BACKEND_URL,
// which is read at runtime (not build time). Browser code goes through the /api proxy instead (see lib/client.ts).
import { notFound } from "next/navigation";
import type {
  AcademicCalendar,
  CourseDetail,
  CourseSummary,
  Department,
  DepartmentInfo,
  FacultyView,
  QaPage,
  Requirements,
  University,
} from "./types";

const BACKEND = process.env.BACKEND_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

async function get<T>(path: string): Promise<T> {
  let res: Response;
  try {
    // Data only changes on an admin reload and is tiny, so always ask the backend for the current version.
    res = await fetch(`${BACKEND}${path}`, { cache: "no-store" });
  } catch {
    throw new ApiError(503, "BACKEND_UNAVAILABLE", "Δεν είναι δυνατή η σύνδεση με τον διακομιστή δεδομένων.");
  }
  if (!res.ok) {
    let code = "ERROR";
    let message = res.statusText;
    try {
      const body = await res.json();
      code = body.error?.code ?? code;
      message = body.error?.message ?? message;
    } catch {
      /* keep defaults */
    }
    throw new ApiError(res.status, code, message);
  }
  return (await res.json()) as T;
}

/** Turns a backend 404 into the Next.js not-found page; every other failure propagates to the error boundary. */
async function orNotFound<T>(p: Promise<T>): Promise<T> {
  try {
    return await p;
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
}

const dept = (uni: string, d: string) => `/api/universities/${encodeURIComponent(uni)}/departments/${encodeURIComponent(d)}`;

export const api = {
  universities: () => get<University[]>("/api/universities"),
  departments: (uni: string) => orNotFound(get<Department[]>(`/api/universities/${encodeURIComponent(uni)}/departments`)),
  department: (uni: string, d: string) => orNotFound(get<DepartmentInfo>(dept(uni, d))),
  courses: (uni: string, d: string, filter: { semester?: number; level?: string; cycle?: string } = {}) => {
    const q = new URLSearchParams();
    if (filter.semester !== undefined) q.set("semester", String(filter.semester));
    if (filter.level) q.set("level", filter.level);
    if (filter.cycle) q.set("cycle", filter.cycle);
    const qs = q.toString();
    return orNotFound(get<CourseSummary[]>(`${dept(uni, d)}/courses${qs ? `?${qs}` : ""}`));
  },
  course: (uni: string, d: string, code: string) =>
    orNotFound(get<CourseDetail>(`${dept(uni, d)}/courses/${encodeURIComponent(code)}`)),
  faculty: (uni: string, d: string, filter: { sector?: string; category?: string } = {}) => {
    const q = new URLSearchParams();
    if (filter.sector) q.set("sector", filter.sector);
    if (filter.category) q.set("category", filter.category);
    const qs = q.toString();
    return orNotFound(get<FacultyView[]>(`${dept(uni, d)}/faculty${qs ? `?${qs}` : ""}`));
  },
  facultyMember: (uni: string, d: string, id: string) =>
    orNotFound(get<FacultyView>(`${dept(uni, d)}/faculty/${encodeURIComponent(id)}`)),
  requirements: (uni: string, d: string) => orNotFound(get<Requirements>(`${dept(uni, d)}/requirements`)),
  calendar: (uni: string, d: string) => orNotFound(get<AcademicCalendar>(`${dept(uni, d)}/calendar`)),
  faq: (uni: string, d: string) => orNotFound(get<QaPage>(`${dept(uni, d)}/faq`)),
  internship: (uni: string, d: string) => orNotFound(get<QaPage>(`${dept(uni, d)}/internship`)),
};
