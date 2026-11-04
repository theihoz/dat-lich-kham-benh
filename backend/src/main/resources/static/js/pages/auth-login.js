
    // Redirect if already logged in
    // Session cookies are restored lazily by authenticated API calls after navigation.

    let forgotModal = null;
    document.addEventListener('DOMContentLoaded', () => {
      forgotModal = new bootstrap.Modal(document.getElementById('forgot-modal'));
    });

    function openForgotPassword(e) {
      e.preventDefault();
      const emailVal = document.getElementById('email').value;
      if (emailVal) document.getElementById('forgot-email').value = emailVal;
      forgotModal.show();
    }

    function togglePassword() {
      const input = document.getElementById('password');
      const icon = document.getElementById('eye-icon');
      if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'bi bi-eye-slash';
      } else {
        input.type = 'password';
        icon.className = 'bi bi-eye';
      }
    }

    async function sendForgotPassword() {
      const button = document.getElementById('btn-forgot-send');
      const email = document.getElementById('forgot-email').value.trim();
      if (!email || !document.getElementById('forgot-email').checkValidity()) {
        Utils.showToast('Vui lòng nhập email hợp lệ.', 'danger'); return;
      }
      button.disabled = true;
      try { const result = await Api.auth.forgotPassword({email}); Utils.showToast(result.message, 'success'); }
      catch (error) { Utils.showToast(error.message, 'danger'); }
      finally { button.disabled = false; }
    }
    document.getElementById('toggle-password').addEventListener('click', togglePassword);
    document.getElementById('forgot-link').addEventListener('click', openForgotPassword);
    document.getElementById('btn-forgot-send').addEventListener('click', sendForgotPassword);

    document.getElementById('login-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const errBox = document.getElementById('error-alert');
      const btn = document.getElementById('btn-submit');
      errBox.classList.add('hidden');
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> Đang đăng nhập...';

      try {
        const res = await Api.auth.login({
          email: document.getElementById('email').value,
          password: document.getElementById('password').value
        });
        Auth.setSession(res.data);
        Utils.showToast('Đăng nhập thành công!', 'success');
        setTimeout(() => Auth.redirectByRole(res.data.role), 500);
      } catch (err) {
        errBox.innerText = err.message || 'Email hoặc mật khẩu không đúng.';
        errBox.classList.remove('hidden');
      } finally {
        btn.disabled = false;
        btn.innerHTML = 'Đăng Nhập';
      }
    });
