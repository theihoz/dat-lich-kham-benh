Auth.checkAuth(['ADMIN']);
    let currentType = 'DOCTOR', records = [], specialties = [];
    let userPage = 0, usersLoading = false, userLoadId = 0;
    let modal = null;
    let schedModal = null;

    document.addEventListener('DOMContentLoaded', () => {
      modal = new bootstrap.Modal(document.getElementById('user-modal'));
      schedModal = new bootstrap.Modal(document.getElementById('schedule-modal'));

      initialize();
    });

    async function initialize() {
      // Đọc tham số URL để chuyển tab Bác sĩ / Bệnh nhân tự động
      const urlParams = new URLSearchParams(window.location.search);
      const typeParam = urlParams.get('type');
      if (typeParam === 'DOCTOR' || typeParam === 'PATIENT') {
        currentType = typeParam;
      }

      // Đổi trạng thái hiển thị của tab group
      const doctorTab = document.getElementById('doctor-tab');
      const patientTab = document.getElementById('patient-tab');
      if (currentType === 'DOCTOR') {
        doctorTab.classList.add('active');
        patientTab.classList.remove('active');
      } else {
        patientTab.classList.add('active');
        doctorTab.classList.remove('active');
      }

      try {
        specialties = (await Api.admin.specialties()).data || [];
        userSpecialty.innerHTML = specialties.map(s => `<option value="${s.id}">${Utils.escapeHtml(s.name)}</option>`).join('');
      } catch (err) {
        console.error("Lỗi lấy danh mục chuyên khoa", err);
      }
      await load();
    }

    async function load() {
      const requestId = ++userLoadId;
      usersLoading = true;
      userRows.innerHTML = '<tr><td colspan="6" role="status">Đang tải người dùng...</td></tr>';
      document.getElementById('users-prev').disabled = true;
      document.getElementById('users-next').disabled = true;
      try {
        const collection = currentType === 'DOCTOR' ? 'doctors' : 'patients';
        const response = await Api.get(`/admin/${collection}?page=${userPage}&size=20`);
        if (requestId !== userLoadId) return;
        records = response.data || [];
        render();
        document.getElementById('users-page').textContent = 'Trang ' + (userPage + 1);
        document.getElementById('users-prev').disabled = userPage === 0;
        document.getElementById('users-next').disabled = records.length < 20;
      } catch (error) {
        if (requestId !== userLoadId) return;
        records = [];
        userRows.innerHTML = `<tr><td colspan="6">${Utils.escapeHtml(error.message)} <button class="btn btn-outline-primary" data-ui-click="action70">Thử lại</button></td></tr>`;
      } finally { if (requestId === userLoadId) usersLoading = false; }
    }
    async function changeUsersPage(offset) {
      if (usersLoading || userPage + offset < 0) return;
      userPage += offset; await load();
    }

    function selectType(type) {
      currentType = type;
      userPage = 0;
      const doctorTab = document.getElementById('doctor-tab');
      const patientTab = document.getElementById('patient-tab');
      if (type === 'DOCTOR') {
        doctorTab.classList.add('active');
        patientTab.classList.remove('active');
      } else {
        patientTab.classList.add('active');
        doctorTab.classList.remove('active');
      }
      load();
    }

    function render() {
      tableHead.innerHTML = currentType === 'DOCTOR' 
        ? '<tr><th>Bác sĩ</th><th>Chuyên khoa</th><th>Giá khám</th><th>Duyệt</th><th>Tài khoản</th><th class="text-end">Thao tác</th></tr>' 
        : '<tr><th>Bệnh nhân</th><th>Điện thoại</th><th>Ngày sinh</th><th>Địa chỉ</th><th>Tài khoản</th><th class="text-end">Thao tác</th></tr>';
      
      userRows.innerHTML = records.map(item => {
        const account = item;
        const isActiveBadge = account.isActive 
          ? '<span class="badge bg-success-subtle text-success">Hoạt động</span>' 
          : '<span class="badge bg-danger-subtle text-danger">Khóa</span>';

        if (currentType === 'DOCTOR') {
          const approvedBadge = item.isApproved 
            ? '<span class="badge bg-success">Đã duyệt</span>' 
            : '<span class="badge bg-warning text-dark">Chờ duyệt</span>';
          


          return `
            <tr>
              <td>
                <div class="d-flex align-items-center gap-2">
                  <div class="bg-primary-subtle text-primary rounded-circle d-flex align-items-center justify-content-center fw-bold" style="width: 38px; height: 38px;">
                    ${Utils.escapeHtml(item.fullName ? item.fullName.charAt(item.fullName.lastIndexOf(' ') + 1) : 'D')}
                  </div>
                  <div>
                    <div class="fw-bold text-dark">${Utils.escapeHtml(item.fullName)}</div>
                    <div class="small text-muted">${Utils.escapeHtml(item.email)}</div>
                  </div>
                </div>
              </td>
              <td><span class="badge bg-info-subtle text-info">${Utils.escapeHtml(item.specialtyName || '')}</span></td>
              <td class="fw-semibold text-primary">${Utils.formatCurrency(item.consultationFee)}</td>
              <td>${approvedBadge}</td>
              <td>${isActiveBadge}</td>
              ${actions(item, account)}
            </tr>`;
        } else {
          return `
            <tr>
              <td>
                <div class="d-flex align-items-center gap-2">
                  <div class="bg-success-subtle text-success rounded-circle d-flex align-items-center justify-content-center fw-bold" style="width: 38px; height: 38px;">
                    ${Utils.escapeHtml(item.fullName ? item.fullName.charAt(item.fullName.lastIndexOf(' ') + 1) : 'P')}
                  </div>
                  <div>
                    <div class="fw-bold text-dark">${Utils.escapeHtml(item.fullName)}</div>
                    <div class="small text-muted">${Utils.escapeHtml(item.email)}</div>
                  </div>
                </div>
              </td>
              <td>${Utils.escapeHtml(item.phone || '')}</td>
              <td>${item.dob ? Utils.formatDate(item.dob) : ''}</td>
              <td class="text-truncate" style="max-width: 200px;">${Utils.escapeHtml(item.address || '')}</td>
              <td>${isActiveBadge}</td>
              ${actions(item, account)}
            </tr>`;
        }
      }).join('') || '<tr><td colspan="9" class="text-center text-muted py-4">Không tìm thấy người dùng nào</td></tr>';
    }

    function actions(item, account) {
      const lockIcon = account.isActive ? 'bi-lock-fill' : 'bi-unlock-fill';
      const lockTitle = account.isActive ? 'Khóa tài khoản' : 'Kích hoạt tài khoản';
      const btnClass = account.isActive ? 'btn-outline-warning' : 'btn-outline-success';

      return `
        <td class="text-end text-nowrap">
          ${currentType === 'DOCTOR' ? `<button class="btn btn-sm btn-outline-info me-1" data-ui-click="action28" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.fullName))}" data-ui-arg1="${Utils.escapeHtml(JSON.stringify(item.userId))}">Lịch khám</button>` : ''}
          <button class="btn btn-sm btn-outline-primary me-1 rounded-circle" data-ui-click="action29" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.userId))}" title="Chỉnh sửa thông tin">
            <i class="bi bi-pencil-fill"></i>
          </button> 
          <button class="btn btn-sm ${btnClass} me-1 rounded-circle" data-ui-click="action30" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.userId))}" data-ui-arg1="${Utils.escapeHtml(JSON.stringify(!account.isActive))}" title="${lockTitle}">
            <i class="bi ${lockIcon}"></i>
          </button> 
          <button class="btn btn-sm btn-outline-danger rounded-circle" data-ui-click="action31" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.userId))}" title="Xóa tài khoản">
            <i class="bi bi-trash-fill"></i>
          </button>
        </td>`;
    }

    function configureFields() {
      document.querySelectorAll('.doctor-fields').forEach(el => {
        if (currentType === 'DOCTOR') el.classList.remove('d-none');
        else el.classList.add('d-none');
      });
      document.querySelectorAll('.patient-fields').forEach(el => {
        if (currentType === 'PATIENT') el.classList.remove('d-none');
        else el.classList.add('d-none');
      });
    }

    function openCreate() {
      userForm.reset();
      editingId.value = '';
      userEmail.disabled = false;
      userPassword.required = true;
      document.getElementById('passwordField').classList.remove('d-none');
      document.getElementById('modalTitle').textContent = `Thêm tài khoản ${currentType === 'DOCTOR' ? 'Bác sĩ' : 'Bệnh nhân'}`;
      configureFields();
      modal.show();
    }

    function openEdit(id) {
      const item = records.find(record => record.userId === id);
      if (!item) return;

      editingId.value = id;
      userEmail.value = item.email || '';
      userEmail.disabled = true;
      userPassword.required = false;
      document.getElementById('passwordField').classList.add('d-none');
      userName.value = item.fullName || '';
      userPhone.value = item.phone || '';

      if (currentType === 'DOCTOR') {
        userSpecialty.value = item.specialtyId || '';
        userDegree.value = item.degree || '';
        userExperience.value = item.experienceYears ?? 0;
        userFee.value = item.consultationFee ?? 0;
      } else {
        userDob.value = item.dob || '';
        userGender.value = item.gender || 'OTHER';
        userAddress.value = item.address || '';
        userHistory.value = item.medicalHistory || '';
      }

      document.getElementById('modalTitle').textContent = `Cập nhật thông tin ${currentType === 'DOCTOR' ? 'Bác sĩ' : 'Bệnh nhân'}`;
      configureFields();
      modal.show();
    }

    Utils.onSubmit(userForm, async event => {
      event.preventDefault();
      const id = editingId.value;
      try {
        if (currentType === 'DOCTOR') {
          const body = {
            fullName: userName.value,
            phone: userPhone.value,
            specialtyId: Number(userSpecialty.value),
            degree: userDegree.value,
            experienceYears: Number(userExperience.value),
            consultationFee: Number(userFee.value)
          };
          if (id) {
            await Api.admin.updateDoctor(id, body);
          } else {
            await Api.admin.createDoctor({
              ...body,
              email: userEmail.value,
              password: userPassword.value,
              approved: true
            });
          }
        } else {
          const body = {
            fullName: userName.value,
            phone: userPhone.value,
            dob: userDob.value || null,
            gender: userGender.value,
            address: userAddress.value,
            medicalHistory: userHistory.value
          };
          if (id) {
            await Api.admin.updatePatient(id, body);
          } else {
            await Api.admin.createPatient({
              ...body,
              email: userEmail.value,
              password: userPassword.value
            });
          }
        }
        modal.hide();
        await load();
        Utils.showToast('Lưu thông tin thành công', 'success');
      } catch (error) {
        console.error(error);
        Utils.showToast(error.message || 'Có lỗi xảy ra', 'danger');
      }
    });

    async function toggleStatus(id, active) {
      try {
        await Api.admin.setUserStatus(id, active);
        await load();
        Utils.showToast(active ? 'Đã kích hoạt tài khoản' : 'Đã khóa tài khoản', 'success');
      } catch (error) {
        console.error(error);
        Utils.showToast(error.message || 'Lỗi cập nhật trạng thái', 'danger');
      }
    }

    async function removeUser(id) {
      if (!confirm('Bạn có chắc chắn muốn xóa tài khoản này? Những tài khoản đã phát sinh lịch hẹn khám sẽ không thể xóa.')) return;
      try {
        if (currentType === 'DOCTOR') {
          await Api.admin.deleteDoctor(id);
        } else {
          await Api.admin.deletePatient(id);
        }
        await load();
        Utils.showToast('Xóa tài khoản thành công', 'success');
      } catch (error) {
        console.error(error);
        Utils.showToast(error.message || 'Không thể xóa tài khoản này', 'danger');
      }
    }

let scheduleDoctorId = null, schedulePage = 0, scheduleLoading = false;
async function showDoctorSchedule(userId, fullName) {
  scheduleDoctorId = userId; schedulePage = 0;
  document.getElementById('scheduleModalTitle').textContent = 'Lịch khám — ' + fullName;
  schedModal.show();
  await loadAdminSchedule();
}
async function changeSchedulePage(offset) {
  if (scheduleLoading || schedulePage + offset < 0) return;
  schedulePage += offset; await loadAdminSchedule();
}
async function loadAdminSchedule() {
  if (scheduleLoading) return;
  scheduleLoading = true;
  const rows = document.getElementById('schedTableRows');
  rows.innerHTML = '<tr><td colspan="5" role="status">Đang tải lịch khám...</td></tr>';
  document.getElementById('schedule-prev').disabled = true;
  document.getElementById('schedule-next').disabled = true;
  try {
    const response = await Api.get(`/admin/appointments?doctorId=${scheduleDoctorId}&page=${schedulePage}&size=20`);
    const appointments = response.data || [];
    rows.innerHTML = appointments.map(item => `<tr><td>${Utils.formatDate(item.workDate)}</td><td>${Utils.formatTime(item.startTime)} – ${Utils.formatTime(item.endTime)}</td><td>${Utils.escapeHtml(item.patientName)}</td><td>${Utils.escapeHtml(item.symptoms)}</td><td>${Utils.getStatusBadge(item.status)}</td></tr>`).join('') || '<tr><td colspan="5">Chưa có lịch hẹn trong trang này.</td></tr>';
    document.getElementById('schedule-page').textContent = 'Trang ' + (schedulePage + 1);
    document.getElementById('schedule-prev').disabled = schedulePage === 0;
    document.getElementById('schedule-next').disabled = appointments.length < 20;
  } catch (error) {
    rows.innerHTML = `<tr><td colspan="5">${Utils.escapeHtml(error.message)} <button class="btn btn-outline-primary" data-ui-click="action67">Thử lại</button></td></tr>`;
  } finally { scheduleLoading = false; }
}
