// ============================================================
// api.js — HTTP client với auto Bearer token & error handling
// ============================================================
const _apiCache = new Map();
const CACHE_TTL = 60000;
function getCached(key) {
    const entry = _apiCache.get(key);
    if (entry && Date.now() - entry.ts < CACHE_TTL) return entry.data;
    _apiCache.delete(key);
    return null;
}
function setCache(key, data) {
    _apiCache.set(key, { data, ts: Date.now() });
    if (_apiCache.size > 50) {
        const oldest = _apiCache.keys().next().value;
        _apiCache.delete(oldest);
    }
}

// Served pages use the backend's origin, including custom ports and HTTPS.
// Separate frontend hosting can opt in to a different API with CLINIC_API_BASE.
const API_BASE = window.CLINIC_API_BASE || '/api/v1';

const Api = {
  _getToken() { return localStorage.getItem('accessToken'); },

  async _request(method, path, body = null, auth = true, retried = false) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth) {
      const token = this._getToken();
      if (token) headers['Authorization'] = `Bearer ${token}`;
    }
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const cacheKey = method === 'GET' ? `${auth ? this._getToken() : 'public'}:${path}` : null;
    if (cacheKey) {
        const cached = getCached(cacheKey);
        if (cached) return cached;
    }

    try {
      const res = await fetch(`${API_BASE}${path}`, opts);
      const data = await res.json().catch(() => ({}));

      if (res.status === 401 && auth) {
        const refreshed = !retried && await this._tryRefresh();
        if (refreshed) return this._request(method, path, body, auth, true);
        _apiCache.clear();
        Auth.logout();
        throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      }
      if (!res.ok) {
        const msg = data.message || data.error || `Lỗi ${res.status}`;
        throw new Error(msg);
      }
      if (cacheKey) setCache(cacheKey, data);
      else if (method !== 'GET') _apiCache.clear();
      return data;
    } catch (err) {
      if (err.name === 'TypeError') throw new Error('Không thể kết nối đến server. Kiểm tra backend đang chạy chưa?');
      throw err;
    }
  },

  async _tryRefresh() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return false;
    try {
      const res = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      });
      if (!res.ok) return false;
      const data = await res.json();
      if (!data.data?.accessToken) return false;
      localStorage.setItem('accessToken', data.data.accessToken);
      if (data.data.refreshToken) localStorage.setItem('refreshToken', data.data.refreshToken);
      return true;
    } catch { return false; }
  },

  get: (path, auth = true) => Api._request('GET', path, null, auth),
  post: (path, body, auth = true) => Api._request('POST', path, body, auth),
  put: (path, body, auth = true) => Api._request('PUT', path, body, auth),
  patch: (path, body, auth = true) => Api._request('PATCH', path, body, auth),
  delete: (path, auth = true) => Api._request('DELETE', path, null, auth),

  // ── Auth ──
  auth: {
    register: (body) => Api.post('/auth/register', body, false),
    login: (body) => Api.post('/auth/login', body, false),
    refresh: (body) => Api.post('/auth/refresh', body, false),
    changePassword: (body) => Api.post('/auth/change-password', body),
  },

  // ── Doctors ──
  doctors: {
    list: (specialtyId, keyword = '', sort = 'name') => {
      const params = new URLSearchParams();
      params.set('sort', sort);
      params.set('size', '100');
      if (specialtyId) params.set('specialtyId', specialtyId);
      if (keyword) params.set('keyword', keyword);
      return Api.get(`/doctors${params.size ? `?${params}` : ''}`);
    },
    get: (id) => Api.get(`/doctors/${id}`),
    specialties: () => Api.get('/doctors/specialties', false),
    slots: (id, date) => Api.get(`/doctors/${id}/slots?date=${date}`),
    createSlot: (body) => Api.post('/doctors/slots', body),
    toggleSlot: (id, avail) => Api.put(`/doctors/slots/${id}/toggle?available=${avail}`, null),
    updateSlot: (id, body) => Api.put(`/doctors/slots/${id}`, body),
    deleteSlot: (id) => Api.delete(`/doctors/slots/${id}`),
    myAppointments: () => Api.get('/doctors/my-appointments'),
    appointmentsByRange: (from, to) => Api.get(`/doctors/my-appointments/range?from=${from}&to=${to}`),
    updateAppointmentStatus: (id, status) => Api.put(`/doctors/appointments/${id}/status?status=${status}`, null),
    updateMedRecord: (body) => Api.post('/doctors/medical-records', body),
    profile: () => Api.get('/doctors/me'),
    updateProfile: (body) => Api.put('/doctors/me', body),
    mySlots: () => Api.get('/doctors/my-slots'),
    completedPatients: () => Api.get('/doctors/my-patients/completed'),
  },

  // ── Appointments ──
  appointments: {
    create: (body) => Api.post('/appointments', body),
    cancel: (id) => Api.delete(`/appointments/${id}/cancel`),
    get: (id) => Api.get(`/appointments/${id}`),
    mine: () => Api.get('/appointments/my-appointments'),
  },

  // ── Admin Appointments ──
  adminAppointments: {
    all: (page = 0, size = 20) => Api.get(`/admin/appointments?page=${page}&size=${size}`),
    getPatientHistory: (patientId) => Api.get(`/admin/patients/${patientId}/appointments`),
  },

  // ── Admin ──
  admin: {
    pendingDoctors: () => Api.get('/admin/doctors/pending'),
    approveDoctor: (id) => Api.put(`/admin/doctors/${id}/approve`, null),
    rejectDoctor: (id) => Api.put(`/admin/doctors/${id}/reject`, null),
    specialties: () => Api.get('/admin/specialties'),
    createSpecialty: (body) => Api.post('/admin/specialties', body),
    updateSpecialty: (id, body) => Api.put(`/admin/specialties/${id}`, body),
    deleteSpecialty: (id) => Api.delete(`/admin/specialties/${id}`),
    users: (role = '') => Api.get(`/admin/users${role ? `?role=${role}` : ''}`),
    setUserStatus: (id, active) => Api.put(`/admin/users/${id}/status?active=${active}`, null),
    stats: () => Api.get('/admin/stats'),
    analytics: (year) => Api.get(`/admin/analytics?year=${year}`),
    doctors: () => Api.get('/admin/doctors'),
    createDoctor: (body) => Api.post('/admin/doctors', body),
    updateDoctor: (id, body) => Api.put(`/admin/doctors/${id}`, body),
    deleteDoctor: (id) => Api.delete(`/admin/doctors/${id}`),
    patients: () => Api.get('/admin/patients'),
    createPatient: (body) => Api.post('/admin/patients', body),
    updatePatient: (id, body) => Api.put(`/admin/patients/${id}`, body),
    deletePatient: (id) => Api.delete(`/admin/patients/${id}`),
    services: () => Api.get('/services/admin'),
    createService: (body) => Api.post('/services', body),
    updateService: (id, body) => Api.put(`/services/${id}`, body),
    deleteService: (id) => Api.delete(`/services/${id}`),
  },

  // ── Patient ──
  patient: {
    profile: () => Api.get('/patients/me'),
    update: (body) => Api.put('/patients/me', body),
    medicalRecords: () => Api.get('/patients/me/medical-records'),
  },
};
