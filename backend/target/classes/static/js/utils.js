// ============================================================
// utils.js — Utility helpers & Bootstrap 5 Toasts
// ============================================================
const Utils = {
  onSubmit(form, callback) {
    let pending = false;
    form.addEventListener('submit', async event => {
      event.preventDefault();
      if (pending) return;
      pending = true;
      const buttons = [...form.querySelectorAll('button[type="submit"], button:not([type]), input[type="submit"]')];
      const prior = buttons.map(button => button.disabled);
      buttons.forEach(button => { button.disabled = true; });
      form.setAttribute('aria-busy', 'true');
      try { await callback(event); }
      catch (error) { Utils.showToast(error.message || 'Không thể lưu. Vui lòng thử lại.', 'error'); }
      finally {
        pending = false;
        buttons.forEach((button, index) => { button.disabled = prior[index]; });
        form.removeAttribute('aria-busy');
      }
    });
  },
  localDate(date = new Date()) {
    const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date);
    const get = type => parts.find(part => part.type === type).value;
    return `${get('year')}-${get('month')}-${get('day')}`;
  },
  escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, char => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[char]));
  },
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
          <div>${Utils.escapeHtml(message)}</div>
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" data-ui-click="action64"></button>
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
    return Utils.escapeHtml(String(timeStr).substring(0, 5));
  },

  getRoleBadge(role) {
    switch (role) {
      case 'ADMIN': return '<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="bi bi-shield-lock me-1"></i>Quản trị</span>';
      case 'DOCTOR': return '<span class="badge bg-info-subtle text-info border border-info-subtle"><i class="bi bi-heart-pulse me-1"></i>Bác sĩ</span>';
      case 'PATIENT': return '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-person me-1"></i>Bệnh nhân</span>';
      default: return `<span class="badge bg-secondary">${Utils.escapeHtml(role)}</span>`;
    }
  },

  getStatusBadge(status) {
    switch (status) {
      case 'CONFIRMED': return '<span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-2"><i class="bi bi-calendar-check-fill me-1"></i>Đã xác nhận</span>';
      case 'COMPLETED': return '<span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-2"><i class="bi bi-check-all me-1"></i>Đã khám</span>';
      case 'CANCELLED': return '<span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-3 py-2"><i class="bi bi-x-circle-fill me-1"></i>Đã hủy</span>';
      default: return `<span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-3 py-2">${Utils.escapeHtml(status)}</span>`;
    }
  }
};


// Shared accessibility and navigation enhancements, after all page markup exists.
document.addEventListener('DOMContentLoaded', () => {
  const sidebar = document.querySelector('.sidebar');
  if (sidebar) {
    sidebar.setAttribute('aria-label', 'Điều hướng tài khoản');
    const heading = document.createElement('div');
    heading.className = 'mobile-menu-label'; heading.textContent = 'Menu tài khoản — vuốt để xem thêm';
    sidebar.prepend(heading);
  }
  document.querySelectorAll('input:not([type="hidden"]), select, textarea').forEach((field, index) => {
    if (!field.id) field.id = 'clinic-field-' + index;
    let label = field.closest('.form-group, .mb-3, .mb-4, [class*="col-"]')?.querySelector('label');
    if (label && !label.htmlFor) label.htmlFor = field.id;
    if (!field.labels?.length && !field.getAttribute('aria-label')) field.setAttribute('aria-label', field.placeholder || label?.textContent?.trim() || field.id);
    if (field.required) field.setAttribute('aria-required', 'true');
  });
  document.querySelectorAll('table').forEach(table => {
    if (!table.closest('.table-responsive')) {
      const wrapper = document.createElement('div'); wrapper.className = 'table-responsive';
      wrapper.tabIndex = 0; wrapper.setAttribute('aria-label', 'Bảng dữ liệu, cuộn ngang để xem thêm');
      table.before(wrapper); wrapper.append(table);
    }
  });
  document.querySelectorAll('.modal').forEach(modal => {
    const title = modal.querySelector('.modal-title');
    if (title && !title.id) title.id = modal.id + '-title';
    if (title) modal.setAttribute('aria-labelledby', title.id);
  });
  const enhanceActions = container => container.querySelectorAll('[data-ui-click]').forEach(element => {
    if (!element.matches('button, a, input, select, textarea')) {
      element.tabIndex = 0; element.setAttribute('role', 'button');
    }
  });
  enhanceActions(document);
  new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(node => {
    if (node.nodeType === 1) { enhanceActions(node.parentElement || node); }
  }))).observe(document.body, { childList: true, subtree: true });
  document.addEventListener('keydown', event => {
    if ((event.key === 'Enter' || event.key === ' ') && event.target.matches('[data-ui-click][role="button"]')) {
      event.preventDefault(); event.target.click();
    }
  });
});
