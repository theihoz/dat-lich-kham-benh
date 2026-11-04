const user = Auth.checkAuth(['DOCTOR']);
    if (user) {
      document.getElementById('doctor-display-name').innerHTML = `<i class="bi bi-person-badge me-1"></i>${Utils.escapeHtml(user.email.split('@')[0])}`;
    }

    async function loadProfile() {
      const feedback = document.getElementById('profile-feedback');
      feedback.textContent = 'Đang tải hồ sơ...';
      document.getElementById('btn-save').disabled = true;
      try {
        const profile = (await Api.doctors.profile()).data;
        document.getElementById('fullName').value = profile.fullName || '';
        document.getElementById('email').value = profile.email || '';
        document.getElementById('phone').value = profile.phone || '';
        document.getElementById('degree').value = profile.degree || '';
        document.getElementById('experienceYears').value = profile.experienceYears ?? '';
        document.getElementById('consultationFee').value = profile.consultationFee ?? '';
        feedback.textContent = '';
        document.getElementById('btn-save').disabled = false;
      } catch (error) {
        feedback.innerHTML = `${Utils.escapeHtml(error.message)} <button class="btn btn-outline-primary" type="button" data-ui-click="action3">Thử lại</button>`;
      }
    }

    Utils.onSubmit(document.getElementById('profile-form'), async event => {
      event.preventDefault();
      const btn = document.getElementById('btn-save');
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';

      try {
        await Api.doctors.updateProfile({
          fullName: document.getElementById('fullName').value,
          phone: document.getElementById('phone').value,
          degree: document.getElementById('degree').value,
          experienceYears: parseInt(document.getElementById('experienceYears').value) || 0,
          consultationFee: parseFloat(document.getElementById('consultationFee').value) || 0,
        });
        Utils.showToast('Đã cập nhật hồ sơ thành công!', 'success');
      } catch (error) {
        Utils.showToast(error.message, 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i>Lưu Thay Đổi';
      }
    });

    loadProfile();
