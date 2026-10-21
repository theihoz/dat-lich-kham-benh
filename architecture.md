# Kiến trúc hệ thống đặt lịch khám bệnh

## 1. Phạm vi

Đồ án mô phỏng một phòng khám địa phương, sử dụng **kiến trúc phân lớp với ba tầng logic**, backend nguyên khối Spring Boot. Đây là bối cảnh học thuật giả định, không phải kết quả khảo sát thực địa hay hệ thống đã triển khai tại một cơ sở y tế.

Xem [báo cáo](docs/BAO_CAO.md), [mẫu thiết kế](docs/DESIGN_PATTERNS.md), [use case](docs/USE_CASES.md), [dữ liệu](schema.md), [kiểm thử](docs/KIEM_THU_VA_HIEU_NANG.md). Sơ đồ chỉnh sửa được: [HeThongDatLich.drawio](docs/diagrams/HeThongDatLich.drawio), trang `Architecture`.

## 2. Ba tầng logic và các mối quan tâm xuyên tầng

| Tầng | Thành phần | Trách nhiệm |
|---|---|---|
| Trình bày | HTML/CSS/JavaScript, `controller/*`, DTO | Hiển thị, nhận HTTP, `@Valid`, lấy danh tính đã xác thực, trả DTO. Không đặt quy tắc sở hữu dữ liệu ở JavaScript. |
| Nghiệp vụ | `service/*`, `service/impl/*`, các cộng tác viên pattern | Quyền sở hữu, quy tắc lịch hẹn/ca/bệnh án, giao dịch, lựa chọn thuật toán, phát yêu cầu thông báo. |
| Truy cập dữ liệu | `repository/*`, entity JPA, MySQL/H2, ranh giới `RedisCacheService` | Đọc/ghi và khóa bản ghi; DB quan hệ là nguồn dữ liệu chuẩn; cache chỉ giữ bản sao. |

Security và tích hợp là mối quan tâm xuyên tầng. `security/*` và `SecurityConfig` xử lý JWT/phân quyền; `messaging/*`, observer demo và sender adapter xử lý tích hợp. Redis, RabbitMQ, SMTP là công nghệ hạ tầng, không phải các tầng nghiệp vụ thứ tư, thứ năm. Ba tầng logic không bắt buộc ba máy chủ và không biến ứng dụng thành microservices.

Luồng đồng bộ: trình duyệt → JWT filter → controller → service/giao dịch → repository → DB. Frontend static được Spring Boot phục vụ; khi checkout có nhiều bản frontend, phải kiểm tra bản tài nguyên của build đang chạy. Tài liệu không yêu cầu chỉnh sửa hay hợp nhất frontend.

## 3. Ranh giới trách nhiệm

Đường dẫn Java dưới đây tính từ `backend/src/main/java/com/datlichkhambenh/backend/`.

| Thành phần | Trách nhiệm chính |
|---|---|
| `AuthServiceImpl` | BCrypt, JWT, đăng ký theo vai trò cho phép; tài khoản bác sĩ chờ duyệt không đăng nhập được. |
| `DoctorServiceImpl` | Tra cứu bác sĩ đã duyệt/đang hoạt động, cache, lọc, chọn `DoctorSortRegistry`, sắp xếp và phân trang. |
| `DoctorScheduleServiceImpl` | Kiểm tra bác sĩ sở hữu, ngày tương lai, khoảng giờ hợp lệ, chồng lấn; không sửa/xóa ca đã có appointment. |
| `BookingServiceImpl` | Khóa slot khi đặt, dùng AppointmentBuilder, giữ giá lúc đặt; khóa appointment khi hủy/hoàn thành. |
| `MedicalRecordServiceImpl` | Chỉ bác sĩ phụ trách ghi, không trước giờ khám/không cho lịch hủy; lưu bệnh án và hoàn thành lịch trong giao dịch. |
| `AdminManagementServiceImpl` | Duyệt bác sĩ, tài khoản, danh mục, thống kê từ bản ghi thực. |
| `EmailQueueConsumer` | Chọn creator qua registry, tạo nội dung, gọi `NotificationSender`; dùng chung cho cả hai chế độ. |

