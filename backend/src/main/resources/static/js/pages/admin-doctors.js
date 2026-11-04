Auth.checkAuth(['ADMIN']);

    async function loadPendingDoctors() {
      const tbody = document.getElementById('pending-doctors-list');
      try {
        const res = await Api.admin.pendingDoctors();
        const list = res.data || [];

        if (list.length === 0) {
          tbody.innerHTML = `
            <tr>
              <td colspan="7" class="text-center py-5">
                <i class="bi bi-check2-circle display-4 text-success"></i>
                <h5 class="fw-bold text-secondary mt-2">Hiện không có bác sĩ nào chờ phê duyệt 🎉</h5>
              </td>
            </tr>
          `;
          return;
        }

        tbody.innerHTML = list.map(item => `
          <tr>
            <td><strong class="text-dark">${Utils.escapeHtml(item.fullName)}</strong></td>
            <td class="text-muted"><i class="bi bi-envelope me-1"></i>${Utils.escapeHtml(item.email)}</td>
            <td class="text-muted"><i class="bi bi-telephone me-1"></i>${Utils.escapeHtml(item.phone || '-')}</td>
            <td><span class="badge bg-warning-subtle text-warning border border-warning-subtle">${Utils.escapeHtml(item.degree || 'Bác sĩ')}</span></td>
            <td>${Utils.escapeHtml(item.experienceYears)} năm</td>
            <td class="fw-bold text-success">${Utils.formatCurrency(item.consultationFee)}</td>
            <td class="text-end">
              <div class="d-inline-flex gap-2">
                <button class="btn btn-success btn-sm rounded-pill px-3 fw-semibold" data-ui-click="action35" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.userId))}">
                  <i class="bi bi-check-lg me-1"></i>Duyệt
                </button>
                <button class="btn btn-outline-danger btn-sm rounded-pill px-3 fw-semibold" data-ui-click="action36" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.userId))}">
                  <i class="bi bi-x-lg me-1"></i>Từ chối
                </button>
              </div>
            </td>
          </tr>
        `).join('');
      } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-danger text-center py-3">${Utils.escapeHtml(err.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action76">Thử lại</button></td></tr>`;
      }
    }

    async function approve(id) {
      try {
        await Api.admin.approveDoctor(id);
        Utils.showToast('Phê duyệt bác sĩ thành công!', 'success');
        loadPendingDoctors();
      } catch (err) {
        Utils.showToast(err.message, 'error');
      }
    }

    async function reject(id) {
      if (!confirm('Bạn có chắc chắn muốn từ chối bác sĩ này?')) return;
      try {
        await Api.admin.rejectDoctor(id);
        Utils.showToast('Đã từ chối bác sĩ!', 'info');
        loadPendingDoctors();
      } catch (err) {
        Utils.showToast(err.message, 'error');
      }
    }

    loadPendingDoctors();
