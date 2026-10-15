/* Admin operations assistant chat. */
'use strict';

(() => {
  const { api, esc } = Civic;
  const log = document.getElementById('chatLog');
  const form = document.getElementById('chatForm');
  const input = document.getElementById('chatInput');
  const suggest = document.getElementById('chatSuggest');

  // escape first, then add **bold** and #123 complaint links
  const format = text => esc(text)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|\s)#(\d+)/g, '$1<a href="/admin/complaints/$2">#$2</a>');

  function add(text, who) {
    const div = document.createElement('div');
    div.className = `msg ${who}`;
    div.innerHTML = format(text);
    log.appendChild(div);
    log.scrollTop = log.scrollHeight;
    return div;
  }

  function setSuggestions(list) {
    suggest.innerHTML = (list || []).map(s => `<button type="button" class="chip">${esc(s)}</button>`).join('');
  }

  async function send(text) {
    text = text.trim();
    if (!text) return;
    add(text, 'me');
    input.value = '';
    const typing = add('Thinking…', 'bot typing');
    try {
      const r = await api('/api/admin/chatbot', { method: 'POST', body: { message: text } });
      typing.remove();
      add(r.reply, 'bot');
      setSuggestions(r.suggestions);
    } catch (e) {
      typing.remove();
      add(`Sorry, I couldn't answer that: ${e.message}`, 'bot');
    }
    input.focus();
  }

  suggest.addEventListener('click', e => { const c = e.target.closest('.chip'); if (c) send(c.textContent); });
  form.addEventListener('submit', e => { e.preventDefault(); send(input.value); });

  add("Hi! I'm your civic operations assistant. Ask me about complaints, workers, proofs waiting for review or citizen feedback. Try \"status of #1\".", 'bot');
  setSuggestions(['Give me a summary', 'How many complaints are pending?', 'Show high priority issues',
    'Which workers are available?', 'Proofs awaiting review', 'Most common category']);
})();
