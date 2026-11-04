const user = Auth.checkAuth(['PATIENT']);
    if (user) {
      document.getElementById('user-name').innerText = user.fullName || user.email.split('@')[0];
      document.getElementById('user-avatar').innerText = (user.fullName || user.email).charAt(0).toUpperCase();
    }

    let recordDetailModal = null;
    let loadedRecords = [];
    document.addEventListener('DOMContentLoaded', () => {
      recordDetailModal = new bootstrap.Modal(document.getElementById('record-detail-modal'));
      loadRecords();
    });

    async function loadRecords() {
      const container = document.getElementById('records-container');
      try {
        const records = (await Api.patient.medicalRecords()).data || [];
        loadedRecords = records;

        if (records.length === 0) {
          container.innerHTML = `
            <div class="card border-0 shadow-sm rounded-4 p-5 text-center bg-white">
              <i class="bi bi-file-earmark-x display-2 text-muted mb-3"></i>
              <h5 class="fw-bold text-secondary">Chưa có hồ sơ bệnh án nào</h5>
              <p class="text-muted">Kết quả chẩn đoán từ bác sĩ sẽ xuất hiện tại đây sau mỗi lần khám bệnh hoàn thành.</p>
              <a href="dashboard.html" class="btn btn-primary rounded-pill px-4 mt-2 fw-semibold">
                <i class="bi bi-search me-1"></i> Đặt Lịch Khám Ngay
              </a>
            </div>`;
          return;
        }

        container.innerHTML = records.map(x => `
          <article class="card record-card p-4 shadow-sm" style="cursor:pointer" data-ui-click="action59" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(x.id))}">
            <div class="row align-items-start g-3">
              <div class="col-auto">
                <div class="bg-success-subtle text-success rounded-3 d-flex align-items-center justify-content-center" style="width:50px;height:50px;font-size:1.4rem;">
                  <i class="bi bi-clipboard2-pulse"></i>
                </div>
              </div>
              <div class="col">
                <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">
                  <h5 class="fw-bold text-dark mb-0">
                    <i class="bi bi-stethoscope me-2 text-primary"></i>${Utils.escapeHtml(x.diagnosis || 'Kết quả khám')}
                  </h5>
                  <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-2">
                    <i class="bi bi-calendar-check me-1"></i>${Utils.formatDate(x.createdAt || x.workDate || '')}
                  </span>
                </div>
                ${x.doctorName ? `<div class="small text-muted mb-2"><i class="bi bi-person-badge me-1 text-primary"></i>Bác sĩ: <strong>${Utils.escapeHtml(x.doctorName)}</strong></div>` : ''}
                <div class="p-3 bg-light rounded-3">
                  <small class="text-muted d-block mb-1 fw-semibold"><i class="bi bi-journal-text me-1"></i>Ghi chú & Đơn thuốc:</small>
                  <p class="mb-0 text-dark text-truncate" style="max-height:48px; overflow:hidden;">${Utils.escapeHtml(x.doctorNotes || 'Không có ghi chú.')}</p>
                </div>
                <div class="mt-2 small text-primary fw-semibold"><i class="bi bi-arrow-right-circle me-1"></i>Bấm để xem đầy đủ chi tiết</div>
              </div>
            </div>
          </article>
        `).join('');
      } catch (e) {
        container.innerHTML = `<div class="alert alert-danger shadow-sm rounded-3">${Utils.escapeHtml(e.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action71">Thử lại</button></div>`;
      }
    }

    function openRecordDetail(id) {
      const x = loadedRecords.find(record => record.id === id);
      if (!x) return;
      document.getElementById('record-detail-content').innerHTML = `
        <div class="row g-3">
          <div class="col-md-6">
            <p class="text-muted fw-semibold small text-uppercase mb-1">Bác sĩ điều trị</p>
            <p class="fw-bold text-dark"><i class="bi bi-person-badge me-1 text-primary"></i>${Utils.escapeHtml(x.doctorName || 'Chưa có thông tin')}</p>
            <p class="text-muted fw-semibold small text-uppercase mb-1">Ngày khám</p>
            <p class="fw-bold text-dark"><i class="bi bi-calendar-check me-1 text-success"></i>${Utils.formatDate(x.createdAt || x.workDate || '')}</p>
          </div>
          <div class="col-md-6">
            <p class="text-muted fw-semibold small text-uppercase mb-1">Chẩn đoán bệnh</p>
            <p class="fw-bold text-dark fs-6"><i class="bi bi-stethoscope me-1 text-primary"></i>${Utils.escapeHtml(x.diagnosis || 'Chưa cập nhật')}</p>
          </div>
          <div class="col-12">
            <hr>
            <p class="text-muted fw-semibold small text-uppercase mb-2"><i class="bi bi-journal-medical me-1"></i>Lời dặn của bác sĩ & Đơn thuốc chi tiết</p>
            <div class="bg-light rounded-4 p-4">
              <p class="mb-0 text-dark" style="white-space: pre-wrap; line-height: 1.8;">${Utils.escapeHtml(x.doctorNotes || 'Không có ghi chú.')}</p>
            </div>
          </div>
        </div>`;
      recordDetailModal.show();
    }
