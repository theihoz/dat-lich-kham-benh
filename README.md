# Đặt lịch khám bệnh — Đồ án Design Patterns và SOLID

Ứng dụng phòng khám dành cho **Bệnh nhân, Bác sĩ, Quản trị viên**. Trọng tâm đồ án là hiện thực mẫu thiết kế trong nghiệp vụ có lưu dữ liệu, kèm phân tích, sơ đồ draw.io và kiểm thử.

## Tài liệu

- [Kịch bản demo và test case](docs/KICH_BAN_DEMO.md).
- [Sơ đồ draw.io có thể chỉnh sửa](docs/diagrams): kiến trúc, Use Case, ERD, và Class/Sequence cho từng pattern (Builder, Factory Method, Adapter, Strategy, Observer, Facade).
- [Lược đồ cơ sở dữ liệu](schema.md).
- [Ảnh minh họa và số liệu đo](docs/evidence).
- [Bộ Postman collection](DatLichKhamBenh.postman_collection.json).

Mở file `.drawio` bằng **draw.io Desktop** hoặc **diagrams.net → File → Open from → Device**.

## Chạy demo trên lớp (không cần Docker)

Yêu cầu **JDK 21** và Maven 3.9+.

> **Lưu ý JDK:** dự án chỉ kiểm thử với JDK 21. Nếu máy có JDK mới hơn (ví dụ 27) làm mặc định, 5 test tích hợp sẽ lỗi `Java 27 (71) is not supported by the current version of Byte Buddy`. Trên macOS hãy chọn JDK 21 trước khi chạy:
>
> ```bash
> export JAVA_HOME=$(/usr/libexec/java_home -v 21)
> ```

Từ thư mục gốc:

```bash
sh run-demo.sh
```

Hoặc:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Windows: chạy lệnh Maven trên trong PowerShell/CMD. `scripts/maven.sh` cũng tự tìm Maven đi kèm Oracle Java extension của VS Code trên macOS nếu máy chưa có lệnh `mvn`; có thể chỉ định đường dẫn qua biến `MVN`.

