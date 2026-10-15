/* Field worker portal pages. */
'use strict';

(() => {
  const { api, esc, fmtDate, fmtDateTime, badge, proofBadge, priority, toast, empty, busy, pathId } = Civic;
  const $ = s => document.querySelector(s);
  const page = document.body.dataset.page;
  const PIN = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z"/><circle cx="12" cy="9.5" r="2.5"/></svg>';

  const nextAction = a => a.status === 'ASSIGNED'
    ? `<a class="btn btn-sm" href="/worker/complaints/${a.id}/status">Start work</a>`
    : a.status === 'IN_PROGRESS' ? `<a class="btn btn-sm" href="/worker/complaints/${a.id}/proof">Upload proof</a>` : '';

  function taskCard(a) {
    const c = a.complaint;
    return `<article class="task-card prio-${esc((c.priority || '').toLowerCase())}">
      <div class="row"><span class="small muted">#${c.id} · ${esc(c.category)}</span><span class="spacer"></span>${priority(c.priority)} ${badge(a.status)}</div>
      <h3><a href="/worker/complaints/${a.id}">${esc(c.title)}</a></h3>
      <p class="small muted loc">${PIN} ${esc(c.location)}</p>
      ${a.notes ? `<p class="small note-inline"><strong>Admin note:</strong> ${esc(a.notes)}</p>` : ''}
      <div class="row task-actions"><span class="small muted">Assigned ${fmtDate(a.assignedAt)}</span><span class="spacer"></span>
        <a class="btn btn-outline btn-sm" href="/worker/complaints/${a.id}">View</a>${nextAction(a)}</div>
    </article>`;
  }

  const summaryCard = a => `<div class="card">
      <div class="small muted">Complaint #${a.complaint.id} · ${esc(a.complaint.category)}</div>
      <h2 class="detail-title">${esc(a.complaint.title)}</h2>
      <div class="row" style="margin:10px 0">${priority(a.complaint.priority)} ${badge(a.status)}</div>
      <p class="small muted loc">${PIN} ${esc(a.complaint.location)}</p>
      <p class="pre small" style="margin-top:12px">${esc(a.complaint.description)}</p>
    </div>`;

  const loadAssignment = () => api(`/api/worker/assignments/${pathId()}`);

  /* ---------- dashboard ---------- */
  async function dashboard() {
    try {
      const d = await api('/api/worker/dashboard');
      $('#welcomeName').textContent = d.name.split(' ')[0];
      $('#statAssigned').textContent = d.stats.assigned;
      $('#statProgress').textContent = d.stats.inProgress;
      $('#statReview').textContent = d.stats.awaitingReview;
      $('#statDone').textContent = d.stats.approved;
      $('#activeList').innerHTML = d.active.length ? d.active.map(taskCard).join('')
        : empty('No open assignments', 'New complaints assigned to you will appear here.');
    } catch (e) { toast(e.message, 'error'); }
  }

  /* ---------- assigned complaints ---------- */
  async function assigned() {
    const el = $('#taskList');
    try {
      const list = await api('/api/worker/assignments');
      el.innerHTML = list.length ? list.map(taskCard).join('') : empty('You are all caught up', 'There are no open complaints assigned to you right now.');
    } catch (e) { el.innerHTML = empty('Could not load assignments', e.message); }
  }

  /* ---------- complaint details ---------- */
  async function details() {
    const root = $('#detail');
    try {
      const { assignment: a, proofs, pendingReview } = await loadAssignment();
      const c = a.complaint;
      let actions = '';
      if (a.status === 'ASSIGNED') actions = `<a class="btn" href="/worker/complaints/${a.id}/status">Start work</a>`;
      if (a.status === 'IN_PROGRESS') actions = `<a class="btn" href="/worker/complaints/${a.id}/proof">Upload proof of work</a>
        <a class="btn btn-outline" href="/worker/complaints/${a.id}/status">Add progress note</a>`;
      if (a.status === 'COMPLETED') actions = pendingReview ? '<span class="badge b-pending">Proof submitted, waiting for admin review</span>' : '<span class="badge b-resolved">Work approved</span>';
      if (a.status === 'CANCELLED') actions = '<span class="badge b-neutral">This assignment was withdrawn</span>';
      root.innerHTML = `
        <div class="card">
          <div class="card-head"><div><div class="small muted">Complaint #${c.id} · assigned ${fmtDateTime(a.assignedAt)}</div>
            <h2 class="detail-title">${esc(c.title)}</h2></div><div class="row">${priority(c.priority)} ${badge(a.status)}</div></div>
          <div class="row">${actions}</div>
        </div>
        <div class="grid layout-side">
          <div class="stack">
            <div class="card"><h2 class="section-title">What the citizen reported</h2><p class="pre">${esc(c.description)}</p>
              ${c.imagePath ? `<a href="${esc(c.imagePath)}" target="_blank" rel="noopener"><img class="photo" src="${esc(c.imagePath)}" alt="Photo of the reported issue"></a>` : '<p class="small muted">No photo attached.</p>'}</div>
            <div class="card"><h2 class="section-title">Proof history</h2>${proofs.length ? proofs.map(p => `<div class="proof-card">
                <a href="${esc(p.imagePath)}" target="_blank" rel="noopener"><img src="${esc(p.imagePath)}" alt="Proof photo"></a>
                <div><div class="row">${proofBadge(p.status)}<span class="spacer"></span><span class="small muted">${fmtDateTime(p.uploadedAt)}</span></div>
                ${p.remarks ? `<p class="small">${esc(p.remarks)}</p>` : ''}${p.reviewComment ? `<p class="small muted"><strong>Admin:</strong> ${esc(p.reviewComment)}</p>` : ''}</div>
              </div>`).join('') : '<p class="small muted">No proof uploaded yet.</p>'}</div>
          </div>
          <div class="stack">
            <div class="card"><h2 class="section-title">Location & contact</h2><dl class="dl">
              <dt>Location</dt><dd>${esc(c.location)}</dd><dt>Citizen</dt><dd>${esc(c.citizen.name)}</dd>
              ${c.citizen.phone ? `<dt>Phone</dt><dd><a href="tel:${esc(c.citizen.phone)}">${esc(c.citizen.phone)}</a></dd>` : ''}
              <dt>Reported</dt><dd>${fmtDateTime(c.createdAt)}</dd></dl>
              <a class="btn btn-outline btn-block" style="margin-top:16px" target="_blank" rel="noopener"
                 href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(c.location)}">Open in Google Maps</a></div>
            ${a.notes ? `<div class="card note"><h2 class="section-title">Instructions from admin</h2><p>${esc(a.notes)}</p></div>` : ''}
            ${a.workerRemarks ? `<div class="card"><h2 class="section-title">Your latest note</h2><p>${esc(a.workerRemarks)}</p></div>` : ''}
          </div>
        </div>`;
    } catch (e) { root.innerHTML = `<div class="card">${empty('Assignment not found', e.message)}</div>`; }
  }

  /* ---------- update status ---------- */
  async function status() {
    const form = $('#statusForm');
    try {
      const { assignment: a } = await loadAssignment();
      $('#summary').innerHTML = summaryCard(a);
      $('#proofLink').href = `/worker/complaints/${a.id}/proof`;
      if (a.status === 'COMPLETED' || a.status === 'CANCELLED') {
        form.innerHTML = `<p class="muted">This assignment is ${a.status === 'COMPLETED' ? 'completed' : 'withdrawn'} and can no longer be updated.</p>
          <a class="btn btn-outline" href="/worker/complaints/${a.id}">Back to complaint</a>`;
        return;
      }
      form.elements.namedItem('remarks').value = a.workerRemarks || '';
    } catch (e) {
      $('#summary').innerHTML = `<div class="card">${empty('Assignment not found', e.message)}</div>`;
      form.remove();
      return;
    }
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const btn = form.querySelector('[type=submit]');
      busy(btn, true, 'Saving…');
      try {
        await api(`/api/worker/assignments/${pathId()}/status`, { method: 'PUT', body: {
          status: form.elements.namedItem('status').value, remarks: form.elements.namedItem('remarks').value } });
        toast('Status updated', 'success');
        setTimeout(() => { location.href = `/worker/complaints/${pathId()}`; }, 500);
      } catch (err) { toast(err.message, 'error'); busy(btn, false); }
    });
  }

  /* ---------- upload proof ---------- */
  async function proof() {
    const form = $('#proofForm');
    try {
      const { assignment: a, pendingReview } = await loadAssignment();
      $('#summary').innerHTML = summaryCard(a);
      if (a.status === 'CANCELLED' || a.status === 'COMPLETED') {
        form.innerHTML = `<p class="muted">${a.status === 'CANCELLED' ? 'This assignment was withdrawn.' : pendingReview
          ? 'You already submitted proof. The admin will review it soon.' : 'This work has already been approved.'}</p>
          <a class="btn btn-outline" href="/worker/complaints/${a.id}">Back to complaint</a>`;
        return;
      }
    } catch (e) {
      $('#summary').innerHTML = `<div class="card">${empty('Assignment not found', e.message)}</div>`;
      form.remove();
      return;
    }
    Civic.dropzone($('#dropzone'), $('#file'));
    form.addEventListener('submit', async e => {
      e.preventDefault();
      if (!$('#file').files.length) return toast('Please add a photo of the finished work', 'error');
      const btn = form.querySelector('[type=submit]');
      busy(btn, true, 'Uploading…');
      try {
        await api(`/api/worker/assignments/${pathId()}/proof`, { method: 'POST', body: new FormData(form) });
        toast('Proof uploaded. The admin will review it.', 'success');
        setTimeout(() => { location.href = `/worker/complaints/${pathId()}`; }, 600);
      } catch (err) { toast(err.message, 'error'); busy(btn, false); }
    });
  }

  /* ---------- history ---------- */
  async function history() {
    const body = $('#historyBody');
    try {
      const list = await api('/api/worker/history');
      body.innerHTML = list.length ? list.map(({ assignment: a, proof: p }) => `<tr class="clickable" data-href="/worker/complaints/${a.id}">
          <td><strong>#${a.complaint.id}</strong> ${esc(a.complaint.title)}</td><td>${esc(a.complaint.category)}</td>
          <td class="small">${esc(a.complaint.location)}</td><td class="small muted">${fmtDate(a.completedAt)}</td>
          <td>${p ? proofBadge(p.status) : '—'}</td><td class="small muted">${esc((p && p.reviewComment) || '')}</td></tr>`).join('')
        : `<tr><td colspan="6">${empty('No completed work yet', 'Complaints you finish will be listed here.')}</td></tr>`;
    } catch (e) { body.innerHTML = `<tr><td colspan="6">${empty('Could not load history', e.message)}</td></tr>`; }
  }

  const pages = {
    dashboard, assigned, details, status, proof, history,
    profile: () => Civic.initProfile('/api/worker/profile', '/api/worker/password')
  };
  document.addEventListener('DOMContentLoaded', () => pages[page] && pages[page]());
})();
