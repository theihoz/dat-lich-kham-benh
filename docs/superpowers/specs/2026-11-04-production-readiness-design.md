# Đặc tả hoàn thiện ứng dụng phòng khám cho production

## Mục tiêu và phạm vi

Thực hiện toàn bộ danh sách cải thiện đã trao đổi, theo thứ tự nghiệm thu: giao diện → xác thực/bảo mật → dữ liệu/thông báo → triển khai/vận hành → kiểm thử tổng hợp. Dùng Spring Boot 4.1.2 sau audit dependency, giữ Java 21, MySQL 8, Redis 7, RabbitMQ 3 và frontend HTML/CSS/JS. Giữ dữ liệu nghiệp vụ, ownership, khóa slot và sáu design patterns. Đây là triển khai mã/cấu hình có thể review; không tự công bố có thể phục vụ bệnh nhân thật khi chưa nghiệm thu hạ tầng thật.

Yêu cầu trực tiếp của người dùng: tạo spec, plan rồi thực hiện lần lượt toàn bộ. Quyết định kỹ thuật dưới đây do agent đề xuất dựa trên repo, được ghi rõ thay vì giả định người dùng đã chọn từng chi tiết. Không cần dừng lại để hỏi lại quyền thực hiện trong phạm vi đã giao.

## Các hướng đã cân nhắc

1. Nâng cấp tại chỗ, frontend dùng chung module nhỏ và backend bổ sung từng chức năng: chọn vì giữ được luồng đang có và kiểm thử hồi quy.
2. Viết lại frontend SPA: không chọn vì tăng phạm vi migration nhưng không giải quyết bảo mật/vận hành.
3. Thay backend/hệ sinh thái cloud: không chọn vì chưa có yêu cầu nhà cung cấp và làm mất lợi thế repo hiện tại.

## S1 — Giao diện và khả năng sử dụng

- Cùng typography, navigation, layout, spacing và trạng thái form cho bệnh nhân/bác sĩ/admin. Chuẩn hóa trang danh sách bác sĩ đang dùng class không được định nghĩa.
- Mobile 375px/768px và desktop 1440px không tràn ngang toàn trang. Bảng quản trị có vùng cuộn riêng, màn bệnh nhân dùng thẻ dễ đọc; menu mobile có nhãn rõ.
- Đặt lịch thể hiện bốn bước bác sĩ → ca khám → kiểm tra → xác nhận, giữ triệu chứng khi xung đột slot, tải lại lựa chọn và giải thích lỗi.
- Trước xác nhận hiển thị tên bác sĩ, ngày/giờ, phí, nơi khám và điều kiện hủy >24h. Địa điểm là cấu hình phòng khám thực, không tự tạo địa chỉ giả; khi chưa cấu hình thông báo chưa có địa chỉ.
- Màn bác sĩ ưu tiên lịch hôm nay, tìm kiếm/lọc trạng thái, đọc/chỉnh bệnh án rộng, cảnh báo thay đổi chưa lưu.
- Tải dữ liệu có loading, empty, error/retry; khóa nút khi submit; lỗi form có nhãn trường và thông báo dễ đọc.
- Hỗ trợ keyboard, focus modal, labels, aria-live, reduced-motion, nút tương tác dễ chạm, tương phản phù hợp. Test accessibility tự động và keyboard trọng điểm; không tự tuyên bố chứng nhận WCAG.
- Escape toàn bộ dữ liệu người dùng trong HTML và attribute; không đưa chuỗi không tin cậy vào onclick. Chuyển inline script/handler ra module hoặc CSP nonce/hash phù hợp, CSP không dùng unsafe-inline cho script.
- Asset có version/hash để không giữ JS cũ 7 ngày sau triển khai; tránh phụ thuộc CDN không kiểm soát (vendor hoặc SRI/CSP allowlist rõ ràng).

## S2 — Xác thực và bảo mật

- Refresh token không lưu localStorage. Lưu cookie HttpOnly, SameSite=Strict, Secure ở production, path auth; access token giữ trong bộ nhớ phía client. Cookie được dùng refresh/logout nên phải kiểm tra Origin/CSRF tương ứng; không làm cookie authentication cho API bearer ngoài phạm vi này.
- Refresh session lưu hash token, user, expiresAt, revokedAt, token family. Mỗi lần refresh chỉ dùng token một lần; replay thu hồi family; logout thu hồi session; đổi/reset mật khẩu thu hồi tất cả session, access token cũ bị vô hiệu theo phiên bản thông tin xác thực hoặc thời điểm.
- Giữ API đăng nhập tương thích với Postman/bearer khi cần, nhưng browser không nhận refresh token qua storage; test cho cả đường browser và API đã công bố.
- Quên mật khẩu luôn trả phản hồi chung chống dò email; token ngẫu nhiên, hash trong DB, hết hạn 30 phút, dùng một lần. Xác minh email có token hết hạn 24h; production yêu cầu email đã xác minh, demo/test vẫn dùng tài khoản seed được xác minh. Link reset/verify chỉ dùng public URL cấu hình.
- Giới hạn login/register/reset/refresh; phản hồi 429 và Retry-After; triển khai nhiều instance phải dùng kho chung/Redis hoặc gateway có quy định cụ thể.
- Tắt patterns/demo inbox, seed, DEBUG SQL và secret mặc định ở prod. Fail-fast cấu hình bắt buộc. CSP, HTTPS/HSTS qua proxy, allowlist CORS; không mở DB/cache/broker ra Internet.

