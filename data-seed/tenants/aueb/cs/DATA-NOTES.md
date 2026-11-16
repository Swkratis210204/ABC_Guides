# AUEB CS seed data: notes and open questions

Sources:
1. *AUEB CS — Οδηγός Προπτυχιακών Σπουδών 2026-2027* (the PDF, 16 pages, last updated 2026-08-25).
2. *AUEB CS Undergraduate Guide 2026-2027.xlsx* (the department's spreadsheet, 7 sheets, in the project root).

The files were first transcribed from the PDF, then compared field by field with the spreadsheet.

## Spreadsheet versus PDF

The two agree on everything that could be compared: ECTS of all 68 courses, instructors, teaching language, offering department, the 23 faculty (rank, sector, on-leave flag) and the 20 free-elective codes. Differences, and what was done:

| Difference | Decision |
|------------|----------|
| The spreadsheet gives **prerequisites for 9 core electives** (ΕΠ) that the PDF lists none for (e.g. 3543: "ΔΔ, ΒΔ") | Added from the spreadsheet |
| The spreadsheet marks **6023, 3802, 3850 as "Ζ΄/Η΄"** (offered in either semester); the PDF only shows them under Η΄ | Stored as `semesters: [7, 8]`; they appear in both semester lists |
| The spreadsheet has the **same cycle contradiction** as the PDF: its "Study Cycles" sheet disagrees with its own per-course "Cycles" columns, for the same 4 courses | Not settled by it; see section 1 |
| Name wording (abbreviations, "&" for "και") | PDF wording kept |

Nothing else in the spreadsheet adds data: it also gives no ECTS for the level-4 courses of other departments.

## Checks that were run

- Counts match the guide's statements: 22 mandatory courses, 10 core electives, 8 cycles, 3 sectors, 8 semesters, 23 ΔΕΠ + 11 ΕΔΙΠ staff, 20 level-4 courses from other departments.
- **ECTS and semester** of all 22 ΥΠ and 10 ΕΠ courses match the second place the guide states them (the short-description lines, e.g. "3335 Δομές Δεδομένων (Γ΄, 7 ECTS)"). No mismatches.
- **ECTS and cycles** of 34 of the 36 level-3 courses match a fresh parse of the guide's table rows. No mismatches. The other two (3802 Πτυχιακή Εργασία, 3850 Ερευνητική Εργασία) were checked by hand: 6 ECTS, cycles "κατά απόφαση επιβλέποντα".
- Every course code in the guide is in `courses.json` and vice versa. The only other 4-digit codes in the guide are 3252 (the old «Λογιστική», mentioned in the transitional provisions) and 8203 (part of the department phone number).
- Every prerequisite code resolves to a course in this department.
- Instructor names and prerequisite wording were read by hand from the PDF, then compared with the spreadsheet: no differences, apart from the 9 core-elective prerequisites that only the spreadsheet has. Course descriptions exist only in the PDF and were not cross-checked.

Items below are places where the guide is inconsistent or ambiguous. **The data follows the guide literally; please confirm the items marked ❓ with the department.**

## ✅ 1. Cycle membership: the guide contradicted itself in 4 places (resolved by the project owner)

The per-course "Κύκλοι" column (pages 7–10) and the "Κύκλοι Σπουδών — Αντιστοίχιση Μαθημάτων" lists (pages 11–12) disagree. The project owner decided the correct values, and `courses.json` uses these:

| Course | Per-course column says | Cycle list says | **Decision (in `courses.json`)** |
|--------|------------------------|-----------------|----------------------------------|
| 3155 Στατιστική στην Πληροφορική | cycles 1, 4 | cycles 1, 3 | **cycles 1, 3** (matches the cycle list) |
| 3515 Λογική | cycles 4, 6 | cycle 4 only | **cycles 4, 7** (confirmed by the project owner; matches neither source) |
| 3672 Δίκτυα Υπολογιστών | cycles 5, 6, 8 | cycles 5, 8 | **cycles 5, 6, 8** (matches the per-course column) |
| 3842 Ανάπτυξη Εφαρμογών Πληροφοριακών Συστημάτων | cycle 7 only | cycles 6, 7 | **cycle 7 only** (matches the per-course column) |

For 3515 the decision (cycles 4 and 7) appears in neither the PDF nor the spreadsheet (which says 4, 6 in the course's column and 4 only in the cycle sheet). The project owner confirmed it explicitly, so it is deliberate and not a typo.

Consequence: the data now differs from the guide's printed cycle lists in three spots, on purpose: cycle 6 has 3672 but not 3842, and cycle 7 has 3515. If the department publishes a corrected guide, re-check these.

## ❓ 2. Cycle sizes do not match "7–9 courses per cycle"

The guide says each cycle has 7–9 courses, but the data (after the decisions in section 1) gives: cycle 1 = 10, 2 = 8, 3 = 9, 4 = 7, 5 = 10, 6 = 11, 7 = 8, 8 = 6.
Completion rule used: at least 5 courses of the cycle, and a course counts toward only one cycle.

## ❓ 3. Prerequisite wording is ambiguous in a few places

- Level 1 descriptions use "ή" (or), which is encoded as alternatives: `"prerequisites": [["3125","3222"]]` means Python **or** Java.
- Level 3 tables (and the core electives, from the spreadsheet) list prerequisites separated by commas (e.g. "ΔΔ, Αλγόριθμοι"). These are encoded as **all required** (AND). The spreadsheet supports this reading: its mandatory-course sheet writes "ή" explicitly wherever it means "either", in the same column where commas appear.
- 3432 Αλγόριθμοι: "Python/Java/Διακριτά Μαθ." is encoded as one "any of the three" group. It could also mean (Python or Java) and Discrete Math.
- 3664 Κατανεμημένα Συστήματα: "Java, ΛΣ" is encoded as both required.
- 3747 Τεχνολογίες και Προγρ. Εφαρμογών στον Ιστό: "Python, Java, ΤΝ" is encoded as all three required (the last abbreviation reads ΤΝ = Τεχνητή Νοημοσύνη; it might be a typo for ΤΛ = Τεχνολογία Λογισμικού).
- The verbatim guide text is kept in `prerequisitesText` for every course so the UI can show exactly what the guide says.

Abbreviations resolved to codes (from the guide's usage): ΕΕΥ = 3135, ΟΣΥ = 3365, ΔΔ = 3335, ΒΔ = 3436, ΤΝ = 3531, ΛΣ = 3464, ΤΛ / Τεχν. Λογ. = 3648, Δίκτυα Επικ. = 3571, Δίκτυα Υπολ. = 3672.

## 4. Other inconsistencies in the guide

- Page 4 says level ΥΠ is taught in "Εξάμηνα Α–Ε", but 3664 Κατανεμημένα Συστήματα (ΥΠ) is in ΣΤ΄. `tenant.json` uses semesters 1–6 for ΥΠ.
- Level 3 (Ε) courses are placed in semester Ζ΄ or Η΄ by the "— Η΄ εξάμηνο —" divider in the PDF table (p. 9): before the divider = Ζ΄, after = Η΄. The spreadsheet confirms this, and additionally marks 6023, 3802 and 3850 (all listed after the divider) as "Ζ΄/Η΄", i.e. offered in either semester.
- Requirements 3 and 4: "Επίπεδο 3: ≥ 64 ECTS από ΕΠ + Ε" includes the ΕΠ courses. The ECTS calculator (not built yet) will count the 4 required ΕΠ courses toward the 64 ECTS, as written.

## 5. Data limitations

- **ECTS values include decimals** (6082 Γραμμική Άλγεβρα ΙΙ = 7.5, 6142 Πιθανότητες ΙΙ = 7.5). `ects` is therefore a number, not an integer.
- **Level 4 (ΕΛ):** the guide lists 20 courses of other OPA departments with codes but **no ECTS and no semester**. They are stored with `"ects": null`. The ECTS calculator (not built yet) will report them as "ECTS unknown" and not count them.
- Free electives offered by the department itself (Πρακτική Άσκηση, Ψηφιακό Εκπαιδευτικό Υλικό, foreign-language IV courses, Pedagogical program) have no codes. They are stored as text in `tenant.json → info.freeElectivesFromDepartment`.
- **Instructors not in the faculty list** (stored by name only, `facultyId: null`): Δρ. Γιαννουλάκης Στυλιανός (3151), Δρ. Κώτσιος Παναγιώτης (3254), Δρ. Μαϊλής Θεόφιλος (3436). Presumably external/contract teaching staff.
- Many courses have no instructor listed ("—"); they are stored with an empty instructor list.
- **Holiday names are not in the guide**, only dates. Names were inferred from standard Greek public holidays for 2026–27 (28/10 Εθνική εορτή, 17/11 Πολυτεχνείου, 6/1 Θεοφάνεια, 15/3 Καθαρά Δευτέρα, 25/3 Εθνική εορτή, 1/5 Πρωτομαγιά, 21/6 Αγίου Πνεύματος). The dates are consistent with an Orthodox Easter on 2 May 2027. Please confirm.
- The September resit period has no concrete dates in the guide ("last week of August, 4 weeks"). Stored as a note with null dates.
- Staff status: "(άδεια)" is stored as `ON_LEAVE`, "(υπό διορισμό)" as `PENDING_APPOINTMENT`.
- `offeredBy` tags (ΣΤΑ, ΔΕΤ, ΟΔΕ, ΛΟΧΡΗ, …) come from the course names in the guide. The department names in `tenant.json → info.offeredByLegend` are my expansion of those abbreviations, not text from the guide.
