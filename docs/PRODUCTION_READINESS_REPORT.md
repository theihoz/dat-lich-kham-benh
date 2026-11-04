# Production readiness acceptance - 2026-11-04

Implementation and local acceptance are complete for the planned application changes. This report does **not** approve serving real patients or claim that an external production deployment has occurred.

## Delivered

| Area | Result |
|---|---|
| UI | Shared layout/navigation, mobile views, booking review before confirmation, conflict recovery preserving symptoms, pending-submit guards, retry/empty states and unsaved clinical changes warning. |
| Browser safety | Inline scripts/handlers moved into external modules with an explicit action allowlist; escaped user data; strict script CSP; locally vendored assets, licenses, hashes and versioned asset URLs. |
| Accounts | Memory-only access tokens, HttpOnly refresh cookie, persisted hashed refresh sessions, rotation/replay detection, logout/password-change revocation, reset/verification tokens, Redis production rate limiting and Origin/CSRF checks for cookie actions. |
| Clinical data | Ownership checks, durable read/write audit and medical record revisions; denied reads are audited with the requesting actor. |
| Delivery | Transactional outbox, after-commit wakeup, publisher confirms, durable receipt deduplication, bounded retry/DLQ and reminder scheduler. SMTP remains at-least-once across the send/commit crash window. |
| Queries | Bounded database search/paging for production doctor directory and admin users/doctors/patients/appointments; aggregate analytics queries. Analytics detail is limited to the latest100 completed appointments and explicitly labels that limit; totals/charts use all qualifying records. |
| Deployment | Fail-fast prod configuration, versioned migrations/validate-only ORM, no production seed, internal data services, TLS proxy configuration, immutable-image deploy/rollback scripts and gated CI workflows. |
| Operations | Encrypted backup/isolated restore, portable checksum, backup metrics/timer, Prometheus/Alertmanager/Grafana configuration and restricted metrics credential; offline first-admin bootstrap. |
| Runtime | Java21, Spring Boot4.1.2, Rabbit AMQP client5.34.0; container clock fixed to Asia/Ho_Chi_Minh. Local HTTP18080; container-internal8080. |

## Verification

| Check | Observed result |
|---|---|
| Maven clean verify | 96 discovered, 88 ordinary tests passed, 8 external-service tests intentionally skipped in the default run. |
| RealServicesIntegrationTests | All8 passed separately on actual MySQL8, Redis7, RabbitMQ3 and local SMTP/Mailpit. |
| Frontend regression | 19 passed. |
| Auth browser-client regression | 5 scenarios passed. |
| Operations safety tests | 4 passed. |
| Browser UI fixtures | 49 checks across15 pages at375/768/1440px, strict CSP, escaped input, labels, booking conflict/retry and keyboard focus; fixtures do not prove backend integration. |
| Browser with real backend | 8 core flows passed on the built Docker image: patient login/sorting/booking/cancellation, doctor read/edit/reload, admin analytics and after-commit demo inbox. No JS errors or failed application requests. |
| Account browser flows | 4 additional groups passed: register, delivered verification link/token removal, forgot/reset UI, consumed-token/old-password rejection and login with the new password without persisted credentials. |
| Prod-profile black-box smoke | Metrics401 without credential/200 with metrics credential; same credential cannot access admin API; demo page404; strict script CSP/no-store; actual Redis login throttling429. |
| Production startup safety | Image rejects missing credentials and DEBUG mode; valid fixture prod configuration starts and validates migrated MySQL schema. |
| Migrations/restore/bootstrap | MySQL migrations validated; encrypted backup restored into an isolated clone with14 tables and the expected test row; offline bootstrap created one test admin and rejected a second bootstrap. |
| Docker | Production image built successfully; demo/prod-profile fixture containers passed readiness/health checks. |
| Artifact dependency audit | Initial54 HIGH/CRITICAL findings; final packaged Java dependencies have0 HIGH/CRITICAL findings recognized by Trivy at scan time. Nonempty JAR detection verified. This is not a guarantee against every vulnerability or an OS/frontend security certification. |
| Assets/docs | Asset-manifest check and frontend sync:0 differences; all15 draw.io documents valid; shell syntax and diff whitespace checks passed. |

The final MySQL regression also reproduces and fixes a login/password-change race: an old password could receive a new-version session after waiting for a user lock. Password verification and session creation now share the same locked transaction. The regression failed before the fix and passed afterwards.

## Evidence and reproduction

- `docs/evidence/production-acceptance-2026-11-04.json`: machine-readable scope and counts.
- `docs/evidence/production-browser-qa.json`: real browser/backend flow result.
- `docs/evidence/production-account-browser.json`: real account UI and demo-inbox flow result.
- `docs/evidence/production-ui-fixtures.json`: responsive/CSP fixture checks.
- `docs/evidence/production-profile-smoke.json`: prod endpoint/Redis throttling checks.
- `docs/evidence/production-dependency-audit.json`: final artifact scanner result.
- Screenshots and full transient logs remain under `/tmp/clinic-production-browser-evidence` and `/tmp/clinic-*.log` on this workstation; CI uploads its own evidence.

```sh
sh scripts/maven.sh -f backend/pom.xml clean verify
node scripts/frontend_regression.cjs
node scripts/auth_regression.cjs
python3 scripts/operations_regression.py
python3 scripts/version_frontend_assets.py --check
python3 scripts/sync_frontend.py --check
python3 scripts/validate_diagrams.py
docker compose -f compose.integration.yml up -d --wait
CLINIC_REAL_SERVICES=true sh scripts/maven.sh -f backend/pom.xml -Dtest=RealServicesIntegrationTests test
```

Use the Playwright runtime described in README for `scripts/ui_browser_regression.cjs` and `scripts/browser_qa.cjs`; run `scripts/account_browser_qa.cjs` on the isolated demo for registration/reset/verification. `scripts/production_smoke.py` accepts a secure METRICS_TOKEN environment and a configured prod-profile base URL; the rate-limit fixture is restricted to the isolated localhost18082 QA endpoint.

## Remaining operator acceptance

1. Configure the clinic-owned domain/DNS and prove external TLS renewal; configure the actual clinic location/phone and first administrator identity.
2. Verify outbound SMTP/SPF/DKIM/DMARC and delivery to real mailboxes. Local Mailpit proves protocol integration only.
3. Configure GitHub environment reviewers, server/registry/SSH credentials, monitoring token file, a real alert receiver and strong Grafana credentials. The example alert URL is deliberately a placeholder.
4. Rehearse restore and rollback with a production-sized clone, measure the24h RPO/4h RTO targets, and arrange encrypted off-host backup storage/key recovery. Local fixture restore does not establish these targets for real data.
5. Validate/baseline any existing database against V1 on a clone before migration. No automatic baseline or destructive migration is enabled.
6. Clinic owners must approve retention/access policies for clinical revisions, audit, token/outbox data and backups; complete clinical/mobile/accessibility pilot acceptance. Automated checks do not certify full WCAG compliance.

The final broad review was performed by the coordinating agent after delegated agents reached their account quota; it was supported by cross-module tests and the new MySQL race regression, but is not an independent security audit.

## Follow-up limits

- Jackson2 compatibility is intentionally temporary and deprecated upstream; migrate the manual API/cache/outbox mappers to Jackson3 before removing that module in a later framework upgrade.
- Analytics detail intentionally shows a bounded latest100-record subset. Full-detail date-filtered browsing/export would be a separate extension; the UI makes the current limit explicit.
- Backup encryption/checksum detects accidental corruption; protect storage and consider authenticated archives/signing against malicious replacement, as described in the operations runbook.
