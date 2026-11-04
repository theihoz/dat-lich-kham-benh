const user = Auth.checkAuth(['DOCTOR']);
    let recordBsModal = null;
    let patientDetailModal = null;
    let activePatientItem = null;

    let allAppointments = [];
    let appointmentsLoadError = null;
    const savedRecords = new Map();
    let medicalDraftDirty = false;
    function warnUnsavedMedicalChanges(event) {
      if (!medicalDraftDirty) return;
      event.preventDefault();
      event.returnValue = '';
    }
    window.addEventListener('beforeunload', warnUnsavedMedicalChanges);
    document.getElementById('record-form').addEventListener('input', () => { medicalDraftDirty = true; });
    document.getElementById('record-modal').addEventListener('hide.bs.modal', event => {
      if (medicalDraftDirty && !confirm('Bệnh án có thay đổi chưa lưu. Bạn muốn bỏ các thay đổi này?')) event.preventDefault();
      else medicalDraftDirty = false;
    });
    let currentFilterStatus = 'ALL'; // ALL, PENDING, COMPLETED
    let currentPage = 1;
    const pageSize = 15;

    document.addEventListener('DOMContentLoaded', () => {
      recordBsModal = new bootstrap.Modal(document.getElementById('record-modal'));
      patientDetailModal = new bootstrap.Modal(document.getElementById('patient-detail-modal'));

      if (user && user.fullName) {
        document.getElementById('doctor-display-name').textContent = `BS. ${user.fullName}`;
      }
      loadDoctorAppointments();
    });

    function filterByCard(status) {
      currentFilterStatus = status;
      currentPage = 1;

      document.querySelectorAll('.stat-card-clickable').forEach(c => c.classList.remove('active-card'));
      document.getElementById(`card-filter-${status}`).classList.add('active-card');

      document.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
      document.getElementById(`pill-${status}`).classList.add('active');

      const titleEl = document.getElementById('section-title');
      if (status === 'ALL') titleEl.innerHTML = '<i class="bi bi-clock-history text-primary me-2"></i>Tất Cả Lịch Hẹn Khám Chi Tiết';
      else if (status === 'PENDING') titleEl.innerHTML = '<i class="bi bi-hourglass-split text-warning me-2"></i>Danh Sách Bệnh Nhân Cần Khám (Đang Chờ)';
      else if (status === 'COMPLETED') titleEl.innerHTML = '<i class="bi bi-check-circle-fill text-success me-2"></i>Danh Sách Ca Khám Đã Hoàn Thành';

      renderAppointmentsTable();
    }

    function onSearchChange() {
      currentPage = 1;
      renderAppointmentsTable();
    }

    function escapeHtml(value) {
      return String(value ?? '').replace(/[&<>"']/g, char => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
      }[char]));
    }

    async function loadRecord(appId) {
      const response = await Api.get(`/doctors/appointments/${appId}/medical-record`);
      if (response.data) savedRecords.set(appId, response.data);
      else savedRecords.delete(appId);
      return response.data;
    }

    async function openPatientDetail(appId) {
      const item = allAppointments.find(a => a.id === appId);
      if (!item) return;
      try { await loadRecord(appId); }
      catch (error) { Utils.showToast(error.message, 'error'); return; }
      activePatientItem = item;
      const btnDiagnose = document.getElementById('btn-diagnose-from-detail');
      btnDiagnose.classList.toggle('d-none', item.status === 'CANCELLED');
      if (item.status === 'CONFIRMED') {
        btnDiagnose.classList.remove('d-none');
        btnDiagnose.innerHTML = '<i class="bi bi-pencil-square me-1"></i>Chẩn Đoán / Kê Đơn';
      } else {
        btnDiagnose.innerHTML = '<i class="bi bi-pencil-square me-1"></i>Cập Nhật Chẩn Đoán';
      }

      const record = savedRecords.get(item.id);
      const diagnosisText = record?.diagnosis;
      const notesText = record?.doctorNotes;

      document.getElementById('patient-detail-content').innerHTML = `
        <div class="row g-3">
          <div class="col-md-6">
            <p class="text-muted fw-bold small text-uppercase mb-1">Bệnh nhân</p>
            <p class="fw-bold text-dark fs-5 mb-2"><i class="bi bi-person-fill me-1 text-primary"></i>${escapeHtml(item.patientName || 'Chưa có tên')}</p>
            <p class="text-muted fw-bold small text-uppercase mb-1">Số điện thoại</p>
            <p class="fw-semibold mb-2"><i class="bi bi-telephone me-1 text-primary"></i>${escapeHtml(item.patientPhone || 'Chưa cung cấp')}</p>
            <p class="text-muted fw-bold small text-uppercase mb-1">Mã lịch hẹn</p>
            <p class="font-monospace fw-bold text-primary mb-0">${escapeHtml(item.bookingCode)}</p>
          </div>
          <div class="col-md-6">
            <p class="text-muted fw-bold small text-uppercase mb-1">Ngày & Giờ khám</p>
            <p class="fw-semibold text-dark mb-1"><i class="bi bi-calendar3 me-1 text-primary"></i>${Utils.formatDate(item.workDate)}</p>
            <p class="fw-semibold text-primary mb-2">${Utils.formatTime(item.startTime)} - ${Utils.formatTime(item.endTime)}</p>
            <p class="text-muted fw-bold small text-uppercase mb-1">Trạng thái</p>
            <p class="mb-0">${Utils.getStatusBadge(item.status)}</p>
          </div>
          <div class="col-12">
            <p class="text-muted fw-bold small text-uppercase mb-1">Triệu chứng / Ca khám gì</p>
            <div class="bg-warning-subtle rounded-3 p-3 border border-warning-subtle">
              <p class="mb-0 text-dark fw-medium">${escapeHtml(item.symptoms || 'Chưa cung cấp')}</p>
            </div>
          </div>
          ${!record ? `
          <div class="col-12"><div class="alert alert-info mb-0">
            Chưa có bệnh án cho lịch hẹn này.
          </div></div>` : ''}
          ${diagnosisText ? `
          <div class="col-12">
            <p class="text-muted fw-bold small text-uppercase mb-1">Kết Quả Chẩn Đoán</p>
            <div class="bg-success-subtle rounded-3 p-3 border border-success-subtle">
              <p class="mb-0 text-success fw-bold fs-6"><i class="bi bi-check-circle-fill me-2"></i>${escapeHtml(diagnosisText)}</p>
            </div>
          </div>` : ''}
          ${notesText ? `
          <div class="col-12">
            <p class="text-muted fw-bold small text-uppercase mb-1">Lời Dặn Bác Sĩ & Đơn Thuốc Kê</p>
            <div class="bg-light rounded-3 p-3 border">
              <p class="mb-0 text-dark" style="white-space: pre-wrap;">${escapeHtml(notesText)}</p>
            </div>
          </div>` : ''}
        </div>`;

      patientDetailModal.show();
    }

    function openDiagnoseFromDetail() {
      if (!activePatientItem) return;
      patientDetailModal.hide();
      setTimeout(() => openRecordModal(activePatientItem.id), 300);
    }

    async function loadDoctorAppointments() {
      try {
        const res = await Api.doctors.myAppointments();
        if (!Array.isArray(res.data)) throw new Error('Dữ liệu lịch hẹn không hợp lệ');
        allAppointments = res.data;
        appointmentsLoadError = null;
        allAppointments.sort((a, b) => {
          const today = Utils.localDate(new Date());
          const rank = x => x.workDate === today ? 0 : x.workDate > today ? 1 : 2;
          if (rank(a) !== rank(b)) return rank(a) - rank(b);
          if (a.workDate !== b.workDate) return rank(a) === 2 ? (b.workDate || '').localeCompare(a.workDate || '') : (a.workDate || '').localeCompare(b.workDate || '');
          return (a.startTime || '').localeCompare(b.startTime || '');
        });
        document.getElementById('stat-doc-total').innerText = allAppointments.length.toLocaleString('vi-VN');
        document.getElementById('stat-doc-pending').innerText = allAppointments.filter(a => a.status === 'CONFIRMED').length.toLocaleString('vi-VN');
        document.getElementById('stat-doc-completed').innerText = allAppointments.filter(a => a.status === 'COMPLETED').length.toLocaleString('vi-VN');
      } catch (err) {
        allAppointments = [];
        appointmentsLoadError = err.message || 'Không thể tải lịch hẹn';
        ['stat-doc-total', 'stat-doc-pending', 'stat-doc-completed'].forEach(id => document.getElementById(id).innerText = '—');
      }
      renderAppointmentsTable();
    }

    function renderAppointmentsTable() {
      const tbody = document.getElementById('doctor-appointments');
      if (appointmentsLoadError) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-danger text-center py-4">${escapeHtml(appointmentsLoadError)} <button class="btn btn-sm btn-outline-primary" data-ui-click="action9">Thử lại</button></td></tr>`;
        document.getElementById('section-count-info').innerText = 'Không thể tải dữ liệu lịch hẹn';
        document.getElementById('pagination-info').innerText = '';
        renderPagination(0);
        return;
      }
      const searchKey = (document.getElementById('searchPatientInput').value || '').toLowerCase().trim();

      let filteredList = allAppointments;

      // Filter Status
      if (currentFilterStatus === 'PENDING') {
        filteredList = filteredList.filter(item => item.status === 'CONFIRMED');
      } else if (currentFilterStatus === 'COMPLETED') {
        filteredList = filteredList.filter(item => item.status === 'COMPLETED');
      }

      // Filter Search Keyword
      if (searchKey) {
        filteredList = filteredList.filter(item =>
          (item.patientName || '').toLowerCase().includes(searchKey) ||
          (item.patientPhone || '').includes(searchKey) ||
          (item.bookingCode || '').toLowerCase().includes(searchKey) ||
          (item.symptoms || '').toLowerCase().includes(searchKey)
        );
      }

      const totalItems = filteredList.length;
      const totalPages = Math.ceil(totalItems / pageSize) || 1;

      if (currentPage > totalPages) currentPage = totalPages;

      const startIndex = (currentPage - 1) * pageSize;
      const endIndex = Math.min(startIndex + pageSize, totalItems);
      const pageItems = filteredList.slice(startIndex, endIndex);

      // Cập nhật thông số số lượng hiển thị
      document.getElementById('section-count-info').innerText = `Đang có tổng cộng ${totalItems.toLocaleString('vi-VN')} ca bệnh nhân`;
      document.getElementById('pagination-info').innerText = `Hiển thị ${totalItems > 0 ? startIndex + 1 : 0} - ${endIndex} trong tổng số ${totalItems.toLocaleString('vi-VN')} ca`;

      if (pageItems.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-5 text-muted">Chưa có lịch hẹn phù hợp</td></tr>`;
        renderPagination(0);
        return;
      }

      tbody.innerHTML = pageItems.map(item => `
        <tr style="cursor:pointer" data-ui-click="action10" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.id))}">
          <td><strong class="font-monospace text-primary">${escapeHtml(item.bookingCode)}</strong></td>
          <td class="fw-bold text-dark">${Utils.formatDate(item.workDate)}</td>
          <td>
            <span class="badge bg-light text-dark fw-bold">${Utils.formatTime(item.startTime)} - ${Utils.formatTime(item.endTime)}</span>
          </td>
          <td>
            <div class="fw-bold text-dark">${escapeHtml(item.patientName || 'Chưa có tên')}</div>
          </td>
          <td class="text-muted"><i class="bi bi-telephone me-1"></i>${escapeHtml(item.patientPhone || 'Chưa cung cấp')}</td>
          <td><small class="text-muted d-inline-block text-truncate" style="max-width: 220px;">${escapeHtml(item.symptoms || 'Chưa cung cấp')}</small></td>
          <td>${Utils.getStatusBadge(item.status)}</td>
          <td class="text-end" data-ui-click="action11">
            ${item.status === 'CONFIRMED' ? `
              <button class="btn btn-primary btn-sm rounded-pill px-3 fw-semibold shadow-sm" data-ui-click="action12" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.id))}">
                <i class="bi bi-pencil-square me-1"></i> Chẩn Đoán
              </button>
            ` : `
              <button class="btn btn-outline-success btn-sm rounded-pill px-3 fw-semibold" data-ui-click="action10" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.id))}">
                <i class="bi bi-eye me-1"></i>Xem Chi Tiết
              </button>
            `}
          </td>
        </tr>
      `).join('');

      renderPagination(totalPages);
    }

    function renderPagination(totalPages) {
      const controls = document.getElementById('pagination-controls');
      if (totalPages <= 1) {
        controls.innerHTML = '';
        return;
      }

      let html = `
        <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
          <button class="page-item page-link" data-ui-click="action13"><i class="bi bi-chevron-double-left"></i></button>
        </li>
        <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
          <button class="page-item page-link" data-ui-click="action14" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(currentPage - 1))}"><i class="bi bi-chevron-left"></i></button>
        </li>
      `;

      let startPage = Math.max(1, currentPage - 2);
      let endPage = Math.min(totalPages, currentPage + 2);

      for (let p = startPage; p <= endPage; p++) {
        html += `
          <li class="page-item ${p === currentPage ? 'active' : ''}">
            <button class="page-item page-link" data-ui-click="action14" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(p))}">${p}</button>
          </li>
        `;
      }

      html += `
        <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
          <button class="page-item page-link" data-ui-click="action14" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(currentPage + 1))}"><i class="bi bi-chevron-right"></i></button>
        </li>
        <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
          <button class="page-item page-link" data-ui-click="action14" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(totalPages))}"><i class="bi bi-chevron-double-right"></i></button>
        </li>
      `;

      controls.innerHTML = html;
    }

    function changePage(page) {
      currentPage = page;
      renderAppointmentsTable();
    }

    async function openRecordModal(appId) {
      const found = allAppointments.find(a => a.id === appId);
      if (!found || found.status === 'CANCELLED') return;
      if (medicalDraftDirty && !confirm('Bỏ thay đổi bệnh án chưa lưu để mở lịch hẹn khác?')) return;
      try { await loadRecord(appId); }
      catch (error) { Utils.showToast(error.message, 'error'); return; }
      const record = savedRecords.get(appId);
      document.getElementById('app-id').value = appId;
      document.getElementById('diagnosis').value = record?.diagnosis || '';
      document.getElementById('doctorNotes').value = record?.doctorNotes || '';
      const needsConfirmation = found.status === 'COMPLETED' && !record;
      document.getElementById('existing-record-warning').classList.toggle('d-none', !needsConfirmation);
      const confirmation = document.getElementById('confirm-record-replacement');
      confirmation.checked = false;
      confirmation.required = needsConfirmation;
      medicalDraftDirty = false;
      recordBsModal.show();
    }

    Utils.onSubmit(document.getElementById('record-form'), async (e) => {
      e.preventDefault();
      const appId = Number(document.getElementById('app-id').value);
      const diagnosis = document.getElementById('diagnosis').value.trim();
      const doctorNotes = document.getElementById('doctorNotes').value;
      const btn = document.getElementById('btn-save-record');
      if (!diagnosis) {
        Utils.showToast('Vui lòng nhập chẩn đoán.', 'error');
        return;
      }

      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';

      try {
        await Api.doctors.updateMedRecord({ appointmentId: appId, diagnosis, doctorNotes });
        // Cache only content accepted by the server in this session.
        savedRecords.set(appId, { diagnosis, doctorNotes });
        medicalDraftDirty = false;
        recordBsModal.hide();
        Utils.showToast('Đã lưu bệnh án & hoàn thành khám trên hệ thống.', 'success');
        await loadDoctorAppointments();
      } catch (err) {
        Utils.showToast(err.message, 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-lg me-1"></i> Lưu Kết Quả & Hoàn Thành Khám';
      }
    });
