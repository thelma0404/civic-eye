/* Admin: users & workers, sending notifications, feedback, reports, profile. */
'use strict';

(() => {
  const { api, esc, fmtDate, timeAgo, badge, toast, empty, busy } = Civic;
  const $ = s => document.querySelector(s);
  const page = document.body.dataset.page;

  /* ---------- users & workers ---------- */
  function users() {
    const tabs = document.querySelectorAll('.tab');
    const activate = name => {
      tabs.forEach(t => t.classList.toggle('active', t.dataset.tab === name));
      document.querySelectorAll('[data-panel]').forEach(p => { p.hidden = p.dataset.panel !== name; });
    };
    tabs.forEach(t => t.addEventListener('click', () => { activate(t.dataset.tab); history.replaceState(null, '', `#${t.dataset.tab}`); }));
    if (location.hash === '#workers') activate('workers');

    // citizens
    let citizens = [];
    const renderCitizens = () => {
      const q = $('#citizenSearch').value.trim().toLowerCase();
      const rows = citizens.filter(u => !q || `${u.name} ${u.email} ${u.phone || ''}`.toLowerCase().includes(q));
      $('#citizenBody').innerHTML = rows.length ? rows.map(u => `<tr>
          <td><div class="person"><span class="avatar">${esc(Civic.initials(u.name))}</span><div><strong>${esc(u.name)}</strong>
            <div class="small muted">${esc(u.email)}</div></div></div></td>
          <td class="small">${esc(u.phone || '—')}</td><td>${u.complaints}</td><td class="small muted">${fmtDate(u.createdAt)}</td>
          <td>${u.active ? '<span class="badge b-resolved">Active</span>' : '<span class="badge b-neutral">Deactivated</span>'}</td>
          <td class="text-right"><button class="btn btn-outline btn-sm" type="button" data-toggle-user="${u.id}">${u.active ? 'Deactivate' : 'Activate'}</button></td></tr>`).join('')
        : `<tr><td colspan="6">${empty(citizens.length ? 'No citizens match your search' : 'No citizens registered yet')}</td></tr>`;
    };
    const loadCitizens = async () => {
      try { citizens = await api('/api/admin/users'); renderCitizens(); }
      catch (e) { $('#citizenBody').innerHTML = `<tr><td colspan="6">${empty('Could not load citizens', e.message)}</td></tr>`; }
    };
    $('#citizenSearch').addEventListener('input', renderCitizens);
    $('#citizenBody').addEventListener('click', async e => {
      const b = e.target.closest('[data-toggle-user]');
      if (!b) return;
      try {
        const u = await api(`/api/admin/users/${b.dataset.toggleUser}/toggle-active`, { method: 'PUT' });
        toast(`${u.name} is now ${u.active ? 'active' : 'deactivated'}`, 'success');
        loadCitizens();
      } catch (err) { toast(err.message, 'error'); }
    });

    // workers
    let workers = [];
    const modal = $('#workerModal');
    const wf = $('#workerForm');
    const el = n => wf.elements.namedItem(n);
    const loadWorkers = async () => {
      try {
        workers = await api('/api/admin/workers');
        $('#workerBody').innerHTML = workers.length ? workers.map(w => `<tr>
            <td><div class="person"><span class="avatar">${esc(Civic.initials(w.name))}</span><div><strong>${esc(w.name)}</strong>
              <div class="small muted">${esc(w.email)}</div></div></div></td>
            <td class="small">${esc(w.phone || '—')}</td><td class="small">${esc(w.department || '—')}</td><td>${badge(w.status)}</td>
            <td class="small">${w.activeAssignments} open · ${w.completedAssignments} done</td>
            <td class="text-right"><button class="btn btn-outline btn-sm" type="button" data-edit-worker="${w.id}">Edit</button></td></tr>`).join('')
          : `<tr><td colspan="6">${empty('No workers yet', 'Add your first field worker.')}</td></tr>`;
      } catch (e) { $('#workerBody').innerHTML = `<tr><td colspan="6">${empty('Could not load workers', e.message)}</td></tr>`; }
    };
    const openModal = w => {
      wf.reset();
      $('#workerModalTitle').textContent = w ? 'Edit worker' : 'Add field worker';
      el('id').value = w ? w.id : '';
      ['name', 'email', 'phone', 'department'].forEach(k => { el(k).value = w ? (w[k] || '') : ''; });
      el('status').value = w ? w.status : 'AVAILABLE';
      el('password').placeholder = w ? 'Leave blank to keep the current password' : 'At least 6 characters';
      $('#statusField').hidden = !w;
      modal.classList.add('open');
      el('name').focus();
    };
    $('#addWorker').addEventListener('click', () => openModal(null));
    modal.addEventListener('click', e => { if (e.target === modal || e.target.closest('[data-close]')) modal.classList.remove('open'); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') modal.classList.remove('open'); });
    $('#workerBody').addEventListener('click', e => {
      const b = e.target.closest('[data-edit-worker]');
      if (b) openModal(workers.find(w => String(w.id) === b.dataset.editWorker));
    });
    wf.addEventListener('submit', async e => {
      e.preventDefault();
      const d = Civic.formJson(wf);
      const id = d.id;
      delete d.id;
      if (!d.password) delete d.password;
      if (!id) delete d.status;
      const btn = wf.querySelector('[type=submit]');
      busy(btn, true, 'Saving…');
      try {
        await api(id ? `/api/admin/workers/${id}` : '/api/admin/workers', { method: id ? 'PUT' : 'POST', body: d });
        toast(id ? 'Worker updated' : 'Worker added. Share their email and password with them.', 'success');
        modal.classList.remove('open');
        loadWorkers();
      } catch (err) { toast(err.message, 'error'); } finally { busy(btn, false); }
    });
    api('/api/admin/complaints/categories').then(cats => {
      $('#deptList').innerHTML = cats.map(c => `<option value="${esc(c)}">`).join('');
    }).catch(() => {});

    loadCitizens();
    loadWorkers();
  }

  /* ---------- send notifications ---------- */
  function notifications() {
    const f = $('#sendForm');
    f.addEventListener('submit', async e => {
      e.preventDefault();
      const btn = f.querySelector('[type=submit]');
      busy(btn, true, 'Sending…');
      try {
        const n = await api('/api/admin/notifications/send', { method: 'POST', body: Civic.formJson(f) });
        toast(`Sent to ${n} recipient${n === 1 ? '' : 's'}`, 'success');
        f.reset();
      } catch (err) { toast(err.message, 'error'); } finally { busy(btn, false); }
    });
  }

  /* ---------- feedback ---------- */
  async function feedback() {
    try {
      const d = await api('/api/admin/feedback');
      const dist = d.distribution;
      $('#avgRating').textContent = d.total ? d.averageRating.toFixed(1) : '—';
      $('#avgStars').textContent = Civic.stars(Math.round(d.averageRating));
      $('#fbTotal').textContent = d.total;
      $('#fbPositive').textContent = d.total ? `${Math.round(((dist['5'] || 0) + (dist['4'] || 0)) * 100 / d.total)}%` : '—';
      Civic.bars($('#fbDist'), dist, k => `${k} star${k === '1' ? '' : 's'}`);
      $('#fbList').innerHTML = d.feedback.length ? d.feedback.map(f => `<div class="list-item"><div style="flex:1">
          <div class="row"><a href="/admin/complaints/${f.complaint.id}"><strong>#${f.complaint.id} ${esc(f.complaint.title)}</strong></a>
            <span class="spacer"></span><span class="stars-static">${Civic.stars(f.rating)}</span></div>
          ${f.comments ? `<p class="small" style="margin:6px 0">${esc(f.comments)}</p>` : ''}
          <div class="small muted">${esc(f.citizen.name)} · ${timeAgo(f.createdAt)}</div></div></div>`).join('')
        : empty('No feedback yet', 'Citizens can rate resolved complaints.');
    } catch (e) { toast(e.message, 'error'); }
  }

  /* ---------- reports ---------- */
  function reports() {
    const f = $('#reportFilter');
    const from = f.elements.namedItem('from');
    const to = f.elements.namedItem('to');
    const iso = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    from.value = iso(new Date(Date.now() - 29 * 864e5));
    to.value = iso(new Date());

    async function load() {
      const params = new URLSearchParams({ from: from.value, to: to.value });
      $('#exportBtn').href = `/api/admin/reports/export?${params}`;
      try {
        const r = await api(`/api/admin/reports?${params}`);
        $('#rangeLabel').textContent = `${fmtDate(r.from)} – ${fmtDate(r.to)}`;
        $('#rTotal').textContent = r.totalComplaints;
        $('#rResolved').textContent = r.resolved;
        $('#rRate').textContent = `${r.resolutionRate}%`;
        $('#rHours').textContent = r.avgResolutionHours ? `${r.avgResolutionHours} h` : '—';
        $('#rRating').textContent = r.feedbackCount ? `${r.averageRating} / 5 from ${r.feedbackCount} reviews` : 'No reviews in this period';
        Civic.bars($('#rStatus'), r.byStatus, Civic.statusLabel);
        Civic.bars($('#rCategory'), r.byCategory);
        Civic.bars($('#rPriority'), r.byPriority, k => k.charAt(0) + k.slice(1).toLowerCase());
        $('#rWorkers').innerHTML = r.workerPerformance.length ? r.workerPerformance.map(w => `<tr>
            <td><strong>${esc(w.name)}</strong><div class="small muted">${esc(w.department || '')}</div></td>
            <td>${w.assigned}</td><td>${w.completed}</td><td>${w.open}</td><td>${w.completionRate}%</td>
            <td>${w.avgCompletionHours ? `${w.avgCompletionHours} h` : '—'}</td></tr>`).join('')
          : `<tr><td colspan="6">${empty('No assignments in this period')}</td></tr>`;
      } catch (e) { toast(e.message, 'error'); }
    }
    f.addEventListener('submit', e => { e.preventDefault(); load(); });
    $('#printBtn').addEventListener('click', () => window.print());
    load();
  }

  const pages = {
    users, notifications, feedback, reports,
    profile: () => Civic.initProfile('/api/admin/profile', '/api/admin/password')
  };
  document.addEventListener('DOMContentLoaded', () => pages[page] && pages[page]());
})();
