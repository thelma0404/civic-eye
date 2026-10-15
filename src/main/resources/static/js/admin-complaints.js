/* Admin complaints list and complaint details (assign, review proof, change status). */
'use strict';

(() => {
  const { api, esc, fmtDate, fmtDateTime, badge, proofBadge, priority, toast, empty, busy, pathId } = Civic;
  const $ = s => document.querySelector(s);
  const page = document.body.dataset.page;

  /* ---------- list ---------- */
  async function list() {
    const body = $('#adminComplaints');
    const f = { q: $('#fSearch'), status: $('#fStatus'), category: $('#fCategory'), priority: $('#fPriority') };
    const initial = new URLSearchParams(location.search);
    if (initial.get('status')) f.status.value = initial.get('status');
    if (initial.get('priority')) f.priority.value = initial.get('priority');
    try {
      const cats = await api('/api/admin/complaints/categories');
      f.category.insertAdjacentHTML('beforeend', cats.map(c => `<option>${esc(c)}</option>`).join(''));
    } catch (e) { /* filters still work without categories */ }

    async function load() {
      const params = new URLSearchParams();
      Object.entries(f).forEach(([k, el]) => { if (el.value) params.set(k, el.value); });
      body.innerHTML = '<tr><td colspan="7" class="muted">Loading…</td></tr>';
      try {
        const rows = await api(`/api/admin/complaints?${params}`);
        $('#resultCount').textContent = `${rows.length} complaint${rows.length === 1 ? '' : 's'}`;
        body.innerHTML = rows.length ? rows.map(c => `<tr class="clickable" data-href="/admin/complaints/${c.id}">
            <td><strong>#${c.id}</strong></td>
            <td><div class="cell-title">${esc(c.title)}</div><div class="small muted">${esc(c.location)}</div></td>
            <td class="small">${esc(c.citizen ? c.citizen.name : '')}</td><td class="small">${esc(c.category)}</td>
            <td>${priority(c.priority)}</td><td>${badge(c.status)}</td><td class="small muted">${fmtDate(c.createdAt)}</td></tr>`).join('')
          : `<tr><td colspan="7">${empty('No complaints found', 'Try changing the filters.')}</td></tr>`;
      } catch (e) { body.innerHTML = `<tr><td colspan="7">${empty('Could not load complaints', e.message)}</td></tr>`; }
    }

    let timer;
    f.q.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(load, 300); });
    [f.status, f.category, f.priority].forEach(el => el.addEventListener('change', load));
    load();
  }

  /* ---------- details ---------- */
  async function details() {
    const id = pathId();
    const root = $('#detail');
    let data;

    const proofCard = p => `<div class="proof-card">
        <a href="${esc(p.imagePath)}" target="_blank" rel="noopener"><img src="${esc(p.imagePath)}" alt="Proof photo"></a>
        <div><div class="row">${proofBadge(p.status)}<span class="spacer"></span><span class="small muted">${fmtDateTime(p.uploadedAt)}</span></div>
          <p class="small">By <strong>${esc(p.assignment.worker.name)}</strong></p>
          ${p.remarks ? `<p class="small">${esc(p.remarks)}</p>` : ''}
          ${p.reviewComment ? `<p class="small muted">Review note: ${esc(p.reviewComment)}</p>` : ''}
          ${p.status === 'PENDING' ? `<div class="row" style="margin-top:10px">
            <input class="input-sm" placeholder="Comment (required to reject)" data-comment="${p.id}">
            <button class="btn btn-success btn-sm" type="button" data-review="${p.id}" data-approve="true">Approve</button>
            <button class="btn btn-danger btn-sm" type="button" data-review="${p.id}" data-approve="false">Reject</button></div>` : ''}
        </div></div>`;

    function render() {
      const { complaint: c, assignments, activeAssignment: active, proofs, feedback, workers, categories } = data;
      const closed = c.status === 'RESOLVED' || c.status === 'REJECTED';
      const sorted = workers.slice().sort((a, b) => (b.department === c.category) - (a.department === c.category));
      const statusOptions = [['PENDING', 'Move back to pending (unassign)'], ['RESOLVED', 'Mark as resolved'], ['REJECTED', 'Reject complaint']]
        .filter(([s]) => s !== c.status).map(([s, l]) => `<option value="${s}">${l}</option>`).join('');
      document.title = `#${c.id} ${c.title} · Civic Eye Admin`;

      root.innerHTML = `
        <div class="card">
          <div class="card-head"><div><div class="small muted">Complaint #${c.id} · reported ${fmtDateTime(c.createdAt)} by ${esc(c.citizen.name)}</div>
            <h2 class="detail-title">${esc(c.title)}</h2></div><div class="row">${priority(c.priority)} ${badge(c.status)}</div></div>
          ${Civic.timeline(c.status)}
        </div>
        <div class="grid layout-side">
          <div class="stack">
            <div class="card"><h2 class="section-title">Description</h2><p class="pre">${esc(c.description)}</p>
              ${c.imagePath ? `<a href="${esc(c.imagePath)}" target="_blank" rel="noopener"><img class="photo" src="${esc(c.imagePath)}" alt="Photo of the reported issue"></a>` : '<p class="small muted">No photo was attached.</p>'}</div>
            <div class="card"><div class="card-head"><h2>Proof of work</h2><span class="small muted">${proofs.length} submitted</span></div>
              ${proofs.length ? proofs.map(proofCard).join('') : '<p class="small muted">No proof has been uploaded yet.</p>'}</div>
            <div class="card"><h2 class="section-title">Assignment history</h2>${assignments.length ? `<div class="table-wrap"><table class="table">
              <thead><tr><th>Worker</th><th>Status</th><th>Assigned</th><th>Completed</th></tr></thead><tbody>${assignments.map(a => `<tr>
                <td><strong>${esc(a.worker.name)}</strong><div class="small muted">${esc(a.worker.department || '')}</div>
                ${a.workerRemarks ? `<div class="small">“${esc(a.workerRemarks)}”</div>` : ''}</td>
                <td>${badge(a.status)}</td><td class="small">${fmtDateTime(a.assignedAt)}</td>
                <td class="small">${a.completedAt ? fmtDateTime(a.completedAt) : '—'}</td></tr>`).join('')}</tbody></table></div>`
              : '<p class="small muted">Not assigned yet.</p>'}</div>
            ${feedback ? `<div class="card"><h2 class="section-title">Citizen feedback</h2><div class="stars-static">${Civic.stars(feedback.rating)}</div>
              <p>${esc(feedback.comments || 'No comment')}</p><div class="small muted">${fmtDate(feedback.createdAt)}</div></div>` : ''}
          </div>
          <div class="stack">
            <div class="card"><h2 class="section-title">Details</h2><dl class="dl">
              <dt>Location</dt><dd>${esc(c.location)}</dd>
              <dt>Citizen</dt><dd>${esc(c.citizen.name)}<div class="small muted">${esc(c.citizen.email)}${c.citizen.phone ? ' · ' + esc(c.citizen.phone) : ''}</div></dd>
              <dt>AI suggestion</dt><dd>${esc(c.aiCategory || '—')}</dd><dt>Updated</dt><dd>${fmtDateTime(c.updatedAt)}</dd>
              ${c.adminRemarks ? `<dt>Remarks</dt><dd>${esc(c.adminRemarks)}</dd>` : ''}</dl></div>
            ${closed ? '' : `<form class="card" id="assignForm"><h2 class="section-title">${active ? 'Reassign worker' : 'Assign a worker'}</h2>
              ${active ? `<p class="small muted">Currently with <strong>${esc(active.worker.name)}</strong> (${esc(Civic.statusLabel(active.status).toLowerCase())}).</p>` : ''}
              <div class="field"><label for="workerId">Worker</label><select id="workerId" name="workerId" required>
                <option value="">Choose a worker…</option>${sorted.map(w => `<option value="${w.id}">${esc(w.name)} · ${esc(w.department || 'General')} · ${w.status === 'AVAILABLE' ? 'available' : `${w.activeAssignments} open`}</option>`).join('')}
              </select><span class="hint">Workers from the ${esc(c.category)} team are listed first.</span></div>
              <div class="field"><label for="notes">Instructions (optional)</label><textarea id="notes" name="notes" rows="3" placeholder="Anything the worker should know"></textarea></div>
              <button class="btn btn-block" type="submit">${active ? 'Reassign' : 'Assign worker'}</button></form>`}
            <form class="card" id="statusForm"><h2 class="section-title">Update status</h2>
              <div class="field"><label for="newStatus">Status</label><select id="newStatus" name="status">${statusOptions}</select></div>
              <div class="field"><label for="remarks">Remarks for the citizen</label><textarea id="remarks" name="remarks" rows="3" placeholder="Required when rejecting"></textarea></div>
              <button class="btn btn-outline btn-block" type="submit">Update status</button></form>
            <form class="card" id="classForm"><h2 class="section-title">Category & priority</h2>
              <div class="field"><label for="cCategory">Category</label><select id="cCategory" name="category">${categories.map(x => `<option ${x === c.category ? 'selected' : ''}>${esc(x)}</option>`).join('')}</select></div>
              <div class="field"><label for="cPriority">Priority</label><select id="cPriority" name="priority">${['HIGH', 'MEDIUM', 'LOW'].map(p => `<option ${p === c.priority ? 'selected' : ''}>${p}</option>`).join('')}</select></div>
              <button class="btn btn-outline btn-block" type="submit">Save</button></form>
          </div>
        </div>`;
      bindForms();
    }

    async function submit(form, url, method, body, okMessage) {
      const btn = form.querySelector('[type=submit]');
      busy(btn, true, 'Saving…');
      try { await api(url, { method, body }); toast(okMessage, 'success'); await load(); }
      catch (err) { toast(err.message, 'error'); busy(btn, false); }
    }

    function bindForms() {
      const assign = $('#assignForm');
      if (assign) assign.addEventListener('submit', e => {
        e.preventDefault();
        const workerId = assign.elements.namedItem('workerId').value;
        if (!workerId) return toast('Please choose a worker', 'error');
        submit(assign, `/api/admin/complaints/${id}/assign`, 'POST', { workerId: +workerId, notes: assign.elements.namedItem('notes').value }, 'Worker assigned and notified');
      });
      const status = $('#statusForm');
      status.addEventListener('submit', e => {
        e.preventDefault();
        const s = status.elements.namedItem('status').value;
        if (s === 'REJECTED' && !confirm('Reject this complaint? The citizen will be notified.')) return;
        submit(status, `/api/admin/complaints/${id}/status`, 'PUT', { status: s, remarks: status.elements.namedItem('remarks').value }, 'Status updated');
      });
      const cls = $('#classForm');
      cls.addEventListener('submit', e => {
        e.preventDefault();
        submit(cls, `/api/admin/complaints/${id}/classification`, 'PUT', Civic.formJson(cls), 'Category and priority saved');
      });
    }

    root.addEventListener('click', async e => {
      const b = e.target.closest('[data-review]');
      if (!b) return;
      const proofId = b.dataset.review;
      const approve = b.dataset.approve === 'true';
      const comment = root.querySelector(`[data-comment="${proofId}"]`).value;
      if (!approve && !comment.trim()) return toast('Add a comment so the worker knows what to fix', 'error');
      busy(b, true, approve ? 'Approving…' : 'Rejecting…');
      try {
        await api(`/api/admin/proofs/${proofId}/review`, { method: 'POST', body: { approve, comment } });
        toast(approve ? 'Proof approved. Complaint resolved.' : 'Proof rejected. Sent back to the worker.', 'success');
        await load();
      } catch (err) { toast(err.message, 'error'); busy(b, false); }
    });

    async function load() {
      try { data = await api(`/api/admin/complaints/${id}`); render(); }
      catch (e) { root.innerHTML = `<div class="card">${empty('Complaint not found', e.message)}</div>`; }
    }
    load();
  }

  document.addEventListener('DOMContentLoaded', () => {
    if (page === 'complaints') list();
    if (page === 'complaint-details') details();
  });
})();