## S3 — Dữ liệu và nghiệp vụ

- Schema migration có version; production ddl-auto=validate, không tự update schema. Migration bổ sung session/token/audit/outbox; không xóa bảng cũ. Nâng cấp DB hiện có cần backup và baseline có kiểm tra, không auto baseline mù.
- Audit bền vững cho đọc/ghi bệnh án: actor, action, record/appointment ID, thời điểm. Revision bệnh án lưu trước/sau chỉnh sửa; không log nội dung bệnh án/password/token ra log ứng dụng. Chỉ admin được đọc audit nếu cung cấp endpoint.
- Notification outbox ghi cùng giao dịch booking/cancellation/reminder; dispatcher retry và publisher confirms; consumer idempotency theo message ID, retry có giới hạn + DLQ. Cam kết at-least-once, không hứa exactly-once SMTP.
- Scheduler nhắc trước 24h, unique event key tránh tạo lại; tôn trọng CONFIRMED/chưa hủy. Demo không gọi SMTP thật; production gửi qua SMTP cấu hình.
- Phân trang/query aggregate ở DB cho admin users/appointments/analytics và tìm bác sĩ để tránh findAll cho dữ liệu tăng. Nếu đổi contract phải cập nhật UI/client/tests đồng bộ.
- Không thêm thanh toán vì ngoài phạm vi đã đề xuất; thống kê phí COMPLETED không gọi là doanh thu thực thu.

## S4 — Vận hành và hạ tầng

- Profile prod riêng; container mặc định không âm thầm demo; demo chỉ chạy khi chỉ định. Compose production dùng secret/env bắt buộc, internal DB/Redis/RabbitMQ, volumes bền vững, healthchecks và readiness.
- HTTPS reverse proxy, domain/public URL cấu hình; staging dùng cùng artifact với prod; deploy theo SHA, smoke test, rollback về SHA trước. Không tự deploy/tạo tài nguyên tính phí khi chưa có môi trường cụ thể.
- Backup tự động MySQL, mã hóa nếu lưu ngoài máy, retention cấu hình; script restore vào DB rỗng/test, quy trình diễn tập, không chạy xóa dữ liệu có sẵn. RPO/RTO mục tiêu pilot: 24h/4h, chỉ đạt sau diễn tập.
- Metrics/health, request correlation ID, logs JSON hoặc structured không chứa PHI; dashboard/alerts cho 5xx, latency, readiness, outbox backlog, broker dead letters, DB/pool và backup thất bại.
- CI chạy unit/integration H2 và MySQL/Redis/RabbitMQ thật ở services/container, browser mobile/desktop, frontend regression, migration, dependency audit. Credentials kiểm thử giả trong CI, không dùng tài khoản production.

## Tiêu chí nghiệm thu và giới hạn ngoài repo

- Bộ kiểm thử hiện có giữ xanh, bổ sung RED→GREEN theo tính năng; clean verify + frontend tests + browser flows.
- Session replay/reset expired/token reuse/cross-user medical access đều có test.
- Tranh chấp đặt/cập nhật slot trên MySQL; notification rollback không lưu outbox; retry không tạo thông báo nghiệp vụ trùng.
- Production startup từ chối secret thiếu và không seed; migration chạy được trên DB mới.
- Backup/restore, gửi email tới hộp thư thật, TLS domain, cảnh báo tới nơi nhận và pilot với người dùng cần môi trường/credentials thật; báo rõ phần chưa chạy, không đánh dấu đạt thay người vận hành.

## Quyết định bổ sung sau audit dependency

Audit artifact thực ngày04/11/2026 tìm54cảnh báo HIGH/CRITICAL trong Boot3.3.2. Thử migration trong bản sao /tmp: Boot4.1.2 + AMQPclient5.34.0, Java21, không đổi API/schema. Cập nhật quyết định stack ban đầu sang Boot4.1.2 vì một số bản vá SpringMVC yêu cầu Spring7. Giữ module Jackson2 compatibility tạm thời để bảo toàn outbox/cache/API; không coi đó là lựa chọn dài hạn. Module tests/auto-config được cập nhật theo migration guide chính thức. Candidate đã qua88test thường +7real-services trước khi áp dụng root; nghiệm thu cuối phải chạy lại trên root và audit lại artifact.
