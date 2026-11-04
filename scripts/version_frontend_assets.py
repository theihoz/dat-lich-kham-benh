"""Version local JS/CSS references by content hash; --check validates without writing."""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "backend/src/main/resources/static"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    assets = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()[:12]
              for p in ROOT.rglob("*") if p.is_file() and p.suffix in (".js", ".css")}
    changed = []
    for page in ROOT.rglob("*.html"):
        original = page.read_text()

        def replace(match):
            attr, quote, reference = match.groups()
            if re.match(r"(?:https?:)?//", reference):
                return match.group(0)
            clean = reference.split("?", 1)[0]
            target = ROOT / clean.lstrip("/") if clean.startswith("/") else page.parent / clean
            try:
                relative = target.resolve().relative_to(ROOT.resolve()).as_posix()
            except ValueError:
                return match.group(0)
            if relative not in assets:
                return match.group(0)
            return f"{attr}={quote}{clean}?v={assets[relative]}{quote}"

        updated = re.sub(r'''\b(src|href)=(["'])([^"']+)["']''', replace, original)
        if updated != original:
            changed.append(page.relative_to(ROOT).as_posix())
            if not args.check:
                page.write_text(updated)
    manifest = json.dumps(assets, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    output = ROOT / "asset-manifest.json"
    if not output.exists() or output.read_text() != manifest:
        changed.append("asset-manifest.json")
        if not args.check:
            output.write_text(manifest)
    print(f"{'Outdated' if args.check else 'Updated'} asset references: {len(changed)} files")
    if args.check and changed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
