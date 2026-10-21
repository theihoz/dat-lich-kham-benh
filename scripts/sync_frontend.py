"""Đồng bộ bản phân phối frontend từ static/; --check không ghi dữ liệu."""
import argparse
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "backend/src/main/resources/static"
TARGET = ROOT / "frontend"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    differences = []
    for source in sorted(SOURCE.rglob("*")):
        if not source.is_file():
            continue
        target = TARGET / source.relative_to(SOURCE)
        if not target.exists() or target.read_bytes() != source.read_bytes():
            differences.append(str(source.relative_to(SOURCE)))
            if not args.check:
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, target)
    extra = [str(p.relative_to(TARGET)) for p in TARGET.rglob("*")
             if p.is_file() and not (SOURCE / p.relative_to(TARGET)).exists()]
    if extra:
        raise SystemExit("Có file chỉ tồn tại ở frontend, cần xem xét: " + ", ".join(extra))
    print(("Khác biệt" if args.check else "Đã đồng bộ") + f": {len(differences)} file")
    if args.check and differences:
        print("\n".join(differences))
        raise SystemExit(1)


if __name__ == "__main__":
    main()
