// Offline UI/CSP/accessibility smoke tests. API fixtures deliberately do not prove backend integration.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../backend/src/main/resources/static');
const malicious = `O'Brian "><img src=x onerror="window.injected=true">`;
const doctor = { userId: 1, fullName: malicious, email: 'doctor@clinic', degree: 'Bác sĩ', specialtyId: 1, specialtyName: malicious, consultationFee: 250000, experienceYears: 5, isApproved: true, isActive: true };
const appointment = { id: 1, bookingCode: 'BK1', doctorId: 1, doctorName: malicious, patientId: 2, patientName: malicious, patientPhone: '0900000000', symptoms: malicious, workDate: '2026-12-10', startTime: '09:00:00', endTime: '09:30:00', status: 'CONFIRMED', price: 250000 };
const slot = { id: 1, workDate: appointment.workDate, startTime: '09:00:00', endTime: '09:30:00', isAvailable: true, isBooked: false };
const profile = { userId: 2, email: 'patient@clinic', fullName: malicious, gender: 'OTHER', isActive: true, phone: '0900000000', dob: '1990-01-01', address: malicious };
(async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const errors = [], results = [];
  let role = 'PATIENT', bookingWrites = 0, conflict = true;
  const context = await browser.newContext();
  await context.addInitScript(() => { window.cspViolations = []; document.addEventListener('securitypolicyviolation', event => window.cspViolations.push(event.violatedDirective)); });
  await context.addInitScript(() => localStorage.setItem('user', JSON.stringify({ id: 2, email: 'user@clinic', role: location.pathname.startsWith('/admin/') ? 'ADMIN' : location.pathname.startsWith('/doctor/') ? 'DOCTOR' : 'PATIENT' })));
  await context.route('http://clinic.test/**', async route => {
    const url = new URL(route.request().url()), p = url.pathname;
    if (p.startsWith('/api/v1/')) {
      let data = [];
      if (p === '/api/v1/auth/refresh') data = { accessToken: 'fixture-token', userId: 2, email: 'user@clinic', role };
      else if (p === '/api/v1/clinic-info') data = { name: 'Phòng khám', address: 'Địa điểm thật', cancellationHours: 24 };
      else if (p.endsWith('/specialties')) data = [{ id: 1, name: malicious, description: malicious }];
      else if (p === '/api/v1/doctors') data = [doctor];
      else if (p === '/api/v1/doctors/me') data = doctor;
      else if (p === '/api/v1/patients/me') data = profile;
      else if (p.includes('/medical-record')) data = p.includes('/patients/') ? [{ id: 1, doctorName: malicious, diagnosis: malicious, doctorNotes: malicious }] : { diagnosis: malicious, doctorNotes: malicious };
      else if (p.endsWith('/slots') || p.endsWith('/my-slots')) data = [slot];
      else if (p.includes('appointments') && route.request().method() === 'GET') data = [appointment];
      else if (p === '/api/v1/appointments') {
        bookingWrites++;
        if (conflict) { conflict = false; return route.fulfill({ status: 409, json: { message: 'Ca khám đã được đặt' } }); }
        data = appointment;
      } else if (p.endsWith('/doctors') || p.endsWith('/doctors/pending')) data = [doctor];
      else if (p.endsWith('/patients')) data = [profile];
      else if (p.endsWith('/users')) data = [{ id: 1, role: 'DOCTOR', isActive: true }, { id: 2, role: 'PATIENT', isActive: true }];
      else if (p.includes('/services')) data = [{ id: 1, code: 'GEN', name: malicious, specialtyName: malicious, durationMinutes: 30, defaultPrice: 250000, active: true }];
      else if (p.endsWith('/stats')) data = { totalDoctors: 1, totalPatients: 1, totalAppointments: 1, totalRevenue: 250000 };
      else if (p.endsWith('/analytics')) data = { yearRevenue: 250000, months: [{ month: 1, revenue: 250000, appointments: 1 }], transactions: [{ ...appointment, appointmentId: 1, amount: 250000, month: 1, specialty: malicious }], doctorRankings: [] };
      return route.fulfill({ json: { success: true, data } });
    }
    const file = path.join(root, p === '/' ? 'index.html' : decodeURIComponent(p));
    if (!file.startsWith(root + path.sep) || !fs.existsSync(file)) return route.fulfill({ status: 404, body: 'missing' });
    const mime = { '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.woff2': 'font/woff2', '.woff': 'font/woff' }[path.extname(file)] || 'application/octet-stream';
    return route.fulfill({ body: fs.readFileSync(file), headers: { 'content-type': mime, 'content-security-policy': "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'" } });
  });
  const page = await context.newPage();
  page.on('pageerror', e => errors.push(e.message));
  const pages = ['index.html', 'patient/dashboard.html', 'patient/doctors.html', 'patient/appointments.html', 'patient/medical-records.html', 'patient/profile.html', 'doctor/dashboard.html', 'doctor/schedule.html', 'doctor/profile.html', 'admin/dashboard.html', 'admin/analytics.html', 'admin/users.html', 'admin/doctors.html', 'admin/specialties.html', 'admin/services.html'];
  try {
    for (const width of [375, 768, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      for (const file of pages) {
        role = file.startsWith('admin/') ? 'ADMIN' : file.startsWith('doctor/') ? 'DOCTOR' : 'PATIENT';
        await page.goto('http://clinic.test/' + file);
        await page.waitForTimeout(90);
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1);
        if (overflow) console.error(await page.locator('body *').evaluateAll(nodes => nodes.filter(n => n.getBoundingClientRect().right > innerWidth + 1).map(n => ({ tag:n.tagName, cls:n.className, width:n.getBoundingClientRect().width, right:n.getBoundingClientRect().right })).slice(0,12)));
        assert.equal(overflow, false, `${file} overflows at ${width}px`);
        assert.equal(await page.evaluate(() => !!window.injected), false, `${file} executes stored markup`);
        assert.equal(await page.locator('img[src=x]').count(), 0, `${file} inserts stored markup`);
        assert.deepEqual(await page.evaluate(() => window.cspViolations), [], `${file} violates CSP`);
        const unlabeled = await page.locator('input:not([type="hidden"]), select, textarea').evaluateAll(fields => fields.filter(field => !field.labels.length && !field.getAttribute('aria-label')).length);
        assert.equal(unlabeled, 0, `${file} has unlabeled form controls`);
        results.push(`${width}px ${file}`);
      }
    }
    await page.goto('http://clinic.test/patient/dashboard.html'); role = 'PATIENT';
    await page.locator('.doctor-card button').last().click();
    await page.locator('#symptoms').fill(malicious);
    await page.locator('.slot-btn').first().click();
    await page.locator('#btn-review-booking').click();
    await page.locator('#booking-review').waitFor({ state: 'visible' });
    assert.match(await page.locator('#booking-review').innerText(), /250.000/);
    assert.match(await page.locator('#booking-review').innerText(), /Địa điểm thật/);
    assert.equal(bookingWrites, 0);
    await page.locator('#btn-confirm-booking').click();
    await page.waitForFunction(() => document.getElementById('booking-feedback').textContent.includes('giữ lại'));
    assert.equal(await page.locator('#symptoms').inputValue(), malicious);
    await page.locator('.slot-btn').first().click(); await page.locator('#btn-review-booking').click();
    await page.locator('#btn-confirm-booking').click();
    await page.locator('#success-modal').waitFor({ state: 'visible' });
    assert.equal(bookingWrites, 2);
    // Keyboard activation through the same declarative click action.
    await page.keyboard.press('Escape'); await page.locator('#doctor-search-input').focus(); await page.keyboard.press('Tab');
    assert.notEqual(await page.evaluate(() => document.activeElement.tagName), 'BODY');
    assert.deepEqual(errors, []);
    process.stdout.write(JSON.stringify({ checks: results.length + 4, fixtureOnly: true, errors, checksRun: ['responsive all pages', 'strict script CSP', 'stored HTML remains text', 'labels', 'booking review/conflict/retry', 'keyboard tab'] }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
