package app.deptinfo.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Calls the real HTTP layer against the real AUEB CS data (loaded from ../data-seed at startup). */
@SpringBootTest(properties = "app.admin-api-key=test-key")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String BASE = "/api/universities/aueb/departments/cs";

    @Autowired
    MockMvc mvc;

    // ---------------------------------------------------------------- tenants

    @Test
    void listsUniversitiesAndDepartments() throws Exception {
        mvc.perform(get("/api/universities")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value("aueb"))
                .andExpect(jsonPath("$[0].name.el").value("Οικονομικό Πανεπιστήμιο Αθηνών"));
        mvc.perform(get("/api/universities/aueb/departments")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("cs"));
    }

    @Test
    void unknownUniversityAndDepartmentGiveDistinctNotFoundCodes() throws Exception {
        mvc.perform(get("/api/universities/nope/departments")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("UNIVERSITY_NOT_FOUND"));
        mvc.perform(get("/api/universities/nope/departments/cs/courses")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("UNIVERSITY_NOT_FOUND"));
        mvc.perform(get("/api/universities/aueb/departments/nope/courses")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void departmentInfoCarriesTaxonomiesWithCounts() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.courses").value(177))
                .andExpect(jsonPath("$.counts.faculty").value(34))
                .andExpect(jsonPath("$.semesters", hasSize(8)))
                .andExpect(jsonPath("$.semesters[2].label").value("Γ΄"))
                .andExpect(jsonPath("$.semesters[2].courseCount").value(4))
                .andExpect(jsonPath("$.semesters[6].courseCount").value(17))      // Ζ΄: 14 of its own + 3 offered in either
                .andExpect(jsonPath("$.semesters[7].courseCount").value(22))
                .andExpect(jsonPath("$.levels[0].id").value("CORE_MANDATORY"))
                .andExpect(jsonPath("$.levels[0].short").value("ΥΠ"))
                .andExpect(jsonPath("$.levels[0].courseCount").value(22))
                .andExpect(jsonPath("$.levels[1].courseCount").value(10))
                .andExpect(jsonPath("$.levels[2].courseCount").value(36))
                .andExpect(jsonPath("$.levels[3].courseCount").value(109))
                .andExpect(jsonPath("$.cycles", hasSize(8)))
                .andExpect(jsonPath("$.cycles[7].id").value("cybersecurity"))
                .andExpect(jsonPath("$.cycles[7].courseCount").value(6))
                .andExpect(jsonPath("$.sectors", hasSize(3)))
                .andExpect(jsonPath("$.info.email").value("infotech@aueb.gr"));
    }

    // ---------------------------------------------------------------- courses

    @Test
    void listsAllCoursesWhenUnfiltered() throws Exception {
        mvc.perform(get(BASE + "/courses")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(177)))
                .andExpect(jsonPath("$[0].semesters[0]").value(1))               // semester order first
                .andExpect(jsonPath("$[176].level").value("FREE"));               // courses without a semester last
    }

    @Test
    void filtersBySemesterLevelAndCycle() throws Exception {
        mvc.perform(get(BASE + "/courses?semester=3")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].code", hasItems("3335", "3365", "3321", "3230")));
        mvc.perform(get(BASE + "/courses?level=CORE_ELECTIVE")).andExpect(jsonPath("$", hasSize(10)));
        mvc.perform(get(BASE + "/courses?cycle=cybersecurity")).andExpect(jsonPath("$", hasSize(6)));
        // 14 courses are Ζ΄-only and 19 are Η΄-only; 3 (6023, 3802, 3850) are offered in either, so they appear in both lists.
        mvc.perform(get(BASE + "/courses?level=ELECTIVE&semester=7")).andExpect(jsonPath("$", hasSize(17)))
                .andExpect(jsonPath("$[*].code", hasItems("6023", "3802", "3850")));
        mvc.perform(get(BASE + "/courses?level=ELECTIVE&semester=8")).andExpect(jsonPath("$", hasSize(22)))
                .andExpect(jsonPath("$[*].code", hasItems("6023", "3802", "3850")));
        mvc.perform(get(BASE + "/courses?level=CORE_MANDATORY&cycle=cybersecurity")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidFiltersAreRejectedNotSilentlyEmpty() throws Exception {
        mvc.perform(get(BASE + "/courses?semester=99")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILTER"));
        mvc.perform(get(BASE + "/courses?level=NOPE")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILTER"));
        mvc.perform(get(BASE + "/courses?cycle=NOPE")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILTER"));
        mvc.perform(get(BASE + "/courses?semester=abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILTER"));
    }

    @Test
    void courseDetailResolvesPrerequisitesAndReverseLinks() throws Exception {
        mvc.perform(get(BASE + "/courses/3335")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name.el").value("Δομές Δεδομένων"))
                .andExpect(jsonPath("$.ects").value(7.0))
                .andExpect(jsonPath("$.semesters[0]").value(3))
                .andExpect(jsonPath("$.prerequisitesText").value("Python ή Java"))
                .andExpect(jsonPath("$.prerequisites", hasSize(1)))                  // one group: Python OR Java
                .andExpect(jsonPath("$.prerequisites[0]", hasSize(2)))
                .andExpect(jsonPath("$.prerequisites[0][0].code").value("3125"))
                .andExpect(jsonPath("$.prerequisites[0][1].name.el").value("Προγραμματισμός Υπολογιστών με Java"))
                .andExpect(jsonPath("$.instructors[0].facultyId").value("sgouritsa-alkmini"))
                .andExpect(jsonPath("$.aliases", hasItem("ΔΔ")))
                .andExpect(jsonPath("$.requiredBy[*].code", hasItems("3648", "3632", "3634")));
    }

    /** Guards against helper methods (e.g. I18n.isBlank) leaking into the JSON as extra properties. */
    @Test
    void translatedStringsSerializeAsPlainElEnObjects() throws Exception {
        mvc.perform(get(BASE + "/courses/3335")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name.blank").doesNotExist())
                .andExpect(jsonPath("$.name.el").value("Δομές Δεδομένων"))
                .andExpect(jsonPath("$.name.en").doesNotExist());
        mvc.perform(get(BASE)).andExpect(jsonPath("$.university.name.blank").doesNotExist())
                .andExpect(jsonPath("$.university.name.en").value("Athens University of Economics and Business"));
        mvc.perform(get(BASE + "/courses")).andExpect(jsonPath("$[0].name.blank").doesNotExist());
        mvc.perform(get(BASE + "/faculty")).andExpect(jsonPath("$[0].name.blank").doesNotExist());
    }

    @Test
    void courseDetailIsCaseInsensitiveAndOmitsNullFields() throws Exception {
        mvc.perform(get(BASE + "/courses/8154")).andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("FREE"))
                .andExpect(jsonPath("$.ects").doesNotExist())
                .andExpect(jsonPath("$.semesters", hasSize(0)))
                .andExpect(jsonPath("$.offeredBy").value("ΔΕΤ"));
        mvc.perform(get(BASE + "/courses/3335 ")).andExpect(status().isOk());
    }

    @Test
    void unknownCourseIs404() throws Exception {
        mvc.perform(get(BASE + "/courses/9999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------- faculty

    @Test
    void listsFacultyWithDerivedCourses() throws Exception {
        mvc.perform(get(BASE + "/faculty")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(34)));
        mvc.perform(get(BASE + "/faculty?sector=systems")).andExpect(jsonPath("$", hasSize(9)));
        mvc.perform(get(BASE + "/faculty?category=EDIP")).andExpect(jsonPath("$", hasSize(11)));
        mvc.perform(get(BASE + "/faculty?sector=nope")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILTER"));
        mvc.perform(get(BASE + "/faculty?category=ZZZ")).andExpect(status().isBadRequest());
    }

    @Test
    void facultyDetailShowsCoursesTaughtAndStatus() throws Exception {
        mvc.perform(get(BASE + "/faculty/amanatidis-georgios")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name.el").value("Αμανατίδης Γεώργιος"))
                .andExpect(jsonPath("$.rank").value("ASSISTANT_PROFESSOR"))
                .andExpect(jsonPath("$.sector").value("applied-math"))
                .andExpect(jsonPath("$.courses[*].code", hasItems("3117", "3434")));
        mvc.perform(get(BASE + "/faculty/vassalos-vasileios")).andExpect(jsonPath("$.status").value("ON_LEAVE"));
        mvc.perform(get(BASE + "/faculty/ghost")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FACULTY_NOT_FOUND"));
    }

    // ---------------------------------------------------------------- requirements and calendar

    @Test
    void servesRequirementsAndCalendar() throws Exception {
        mvc.perform(get(BASE + "/requirements")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEcts").value(240.0))
                .andExpect(jsonPath("$.rules[?(@.id=='level2-min-courses')].params.min").value(4))
                .andExpect(jsonPath("$.transitionalProvisions", hasSize(3)));
        mvc.perform(get(BASE + "/calendar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.academicYear").value("2026-2027"))
                .andExpect(jsonPath("$.terms", hasSize(2)))
                .andExpect(jsonPath("$.terms[0].start").value("2026-10-05"))      // ISO date, not a timestamp array
                .andExpect(jsonPath("$.examPeriods[2].start").doesNotExist());     // resit has no dates in the guide
    }

    // ---------------------------------------------------------------- FAQ and internship

    @Test
    void servesFaqAndInternship() throws Exception {
        mvc.perform(get(BASE + "/faq")).andExpect(status().isOk())
                .andExpect(jsonPath("$.sections", hasSize(6)))
                .andExpect(jsonPath("$.sections[0].title.el").value("Γραμματεία"))
                .andExpect(jsonPath("$.sections[*].items[*].id", hasSize(30)));
        mvc.perform(get(BASE + "/internship")).andExpect(status().isOk())
                .andExpect(jsonPath("$.title.el").value("Πρακτική Άσκηση"))
                .andExpect(jsonPath("$.sections[*].items[*].id", hasSize(8)));
    }

    // ---------------------------------------------------------------- ECTS calculator

    private static final String CALC = BASE + "/ects-calculator";

    @Test
    void calculatorReturnsTotalsRulesAndCycles() throws Exception {
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content("{\"passedCourses\":[\"3117\",\"3119\",\"9999\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.earned").value(12.0))
                .andExpect(jsonPath("$.totals.remaining").value(228.0))
                .andExpect(jsonPath("$.unknownCodes[0]").value("9999"))
                .andExpect(jsonPath("$.rules[?(@.id=='level1-all-mandatory')].missing.length()").value(20))
                .andExpect(jsonPath("$.cycles.minCourses").value(5))
                .andExpect(jsonPath("$.cycles.items", hasSize(8)))
                .andExpect(jsonPath("$.manualChecks", hasSize(7)))
                .andExpect(jsonPath("$.allCalculableRulesMet").value(false));
    }

    @Test
    void calculatorAddsEctsTheUserTypedInForFreeElectives() throws Exception {
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passedCourses\":[\"8154\",\"3117\"],\"ectsOverrides\":{\"8154\":6},\"otherEcts\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.earned").value(16.0))           // 6 typed + 6 from the guide + 4 other
                .andExpect(jsonPath("$.totals.userProvided").value(10.0))
                .andExpect(jsonPath("$.warnings[?(@.code=='ECTS_UNKNOWN')]").isEmpty());
        // The old request shape (only passedCourses) still works.
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content("{\"passedCourses\":[\"3117\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totals.userProvided").value(0.0));
    }

    @Test
    void calculatorRejectsSillyTypedInNumbers() throws Exception {
        for (String body : List.of(
                "{\"passedCourses\":[\"8154\"],\"ectsOverrides\":{\"8154\":-1}}",
                "{\"passedCourses\":[\"8154\"],\"ectsOverrides\":{\"8154\":0}}",
                "{\"passedCourses\":[\"8154\"],\"ectsOverrides\":{\"8154\":61}}",
                "{\"passedCourses\":[\"8154\"],\"ectsOverrides\":{\"8154\":null}}",
                "{\"passedCourses\":[],\"otherEcts\":-5}",
                "{\"passedCourses\":[],\"otherEcts\":241}")) {
            mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_BODY"));
        }
    }

    @Test
    void calculatorRejectsBadBodies() throws Exception {
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_BODY"));
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_BODY"));
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_BODY"));
        String tooMany = java.util.stream.IntStream.range(0, 201).mapToObj(i -> "\"" + i + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{\"passedCourses\":[", "]}"));
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content(tooMany))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_BODY"));
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content("{\"passedCourses\":[\"" + "x".repeat(40) + "\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculatorChecksTheTenantAndTheMethod() throws Exception {
        mvc.perform(post("/api/universities/aueb/departments/nope/ects-calculator")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"passedCourses\":[]}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));
        mvc.perform(get(CALC)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(CALC).contentType(MediaType.APPLICATION_JSON).content("{\"passedCourses\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totals.earned").value(0.0));
    }

    // ---------------------------------------------------------------- errors and admin

    @Test
    void unknownPathAndWrongMethodUseTheErrorFormat() throws Exception {
        mvc.perform(get("/api/nothing-here")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(post(BASE + "/courses")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void adminReloadRequiresTheKey() throws Exception {
        mvc.perform(post("/api/admin/reload")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(post("/api/admin/reload").header("X-Admin-Key", "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminReloadWithTheKeyReloadsValidData() throws Exception {
        mvc.perform(post("/api/admin/reload").header("X-Admin-Key", "test-key")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.tenants[0].tenant").value("aueb/cs"))
                .andExpect(jsonPath("$.tenants[0].courses").value(177))
                .andExpect(jsonPath("$.tenants[0].errors", hasSize(0)));
        mvc.perform(get(BASE + "/courses")).andExpect(jsonPath("$", hasSize(177)));   // data still served after reload
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
