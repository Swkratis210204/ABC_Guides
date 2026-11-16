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
    assert 'needs course "9999" first' in out


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
    assert "is not a language code" in out


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
