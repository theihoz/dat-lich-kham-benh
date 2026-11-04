
    function onRoleChange(role) {
      document.querySelectorAll('.patient-field').forEach(el => el.classList.toggle('hidden', role !== 'PATIENT'));
      document.querySelectorAll('.doctor-field').forEach(el => el.classList.toggle('hidden', role !== 'DOCTOR'));
      document.getElementById('specialtyId').disabled = role !== 'DOCTOR';
    }

    async function loadSpecialties() {
      try {
        const specialties = (await Api.doctors.specialties()).data || [];
        document.getElementById('specialtyId').innerHTML = specialties
          .map(item => `<option value="${Utils.escapeHtml(String(item.id))}">${Utils.escapeHtml(item.name)}</option>`).join('');
      } catch (error) {
        document.getElementById('error-alert').innerText = error.message;
        document.getElementById('error-alert').classList.remove('hidden');
      }
    }

    document.getElementById('register-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const role = document.getElementById('role').value;
      const errBox = document.getElementById('error-alert');
      const btn = document.getElementById('btn-submit');
      errBox.classList.add('hidden');
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> Đang tạo tài khoản...';

      const body = {
        email: document.getElementById('email').value,
        password: document.getElementById('password').value,
        role: role,
        fullName: document.getElementById('fullName').value,
        phone: document.getElementById('phone').value,
      };

      if (role === 'PATIENT') {
        body.dob = document.getElementById('dob').value;
        body.gender = document.getElementById('gender').value;
        body.address = document.getElementById('address').value;
      } else {
        body.specialtyId = parseInt(document.getElementById('specialtyId').value);
        body.degree = document.getElementById('degree').value;
        body.experienceYears = parseInt(document.getElementById('experienceYears').value);
        body.consultationFee = parseFloat(document.getElementById('consultationFee').value);
      }

      try {
        await Api.auth.register(body);
        Utils.showToast('Đăng ký thành công. Hãy kiểm tra email để xác minh tài khoản trước khi đăng nhập.', 'success');
        setTimeout(() => { window.location.href = 'login.html'; }, 1500);
      } catch (err) {
        errBox.innerText = err.message;
        errBox.classList.remove('hidden');
      } finally {
        btn.disabled = false;
        btn.innerHTML = 'Tạo Tài Khoản';
      }
    });
    document.getElementById('role').addEventListener('change', event => onRoleChange(event.target.value));
    onRoleChange(document.getElementById('role').value);
    loadSpecialties();
