const user = Auth.checkAuth(['PATIENT']);
    if (user) {
      document.getElementById('user-name').innerText = user.fullName || user.email.split('@')[0];
      document.getElementById('user-avatar').innerText = (user.fullName || user.email).charAt(0).toUpperCase();
    }

    let allAppointments = [];
    let currentFilter = 'ALL';
    let detailModal = null, cancelModal = null;
    let activeDetailId = null;

    document.addEventListener('DOMContentLoaded', () => {
      detailModal = new bootstrap.Modal(document.getElementById('detail-modal'));
      cancelModal = new bootstrap.Modal(document.getElementById('cancel-modal'));
      loadAppointments();
    });

    async function loadAppointments() {
      const container = document.getElementById('appointments-container');
      try {
        const res = await Api.appointments.mine();
        allAppointments = res.data || [];
        renderAppointments();
      } catch (err) {
        container.innerHTML = `<div class="alert alert-danger shadow-sm rounded-3">${Utils.escapeHtml(err.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action72">Thử lại</button></div>`;
      }
    }

    function filterStatus(status) {
      currentFilter = status;
      document.querySelectorAll('[id^="filter-"]').forEach(btn => {
        btn.classList.remove('btn-primary', 'btn-success', 'btn-danger', 'btn-warning');
        btn.classList.add('btn-outline-secondary');
      });
      const active = document.getElementById('filter-' + status);
      if (active) { active.classList.remove('btn-outline-secondary'); active.classList.add('btn-primary'); }
      renderAppointments();
    }

    function renderAppointments() {
      const container = document.getElementById('appointments-container');
      const filtered = currentFilter === 'ALL' ? allAppointments : allAppointments.filter(a => a.status === currentFilter);

      if (filtered.length === 0) {
        container.innerHTML = `
          <div class="card border-0 shadow-sm p-5 text-center rounded-4">
            <i class="bi bi-calendar-x display-2 text-muted"></i>
            <h5 class="fw-bold text-secondary mt-3">Không có lịch hẹn nào</h5>
            <p class="text-muted">Chưa có lịch hẹn nào trong danh sách này.</p>
            <div><a href="dashboard.html" class="btn btn-primary rounded-pill px-4 mt-2 fw-semibold"><i class="bi bi-search me-1"></i> Tìm Bác Sĩ & Đặt Ngay</a></div>
          </div>`;
        return;
      }

      container.innerHTML = filtered.map(item => {
        let badgeBg = 'bg-primary-subtle text-primary';
        let icon = 'bi-calendar-check-fill';
        let borderClass = '';
        if (item.status === 'COMPLETED') { badgeBg = 'bg-success-subtle text-success'; icon = 'bi-check-circle-fill'; borderClass = 'border-start border-success border-4'; }
        if (item.status === 'CANCELLED') { badgeBg = 'bg-danger-subtle text-danger'; icon = 'bi-x-circle-fill'; borderClass = 'border-start border-danger border-4'; }

        return `
          <div class="card appointment-card p-4 shadow-sm clickable ${borderClass}" data-ui-click="action52" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.id))}">
            <div class="row align-items-center g-3">
              <div class="col-auto">
                <div class="timeline-badge ${badgeBg}"><i class="bi ${icon}"></i></div>
              </div>
              <div class="col-md-4">
                <div class="detail-label mb-1">Mã lịch hẹn</div>
                <div class="font-monospace fw-bold text-primary small">${Utils.escapeHtml(item.bookingCode)}</div>
                <h5 class="fw-bold text-dark mb-0 mt-1"><i class="bi bi-person-badge text-secondary me-1"></i>${Utils.escapeHtml(item.doctorName || 'Bác sĩ chuyên khoa')}</h5>
              </div>
              <div class="col-md-3">
                <div class="detail-label mb-1">Thời gian khám</div>
                <div class="fw-semibold text-dark">${Utils.formatDate(item.workDate)}</div>
                <div class="small text-primary fw-medium">${Utils.formatTime(item.startTime)} — ${Utils.formatTime(item.endTime)}</div>
              </div>
              <div class="col-md-3">
                <div class="detail-label mb-1">Triệu chứng</div>
                <div class="small text-dark text-truncate" style="max-width: 220px;" title="${Utils.escapeHtml(item.symptoms || '')}">${Utils.escapeHtml(item.symptoms || 'Không ghi nhận')}</div>
                <div class="mt-2">${Utils.getStatusBadge(item.status)}</div>
              </div>
              <div class="col-md-1 text-end">
                <i class="bi bi-chevron-right text-muted fs-5"></i>
              </div>
            </div>
          </div>`;
      }).join('');
    }

    function openDetail(id) {
      const item = allAppointments.find(a => a.id === id);
      if (!item) return;
      activeDetailId = id;

      const isCompleted = item.status === 'COMPLETED';
      const isConfirmed = item.status === 'CONFIRMED';

      const cancelBtn = document.getElementById('btn-cancel-from-detail');
      if (isConfirmed) cancelBtn.classList.remove('d-none');
      else cancelBtn.classList.add('d-none');

      document.getElementById('detail-content').innerHTML = `
        <div class="row g-4">
          <div class="col-md-6">
            <p class="detail-label mb-1">Mã lịch hẹn</p>
            <p class="fw-bold font-monospace text-primary">${Utils.escapeHtml(item.bookingCode)}</p>

            <p class="detail-label mb-1">Bác sĩ khám</p>
            <p class="fw-bold text-dark"><i class="bi bi-person-badge me-1 text-primary"></i>${Utils.escapeHtml(item.doctorName || 'Chưa có thông tin')}</p>

            <p class="detail-label mb-1">Ngày & Giờ khám</p>
            <p class="fw-bold text-dark"><i class="bi bi-clock me-1 text-primary"></i>${Utils.formatDate(item.workDate)} | ${Utils.formatTime(item.startTime)} - ${Utils.formatTime(item.endTime)}</p>

            <p class="detail-label mb-1">Chi phí khám</p>
            <p class="fw-bold text-success fs-5"><i class="bi bi-currency-dollar me-1"></i>${Utils.formatCurrency(item.price)}</p>
          </div>
          <div class="col-md-6">
            <p class="detail-label mb-1">Trạng thái</p>
            <p>${Utils.getStatusBadge(item.status)}</p>

            <p class="detail-label mb-1">Triệu chứng / Lý do khám</p>
            <p class="text-dark">${Utils.escapeHtml(item.symptoms || 'Không ghi nhận')}</p>
          </div>

          ${isCompleted ? `
          <div class="col-12">
            <hr class="my-1">
            <h6 class="fw-bold text-success mb-3"><i class="bi bi-clipboard2-pulse me-2"></i>Kết Quả Chẩn Đoán Của Bác Sĩ</h6>
            <div class="bg-success-subtle rounded-4 p-4">
              <div class="mb-3">
                <p class="detail-label mb-1">Chẩn đoán bệnh</p>
                <p class="fw-bold text-dark fs-6">${Utils.escapeHtml(item.diagnosis || 'Chưa cập nhật')}</p>
              </div>
              <div>
                <p class="detail-label mb-1">Lời dặn & Đơn thuốc bác sĩ</p>
                <p class="text-dark mb-0" style="white-space: pre-wrap; line-height: 1.7;">${Utils.escapeHtml(item.doctorNotes || 'Không có ghi chú.')}</p>
              </div>
            </div>
          </div>` : ''}
        </div>`;

      detailModal.show();
    }

    function promptCancelFromDetail() {
      document.getElementById('cancel-target-id').value = activeDetailId;
      detailModal.hide();
      setTimeout(() => cancelModal.show(), 300);
    }

    function promptCancel(id) {
      document.getElementById('cancel-target-id').value = id;
      cancelModal.show();
    }

    async function executeCancel() {
      const id = document.getElementById('cancel-target-id').value;
      const btn = document.getElementById('btn-confirm-cancel');
      if (!id) return;
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang hủy...';
      try {
        await Api.appointments.cancel(parseInt(id));
        cancelModal.hide();
        Utils.showToast('Đã hủy lịch hẹn thành công!', 'success');
        loadAppointments();
      } catch (err) {
        Utils.showToast(err.message, 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-x-circle me-1"></i> Đồng Ý Hủy Lịch';
      }
    }
