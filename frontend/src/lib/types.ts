// TypeScript mirrors of the backend's JSON. Optional fields are absent (not null) when they do not apply.

export interface I18n {
  el: string;
  en?: string;
}

export interface University {
  slug: string;
  name: I18n;
  shortName?: string;
  website?: string;
}

export interface Department {
  slug: string;
  name: I18n;
}

export interface SemesterInfo {
  number: number;
  label: string;
  courseCount: number;
}

export interface LevelInfo {
  id: string;
  number: number;
  short: string;
  name: I18n;
  color?: string;
  semesterRange: number[];
  courseCount: number;
}

export interface CycleInfo {
  id: string;
  number: number;
  name: I18n;
  courseCount: number;
}

export interface SectorInfo {
  id: string;
  name: I18n;
  topics?: string;
  memberCount: number;
}

export interface Rank {
  id: string;
  label: I18n;
}

/** The free-form "info" block of tenant.json. Every field is optional. */
export interface DepartmentExtra {
  guideTitle?: string;
  guideLastUpdated?: string;
  school?: string;
  address?: string;
  website?: string;
  phone?: string;
  email?: string;
  administration?: { role: string; name: string }[];
  postgraduatePrograms?: string[];
  englishOnlyCourses?: string[];
  mandatoryComposition?: string;
  freeElectivesFromDepartment?: string[];
  freeElectivesNote?: string;
  offeredByLegend?: Record<string, string>;
}

export interface DepartmentInfo {
  university: University;
  department: Department;
  semesters: SemesterInfo[];
  levels: LevelInfo[];
  cycles: CycleInfo[];
  sectors: SectorInfo[];
  ranks: Rank[];
  info: DepartmentExtra;
  counts: { courses: number; faculty: number };
}

export interface CourseRef {
  code: string;
  name?: I18n;
}

export interface CourseSummary {
  code: string;
  name: I18n;
  ects?: number;
  /** Semesters it is offered in; empty for free electives. Two entries mean "either" (e.g. the thesis, Ζ΄/Η΄). */
  semesters: number[];
  level: string;
  cycles: string[];
  offeredBy?: string;
  language?: string;
}

export interface Instructor {
  name: string;
  facultyId?: string;
}

export interface CourseDetail {
  code: string;
  name: I18n;
  level: string;
  ects?: number;
  semesters: number[];
  cycles: string[];
  cyclesNote?: string;
  /** Groups: every group is required; within a group any one course is enough. */
  prerequisites: CourseRef[][];
  prerequisitesText?: string;
  instructors: Instructor[];
  offeredBy?: string;
  language?: string;
  description?: I18n;
  aliases: string[];
  eclassUrl?: string;
  requiredBy: CourseRef[];
}

export interface FacultyView {
  id: string;
  name: I18n;
  category: "DEP" | "EDIP";
  rank?: string;
  sector?: string;
  status: "ACTIVE" | "ON_LEAVE" | "PENDING_APPOINTMENT";
  statusNote?: string;
  email?: string;
  homepageUrl?: string;
  courses: CourseRef[];
}

export interface Rule {
  id: string;
  kind: string;
  title: I18n;
  description: I18n;
  params: Record<string, unknown>;
}

export interface Requirements {
  schemaVersion: number;
  totalEcts: number;
  rules: Rule[];
  transitionalProvisions: { validUntil: string; description: I18n }[];
  recentChanges: { year: string; text: I18n }[];
}

export interface CalendarItem {
  id: string;
  name: I18n;
  start?: string;
  end?: string;
  note?: I18n;
}

export interface AcademicCalendar {
  academicYear: string;
  terms: (CalendarItem & { semesterType: string })[];
  examPeriods: (CalendarItem & { termId?: string })[];
  holidays: (CalendarItem & { kind: "BREAK" | "PUBLIC_HOLIDAY" })[];
}

export interface CalculatorResult {
  /** userProvided: the part of `earned` the user typed in (the guide gives no ECTS for those courses). */
  totals: { earned: number; required: number; remaining: number; userProvided: number };
  levels: { level: string; coursesPassed: number; ectsEarned: number }[];
  rules: {
    id: string;
    kind: string;
    title: I18n;
    satisfied: boolean;
    current: number;
    required: number;
    remaining: number;
    missing: CourseRef[];
  }[];
  cycles?: {
    minCourses: number;
    maxCompletable: number;
    items: {
      id: string;
      number: number;
      name: I18n;
      passedCodes: string[];
      missingToComplete: number;
      completedInBestPlan: boolean;
      assignedCodes: string[];
    }[];
  };
  manualChecks: { id: string; title: I18n }[];
  unknownCodes: string[];
  warnings: { code: string; message: string; courseCode?: string }[];
  allCalculableRulesMet: boolean;
}
