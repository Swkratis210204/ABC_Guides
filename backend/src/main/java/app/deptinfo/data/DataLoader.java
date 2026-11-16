package app.deptinfo.data;

import app.deptinfo.calendar.AcademicCalendar;
import app.deptinfo.courses.Course;
import app.deptinfo.faculty.FacultyMember;
import app.deptinfo.qa.QaPage;
import app.deptinfo.requirements.Requirements;
import app.deptinfo.tenant.TenantKey;
import app.deptinfo.tenant.TenantMeta;
import app.deptinfo.tenant.TenantSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads tenants/{uni}/{dept}/ folders from disk. Each tenant needs five files (plus the optional faq.json and internship.json); unknown JSON properties are rejected so
 * a mistyped column name becomes an error instead of silently lost data.
 */
@Component
public class DataLoader {

    public static final String TENANT_FILE = "tenant.json";
    public static final String COURSES_FILE = "courses.json";
    public static final String FACULTY_FILE = "faculty.json";
    public static final String REQUIREMENTS_FILE = "requirements.json";
    public static final String CALENDAR_FILE = "calendar.json";
    /** Optional: a tenant without these files simply has empty FAQ / internship pages. */
    public static final String FAQ_FILE = "faq.json";
    public static final String INTERNSHIP_FILE = "internship.json";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);

    private final DataValidator validator;

    public DataLoader(DataValidator validator) {
        this.validator = validator;
    }

    /** Outcome for one tenant folder. {@code snapshot} is non-null only when {@code errors} is empty. */
    public record TenantLoadResult(TenantKey key, Path dir, TenantSnapshot snapshot, List<String> errors,
                                   List<String> warnings) {
        public boolean ok() {
            return errors.isEmpty() && snapshot != null;
        }
    }

    /** Loads every {@code tenants/{uni}/{dept}} folder under {@code dataDir}, sorted by key. */
    public List<TenantLoadResult> loadAll(Path dataDir) {
        Path tenants = dataDir.resolve("tenants");
        List<TenantLoadResult> results = new ArrayList<>();
        if (!Files.isDirectory(tenants)) {
            return results;
        }
        try (Stream<Path> unis = Files.list(tenants)) {
            for (Path uniDir : unis.filter(Files::isDirectory).sorted().toList()) {
                try (Stream<Path> depts = Files.list(uniDir)) {
                    for (Path deptDir : depts.filter(Files::isDirectory).sorted().toList()) {
                        results.add(loadTenant(new TenantKey(uniDir.getFileName().toString(), deptDir.getFileName().toString()), deptDir));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read data directory " + tenants + ": " + e.getMessage(), e);
        }
        results.sort(Comparator.comparing(r -> r.key().toString()));
        return results;
    }

    public TenantLoadResult loadTenant(TenantKey key, Path dir) {
        List<String> errors = new ArrayList<>();

        TenantMeta meta = read(dir, TENANT_FILE, new TypeReference<TenantMeta>() {}, errors);
        List<Course> courses = read(dir, COURSES_FILE, new TypeReference<List<Course>>() {}, errors);
        List<FacultyMember> faculty = read(dir, FACULTY_FILE, new TypeReference<List<FacultyMember>>() {}, errors);
        Requirements requirements = read(dir, REQUIREMENTS_FILE, new TypeReference<Requirements>() {}, errors);
        AcademicCalendar calendar = read(dir, CALENDAR_FILE, new TypeReference<AcademicCalendar>() {}, errors);

        QaPage faq = readOptional(dir, FAQ_FILE, errors);
        QaPage internship = readOptional(dir, INTERNSHIP_FILE, errors);

        if (!errors.isEmpty()) {
            return new TenantLoadResult(key, dir, null, errors, List.of());
        }
        DataValidator.Result result = validator.validate(key, meta, courses, faculty, requirements, calendar);
        validator.validateQa(FAQ_FILE, faq, result.errors());
        validator.validateQa(INTERNSHIP_FILE, internship, result.errors());
        if (!result.ok()) {
            return new TenantLoadResult(key, dir, null, result.errors(), result.warnings());
        }
        return new TenantLoadResult(key, dir, TenantSnapshot.of(key, meta, courses, faculty, requirements, calendar, faq, internship),
                List.of(), result.warnings());
    }

    private QaPage readOptional(Path dir, String file, List<String> errors) {
        if (!Files.isRegularFile(dir.resolve(file))) {
            return QaPage.EMPTY;
        }
        return read(dir, file, new TypeReference<QaPage>() {}, errors);
    }

    private <T> T read(Path dir, String file, TypeReference<T> type, List<String> errors) {
        Path path = dir.resolve(file);
        if (!Files.isRegularFile(path)) {
            errors.add(file + ": file is missing");
            return null;
        }
        try {
            T value = mapper.readValue(path.toFile(), type);
            if (value == null) {
                errors.add(file + ": file is empty");
            }
            return value;
        } catch (JsonProcessingException e) {
            String location = e.getLocation() == null ? "" : " (line " + e.getLocation().getLineNr() + ")";
            errors.add(file + location + ": " + firstLine(e.getOriginalMessage()));
        } catch (IOException e) {
            errors.add(file + ": cannot be read: " + e.getMessage());
        }
        return null;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "invalid JSON";
        }
        int nl = message.indexOf('\n');
        return nl < 0 ? message : message.substring(0, nl);
    }
}