Đợi `http://localhost:18080/actuator/health/readiness` báo `UP` (khoảng 8 giây, sau khi seed xong) rồi mở [ứng dụng](http://localhost:18080), [trang Design Patterns và hộp thư demo](http://localhost:18080/patterns.html), [health check](http://localhost:18080/actuator/health).

| Vai trò | Email demo | Mật khẩu |
|---|---|---|
| Bệnh nhân | `bn.hoangminh@gmail.com` | `Patient@123` |
| Bệnh nhân | `patient@clinic` | `user@2026` |
| Bác sĩ có bệnh án mẫu | `bs.tranthib@clinic.vn` | `Doctor@123` |
| Bác sĩ | `doctor@clinic` | `doctor@2026` |
| Admin | `admin@clinic.vn` | `Admin@123` |
| Admin | `admin@clinic` | `admin@2026` |

Đăng nhập qua API: `POST /api/v1/auth/login` với body `{"email": "...", "password": "..."}`.

Profile `demo` dùng H2 **trong RAM**, khởi tạo dữ liệu mẫu vào các bảng thật và đặt lại dữ liệu khi khởi động lại. Thông báo được xử lý sau commit vào hộp thư cục bộ, chỉ Admin được đọc; không gửi SMTP. Redis được thay bằng cache no-op, RabbitMQ listener tắt. Giao diện hiện vẫn tải Bootstrap/Chart.js từ CDN nên cần mạng để hiển thị đầy đủ.

## Các pattern được áp dụng (6 pattern)

| Nhóm | Pattern | Vị trí trong luồng nghiệp vụ |
|---|---|---|
| Creational | Builder | `BookingServiceImpl` dùng `Appointment.AppointmentBuilder` tạo lịch hẹn |
| Creational | Factory Method | Ba `NotificationCreator` tạo ba Product xác nhận/hủy/nhắc lịch |
| Structural | Adapter | `NotificationSender` → `SmtpNotificationAdapter` → JavaMail; demo dùng hộp thư cục bộ |
| Behavioral | Strategy | `DoctorSortStrategy` có bốn thuật toán; màn hình tìm bác sĩ gọi API theo lựa chọn |
| Structural | Facade | `AppointmentBookingFacade` gom luồng đặt/hủy lịch cho `BookingController` |
| Behavioral | Observer | Sự kiện thông báo → listener bất đồng bộ `AFTER_COMMIT`; rollback không gửi |

Đây là ứng dụng **phân tầng**, với ba tầng logic giao diện, nghiệp vụ, lưu trữ; security và tích hợp là các khối hỗ trợ. Xem [sơ đồ kiến trúc](docs/diagrams/architecture.drawio) và [schema.md](schema.md).

## Chức năng và giới hạn phạm vi

- Bệnh nhân đăng ký/đăng nhập, cập nhật hồ sơ, tìm và sắp xếp bác sĩ, chọn ca, đặt/hủy lịch, xem lịch sử và bệnh án của mình.
- Bác sĩ quản lý ca, xem lịch hẹn, đọc/cập nhật bệnh án của lịch mình phụ trách; ghi bệnh án chuyển lịch sang COMPLETED.
- Admin duyệt bác sĩ, quản lý người dùng/chuyên khoa/dịch vụ, xem thống kê từ lịch hẹn đã lưu.
- Đặt lịch khóa bi quan trên Schedule; unique schedule_id và booking_code bảo vệ ở cơ sở dữ liệu.
- Hủy trước hơn 24 giờ: giữ lịch hẹn cũ CANCELLED, tạo ca thay thế cùng khung giờ với ID mới.
- Phí khám thống kê là tổng giá lịch **COMPLETED**, chưa phải số tiền thực thu. Không có tích hợp thanh toán.
- Creator nhắc lịch đã có và được kiểm thử; chưa có scheduler tự động.
- Luồng RabbitMQ chưa dùng transactional outbox: việc commit lịch và chuyển thông báo sang queue chưa đảm bảo giao nhận nguyên tử.

Các API không tự chèn bệnh án, lịch hẹn hay doanh thu giả. Dữ liệu mẫu chỉ được khởi tạo qua `DataInitializer` trong profile dev/demo.

## Chạy với MySQL, Redis, RabbitMQ

```bash
docker compose up -d
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

| Thành phần | Cổng host |
|---|---|
| Spring Boot | 8080 |
| MySQL | 3307 |
| Redis | 6379 |
| RabbitMQ | 5672 |
| RabbitMQ Management | 15672 |

Cấu hình bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `RABBITMQ_HOST`, `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`. Ngoài dev/demo/test, phải cấp `JWT_SECRET`. Chế độ dev dùng queue và SMTP; chưa cấu hình SMTP thì chưa chứng minh gửi email đến hộp thư thật.

Chạy luôn backend trong container cùng MySQL/Redis/RabbitMQ:

```bash
docker compose --profile full up -d --build
```

Hoặc chỉ chạy image demo độc lập (H2 trong RAM, không cần dịch vụ nào khác):

```bash
docker build -t clinic-backend backend
docker run --rm -e SPRING_PROFILES_ACTIVE=demo -p 18080:8080 clinic-backend
```

Lưu ý: dữ liệu seed chỉ được tạo khi bảng còn trống và các ca khám tính theo ngày seed. Volume MySQL cũ sẽ chứa ca đã quá hạn; chạy `docker compose down -v` để seed lại từ đầu (xóa dữ liệu dev).

Docker Desktop macOS: nếu chạy backend trong container, publish cổng `-p 18080:8080` và trỏ DB/Redis/RabbitMQ về `host.docker.internal` với các cổng host phía trên.

## Kiểm thử và đóng gói

Dùng JDK 21 (xem lưu ý ở trên).

```bash
sh scripts/maven.sh -f backend/pom.xml test       # 88 standard tests; 8 real-service tests run separately
node scripts/frontend_regression.cjs             # 19 frontend regression tests
sh scripts/maven.sh -f backend/pom.xml package
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Nếu đã có lệnh `mvn` thì có thể chạy `cd backend && mvn test`.

Các script phụ trợ (cần Python 3; đã chạy khi server demo đang bật):

```bash
python3 scripts/sync_frontend.py --check          # kiểm tra frontend/ khớp static/
CLINIC_BENCH_PASSWORD='Patient@123' \
  python3 scripts/benchmark.py --requests 60 --workers 4 --output docs/evidence/benchmark.json
```

Benchmark chỉ gọi `GET /api/v1/doctors` trên localhost, không ghi dữ liệu nghiệp vụ. Trên máy phát triển (profile demo) đo được khoảng 5 ms trung bình, p95 khoảng 8 ms, không phải ước tính tải production.

Kiểm thử browser bằng Playwright (cần Google Chrome đã cài; script dùng `channel: 'chrome'`). Cài package ở ngoài repo rồi trỏ `NODE_PATH`:

```bash
mkdir -p ~/pw && (cd ~/pw && npm init -y && npm i playwright)
NODE_PATH=~/pw/node_modules node scripts/browser_qa.cjs
```

Có thể đặt `CLINIC_BASE_URL` để kiểm tra demo ở cổng khác và `CLINIC_QA_OUTPUT` để lưu bằng chứng ở thư mục riêng. Xem [báo cáo kiểm tra và sửa lỗi](docs/DEBUG_REPORT.md).

Chạy khi server demo đang bật và **đã seed xong**: `/actuator/health` báo UP sớm hơn khoảng 8 giây, nên hãy chờ `/actuator/health/readiness` báo UP. Đăng nhập sớm hơn sẽ bị 401 vì tài khoản chưa được tạo. Script ghi đè ảnh và `browser-qa.json` trong `docs/evidence/`.

Script thao tác trên server demo localhost: đăng nhập, đặt/hủy một lịch, xem bệnh án, thống kê và hộp thư. Không chạy cùng cổng với môi trường thật.

## CI/CD

- `.github/workflows/ci.yml` (mỗi push lên `main`/`thaihoa` và mỗi pull request): `mvn verify` với JDK 21, kiểm tra file draw.io và đồng bộ frontend, build image Docker rồi smoke test (chờ readiness, đăng nhập tài khoản demo).
- `.github/workflows/cd.yml` (push lên `main`, tag `v*`, hoặc chạy tay): build và đẩy image lên `ghcr.io/<owner>/<repo>` với tag `latest`, `sha-…` và phiên bản.

## Cấu trúc mã nguồn

```text
backend/src/main/java/com/datlichkhambenh/backend/
  controller/       REST endpoints
  service/          Interface nghiệp vụ
  service/impl/     Luồng giao dịch và kiểm tra nghiệp vụ
  entity/           JPA entities, Builder
  repository/       JPA, truy vấn có khóa
  pattern/
    facade/         Facade đặt/hủy lịch
    notification/   Factory Method, Product, Adapter
    sorting/        Strategy và registry
  messaging/        Observer và cầu nối RabbitMQ
  demo/             Observer và inbox cục bộ cho profile demo
  security/         JWT, BCrypt, phân quyền
backend/src/main/resources/static/   Giao diện được phục vụ, bản chính
frontend/                           Bản phân phối đồng bộ
docs/                               Kịch bản demo, sơ đồ draw.io, bằng chứng
scripts/                            maven.sh, đồng bộ frontend, benchmark, browser QA
```

Sau khi chỉnh giao diện trong `static/`, chạy `python3 scripts/sync_frontend.py` để cập nhật `frontend/`; `--check` phát hiện khác biệt. Build output `backend/target/` không phải mã nguồn.

Repository: [theihoz/dat-lich-kham-benh](https://github.com/theihoz/dat-lich-kham-benh).

## Production upgrade

Runtime: Java21, Spring Boot4.1.2, AMQP client5.34.0. Jackson2 compatibility temporarily preserves the existing API/cache/outbox contract. Local HTTP defaults to **http://localhost:18080**, with container-internal port8080. No startup script terminates processes belonging to another project.

- [Spec](docs/superpowers/specs/2026-11-04-production-readiness-design.md), [plan](docs/superpowers/plans/2026-11-04-production-readiness.md), [progress](docs/superpowers/plans/production-progress.md).
- [Operations/backup/restore/deploy](docs/OPERATIONS.md), [sessions/reset/email verification](docs/auth/README.md), [outbox/audit](docs/notification-delivery.md).
- [Acceptance report](docs/PRODUCTION_READINESS_REPORT.md).

The image defaults to **prod**, with no seed and required real credentials. Explicitly select demo for classroom use. Production Compose includes TLS proxy and internal database/cache/broker. Store real settings outside Git using .env.production.example; integration credentials are fixtures only.

```sh
node scripts/auth_regression.cjs
python3 scripts/operations_regression.py
python3 scripts/version_frontend_assets.py --check
docker compose -f compose.integration.yml up -d --wait
CLINIC_REAL_SERVICES=true sh scripts/maven.sh -f backend/pom.xml -Dtest=RealServicesIntegrationTests test
```

CI blocks HIGH/CRITICAL dependency findings and covers browser/CSP/mobile plus MySQL/Redis/RabbitMQ/SMTP integration. Real deployment still requires operator-owned domain/TLS, external SMTP, alert receivers and clinical workflow acceptance.
