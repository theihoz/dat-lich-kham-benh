# Báo cáo kiểm tra và sửa lỗi — 04/11/2026

## Phạm vi đối chiếu

Đọc `README.md`, `schema.md`, `docs/KICH_BAN_DEMO.md` và sơ đồ kiến trúc. Repo không có `architecture.md` được nhắc trong skill backend. Ba subagent kiểm tra booking/ca khám/bệnh án, xác thực/phân quyền/tìm bác sĩ, và frontend/API; agent điều phối chạy kiểm thử tổng hợp và sửa cấu hình index. Có review chéo thứ tự khóa và các thay đổi API client.

## Lỗi đã sửa

| Phần | Nguyên nhân và kết quả sau sửa |
|---|---|
| Đổi mật khẩu | Endpoint công khai và JWT filter bỏ qua auth, khiến yêu cầu trả 500. Nay yêu cầu access token; token thiếu hoặc refresh token trả 401. |
| Actuator | Filter bỏ qua JWT của admin. Nay xác thực được endpoint quản trị. |
| Tìm bác sĩ | Bộ lọc dereference phí/kinh nghiệm nullable gây 500. Nay loại dữ liệu thiếu khỏi bộ lọc tương ứng. |
| Ca trống | Ca đã qua giờ vẫn trả từ DB/cache. Nay lọc giờ bắt đầu cả hai nguồn. |
| Sửa ca đồng thời | Thiếu khóa chung với tạo ca nên có thể tạo overlap. Nay giữ khóa schedule rồi doctor, dùng READ_COMMITTED để kiểm tra dữ liệu vừa commit; thứ tự khóa đã review để tránh đảo khóa với booking/FK. |
| Quyền sở hữu | Các thao tác ghi trái chủ sở hữu trả 400. Nay trả 403 cho lịch hẹn, ca khám, bệnh án. |
| Index | Cấu hình dùng cột patient_user_id/doctor_user_id không tồn tại và cú pháp IF NOT EXISTS không hỗ trợ trên MySQL. Nay dùng patient_id/doctor_id, kiểm tra metadata trước CREATE INDEX. |
| Tải JavaScript | Inline gọi Auth/Api/Utils trước script defer; trang dịch vụ cũng khởi tạo Bootstrap sớm. Nay tải dependency đúng thứ tự. |
| Cache frontend | GET giữ dữ liệu cũ sau mutation và dùng chung giữa tài khoản. Nay xóa cache sau ghi thành công, khóa cache gồm token. |
| Phiên đăng nhập | Sai mật khẩu bị logout/refresh, refresh có thể lặp và bỏ token xoay vòng. Nay hiển thị lỗi login, retry tối đa một lần, lưu cả hai token. |
| API origin | Backend chạy cổng khác vẫn bị frontend gọi localhost:8080. Nay dùng cùng origin, có CLINIC_API_BASE override. |
| Dịch vụ admin | Input tên trùng window.name làm mất tên khi submit. Nay lấy input rõ ràng. |
| Nội dung văn bản | Dấu nháy làm vỡ inline handler và dữ liệu có thể được diễn giải như HTML. Đã dùng ID lookup và escape ở booking, bệnh án, danh sách/chi tiết lịch hẹn bệnh nhân; test xác nhận màn hình bác sĩ đã escape dữ liệu bệnh nhân. |
| Đồng bộ frontend | Hai bản frontend lệch 21 file. Đã đồng bộ từ static và thêm lệnh regression frontend vào CI. |

Các lỗi chức năng mới có test tái hiện trước sửa; không commit hoặc triển khai.

## Kết quả kiểm chứng

- `sh scripts/maven.sh -f backend/pom.xml clean verify`: **50 tests, 0 failures, 0 errors**, đóng gói JAR thành công trên JDK 21.
- `node scripts/frontend_regression.cjs`: **13/13 pass**, không cần dependency ngoài Node.
- `python3 scripts/sync_frontend.py --check`: **0 khác biệt**.
- `python3 scripts/validate_diagrams.py`: **15 sơ đồ, 0 lỗi**.
- `git diff --check`: sạch.
- Chrome/Playwright trên JAR mới, profile demo riêng ở cổng 18080: **8 bước pass**, không pageerror hoặc HTTP lỗi trong các luồng kiểm tra. Bao gồm login ba vai trò, sắp xếp bác sĩ, đặt/hủy lịch, đọc/sửa bệnh án và đọc lại sau reload, thống kê admin, thông báo sau commit. Kiểm tra responsive trang patterns tại 375/768/1440px.

Bằng chứng browser có tại `docs/evidence/debug-browser-2026-11-04.json`; ảnh của lần chạy được giữ tại `/tmp/clinic-qa-evidence`. Bộ kiểm thử không thao tác server có sẵn ở cổng 8080.

## Giới hạn và việc còn cần xác minh

- Kiểm thử tích hợp và concurrency dùng H2; chưa chạy lại trên MySQL thật hoặc kiểm chứng Redis/RabbitMQ/SMTP thật. Không suy diễn kết quả demo thành độ tin cậy production.
- Chưa audit toàn bộ nội suy HTML ở các màn hình admin khác; kết quả escape chỉ bao phủ các luồng được nêu và test.
- Các giới hạn đã có trong spec vẫn còn: chưa có scheduler nhắc lịch, chưa có transactional outbox và chưa tích hợp thanh toán.
- Một số build output trong `backend/target` đã được Git theo dõi từ trước. Sau kiểm thử, phần generated được đưa về nội dung ban đầu để bản sửa chỉ gồm nguồn; chạy Maven lại khi cần dùng trực tiếp target/classes.
