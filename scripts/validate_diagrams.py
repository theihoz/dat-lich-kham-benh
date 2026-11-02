#!/usr/bin/env python3
"""Kiểm tra các file draw.io trong docs/diagrams: XML hợp lệ, có ít nhất một trang và một ô vẽ."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parent.parent / "docs" / "diagrams"
files = sorted(root.glob("*.drawio"))
errors = []
for f in files:
    try:
        tree = ET.parse(f)
    except ET.ParseError as e:
        errors.append(f"{f.name}: XML lỗi ({e})")
        continue
    diagrams = tree.getroot().iter("diagram")
    if not list(diagrams):
        errors.append(f"{f.name}: không có thẻ <diagram>")
    if not list(tree.getroot().iter("mxCell")) and not any(d.text and d.text.strip() for d in tree.getroot().iter("diagram")):
        errors.append(f"{f.name}: không có ô vẽ nào")
if not files:
    errors.append(f"Không tìm thấy file .drawio trong {root}")
for e in errors:
    print("LỖI", e)
print(f"Đã kiểm tra {len(files)} file draw.io, {len(errors)} lỗi")
sys.exit(1 if errors else 0)
