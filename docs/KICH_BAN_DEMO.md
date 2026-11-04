# KỊCH BẢN DEMO — Hệ thống đặt lịch khám bệnh

## 1. Yêu cầu môi trường

- **Java 21** (OpenJDK hoặc Eclipse Adoptium)
- **Maven 3.8+**
- Không cần Docker, MySQL, Redis hay RabbitMQ (chế độ demo dùng H2 trong RAM)

## 2. Khởi động ứng dụng

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Ứng dụng chạy tại: **http://localhost:8080**

## 3. Tài khoản seed

| Vai trò | Email | Mật khẩu | Ghi chú |
|---------|-------|-----------|---------|
| Quản trị viên | `admin@clinic` | `admin@2026` | Quyền quản lý toàn hệ thống |
| Bác sĩ | `doctor@clinic` | `doctor@2026` | BS. Hoàng Văn Cường, Thần kinh |
| Bệnh nhân | `patient@clinic` | `user@2026` | Trần Thị Bình |

Ngoài ra có 8 bác sĩ mặc định (mật khẩu `Doctor@123`), 5 bệnh nhân mặc định (mật khẩu `Patient@123`), và 50 bác sĩ + 40 bệnh nhân sinh tự động.

## 4. Kịch bản trình diễn

### Kịch bản 1: Đặt lịch khám (Builder + Facade + Observer)

1. Mở trình duyệt → `http://localhost:8080`
2. Đăng nhập bằng tài khoản bệnh nhân: `patient@clinic` / `user@2026`
3. Vào **Tìm bác sĩ** → Chọn bác sĩ có ca trống
4. Chọn ca khám → Nhập triệu chứng → **Đặt lịch**
5. **Kết quả**: Lịch hẹn tạo thành công, status = CONFIRMED
6. **Pattern chứng minh**:
   - **Builder**: `Appointment.builder()...build()` tạo đối tượng lịch hẹn
   - **Facade**: `BookingController` gọi `AppointmentBookingFacade.bookAppointment()`
   - **Observer**: `EmailNotificationRequestedEvent` phát sau khi giao dịch commit

### Kịch bản 2: Hủy lịch hẹn (Observer + Adapter)

1. Từ trang **Lịch hẹn của tôi** → Chọn lịch hẹn đã đặt
2. Nhấn **Hủy lịch**
3. **Kết quả**: Lịch chuyển sang CANCELLED, slot thay thế được tạo
4. **Pattern chứng minh**:
   - **Observer**: Sự kiện BOOKING_CANCELLED được phát
   - **Adapter**: `DemoNotificationAdapter` lưu thông báo vào inbox RAM

### Kịch bản 3: Xem thông báo demo (Factory Method + Adapter)

1. Đăng nhập bằng admin: `admin@clinic` / `admin@2026`
2. Truy cập API: `GET http://localhost:8080/api/v1/admin/demo-notifications`
3. **Kết quả**: Danh sách thông báo đã gửi (xác nhận, hủy) với nội dung đầy đủ
4. **Pattern chứng minh**:
   - **Factory Method**: `NotificationCreatorRegistry` chọn đúng Creator theo type
   - **Adapter**: `DemoNotificationAdapter` lưu thay vì gửi SMTP

### Kịch bản 4: Sắp xếp bác sĩ (Strategy)

1. Đăng nhập bệnh nhân → **Tìm bác sĩ**
2. Thay đổi cách sắp xếp: Theo tên / Phí tăng dần / Phí giảm dần / Kinh nghiệm
3. **Kết quả**: Danh sách bác sĩ thay đổi thứ tự tương ứng
4. **Pattern chứng minh**: `DoctorSortRegistry.get(sort).comparator()` chọn Strategy phù hợp

### Kịch bản 5: Quản lý ca khám (Bác sĩ)

1. Đăng nhập bác sĩ: `doctor@clinic` / `doctor@2026`
2. Vào **Quản lý ca khám** → Tạo ca mới (chọn ngày tương lai, giờ bắt đầu/kết thúc)
3. **Kết quả**: Ca khám mới xuất hiện, bệnh nhân có thể đặt lịch vào ca này

### Kịch bản 6: Ghi bệnh án (Bác sĩ)

1. Đăng nhập bác sĩ → **Lịch khám của tôi**
2. Chọn lịch hẹn đã qua giờ khám (status CONFIRMED)
3. Nhập chẩn đoán và ghi chú → **Lưu bệnh án**
4. **Kết quả**: Bệnh án được tạo, lịch chuyển sang COMPLETED

## 5. Chạy kiểm thử

```bash
cd backend
JAVA_HOME=/path/to/java-21 mvn test
```

Kết quả mong đợi: `Tests run: 50, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

## 6. Lưu ý

- Dữ liệu H2 **mất khi dừng ứng dụng** (create-drop). Mỗi lần khởi động lại, DataInitializer tạo dữ liệu mới.
- Inbox thông báo demo tối đa 100 mục, không bền vững.
- Trang `patterns.html` (http://localhost:8080/patterns.html) hiển thị tổng quan 6 design patterns đã áp dụng.
