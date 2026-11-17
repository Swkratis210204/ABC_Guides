"""Tests for scripts/validate.py.

Each test copies the sample content to a temporary folder, breaks one thing, runs
the validator on the copy and checks the plain-language message. Real files are never touched.
"""
import json
import shutil
import subprocess
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parent.parent
SAMPLE = ROOT / "content"
CURRICULUM = "example-university/computer-science/curriculum/2026-2027.json"
CALENDAR = "example-university/university/calendar/2026-2027.json"
FACULTY = "example-university/computer-science/general/faculty.json"


@pytest.fixture
def content(tmp_path):
    dest = tmp_path / "content"
    shutil.copytree(SAMPLE, dest)
    return dest


def run(content_dir):
    result = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "validate.py"), str(content_dir)],
        capture_output=True, text=True, encoding="utf-8",
    )
    return result.returncode, result.stdout


def edit(content_dir, rel, change):
    path = content_dir / rel
    data = json.loads(path.read_text(encoding="utf-8"))
    change(data)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def test_sample_content_is_valid():
    code, out = run(SAMPLE)
    assert code == 0, out
    assert "All good" in out


def test_typo_in_field_name_suggests_fix(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].update(semester=d["courses"][0].pop("semesters")))
    code, out = run(content)
    assert code == 1
    assert 'did you mean "semesters"?' in out


def test_missing_required_field_is_named(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].pop("ects"))
    code, out = run(content)
    assert code == 1
    assert 'The field "ects" is missing' in out
    assert '"1001"' in out  # tells the person which course


def test_unknown_level_is_caught(content):
    edit(content, CURRICULUM, lambda d: d["courses"][1].update(level="mandatry"))
    code, out = run(content)
    assert code == 1
    assert 'has the level "mandatry"' in out


def test_unknown_prerequisite_is_caught(content):
    edit(content, CURRICULUM, lambda d: d["courses"][2].update(prerequisites=["9999"]))
    code, out = run(content)
    assert code == 1
    assert 'needs course "9999", but no course with that code exists in this file' in out


def test_unknown_teacher_is_caught(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].update(instructors=["nobody"]))
    code, out = run(content)
    assert code == 1
    assert '"nobody", who is not in faculty.json' in out


def test_duplicate_course_code_is_caught(content):
    edit(content, CURRICULUM, lambda d: d["courses"][1].update(code="1001"))
    code, out = run(content)
    assert code == 1
    assert 'course code "1001" appears more than once' in out


def test_bad_date_format_is_explained(content):
    edit(content, CALENDAR, lambda d: d["terms"][0].update(start="05/10/2026"))
    code, out = run(content)
    assert code == 1
    assert "year-month-day" in out


def test_end_before_start_is_caught(content):
    edit(content, CALENDAR, lambda d: d["holidays"][0].update(end="2026-12-01"))
    code, out = run(content)
    assert code == 1
    assert "before it starts" in out


def test_year_must_match_file_name(content):
    edit(content, CURRICULUM, lambda d: d.update(academic_year="2025-2026"))
    code, out = run(content)
    assert code == 1
    assert "They must match" in out


def test_bad_language_code_is_explained(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].update(name={"english": "Maths"}))
    code, out = run(content)
    assert code == 1
    assert "is not a supported language" in out


def test_broken_json_gives_line_number(content):
    (content / FACULTY).write_text('{"members": [ {"id": "a-b", "name": "X",} ]}', encoding="utf-8")
    code, out = run(content)
    assert code == 1
    assert "not valid JSON" in out
    assert "line 1" in out


def test_broken_faculty_file_does_not_flood_with_follow_up_errors(content):
    (content / FACULTY).write_text("{ not json", encoding="utf-8")
    code, out = run(content)
    assert code == 1
    assert "who is not in faculty.json" not in out


def test_unrecognised_file_is_reported(content):
    (content / "example-university" / "university" / "random.json").write_text("{}", encoding="utf-8")
    code, out = run(content)
    assert code == 1
    assert "do not recognise" in out


# ---- Previous years ----------------------------------------------------------------------

OLD_CURRICULUM = "example-university/computer-science/curriculum/2022-2023.json"
OLD_ADMIN = "example-university/computer-science/general/2022-2023/administration.json"


