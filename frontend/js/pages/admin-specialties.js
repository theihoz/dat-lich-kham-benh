Auth.checkAuth(['ADMIN']);

    let editBsModal = null;
    document.addEventListener('DOMContentLoaded', () => {
      editBsModal = new bootstrap.Modal(document.getElementById('edit-modal'));
    });

    async function loadSpecialties() {
      const tbody = document.getElementById('specialties-list');
      try {
        const res = await Api.admin.specialties();
        const list = res.data || [];

        if (list.length === 0) {
          tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Chưa có chuyên khoa nào.</td></tr>`;
          return;
        }

        tbody.innerHTML = list.map(item => `
          <tr>
            <td><strong class="font-monospace text-muted">#${item.id}</strong></td>
            <td><strong class="text-primary">${Utils.escapeHtml(item.name)}</strong></td>
            <td><small class="text-muted">${Utils.escapeHtml(item.description || '-')}</small></td>
            <td class="text-end text-nowrap">
              <button class="btn btn-sm btn-outline-primary rounded-pill me-1" data-ui-click="action19" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.name))}" data-ui-arg1="${Utils.escapeHtml(JSON.stringify(item.description || ''))}" data-ui-arg2="${Utils.escapeHtml(JSON.stringify(item.id))}">
                <i class="bi bi-pencil me-1"></i>Sửa
              </button>
              <button class="btn btn-sm btn-outline-danger rounded-pill" data-ui-click="action20" data-ui-arg0="${Utils.escapeHtml(JSON.stringify(item.name))}" data-ui-arg1="${Utils.escapeHtml(JSON.stringify(item.id))}">
                <i class="bi bi-trash me-1"></i>Xóa
              </button>
            </td>
          </tr>
        `).join('');
      } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-danger text-center py-3">${Utils.escapeHtml(err.message)} <button type="button" class="btn btn-outline-primary btn-sm" data-ui-click="action75">Thử lại</button></td></tr>`;
      }
    }

    function openEdit(id, name, description) {
      document.getElementById('edit-spec-id').value = id;
      document.getElementById('edit-spec-name').value = name;
      document.getElementById('edit-spec-description').value = description;
      editBsModal.show();
    }

    Utils.onSubmit(document.getElementById('edit-specialty-form'), async (e) => {
      e.preventDefault();
      const id = document.getElementById('edit-spec-id').value;
      const btn = document.getElementById('btn-save-edit');
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';
      try {
        await Api.admin.updateSpecialty(id, {
          name: document.getElementById('edit-spec-name').value,
          description: document.getElementById('edit-spec-description').value
        });
        Utils.showToast('Đã cập nhật chuyên khoa!', 'success');
        editBsModal.hide();
        loadSpecialties();
      } catch (err) {
        Utils.showToast(err.message, 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-lg me-1"></i>Lưu Thay Đổi';
      }
    });

    async function deleteSpecialty(id, name) {
      if (!confirm(`Bạn có chắc chắn muốn xóa chuyên khoa "${name}"? Hành động này không thể hoàn tác.`)) return;
      try {
        await Api.admin.deleteSpecialty(id);
        Utils.showToast(`Đã xóa chuyên khoa "${name}".`, 'success');
        loadSpecialties();
      } catch (err) {
        Utils.showToast(err.message, 'error');
      }
    }

    Utils.onSubmit(document.getElementById('create-specialty-form'), async (e) => {
      e.preventDefault();
      const btn = document.getElementById('btn-submit-spec');
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang thêm...';

      try {
        await Api.admin.createSpecialty({
          name: document.getElementById('name').value,
          description: document.getElementById('description').value
        });
        Utils.showToast('Thêm chuyên khoa mới thành công!', 'success');
        document.getElementById('create-specialty-form').reset();
        loadSpecialties();
      } catch (err) {
        Utils.showToast(err.message, 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-plus-lg me-1"></i> Thêm Chuyên Khoa';
      }
    });

    loadSpecialties();