## 4. Hai chế độ chạy rõ ràng

| Đặc điểm | `demo` — trình diễn không Docker | Tích hợp hạ tầng — cấu hình mặc định, có thể dùng `dev` để seed |
|---|---|---|
| DB | H2 trong RAM, chế độ tương thích MySQL; `create-drop` | MySQL, mặc định Hibernate `update` |
| Dữ liệu khởi tạo | `DataInitializer` hoạt động với profile `demo` | Initializer chỉ hoạt động khi có `dev` (hoặc `demo`), không mặc định seed mọi môi trường |
| Cache | `NoOpRedisCacheService`; không đo được lợi ích Redis | Redis cache-aside với TTL |
| Tiếp nhận sự kiện | `DemoNotificationObserver`, `@Async`, AFTER_COMMIT | `EmailQueuePublisher`, `@Async`, AFTER_COMMIT |
| Chuyển tiếp | Gọi cùng `EmailQueueConsumer` trong tiến trình | RabbitMQ → `EmailQueueConsumer` |
| Sender | `DemoNotificationAdapter`, inbox trong RAM tối đa 100 mục | `SmtpNotificationAdapter` bọc `JavaMailSender` |
| Kết quả thư | Thư giả lập có nội dung, **không gửi SMTP** | Cần cấu hình SMTP và xác nhận nhận thư thật |
| Bằng chứng | Admin GET `/api/v1/admin/demo-notifications`, chỉ profile demo | Log/broker/hộp thư nhận thực tế |

`EmailQueuePublisher` và `SmtpNotificationAdapter` dùng `@Profile("!demo")`; các thành phần demo chỉ thuộc profile demo. Không mô tả đường demo là đã đi qua RabbitMQ. Inbox không bền vững, chỉ giữ tối đa 100 thông báo và mất khi dừng ứng dụng; H2 cũng mất dữ liệu khi dừng. H2 có dependency runtime nhưng profile quyết định datasource, không thay schema nghiệp vụ.

Khởi động từ `backend`: `mvn spring-boot:run -Dspring-boot.run.profiles=demo`, cần Java 21 và Maven khả dụng. Xem tài khoản seed và kịch bản đầy đủ tại [demo](docs/KICH_BAN_DEMO.md).

## 5. Security

JWT filter kiểm tra chữ ký, hạn token; Spring Security kiểm tra authority `PATIENT`, `DOCTOR`, `ADMIN`; service kiểm tra quyền sở hữu bản ghi. Header/payload JWT ký chỉ được mã hóa biểu diễn Base64URL, **không phải mã hóa bí mật**. BCrypt là băm mật khẩu.

`SecurityConfig` cho qua ở lớp URL: `/api/v1/auth/**`, GET `/api/v1/doctors/specialties`, `/actuator/**` và tài nguyên tĩnh; các API `/api/v1/**` còn lại cần xác thực. Matcher auth không tự chứng minh change-password được dùng vô danh: phải kiểm tra danh tính tại controller/service. API theo vai trò dùng `@PreAuthorize`. Test 401, 403 và lỗi không sở hữu riêng; một số từ chối nghiệp vụ dùng `AppException`, không mặc định đều là HTTP 403.

Cấu hình học tập chưa chứng minh bảo mật triển khai thực. Các việc còn cần làm gồm HTTPS, bí mật ngoài mã nguồn, thu hẹp Actuator, chính sách token, audit dữ liệu nhạy cảm, sao lưu/phục hồi. Không tuyên bố đạt chứng nhận hay tuân thủ y tế.

## 6. Giao dịch đặt, hủy và hoàn thành

