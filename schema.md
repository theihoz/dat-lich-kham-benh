# Lược đồ dữ liệu hệ thống đặt lịch khám

## 1. Nguồn và ký hiệu

Đối chiếu entity tại `backend/src/main/java/com/datlichkhambenh/backend/entity/`. Đây là từ điển mô hình hiện có, không phải migration. `schema.sql` chỉ có `SELECT 1`; Hibernate sinh/cập nhật bảng theo profile. Không xóa DB hoặc thêm/bớt cột chỉ để khớp tài liệu. Demo dùng cùng entity trên H2 RAM, `create-drop`; chế độ tích hợp dùng MySQL, mặc định `update`.

PK: khóa chính; FK: khóa ngoại; UQ: duy nhất; NN: không null. Kiểu vật lý cuối và tên constraint cần kiểm tra metadata DB khi nghiệm thu. Các cột camelCase không chỉ định tên được ghi theo quy ước snake_case Spring Boot. Miền chuỗi trạng thái/giới tính là quy ước nghiệp vụ, không mặc định có enum/CHECK tại DB. “Mặc định Java” không khẳng định có SQL DEFAULT.

## 2. USERS

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | BIGINT | PK, IDENTITY |
| `email` | VARCHAR(255) | NN, UQ |
| `password_hash` | VARCHAR(255) | NN; kết quả băm BCrypt |
| `role` | VARCHAR(20) | NN; PATIENT/DOCTOR/ADMIN |
| `is_active` | BOOLEAN | NN; mặc định Java true |
| `created_at` | timestamp theo JPA | NN, tự sinh, không cập nhật |
| `updated_at` | timestamp theo JPA | Tự cập nhật |

## 3. PATIENTS

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `user_id` | BIGINT | PK và FK → `users.id`, `@MapsId`; không có `id` riêng |
| `full_name` | VARCHAR(255) | NN |
| `dob` | DATE | Có thể null |
| `gender` | VARCHAR(10) | Có thể null; MALE/FEMALE/OTHER theo nghiệp vụ |
| `phone` | VARCHAR(20) | UQ trong bảng, có thể null |
| `address` | TEXT | Có thể null |
| `medical_history` | TEXT | Tiền sử, khác kết quả từng lần khám |

## 4. SPECIALTIES

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | BIGINT | PK, IDENTITY |
| `name` | VARCHAR(100) | NN; entity không khai báo UQ |
| `description` | TEXT | Có thể null |

## 5. DOCTORS

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `user_id` | BIGINT | PK, FK → `users.id`, `@MapsId` |
| `specialty_id` | BIGINT | NN, FK → `specialties.id` |
| `full_name` | VARCHAR(255) | NN |
| `phone` | VARCHAR(20) | UQ trong bảng, có thể null |
| `degree` | VARCHAR(100) | Có thể null |
| `experience_years` | INTEGER | Mapping cho phép null; kiểm tra đầu vào ở DTO/service |
| `consultation_fee` | DECIMAL(12,2) | Mapping cho phép null; giá dùng lúc đặt lịch |
| `is_approved` | BOOLEAN | NN; mặc định Java false |

UQ số điện thoại của PATIENTS và DOCTORS là hai ràng buộc riêng, không bảo đảm duy nhất xuyên hệ thống. `@MapsId` dùng chung danh tính, không phải kế thừa bảng bằng `@Inheritance`.

## 6. MEDICAL_SERVICES — danh mục dịch vụ đã có

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | BIGINT | PK, IDENTITY |
| `code` | VARCHAR(30) | NN, UQ |
| `name` | VARCHAR(150) | NN |
| `description` | TEXT | Có thể null |
| `specialty_id` | BIGINT | FK → `specialties.id`, có thể null |
| `default_price` | DECIMAL(12,2) | NN |
| `duration_minutes` | INTEGER | NN |
| `active` | BOOLEAN | NN |

Danh mục được quản lý qua `MedicalServiceServiceImpl` và `/api/v1/services`. Appointment **không có `service_id`**, không có chi tiết dịch vụ sử dụng, hóa đơn hay payment. Không vẽ quan hệ dịch vụ–appointment hoặc khẳng định `default_price` quyết định phí đặt lịch; hiện lấy phí bác sĩ.

## 7. SCHEDULES

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | BIGINT | PK, IDENTITY |
| `doctor_id` | BIGINT | NN, FK → `doctors.user_id` |
| `work_date` | DATE | NN |
| `start_time` | TIME | NN |
| `end_time` | TIME | NN |
| `is_available` | BOOLEAN | NN, mặc định Java true |

