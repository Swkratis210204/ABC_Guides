"""The sample data must use every field and every allowed value the schema offers.

If someone adds a field or an allowed value to a schema, these tests fail until the sample
content shows it, so the sample content can never fall behind the schema.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCHEMAS = ROOT / "schema"
CONTENT = ROOT / "content"

# Which schema describes which file name
KIND_BY_FILE = {
    "university.json": "university", "schools.json": "schools", "index.json": "index",
    "international_partners.json": "international_partners", "grading_scale.json": "grading_scale",
    "free_electives.json": "free_electives", "department.json": "department",
    "administration.json": "administration", "sectors.json": "sectors", "faculty.json": "faculty",
    "postgraduate_programs.json": "postgraduate_programs",
}


def kind_of(path):
    if path.name in KIND_BY_FILE:
        return KIND_BY_FILE[path.name]
    if path.parent.name == "calendar":
        return "calendar"
    if "curriculum" in path.parts:
        return "curriculum"
    raise AssertionError(f"Unknown content file: {path}")


COMMON = json.loads((SCHEMAS / "common.schema.json").read_text(encoding="utf-8"))["$defs"]


def resolve(node):
    ref = node.get("$ref")
    return COMMON[ref.split("/")[-1]] if ref else node


def schema_fields(node, prefix=""):
    """Yield (path, enum_values_or_None) for every field the schema allows."""
    node = {**resolve(node), **{k: v for k, v in node.items() if k != "$ref"}}
    if node.get("type") == "object" and "properties" in node:
        for name, sub in node["properties"].items():
            sub_resolved = {**resolve(sub), **{k: v for k, v in sub.items() if k != "$ref"}}
            path = f"{prefix}.{name}" if prefix else name
            yield path, sub_resolved.get("enum")
            yield from schema_fields(sub, path)
    elif node.get("type") == "array" and "items" in node:
        yield from schema_fields(node["items"], prefix + "[]")


def data_fields(value, prefix="", out=None):
    """Collect every field path used in the data, and the values seen at each path."""
    out = {} if out is None else out
    if isinstance(value, dict):
        for k, v in value.items():
            path = f"{prefix}.{k}" if prefix else k
            out.setdefault(path, set())
            data_fields(v, path, out)
    elif isinstance(value, list):
        for item in value:
            data_fields(item, prefix + "[]", out)
    else:
        out.setdefault(prefix, set()).add(value)
    return out


def used_by_kind():
    used = {}
    for file in CONTENT.rglob("*.json"):
        kind = kind_of(file)
        fields = data_fields(json.loads(file.read_text(encoding="utf-8")))
        bucket = used.setdefault(kind, {})
        for path, values in fields.items():
            bucket.setdefault(path, set()).update(values)
    return used


def test_every_schema_field_is_used_in_sample_content():
    used, missing = used_by_kind(), []
    for schema_file in sorted(SCHEMAS.glob("*.schema.json")):
        kind = schema_file.name.replace(".schema.json", "")
        if kind == "common":
            continue
        schema = json.loads(schema_file.read_text(encoding="utf-8"))
        for path, _ in schema_fields(schema):
            if path not in used.get(kind, {}):
                missing.append(f"{kind}: {path}")
    assert not missing, "Sample content never uses these fields:\n  " + "\n  ".join(missing)


def test_every_allowed_value_is_used_in_sample_content():
    used, missing = used_by_kind(), []
    for schema_file in sorted(SCHEMAS.glob("*.schema.json")):
        kind = schema_file.name.replace(".schema.json", "")
        if kind == "common":
            continue
        schema = json.loads(schema_file.read_text(encoding="utf-8"))
        for path, enum in schema_fields(schema):
            for value in enum or []:
                if value not in used.get(kind, {}).get(path, set()):
                    missing.append(f'{kind}: {path} never uses the value "{value}"')
    assert not missing, "Sample content never uses these allowed values:\n  " + "\n  ".join(missing)


def test_sample_has_more_than_one_university_department_and_year():
    unis = [p for p in CONTENT.iterdir() if p.is_dir()]
    assert len(unis) >= 2
    depts = [d for u in unis for d in u.iterdir() if d.is_dir() and d.name != "university"]
    assert len(depts) >= 3
    years = {f.stem for f in CONTENT.rglob("curriculum/*.json")}
    assert len(years) >= 2
