# Kết quả kiểm thử thực tế

Ngày thực hiện: **21/10/2026**, múi giờ Việt Nam. Kiểm tra trên bản mã đã áp dụng pattern, không dùng kết quả cũ của repository.

## 1. Build và tự động hóa

`mvn -B package` trên JDK 21.0.12.1 / Maven 3.9.16, macOS arm64: **BUILD SUCCESS**, **31 tests, 0 failures, 0 errors, 0 skipped**. JAR Spring Boot có thể chạy độc lập. Sau đó chỉnh ngày của ca COMPLETED mẫu về quá khứ và đóng gói lại; dữ liệu seed này được kiểm tra qua giao diện.

| Bộ test | Số test | Nội dung |
|---|---:|---|
| BackendApplicationTests | 1 | Spring context test profile |
| AuthSecurityIntegrationTests | 4 | Đăng ký không tạo Admin, API quản trị đòi xác thực, danh mục công khai, favicon |
| BookingFlowIntegrationTests | 5 | Commit phát thông báo, rollback không phát, đặt đồng thời một người thành công, hủy/tái đặt, ghi/sửa/đọc bệnh án |
| DesignPatternTests | 6 | Builder, ba Concrete Product của Factory Method, dịch nội dung qua SMTP Adapter, phối hợp consumer, bốn Strategy, bổ sung Strategy |
| AuthServiceImplTests | 2 | Bác sĩ chưa duyệt, phân biệt access/refresh token |
| OwnershipServiceTests | 3 | Không sửa ca và không đọc/ghi bệnh án bác sĩ khác |
| BookingServiceImplTests | 3 | Lịch sử rỗng, quyền xem lịch, giữ lịch sử hủy và tạo slot thay thế |
| TruthfulDataTests | 7 | Hồ sơ rỗng và thống kê theo dữ liệu lưu, không cộng số giả |

Các bài test tích hợp dùng H2 và mock RabbitTemplate: chứng minh giao dịch, khóa trong môi trường H2 và đúng lời gọi cầu nối; **không thay thế test MySQL/InnoDB và broker RabbitMQ thực**. SMTP Adapter kiểm tra bằng JavaMailSender mock, không tuyên bố đã gửi email qua mạng.

## 2. Kiểm thử trình duyệt

Chạy `scripts/browser_qa.cjs` bằng Playwright/Chrome headless, server JAR profile `demo` tại `localhost:8080`. Dữ liệu sử dụng là dữ liệu mẫu của DataInitializer, được lưu thật trong H2 của phiên demo.

Các luồng đã qua:

1. Đăng nhập bệnh nhân và tải bác sĩ đã lưu.
2. Chọn phí tăng dần; kiểm tra request `sort=feeAsc` và thứ tự phí do server trả về.
3. Chọn ca sau hai ngày, nhập triệu chứng, xác nhận đặt lịch qua form; nhận ID và booking code đã lưu.
4. Hủy lịch qua API cùng phiên đăng nhập; đọc lại trạng thái CANCELLED.
5. Đăng nhập bác sĩ Trần Thị Bích, tải nội dung bệnh án đã lưu.
6. Sửa bệnh án bằng form, nhận HTTP 200, tải lại trang và đọc lại nội dung cập nhật.
7. Đăng nhập Admin, xem tổng phí COMPLETED và nhãn chưa xác nhận thanh toán.
8. Tải hộp thư demo: nhận cả thông báo xác nhận và hủy từ các thao tác đã thực hiện.

Kết quả: **0 lỗi JavaScript `pageerror`, 0 response HTTP ≥400 từ localhost** trong lượt thành công. Trang patterns không tràn ngang ở **375, 768, 1440 px**. Chưa có ảnh baseline nên không kết luận visual regression; chưa thực hiện đầy đủ audit accessibility hay Core Web Vitals.

Bằng chứng tái lập: [browser-qa.json](evidence/browser-qa.json), [đặt lịch](evidence/patient-booking.png), [đọc bệnh án](evidence/doctor-record.png), [thống kê](evidence/admin-analytics.png), [mobile](evidence/patterns-375.png), [desktop](evidence/patterns-1440.png).

Health check sau khi chạy JAR: HTTP 200, `{"status":"UP"}`. Profile demo không dùng RabbitMQ/SMTP; hộp thư cục bộ thể hiện nội dung của Factory Method và đường đi Observer/Adapter.

## 3. Đánh giá hiệu năng

Đo bằng `scripts/benchmark.py`, **60 GET, 4 worker, 5 lần warm-up**, endpoint `/api/v1/doctors?sort=feeAsc&size=100`. Mỗi response có 57 bác sĩ. Máy macOS arm64, Java 21, server JAR profile demo, H2 trong RAM, cache no-op; chưa đo MySQL/Redis hay gửi email thật. Phép đo diễn ra lúc 18:28 ngày 21/10/2026.

| Chỉ số | Kết quả |
|---|---:|
| Trung bình | 11,40 ms |
| p95 | 20,02 ms |
| Lớn nhất | 27,71 ms |
| Thông lượng trong mẫu | 342,08 request/giây |

Nguồn số liệu: [benchmark.json](evidence/benchmark.json). Không suy rộng mẫu nhỏ này thành công suất triển khai thực; không kết luận Design Pattern tự làm hệ thống nhanh hơn. Tra cứu hiện tải danh sách DTO, lọc và sort trong bộ nhớ O(N log N); khi tăng dữ liệu nên chuyển lọc/phân trang xuống truy vấn SQL và đo cache hit/miss trên cấu hình MySQL/Redis riêng.

## 4. Khả năng bảo trì và giới hạn

- Thêm một cách sắp xếp bằng class implement DoctorSortStrategy; registry tự nhận bean. Test bổ sung Strategy chứng minh không cần đổi registry.
- Thêm loại thông báo bằng ConcreteCreator và ConcreteProduct; consumer vẫn gọi giao diện chung.
- Đổi nhà cung cấp gửi thư bằng NotificationSender adapter; kiểm thử nghiệp vụ không cần gọi SMTP.
- Builder làm rõ các trường tạo lịch; vẫn cần service kiểm tra quy tắc nghiệp vụ, builder không tự đảm bảo toàn bộ invariant.
- Observer tách phát yêu cầu khỏi xử lý thông báo, nhưng chưa có transactional outbox hoặc retry/DLQ bảo đảm giao nhận.
- Class nhiều hơn cách viết switch trực tiếp; các interface nhỏ chỉ được đặt tại điểm có biến thể thực tế.

Các kiểm tra đã thực hiện không chứng minh triển khai production, chịu tải lớn, gửi thư đến hộp thư thật hoặc độ bền dữ liệu H2 sau khi dừng ứng dụng. Chính sách hủy hiện yêu cầu **hơn 24 giờ**; đúng mốc 24 giờ bị từ chối.