def test_old_year_may_use_former_teacher_and_names_not_in_faculty():
    # The sample already contains such a year; it must pass.
    code, out = run(SAMPLE)
    assert code == 0, out


def test_old_year_courses_are_still_checked(content):
    edit(content, OLD_CURRICULUM, lambda d: d["courses"][0].update(instructors=["nobody"]))
    code, out = run(content)
    assert code == 1
    assert '"nobody", who is not in faculty.json' in out


def test_old_year_administration_is_checked(content):
    edit(content, OLD_ADMIN, lambda d: d["roles"][0].update(faculty_id="nobody"))
    code, out = run(content)
    assert code == 1
    assert '"nobody", who is not in faculty.json' in out


def test_last_year_before_first_year_is_caught(content):
    def change(d):
        d["members"][-1].update(first_year="2020-2021", last_year="2015-2016")
    edit(content, FACULTY, change)
    code, out = run(content)
    assert code == 1
    assert "is earlier than" in out


def test_old_year_file_in_wrong_folder_is_reported(content):
    (content / "example-university" / "computer-science" / "general" / "2022-2023" / "faculty.json").write_text("{}", encoding="utf-8")
    code, out = run(content)
    assert code == 1
    assert "do not recognise" in out


# ---- University entity decisions ----------------------------------------------------------

def test_short_name_and_country_are_translatable(content):
    uni = "example-university/university/university.json"
    edit(content, uni, lambda d: d.update(short_name="EXU"))
    code, out = run(content)
    assert code == 1
    assert '"short_name"' in out and "group in {curly brackets}" in out


def test_free_electives_belong_to_a_department_not_the_university(content):
    src = content / "example-university" / "computer-science" / "general" / "free_electives.json"
    shutil.copy(src, content / "example-university" / "university" / "free_electives.json")
    code, out = run(content)
    assert code == 1
    assert "do not recognise" in out


def test_only_english_and_greek_are_accepted(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0]["name"].update(fr="Maths"))
    code, out = run(content)
    assert code == 1
    assert 'Use "en" for English or "el" for Greek' in out


def test_default_language_must_be_in_language_list(content):
    uni = "example-university/university/university.json"
    edit(content, uni, lambda d: d.update(languages=["en"], default_language="el"))
    code, out = run(content)
    assert code == 1
    assert "only lists en" in out


def test_term_needs_semester_number(content):
    edit(content, CALENDAR, lambda d: d["terms"][0].pop("semester"))
    code, out = run(content)
    assert code == 1
    assert 'The field "semester" is missing' in out


def test_winter_semester_must_be_october_to_january(content):
    edit(content, CALENDAR, lambda d: d["terms"][0].update(start="2026-09-21"))
    code, out = run(content)
    assert code == 1
    assert "must run October to January" in out


def test_spring_semester_must_be_february_to_june(content):
    edit(content, CALENDAR, lambda d: d["terms"][1].update(end="2027-07-10"))
    code, out = run(content)
    assert code == 1
    assert "must run February to June" in out


def test_calendar_needs_one_winter_and_one_spring(content):
    edit(content, CALENDAR, lambda d: d["terms"][1].update(semester=1))
    code, out = run(content)
    assert code == 1
    assert "one of each" in out


def test_exam_period_type_must_be_regular_or_resit(content):
    edit(content, CALENDAR, lambda d: d["exam_periods"][2].update(type="makeup"))
    code, out = run(content)
    assert code == 1
    assert '"regular", "resit"' in out


# ---- Department entity decisions ----------------------------------------------------------

DEPARTMENT = "example-university/computer-science/general/department.json"
PARTNERS = "example-university/computer-science/general/international_partners.json"


def test_partner_type_must_be_known(content):
    edit(content, PARTNERS, lambda d: d["partners"][0].update(type="visit"))
    code, out = run(content)
    assert code == 1
    assert '"erasmus", "exchange", "double_degree", "other"' in out


def test_partner_needs_country(content):
    edit(content, PARTNERS, lambda d: d["partners"][1].pop("country"))
    code, out = run(content)
    assert code == 1
    assert 'The field "country" is missing' in out


def test_duplicate_partner_id_is_caught(content):
    edit(content, PARTNERS, lambda d: d["partners"][1].update(id="tu-munich"))
    code, out = run(content)
    assert code == 1
    assert 'partner id "tu-munich" appears more than once' in out


