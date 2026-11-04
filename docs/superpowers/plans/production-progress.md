# Execution ledger — plan: docs/superpowers/plans/2026-11-04-production-readiness.md

- Baseline: user-authorized changes from previous debug turn remain uncommitted; preserve them.
- Spec/plan created 2026-11-04 before implementation; user explicitly requested create then execute all stages.
- Pre-flight shared interfaces: Task1 auth client -> Task2 cookie/memory contract; UI agent owns pages, auth agent owns api/auth JS and account pages after coordination.
- Pre-flight shared interfaces: Tasks2/3 new entities -> Task4 migration; agents supply schema fragments, root assembles.
- Pre-flight shared interfaces: Task3 MedicalRecordService overlaps previous debug fixes; retain ownership403 and existing transaction semantics.
- Environment: Docker client installed; initial sandbox connection denied, recheck outside sandbox before marking infrastructure blocked.
- Task 1: complete locally - UI19, client5, responsive/CSP49, real browser8 + account UI4; external clinical/accessibility pilot pending.
- Task 2: complete locally - session/reset/verify/revocation tests, real Redis429, MySQL refresh and login/password race tests.
- Task 3: complete locally - audit/revisions/outbox/reminder; real Rabbit confirms/SMTP and bounded retry/DLQ tested.
- Task 4: code/configuration complete and locally validated - migrations/Docker/backup/restore/bootstrap/metrics; operator-owned domain/credentials/receivers and staging rollout remain external acceptance.
- Task 5: local acceptance complete -88 ordinary +8 external tests, frontend/auth/ops/browser checks, Java audit0 HIGH/CRITICAL. See acceptance report for scope and external gates.

- User steering: temporary host port 18080; preserve other projects on8080; removed run.bat automatic process killing. Container-internal port remains8080, exposed host18080.
- Task4 evidence: real MySQL DDL probe14tables; encrypted backup restored into isolated database with14tables/1testrow.
- Delegated source work delivered; subagents hit account usage limits before final review. Root continues integration and verification.

- Ruling: initial Boot3.3 constraint superseded after actual scan54HIGH/CRITICAL; migrate to Boot4.1.2 + Rabbit client5.34.0, keep Java21 and API. Candidate isolated in/tmp passed88standard/7real tests before root changes. Jackson2compat deprecated but patched; track eventual Jackson3 migration, no API rewrite in this release.

- Final review: coordinator self-review after delegated-agent quota exhaustion; cross-module testing and MySQL regressions supplied separate execution evidence, not an independent security reviewer.
- Final fixed: concurrent login/password-change race - MySQL test RED expectedfalse/actualtrue, then GREEN after credential check and issue shared user lock. Standard88 and external8 suites green.
- Ruling: keep a bounded latest100 analytics-detail list with explicit UI notice; all totals/charts aggregate the entire qualifying dataset. Full detail export is outside this release; cost is limited historical-detail browsing in that view.
- Ruling: keep patched Jackson2 compatibility for current message/cache contracts; cost is a later Jackson3 migration before upstream compatibility removal.
- Final minor deferred: authenticated backup signing/archives and a full manual accessibility audit remain operator/security follow-up; encryption/checksum and automatic UI checks are not equivalent guarantees.
- No external production deployment, messages to real patients, or paid infrastructure provisioning was performed. Demo remains at localhost18080; do not terminate services owned by other projects.

- Final fixed: broker consumer DLQ metric missing - real RabbitMQ regression RED then GREEN; added clinic_notifications_consumer_dead and critical alert, separate from outbox publisher failures.

- Final account-browser acceptance: registration, delivered verification link, forgot/reset UI, consumed token/old password rejection and new-password login passed; tokens absent from localStorage. Added script to CI.
- Final cleanup: auxiliary prod QA and integration stack stopped without deleting data; final demo image healthy on localhost18080.
