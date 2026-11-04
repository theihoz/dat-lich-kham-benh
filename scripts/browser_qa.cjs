// Chạy trên demo localhost với Playwright đã cài (NODE_PATH nếu dùng runtime riêng).
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const base = process.env.CLINIC_BASE_URL || 'http://localhost:18080';
  const out = process.env.CLINIC_QA_OUTPUT || path.resolve(__dirname, '../docs/evidence');
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ headless: true, channel: 'chrome' });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const errors = [];
  const failed = [];
  const checks = [];
  page.on('pageerror', e => errors.push(e.message));
  page.on('response', r => { if (r.status() >= 400 && r.url().startsWith(base)) failed.push({url:r.url(),status:r.status()}); });
  async function login(email, password, role) {
    await page.goto(base + '/login.html');
    await page.locator('#email').fill(email);
    await page.locator('#password').fill(password);
    await page.locator('#btn-submit').click();
    await page.waitForURL('**/' + role + '/dashboard.html');
  }
  async function logout() {
    await page.evaluate(() => Auth.logout());
    await page.waitForURL('**/login.html');
  }
  try {
    await login('bn.hoangminh@gmail.com', 'Patient@123', 'patient');
    await page.waitForFunction(() => document.querySelectorAll('.doctor-card').length > 0);
    checks.push('Patient login and persisted doctor list');
    const sortedResponse = page.waitForResponse(r => r.url().includes('/api/v1/doctors?') && r.url().includes('sort=feeAsc'));
    await page.locator('#doctor-sort').selectOption('feeAsc');
    const sorted = (await (await sortedResponse).json()).data;
    assert(sorted.every((d, i) => i === 0 || sorted[i-1].consultationFee <= d.consultationFee));
    checks.push('Strategy feeAsc changes server-side order');
    await page.waitForFunction(() => document.querySelectorAll('.doctor-card').length > 0);
    await page.locator('#doctor-search-input').fill('Nguyễn Văn An');
    await page.waitForFunction(() => document.querySelectorAll('.doctor-card').length === 1);
    await page.locator('.doctor-card button').last().click();
    await page.locator('#booking-modal').waitFor({state:'visible'});
    const day = new Date(); day.setDate(day.getDate() + 2);
    const date = [day.getFullYear(), String(day.getMonth()+1).padStart(2,'0'), String(day.getDate()).padStart(2,'0')].join('-');
    await page.locator('#booking-date').fill(date);
    await page.locator('#booking-date').dispatchEvent('change');
    await page.locator('.slot-btn:not([disabled])').first().click();
    await page.locator('#symptoms').fill('Kiểm thử demo môn học');
    await page.locator('#btn-review-booking').click();
    const createdResponse = page.waitForResponse(r => r.url().endsWith('/api/v1/appointments') && r.request().method() === 'POST');
    await page.locator('#btn-confirm-booking').click();
    const created = (await (await createdResponse).json()).data;
    assert(created.id && created.bookingCode.startsWith('BK-'));
    await page.locator('#success-modal').waitFor({state:'visible'});
    checks.push('Builder booking through UI persists real appointment');
    await page.screenshot({path:path.join(out,'patient-booking.png'),fullPage:true});
    // Close modal before navigating; cancellation exercises API on the same authenticated browser session.
    const cancelled = await page.evaluate(async id => {
      const res = await Api.appointments.cancel(id);
      const detail = await Api.appointments.get(id);
      return {success:res.success,status:detail.data.status};
    }, created.id);
    assert(cancelled.success && cancelled.status === 'CANCELLED');
    checks.push('Cancellation updates persisted status');
    await logout();
    await login('bs.tranthib@clinic.vn', 'Doctor@123', 'doctor');
    await page.waitForFunction(() => document.getElementById('stat-doc-total').innerText !== '0' && document.getElementById('stat-doc-total').innerText !== '...');
    const all = await page.evaluate(async () => (await Api.doctors.myAppointments()).data);
    const completed = all.find(a => a.status === 'COMPLETED');
    assert(completed, 'Seed contains a completed appointment for this doctor');
    const persistedBefore = await page.evaluate(async id => (await Api.get(`/doctors/appointments/${id}/medical-record`)).data, completed.id);
    await page.evaluate(id => openPatientDetail(id), completed.id);
    await page.locator('#patient-detail-modal').waitFor({state:'visible'});
    assert((await page.locator('#patient-detail-content').innerText()).includes(persistedBefore.diagnosis));
    checks.push('Doctor sees persisted medical record after login');
    await page.screenshot({path:path.join(out,'doctor-record.png'),fullPage:true});
    await page.locator('#btn-diagnose-from-detail').click();
    await page.locator('#record-modal').waitFor({state:'visible'});
    await page.locator('#diagnosis').fill('Bệnh án kiểm thử cập nhật từ giao diện');
    const saved = page.waitForResponse(r => r.url().endsWith('/api/v1/doctors/medical-records') && r.request().method() === 'POST');
    await page.locator('#btn-save-record').click();
    assert.equal((await saved).status(), 200);
    await page.reload();
    await page.waitForFunction(() => document.getElementById('stat-doc-total').innerText === '1');
    await page.evaluate(id => openPatientDetail(id), completed.id);
    await page.locator('#patient-detail-modal').waitFor({state:'visible'});
    assert((await page.locator('#patient-detail-content').innerText()).includes('Bệnh án kiểm thử cập nhật từ giao diện'));
    checks.push('Doctor edit survives page reload and real API read');
    await logout();
    await login('admin@clinic.vn', 'Admin@123', 'admin');
    await page.goto(base + '/admin/analytics.html');
    await page.waitForFunction(() => document.getElementById('stat-year-revenue').innerText !== '—');
    assert((await page.locator('body').innerText()).includes('chưa xác nhận thanh toán'));
    checks.push('Admin analytics uses completed fees without fake payments');
    await page.evaluate(() => Object.values(Chart.instances).forEach(chart => { chart.stop(); chart.update('none'); }));
    await page.screenshot({path:path.join(out,'admin-analytics.png'),fullPage:true});
    await page.goto(base + '/patterns.html');
    for (const width of [375,768,1440]) {
      await page.setViewportSize({width,height:1000});
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), 'No horizontal overflow at '+width);
      await page.screenshot({path:path.join(out,`patterns-${width}.png`),fullPage:true});
    }
    await page.locator('#refresh').click();
    await page.waitForFunction(() => document.querySelectorAll('#inbox article').length >= 2);
    const subjects = await page.locator('#inbox h3').allTextContents();
    assert(subjects.some(s=>s.includes('Xác nhận')) && subjects.some(s=>s.includes('hủy')));
    await page.screenshot({path:path.join(out,'patterns-inbox.png'),fullPage:true});
    checks.push('Observer + Factory Method + Adapter delivered confirmed/cancelled messages into demo inbox');
    assert.deepEqual(errors, []);
    assert.deepEqual(failed, []);
    fs.writeFileSync(path.join(out,'browser-qa.json'), JSON.stringify({time:new Date().toISOString(),checks,errors,failed,
      scope:'Local demo H2; no SMTP/RabbitMQ; responsive smoke at 375/768/1440. No visual baseline or full accessibility audit.'},null,2));
    const diagnostic = path.join(out,'browser-failure.png');
    if (fs.existsSync(diagnostic)) fs.unlinkSync(diagnostic);
    console.log(JSON.stringify({checks,errors,failed},null,2));
  } catch (error) {
    await page.screenshot({path:path.join(out,'browser-failure.png'),fullPage:true});
    console.error(JSON.stringify({url:page.url(),errors,failed,body:(await page.locator('body').innerText()).slice(-2000)},null,2));
    throw error;
  } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1;});
