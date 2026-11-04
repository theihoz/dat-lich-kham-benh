const accountMode = document.body.dataset.accountMode;
const token = new URLSearchParams(window.location.hash.slice(1)).get('token') || '';
if (window.location.hash) window.history.replaceState(null, '', window.location.pathname);
const form = document.getElementById('account-form');
const message = document.getElementById('account-message');
const submit = document.getElementById('account-submit');
function show(text, error = false) { message.textContent = text; message.className = error ? 'alert alert-danger' : 'alert alert-info'; message.focus(); }
if ((accountMode === 'reset' || accountMode === 'verify') && !token) {
  show('Liên kết thiếu token. Vui lòng yêu cầu một liên kết mới.', true); submit.disabled = true;
}
form.addEventListener('submit', async event => {
 event.preventDefault(); submit.disabled = true;
 try {
  let result;
  if (accountMode === 'reset') result = await Api.auth.resetPassword({token, password: document.getElementById('new-password').value});
  else if (accountMode === 'verify') result = await Api.auth.verifyEmail({token});
  else result = await Api.auth.resendVerification({email: document.getElementById('account-email').value});
  show(result.message); form.hidden = accountMode !== 'resend';
 } catch (error) { show(error.message, true); }
 finally { submit.disabled = false; }
});
