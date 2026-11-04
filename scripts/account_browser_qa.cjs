// Exercises the real account UI against the isolated demo inbox; never sends external email.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
(async () => {
 const base = process.env.CLINIC_BASE_URL || 'http://localhost:18080';
 assert.match(base, /^http:\/\/(localhost|127\.0\.0\.1):18080$/, 'Account fixture is local demo only');
 const browser = await chromium.launch({headless:true,channel:'chrome'});
 const context = await browser.newContext();const page = await context.newPage();const errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 const email=`account-${Date.now()}@example.test`,password='AccountPassword123',changed='ChangedAccountPassword123';
 try {
  const admin = await context.request.post(base+'/api/v1/auth/login',{headers:{'X-Auth-Client':'api'},data:{email:'admin@clinic',password:'admin@2026'}});
  assert.equal(admin.status(),200);const adminToken=(await admin.json()).data.accessToken;
  async function inboxLink(fragment) {
   for(let i=0;i<50;i++) {
    const response=await context.request.get(base+'/api/v1/admin/demo-notifications',{headers:{Authorization:'Bearer '+adminToken}});
    assert.equal(response.status(),200);
    const item=(await response.json()).data.find(m=>m.recipient===email && m.body.includes(fragment));
    if(item)return item.body.match(/https?:\/\/\S+/)[0];
    await page.waitForTimeout(100);
   }
   throw new Error('Account link was not delivered to the demo inbox');
  }
  await page.goto(base+'/register.html');
  await page.locator('#email').fill(email);await page.locator('#password').fill(password);
  await page.locator('#fullName').fill('Account browser test');await page.locator('#phone').fill('09'+String(Date.now()).slice(-8));
  const registered=page.waitForResponse(r=>r.url().endsWith('/api/v1/auth/register')&&r.request().method()==='POST');
  await page.locator('#btn-submit').click();assert.equal((await registered).status(),201);await page.waitForURL('**/login.html');
  const verification=await inboxLink('/verify-email.html');await page.goto(verification);assert.equal(new URL(page.url()).hash,'');
  const verified=page.waitForResponse(r=>r.url().endsWith('/api/v1/auth/verify-email')&&r.request().method()==='POST');
  await page.locator('#account-submit').click();assert.equal((await verified).status(),200);
  await page.goto(base+'/login.html');await page.locator('#email').fill(email);await page.locator('#forgot-link').click();
  await page.locator('#forgot-modal').waitFor({state:'visible'});
  const requested=page.waitForResponse(r=>r.url().endsWith('/api/v1/auth/forgot-password')&&r.request().method()==='POST');
  await page.locator('#btn-forgot-send').click();assert.equal((await requested).status(),200);
  const reset=await inboxLink('/reset-password.html');const token=new URLSearchParams(new URL(reset).hash.slice(1)).get('token');
  await page.goto(reset);assert.equal(new URL(page.url()).hash,'');await page.locator('#new-password').fill(changed);
  const resetDone=page.waitForResponse(r=>r.url().endsWith('/api/v1/auth/reset-password')&&r.request().method()==='POST');
  await page.locator('#account-submit').click();assert.equal((await resetDone).status(),200);
  const replay=await context.request.post(base+'/api/v1/auth/reset-password',{data:{token,password:changed}});assert.equal(replay.status(),400);
  const old=await context.request.post(base+'/api/v1/auth/login',{headers:{'X-Auth-Client':'api'},data:{email,password}});assert.equal(old.status(),401);
  await page.goto(base+'/login.html');await page.locator('#email').fill(email);await page.locator('#password').fill(changed);await page.locator('#btn-submit').click();
  await page.waitForURL('**/patient/dashboard.html');
  assert.deepEqual(await page.evaluate(()=>[localStorage.getItem('accessToken'),localStorage.getItem('refreshToken')]),[null,null]);
  assert.deepEqual(errors,[]);
  const result={checks:['register through UI','verify delivered link and remove URL token','request and reset password through UI','reject consumed token and old password; login with new password without persisted credentials'],errors,scope:'Local demo/H2 inbox; backend token tests and real SMTP integration run separately.'};
  if(process.env.CLINIC_ACCOUNT_QA_OUTPUT)fs.writeFileSync(process.env.CLINIC_ACCOUNT_QA_OUTPUT,JSON.stringify(result,null,2)+'\n');
  console.log(JSON.stringify(result,null,2));
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
