#!/usr/bin/env python3
from __future__ import annotations
import argparse
import json
from pathlib import Path
import sys

try:
    import jsonschema
except ImportError:
    print("Missing jsonschema. Install with: python -m pip install -r tools/requirements.txt", file=sys.stderr)
    raise SystemExit(2)

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("trip", nargs="?", default="trip-package/starter/trip.json")
    parser.add_argument("--schema", default="trip-package/schema/trip.schema.json")
    args = parser.parse_args()

    trip = json.loads(Path(args.trip).read_text(encoding="utf-8"))
    schema = json.loads(Path(args.schema).read_text(encoding="utf-8"))

    validator_cls = jsonschema.validators.validator_for(schema)
    validator_cls.check_schema(schema)
    validator = validator_cls(schema, format_checker=jsonschema.FormatChecker())
    errors = sorted(validator.iter_errors(trip), key=lambda e: list(e.absolute_path))

    if errors:
        print(f"FAIL: {len(errors)} schema error(s)")
        for error in errors[:50]:
            path = ".".join(str(p) for p in error.absolute_path) or "<root>"
            print(f"- {path}: {error.message}")
        return 1

    print(f"PASS: {args.trip} validates against {args.schema}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
