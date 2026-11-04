// Dependency-free regressions for the browser HTTP client and script loading.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { test } = require('node:test');
const root = path.resolve(__dirname, '../backend/src/main/resources/static');
test('service editor uses the name input instead of the browser window.name property', async () => {
  const elements = Object.fromEntries(['modal', 'form', 'sid', 'code', 'name', 'specialtyId', 'price', 'duration', 'description', 'rows'].map(id => [id, { value: '', innerHTML: '', reset() {}, events: {}, addEventListener(type, callback) { this.events[type] = callback; }, querySelectorAll() { return []; }, setAttribute() {}, removeAttribute() {} }]));
  let submitted;
  const service = { id: 1, name: 'Khám tổng quát', code: 'GEN', defaultPrice: 100000, durationMinutes: 30, active: true };
  const context = vm.createContext({ ...elements, name: '', document: { getElementById: id => elements[id], addEventListener() {} },
    Auth: { checkAuth() {} }, bootstrap: { Modal: class { show() {} hide() {} } },
    Api: { admin: { services: async () => ({ data: [service] }), specialties: async () => ({ data: [] }), updateService: async (id, body) => { submitted = body; } } },
    Utils: { formatCurrency: String, showToast() {} }
  });
  const html = fs.readFileSync(path.join(root, 'admin/services.html'), 'utf8');
  const script = pageScript('admin/services.html');
  vm.runInContext(fs.readFileSync(path.join(root, 'js/utils.js'), 'utf8'), context);
  vm.runInContext('Utils.showToast = () => {};', context);
  vm.runInContext(script, context);
  await vm.runInContext('load()', context);
  vm.runInContext('edit(1)', context);
  assert.equal(elements.name.value, service.name);
  elements.name.value = 'Tên mới';
  await elements.form.events.submit({ preventDefault() {} });
  assert.equal(submitted.name, 'Tên mới');
});
test('service editor loads Bootstrap before constructing its modal inline', () => {
  const html = fs.readFileSync(path.join(root, 'admin/services.html'), 'utf8');
  assert.match(html, /<script\s+src="[^"]*bootstrap[^\"]*"><\/script>/);
});
function pageContext(extra = {}) {
  const elements = new Map();
  const element = id => {
    if (!elements.has(id)) elements.set(id, { innerHTML: '', value: '', innerText: '', classList: { add() {}, remove() {}, toggle() {} }, focus() {}, events: {}, addEventListener(type, fn) { this.events[type] = fn; } });
    return elements.get(id);
  };
  const documentEvents = {};
  const context = vm.createContext({ window: { addEventListener() {} }, document: { getElementById: element, addEventListener(type, fn) { documentEvents[type] = fn; } },
    Auth: { checkAuth: () => null }, bootstrap: { Modal: class {} }, ...extra });
  vm.runInContext(fs.readFileSync(path.join(root, 'js/utils.js'), 'utf8'), context);
  return { context, element, documentEvents };
}
test('booking button accepts doctor names with apostrophes and displays markup as text', () => {
  const { context, element } = pageContext();
  vm.runInContext(fs.readFileSync(path.join(root, 'js/patient-dashboard.js'), 'utf8'), context);
  vm.runInContext(`allDoctors = [{ userId: 1, fullName: "Dr O'Brian <em>literal</em>", specialtyName: 'General', experienceYears: 2 }]; renderDoctors(allDoctors);`, context);
  const html = element('doctor-grid').innerHTML;
  for (const [, handler] of html.matchAll(/onclick="([^"]*)"/g)) assert.doesNotThrow(() => new vm.Script(handler));
  assert.ok(!html.includes('<em>literal</em>'), 'doctor-entered HTML must be rendered as text');
});
test('medical record cards support apostrophes and escape clinical text', async () => {
  const { context, element } = pageContext({ Api: { patient: { medicalRecords: async () => ({ data: [{ id: 1, diagnosis: "Patient's <img src=x onerror=alert(1)>", doctorNotes: '<script>bad</script>' }] }) } } });
  const html = fs.readFileSync(path.join(root, 'patient/medical-records.html'), 'utf8');
  vm.runInContext(pageScript('patient/medical-records.html'), context);
  await vm.runInContext('loadRecords()', context);
  const cards = element('records-container').innerHTML;
  for (const [, handler] of cards.matchAll(/onclick="([^"]*)"/g)) assert.doesNotThrow(() => new vm.Script(handler));
  assert.ok(!cards.includes('<img'), 'diagnosis must be text');
  assert.ok(!cards.includes('<script>'), 'doctor notes must be text');
});

