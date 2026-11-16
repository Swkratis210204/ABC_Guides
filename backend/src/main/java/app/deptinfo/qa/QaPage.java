package app.deptinfo.qa;

import app.deptinfo.tenant.I18n;

import java.util.List;

/** Contents of faq.json / internship.json: a page of questions and answers grouped into sections. */
public record QaPage(int schemaVersion, I18n title, I18n intro, List<Section> sections) {

    /** Used when a tenant has no such file: the page exists but is empty. */
    public static final QaPage EMPTY = new QaPage(1, null, null, List.of());

    public QaPage {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    public record Section(String id, I18n title, List<Item> items) {
        public Section {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    /** note: an optional clarification shown under the answer; links: optional related URLs (https: or mailto:). */
    public record Item(String id, I18n question, I18n answer, I18n note, List<Link> links) {
        public Item {
            links = links == null ? List.of() : List.copyOf(links);
        }
    }

    public record Link(String label, String url) {
    }
}
