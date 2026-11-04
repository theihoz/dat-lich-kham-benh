Auth.checkAuth(['PATIENT']);

    async function loadDoctors() {
      const grid = document.getElementById('doctors-grid');
      try {
        const res = await Api.doctors.list();
        const list = res.data || [];

        if (!list.length) { grid.innerHTML = '<p role="status">Chưa có bác sĩ phù hợp.</p>'; return; }
        grid.innerHTML = list.map(doc => `
          <div class="card doctor-card">
            <div class="avatar">👨‍⚕️</div>
            <div class="name">${Utils.escapeHtml(doc.fullName)}</div>
            <div class="specialty">${Utils.escapeHtml(doc.specialtyName || 'Chuyên khoa')}</div>
            <div class="meta">
              <div class="meta-row">⭐ Kinh nghiệm: ${Utils.escapeHtml(doc.experienceYears)} năm</div>
              <div class="meta-row">📞 SĐT: ${Utils.escapeHtml(doc.phone || '-')}</div>
            </div>
            <div class="fee">${Utils.formatCurrency(doc.consultationFee)}</div>
            <a href="/patient/dashboard.html" class="btn btn-primary btn-sm w-full mt-2">Đặt Lịch</a>
          </div>
        `).join('');
      } catch (err) {
        grid.innerHTML = `<div class="alert alert-error">${Utils.escapeHtml(err.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action73">Thử lại</button></div>`;
      }
    }

    loadDoctors();