function pageScript(file) {
  const html = fs.readFileSync(path.join(root, file), 'utf8');
  const inline = [...html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)].map(x => x[1]).filter(x => x.trim());
  if (inline.length) return inline.join('\n');
  const src = [...html.matchAll(/<script[^>]*src="([^"]+)"[^>]*>/g)].map(x => x[1]).find(x => x.includes('/pages/'));
  return fs.readFileSync(path.resolve(root, path.dirname(file), src.split('?')[0]), 'utf8');
}
const attackText = `Patient's "><img src=x onerror=alert(1)>`;
test('patient appointment list and detail escape stored symptoms, doctor names and clinical notes', () => {
  const { context, element } = pageContext({ attackText });
  vm.runInContext(pageScript('patient/appointments.html'), context);
  vm.runInContext(`allAppointments = [{ id: 1, status: 'COMPLETED', bookingCode: attackText, doctorName: attackText, symptoms: attackText, diagnosis: attackText, doctorNotes: attackText }]; detailModal = { show() {} }; renderAppointments(); openDetail(1);`, context);
  for (const id of ['appointments-container', 'detail-content']) {
    const html = element(id).innerHTML;
    assert.ok(!html.includes('<img'), `${id} must render stored HTML as text`);
    assert.ok(html.includes('&lt;img'), `${id} must retain the literal clinical text`);
    for (const [, handler] of html.matchAll(/onclick="([^"]*)"/g)) assert.doesNotThrow(() => new vm.Script(handler));
  }
  assert.ok(element('appointments-container').innerHTML.includes('title="Patient&#39;s &quot;&gt;&lt;img'), 'symptom title must escape attribute quotes');
});
test('doctor appointment list and detail render patient-controlled text without markup', async () => {
  const { context, element } = pageContext({ attackText, Api: { get: async () => ({ data: { diagnosis: attackText, doctorNotes: attackText } }) } });
  vm.runInContext(pageScript('doctor/dashboard.html'), context);
  vm.runInContext(`allAppointments = [{ id: 1, status: 'CONFIRMED', bookingCode: 'BK1', patientName: attackText, patientPhone: attackText, symptoms: attackText }]; patientDetailModal = { show() {} }; renderAppointmentsTable();`, context);
  await vm.runInContext('openPatientDetail(1)', context);
  for (const id of ['doctor-appointments', 'patient-detail-content']) {
    const html = element(id).innerHTML;
    assert.ok(!html.includes('<img'), `${id} must render patient text as text`);
    assert.ok(html.includes('&lt;img'), `${id} must retain the literal patient text`);
    for (const [, handler] of html.matchAll(/onclick="([^"]*)"/g)) assert.doesNotThrow(() => new vm.Script(handler));
  }
});

