const user = Auth.checkAuth(['PATIENT']);
if (user) {
  document.getElementById('user-name').innerText = user.email.split('@')[0];
  document.getElementById('user-avatar').innerText = user.email.charAt(0).toUpperCase();
}

let allDoctors = [];
let doctorPage = 0, doctorsRequestId = 0;
let availableSlots = [];
let bookingReviewed = false;
let bookingPending = false;
let slotRequestId = 0;
let clinicInfo = null;
let specialtiesList = [];
let bookingBsModal = null;
let successBsModal = null;
let detailBsModal = null;

const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
document.getElementById('booking-date').value = Utils.localDate(tomorrow);

document.addEventListener('DOMContentLoaded', () => {
  bookingBsModal = new bootstrap.Modal(document.getElementById('booking-modal'));
  successBsModal = new bootstrap.Modal(document.getElementById('success-modal'));
  detailBsModal = new bootstrap.Modal(document.getElementById('doc-detail-modal'));
  init();
});

async function init() {
  try {
    const specRes = await Api.doctors.specialties();
    specialtiesList = specRes.data || [];

    const specSelect = document.getElementById('specialty-select');
    const specPills = document.getElementById('specialty-pills');

    specialtiesList.forEach(s => {
      specSelect.innerHTML += `<option value="${s.id}">${Utils.escapeHtml(s.name)}</option>`;
      specPills.innerHTML += `
        <span class="spec-pill" data-ui-click="action60" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(s.id))}">
          <i class="bi bi-hospital me-1"></i> ${Utils.escapeHtml(s.name)}
        </span>
      `;
    });
  } catch (e) {}

  loadDoctors('');
}

function onPillClick(specId, element) {
  document.querySelectorAll('.spec-pill').forEach(p => p.classList.remove('active'));
  element.classList.add('active');
  document.getElementById('specialty-select').value = specId;
  loadDoctors(specId);
}

const _skeletonCard = `<div class="col-md-6 col-lg-4"><div class="card p-4 h-100"><div class="skeleton" style="height:80px;width:80px;border-radius:50%;margin:0 auto 12px"></div><div class="skeleton mx-auto" style="height:20px;width:60%;margin-bottom:8px"></div><div class="skeleton mx-auto" style="height:16px;width:40%;margin-bottom:16px"></div><div class="skeleton" style="height:14px;width:80%;margin-bottom:6px"></div><div class="skeleton" style="height:14px;width:70%"></div></div></div>`;

async function loadDoctors(specialtyId, keepPage = false) {
  if (!keepPage) doctorPage = 0;
  specialtyId = specialtyId ?? document.getElementById('specialty-select').value;
  const requestId = ++doctorsRequestId;
  const grid = document.getElementById('doctor-grid');
  grid.innerHTML = _skeletonCard.repeat(6);

  try {
    const params = new URLSearchParams({sort: document.getElementById('doctor-sort').value, size: '20', page: String(doctorPage)});
    if (specialtyId) params.set('specialtyId', specialtyId);
    const keyword = document.getElementById('doctor-search-input').value.trim();
    if (keyword) params.set('keyword', keyword);
    const res = await Api.get(`/doctors?${params}`);
    if (requestId !== doctorsRequestId) return;
    allDoctors = res.data || [];
    document.getElementById('doctors-prev').disabled = doctorPage === 0;
    document.getElementById('doctors-next').disabled = allDoctors.length < 20;
    document.getElementById('doctors-page').textContent = 'Trang ' + (doctorPage + 1);
    filterDoctors();
  } catch (err) {
    if (requestId !== doctorsRequestId) return;
    grid.innerHTML = `<div class="col-12"><div class="alert alert-danger shadow-sm">${Utils.escapeHtml(err.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action73">Thử lại</button></div></div>`;
  }
}

