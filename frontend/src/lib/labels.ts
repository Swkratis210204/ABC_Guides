import type { DepartmentInfo, I18n } from "./types";

/** Greek text first, English as a fallback. */
export const t = (s: I18n | undefined): string => (s ? s.el || s.en || "" : "");

const numberFormat = new Intl.NumberFormat("el-GR", { maximumFractionDigits: 1 });

/** 7.5 -> "7,5", 6 -> "6". Missing values show as an en dash. */
export const fmt = (n: number | undefined | null): string => (n === undefined || n === null ? "–" : numberFormat.format(n));

const dateFormat = new Intl.DateTimeFormat("el-GR", { day: "numeric", month: "long", year: "numeric", timeZone: "UTC" });

/** "2026-10-05" -> "5 Οκτωβρίου 2026". Dates are calendar days, so they are formatted in UTC to avoid shifting. */
export const fmtDate = (iso: string | undefined): string => (iso ? dateFormat.format(new Date(`${iso}T00:00:00Z`)) : "");

export const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "",
  ON_LEAVE: "Σε άδεια",
  PENDING_APPOINTMENT: "Υπό διορισμό",
};

export const CATEGORY_LABEL: Record<string, string> = {
  DEP: "Διδακτικό Ερευνητικό Προσωπικό (ΔΕΠ)",
  EDIP: "Εργαστηριακό Διδακτικό Προσωπικό (Ε.ΔΙ.Π.)",
};

export const RULE_KIND_GROUP: Record<string, "core" | "other"> = {
  TOTAL_ECTS: "core",
  ALL_MANDATORY: "core",
  MIN_COURSES_BY_LEVEL: "core",
  ECTS_FROM_LEVELS: "core",
  CYCLES: "core",
  COURSE_REQUIRED: "core",
  FREE_TEXT: "other",
};

/** Small lookups over the department info so pages do not repeat the same find() calls. */
export function lookups(info: DepartmentInfo) {
  const semester = new Map(info.semesters.map((s) => [s.number, s.label]));
  const level = new Map(info.levels.map((l) => [l.id, l]));
  const cycle = new Map(info.cycles.map((c) => [c.id, c]));
  const sector = new Map(info.sectors.map((s) => [s.id, s]));
  const rank = new Map(info.ranks.map((r) => [r.id, r]));
  const semesterLabel = (n: number | undefined) => (n === undefined ? "" : (semester.get(n) ?? String(n)));
  return {
    semesterLabel,
    /** [7, 8] -> "Ζ΄/Η΄"; [] -> "". */
    semestersLabel: (list: number[]) => list.map(semesterLabel).join("/"),
    level: (id: string) => level.get(id),
    cycle: (id: string) => cycle.get(id),
    sector: (id: string | undefined) => (id ? sector.get(id) : undefined),
    rankLabel: (id: string | undefined) => (id ? t(rank.get(id)?.label) : ""),
  };
}

export const departmentBase = (uni: string, dept: string) => `/${encodeURIComponent(uni)}/${encodeURIComponent(dept)}`;
