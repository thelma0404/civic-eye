/* Admin dashboard: stats, charts, recent complaints and proofs to review. */
'use strict';

(() => {
  const { api, esc, fmtDate, timeAgo, badge, priority, toast, empty } = Civic;
  const $ = s => document.querySelector(s);

  function columns(el, map) {
    const entries = Object.entries(map || {});
    const max = Math.max(...entries.map(e => e[1]), 1);
    el.innerHTML = entries.map(([day, v]) => {
      const label = new Date(`${day}T00:00:00`).toLocaleDateString(undefined, { weekday: 'short' });
      return `<div class="col" title="${esc(day)}: ${v}"><strong>${v}</strong>
        <div class="col-bar" style="height:${(v / max) * 100}%"></div><small>${esc(label)}</small></div>`;
    }).join('');
  }

  async function load() {
    try {
      const d = await api('/api/admin/dashboard');
      $('#statTotal').textContent = d.totalComplaints;
      $('#statPending').textContent = d.pending;
      $('#statActive').textContent = d.assigned + d.inProgress;
      $('#statResolved').textContent = d.resolved;
      $('#statHigh').textContent = d.highPriorityOpen;
      $('#statWorkers').textContent = `${d.availableWorkers}/${d.totalWorkers}`;
      $('#statProofs').textContent = d.pendingProofs;
      $('#statRating').textContent = d.averageRating ? d.averageRating.toFixed(1) : '—';
      $('#todayLine').textContent = `${d.todayComplaints} new today · ${d.totalCitizens} registered citizens`;

      Civic.bars($('#catChart'), d.byCategory);
      columns($('#trendChart'), d.last7Days);

      $('#recentBody').innerHTML = d.recentComplaints.length ? d.recentComplaints.map(c => `
        <tr class="clickable" data-href="/admin/complaints/${c.id}"><td><strong>#${c.id}</strong></td>
        <td><div class="cell-title">${esc(c.title)}</div><div class="small muted">${esc(c.category)}</div></td>
        <td>${priority(c.priority)}</td><td>${badge(c.status)}</td><td class="small muted">${fmtDate(c.createdAt)}</td></tr>`).join('')
        : `<tr><td colspan="5">${empty('No complaints yet', 'New reports from citizens will appear here.')}</td></tr>`;

      $('#proofQueue').innerHTML = d.proofQueue.length ? d.proofQueue.map(p => `
        <div class="list-item"><div style="flex:1">
          <a href="/admin/complaints/${p.assignment.complaint.id}"><strong>#${p.assignment.complaint.id} ${esc(p.assignment.complaint.title)}</strong></a>
          <div class="small muted">by ${esc(p.assignment.worker.name)} · ${timeAgo(p.uploadedAt)}</div></div>
          <a class="btn btn-outline btn-sm" href="/admin/complaints/${p.assignment.complaint.id}">Review</a></div>`).join('')
        : empty('Nothing to review', 'Proofs uploaded by workers will queue here.');
    } catch (e) { toast(e.message, 'error'); }
  }

  document.addEventListener('DOMContentLoaded', load);
})();
