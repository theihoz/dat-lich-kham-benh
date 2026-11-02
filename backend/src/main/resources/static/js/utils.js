// ============================================================
// utils.js — Utility helpers & Bootstrap 5 Toasts
// ============================================================
const Utils = {
  debounce(fn, delay = 300) {
    let timer;
    return (...args) => {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(null, args), delay);
    };
  },

  showToast(message, type = 'info') {
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container position-fixed top-0 end-0 p-3';
      container.style.zIndex = '1090';
      document.body.appendChild(container);
    }

    const toastId = 'toast-' + Date.now();
    const isError = type === 'error' || type === 'danger';
    const bgClass = type === 'success' ? 'bg-success text-white' : (isError ? 'bg-danger text-white' : 'bg-info text-white');
    const icon = type === 'success' ? '<i class="bi bi-check-circle-fill me-2 fs-5"></i>' : (isError ? '<i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>' : '<i class="bi bi-info-circle-fill me-2 fs-5"></i>');

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center ${bgClass} border-0 shadow-lg mb-2 show`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    toastEl.innerHTML = `
      <div class="d-flex">
        <div class="toast-body d-flex align-items-center py-2 px-3 fw-medium">
          ${icon}
          <div>${message}</div>
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" onclick="this.parentElement.parentElement.remove()"></button>
      </div>
    `;

    container.appendChild(toastEl);
    setTimeout(() => {
      toastEl.classList.remove('show');
      setTimeout(() => toastEl.remove(), 300);
    }, 4000);
  },

  formatCurrency(amount) {
    if (amount == null) return '0 đ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  },

  formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' });
  },

  formatTime(timeStr) {
    if (!timeStr) return '';
    return timeStr.substring(0, 5);
  },

  getRoleBadge(role) {
    switch (role) {
      case 'ADMIN': return '<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="bi bi-shield-lock me-1"></i>Quản trị</span>';
      case 'DOCTOR': return '<span class="badge bg-info-subtle text-info border border-info-subtle"><i class="bi bi-heart-pulse me-1"></i>Bác sĩ</span>';
      case 'PATIENT': return '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-person me-1"></i>Bệnh nhân</span>';
      default: return `<span class="badge bg-secondary">${role}</span>`;
    }
  },

  getStatusBadge(status) {
    switch (status) {
      case 'CONFIRMED': return '<span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-2"><i class="bi bi-calendar-check-fill me-1"></i>Đã xác nhận</span>';
      case 'COMPLETED': return '<span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-2"><i class="bi bi-check-all me-1"></i>Đã khám</span>';
      case 'CANCELLED': return '<span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-3 py-2"><i class="bi bi-x-circle-fill me-1"></i>Đã hủy</span>';
      default: return `<span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-3 py-2">${status}</span>`;
    }
  }
};

