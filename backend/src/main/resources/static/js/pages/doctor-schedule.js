const user = Auth.checkAuth(['DOCTOR']);
    let slotsList = [];

    if (user) {
      document.getElementById('doctor-display-name').innerHTML = `<i class="bi bi-person-badge me-1"></i>BS. ${Utils.escapeHtml(user.fullName || user.email.split('@')[0])}`;
    }

    // Set min date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().slice(0, 10);
    document.getElementById('workDate').min = tomorrowStr;
    document.getElementById('workDate').value = tomorrowStr;

    async function loadSlots() {
      const tbody = document.getElementById('slot-rows');
      try {
        slotsList = (await Api.doctors.mySlots()).data || [];
        if (slotsList.length === 0) {
          tbody.innerHTML = `
            <tr>
              <td colspan="4" class="text-center py-5 text-muted">
                <i class="bi bi-calendar-x display-4 d-block mb-2"></i>
                Chưa có ca làm việc nào. Hãy tạo ca mới ở trên.
              </td>
            </tr>`;
          return;
        }
        tbody.innerHTML = slotsList.map(slot => {
          const actionButtons = slot.isBooked 
            ? '<span class="text-muted small"><i class="bi bi-lock-fill me-1"></i>Đã được đặt lịch</span>' 
            : `
              <button class="btn btn-sm ${slot.isAvailable ? 'btn-outline-warning' : 'btn-outline-success'} rounded-pill px-2 me-1"
                data-ui-click="action4" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(slot.id))}" data-ui-arg1="${Utils.escapeHtml(JSON.stringify(!slot.isAvailable))}" title="Bật/Tắt ca khám">
                ${slot.isAvailable ? '<i class="bi bi-dash-circle"></i> Tắt' : '<i class="bi bi-check-circle"></i> Bật'}
              </button>
              <button class="btn btn-sm btn-outline-primary rounded-pill px-2 me-1"
                data-ui-click="action5" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(slot.workDate))}" data-ui-arg1="${Utils.escapeHtml(JSON.stringify(slot.startTime))}" data-ui-arg2="${Utils.escapeHtml(JSON.stringify(slot.endTime))}" data-ui-arg3="${Utils.escapeHtml(JSON.stringify(slot.id))}" title="Chỉnh sửa ca khám">
                <i class="bi bi-pencil-fill"></i> Sửa
              </button>
              <button class="btn btn-sm btn-outline-danger rounded-pill px-2"
                data-ui-click="action6" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(slot.id))}" title="Xóa ca khám">
                <i class="bi bi-trash-fill"></i> Xóa
              </button>`;

          return `
            <tr>
              <td class="fw-semibold">${Utils.formatDate(slot.workDate)}</td>
              <td><i class="bi bi-clock me-1 text-primary"></i>${Utils.formatTime(slot.startTime)} - ${Utils.formatTime(slot.endTime)}</td>
              <td>
                <span class="badge ${slot.isBooked ? 'bg-warning-subtle text-warning border border-warning-subtle' : (slot.isAvailable ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-secondary-subtle text-secondary border border-secondary-subtle')} rounded-pill px-3 py-2">
                  ${slot.isBooked ? '<i class="bi bi-person-check me-1"></i>Đã đặt' : (slot.isAvailable ? '<i class="bi bi-check-circle me-1"></i>Đang mở' : '<i class="bi bi-dash-circle me-1"></i>Đã tắt')}
                </span>
              </td>
              <td class="text-end text-nowrap">
                ${actionButtons}
              </td>
            </tr>`;
        }).join('');
      } catch (error) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-danger text-center py-3">${Utils.escapeHtml(error.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action8">Thử lại</button></td></tr>`;
        Utils.showToast(error.message, 'error');
      }
    }

    async function toggleSlot(id, available) {
      try {
        await Api.doctors.toggleSlot(id, available);
        Utils.showToast(available ? 'Đã bật ca khám.' : 'Đã tắt ca khám.', 'success');
        await loadSlots();
      } catch (error) {
        Utils.showToast(error.message, 'error');
      }
    }

    function enterEditMode(id, date, start, end) {
      document.getElementById('editingSlotId').value = id;
      document.getElementById('workDate').value = date;
      document.getElementById('startTime').value = start.slice(0, 5);
      document.getElementById('endTime').value = end.slice(0, 5);
      
      document.getElementById('form-title').innerHTML = '<i class="bi bi-pencil-square text-primary me-2"></i>Chỉnh Sửa Ca Khám';
      document.getElementById('btn-submit-slot').innerHTML = '<i class="bi bi-check-lg me-1"></i>Cập Nhật';
      document.getElementById('btn-cancel-edit').classList.remove('d-none');
      
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function cancelEditMode() {
      document.getElementById('editingSlotId').value = '';
      document.getElementById('workDate').value = tomorrowStr;
      document.getElementById('startTime').value = "08:00";
      document.getElementById('endTime').value = "08:30";
      
      document.getElementById('form-title').innerHTML = '<i class="bi bi-plus-circle text-primary me-2"></i>Tạo Ca Khám Mới';
      document.getElementById('btn-submit-slot').innerHTML = '<i class="bi bi-plus-lg me-1"></i>Tạo Ca';
      document.getElementById('btn-cancel-edit').classList.add('d-none');
    }

    async function deleteSlot(id) {
      if (!confirm('Bạn có chắc chắn muốn xóa ca khám này?')) return;
      try {
        await Api.doctors.deleteSlot(id);
        Utils.showToast('Xóa ca khám thành công!', 'success');
        await loadSlots();
      } catch (error) {
        Utils.showToast(error.message, 'error');
      }
    }

    Utils.onSubmit(document.getElementById('slot-form'), async event => {
      event.preventDefault();
      const btn = document.getElementById('btn-submit-slot');
      const editingId = document.getElementById('editingSlotId').value;
      const workDate = document.getElementById('workDate').value;
      const startTime = document.getElementById('startTime').value;
      const endTime = document.getElementById('endTime').value;

      if (startTime >= endTime) {
        Utils.showToast('Giờ kết thúc phải sau giờ bắt đầu!', 'error');
        return;
      }

      btn.disabled = true;
      btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> ${editingId ? 'Đang lưu...' : 'Đang tạo...'}`;

      try {
        const body = {
          workDate: workDate,
          startTime: startTime,
          endTime: endTime
        };

        if (editingId) {
          await Api.doctors.updateSlot(editingId, body);
          Utils.showToast('Cập nhật ca khám thành công!', 'success');
          cancelEditMode();
        } else {
          await Api.doctors.createSlot(body);
          Utils.showToast('Đã tạo ca khám thành công!', 'success');
        }
        await loadSlots();
      } catch (error) {
        Utils.showToast(error.message, 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = editingId ? '<i class="bi bi-check-lg me-1"></i>Cập Nhật' : '<i class="bi bi-plus-lg me-1"></i>Tạo Ca';
      }
    });

    loadSlots();
