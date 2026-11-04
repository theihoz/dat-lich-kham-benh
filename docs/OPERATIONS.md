# Vận hành staging / production

## Chuẩn bị môi trường

Bản nâng cấp chạy Java21/Spring Boot, MySQL8, Redis7, RabbitMQ3. Dùng staging riêng với cùng image digest trước production. Không trộn prod với demo/dev/test. Profile demo giữ để trình diễn; image mặc định prod và sẽ từ chối cấu hình thiếu.

1. Đặt repo release trong `/opt/clinic`, sao chép `.env.production.example` thành file `.env.production` quyền0600 ngoài Git. Điền địa chỉ/số điện thoại thật và domain trỏ tới máy; mở80/443, không công khai DB/cache/broker.
2. Tạo riêng từng secret bằng `openssl rand -hex 32`; JWT tối thiểu32 ký tự, password DB/Redis/broker tối thiểu12. Không dùng root/guest làm application user. SMTP dùng tài khoản chuyên gửi thư, xác minh domain theo nhà cung cấp.
3. Chỉ định `CLINIC_IMAGE` bằng digest của release đã qua CI. `docker compose --env-file .env.production -f compose.production.yml config --quiet` kiểm tra biến bắt buộc mà không in secret.
4. `docker compose --env-file .env.production -f compose.production.yml up -d --wait`. Caddy cấp/renewTLS; readiness chỉ có giá trị khi migrations và app xong. Kiểm tra cấu hình firewall và xác nhận application không thể truy cập trực tiếp ngoài proxy.
5. Khi DB được quản lý bên ngoài, dùng TLS `sslMode=VERIFY_IDENTITY` cùng CA trust phù hợp. Compose nội bộ dùng network không publish và `sslMode=REQUIRED`; không mặc định đó là xác minh danh tính máy chủ.

Không có admin mẫu ở prod. Người vận hành tạo admin đầu tiên qua quy trình bootstrap được kiểm soát, với mật khẩu BCrypt và email đã xác minh; không dùng lại tài khoản seed. Lệnh offline có khóa MySQL và từ chối khi đã có admin:

```sh
# Export DB_URL/DB_USERNAME/DB_PASSWORD và BOOTSTRAP_ADMIN_EMAIL/BOOTSTRAP_ADMIN_PASSWORD
# từ môi trường operator bảo mật; password ít nhất16ký tự, tối đa72byteUTF8.
export BOOTSTRAP_EMAIL_VERIFIED=yes  # chỉ sau khi operator kiểm tra chủ sở hữu email
java -Dloader.main=com.datlichkhambenh.backend.ops.BootstrapAdminCommand \
  -cp backend/target/backend-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher
unset BOOTSTRAP_ADMIN_PASSWORD BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_EMAIL_VERIFIED
``` Không tạo admin bằng public registration.

## Migration và nâng cấp DB cũ

Flyway quản lý V1 baseline, V2auth và V3delivery/audit; prod Hibernate chỉ validate, không update. Không tự baseline một database không rỗng.

- DB mới: để Flyway tạo toàn bộ schema.
- DB cũ: backup trước; phục hồi clone riêng, đối chiếu schema với V1 gồm kiểu/cột/unique/FK. Chỉ khi tương đương V1 mới chạy lệnh Flyway `baseline -baselineVersion=1` trên clone rồi migrate, test dữ liệu và ứng dụng. Sau nghiệm thu mới lập maintenance window làm tương tự production.
- Không sửa migration đã áp dụng; tạo V4 trở đi. Database rollback không tự động: migration phải tương thích ngược để image cũ chạy được. Nếu không, cần kế hoạch maintenance/phục hồi được duyệt.

## Nâng cấp thông báo từ bản cũ

Production dùng exchange/queue/routing key v2 để không khai báo lại queue cũ với DLX arguments khác (RabbitMQ sẽ từ chối). Trước cutover: ngừng tạo thông báo ở bản cũ, drain legacy queue bằng consumer bản cũ, xác nhận không còn pending; dừng consumer cũ rồi mới bật v2. Không bind cả hai queue vào cùng routing key; không xóa queue còn message. SMTP at-least-once vẫn có cửa sổ crash gây gửi lại; không coi receipt là exactly-once.

## Backup và diễn tập phục hồi

Mục tiêu pilot RPO24h/RTO4h cần đo và nghiệm thu, không mặc định đạt.

