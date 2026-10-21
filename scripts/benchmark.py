"""Đo HTTP GET danh sách bác sĩ trên localhost; không ghi dữ liệu nghiệp vụ."""
import argparse
import getpass
import json
import os
import statistics
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="http://localhost:8080")
    parser.add_argument("--requests", type=int, default=60)
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--email", default="bn.hoangminh@gmail.com")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if urlparse(args.base).hostname not in {"localhost", "127.0.0.1", "::1"}:
        parser.error("Chỉ đo trên localhost dành cho demo/test.")
    if not 1 <= args.requests <= 500 or not 1 <= args.workers <= 8:
        parser.error("requests: 1..500; workers: 1..8")
    password = os.environ.get("CLINIC_BENCH_PASSWORD") or getpass.getpass("Mật khẩu demo: ")
    payload = json.dumps({"email": args.email, "password": password}).encode()
    login = urllib.request.Request(args.base + "/api/v1/auth/login", payload,
                                   {"Content-Type": "application/json"})
    with urllib.request.urlopen(login, timeout=15) as response:
        token = json.load(response)["data"]["accessToken"]
    headers = {"Authorization": "Bearer " + token}

    def sample(_):
        start = time.perf_counter()
        request = urllib.request.Request(args.base + "/api/v1/doctors?sort=feeAsc&size=100", headers=headers)
        with urllib.request.urlopen(request, timeout=15) as response:
            body = json.load(response)
            if response.status != 200 or not body.get("success"):
                raise RuntimeError("API không trả kết quả thành công")
        return (time.perf_counter() - start) * 1000, len(body["data"])

    for _ in range(5):
        sample(None)
    start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        samples = list(pool.map(sample, range(args.requests)))
    seconds = time.perf_counter() - start
    times = sorted(s[0] for s in samples)
    result = {"measured_at": datetime.now(timezone.utc).isoformat(), "endpoint": "/api/v1/doctors?sort=feeAsc&size=100",
              "requests": args.requests, "workers": args.workers, "warmup": 5,
              "rows_per_response": samples[0][1], "mean_ms": round(statistics.mean(times), 2),
              "p95_ms": round(times[max(0, (95 * len(times) + 99) // 100 - 1)], 2),
              "max_ms": round(max(times), 2), "requests_per_second": round(len(times) / seconds, 2),
              "scope": "Local HTTP sample; record active profile, hardware and DB separately. Not a production capacity estimate."}
    print(json.dumps(result, indent=2, ensure_ascii=False))
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
