/* Citizen login, citizen registration, worker login and admin login. */
'use strict';

(() => {
  const page = document.body.dataset.page;
  const form = document.getElementById('authForm');
  const alertBox = document.getElementById('alert');
  const ENDPOINT = {
    login: '/api/auth/login',
    register: '/api/auth/register',
    'worker-login': '/api/auth/worker/login',
    'admin-login': '/api/admin/auth/login'
  };

  function show(message, type = 'error') {
    alertBox.textContent = message;
    alertBox.className = `alert alert-${type} show`;
  }

  if (new URLSearchParams(location.search).has('registered')) {
    show('Your account is ready. Log in to continue.', 'success');
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    alertBox.className = 'alert';
    const data = Civic.formJson(form);
    if (!data.email || !data.password) return show('Please enter your email and password.');
    if (page === 'register') {
      if (!data.name || !data.name.trim()) return show('Please enter your name.');
      if (data.password.length < 6) return show('Password must be at least 6 characters.');
      if (data.password !== data.confirmPassword) return show('The passwords do not match.');
      delete data.confirmPassword;
    }
    const btn = form.querySelector('[type=submit]');
    Civic.busy(btn, true, page === 'register' ? 'Creating account…' : 'Logging in…');
    try {
      const res = await Civic.api(ENDPOINT[page], { method: 'POST', body: data });
      location.href = page === 'register' ? '/login?registered=1' : res.redirectUrl;
    } catch (err) {
      show(err.message);
      Civic.busy(btn, false);
    }
  });
})();
