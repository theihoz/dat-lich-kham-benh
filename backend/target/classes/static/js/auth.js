// Only public account metadata persists; credentials stay in memory or HttpOnly cookies.
localStorage.removeItem('accessToken');
localStorage.removeItem('refreshToken');
const Auth = {
  basePath() { return window.location.pathname.startsWith('/frontend/') ? '/frontend' : ''; },
  getUser() {
    try { return JSON.parse(localStorage.getItem('user') || 'null'); }
    catch { localStorage.removeItem('user'); return null; }
  },
  setSession(data) {
    Api._setToken(data.accessToken);
    localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken');
    localStorage.setItem('user', JSON.stringify({id: data.userId, email: data.email, role: data.role}));
  },
  clearSession() {
    Api._setToken(null); localStorage.removeItem('user');
    localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken');
  },
  async logout() {
    try {
      await fetch(`${API_BASE}/auth/logout`, {method: 'POST', credentials: 'same-origin',
        headers: {'Content-Type':'application/json', 'X-CSRF-Token':'clinic'}});
    } finally { this.clearSession(); window.location.href = `${this.basePath()}/login.html`; }
  },
  checkAuth(roles = []) {
    const user = this.getUser();
    if (!user) { window.location.href = `${this.basePath()}/login.html`; return null; }
    if (roles.length && !roles.includes(user.role)) { this.redirectByRole(user.role); return null; }
    return user;
  },
  redirectByRole(role) {
    const destinations = {ADMIN:'/admin/dashboard.html', DOCTOR:'/doctor/dashboard.html', PATIENT:'/patient/dashboard.html'};
    window.location.href = `${this.basePath()}${destinations[role] || '/index.html'}`;
  }
};