function renderDoctors(doctors) {
  const grid = document.getElementById('doctor-grid');
  if (doctors.length === 0) {
    grid.innerHTML = `
      <div class="col-12 text-center py-5">
        <i class="bi bi-person-x display-1 text-muted"></i>
        <h5 class="fw-bold mt-3 text-secondary">Không tìm thấy bác sĩ phù hợp</h5>
        <p class="text-muted">Vui lòng thử lại với từ khóa hoặc chuyên khoa khác.</p>
      </div>
    `;
    return;
  }

  grid.innerHTML = doctors.map(d => `
    <div class="col-md-6 col-lg-4">
      <div class="card doctor-card h-100 p-4 shadow-sm text-center">
        <div class="avatar-circle">
          <i class="bi bi-person-fill"></i>
        </div>

        <h5 class="fw-bold text-dark mb-1">${Utils.escapeHtml(d.fullName)}</h5>
        <div class="mb-3">
          <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fw-semibold">
            ${Utils.escapeHtml(d.specialtyName || 'Chuyên khoa tổng quát')}
          </span>
        </div>

        <div class="d-flex justify-content-center gap-2 mb-3">
          <div class="stat-badge small text-muted"><i class="bi bi-mortarboard-fill text-warning me-1"></i> ${Utils.escapeHtml(d.degree || 'Bác sĩ')}</div>
          <div class="stat-badge small text-muted"><i class="bi bi-award-fill text-info me-1"></i> ${Utils.escapeHtml(d.experienceYears)} năm KN</div>
        </div>

        <div class="mt-auto pt-3 border-top d-flex align-items-center justify-content-between">
          <div class="text-start">
            <small class="text-muted d-block">Giá khám:</small>
            <span class="fw-bold text-success fs-5">${Utils.formatCurrency(d.consultationFee)}</span>
          </div>
          <div class="d-flex gap-2">
            <button class="btn btn-outline-info btn-sm rounded-circle" title="Xem thông tin chi tiết" data-ui-click="action61" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(d.userId))}">
              <i class="bi bi-info-circle fs-6"></i>
            </button>
            <button class="btn btn-primary rounded-pill px-3 fw-semibold shadow-sm" data-ui-click="action62" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(d.userId))}">
              <i class="bi bi-calendar-plus me-1"></i> Đặt Lịch
            </button>
          </div>
        </div>
      </div>
    </div>
  `).join('');
}

function bookDoctor(docId) {
  const doc = allDoctors.find(d => d.userId === docId);
  if (doc) openBookingModal(doc.userId, doc.fullName, doc.specialtyName || 'Chuyên khoa');
}

function viewDoctorDetail(docId) {
  const doc = allDoctors.find(d => d.userId === docId);
  if (!doc) return;

  document.getElementById('doc-detail-body').innerHTML = `
    <div class="text-center mb-4">
      <div class="avatar-circle shadow-sm mb-2" style="width:90px; height:90px; font-size:2.5rem;">
        <i class="bi bi-person-fill"></i>
      </div>
      <h4 class="fw-bold text-dark mb-1">${Utils.escapeHtml(doc.fullName)}</h4>
      <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fs-6">
        ${Utils.escapeHtml(doc.specialtyName || 'Chuyên khoa')}
      </span>
    </div>

    <div class="row g-3">
      <div class="col-md-6">
        <div class="bg-light p-3 rounded-3">
          <small class="text-muted d-block mb-1"><i class="bi bi-mortarboard me-1"></i>Bằng cấp / Học vị:</small>
          <strong class="text-dark">${Utils.escapeHtml(doc.degree || 'Bác sĩ chuyên khoa')}</strong>
        </div>
      </div>
      <div class="col-md-6">
        <div class="bg-light p-3 rounded-3">
          <small class="text-muted d-block mb-1"><i class="bi bi-award me-1"></i>Kinh nghiệm:</small>
          <strong class="text-dark">${Utils.escapeHtml(doc.experienceYears)} năm kinh nghiệm</strong>
        </div>
      </div>
      <div class="col-md-6">
        <div class="bg-light p-3 rounded-3">
          <small class="text-muted d-block mb-1"><i class="bi bi-telephone me-1"></i>Điện thoại liên hệ:</small>
          <strong class="text-dark">${Utils.escapeHtml(doc.phone || 'Chưa cung cấp')}</strong>
        </div>
      </div>
      <div class="col-md-6">
        <div class="bg-light p-3 rounded-3">
          <small class="text-muted d-block mb-1"><i class="bi bi-cash-stack me-1"></i>Chi phí tư vấn & khám:</small>
          <strong class="text-success fs-5">${Utils.formatCurrency(doc.consultationFee)}</strong>
        </div>
      </div>
    </div>

    <div class="mt-4 p-3 bg-primary-subtle rounded-3 text-primary border border-primary-subtle">
      <h6 class="fw-bold mb-1"><i class="bi bi-shield-check me-1"></i>Cam kết chất lượng dịch vụ</h6>
      <small>Tài khoản bác sĩ đã được Admin phê duyệt trên hệ thống.</small>
    </div>
  `;

  document.getElementById('btn-modal-start-booking').onclick = () => {
    detailBsModal.hide();
    openBookingModal(doc.userId, doc.fullName, doc.specialtyName || 'Chuyên khoa');
  };

  detailBsModal.show();
}