Service yêu cầu ngày tạo/sửa sau ngày hiện tại, end > start; kiểm tra giao nhau bằng `start < requestedEnd AND end > requestedStart`. Hai ca chạm đúng ranh giới không giao nhau theo điều kiện đó. Entity không có UQ tổ hợp bác sĩ/ngày/giờ; cần giữ được slot lịch sử cùng khung giờ với slot thay thế sau hủy.

## 8. APPOINTMENTS

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | BIGINT | PK, IDENTITY |
| `booking_code` | VARCHAR(100) | NN, UQ; service tạo `BK-` + một phần UUID |
| `patient_id` | BIGINT | NN, FK → `patients.user_id` |
| `doctor_id` | BIGINT | NN, FK → `doctors.user_id` |
| `schedule_id` | BIGINT | NN, FK → `schedules.id`, UQ trên **mọi trạng thái** |
| `symptoms` | TEXT | Có thể null tại mapping |
| `status` | VARCHAR(20) | NN; CONFIRMED/CANCELLED/COMPLETED |
| `price` | DECIMAL(12,2) | Bản chụp phí bác sĩ lúc đặt; mapping cho phép null |
| `created_at` | timestamp theo JPA | NN, tự sinh, không cập nhật |

FK riêng không tự bảo đảm doctor của appointment bằng doctor của schedule; service lấy doctor từ schedule để duy trì. UUID bị cắt không phải chứng minh không bao giờ va chạm; UQ là hàng rào cuối, chưa suy ra có retry tự động.

## 9. MEDICAL_RECORDS

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | BIGINT | PK, IDENTITY |
| `appointment_id` | BIGINT | NN, UQ, FK → `appointments.id` |
| `diagnosis` | TEXT | Mapping cho phép null; DTO có thể yêu cầu nội dung |
| `doctor_notes` | TEXT | Có thể null |
| `created_at` | timestamp theo JPA | NN, tự sinh; entity không có `updated_at` |

API bệnh án tạo hoặc cập nhật một bản ghi và chuyển lịch sang COMPLETED. Appointment có thể chưa có bệnh án, kể cả khi được hoàn thành bằng API trạng thái riêng; không suy ra tồn tại bệnh án chỉ từ COMPLETED.

## 10. Quan hệ và ERD

| Quan hệ | Lực lượng |
|---|---|
| User → Patient | 1 → 0..1; mỗi Patient có đúng một User |
| User → Doctor | 1 → 0..1; Admin không cần hai loại hồ sơ này |
| Specialty → Doctor | 1 → 0..N; mỗi Doctor thuộc một Specialty |
| Specialty → MedicalService | 1 → 0..N; MedicalService có thể không gắn Specialty |
| Doctor → Schedule | 1 → 0..N |
| Doctor → Appointment | 1 → 0..N |
| Patient → Appointment | 1 → 0..N |
| Schedule → Appointment | 1 → 0..1; mỗi Appointment có đúng một Schedule |
| Appointment → MedicalRecord | 1 → 0..1 |

Nguồn sơ đồ: [HeThongDatLich.drawio](docs/diagrams/HeThongDatLich.drawio), trang `ERD` và `Domain Classes`. Giữ lực lượng 0..1 thay vì khẳng định mọi tài khoản có hồ sơ/mọi lịch có bệnh án.

## 11. Hủy rồi đặt lại

A1 đang tham chiếu S1. Hủy hợp lệ giữ A1→S1, A1=CANCELLED và S1 không khả dụng; thêm S2 cùng bác sĩ/ngày/giờ nhưng ID mới, khả dụng. Lần đặt mới A2→S2, UQ schedule_id vẫn đúng và không mất A1. Truy vấn lịch có thể thấy nhiều dòng cùng khung giờ với mục đích khác nhau.

Không bật lại S1 để đặt thêm, không bỏ unique, không xóa A1. Khóa slot khi đặt/appointment khi hủy giúp tuần tự hóa nghiệp vụ; khả năng tranh chấp cần test MySQL thật, không lấy ERD hoặc test H2 làm bằng chứng đủ.

## 12. Phân tích và giới hạn

Tài khoản, hồ sơ, chuyên khoa tách thành các bảng có khóa riêng. `Appointment.price` cố ý giữ giá lịch sử; `doctor_id` trên appointment lặp thông tin suy ra từ schedule, cần service bảo đảm nhất quán. Không khẳng định chứng minh đạt 3NF cho mọi phụ thuộc hàm chỉ vì có tách bảng.

Chưa có bảng payment/receipt, đơn thuốc, audit đầy đủ hay transactional outbox. Tổng `price` của các lịch COMPLETED là tổng phí gắn lịch đã hoàn thành, không phải tiền đã thu. Inbox demo là bộ nhớ ứng dụng, không phải bảng mới; tài liệu không tự bổ sung cấu trúc DB.
