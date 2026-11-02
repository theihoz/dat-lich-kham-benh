# BÁO CÁO ĐỒ ÁN: HỆ THỐNG QUẢN LÝ ĐẶT LỊCH KHÁM BỆNH

**Đề tài 3: Ứng dụng Design Patterns trong xây dựng hệ thống đặt lịch khám bệnh**

---

## MỤC LỤC

- [Chương 1: Tổng quan đề tài](#chương-1-tổng-quan-đề-tài)
- [Chương 2: Khảo sát yêu cầu](#chương-2-khảo-sát-yêu-cầu)
- [Chương 3: Cơ sở lý thuyết và kiến trúc hệ thống](#chương-3-cơ-sở-lý-thuyết-và-kiến-trúc-hệ-thống)
- [Chương 4: Phân tích và thiết kế chi tiết Design Patterns](#chương-4-phân-tích-và-thiết-kế-chi-tiết-design-patterns)
- [Chương 5: Cài đặt và minh chứng SOLID](#chương-5-cài-đặt-và-minh-chứng-solid)
- [Chương 6: Kiểm thử và đánh giá](#chương-6-kiểm-thử-và-đánh-giá)
- [Chương 7: Kết luận](#chương-7-kết-luận)

---

## Chương 1: Tổng quan đề tài

### 1.1. Bối cảnh thực tế

Tại các phòng khám địa phương, quy trình đặt lịch khám bệnh phần lớn vẫn được thực hiện thủ công: bệnh nhân gọi điện thoại hoặc đến trực tiếp quầy lễ tân để đăng ký, nhân viên ghi sổ tay hoặc bảng tính Excel, bác sĩ kiểm tra lịch bằng giấy. Cách làm này dẫn đến nhiều bất cập: trùng lịch hẹn khi hai bệnh nhân cùng đặt một khung giờ, thời gian chờ đợi kéo dài, khó khăn trong thống kê và quản lý, đồng thời không có cơ chế thông báo tự động khi lịch hẹn thay đổi.

### 1.2. Vấn đề cần giải quyết

- Quản lý lịch hẹn thủ công dễ xảy ra trùng lịch, mất thông tin.
- Bệnh nhân không chủ động tra cứu lịch trống và đặt lịch từ xa.
- Bác sĩ khó nắm bắt tổng quan lịch khám trong ngày, trong tuần.
- Quản trị viên thiếu công cụ thống kê để ra quyết định.
- Hệ thống khó mở rộng khi cần bổ sung kênh thông báo, loại hình khám hay cách sắp xếp mới.

### 1.3. Mục tiêu đồ án

- Xây dựng hệ thống đặt lịch khám bệnh trực tuyến hoàn chỉnh sử dụng Spring Boot.
- Áp dụng tối thiểu **6 Design Patterns** thuộc đủ **3 nhóm** (Creational, Structural, Behavioral).
- Chứng minh hệ thống tuân thủ **5 nguyên lý SOLID** bằng mã nguồn thực tế.
- Thiết kế UML chi tiết: Class Diagram và Sequence Diagram cho từng pattern.
- Xây dựng bộ test case kiểm chứng hoạt động của từng pattern và nghiệp vụ cốt lõi.

### 1.4. Phạm vi chức năng

| Mã  | Chức năng              | Mô tả                                            |
|-----|------------------------|---------------------------------------------------|
| F01 | Quản lý tài khoản      | Đăng ký, đăng nhập, phân quyền (JWT)             |
| F02 | Quản lý bệnh nhân      | Cập nhật hồ sơ, thông tin liên hệ                |
| F03 | Quản lý bác sĩ         | Thêm, sửa thông tin bác sĩ và chuyên khoa        |
| F04 | Quản lý lịch làm việc  | Thiết lập ca khám, khung giờ trống                |
| F05 | Tìm kiếm bác sĩ        | Tìm theo chuyên khoa, tên, sắp xếp đa tiêu chí  |
| F06 | Đặt lịch khám          | Chọn bác sĩ, ngày, giờ và tạo lịch hẹn           |
| F07 | Quản lý lịch hẹn       | Xem, hủy lịch và cập nhật trạng thái              |
| F08 | Thông báo              | Gửi thông báo xác nhận, hủy lịch qua email        |
| F09 | Ghi bệnh án            | Bác sĩ ghi chẩn đoán sau khám                     |
| F10 | Thống kê               | Thống kê lịch hẹn theo trạng thái, doanh thu      |

### 1.5. Đối tượng sử dụng

| Tác nhân          | Vai trò                                                       |
|-------------------|---------------------------------------------------------------|
| **Bệnh nhân**    | Tìm bác sĩ, xem lịch trống, đặt và hủy lịch, xem bệnh án    |
| **Bác sĩ**       | Xem lịch khám, quản lý ca khám, cập nhật trạng thái, ghi bệnh án |
| **Quản trị viên** | Quản lý tài khoản, duyệt bác sĩ, quản lý chuyên khoa, thống kê |

---

## Chương 2: Khảo sát yêu cầu

### 2.1. Use Case Diagram tổng quát

> Xem sơ đồ: [`docs/diagrams/use-case.drawio`](diagrams/use-case.drawio)

Sơ đồ Use Case thể hiện 3 tác nhân (Bệnh nhân, Bác sĩ, Quản trị viên) và các ca sử dụng chính của hệ thống đặt lịch khám bệnh.

### 2.2. Đặc tả Use Case cốt lõi

#### UC-01: Đặt lịch khám

| Thuộc tính       | Mô tả                                                                    |
|------------------|---------------------------------------------------------------------------|
| **Tên**          | Đặt lịch khám                                                            |
| **Tác nhân**     | Bệnh nhân                                                                |
| **Tiền điều kiện** | Bệnh nhân đã đăng nhập và có hồ sơ bệnh nhân                         |
| **Hậu điều kiện** | Lịch hẹn được tạo với trạng thái CONFIRMED, slot trở thành không khả dụng |

**Luồng chính:**

1. Bệnh nhân truy cập danh sách bác sĩ và chọn bác sĩ theo chuyên khoa.
2. Hệ thống hiển thị các ca khám còn trống của bác sĩ đã chọn.
3. Bệnh nhân chọn ngày, khung giờ và nhập triệu chứng.
4. Hệ thống kiểm tra slot còn khả dụng (Pessimistic Lock trên bản ghi Schedule).
5. Hệ thống tạo lịch hẹn mới bằng Builder Pattern, lưu cơ sở dữ liệu.
6. Hệ thống cập nhật slot thành không khả dụng.
7. Hệ thống phát sự kiện thông báo (Observer Pattern) → tạo nội dung email (Factory Method) → gửi qua Adapter.
8. Hệ thống trả kết quả đặt lịch thành công cho bệnh nhân.

**Luồng ngoại lệ:**

- 4a. Slot đã được đặt bởi người khác → Hệ thống thông báo "Ca khám đã được đặt" (`BookingConflictException`).
- 4b. Ca khám đã qua thời gian → Hệ thống từ chối đặt lịch.
- 4c. Bác sĩ chưa được duyệt hoặc không hoạt động → Hệ thống từ chối.

#### UC-02: Hủy lịch hẹn

| Thuộc tính       | Mô tả                                                                    |
|------------------|---------------------------------------------------------------------------|
| **Tên**          | Hủy lịch hẹn                                                             |
| **Tác nhân**     | Bệnh nhân                                                                |
| **Tiền điều kiện** | Bệnh nhân có lịch hẹn trạng thái CONFIRMED, còn trước giờ khám ≥ 24 giờ |
| **Hậu điều kiện** | Lịch hẹn chuyển sang CANCELLED, slot thay thế được tạo mới               |

**Luồng chính:**

1. Bệnh nhân chọn lịch hẹn cần hủy.
2. Hệ thống kiểm tra quyền sở hữu (chỉ bệnh nhân tạo lịch mới được hủy).
3. Hệ thống kiểm tra điều kiện thời gian (trước giờ khám ít nhất 24 giờ).
4. Hệ thống cập nhật trạng thái lịch hẹn thành CANCELLED.
5. Hệ thống giữ slot cũ không khả dụng (bảo toàn lịch sử) và tạo slot thay thế mới cùng bác sĩ/ngày/giờ với trạng thái khả dụng.
6. Hệ thống phát sự kiện hủy lịch → gửi email thông báo cho bệnh nhân.

**Luồng ngoại lệ:**

- 2a. Bệnh nhân không phải người tạo lịch → Từ chối (`AppException`).
- 3a. Còn dưới 24 giờ trước giờ khám → Từ chối hủy.
- 3b. Lịch đã bị hủy hoặc hoàn thành → Từ chối.

#### UC-03: Quản lý lịch làm việc

| Thuộc tính       | Mô tả                                                                    |
|------------------|---------------------------------------------------------------------------|
| **Tên**          | Quản lý lịch làm việc                                                    |
| **Tác nhân**     | Bác sĩ                                                                   |
| **Tiền điều kiện** | Bác sĩ đã đăng nhập, đã được duyệt bởi admin                          |
| **Hậu điều kiện** | Ca khám được tạo/sửa/xóa thành công                                     |

**Luồng chính:**

1. Bác sĩ truy cập trang quản lý lịch làm việc.
2. Hệ thống hiển thị danh sách ca khám hiện có của bác sĩ.
3. Bác sĩ tạo ca khám mới: chọn ngày (phải là ngày tương lai), giờ bắt đầu, giờ kết thúc.
4. Hệ thống kiểm tra không chồng lấn với ca khám hiện có.
5. Hệ thống lưu ca khám mới với trạng thái khả dụng.

**Luồng ngoại lệ:**

- 3a. Ngày đã qua → Từ chối.
- 4a. Ca khám chồng lấn → Hệ thống thông báo lỗi.
- Bác sĩ không thể sửa/xóa ca đã có lịch hẹn.

### 2.3. Yêu cầu phi chức năng

| Yêu cầu            | Mô tả                                                                              |
|---------------------|-------------------------------------------------------------------------------------|
| **Bảo mật**         | Xác thực JWT, phân quyền PATIENT/DOCTOR/ADMIN, kiểm tra quyền sở hữu bản ghi      |
| **Hiệu năng**       | Thời gian phản hồi API hợp lý, cache Redis cho danh sách bác sĩ và slot            |
| **Toàn vẹn dữ liệu** | Pessimistic Lock khi đặt lịch, Unique Constraint trên schedule_id của appointment |
| **Khả năng bảo trì** | Code tổ chức theo kiến trúc 3 tầng, tách biệt trách nhiệm rõ ràng                 |
| **Khả năng mở rộng** | Thêm kênh thông báo, cách sắp xếp, loại thông báo mà ít ảnh hưởng code hiện có    |
| **Độ tin cậy**       | Xử lý lỗi, kiểm tra dữ liệu đầu vào, rollback khi giao dịch thất bại             |

---

## Chương 3: Cơ sở lý thuyết và kiến trúc hệ thống

### 3.1. Lý thuyết Design Patterns

Design Patterns (mẫu thiết kế) là các giải pháp tổng quát, đã được kiểm chứng cho các vấn đề thường gặp trong thiết kế phần mềm hướng đối tượng. Chúng được phân thành 3 nhóm chính:

#### 3.1.1. Creational Patterns (Mẫu khởi tạo)

Nhóm này giải quyết vấn đề liên quan đến việc tạo đối tượng, cung cấp cơ chế khởi tạo linh hoạt, tái sử dụng và giảm phụ thuộc vào class cụ thể.

- **Builder**: Tách biệt quá trình xây dựng một đối tượng phức tạp khỏi biểu diễn của nó, cho phép cùng một quá trình xây dựng tạo ra các biểu diễn khác nhau.
- **Factory Method**: Định nghĩa interface cho việc tạo đối tượng, nhưng để lớp con quyết định lớp nào được khởi tạo.

#### 3.1.2. Structural Patterns (Mẫu cấu trúc)

Nhóm này giải quyết vấn đề tổ chức cấu trúc lớp và đối tượng, giúp các thành phần kết hợp với nhau thành cấu trúc lớn hơn mà vẫn linh hoạt và hiệu quả.

- **Adapter**: Chuyển đổi giao diện của một lớp thành giao diện khác mà client mong đợi, cho phép các lớp không tương thích làm việc cùng nhau.
- **Facade**: Cung cấp giao diện thống nhất, đơn giản cho một tập hợp các giao diện phức tạp trong hệ thống con.

#### 3.1.3. Behavioral Patterns (Mẫu hành vi)

Nhóm này giải quyết vấn đề phân phối trách nhiệm và giao tiếp giữa các đối tượng, đảm bảo sự linh hoạt trong tương tác.

- **Strategy**: Định nghĩa một họ thuật toán, đóng gói từng thuật toán và cho phép chúng thay thế lẫn nhau tại runtime.
- **Observer**: Định nghĩa mối quan hệ phụ thuộc một-nhiều giữa các đối tượng, khi một đối tượng thay đổi trạng thái thì tất cả đối tượng phụ thuộc được thông báo và cập nhật tự động.

### 3.2. Năm nguyên lý SOLID

| Nguyên lý | Tên đầy đủ                  | Định nghĩa                                                                                                |
|-----------|-----------------------------|------------------------------------------------------------------------------------------------------------|
| **S**     | Single Responsibility       | Mỗi lớp chỉ nên có duy nhất một lý do để thay đổi, tức là chỉ đảm nhận một trách nhiệm.                  |
| **O**     | Open/Closed                 | Các thực thể phần mềm nên mở cho mở rộng nhưng đóng cho sửa đổi.                                         |
| **L**     | Liskov Substitution         | Các đối tượng của lớp con phải có thể thay thế đối tượng của lớp cha mà không làm thay đổi tính đúng đắn. |
| **I**     | Interface Segregation       | Client không nên bị buộc phụ thuộc vào các interface mà nó không sử dụng.                                  |
| **D**     | Dependency Inversion        | Module cấp cao không nên phụ thuộc vào module cấp thấp; cả hai nên phụ thuộc vào abstraction.              |

### 3.3. Kiến trúc 3 tầng

> Xem sơ đồ: [`docs/diagrams/architecture.drawio`](diagrams/architecture.drawio)

Hệ thống được thiết kế theo kiến trúc phân lớp với ba tầng logic:

| Tầng                  | Thành phần                                           | Trách nhiệm                                                            |
|-----------------------|------------------------------------------------------|-------------------------------------------------------------------------|
| **Tầng trình bày**    | HTML/CSS/JavaScript, REST Controllers (`controller/*`), DTOs | Hiển thị giao diện, nhận HTTP request, validation, trả response DTO    |
| **Tầng nghiệp vụ**    | Services (`service/*`, `service/impl/*`), Design Patterns (`pattern/*`) | Quy tắc nghiệp vụ, giao dịch, lựa chọn thuật toán, phát sự kiện       |
| **Tầng truy cập dữ liệu** | JPA Repositories (`repository/*`), Entities, MySQL/H2     | Đọc/ghi cơ sở dữ liệu, ánh xạ quan hệ                                |

**Mối quan tâm xuyên tầng:**

- **Security**: JWT filter, Spring Security với 3 authority (PATIENT, DOCTOR, ADMIN).
- **Messaging**: RabbitMQ (production) / Demo Observer (trình diễn) cho thông báo bất đồng bộ.
- **Cache**: Redis cache-aside (production) / NoOp (demo) cho danh sách bác sĩ và slot.

### 3.4. Mô hình thực thể dữ liệu

> Xem sơ đồ: [`docs/diagrams/erd.drawio`](diagrams/erd.drawio)

Hệ thống gồm 8 thực thể chính:

| Thực thể           | Mô tả                                   | Quan hệ chính                          |
|---------------------|------------------------------------------|-----------------------------------------|
| **Users**           | Tài khoản người dùng                     | 1 → 0..1 Patient, 1 → 0..1 Doctor      |
| **Patients**        | Hồ sơ bệnh nhân                         | 1 → 0..N Appointments                  |
| **Doctors**         | Hồ sơ bác sĩ                            | N → 1 Specialty, 1 → 0..N Schedules    |
| **Specialties**     | Chuyên khoa y tế                         | 1 → 0..N Doctors                       |
| **MedicalServices** | Danh mục dịch vụ khám                    | N → 1 Specialty                         |
| **Schedules**       | Ca khám (slot thời gian)                 | 1 → 0..1 Appointment                   |
| **Appointments**    | Lịch hẹn khám bệnh                      | N → 1 Patient, N → 1 Doctor            |
| **MedicalRecords**  | Hồ sơ bệnh án                           | 1 → 1 Appointment                      |

---

## Chương 4: Phân tích và thiết kế chi tiết Design Patterns

Hệ thống áp dụng 6 Design Patterns thuộc đủ 3 nhóm:

| Nhóm       | Pattern        | Áp dụng trong hệ thống                             |
|------------|----------------|-----------------------------------------------------|
| Creational | Builder        | Xây dựng đối tượng lịch hẹn (`Appointment`)         |
| Creational | Factory Method | Tạo nội dung thông báo theo loại sự kiện            |
| Structural | Adapter        | Tích hợp kênh gửi thông báo (SMTP / Demo inbox)    |
| Structural | Facade         | Giao diện thống nhất cho quy trình đặt/hủy lịch     |
| Behavioral | Strategy       | Thay đổi thuật toán sắp xếp danh sách bác sĩ       |
| Behavioral | Observer       | Thông báo bất đồng bộ khi lịch hẹn thay đổi         |

### 4.1. Builder Pattern (Creational)

#### 4.1.1. Vấn đề thực tế

Đối tượng `Appointment` có 8 thuộc tính (bookingCode, patient, doctor, schedule, symptoms, status, price, createdAt). Nếu dùng constructor truyền thống, lời gọi constructor dài và dễ nhầm lẫn thứ tự tham số, đặc biệt khi nhiều tham số cùng kiểu dữ liệu.

#### 4.1.2. Lý do chọn Builder

Builder Pattern cho phép khởi tạo đối tượng phức tạp theo từng bước với các phương thức đặt tên rõ ràng (fluent API), giúp code dễ đọc, dễ bảo trì và giảm lỗi do nhầm tham số.

#### 4.1.3. Các lớp và trách nhiệm

| Lớp                          | Vai trò   | Trách nhiệm                                                    |
|-------------------------------|-----------|----------------------------------------------------------------|
| `Appointment`                 | Product   | Entity chính, chứa static `builder()` trả về Builder          |
| `Appointment.AppointmentBuilder` | Builder  | Inner class với fluent setter cho từng thuộc tính và `build()` |
| `BookingServiceImpl`          | Client    | Sử dụng Builder để tạo lịch hẹn mới                           |

#### 4.1.4. Class Diagram

> Xem sơ đồ: [`docs/diagrams/builder-class.drawio`](diagrams/builder-class.drawio)

#### 4.1.5. Sequence Diagram

> Xem sơ đồ: [`docs/diagrams/builder-sequence.drawio`](diagrams/builder-sequence.drawio)

#### 4.1.6. Mã nguồn minh chứng

**Builder inner class trong `Appointment.java`:**

```java
public static class AppointmentBuilder {
    private Long id;
    private String bookingCode;
    private Patient patient;
    private Doctor doctor;
    private Schedule schedule;
    private String symptoms;
    private String status;
    private BigDecimal price;
    private LocalDateTime createdAt;

    public AppointmentBuilder bookingCode(String bookingCode) {
        this.bookingCode = bookingCode; return this;
    }
    public AppointmentBuilder patient(Patient patient) {
        this.patient = patient; return this;
    }
    public AppointmentBuilder doctor(Doctor doctor) {
        this.doctor = doctor; return this;
    }
    public AppointmentBuilder schedule(Schedule schedule) {
        this.schedule = schedule; return this;
    }
    public AppointmentBuilder symptoms(String symptoms) {
        this.symptoms = symptoms; return this;
    }
    public AppointmentBuilder status(String status) {
        this.status = status; return this;
    }
    public AppointmentBuilder price(BigDecimal price) {
        this.price = price; return this;
    }

    public Appointment build() {
        return new Appointment(id, bookingCode, patient, doctor,
                schedule, symptoms, status, price, createdAt);
    }
}
```

**Sử dụng Builder trong `BookingServiceImpl.createBooking()`:**

```java
Appointment appointment = Appointment.builder()
        .bookingCode(bookingCode)
        .patient(patient)
        .doctor(doctor)
        .schedule(schedule)
        .symptoms(request.getSymptoms())
        .status("CONFIRMED")
        .price(doctor.getConsultationFee())
        .build();
```

#### 4.1.7. Ưu điểm và nhược điểm

| Ưu điểm                                               | Nhược điểm                                 |
|--------------------------------------------------------|---------------------------------------------|
| Code dễ đọc, mỗi thuộc tính được đặt tên rõ ràng      | Cần viết thêm inner class Builder           |
| Không nhầm lẫn thứ tự tham số                          | Tăng số lượng code so với constructor đơn   |
| Dễ thêm thuộc tính mới mà không ảnh hưởng client hiện có | Không bắt buộc điền đầy đủ thuộc tính (có thể null) |
| Hỗ trợ đối tượng bất biến (immutable) sau khi build   |                                             |

#### 4.1.8. Đánh giá tuân thủ SOLID

- **SRP**: `AppointmentBuilder` chỉ chịu trách nhiệm khởi tạo `Appointment`, tách biệt khỏi logic nghiệp vụ.
- **OCP**: Thêm thuộc tính mới vào `Appointment` chỉ cần bổ sung fluent setter trong Builder, không sửa code client.

---

### 4.2. Factory Method Pattern (Creational)

#### 4.2.1. Vấn đề thực tế

Hệ thống cần tạo nhiều loại nội dung thông báo email khác nhau: xác nhận đặt lịch, hủy lịch và nhắc lịch hẹn. Mỗi loại có tiêu đề và nội dung khác biệt. Nếu dùng `if-else` hoặc `switch-case` trong một class duy nhất, mỗi lần thêm loại thông báo mới phải sửa class hiện có.

#### 4.2.2. Lý do chọn Factory Method

Factory Method cho phép mỗi lớp con (Concrete Creator) tự quyết định loại sản phẩm (Content) được tạo ra. Thêm loại thông báo mới chỉ cần tạo Creator và Product mới, không sửa code hiện có.

#### 4.2.3. Các lớp và trách nhiệm

| Lớp                                | Vai trò            | Trách nhiệm                                          |
|-------------------------------------|---------------------|------------------------------------------------------|
| `NotificationCreator`               | Abstract Creator    | Định nghĩa `createMessage()` và template `appointmentDetails()` |
| `ConfirmationNotificationCreator`   | Concrete Creator    | Tạo nội dung xác nhận đặt lịch                       |
| `CancellationNotificationCreator`   | Concrete Creator    | Tạo nội dung hủy lịch                                |
| `ReminderNotificationCreator`       | Concrete Creator    | Tạo nội dung nhắc lịch                               |
| `NotificationContent`               | Product Interface   | Interface với `subject()` và `body()`                 |
| `ConfirmationContent`               | Concrete Product    | Nội dung xác nhận (Java record)                       |
| `CancellationContent`               | Concrete Product    | Nội dung hủy lịch (Java record)                       |
| `ReminderContent`                   | Concrete Product    | Nội dung nhắc lịch (Java record)                      |
| `NotificationCreatorRegistry`       | Registry            | Auto-discover creators qua Spring DI                  |
| `EmailQueueConsumer`                | Client              | Sử dụng registry để chọn creator và tạo nội dung     |

#### 4.2.4. Class Diagram

> Xem sơ đồ: [`docs/diagrams/factory-method-class.drawio`](diagrams/factory-method-class.drawio)

#### 4.2.5. Sequence Diagram

> Xem sơ đồ: [`docs/diagrams/factory-method-sequence.drawio`](diagrams/factory-method-sequence.drawio)

#### 4.2.6. Mã nguồn minh chứng

**Abstract Creator — `NotificationCreator.java`:**

```java
public abstract class NotificationCreator {
    public abstract String type();
    public abstract NotificationContent createMessage(EmailNotificationMessage notification);

    protected String appointmentDetails(EmailNotificationMessage n, String introduction) {
        return ("Kính gửi %s,\n\n%s\nMã đặt chỗ: %s\nBác sĩ: %s\nNgày khám: %s\n"
                + "Thời gian: %s - %s\nPhí khám: %s VNĐ")
                .formatted(n.patientName(), introduction, n.bookingCode(), n.doctorName(),
                        n.workDate(), n.startTime(), n.endTime(), n.price());
    }
}
```

**Concrete Creator — `ConfirmationNotificationCreator.java`:**

```java
@Component
public class ConfirmationNotificationCreator extends NotificationCreator {
    @Override public String type() { return "BOOKING_CONFIRMED"; }
    @Override public NotificationContent createMessage(EmailNotificationMessage n) {
        return new ConfirmationContent(
                "Xác nhận đặt lịch khám - Mã: " + n.bookingCode(),
                appointmentDetails(n, "Bạn đã đặt lịch khám thành công."));
    }
}
```

**Registry — `NotificationCreatorRegistry.java`:**

```java
@Component
public class NotificationCreatorRegistry {
    private final Map<String, NotificationCreator> creators;

    public NotificationCreatorRegistry(List<NotificationCreator> creators) {
        this.creators = creators.stream().collect(Collectors.toUnmodifiableMap(
                NotificationCreator::type, Function.identity()));
    }

    public NotificationCreator get(String type) {
        NotificationCreator creator = creators.get(type);
        if (creator == null)
            throw new IllegalArgumentException("Loại thông báo không hỗ trợ: " + type);
        return creator;
    }
}
```

**Client — `EmailQueueConsumer.java`:**

```java
public void consume(EmailNotificationMessage notification) {
    NotificationContent content = creators.get(notification.type()).createMessage(notification);
    sender.send(notification.recipientEmail(), content);
}
```

#### 4.2.7. Ưu điểm và nhược điểm

| Ưu điểm                                                    | Nhược điểm                                  |
|-------------------------------------------------------------|----------------------------------------------|
| Thêm loại thông báo mới không sửa code hiện có              | Nhiều lớp hơn so với `if-else` đơn giản      |
| Spring DI tự phát hiện Creator mới qua `@Component`         | Cần hiểu cấu trúc Creator/Product             |
| Mỗi Creator kiểm thử độc lập                                |                                              |
| Template method `appointmentDetails()` tái sử dụng logic chung |                                          |

#### 4.2.8. Đánh giá tuân thủ SOLID

- **OCP**: Thêm loại thông báo mới (ví dụ: nhắc thanh toán) chỉ cần tạo Creator + Product mới, không sửa registry hay consumer.
- **SRP**: Mỗi Creator chỉ lo tạo một loại nội dung cụ thể.
- **DIP**: `EmailQueueConsumer` phụ thuộc interface `NotificationContent`, không phụ thuộc trực tiếp implementation.

---

### 4.3. Adapter Pattern (Structural)

#### 4.3.1. Vấn đề thực tế

Hệ thống cần gửi thông báo qua nhiều kênh khác nhau: SMTP thật (production) và inbox giả lập trong bộ nhớ (demo). API của `JavaMailSender` (Spring) khác với domain interface mong muốn. Khi trình diễn, không cần cấu hình SMTP thật.

#### 4.3.2. Lý do chọn Adapter

Adapter cho phép hệ thống làm việc với các nhà cung cấp gửi thông báo có API khác nhau thông qua một interface chung (`NotificationSender`). Kênh gửi được chọn bằng Spring Profile, không sửa code nghiệp vụ.

#### 4.3.3. Các lớp và trách nhiệm

| Lớp                         | Vai trò   | Trách nhiệm                                               |
|------------------------------|-----------|-----------------------------------------------------------|
| `NotificationSender`        | Target    | Interface nghiệp vụ: `send(recipient, content)`           |
| `SmtpNotificationAdapter`   | Adapter   | Dịch `NotificationContent` sang `SimpleMailMessage`, gọi `JavaMailSender` |
| `DemoNotificationAdapter`   | Adapter   | Lưu thông báo vào `ArrayDeque` trong bộ nhớ (tối đa 100 mục) |
| `JavaMailSender`            | Adaptee   | API gửi email của Spring (giao diện bên thứ ba)           |
| `EmailQueueConsumer`        | Client    | Gọi `NotificationSender.send()`, không biết kênh cụ thể  |

#### 4.3.4. Class Diagram

> Xem sơ đồ: [`docs/diagrams/adapter-class.drawio`](diagrams/adapter-class.drawio)

#### 4.3.5. Sequence Diagram

> Xem sơ đồ: [`docs/diagrams/adapter-sequence.drawio`](diagrams/adapter-sequence.drawio)

#### 4.3.6. Mã nguồn minh chứng

**Target Interface — `NotificationSender.java`:**

```java
public interface NotificationSender {
    void send(String recipient, NotificationContent content);
}
```

**Adapter (SMTP) — `SmtpNotificationAdapter.java`:**

```java
@Component
@Profile("!demo")
@RequiredArgsConstructor
public class SmtpNotificationAdapter implements NotificationSender {
    private final JavaMailSender mailSender;

    @Override
    public void send(String recipient, NotificationContent content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setSubject(content.subject());
        message.setText(content.body());
        mailSender.send(message);
    }
}
```

**Client — `EmailQueueConsumer.java` (trích):**

```java
@RequiredArgsConstructor
public class EmailQueueConsumer {
    private final NotificationCreatorRegistry creators;
    private final NotificationSender sender;  // interface, không phụ thuộc JavaMail

    public void consume(EmailNotificationMessage notification) {
        NotificationContent content = creators.get(notification.type()).createMessage(notification);
        sender.send(notification.recipientEmail(), content);
    }
}
```

#### 4.3.7. Ưu điểm và nhược điểm

| Ưu điểm                                                        | Nhược điểm                        |
|-----------------------------------------------------------------|------------------------------------|
| Consumer không cần biết chi tiết API JavaMail                   | Thêm lớp trung gian                |
| Dễ thêm kênh gửi mới (SMS, push notification) bằng adapter mới | Mỗi kênh cần một adapter riêng     |
| Chuyển đổi kênh bằng Spring Profile, không sửa code            |                                    |

#### 4.3.8. Đánh giá tuân thủ SOLID

- **DIP**: `EmailQueueConsumer` phụ thuộc abstraction `NotificationSender`, không phụ thuộc `JavaMailSender`.
- **LSP**: `SmtpNotificationAdapter` và `DemoNotificationAdapter` thay thế nhau mà consumer hoạt động bình thường.
- **OCP**: Thêm kênh gửi mới (ví dụ: SMS) chỉ cần tạo adapter mới, không sửa consumer.

---

### 4.4. Facade Pattern (Structural)

#### 4.4.1. Vấn đề thực tế

Quy trình đặt lịch khám và hủy lịch hẹn liên quan đến nhiều service con: kiểm tra lịch trống (`DoctorScheduleService`), tạo/hủy lịch hẹn (`BookingService`), gửi thông báo (`AsyncEmailNotificationService`), xóa cache (`RedisCacheService`). Controller phải biết và gọi trực tiếp nhiều service, tạo coupling cao.

#### 4.4.2. Lý do chọn Facade

Facade cung cấp giao diện đơn giản (chỉ 2 phương thức) cho quy trình đặt/hủy lịch, ẩn đi sự phức tạp của hệ thống con bên dưới. Controller chỉ cần biết Facade, không cần biết chi tiết các service con.

#### 4.4.3. Các lớp và trách nhiệm

| Lớp                             | Vai trò           | Trách nhiệm                                              |
|----------------------------------|--------------------|----------------------------------------------------------|
| `AppointmentBookingFacade`       | Facade Interface   | 2 phương thức: `bookAppointment()`, `cancelAppointment()` |
| `AppointmentBookingFacadeImpl`   | Concrete Facade    | Ủy quyền cho BookingService và các service con            |
| `BookingService`                 | Subsystem          | Logic đặt/hủy lịch, giao dịch, khóa bi quan             |
| `DoctorScheduleService`          | Subsystem          | Quản lý ca khám, kiểm tra lịch trống                     |
| `BookingController`              | Client             | Gọi Facade thay vì gọi trực tiếp BookingService          |

#### 4.4.4. Class Diagram

> Xem sơ đồ: [`docs/diagrams/facade-class.drawio`](diagrams/facade-class.drawio)

#### 4.4.5. Sequence Diagram

> Xem sơ đồ: [`docs/diagrams/facade-sequence.drawio`](diagrams/facade-sequence.drawio)

#### 4.4.6. Mã nguồn minh chứng

**Facade Interface — `AppointmentBookingFacade.java`:**

```java
public interface AppointmentBookingFacade {
    AppointmentDto bookAppointment(Long patientUserId, CreateBookingDto request);
    void cancelAppointment(Long patientUserId, Long appointmentId);
}
```

**Concrete Facade — `AppointmentBookingFacadeImpl.java`:**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentBookingFacadeImpl implements AppointmentBookingFacade {
    private final BookingService bookingService;
    private final DoctorScheduleService doctorScheduleService;

    @Override
    public AppointmentDto bookAppointment(Long patientUserId, CreateBookingDto request) {
        log.info("Facade: bắt đầu quy trình đặt lịch cho bệnh nhân {}", patientUserId);
        return bookingService.createBooking(patientUserId, request);
    }

    @Override
    public void cancelAppointment(Long patientUserId, Long appointmentId) {
        log.info("Facade: bắt đầu quy trình hủy lịch hẹn {} cho bệnh nhân {}",
                appointmentId, patientUserId);
        bookingService.cancelBooking(appointmentId, patientUserId);
    }
}
```

**Client — `BookingController.java` (trích):**

```java
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class BookingController {
    private final AppointmentBookingFacade bookingFacade;
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<ApiResponseDto<AppointmentDto>> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateBookingDto request) {
        Long patientUserId = currentUserService.getUserId(userDetails);
        AppointmentDto appointment = bookingFacade.bookAppointment(patientUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(appointment, "Đặt lịch khám thành công"));
    }
}
```

#### 4.4.7. Ưu điểm và nhược điểm

| Ưu điểm                                                   | Nhược điểm                                              |
|------------------------------------------------------------|----------------------------------------------------------|
| Controller đơn giản, chỉ cần biết Facade                  | Thêm lớp trung gian                                     |
| Dễ thay đổi luồng nghiệp vụ bên trong mà không ảnh hưởng Controller | Facade có nguy cơ trở thành "God object" nếu mở rộng quá mức |
| Dễ kiểm thử: mock Facade khi test Controller               |                                                          |

#### 4.4.8. Đánh giá tuân thủ SOLID

- **SRP**: Facade chỉ chịu trách nhiệm điều phối, không chứa logic nghiệp vụ chi tiết.
- **DIP**: Controller phụ thuộc interface `AppointmentBookingFacade`, không phụ thuộc class cụ thể.

---

### 4.5. Strategy Pattern (Behavioral)

#### 4.5.1. Vấn đề thực tế

Trang tìm kiếm bác sĩ cần hỗ trợ nhiều cách sắp xếp: theo tên (mặc định), theo kinh nghiệm giảm dần, theo phí khám tăng/giảm. Nếu dùng `if-else` hoặc `switch-case` trong service, mỗi lần thêm cách sắp xếp mới phải sửa service.

#### 4.5.2. Lý do chọn Strategy

Strategy cho phép định nghĩa một họ thuật toán sắp xếp, đóng gói mỗi thuật toán thành class riêng và cho phép chuyển đổi thuật toán tại runtime dựa trên tham số người dùng.

#### 4.5.3. Các lớp và trách nhiệm

| Lớp                         | Vai trò            | Trách nhiệm                                    |
|------------------------------|---------------------|-------------------------------------------------|
| `DoctorSortStrategy`        | Strategy Interface  | Định nghĩa `key()` và `comparator()`            |
| `NameSortStrategy`          | Concrete Strategy   | Sắp xếp theo tên (case-insensitive, mặc định)  |
| `ExperienceSortStrategy`    | Concrete Strategy   | Sắp xếp theo kinh nghiệm giảm dần              |
| `FeeAscendingSortStrategy`  | Concrete Strategy   | Sắp xếp theo phí khám tăng dần                  |
| `FeeDescendingSortStrategy` | Concrete Strategy   | Sắp xếp theo phí khám giảm dần                  |
| `DoctorSortRegistry`        | Context / Registry  | Auto-discover strategies, cung cấp `get(key)`   |
| `DoctorServiceImpl`         | Client              | Gọi `registry.get(sort).comparator()` để sắp xếp |

#### 4.5.4. Class Diagram

> Xem sơ đồ: [`docs/diagrams/strategy-class.drawio`](diagrams/strategy-class.drawio)

#### 4.5.5. Sequence Diagram

> Xem sơ đồ: [`docs/diagrams/strategy-sequence.drawio`](diagrams/strategy-sequence.drawio)

#### 4.5.6. Mã nguồn minh chứng

**Strategy Interface — `DoctorSortStrategy.java`:**

```java
public interface DoctorSortStrategy {
    String key();
    Comparator<DoctorDto> comparator();
}
```

**Concrete Strategy — `FeeAscendingSortStrategy.java`:**

```java
@Component
public class FeeAscendingSortStrategy implements DoctorSortStrategy {
    @Override public String key() { return "feeAsc"; }
    @Override public Comparator<DoctorDto> comparator() {
        return Comparator.comparing(DoctorDto::getConsultationFee);
    }
}
```

**Registry — `DoctorSortRegistry.java`:**

```java
@Component
public class DoctorSortRegistry {
    private final Map<String, DoctorSortStrategy> strategies;

    public DoctorSortRegistry(List<DoctorSortStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toUnmodifiableMap(
                DoctorSortStrategy::key, Function.identity()));
    }

    public DoctorSortStrategy get(String key) {
        String effectiveKey = key == null || key.isBlank() ? "name" : key;
        DoctorSortStrategy strategy = strategies.get(effectiveKey);
        if (strategy == null)
            throw new AppException("Cách sắp xếp không hỗ trợ: " + effectiveKey);
        return strategy;
    }
}
```

#### 4.5.7. Ưu điểm và nhược điểm

| Ưu điểm                                                      | Nhược điểm                                   |
|---------------------------------------------------------------|-----------------------------------------------|
| Thêm cách sắp xếp mới chỉ cần tạo class `@Component`         | Nhiều class hơn so với switch-case đơn giản   |
| Spring DI tự phát hiện strategy mới, không sửa registry       | Client cần biết key để chọn strategy          |
| Mỗi strategy kiểm thử độc lập                                 |                                               |
| Thuật toán mặc định khi key null/blank                         |                                               |

#### 4.5.8. Đánh giá tuân thủ SOLID

- **OCP**: Thêm cách sắp xếp mới (ví dụ: theo đánh giá) chỉ cần tạo class mới, không sửa registry hay service.
- **SRP**: Mỗi strategy đóng gói một thuật toán sắp xếp duy nhất.
- **DIP**: `DoctorServiceImpl` phụ thuộc interface `DoctorSortStrategy`, không phụ thuộc implementation cụ thể.

---

### 4.6. Observer Pattern (Behavioral)

#### 4.6.1. Vấn đề thực tế

Khi trạng thái lịch hẹn thay đổi (tạo mới, hủy), hệ thống cần thông báo cho nhiều thành phần: gửi email cho bệnh nhân, ghi log, cập nhật thống kê. Nếu `BookingServiceImpl` gọi trực tiếp từng dịch vụ thông báo, sẽ tạo coupling chặt và khó mở rộng.

#### 4.6.2. Lý do chọn Observer

Observer cho phép `BookingServiceImpl` chỉ phát sự kiện, các subscriber (observer) đăng ký nhận và xử lý sự kiện một cách độc lập. Thêm observer mới không sửa code publisher. Spring Framework hỗ trợ sẵn `ApplicationEventPublisher` và `@TransactionalEventListener`.

#### 4.6.3. Các lớp và trách nhiệm

| Lớp                                   | Vai trò   | Trách nhiệm                                                    |
|----------------------------------------|-----------|-----------------------------------------------------------------|
| `ApplicationEventPublisher`            | Subject   | Interface phát sự kiện (Spring Framework)                       |
| `AsyncEmailNotificationServiceImpl`    | Publisher | Tạo `EmailNotificationMessage` và phát `EmailNotificationRequestedEvent` |
| `EmailNotificationRequestedEvent`      | Event     | Java record bọc message, mang dữ liệu sự kiện                  |
| `EmailQueuePublisher`                  | Observer  | Production: nhận event AFTER_COMMIT, gửi message sang RabbitMQ   |
| `DemoNotificationObserver`             | Observer  | Demo: nhận event AFTER_COMMIT, gọi trực tiếp `EmailQueueConsumer` |

#### 4.6.4. Class Diagram

> Xem sơ đồ: [`docs/diagrams/observer-class.drawio`](diagrams/observer-class.drawio)

#### 4.6.5. Sequence Diagram

> Xem sơ đồ: [`docs/diagrams/observer-sequence.drawio`](diagrams/observer-sequence.drawio)

#### 4.6.6. Mã nguồn minh chứng

**Publisher — `AsyncEmailNotificationServiceImpl.java`:**

```java
@Service
@RequiredArgsConstructor
public class AsyncEmailNotificationServiceImpl implements AsyncEmailNotificationService {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void sendBookingConfirmation(Appointment appointment) {
        publish("BOOKING_CONFIRMED", appointment);
    }

    @Override
    public void sendCancellationNotice(Appointment appointment) {
        publish("BOOKING_CANCELLED", appointment);
    }

    private void publish(String type, Appointment appointment) {
        EmailNotificationMessage message = new EmailNotificationMessage(
                type,
                appointment.getPatient().getUser().getEmail(),
                appointment.getPatient().getFullName(),
                appointment.getBookingCode(),
                appointment.getDoctor().getFullName(),
                appointment.getSchedule().getWorkDate().toString(),
                appointment.getSchedule().getStartTime().toString(),
                appointment.getSchedule().getEndTime().toString(),
                appointment.getPrice() == null ? null
                        : appointment.getPrice().toPlainString());
        eventPublisher.publishEvent(new EmailNotificationRequestedEvent(message));
    }
}
```

**Observer (Demo) — `DemoNotificationObserver.java`:**

```java
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoNotificationObserver {
    private final EmailQueueConsumer consumer;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(EmailNotificationRequestedEvent event) {
        consumer.consume(event.message());
    }
}
```

#### 4.6.7. Ưu điểm và nhược điểm

| Ưu điểm                                                       | Nhược điểm                                              |
|----------------------------------------------------------------|----------------------------------------------------------|
| `BookingService` không biết và không phụ thuộc vào observer    | Sự kiện bất đồng bộ khó debug hơn gọi đồng bộ          |
| Dễ thêm observer mới (log, analytics) không sửa publisher     | Chưa có exactly-once guarantee                           |
| `AFTER_COMMIT` đảm bảo chỉ gửi thông báo khi giao dịch thành công | Không có transactional outbox, có thể mất sự kiện nếu crash |
| Hai observer chuyển đổi qua Spring Profile                      |                                                          |

#### 4.6.8. Đánh giá tuân thủ SOLID

- **SRP**: Publisher chỉ phát sự kiện, Observer chỉ xử lý sự kiện — hai trách nhiệm tách biệt.
- **OCP**: Thêm observer mới (ví dụ: ghi audit log) chỉ cần tạo listener mới, không sửa publisher.
- **DIP**: Publisher phụ thuộc `ApplicationEventPublisher` (abstraction), không phụ thuộc observer cụ thể.

---

## Chương 5: Cài đặt và minh chứng SOLID

### 5.1. Cấu trúc mã nguồn

```
backend/src/main/java/com/datlichkhambenh/backend/
├── config/              # Cấu hình: Security, Redis, RabbitMQ, Data seed
├── controller/          # REST Controllers (Tầng trình bày)
├── demo/                # Demo adapter và observer
├── dto/
│   ├── request/         # DTO nhận dữ liệu từ client
│   └── response/        # DTO trả kết quả cho client
├── entity/              # JPA Entities (Tầng truy cập dữ liệu)
├── exception/           # Custom exceptions
├── messaging/           # Event, Queue consumer/publisher (Observer)
├── pattern/
│   ├── facade/          # Facade Pattern
│   ├── notification/    # Factory Method + Adapter Patterns
│   └── sorting/         # Strategy Pattern
├── repository/          # Spring Data JPA Repositories
├── security/            # JWT Filter, Token Provider
└── service/
    ├── *.java           # Business Logic interfaces
    └── impl/            # Service implementations (Tầng nghiệp vụ)
```

### 5.2. SRP — Single Responsibility Principle

> *Mỗi lớp chỉ nên có duy nhất một lý do để thay đổi.*

Hệ thống tách biệt trách nhiệm rõ ràng:

- `BookingServiceImpl` — chỉ xử lý nghiệp vụ đặt/hủy/tra cứu lịch hẹn.
- `AsyncEmailNotificationServiceImpl` — chỉ chịu trách nhiệm phát sự kiện thông báo.
- `DoctorRepository` — chỉ truy cập dữ liệu bác sĩ.
- `CurrentUserService` — interface với đúng 1 phương thức.

**Minh chứng:**

```java
// CurrentUserService.java — Interface chỉ có 1 trách nhiệm
public interface CurrentUserService {
    Long getUserId(UserDetails userDetails);
}
```

```java
// AsyncEmailNotificationService.java — Tách riêng 3 loại thông báo
public interface AsyncEmailNotificationService {
    void sendBookingConfirmation(Appointment appointment);
    void sendAppointmentReminder(Appointment appointment);
    void sendCancellationNotice(Appointment appointment);
}
```

**Lợi ích:** Khi cần thay đổi cách gửi thông báo, chỉ sửa `AsyncEmailNotificationServiceImpl`, không ảnh hưởng `BookingServiceImpl`.

### 5.3. OCP — Open/Closed Principle

> *Các thực thể phần mềm nên mở cho mở rộng nhưng đóng cho sửa đổi.*

Hệ thống chứng minh OCP qua cơ chế registry của Strategy và Factory Method:

- Thêm cách sắp xếp bác sĩ mới: tạo class implements `DoctorSortStrategy` với `@Component` → `DoctorSortRegistry` tự phát hiện qua Spring DI.
- Thêm loại thông báo mới: tạo class extends `NotificationCreator` với `@Component` → `NotificationCreatorRegistry` tự phát hiện.

**Minh chứng — test chứng minh OCP:**

```java
@Test void newStrategyCanBeRegisteredWithoutEditingRegistry() {
    DoctorSortStrategy custom = new DoctorSortStrategy() {
        public String key() { return "reverseName"; }
        public Comparator<DoctorDto> comparator() {
            return new NameSortStrategy().comparator().reversed();
        }
    };
    assertSame(custom, new DoctorSortRegistry(List.of(custom)).get("reverseName"));
}
```

**Lợi ích:** Mở rộng chức năng mà không sửa code hiện có, giảm rủi ro regression.

### 5.4. LSP — Liskov Substitution Principle

> *Đối tượng lớp con phải thay thế được đối tượng lớp cha mà không phá vỡ tính đúng đắn.*

Hệ thống chứng minh LSP qua profile-based polymorphism:

- `NoOpRedisCacheService` và `RedisCacheServiceImpl` đều implement `RedisCacheService` — chuyển đổi qua Spring Profile mà `BookingServiceImpl` hoạt động bình thường.
- `SmtpNotificationAdapter` và `DemoNotificationAdapter` đều implement `NotificationSender` — `EmailQueueConsumer` xử lý đúng với cả hai.

**Lợi ích:** Demo và production dùng cùng code nghiệp vụ, chỉ khác hạ tầng bên dưới.

### 5.5. ISP — Interface Segregation Principle

> *Client không nên bị buộc phụ thuộc vào các interface mà nó không sử dụng.*

Hệ thống tách interface theo trách nhiệm, giữ số phương thức tối thiểu:

| Interface                      | Số phương thức | Mục đích                           |
|--------------------------------|----------------|-------------------------------------|
| `CurrentUserService`           | 1              | Lấy ID người dùng từ JWT           |
| `MedicalRecordService`         | 2              | Xem và cập nhật bệnh án            |
| `AsyncEmailNotificationService`| 3              | Gửi 3 loại thông báo email         |
| `PatientService`               | 3              | Quản lý hồ sơ bệnh nhân           |
| `RedisCacheService`            | 6              | Cache bác sĩ (3) + slot (3)        |
| `BookingService`               | 8              | Toàn bộ nghiệp vụ lịch hẹn        |

**Điểm cần cải thiện:** `AdminManagementService` với 17 phương thức gom nhiều trách nhiệm (duyệt bác sĩ, quản lý tài khoản, thống kê) — có thể tách thành các interface nhỏ hơn.

### 5.6. DIP — Dependency Inversion Principle

> *Module cấp cao không nên phụ thuộc vào module cấp thấp; cả hai nên phụ thuộc vào abstraction.*

Tất cả service implementation được inject qua interface:

**Minh chứng:**

```java
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final ScheduleRepository scheduleRepository;           // interface (Spring Data)
    private final AppointmentRepository appointmentRepository;     // interface (Spring Data)
    private final AsyncEmailNotificationService emailNotificationService; // interface
    private final RedisCacheService redisCacheService;             // interface
    // Spring DI inject implementation phù hợp theo profile
}
```

```java
@RequiredArgsConstructor
public class EmailQueueConsumer {
    private final NotificationCreatorRegistry creators;  // phụ thuộc registry, không phụ thuộc creator cụ thể
    private final NotificationSender sender;             // interface, không phụ thuộc JavaMailSender
}
```

**Lợi ích:** Thay đổi implementation (ví dụ: chuyển từ SMTP sang SendGrid) không ảnh hưởng code nghiệp vụ.

---

## Chương 6: Kiểm thử và đánh giá

### 6.1. Bảng Test Cases TC01–TC12

| Mã   | Trường hợp kiểm thử                    | Kết quả mong đợi                          | Test class                                                                     | Kết quả  |
|------|----------------------------------------|-------------------------------------------|--------------------------------------------------------------------------------|----------|
| TC01 | Đặt lịch ở khung giờ còn trống         | Tạo lịch thành công, status=CONFIRMED     | `BookingFlowIntegrationTests.bookingCommitsThenObserverQueuesImmutableNotification` | ✅ Pass  |
| TC02 | Đặt lịch ở khung giờ đã kín            | Từ chối, ném `BookingConflictException`   | `BookingFlowIntegrationTests.concurrentBookingsHaveExactlyOneWinner`             | ✅ Pass  |
| TC03 | Thiếu thông tin bệnh nhân              | Ném `ResourceNotFoundException`            | `BookingServiceImplTests.bookingWithNonexistentPatientThrowsResourceNotFound`    | ✅ Pass  |
| TC04 | Hủy một lịch hẹn hợp lệ               | Status=CANCELLED, tạo slot thay thế       | `BookingFlowIntegrationTests.cancelPreservesOldAppointmentAndReplacementCanBeBooked` | ✅ Pass |
| TC05 | Factory tạo nội dung thông báo đúng loại | `ConfirmationContent` cho BOOKING_CONFIRMED | `DesignPatternTests.factoryMethodCreatesThreeDifferentMessagesAndRejectsUnknownType` | ✅ Pass |
| TC06 | Tạo đối tượng bằng Builder              | Các thuộc tính thiết lập chính xác        | `DesignPatternTests.builderAssemblesBookingWithoutPositionalConstructor`          | ✅ Pass  |
| TC07 | Gửi thông báo qua Adapter               | Adapter dịch đúng sang `SimpleMailMessage` | `DesignPatternTests.adapterTranslatesDomainContentToJavaMail`                    | ✅ Pass  |
| TC08 | Đặt lịch qua Facade                    | Facade ủy quyền đúng cho BookingService   | `FacadePatternTests.facadeDelegatesToBookingServiceForCreate`                     | ✅ Pass  |
| TC09 | Thay đổi cách sắp xếp bác sĩ           | Strategy trả comparator tương ứng         | `DesignPatternTests.strategiesChangeOrderWithoutChangingSearchService`            | ✅ Pass  |
| TC10 | Tạo lịch → Observer nhận event           | RabbitMQ nhận event AFTER_COMMIT          | `BookingFlowIntegrationTests.bookingCommitsThenObserverQueuesImmutableNotification` | ✅ Pass |
| TC11 | Hủy rồi đặt lại cùng khung giờ         | Đặt lại thành công với slot thay thế      | `BookingFlowIntegrationTests.cancelPreservesOldAppointmentAndReplacementCanBeBooked` | ✅ Pass |
| TC12 | Hai yêu cầu đặt cùng khung giờ đồng thời | Chỉ 1 thành công (Pessimistic Lock)     | `BookingFlowIntegrationTests.concurrentBookingsHaveExactlyOneWinner`             | ✅ Pass  |

### 6.2. Kết quả chạy thử

```
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

Tổng cộng 35 test methods thuộc 8 test class:

| Test class                         | Loại         | Số test | Mô tả                                        |
|------------------------------------|--------------|---------|------------------------------------------------|
| `DesignPatternTests`               | Unit         | 6       | Builder, Factory Method, Adapter, Strategy     |
| `FacadePatternTests`               | Unit         | 3       | Facade delegation, interface simplification    |
| `BookingServiceImplTests`          | Unit (Mockito) | 4     | Empty history, access control, cancel, TC03    |
| `AuthServiceImplTests`             | Unit (Mockito) | 2     | Pending doctor login, token separation         |
| `OwnershipServiceTests`            | Unit (Mockito) | 3     | Cross-doctor, medical record authorization     |
| `TruthfulDataTests`               | Unit (Mockito) | 7     | Analytics, stats, medical records              |
| `BookingFlowIntegrationTests`      | Integration  | 5       | Full booking flow, concurrency, cancel-rebook  |
| `AuthSecurityIntegrationTests`     | Integration  | 4       | Role injection, auth enforcement, public API   |
| `BackendApplicationTests`          | Integration  | 1       | Spring context load                            |

### 6.3. So sánh có và không sử dụng Design Patterns

| Tiêu chí                        | Không dùng Pattern                     | Có dùng Pattern                               |
|----------------------------------|----------------------------------------|------------------------------------------------|
| Thêm loại thông báo mới         | Sửa if-else trong consumer             | Tạo Creator + Product class mới, `@Component`  |
| Thêm cách sắp xếp bác sĩ       | Sửa switch-case trong service          | Tạo Strategy class mới, `@Component`            |
| Thay đổi kênh gửi thư           | Sửa code gửi thư trực tiếp trong service | Tạo Adapter mới, đổi Spring Profile           |
| Tạo đối tượng phức tạp          | Constructor 8+ tham số, dễ nhầm        | Builder fluent API, từng bước rõ ràng           |
| Thêm bên nhận thông báo mới     | Sửa code publisher                     | Tạo listener mới, Spring tự đăng ký            |
| Giảm coupling Controller-Service | Controller gọi trực tiếp nhiều service | Facade cung cấp 1 entry point đơn giản          |

### 6.4. Đánh giá tổng thể

**Khả năng mở rộng**: Tốt — OCP được chứng minh qua cơ chế registry tự phát hiện (auto-discovery) của Strategy và Factory Method. Thêm chức năng mới chỉ cần tạo class mới, không sửa code hiện có.

**Khả năng bảo trì**: Tốt — SRP rõ ràng, mỗi class có trách nhiệm đơn nhất. Code tổ chức theo kiến trúc 3 tầng với package rõ ràng.

**Hiệu năng**: Chấp nhận được cho phạm vi demo. Phân trang danh sách bác sĩ đang thực hiện in-memory với O(N log N), cần chuyển sang SQL pagination (Spring Data `Pageable`) cho production. Cache Redis giúp giảm tải database cho các truy vấn lặp.

**Hạn chế**:

- Chưa có transactional outbox: có thể mất thông báo nếu ứng dụng crash sau DB commit nhưng trước khi publish event.
- Phân trang bác sĩ in-memory: hiệu năng giảm khi số lượng bác sĩ lớn.
- Chưa có audit đầy đủ cho các thao tác nhạy cảm.
- H2 demo không thay thế bằng chứng tranh chấp trên MySQL/InnoDB.

---

## Chương 7: Kết luận

### 7.1. Kết quả đạt được

- Xây dựng hệ thống đặt lịch khám bệnh hoàn chỉnh sử dụng Spring Boot 3.3, Java 21.
- Áp dụng thành công **6 Design Patterns** thuộc đủ **3 nhóm**:
  - Creational: Builder, Factory Method
  - Structural: Adapter, Facade
  - Behavioral: Strategy, Observer
- Chứng minh **5 nguyên lý SOLID** bằng mã nguồn thực tế kèm giải thích.
- Thiết kế **12 sơ đồ UML** (6 Class Diagram + 6 Sequence Diagram) cho từng pattern.
- Xây dựng **35 test case** bao gồm unit test và integration test, tất cả pass.
- Hệ thống demo chạy không cần Docker, sử dụng H2 database và inbox giả lập.

### 7.2. Hạn chế

- Chưa có hệ thống thanh toán (payment/receipt).
- Chưa có transactional outbox pattern đảm bảo gửi thông báo atomically.
- Phân trang bác sĩ thực hiện in-memory, chưa phân trang SQL.
- Chưa có scheduler tự động gửi nhắc lịch hẹn.
- `AdminManagementService` (17 methods) chưa tách nhỏ đủ theo ISP.
- Bằng chứng concurrency chạy trên H2, cần kiểm chứng thêm trên MySQL/InnoDB.

### 7.3. Hướng phát triển

- Tích hợp thanh toán trực tuyến (VNPay, MoMo).
- Bổ sung transactional outbox pattern cho thông báo tin cậy.
- Chuyển sang phân trang SQL sử dụng Spring Data `Pageable`.
- Thêm scheduler tự động gửi nhắc lịch hẹn trước ngày khám.
- Tách `AdminManagementService` thành các interface nhỏ hơn theo ISP.
- Triển khai Docker Compose cho môi trường staging/production.
- Bổ sung thêm kênh thông báo (SMS, push notification) qua Adapter pattern.

---

## Tài liệu tham khảo

1. Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides, *"Design Patterns: Elements of Reusable Object-Oriented Software"*, Addison-Wesley, 1994.
2. Robert C. Martin, *"Clean Architecture: A Craftsman's Guide to Software Structure and Design"*, Prentice Hall, 2017.
3. Robert C. Martin, *"Agile Software Development, Principles, Patterns, and Practices"*, Pearson, 2002.
4. Spring Framework Reference Documentation, https://docs.spring.io/spring-framework/reference/
5. Spring Boot Reference Documentation, https://docs.spring.io/spring-boot/reference/

---

## Phụ lục: Bảng tổng hợp Design Patterns

| #  | Pattern        | Nhóm       | Package                   | Lớp chính                                  | Test class            |
|----|----------------|------------|---------------------------|---------------------------------------------|-----------------------|
| 1  | Builder        | Creational | `entity`                  | `Appointment.AppointmentBuilder`            | `DesignPatternTests`  |
| 2  | Factory Method | Creational | `pattern.notification`    | `NotificationCreator`, `NotificationCreatorRegistry` | `DesignPatternTests` |
| 3  | Adapter        | Structural | `pattern.notification`    | `NotificationSender`, `SmtpNotificationAdapter` | `DesignPatternTests` |
| 4  | Facade         | Structural | `pattern.facade`          | `AppointmentBookingFacade`, `AppointmentBookingFacadeImpl` | `FacadePatternTests` |
| 5  | Strategy       | Behavioral | `pattern.sorting`         | `DoctorSortStrategy`, `DoctorSortRegistry`   | `DesignPatternTests` |
| 6  | Observer       | Behavioral | `messaging`, `demo`       | `EmailNotificationRequestedEvent`, `DemoNotificationObserver` | `BookingFlowIntegrationTests` |