# ---- Programme type and postgraduate curricula --------------------------------------------

PG = "example-university/computer-science/curriculum/msc-data-science/2026-2027.json"


def test_curriculum_needs_degree_tag(content):
    edit(content, CURRICULUM, lambda d: d.pop("degree"))
    code, out = run(content)
    assert code == 1
    assert 'The field "degree" is missing' in out


def test_degree_must_be_undergraduate_or_postgraduate(content):
    edit(content, CURRICULUM, lambda d: d.update(degree="masters"))
    code, out = run(content)
    assert code == 1
    assert '"undergraduate", "postgraduate"' in out


def test_postgraduate_needs_program(content):
    edit(content, PG, lambda d: d.pop("program"))
    code, out = run(content)
    assert code == 1
    assert "must name its programme" in out


def test_postgraduate_program_must_exist(content):
    edit(content, PG, lambda d: d.update(program="msc-magic"))
    code, out = run(content)
    assert code == 1
    assert 'The programme "msc-magic" is not in postgraduate_programs.json' in out
    assert "msc-data-science" in out


def test_postgraduate_must_be_in_its_programme_folder(content):
    src = content / PG
    dest = content / "example-university/computer-science/curriculum/wrong-folder"
    dest.mkdir()
    shutil.move(str(src), str(dest / "2026-2027.json"))
    code, out = run(content)
    assert code == 1
    assert "must be saved in curriculum/msc-data-science/" in out


def test_undergraduate_must_not_name_a_program(content):
    edit(content, CURRICULUM, lambda d: d.update(program="msc-data-science"))
    code, out = run(content)
    assert code == 1
    assert "must not name a" in out


def test_year_check_works_for_postgraduate_files(content):
    edit(content, PG, lambda d: d.update(academic_year="2020-2021"))
    code, out = run(content)
    assert code == 1
    assert "They must match" in out


# ---- Step 2 decisions ---------------------------------------------------------------------

UNIVERSITY = "example-university/university/university.json"
CS = "example-university/computer-science"
ADMIN = f"{CS}/general/administration.json"
ELECTIVES = f"{CS}/general/free_electives.json"
PROGRAMS = f"{CS}/general/postgraduate_programs.json"
MSC = f"{CS}/curriculum/msc-data-science/2026-2027.json"


def test_index_listing_a_missing_university_is_caught(content):
    edit(content, "index.json", lambda d: d["universities"].append("nowhere"))
    code, out = run(content)
    assert code == 1
    assert 'The university "nowhere" is listed here' in out


def test_listed_department_without_folder_is_caught(content):
    edit(content, UNIVERSITY, lambda d: d["departments"].append("ghost"))
    code, out = run(content)
    assert code == 1
    assert 'The department "ghost" is listed here' in out


def test_unlisted_department_is_only_a_note(content):
    edit(content, UNIVERSITY, lambda d: d.update(departments=["computer-science"]))
    code, out = run(content)
    assert code == 0, out
    assert 'The department folder "example-university/mathematics" is not listed' in out


def test_people_names_must_be_text_per_language(content):
    edit(content, f"{CS}/general/faculty.json", lambda d: d["members"][0].update(name="Alex Morgan"))
    code, out = run(content)
    assert code == 1
    assert '"name"' in out and "group in {curly brackets}" in out


def test_role_needs_exactly_one_of_faculty_id_or_person(content):
    edit(content, ADMIN, lambda d: d["roles"][0].pop("faculty_id"))
    code, out = run(content)
    assert code == 1 and 'needs either "faculty_id" or "person"' in out


def test_role_with_both_faculty_id_and_person_is_caught(content):
    edit(content, ADMIN, lambda d: d["roles"][0].update(person={"en": "Someone"}))
    code, out = run(content)
    assert code == 1 and 'has both "faculty_id" and "person"' in out


def test_other_title_only_with_rank_other(content):
    edit(content, f"{CS}/general/faculty.json", lambda d: d["members"][0].update(other_title={"en": "Reader"}))
    code, out = run(content)
    assert code == 1 and 'Use "other_title" only with the rank "other"' in out


def test_meeting_hours_must_be_text_per_language(content):
    edit(content, f"{CS}/general/faculty.json", lambda d: d["members"][0].update(meeting_hours="Mondays"))
    code, out = run(content)
    assert code == 1 and "group in {curly brackets}" in out