function filterDoctors() {
  const search = document.getElementById('doctor-search-input').value.toLowerCase().trim();
  let filtered = allDoctors;
  if (search) {
    filtered = filtered.filter(d =>
      (d.fullName && d.fullName.toLowerCase().includes(search)) ||
      (d.degree && d.degree.toLowerCase().includes(search))
    );
  }
  renderDoctors(filtered);
}

const debouncedFilter = Utils.debounce(() => loadDoctors(), 300);
async function changeDoctorPage(offset) {
  if (doctorPage + offset < 0) return;
  doctorPage += offset; await loadDoctors(undefined, true);
}

function onSpecialtyChange(val) {
  loadDoctors(val);
}

function resetFilters() {
  document.getElementById('doctor-search-input').value = '';
  document.getElementById('specialty-select').value = '';
  document.getElementById('doctor-sort').value = 'name';
  document.querySelectorAll('.spec-pill').forEach(p => p.classList.remove('active'));
  if (document.querySelectorAll('.spec-pill')[0]) document.querySelectorAll('.spec-pill')[0].classList.add('active');
  loadDoctors('');
}

function openBookingModal(docId, docName, specName) {
  document.getElementById('selected-doctor-id').value = docId;
  document.getElementById('modal-doc-name').innerText = `Đặt lịch khám với ${docName}`;
  document.getElementById('modal-doc-detail-name').innerText = docName;
  document.getElementById('modal-doc-detail-spec').innerText = specName;

  editBooking();
  bookingBsModal.show();
  loadSlots();
}

async function loadSlots() {
  const docId = document.getElementById('selected-doctor-id').value;
  const date = document.getElementById('booking-date').value;
  const container = document.getElementById('slots-container');
  if (!date || !docId) return;
  const requestId = ++slotRequestId;
  editBooking();
  availableSlots = [];
  document.getElementById('btn-review-booking').disabled = true;
  document.getElementById('selected-schedule-id').value = '';
  document.getElementById('btn-confirm-booking').disabled = true;

  container.innerHTML = `
    <div class="d-flex align-items-center gap-2 text-primary py-2">
      <div class="spinner-border spinner-border-sm" role="status"></div>
      <span class="small fw-medium">Đang tải danh sách khung giờ rảnh...</span>
    </div>
  `;

  try {
    const res = await Api.doctors.slots(docId, date);
    if (requestId !== slotRequestId) return;
    const slots = res.data || [];
    availableSlots = slots;

    if (slots.length === 0) {
      container.innerHTML = '<p class="text-muted small my-2"><i class="bi bi-info-circle me-1"></i>Bác sĩ chưa có lịch rảnh vào ngày này. Vui lòng chọn ngày khác.</p>';
      return;
    }

    container.innerHTML = slots.map(s => {
      if (!s.isAvailable) {
        return `
          <button type="button" class="btn btn-light text-muted opacity-50 border rounded-3 px-3 py-2 slot-btn unavailable" disabled>
            <i class="bi bi-lock-fill me-1"></i>${Utils.formatTime(s.startTime)} - ${Utils.formatTime(s.endTime)}
          </button>
        `;
      }
      return `
        <button type="button" class="btn btn-outline-success rounded-3 px-3 py-2 fw-semibold slot-btn"
          data-ui-click="action63" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(s.id))}">
          <i class="bi bi-clock me-1"></i>${Utils.formatTime(s.startTime)} - ${Utils.formatTime(s.endTime)}
        </button>
      `;
    }).join('');
  } catch (err) {
    if (requestId !== slotRequestId) return;
    container.innerHTML = `<div class="alert alert-danger py-2 px-3 small mb-0">${Utils.escapeHtml(err.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action8">Thử lại</button></div>`;
  }
}

function selectSlot(btn, scheduleId) {
  document.querySelectorAll('.slot-btn').forEach(b => {
    if (!b.classList.contains('unavailable')) {
      b.classList.remove('selected', 'btn-success');
      b.classList.add('btn-outline-success');
    }
  });

  btn.classList.remove('btn-outline-success');
  btn.classList.add('selected', 'btn-success');
  document.getElementById('selected-schedule-id').value = scheduleId;
  document.getElementById('btn-review-booking').disabled = false;
}

