const user = Auth.getUser();
    if (user) {
      document.getElementById('nav-actions').innerHTML = `
        <button data-ui-click="action1" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(user.role))}" class="btn btn-primary rounded-pill px-4 fw-semibold">Vào Trang Quản Lý (${Utils.escapeHtml(user.role)})</button>
        <button data-ui-click="action2" class="btn btn-outline-light rounded-pill px-3">Đăng xuất</button>
      `;
    }
