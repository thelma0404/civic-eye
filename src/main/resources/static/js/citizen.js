/* Citizen portal pages. */
'use strict';

(() => {
  const { api, esc, fmtDate, fmtDateTime, timeAgo, badge, priority, toast, empty, busy, pathId } = Civic;
  const $ = s => document.querySelector(s);
  const page = document.body.dataset.page;

  const complaintRows = list => list.map(c => `<tr class="clickable" data-href="/citizen/complaints/${c.id}">
      <td><strong>#${c.id}</strong></td>
      <td><div class="cell-title">${esc(c.title)}</div><div class="small muted">${esc(c.location)}</div></td>
      <td>${esc(c.category)}</td><td>${priority(c.priority)}</td><td>${badge(c.status)}</td>
      <td class="small muted">${fmtDate(c.createdAt)}</td></tr>`).join('');

  /* ---------- dashboard ---------- */
  async function dashboard() {
    try {
      const d = await api('/api/citizen/dashboard');
      $('#welcomeName').textContent = d.name.split(' ')[0];
      $('#statTotal').textContent = d.stats.total;
      $('#statPending').textContent = d.stats.pending;
      $('#statActive').textContent = d.stats.active;
      $('#statResolved').textContent = d.stats.resolved;
      $('#recentBody').innerHTML = d.recent.length ? complaintRows(d.recent)
        : `<tr><td colspan="6">${empty('No complaints yet', 'Report your first civic issue to get started.')}</td></tr>`;
      $('#recentNotifs').innerHTML = d.notifications.length ? d.notifications.map(n => `<div class="list-item"><div>
          <strong class="small">${esc(n.title)}</strong><div class="small muted">${esc(n.message)}</div>
          <div class="small muted">${timeAgo(n.createdAt)}</div></div></div>`).join('')
        : empty('You are all caught up');
    } catch (e) { toast(e.message, 'error'); }
  }

  /* ---------- report an issue ---------- */
  async function report() {
    const form = $('#reportForm');
    try {
      const cats = await api('/api/citizen/complaints/categories');
      $('#category').insertAdjacentHTML('beforeend', cats.map(c => `<option>${esc(c)}</option>`).join(''));
    } catch (e) { /* the AI option still works */ }
    Civic.dropzone($('#dropzone'), $('#image'));

    // live AI suggestion
    let timer;
    const aiBox = $('#aiResult');
    const classify = () => {
      clearTimeout(timer);
      timer = setTimeout(async () => {
        const title = $('#title').value.trim();
        const description = $('#description').value.trim();
        if ((title + description).length < 12) {
          aiBox.innerHTML = '<p class="small muted">Start describing the issue and a category and priority will be suggested here.</p>';
          return;
        }
        try {
          const r = await api('/api/citizen/complaints/classify', { method: 'POST', body: { title, description } });
          aiBox.innerHTML = `<dl class="dl small"><dt>Category</dt><dd>${esc(r.category)}</dd><dt>Priority</dt><dd>${priority(r.priority)}</dd>
            <dt>Confidence</dt><dd>${Math.round(r.confidence * 100)}%</dd></dl>
            ${r.matchedKeywords.length ? `<p class="small muted" style="margin-top:10px">Based on: ${r.matchedKeywords.map(esc).join(', ')}</p>` : ''}`;
        } catch (e) { /* ignore while typing */ }
      }, 400);
    };
    $('#title').addEventListener('input', classify);
    $('#description').addEventListener('input', classify);

    $('#useLocation').addEventListener('click', () => {
      if (!navigator.geolocation) return toast('Location is not available in this browser', 'error');
      navigator.geolocation.getCurrentPosition(p => {
        const loc = $('#location');
        const coords = `${p.coords.latitude.toFixed(5)}, ${p.coords.longitude.toFixed(5)}`;
        loc.value = loc.value ? `${loc.value} (${coords})` : coords;
      }, () => toast('Could not get your location. Please type it instead.', 'error'));
    });

    form.addEventListener('submit', async e => {
      e.preventDefault();
      const btn = form.querySelector('[type=submit]');
      busy(btn, true, 'Submitting…');
      try {
        const c = await api('/api/citizen/complaints', { method: 'POST', body: new FormData(form) });
        location.href = `/citizen/complaints/${c.id}?new=1`;
      } catch (err) { toast(err.message, 'error'); busy(btn, false); }
    });
  }

  /* ---------- my complaints ---------- */
  async function complaints() {
    let all = [];
    let filter = 'ALL';
    const body = $('#complaintsBody');
    const search = $('#search');
    const render = () => {
      const q = search.value.trim().toLowerCase();
      const list = all.filter(c =>
        (filter === 'ALL' || c.status === filter || (filter === 'ACTIVE' && ['ASSIGNED', 'IN_PROGRESS'].includes(c.status))) &&
        (!q || `${c.id} ${c.title} ${c.location} ${c.category}`.toLowerCase().includes(q)));
      body.innerHTML = list.length ? complaintRows(list) : `<tr><td colspan="6">${all.length
        ? empty('No complaints match your filters')
        : empty('You have not reported any issues yet', 'Use "Report an issue" to submit your first complaint.')}</td></tr>`;
    };
    document.querySelectorAll('.chip').forEach(ch => ch.addEventListener('click', () => {
      document.querySelectorAll('.chip').forEach(x => x.classList.remove('active'));
      ch.classList.add('active');
      filter = ch.dataset.filter;
      render();
    }));
    search.addEventListener('input', render);
    try { all = await api('/api/citizen/complaints'); render(); }
    catch (e) { body.innerHTML = `<tr><td colspan="6">${empty('Could not load complaints', e.message)}</td></tr>`; }
  }

  /* ---------- feedback helpers ---------- */
  const starInput = () => `<div class="stars" data-stars><input type="hidden" name="rating" value="0">${[1, 2, 3, 4, 5]
    .map(i => `<button type="button" class="star" data-v="${i}" aria-label="${i} star${i > 1 ? 's' : ''}">★</button>`).join('')}</div>`;
  const feedbackForm = id => `<form class="fb-form" data-complaint="${id}">${starInput()}
      <div class="field" style="margin-top:10px"><textarea name="comments" maxlength="1000" placeholder="How was the fix? (optional)"></textarea></div>
      <button class="btn btn-sm" type="submit">Submit feedback</button></form>`;

  document.addEventListener('click', e => {
    const s = e.target.closest('.star');
    if (!s) return;
    const wrap = s.closest('[data-stars]');
    const v = +s.dataset.v;
    wrap.querySelector('input').value = v;
    wrap.querySelectorAll('.star').forEach(x => x.classList.toggle('on', +x.dataset.v <= v));
  });
  document.addEventListener('submit', async e => {
    const f = e.target.closest('.fb-form');
    if (!f) return;
    e.preventDefault();
    const rating = +f.querySelector('[name=rating]').value;
    if (!rating) return toast('Please choose a star rating', 'error');
    const btn = f.querySelector('[type=submit]');
    busy(btn, true, 'Sending…');
    try {
      await api('/api/citizen/feedback', { method: 'POST', body: { complaintId: +f.dataset.complaint, rating, comments: f.querySelector('[name=comments]').value } });
      toast('Thank you for your feedback!', 'success');
      setTimeout(() => location.reload(), 700);
    } catch (err) { toast(err.message, 'error'); busy(btn, false); }
  });

  /* ---------- complaint details ---------- */
  async function details() {
    const root = $('#detail');
    if (new URLSearchParams(location.search).has('new')) toast('Complaint submitted. You will be notified as it progresses.', 'success');
    try {
      const d = await api(`/api/citizen/complaints/${pathId()}`);
      const c = d.complaint;
      const a = d.assignment;
      document.title = `#${c.id} ${c.title} · Civic Eye`;
      root.innerHTML = `
        <div class="card">
          <div class="card-head"><div><div class="small muted">Complaint #${c.id} · ${fmtDateTime(c.createdAt)}</div>
            <h2 class="detail-title">${esc(c.title)}</h2></div>${badge(c.status)}</div>
          ${Civic.timeline(c.status)}
        </div>
        <div class="grid layout-side">
          <div class="stack">
            <div class="card"><h2 class="section-title">What you reported</h2><p class="pre">${esc(c.description)}</p>
              ${c.imagePath ? `<a href="${esc(c.imagePath)}" target="_blank" rel="noopener"><img class="photo" src="${esc(c.imagePath)}" alt="Photo of the reported issue"></a>` : ''}</div>
            ${c.adminRemarks ? `<div class="card note"><h2 class="section-title">Remarks from the municipality</h2><p>${esc(c.adminRemarks)}</p></div>` : ''}
            ${d.proofs.length ? `<div class="card"><h2 class="section-title">Proof of completed work</h2><div class="proof-grid">${d.proofs.map(p => `
              <figure><a href="${esc(p.imagePath)}" target="_blank" rel="noopener"><img class="photo" src="${esc(p.imagePath)}" alt="Photo of the completed work"></a>
              <figcaption class="small muted">${esc(p.remarks || '')} ${fmtDate(p.uploadedAt)}</figcaption></figure>`).join('')}</div></div>` : ''}
            ${c.status === 'RESOLVED' ? `<div class="card"><h2 class="section-title">Your feedback</h2>${d.feedback
              ? `<div class="stars-static">${Civic.stars(d.feedback.rating)}</div>${d.feedback.comments ? `<p>${esc(d.feedback.comments)}</p>` : ''}<div class="small muted">${fmtDate(d.feedback.createdAt)}</div>`
              : `<p class="small muted">Your issue has been resolved. How did we do?</p>${feedbackForm(c.id)}`}</div>` : ''}
          </div>
          <div class="stack">
            <div class="card"><h2 class="section-title">Summary</h2><dl class="dl">
              <dt>Category</dt><dd>${esc(c.category)}</dd><dt>Priority</dt><dd>${priority(c.priority)}</dd>
              <dt>Location</dt><dd>${esc(c.location)}</dd><dt>Last update</dt><dd>${fmtDateTime(c.updatedAt)}</dd>
              ${c.resolvedAt ? `<dt>Resolved</dt><dd>${fmtDateTime(c.resolvedAt)}</dd>` : ''}</dl></div>
            <div class="card"><h2 class="section-title">Field worker</h2>${a
              ? `<div class="person"><span class="avatar">${esc(Civic.initials(a.worker.name))}</span><div><strong>${esc(a.worker.name)}</strong>
                 <div class="small muted">${esc(a.worker.department || 'Field team')}</div></div></div>
                 <p class="small muted" style="margin-top:12px">Assigned ${fmtDateTime(a.assignedAt)}</p>`
              : '<p class="small muted">No worker assigned yet. You will be notified as soon as someone is.</p>'}</div>
          </div>
        </div>`;
    } catch (e) { root.innerHTML = `<div class="card">${empty('Complaint not found', e.message)}</div>`; }
  }

  /* ---------- feedback page ---------- */
  async function feedback() {
    try {
      const [pending, mine] = await Promise.all([api('/api/citizen/feedback/pending'), api('/api/citizen/feedback')]);
      $('#pendingFb').innerHTML = pending.length ? pending.map(c => `<div class="fb-item">
          <a href="/citizen/complaints/${c.id}"><strong>#${c.id} ${esc(c.title)}</strong></a>
          <div class="small muted">${esc(c.category)} · resolved ${fmtDate(c.resolvedAt)}</div>${feedbackForm(c.id)}</div>`).join('')
        : empty('Nothing waiting for a rating', 'Resolved complaints will appear here so you can rate the service.');
      $('#myFb').innerHTML = mine.length ? mine.map(f => `<div class="list-item"><div style="flex:1">
          <div class="row"><a href="/citizen/complaints/${f.complaint.id}"><strong>#${f.complaint.id} ${esc(f.complaint.title)}</strong></a>
          <span class="spacer"></span><span class="stars-static">${Civic.stars(f.rating)}</span></div>
          ${f.comments ? `<p class="small">${esc(f.comments)}</p>` : ''}<div class="small muted">${fmtDate(f.createdAt)}</div></div></div>`).join('')
        : empty('No feedback given yet');
    } catch (e) { toast(e.message, 'error'); }
  }

  const pages = {
    dashboard, report, complaints, details, feedback,
    profile: () => Civic.initProfile('/api/citizen/profile', '/api/citizen/password')
  };
  document.addEventListener('DOMContentLoaded', () => pages[page] && pages[page]());
})();