async function submitBooking(e) {
  e.preventDefault();
  const scheduleId = document.getElementById('selected-schedule-id').value;
  const symptoms = document.getElementById('symptoms').value;
  const btn = document.getElementById('btn-confirm-booking');
  const docName = document.getElementById('modal-doc-detail-name').innerText;
  const dateVal = document.getElementById('booking-date').value;

  if (!scheduleId || !bookingReviewed || bookingPending) return;
  bookingPending = true;
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý đặt lịch...';

  try {
    const res = await Api.appointments.create({
      scheduleId: parseInt(scheduleId),
      symptoms: symptoms
    });

    bookingReviewed = false;
    bookingBsModal.hide();
    document.getElementById('symptoms').value = '';
    Utils.showToast('Đặt lịch khám thành công!', 'success');

    const createdApp = res.data || {};
    document.getElementById('success-booking-code').innerText = createdApp.bookingCode || 'BK-SUCCESS';
    document.getElementById('success-doctor-name').innerText = docName;
    document.getElementById('success-date-time').innerText = `${Utils.formatDate(dateVal)} ${Utils.formatTime(createdApp.startTime)} – ${Utils.formatTime(createdApp.endTime)}`;
    successBsModal.show();

  } catch (err) {
    const conflict = err.status === 409 || /đã được đặt|không còn trống|409|conflict/i.test(err.message);
    if (conflict) {
      await loadSlots();
      const feedback = document.getElementById('booking-feedback');
      feedback.textContent = 'Ca khám vừa được người khác chọn. Triệu chứng đã được giữ lại; vui lòng chọn ca khác và kiểm tra lại.';
      feedback.classList.remove('hidden');
    } else {
      const feedback = document.getElementById('booking-feedback');
      feedback.textContent = err.message || 'Không thể đặt lịch. Vui lòng thử lại.';
      feedback.classList.remove('hidden');
    }
  } finally {
    bookingPending = false;
    btn.disabled = !bookingReviewed;
    btn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Xác Nhận Đặt Lịch';
  }
}
document.getElementById('booking-form').addEventListener('submit', submitBooking);

function editBooking() {
  bookingReviewed = false;
  document.getElementById('booking-selection').classList.remove('hidden');
  document.getElementById('booking-review').classList.add('hidden');
  document.getElementById('btn-confirm-booking').classList.add('hidden');
  document.getElementById('btn-confirm-booking').disabled = true;
  document.getElementById('btn-review-booking').classList.remove('hidden');
  document.getElementById('btn-edit-booking').classList.add('hidden');
}
async function reviewBooking() {
  const doc = allDoctors.find(d => d.userId === Number(document.getElementById('selected-doctor-id').value));
  const slot = availableSlots.find(s => s.id === Number(document.getElementById('selected-schedule-id').value));
  if (!doc || !slot || bookingPending) return;
  const feedback = document.getElementById('booking-feedback');
  try {
    clinicInfo = (await Api.get('/clinic-info', false)).data || {};
  } catch (error) {
    feedback.textContent = 'Không thể tải thông tin phòng khám: ' + error.message + '. Vui lòng thử lại trước khi xác nhận.';
    feedback.classList.remove('hidden');
    return;
  }
  document.getElementById('booking-review').innerHTML = `
    <h6 class="fw-bold">3. Kiểm tra trước khi xác nhận</h6>
    <dl class="booking-summary"><dt>Bác sĩ</dt><dd>${Utils.escapeHtml(doc.fullName)}</dd>
    <dt>Ngày, giờ khám</dt><dd>${Utils.formatDate(document.getElementById('booking-date').value)} ${Utils.formatTime(slot.startTime)} – ${Utils.formatTime(slot.endTime)}</dd>
    <dt>Phí khám</dt><dd>${Utils.formatCurrency(doc.consultationFee)}</dd>
    <dt>Nơi khám</dt><dd>${Utils.escapeHtml(clinicInfo.name || 'Phòng khám')} — ${Utils.escapeHtml(clinicInfo.address || 'Chưa cấu hình địa chỉ. Vui lòng liên hệ phòng khám trước khi đến.')}</dd>
    <dt>Triệu chứng</dt><dd>${Utils.escapeHtml(document.getElementById('symptoms').value || 'Không ghi nhận')}</dd></dl>
    <p class="alert alert-info">Chỉ hủy lịch khi còn hơn ${Number(clinicInfo.cancellationHours) || 24} giờ trước giờ khám. Phí hiển thị là phí khám, chưa phải thanh toán.</p>`;
  bookingReviewed = true;
  feedback.classList.add('hidden');
  document.getElementById('booking-selection').classList.add('hidden');
  document.getElementById('booking-review').classList.remove('hidden');
  document.getElementById('btn-confirm-booking').classList.remove('hidden');
  document.getElementById('btn-confirm-booking').disabled = false;
  document.getElementById('btn-review-booking').classList.add('hidden');
  document.getElementById('btn-edit-booking').classList.remove('hidden');
  document.getElementById('btn-confirm-booking').focus();
}