- `ops/backup/backup.sh` tạo consistent dump, gzip rồi mã hóa AES256-CBC với PBKDF2, quyền0600 và checksum; retention mặc định14ngày. Lưu passphrase độc lập và sao chép backup ra kho ngoài máy qua kênh được kiểm soát. Checksum phát hiện hỏng file, không thay thế chữ ký/xác thực đối thủ sửa file.
- Chạy bằng `ops/backup/clinic-backup.timer` (02:00 theo timezone host; đặt host Asia/Ho_Chi_Minh) và service. User vận hành thuộc Docker group có quyền tương đương root; chỉ cấp cho operator tin cậy. EnvironmentFile `/etc/clinic/backup.env` quyền0600 chứa CLINIC_ENV_FILE, BACKUP_ENCRYPTION_PASSWORD và đường dẫn lưu.
- Restore yêu cầu `CLINIC_RESTORE_CONTAINER` riêng và `CLINIC_RESTORE_DATABASE=clinic_restore_<timestamp>`. Script từ chối database production/không rỗng; giải mã toàn bộ trước khi gửi SQL. Kiểm tra checksum, rowcounts, schema version và khởi động ứng dụng trỏ vào clone trước cutover.
- Ghi thời điểm backup thành công cho exporter và cảnh báo khi quá25h. Kiểm thử phục hồi hàng tháng, ghi thời gian thực và người nghiệm thu; không chỉ kiểm tra file tồn tại.

## Triển khai và rollback

CD chỉ publish sau CI thành công (manual cũng verify revision trước publish). Workflow `Deploy approved release` dùng GitHub Environments staging/production: cấu hình required reviewers, deployment branches, SSH key riêng và pinned known_hosts. Secrets gồm DEPLOY_HOST/USER/KEY/KNOWN_HOSTS; server có credentials đọc registry trước.

`ops/deploy.sh` yêu cầu image immutable digest, lưu image trước, chờ health và publicTLS rồi rollback application nếu readiness thất bại. Operator cần cấu hình CLINIC_PUBLIC_URL trong môi trường server. Release tài liệu ops/code trên host phải tương ứng image trước chạy deploy. Workflow thủ công không thay bằng chứng CI của image: operator đối chiếu digest/commit CI trước approve.

## Monitoring và ứng phó

Actuator metrics cần admin; Prometheus chấp nhận METRICS_TOKEN riêng, chỉ có quyền scrape /actuator/prometheus, không dùng được cho API admin. Không công khai token. `docker compose --env-file .env.production -f compose.production.yml --profile monitoring up -d` cung cấp Prometheus, Alertmanager, backup textfile exporter và Grafana dashboard (localhost13000). Trước chạy phải đặt GRAFANA_ADMIN_PASSWORD mạnh, METRICS_TOKEN_FILE chứa token cùng METRICS_TOKEN, ALERTMANAGER_CONFIG trỏ file có receiver thật. Grafana chỉ truy cập qua SSH tunnel/proxy có xác thực; không expose trực tiếp Internet. File receiver ví dụ cố ý chưa có địa chỉ thật và không được coi là nghiệm thu gửi cảnh báo. Lưu METRICS_TOKEN vào /run/secrets/clinic_metrics_token trên Prometheus và rotate đồng thời hai phía theo lịch; không dùng access token của admin để scrape lâu dài. Alert rules bao gồm unavailable,5xx,latency,DBpool,outboxbacklog,backupstale; bổ sung broker DLQ exporter và receiver/email/PagerDuty của phòng khám.

Logs có requestId để truy nguyên, không ghi request body/token/email y tế. Restrict logs/audit/revisions/backup theo operator tối thiểu; lịch sử bệnh án chứa dữ liệu nhạy cảm. Thời hạn lưu/xóa dữ liệu phải được phòng khám xác nhận theo chính sách áp dụng trước pilot.

## Nghiệm thu bắt buộc với môi trường thật

- TLS/domain/SMTP inbox thật, email verification/reset, chống spam và thu hồi phiên.
- Tranh chấp lịch MySQL nhiều phiên; mất Redis/broker và phục hồi; outbox không mất sau restart.
- Backuprestore trên clone, downtime và rollback qua staging.
- Người dùng thử trên mobile/keyboard, bác sĩ nghiệm thu quy trình bệnh án, người vận hành ký nghiệm thu alertreceivers/permissions/dataretention.

### Monitoring file permissions

The metrics token bind-mount must be readable by the Prometheus container UID65534 (for example owner65534 and mode0400 on a Linux host); keep its parent directory restricted to operators. Backup metric files intentionally contain only a success timestamp and are0644 in a0755 metrics directory so node-exporter can read them. Encrypted backup files remain0600. The application exports `clinic_notifications_consumer_dead` for RabbitMQ DLQ messages separately from `clinic_notifications_outbox_dead` for publisher exhaustion; both have alert rules. Configure a real receiver before enabling the monitoring profile.
