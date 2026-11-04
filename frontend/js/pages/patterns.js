document.getElementById('refresh').addEventListener('click', async () => {
    const status = document.getElementById('status');
    const inbox = document.getElementById('inbox');
    const user = Auth.getUser();
    if (!user || user.role !== 'ADMIN') { status.textContent = 'Cần đăng nhập tài khoản Admin trước khi tải hộp thư.'; return; }
    const button = document.getElementById('refresh');
    button.disabled = true;
    try {
      const result = await Api.get('/admin/demo-notifications');
      inbox.replaceChildren();
      for (const mail of result.data) {
        const article = document.createElement('article');
        article.className = 'mail';
        const title = document.createElement('h3'); title.textContent = mail.subject;
        const recipient = document.createElement('p'); recipient.textContent = mail.time + ' · ' + mail.recipient;
        const body = document.createElement('p'); body.textContent = mail.body;
        article.append(title, recipient, body); inbox.append(article);
      }
      status.textContent = result.data.length ? `${result.data.length} thông báo gần nhất (tối đa 100).` : 'Chưa có thông báo. Hãy đặt một lịch khám trước.';
    } catch (error) { status.textContent = error.message; }
    finally { button.disabled = false; }
  });
