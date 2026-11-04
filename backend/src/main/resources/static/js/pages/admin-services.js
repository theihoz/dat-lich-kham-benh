Auth.checkAuth(['ADMIN']);
const serviceForm = document.getElementById('form');
const fields = Object.fromEntries(['sid', 'code', 'name', 'specialtyId', 'price', 'duration', 'description'].map(id => [id, document.getElementById(id)]));
let data = [], dialog = new bootstrap.Modal(document.getElementById('modal'));
async function load() {
  const rows = document.getElementById('rows');
  rows.innerHTML = '<tr><td colspan="7" role="status">Đang tải dịch vụ...</td></tr>';
  try {
    const [services, specialties] = await Promise.all([Api.admin.services(), Api.admin.specialties()]);
    data = services.data || [];
    fields.specialtyId.innerHTML = '<option value="">Đa khoa</option>' + (specialties.data || []).map(x => `<option value="${Number(x.id)}">${Utils.escapeHtml(x.name)}</option>`).join('');
    rows.innerHTML = data.map(x => `<tr><td><code>${Utils.escapeHtml(x.code)}</code></td><td class="fw-bold">${Utils.escapeHtml(x.name)}</td><td>${Utils.escapeHtml(x.specialtyName || 'Đa khoa')}</td><td>${Number(x.durationMinutes)} phút</td><td>${Utils.formatCurrency(x.defaultPrice)}</td><td>${x.active ? '<span class="badge bg-success">Đang phục vụ</span>' : '<span class="badge bg-secondary">Đã ngừng</span>'}</td><td><button class="btn btn-sm btn-outline-primary" data-ui-click="action42" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(x.id))}">Sửa</button> ${x.active ? `<button class="btn btn-sm btn-outline-danger" data-ui-click="action43" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(x.id))}">Ngừng</button>` : ''}</td></tr>`).join('') || '<tr><td colspan="7">Chưa có dịch vụ. Chọn Thêm dịch vụ để bắt đầu.</td></tr>';
  } catch (error) {
    rows.innerHTML = `<tr><td colspan="7">${Utils.escapeHtml(error.message)} <button type="button" class="btn btn-outline-primary" data-ui-click="action70">Thử lại</button></td></tr>`;
  }
}
function edit(id) {
  serviceForm.reset(); fields.sid.value = ''; fields.duration.value = 30;
  const item = data.find(value => value.id === id);
  if (item) {
    fields.sid.value = item.id; fields.code.value = item.code; fields.name.value = item.name;
    fields.specialtyId.value = item.specialtyId || ''; fields.price.value = item.defaultPrice;
    fields.duration.value = item.durationMinutes; fields.description.value = item.description || '';
  }
  dialog.show();
}
Utils.onSubmit(serviceForm, async event => {
  const existing = data.find(item => item.id === Number(fields.sid.value));
  const body = { code: fields.code.value, name: fields.name.value, specialtyId: fields.specialtyId.value ? Number(fields.specialtyId.value) : null, defaultPrice: Number(fields.price.value), durationMinutes: Number(fields.duration.value), description: fields.description.value, active: existing?.active ?? true };
  if (fields.sid.value) await Api.admin.updateService(fields.sid.value, body);
  else await Api.admin.createService(body);
  dialog.hide(); await load(); Utils.showToast('Đã lưu dịch vụ', 'success');
});
async function disable(id) {
  if (!confirm('Ngừng cung cấp dịch vụ này?')) return;
  try { await Api.admin.deleteService(id); await load(); Utils.showToast('Đã ngừng dịch vụ', 'success'); }
  catch (error) { Utils.showToast(error.message, 'error'); }
}
load();