Đặt lịch: controller lấy user hiện hành → `createBooking` mở giao dịch → `ScheduleRepository.findByIdForUpdate` với `PESSIMISTIC_WRITE` → kiểm tra khả dụng, chưa gắn appointment, giờ tương lai, bác sĩ hợp lệ → `Appointment.builder()` tạo CONFIRMED, bản chụp phí bác sĩ → lưu → khóa khả dụng của slot → evict cache → phát sự kiện. Unique `appointments.schedule_id` là hàng rào DB cuối; cache không làm khóa. H2 không thay thế bằng chứng tranh chấp trên MySQL/InnoDB.

Hủy: khóa appointment → kiểm tra bệnh nhân sở hữu, chưa CANCELLED/COMPLETED, giờ khám **lớn hơn** `now + 24 giờ` → đổi CANCELLED → giữ slot cũ không khả dụng → tạo dòng Schedule thay thế cùng bác sĩ/ngày/giờ, ID mới, khả dụng. Điều kiện `isAfter(now.plusHours(24))` từ chối đúng mốc 24 giờ, dù thông báo dùng “ít nhất 24 giờ”.

Unique schedule→appointment áp dụng **mọi trạng thái**, kể cả lịch đã hủy; không bật lại slot cũ để tái sử dụng, không xóa lịch sử. Không có unique tổ hợp bác sĩ/ngày/giờ vì lịch sử và dòng thay thế có thể trùng khung giờ. Hoàn thành chỉ cho bác sĩ phụ trách, lịch CONFIRMED và đã tới giờ; ghi bệnh án cũng chuyển lịch sang COMPLETED. API hoàn thành riêng không bắt buộc tạo bệnh án.

## 7. Chuỗi thông báo và giới hạn

`AsyncEmailNotificationServiceImpl` phát `EmailNotificationRequestedEvent` qua `ApplicationEventPublisher`; listener AFTER_COMMIT là Observer trong tiến trình. Ở chế độ tích hợp, listener chuyển message sang RabbitMQ, rồi consumer chọn `NotificationCreatorRegistry.get(type)` → `NotificationCreator.createMessage(message)` → `NotificationContent(subject, body)` → `NotificationSender.send(recipient, content)`. Creator cụ thể: Confirmation/Cancellation/Reminder. Demo đi từ observer đến cùng consumer, bỏ broker và thay sender bằng inbox.

**Chưa có transactional outbox.** DB commit và publish RabbitMQ không nguyên tử: tiến trình dừng hoặc publish thất bại sau commit có thể làm mất thư. Queue bền không xử lý được cửa sổ trước publish. Không tuyên bố exactly-once, retry đầy đủ, chống gửi lặp hay có dead-letter queue; `default-requeue-rejected=false` không chứng minh những cơ chế đó. Demo cũng có thể mất sự kiện do lỗi tiến trình/executor và không chứng minh độ tin cậy broker. Có API nội bộ/creator reminder không có nghĩa đã có scheduler tự động.

## 8. Cache, dữ liệu và đánh giá

Redis dùng cache-aside: hit trả DTO, miss đọc DB rồi cache; TTL mặc định bác sĩ 300 giây, slot 60 giây. Evict được gọi khi dữ liệu thay đổi nhưng không phải giao dịch nguyên tử cache/DB, còn nguy cơ race/dữ liệu cũ. Demo NoOp luôn đi đường không cache.

Tra cứu đang lọc/sắp xếp DTO trong bộ nhớ rồi cắt trang; phân trang API chưa phải phân trang SQL. Với N bản ghi, chi phí sort thường O(N log N), bộ nhớ O(N). Pattern không tự làm endpoint nhanh hơn. [Kế hoạch đo](docs/KIEM_THU_VA_HIEU_NANG.md) phân biệt rõ demo H2/NoOp với MySQL/Redis.

Compose công bố MySQL `3307→3306`, Redis `6379`, RabbitMQ `5672`, quản trị `15672`; backend mặc định `8080`. `schema.sql` chỉ `SELECT 1`, SQL init `never`; tài liệu không yêu cầu xóa DB hay volume. Các bằng chứng chạy, test và benchmark phải gắn bản mã/môi trường/thời điểm; tài liệu đọc mã không xác nhận runtime hiện đang khỏe.