def test_free_elective_cannot_mix_course_and_own_details(content):
    edit(content, ELECTIVES, lambda d: d["courses"][0].update(code="1234"))
    code, out = run(content)
    assert code == 1 and 'uses "course" together with code' in out


def test_free_elective_needs_course_or_full_details(content):
    edit(content, ELECTIVES, lambda d: d["courses"][1].pop("ects"))
    code, out = run(content)
    assert code == 1 and 'must have either "course", or all of "code", "name" and "ects"' in out


def test_free_elective_pointing_at_unknown_course_is_caught(content):
    edit(content, ELECTIVES, lambda d: d["courses"][0].update(course="mathematics:9999"))
    code, out = run(content)
    assert code == 1 and "no course with that code exists there" in out


def test_teacher_in_other_department_is_checked(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].update(instructors=["mathematics:nobody"]))
    code, out = run(content)
    assert code == 1 and 'who is not in the faculty.json of the department "mathematics"' in out


def test_teacher_in_unknown_department_is_caught(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].update(instructors=["ghost:euler-leo"]))
    code, out = run(content)
    assert code == 1 and 'there is no department folder "ghost"' in out


def test_director_in_other_department_is_checked(content):
    edit(content, PROGRAMS, lambda d: d["programs"][3].update(director="mathematics:nobody"))
    code, out = run(content)
    assert code == 1 and 'who is not in the faculty.json of the department "mathematics"' in out


def test_prerequisite_in_other_department_is_checked(content):
    edit(content, CURRICULUM, lambda d: d["courses"][3].update(prerequisites=["mathematics:9999"]))
    code, out = run(content)
    assert code == 1 and "no course with that code exists there" in out


def test_plain_code_of_another_programme_gets_a_hint(content):
    edit(content, MSC, lambda d: d["courses"][2].update(prerequisites=["2010"]))
    code, out = run(content)
    assert code == 1
    assert 'is written like "computer-science/undergraduate:2010"' in out


def test_code_in_several_programmes_must_say_which(content):
    edit(content, MSC, lambda d: d["courses"].append(
        {"code": "2010", "name": {"en": "Clash"}, "level": "core", "ects": 5, "semesters": [1]}))
    code, out = run(content)
    assert code == 1
    assert "exists in several programmes" in out
    assert "computer-science/undergraduate:2010" in out or "computer-science/msc-data-science:2010" in out


def test_available_from_cannot_be_after_the_course_semester(content):
    edit(content, CURRICULUM, lambda d: d["courses"][3].update(available_from=6))
    code, out = run(content)
    assert code == 1 and "is later than the first semester it is taught in (5)" in out


def test_course_language_only_en_or_el(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].update(language="fr"))
    code, out = run(content)
    assert code == 1 and '"en", "el"' in out


def test_curriculum_needs_requirements(content):
    edit(content, CURRICULUM, lambda d: d.pop("requirements"))
    code, out = run(content)
    assert code == 1 and 'The field "requirements" is missing' in out


def test_programme_no_longer_has_its_own_ects(content):
    edit(content, PROGRAMS, lambda d: d["programs"][0].update(ects=90))
    code, out = run(content)
    assert code == 1 and '"ects" is not a field we know' in out


def test_missing_calendar_is_only_a_note(content):
    (content / "example-university" / "university" / "calendar" / "2027-2028.json").unlink()
    code, out = run(content)
    assert code == 0, out
    assert "There is no calendar file for 2027-2028" in out


# ---- Gaps found by the final check --------------------------------------------------------

def test_school_named_but_no_schools_file_is_reported(content):
    (content / "example-university" / "university" / "schools.json").unlink()
    code, out = run(content)
    assert code == 1
    assert "there is no university/schools.json" in out


def test_sector_named_but_no_sectors_file_is_reported(content):
    (content / FACULTY).parent.joinpath("sectors.json").unlink()
    code, out = run(content)
    assert code == 1
    assert "has no sectors.json" in out


def test_course_code_cannot_contain_slash_or_colon_or_space(content):
    edit(content, CURRICULUM, lambda d: d["courses"][0].update(code="10/01"))
    code, out = run(content)
    assert code == 1
    assert '"10/01" is not written in the expected form' in out
