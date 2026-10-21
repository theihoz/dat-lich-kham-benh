# Đặt lịch khám bệnh — Đồ án Design Patterns và SOLID

Ứng dụng phòng khám dành cho **Bệnh nhân, Bác sĩ, Quản trị viên**. Trọng tâm đồ án là hiện thực mẫu thiết kế trong nghiệp vụ có lưu dữ liệu, kèm phân tích, sơ đồ draw.io và kiểm thử.

## Bắt đầu từ đây

- [Báo cáo đề tài, yêu cầu và Use Case](docs/BAO_CAO.md).
- [Lý thuyết, thiết kế chi tiết 5 pattern và SOLID](docs/DESIGN_PATTERNS.md).
- [Đối chiếu từng yêu cầu của đề bài](docs/DOI_CHIEU_YEU_CAU.md).
- [Kịch bản demo và test case](docs/KICH_BAN_DEMO.md).
- [Kết quả kiểm thử và hiệu năng đo thực tế](docs/KET_QUA_KIEM_THU.md).
- [Bộ sơ đồ draw.io có thể chỉnh sửa](docs/diagrams/HeThongDatLich.drawio).

Mở file `.drawio` bằng **draw.io Desktop** hoặc **diagrams.net → File → Open from → Device**. Các trang bao gồm kiến trúc, Use Case, DFD, ERD, lớp miền, trạng thái lịch hẹn và Class/Sequence cho từng pattern.

## Chạy demo trên lớp (không cần Docker)

Yêu cầu JDK 21 và Maven 3.9+. Từ thư mục gốc:

```bash
sh run-demo.sh
```

Hoặc:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Windows: chạy lệnh Maven trên trong PowerShell/CMD. `scripts/maven.sh` cũng tự tìm Maven đi kèm Oracle Java extension của VS Code trên macOS nếu máy chưa có lệnh `mvn`; có thể chỉ định đường dẫn qua biến `MVN`.

Mở [ứng dụng](http://localhost:8080), [trang Design Patterns và hộp thư demo](http://localhost:8080/patterns.html), [health check](http://localhost:8080/actuator/health).

| Vai trò | Email demo | Mật khẩu |
|---|---|---|
| Bệnh nhân | `bn.hoangminh@gmail.com` | `Patient@123` |
| Bác sĩ có bệnh án mẫu | `bs.tranthib@clinic.vn` | `Doctor@123` |
| Bác sĩ | `doctor@clinic` | `doctor@2026` |
| Admin | `admin@clinic.vn` | `Admin@123` |

Profile `demo` dùng H2 **trong RAM**, khởi tạo dữ liệu mẫu vào các bảng thật và đặt lại dữ liệu khi khởi động lại. Thông báo được xử lý sau commit vào hộp thư cục bộ, chỉ Admin được đọc; không gửi SMTP. Redis được thay bằng cache no-op, RabbitMQ listener tắt. Giao diện hiện vẫn tải Bootstrap/Chart.js từ CDN nên cần mạng để hiển thị đầy đủ.

## Các pattern được áp dụng

| Nhóm | Pattern | Vị trí trong luồng nghiệp vụ |
|---|---|---|
| Creational | Builder | `BookingServiceImpl` dùng `Appointment.AppointmentBuilder` tạo lịch hẹn |
| Creational | Factory Method | Ba `NotificationCreator` tạo ba Product xác nhận/hủy/nhắc lịch |
| Structural | Adapter | `NotificationSender` → `SmtpNotificationAdapter` → JavaMail; demo dùng hộp thư cục bộ |
| Behavioral | Strategy | `DoctorSortStrategy` có bốn thuật toán; màn hình tìm bác sĩ gọi API theo lựa chọn |
| Behavioral | Observer | Sự kiện thông báo → listener bất đồng bộ `AFTER_COMMIT`; rollback không gửi |

Đây là ứng dụng **phân tầng**, với ba tầng logic giao diện, nghiệp vụ, lưu trữ; security và tích hợp là các khối hỗ trợ. Xem [architecture.md](architecture.md) và [schema.md](schema.md).

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

Docker Desktop macOS: nếu chạy backend trong container, publish cổng `-p 8080:8080` và trỏ DB/Redis/RabbitMQ về `host.docker.internal` với các cổng host phía trên.

## Kiểm thử và đóng gói

```bash
cd backend
mvn test
mvn package
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Từ thư mục gốc trên macOS/Linux có thể thay `mvn` bằng `sh scripts/maven.sh -f backend/pom.xml`.

```bash
python3 scripts/generate_diagrams.py
python3 scripts/validate_diagrams.py
python3 scripts/sync_frontend.py --check
python3 scripts/benchmark.py --requests 60 --workers 4 --output docs/evidence/benchmark.json
```

Benchmark chỉ gọi localhost, hỏi mật khẩu demo nếu chưa cấp `CLINIC_BENCH_PASSWORD`; không lưu token/mật khẩu trong báo cáo. Script đo đọc danh sách bác sĩ, không tạo tải đặt lịch trên dữ liệu thật.

Kiểm thử browser tái lập với Playwright đã cài:

```bash
node scripts/browser_qa.cjs
```

Script này thao tác trên server demo localhost: đăng nhập, đặt/hủy một lịch, xem bệnh án, thống kê và hộp thư. Dùng dữ liệu demo, không chạy cùng cổng của môi trường thật.

## Cấu trúc mã nguồn

```text
backend/src/main/java/com/datlichkhambenh/backend/
  controller/       REST endpoints
  service/          Interface nghiệp vụ
  service/impl/     Luồng giao dịch và kiểm tra nghiệp vụ
  entity/           JPA entities, Builder
  repository/       JPA, truy vấn có khóa
  pattern/
    notification/   Factory Method, Product, Adapter
    sorting/        Strategy và registry
  messaging/        Observer và cầu nối RabbitMQ
  demo/             Observer và inbox cục bộ cho profile demo
  security/         JWT, BCrypt, phân quyền
backend/src/main/resources/static/   Giao diện được phục vụ, bản chính
frontend/                           Bản phân phối đồng bộ
docs/                               Báo cáo, sơ đồ và bằng chứng
scripts/                            Sinh/kiểm tra sơ đồ, benchmark, browser QA
```

Sau khi chỉnh giao diện trong `static/`, chạy `python3 scripts/sync_frontend.py` để cập nhật `frontend/`; `--check` phát hiện khác biệt. Build output `backend/target/` không phải mã nguồn.

Repository: [theihoz/dat-lich-kham-benh](https://github.com/theihoz/dat-lich-kham-benh).
