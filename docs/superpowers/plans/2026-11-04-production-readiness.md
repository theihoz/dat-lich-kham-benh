# Production Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Hoàn thiện giao diện, bảo mật phiên, dữ liệu/thông báo và bộ công cụ production theo spec.
**Architecture:** Nâng cấp tại chỗ Spring Boot phân tầng. Các module UI/auth/delivery dùng giao diện công bố dưới đây; điều phối nghiệm thu theo thứ tự task, không ghi đè thay đổi sẵn có.
**Tech Stack:** Java21, Spring Boot3.3, MySQL8, Redis7, RabbitMQ3, HTML/CSS/JS, Node tests, Playwright, Docker Compose.
**Spec:** docs/superpowers/specs/2026-11-04-production-readiness-design.md

## Global Constraints
- Giữ schema hiện có; thêm bảng/cột có migration, không xóa dữ liệu; không commit tự động các thay đổi từ lượt trước.
- Profile production tên prod; demo phải chỉ định rõ; public URL qua APP_PUBLIC_URL.
- Kiểm thử thật khi có Docker; thiếu môi trường ghi blocked rõ thay vì thay bằng mock rồi tuyên bố đạt.
- Shared filesystem: agent chỉ sửa phần được giao; chỉ root chạy Maven toàn suite tránh race target.
- Spec/plan được tạo trước triển khai theo yêu cầu trực tiếp của người dùng; giữ ledger trong docs/superpowers/plans/production-progress.md.

## Review Focus
- Refresh đồng thời/replay, logout/đổi mật khẩu vẫn dùng access token cũ: Task2 tests.
- Dữ liệu tên/triệu chứng chứa nháy/HTML, mobile modal và bàn phím: Task1 tests.
- Crash giữa commit và publish/SMTP retry: Task3 tests và ghi hạn chế SMTP.
- Migration DB cũ/backup restore nhầm DB: Task4 validate/script tests.
- Thiếu secrets, Docker unavailable, staging rollback lỗi: Task4/5 không kết luận production-ready.

### Task 1: Giao diện thống nhất và an toàn
**Files:** static/css/style.css; static/js/{api,auth,utils,patient-dashboard}.js; static/**/*.html; scripts/frontend_regression.cjs; scripts/browser_qa.cjs; frontend/ sync.
**Interfaces:** API giữ hợp đồng hiện có trừ auth ở Task2; client public API Auth.login/checkAuth/logout và Api._request vẫn dùng bởi các màn. Cấu hình clinic GET /api/v1/clinic-info do root cung cấp.
- [ ] Viết regression lỗi layout, XSS các trang còn lại, submit duplicate, slot409 giữ triệu chứng, unsaved warning.
- [ ] Chạy Node tests để thấy RED, sửa module/layout/mobile/4 bước booking/states/accessibility và escape.
- [ ] Chạy Node GREEN, sync frontend, browser desktop/mobile và keyboard; ghi evidence.

### Task 2: Phiên, reset/verify và abuse protection
**Files:** security/*; config/SecurityConfig.java; service/impl/AuthServiceImpl.java; controller/AuthController.java; entity/repository mới cho auth; DTO auth; static/js/api.js/auth.js và login/register UI phối hợp Task1.
**Interfaces:** NotificationDeliveryService.enqueueAccountMessage(type,email,token) dùng public URL và message body an toàn; nếu module khác chưa cung cấp có account notification class riêng. Cookie clinic_refresh HttpOnly; access token memory; login trả userId,email,role,accessToken không refreshToken cho browser. Origin guard áp dụng refresh/logout; API bearer tài liệu hóa.
- [ ] Viết test session rotation/replay/logout/password revoke/reset expiry/verify/rate limit/Origin.
- [ ] RED→GREEN backend module; frontend memory access + cookie refresh, không còn refresh token localStorage; login/register/reset/verify hoạt động.
- [ ] Cập nhật migration fragments schema-auth.sql (root tích hợp), focused tests và full auth/browser login.

### Task 3: Audit, outbox, reminder và truy vấn
**Files:** messaging/*; audit/*; service/impl/AsyncEmailNotificationServiceImpl.java; MedicalRecordServiceImpl.java; repository/* liên quan; entity mới; scheduler mới.
**Interfaces:** OutboxService.enqueue(EmailNotificationMessage) ghi trong giao dịch hiện tại; audit record gồm actor/action/entity/time và revision. Không thay trường record message mà không cập nhật mọi constructor/test.
- [ ] Tests rollback/outbox/dedupe/retry/reminder/cross-user audit; đọc/ghi bệnh án audit đúng principal không chứa PHI trong log.
- [ ] RED→GREEN outbox + dispatcher confirms/consumer dedupe, scheduler 24h, audit/revisions; schema-delivery.sql cung cấp root.
- [ ] Sửa aggregate/findAll ở các luồng dữ liệu lớn, giữ hoặc version contract; focused/full tests.

### Task 4: Production profile, migration và operations
**Files:** application-prod.yml; config/ProductionSafetyConfig.java; resources/db/migration/*; pom.xml; Dockerfile; compose.production.yml; ops/*; scripts/backup*.sh/restore*.sh; .env.production.example; docs/operations.md; workflows/*.
**Interfaces:** Tích hợp auth/delivery schema fragments sau Tasks2/3; APP_PUBLIC_URL và CLINIC_ADDRESS; mọi credential qua env; ddl validate.
- [ ] Tests production rejects demo/missing secrets, migrations fresh schema and known upgrades, shell backup refuses unsafe restore; RED→GREEN.
- [ ] Tạo profile fail-fast, Flyway migration/schema; TLS proxy/health checks, Redis/broker authenticated internal-only, backup/restore/retention và monitoring config.
- [ ] CI services MySQL/Redis/Rabbit/SMTP và browser tests; deploy staging/production manual environments gated CI + rollback by immutable image tag.
- [ ] Chạy Docker thực nếu available; ghi rõ bước không chạy do môi trường.

### Task 5: Tích hợp và nghiệm thu
**Files:** tests integration/browser; docs/evidence/*; README.md; spec/plan/progress/runbook.
- [ ] Review diff/cross-module contracts và fix findings nghiêm trọng bằng regression tests.
- [ ] clean verify + frontend regression + sync/diagrams + browser 375/768/1440; CI config/scripts lint.
- [ ] Smoke demo và prod fail-fast; MySQL/Redis/Rabbit/SMTP và backup restore thực khi có Docker.
- [ ] Cập nhật ledger từng task với lệnh/output, deviations/blocked; chốt báo cáo phần đã đạt và điều kiện còn cần nghiệm thu, không gọi triển khai thật đã hoàn tất.
