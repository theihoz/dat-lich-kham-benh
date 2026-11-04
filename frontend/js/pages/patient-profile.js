const user = Auth.checkAuth(['PATIENT']);
    if (user) {
      document.getElementById('user-name').innerText = user.fullName || user.email.split('@')[0];
      document.getElementById('user-avatar').innerText = (user.fullName || user.email).charAt(0).toUpperCase();
    }

    async function loadProfile() {
      const feedback = document.getElementById('profile-feedback');
      feedback.textContent = 'Đang tải hồ sơ...';
      document.getElementById('btn-save').disabled = true;
      try {
        const profile = (await Api.patient.profile()).data;
        ['fullName', 'email', 'dob', 'gender', 'phone', 'address', 'medicalHistory'].forEach(key => {
          const el = document.getElementById(key);
          if (el) el.value = profile[key] || '';
        });
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
        await Api.patient.update({
          fullName: document.getElementById('fullName').value,
          dob: document.getElementById('dob').value || null,
          gender: document.getElementById('gender').value,
          phone: document.getElementById('phone').value,
          address: document.getElementById('address').value,
          medicalHistory: document.getElementById('medicalHistory').value
        });
        Utils.showToast('Đã cập nhật hồ sơ thành công!', 'success');
      } catch (error) {
        Utils.showToast(error.message, 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i>Lưu Thay Đổi';
      }
    });

    Utils.onSubmit(document.getElementById('password-form'), async event => {
      event.preventDefault();
      const current = document.getElementById('currentPassword').value;
      const newPw = document.getElementById('newPassword').value;
      const confirm = document.getElementById('confirmPassword').value;

      if (newPw !== confirm) {
        Utils.showToast('Mật khẩu xác nhận không khớp!', 'error');
        return;
      }
      if (newPw.length < 8) {
        Utils.showToast('Mật khẩu mới phải có ít nhất 8 ký tự!', 'error');
        return;
      }

      const btn = document.getElementById('btn-change-pw');
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang cập nhật...';
      try {
        await Api.auth.changePassword({ currentPassword: current, newPassword: newPw });
        Utils.showToast('Đổi mật khẩu thành công! Vui lòng đăng nhập lại.', 'success');
        document.getElementById('password-form').reset();
        setTimeout(() => Auth.logout(), 2000);
      } catch (error) {
        Utils.showToast(error.message || 'Mật khẩu hiện tại không đúng', 'error');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-key me-1"></i>Cập Nhật Mật Khẩu';
      }
    });

    loadProfile();