test('application pages use external scripts and declarative event actions compatible with strict CSP', () => {
  for (const file of fs.readdirSync(root, { recursive: true }).filter(x => x.endsWith('.html') && !/^(login|register|reset-password|verify-email)\.html$/.test(x))) {
    const html = fs.readFileSync(path.join(root, file), 'utf8');
    assert.ok(!/<script\b[^>]*>\s*\S[\s\S]*?<\/script>/.test(html.replace(/<script[^>]*src="[^"]*"[^>]*><\/script>/g, '')), `${file} inline script`);
    assert.ok(!/\son(?:click|input|change|submit|keydown)="/.test(html), `${file} inline handler`);
  }
});
test('specialty names and descriptions render as text even with malicious markup', async () => {
  const { context, element } = pageContext({ Api: { admin: { specialties: async () => ({ data: [{ id: 1, name: attackText, description: attackText }] }) } } });
  vm.runInContext(pageScript('admin/specialties.html'), context);
  await vm.runInContext('loadSpecialties()', context);
  assert.ok(!element('specialties-list').innerHTML.includes('<img'));
});
test('booking requires a review of the slot, fee, location and cancellation policy before sending', async () => {
  let writes = 0;
  const { context, element } = pageContext({ Api: { get: async () => ({ data: { address: 'Địa điểm thật', cancellationHours: 24 } }), appointments: { create: async () => { writes++; return { data: {} }; } } } });
  vm.runInContext(fs.readFileSync(path.join(root, 'js/patient-dashboard.js'), 'utf8'), context);
  vm.runInContext(`allDoctors = [{userId: 1, fullName: 'Bác sĩ', consultationFee: 250000}]; availableSlots = [{id: 2, startTime: '09:00', endTime: '09:30'}];`, context);
  element('selected-doctor-id').value = '1'; element('selected-schedule-id').value = '2';
  await vm.runInContext('reviewBooking()', context);
  assert.equal(writes, 0);
  assert.ok(element('booking-review').innerHTML.includes('250.000'));
  assert.ok(element('booking-review').innerHTML.includes('Địa điểm thật'));
  assert.ok(element('booking-review').innerHTML.includes('24'));
});
test('medical editor warns before leaving with unsaved changes', () => {
  const { context } = pageContext();
  vm.runInContext(pageScript('doctor/dashboard.html'), context);
  const event = { preventDefault() { this.prevented = true; } };
  context.dirtyEvent = event;
  vm.runInContext('medicalDraftDirty = true; warnUnsavedMedicalChanges(dirtyEvent)', context);
  assert.equal(event.prevented, true);
});

test('booking conflicts reload slots, retain symptoms and require another review', async () => {
  let slotsRead = 0;
  const { context, element } = pageContext({ Api: { doctors: { slots: async () => { slotsRead++; return { data: [] }; } }, appointments: { create: async () => { throw Object.assign(new Error('conflict'), { status: 409 }); } } } });
  vm.runInContext(fs.readFileSync(path.join(root, 'js/patient-dashboard.js'), 'utf8'), context);
  element('selected-schedule-id').value = '2'; element('selected-doctor-id').value = '1'; element('booking-date').value = '2026-12-01'; element('symptoms').value = attackText;
  vm.runInContext('bookingReviewed = true;', context);
  await vm.runInContext('submitBooking({preventDefault(){}})', context);
  assert.equal(element('symptoms').value, attackText);
  assert.equal(slotsRead, 1);
  assert.equal(element('selected-schedule-id').value, '');
  assert.equal(vm.runInContext('bookingReviewed', context), false);
});
test('booking sends only one write while confirmation is pending', async () => {
  let writes = 0, release;
  const { context, element } = pageContext({ Api: { appointments: { create: async () => { writes++; await new Promise(resolve => { release = resolve; }); return { data: {} }; } } } });
  vm.runInContext(fs.readFileSync(path.join(root, 'js/patient-dashboard.js'), 'utf8'), context);
  element('selected-schedule-id').value = '1';
  vm.runInContext('bookingReviewed=true; bookingBsModal={hide(){}}; successBsModal={show(){}};', context);
  // Toast rendering is unrelated to submission concurrency.
  vm.runInContext('Utils.showToast = () => {};', context);
  const first = vm.runInContext('submitBooking({preventDefault(){}})', context);
  await vm.runInContext('submitBooking({preventDefault(){}})', context);
  assert.equal(writes, 1);
  release(); await first;
});
test('declarative action dispatcher never executes attacker supplied action names or JSON strings', () => {
  let clicked = 0;
  const { context, documentEvents } = pageContext({ bookDoctor: value => { clicked++; assert.equal(value, attackText); } });
  const source = fs.readFileSync(path.join(root, 'js/ui-actions.js'), 'utf8');
  assert.ok(!/\beval\s*\(|new Function\b/.test(source));
  vm.runInContext(source, context);
  const action = source.match(/(action\d+)\(event\) \{ (?:return )?bookDoctor/)[1];
  const event = { target: { dataset: { uiArg0: JSON.stringify(attackText) }, getAttribute: key => key === 'data-ui-click' ? action : null } };
  documentEvents.click(event);
  assert.equal(clicked, 1);
  event.target.getAttribute = () => 'bookDoctor(1); alert(1)';
  documentEvents.click(event);
  assert.equal(clicked, 1);
});
test('shared form submission blocks duplicates and restores buttons after a retryable failure', async () => {
  const { context } = pageContext();
  let handler, release, count = 0;
  const button = { disabled: false };
  context.form = { addEventListener(type, callback) { handler = callback; }, querySelectorAll: () => [button], setAttribute() {}, removeAttribute() {} };
  context.save = async () => { count++; await new Promise(resolve => { release = resolve; }); };
  vm.runInContext('Utils.onSubmit(form, save)', context);
  const first = handler({ preventDefault() {} });
  await handler({ preventDefault() {} });
  assert.equal(count, 1); assert.equal(button.disabled, true);
  release(); await first;
  assert.equal(button.disabled, false);
});
test('admin fee table uses completed appointments rather than claiming payments received', () => {
  const { context, element } = pageContext();
  vm.runInContext(pageScript('admin/dashboard.html').replace('    loadDashboardData();', ''), context);
  vm.runInContext(`analyticsData={transactions:[{appointmentId:12,bookingCode:'BK12',patientName:'A',doctorName:'D',specialty:'S',workDate:'2026-11-04',startTime:'09:00',amount:250000}]}; renderTxTable();`, context);
  const html = element('transactionRows').innerHTML;
  assert.ok(html.includes('04/11/2026'));
  assert.ok(!html.includes('Đã nhận tiền'));
});
test('admin user management contains no synthetic patient histories or fabricated doctor counters', () => {
  const script = pageScript('admin/users.html');
  assert.ok(!script.includes('generateDocStats'));
  assert.ok(!script.includes('generateDocSchedule'));
});
test('admin doctor schedule fetches a real bounded page and shows an honest empty state', async () => {
  let requested;
  const { context, element } = pageContext({ Api: { get: async url => { requested = url; return { data: [] }; } } });
  // Legacy globals are DOM named properties in a browser.
  context.userForm = { addEventListener() {} };
  vm.runInContext(pageScript('admin/users.html'), context);
  vm.runInContext('schedModal = {show(){}};', context);
  await vm.runInContext('showDoctorSchedule(9, "Bác sĩ")', context);
  assert.equal(requested, '/admin/appointments?doctorId=9&page=0&size=20');
  assert.ok(element('schedTableRows').innerHTML.includes('Chưa có lịch hẹn'));
});
test('admin users load one bounded page and use account status from profile DTOs', async () => {
  let requested;
  const fixture = [{ userId: 99, fullName: attackText, email: 'a@clinic', isActive: true, isApproved: true }];
  const { context, element } = pageContext({ Api: { get: async url => { requested = url; return { data: fixture }; }, admin: { doctors: async () => ({ data: fixture }), users: async () => ({ data: [] }) } } });
  context.userForm = { addEventListener() {} }; context.userRows = element('userRows'); context.tableHead = element('tableHead');
  vm.runInContext(pageScript('admin/users.html'), context);
  await vm.runInContext('load()', context);
  assert.equal(requested, '/admin/doctors?page=0&size=20');
  assert.ok(element('userRows').innerHTML.includes('Hoạt động'));
  assert.ok(!element('userRows').innerHTML.includes('<img'));
});

test('all application HTML avoids inline event attributes under production CSP', () => {
  const failures=[];
  for(const file of fs.readdirSync(root,{recursive:true}).filter(f=>f.endsWith('.html'))) {
    if(/<[^>]+\son[a-z]+\s*=/i.test(fs.readFileSync(path.join(root,file),'utf8'))) failures.push(file);
  }
  assert.deepEqual(failures,[]);
});
