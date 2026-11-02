const user = Auth.checkAuth(['PATIENT']);
if (user) {
  document.getElementById('user-name').innerText = user.email.split('@')[0];
  document.getElementById('user-avatar').innerText = user.email.charAt(0).toUpperCase();
}

let allDoctors = [];
let specialtiesList = [];
let bookingBsModal = null;
let successBsModal = null;
let detailBsModal = null;

const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
document.getElementById('booking-date').value = tomorrow.toISOString().split('T')[0];

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
      specSelect.innerHTML += `<option value="${s.id}">${s.name}</option>`;
      specPills.innerHTML += `
        <span class="spec-pill" onclick="onPillClick(${s.id}, this)">
          <i class="bi bi-hospital me-1"></i> ${s.name}
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

async function loadDoctors(specialtyId) {
  const grid = document.getElementById('doctor-grid');
  grid.innerHTML = _skeletonCard.repeat(6);

  try {
    const res = await Api.doctors.list(specialtyId || null, '', document.getElementById('doctor-sort').value);
    allDoctors = res.data || [];
    filterDoctors();
  } catch (err) {
    grid.innerHTML = `<div class="col-12"><div class="alert alert-danger shadow-sm">${err.message}</div></div>`;
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

        <h5 class="fw-bold text-dark mb-1">${d.fullName}</h5>
        <div class="mb-3">
          <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fw-semibold">
            ${d.specialtyName || 'Chuyên khoa tổng quát'}
          </span>
        </div>

        <div class="d-flex justify-content-center gap-2 mb-3">
          <div class="stat-badge small text-muted"><i class="bi bi-mortarboard-fill text-warning me-1"></i> ${d.degree || 'Bác sĩ'}</div>
          <div class="stat-badge small text-muted"><i class="bi bi-award-fill text-info me-1"></i> ${d.experienceYears} năm KN</div>
        </div>

        <div class="mt-auto pt-3 border-top d-flex align-items-center justify-content-between">
          <div class="text-start">
            <small class="text-muted d-block">Giá khám:</small>
            <span class="fw-bold text-success fs-5">${Utils.formatCurrency(d.consultationFee)}</span>
          </div>
          <div class="d-flex gap-2">
            <button class="btn btn-outline-info btn-sm rounded-circle" title="Xem thông tin chi tiết" onclick="viewDoctorDetail(${d.userId})">
              <i class="bi bi-info-circle fs-6"></i>
            </button>
            <button class="btn btn-primary rounded-pill px-3 fw-semibold shadow-sm" onclick="openBookingModal(${d.userId}, '${d.fullName}', '${d.specialtyName || 'Chuyên khoa'}')">
              <i class="bi bi-calendar-plus me-1"></i> Đặt Lịch
            </button>
          </div>
        </div>
      </div>
    </div>
  `).join('');
}

function viewDoctorDetail(docId) {
  const doc = allDoctors.find(d => d.userId === docId);
  if (!doc) return;

  document.getElementById('doc-detail-body').innerHTML = `
    <div class="text-center mb-4">
      <div class="avatar-circle shadow-sm mb-2" style="width:90px; height:90px; font-size:2.5rem;">
        <i class="bi bi-person-fill"></i>
      </div>
      <h4 class="fw-bold text-dark mb-1">${doc.fullName}</h4>
      <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fs-6">
        ${doc.specialtyName || 'Chuyên khoa'}
      </span>
    </div>

    <div class="row g-3">
      <div class="col-md-6">
        <div class="bg-light p-3 rounded-3">
          <small class="text-muted d-block mb-1"><i class="bi bi-mortarboard me-1"></i>Bằng cấp / Học vị:</small>
          <strong class="text-dark">${doc.degree || 'Bác sĩ chuyên khoa'}</strong>
        </div>
      </div>
      <div class="col-md-6">
        <div class="bg-light p-3 rounded-3">
          <small class="text-muted d-block mb-1"><i class="bi bi-award me-1"></i>Kinh nghiệm:</small>
          <strong class="text-dark">${doc.experienceYears} năm kinh nghiệm</strong>
        </div>
      </div>
      <div class="col-md-6">
        <div class="bg-light p-3 rounded-3">
          <small class="text-muted d-block mb-1"><i class="bi bi-telephone me-1"></i>Điện thoại liên hệ:</small>
          <strong class="text-dark">${doc.phone || 'Chưa cung cấp'}</strong>
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

const debouncedFilter = Utils.debounce(filterDoctors, 300);

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

  bookingBsModal.show();
  loadSlots();
}

async function loadSlots() {
  const docId = document.getElementById('selected-doctor-id').value;
  const date = document.getElementById('booking-date').value;
  const container = document.getElementById('slots-container');
  if (!date || !docId) return;
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
    const slots = res.data || [];

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
          onclick="selectSlot(this, ${s.id})">
          <i class="bi bi-clock me-1"></i>${Utils.formatTime(s.startTime)} - ${Utils.formatTime(s.endTime)}
        </button>
      `;
    }).join('');
  } catch (err) {
    container.innerHTML = `<div class="alert alert-danger py-2 px-3 small mb-0">${err.message}</div>`;
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
  document.getElementById('btn-confirm-booking').disabled = false;
}

document.getElementById('booking-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const scheduleId = document.getElementById('selected-schedule-id').value;
  const symptoms = document.getElementById('symptoms').value;
  const btn = document.getElementById('btn-confirm-booking');
  const docName = document.getElementById('modal-doc-detail-name').innerText;
  const dateVal = document.getElementById('booking-date').value;

  if (!scheduleId) return;
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý đặt lịch...';

  try {
    const res = await Api.appointments.create({
      scheduleId: parseInt(scheduleId),
      symptoms: symptoms
    });

    bookingBsModal.hide();
    Utils.showToast('Đặt lịch khám thành công!', 'success');

    const createdApp = res.data || {};
    document.getElementById('success-booking-code').innerText = createdApp.bookingCode || 'BK-SUCCESS';
    document.getElementById('success-doctor-name').innerText = docName;
    document.getElementById('success-date-time').innerText = Utils.formatDate(dateVal);
    successBsModal.show();

  } catch (err) {
    Utils.showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Xác Nhận Đặt Lịch';
  }
});
