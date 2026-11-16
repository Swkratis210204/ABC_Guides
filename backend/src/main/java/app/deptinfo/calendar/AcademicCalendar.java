package app.deptinfo.calendar;

import app.deptinfo.tenant.I18n;

import java.time.LocalDate;
import java.util.List;

/** Contents of calendar.json. All dates are ISO (YYYY-MM-DD); some may be null when the guide gives none. */
public record AcademicCalendar(
        int schemaVersion,
        String academicYear,
        List<Term> terms,
        List<ExamPeriod> examPeriods,
        List<Holiday> holidays) {

    public AcademicCalendar {
        terms = terms == null ? List.of() : List.copyOf(terms);
        examPeriods = examPeriods == null ? List.of() : List.copyOf(examPeriods);
        holidays = holidays == null ? List.of() : List.copyOf(holidays);
    }

    public record Term(String id, I18n name, String semesterType, LocalDate start, LocalDate end) {
    }

    public record ExamPeriod(String id, I18n name, String termId, LocalDate start, LocalDate end, I18n note) {
    }

    /** kind is BREAK (a range such as Christmas) or PUBLIC_HOLIDAY (a single day, end may be null). */
    public record Holiday(String id, I18n name, LocalDate start, LocalDate end, String kind) {
    }
}
