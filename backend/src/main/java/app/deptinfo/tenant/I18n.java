package app.deptinfo.tenant;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** A user-visible string in Greek (primary) with an optional English variant. */
public record I18n(String el, String en) {

    /** Greek if present, otherwise English. */
    public String text() {
        return el != null && !el.isBlank() ? el : en;
    }

    @JsonIgnore
    public boolean isBlank() {
        return (el == null || el.isBlank()) && (en == null || en.isBlank());
    }
}
