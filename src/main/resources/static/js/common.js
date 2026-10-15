/* Shared helpers used by every page: API calls, formatting, toasts, layout. */
'use strict';

const Civic = (() => {
  const LOGIN = { citizen: '/login', worker: '/worker/login', admin: '/admin/login' };
  const NOTIF_API = { citizen: '/api/citizen/notifications', worker: '/api/worker/notifications', admin: '/api/admin/notifications' };
  const role = () => document.body.dataset.role;

  /** fetch() wrapper that returns `data` from the ApiResponse envelope or throws a readable error. */
  async function api(url, options = {}) {
    const opts = { credentials: 'same-origin', ...options };
    opts.headers = { Accept: 'application/json', ...(options.headers || {}) };
    if (opts.body && !(opts.body instanceof FormData) && typeof opts.body !== 'string') {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(opts.body);
    }
    let res;
    try {
      res = await fetch(url, opts);
    } catch (e) {
      throw new Error('Cannot reach the server. Is the application running?');
    }
    let json = null;
    try { json = await res.json(); } catch (e) { /* not JSON */ }
    if (res.status === 401 && role()) {
      location.href = LOGIN[role()];
      throw new Error('Your session has expired. Please log in again.');
    }
    if (!res.ok || (json && json.success === false)) {
      throw new Error((json && json.message) || `Request failed (${res.status})`);
    }
    return json ? json.data : null;
  }

  const esc = v => String(v ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  function toDate(v) {
    if (!v) return null;
    if (Array.isArray(v)) return new Date(v[0], v[1] - 1, v[2], v[3] || 0, v[4] || 0, v[5] || 0);
    const d = new Date(String(v).replace(/(\.\d{3})\d+/, '$1'));
    return isNaN(d) ? null : d;
  }
  const fmtDate = v => { const d = toDate(v); return d ? d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : '—'; };
  const fmtDateTime = v => { const d = toDate(v); return d ? d.toLocaleString(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' }) : '—'; };
  function timeAgo(v) {
    const d = toDate(v);
    if (!d) return '';
    const s = (Date.now() - d) / 1000;
    if (s < 60) return 'just now';
    if (s < 3600) return `${Math.floor(s / 60)} min ago`;
    if (s < 86400) return `${Math.floor(s / 3600)} h ago`;
    if (s < 604800) return `${Math.floor(s / 86400)} d ago`;
    return fmtDate(v);
  }

  const STATUS = {
    PENDING: ['Pending', 'b-pending'], ASSIGNED: ['Assigned', 'b-assigned'], IN_PROGRESS: ['In progress', 'b-progress'],
    RESOLVED: ['Resolved', 'b-resolved'], REJECTED: ['Rejected', 'b-rejected'], COMPLETED: ['Completed', 'b-resolved'],
    CANCELLED: ['Cancelled', 'b-neutral'], APPROVED: ['Approved', 'b-resolved'], AVAILABLE: ['Available', 'b-resolved'],
    BUSY: ['Busy', 'b-assigned'], INACTIVE: ['Inactive', 'b-neutral']
  };
  const statusLabel = s => (STATUS[s] || [String(s || '').replace(/_/g, ' ')])[0];
  function badge(status) {
    const [label, cls] = STATUS[status] || [status, 'b-neutral'];
    return `<span class="badge ${cls}">${esc(label)}</span>`;
  }
  const proofBadge = s => s === 'PENDING' ? '<span class="badge b-pending">Awaiting review</span>' : badge(s);
  const priority = p => p ? `<span class="prio prio-${esc(p)}">${esc(p)}</span>` : '';
  const stars = n => '★'.repeat(n) + '☆'.repeat(5 - n);
  const initials = name => (name || '?').split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0].toUpperCase()).join('');
  const empty = (title, text = '') => `<div class="empty"><strong>${esc(title)}</strong>${esc(text)}</div>`;
  const pathId = () => { const m = location.pathname.match(/\/(\d+)(?:\/|$)/); return m ? m[1] : null; };
  const formJson = form => Object.fromEntries(new FormData(form).entries());

  function toast(message, type = 'info') {
    let stack = document.getElementById('toasts');
    if (!stack) {
      stack = document.createElement('div');
      stack.id = 'toasts';
      stack.className = 'toast-stack';
      document.body.appendChild(stack);
    }
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    stack.appendChild(el);
    setTimeout(() => el.remove(), 4200);
  }

  function busy(btn, on, label) {
    if (!btn) return;
    if (on) {
      btn.dataset.label = btn.innerHTML;
      btn.disabled = true;
      btn.textContent = label || 'Please wait…';
    } else {
      btn.disabled = false;
      if (btn.dataset.label) btn.innerHTML = btn.dataset.label;
    }
  }

  /** Status progress bar: Submitted → Assigned → In progress → Resolved. */
  function timeline(status) {
    if (status === 'REJECTED') {
      return '<div class="timeline"><div class="tl-step done"><div class="tl-dot"></div>Submitted</div>' +
        '<div class="tl-step done rejected"><div class="tl-dot"></div>Rejected</div></div>';
    }
    const steps = [['PENDING', 'Submitted'], ['ASSIGNED', 'Assigned'], ['IN_PROGRESS', 'In progress'], ['RESOLVED', 'Resolved']];
    const idx = steps.findIndex(s => s[0] === status);
    return `<div class="timeline">${steps.map((s, i) =>
      `<div class="tl-step ${i <= idx ? 'done' : ''}"><div class="tl-dot"></div>${s[1]}</div>`).join('')}</div>`;
  }

  /** Horizontal bar chart from an object { label: number }. */
  function bars(el, map, labeler = k => k) {
    const entries = Object.entries(map || {});
    if (!entries.length || entries.every(e => !e[1])) { el.innerHTML = empty('No data yet'); return; }
    const max = Math.max(...entries.map(e => e[1]), 1);
    el.innerHTML = entries.map(([k, v]) => `<div class="bar-row"><span class="bar-label" title="${esc(labeler(k))}">${esc(labeler(k))}</span>
      <div class="bar-track"><div class="bar-fill" style="width:${(v / max) * 100}%"></div></div><strong>${v}</strong></div>`).join('');
  }

  /** Image picker with drag & drop and preview. */
  function dropzone(drop, input) {
    const initial = drop.innerHTML;
    const show = file => {
      if (!file) { drop.innerHTML = initial; return; }
      if (!file.type.startsWith('image/')) { toast('Please choose an image file', 'error'); input.value = ''; drop.innerHTML = initial; return; }
      if (file.size > 10 * 1024 * 1024) { toast('Image must be smaller than 10 MB', 'error'); input.value = ''; drop.innerHTML = initial; return; }
      drop.innerHTML = `<img src="${URL.createObjectURL(file)}" alt="Selected photo preview"><span class="small">${esc(file.name)} · click to change</span>`;
    };
    drop.addEventListener('click', () => input.click());
    drop.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); input.click(); } });
    input.addEventListener('change', () => show(input.files[0]));
    ['dragenter', 'dragover'].forEach(t => drop.addEventListener(t, e => { e.preventDefault(); drop.classList.add('drag'); }));
    ['dragleave', 'drop'].forEach(t => drop.addEventListener(t, e => { e.preventDefault(); drop.classList.remove('drag'); }));
    drop.addEventListener('drop', e => {
      const files = e.dataTransfer.files;
      if (files && files[0]) { input.files = files; show(files[0]); }
    });
  }

  function bindPasswordForm(url) {
    const form = document.getElementById('passwordForm');
    if (!form) return;
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const v = n => form.elements.namedItem(n).value;
      if (v('newPassword') !== v('confirmPassword')) return toast('The new passwords do not match', 'error');
      const btn = form.querySelector('[type=submit]');
      busy(btn, true, 'Updating…');
      try {
        await api(url, { method: 'PUT', body: { currentPassword: v('currentPassword'), newPassword: v('newPassword') } });
        toast('Password updated', 'success');
        form.reset();
      } catch (err) { toast(err.message, 'error'); } finally { busy(btn, false); }
    });
  }

  /** Profile page: fills inputs by name, saves fields marked data-editable. */
  async function initProfile(url, passwordUrl) {
    const form = document.getElementById('profileForm');
    try {
      const p = await api(url);
      form.querySelectorAll('input, textarea, select').forEach(i => { if (i.name && i.name in p) i.value = p[i.name] ?? ''; });
      const since = document.getElementById('memberSince');
      if (since) since.textContent = fmtDate(p.createdAt);
    } catch (e) { toast(e.message, 'error'); }
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const body = {};
      form.querySelectorAll('[data-editable]').forEach(i => { body[i.name] = i.value; });
      const btn = form.querySelector('[type=submit]');
      busy(btn, true, 'Saving…');
      try { await api(url, { method: 'PUT', body }); toast('Profile saved', 'success'); loadMe(); }
      catch (err) { toast(err.message, 'error'); } finally { busy(btn, false); }
    });
    bindPasswordForm(passwordUrl);
  }

  async function refreshUnread() {
    const el = document.getElementById('notifCount');
    if (!el || !role()) return;
    try {
      const n = await api(`${NOTIF_API[role()]}/unread-count`);
      el.textContent = n > 99 ? '99+' : n;
      el.hidden = !n;
    } catch (e) { /* ignore */ }
  }

  async function loadMe() {
    try {
      const me = await api('/api/auth/me');
      Civic.me = me;
      const chip = document.getElementById('userChip');
      if (chip) chip.innerHTML = `<span class="avatar">${esc(initials(me.name))}</span><span class="user-name">${esc(me.name)}</span>`;
    } catch (e) { /* handled by api() */ }
  }

  /** Notifications page - same markup for citizens, workers and admins. */
  function initNotificationsPage() {
    const box = document.getElementById('notifList');
    if (!box) return;
    const base = NOTIF_API[role()];
    async function load() {
      try {
        const list = await api(base);
        box.innerHTML = list.length ? list.map(n => `<div class="notif ${n.read ? '' : 'unread'}" data-id="${n.id}">
            <span class="notif-dot lvl-${esc(n.level)}"></span>
            <div class="notif-body"><strong>${esc(n.title)}</strong><p>${esc(n.message)}</p><span class="small muted">${timeAgo(n.createdAt)}</span></div>
            ${n.read ? '' : '<button class="btn btn-outline btn-sm" type="button" data-read>Mark read</button>'}
          </div>`).join('')
          : empty('No notifications yet', 'Updates will show up here.');
      } catch (e) { box.innerHTML = empty('Could not load notifications', e.message); }
    }
    box.addEventListener('click', async e => {
      const b = e.target.closest('[data-read]');
      if (!b) return;
      try { await api(`${base}/${b.closest('.notif').dataset.id}/read`, { method: 'POST' }); await load(); refreshUnread(); }
      catch (err) { toast(err.message, 'error'); }
    });
    const all = document.getElementById('readAll');
    if (all) all.addEventListener('click', async () => {
      try { await api(`${base}/read-all`, { method: 'POST' }); toast('All caught up', 'success'); await load(); refreshUnread(); }
      catch (err) { toast(err.message, 'error'); }
    });
    load();
  }

  function initLayout() {
    if (!role()) return;
    const menu = document.querySelector('[data-menu]');
    if (menu) menu.addEventListener('click', () => document.getElementById('sidebar').classList.toggle('open'));
    document.querySelectorAll('[data-logout]').forEach(b => b.addEventListener('click', async () => {
      try { await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' }); } finally { location.href = LOGIN[role()]; }
    }));
    // whole table rows act as links
    document.addEventListener('click', e => {
      const row = e.target.closest('tr[data-href]');
      if (row && !e.target.closest('a, button')) location.href = row.dataset.href;
    });
    loadMe();
    refreshUnread();
    if (document.body.dataset.page === 'notifications') initNotificationsPage();
  }

  document.addEventListener('DOMContentLoaded', initLayout);

  return {
    api, esc, toDate, fmtDate, fmtDateTime, timeAgo, badge, proofBadge, statusLabel, priority, stars, initials,
    empty, pathId, formJson, toast, busy, timeline, bars, dropzone, initProfile, refreshUnread, me: null
  };
})();
window.Civic = Civic;
