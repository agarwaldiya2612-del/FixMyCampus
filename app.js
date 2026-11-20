const issueList = document.getElementById('issue-list');
const tabs = document.querySelectorAll('.filter-tabs button');
let issues = [];
let filter = 'all';

async function loadIssues() {
  try {
    const response = await fetch('/api/issues');
    issues = await response.json();
    render();
  } catch (error) {
    issueList.innerHTML = '<div class="empty">Could not connect to the Java server. Start it using run-web.bat and refresh this page.</div>';
  }
}

function render() {
  const resolved = issues.filter(issue => issue.status === 'RESOLVED').length;
  document.getElementById('total-count').textContent = issues.length;
  document.getElementById('active-count').textContent = issues.length - resolved;
  document.getElementById('resolved-count').textContent = resolved;
  document.getElementById('all-num').textContent = issues.length;
  let shown = issues.filter(issue => filter === 'all' || (filter === 'active' ? issue.status !== 'RESOLVED' : issue.status === filter));
  const sort = document.getElementById('sort').value;
  shown.sort((a, b) => sort === 'newest' ? b.id - a.id : b.priority - a.priority);
  if (!shown.length) {
    issueList.innerHTML = '<div class="empty">No issues here yet. You can be the first to report one.</div>';
    return;
  }
  issueList.innerHTML = shown.map(issue => `<article class="issue-card"><div class="issue-top"><span class="tag">${escapeText(issue.category)}</span><span class="tag ${issue.status}">${escapeText(issue.status.replaceAll('_', ' '))}</span></div><h3>${escapeText(issue.title)}</h3><p>${escapeText(issue.description)}</p><div class="issue-meta"><span>⌖ ${escapeText(issue.location)}</span><span>Severity ${issue.severity}/5</span></div><div class="issue-bottom"><div class="vote-controls"><button data-vote="1" data-id="${issue.id}">↑ Support</button><button data-vote="-1" data-id="${issue.id}">↓</button></div><span class="support-label">${issue.support} net support · Priority ${issue.priority}</span></div>${issue.messages.length ? `<div class="messages">${issue.messages.map(message => `<div>${escapeText(message)}</div>`).join('')}</div>` : ''}<form class="message-form" data-id="${issue.id}"><input name="message" required placeholder="Join the discussion"><button>Post</button></form></article>`).join('');
}

function escapeText(value) {
  return String(value).replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
}

tabs.forEach(tab => tab.addEventListener('click', () => {
  tabs.forEach(item => item.classList.remove('active'));
  tab.classList.add('active'); filter = tab.dataset.filter; render();
}));
document.getElementById('sort').addEventListener('change', render);
issueList.addEventListener('click', async event => {
  const button = event.target.closest('[data-vote]');
  if (!button) return;
  const body = new URLSearchParams({vote: button.dataset.vote});
  await fetch(`/api/issues/${button.dataset.id}/vote`, {method: 'PUT', body});
  await loadIssues();
});
issueList.addEventListener('submit', async event => {
  if (!event.target.matches('.message-form')) return;
  event.preventDefault();
  const body = new URLSearchParams(new FormData(event.target));
  body.set('name', 'Campus student');
  await fetch(`/api/issues/${event.target.dataset.id}/messages`, {method: 'POST', body});
  await loadIssues();
});
document.getElementById('report-form').addEventListener('submit', async event => {
  event.preventDefault();
  const body = new URLSearchParams(new FormData(event.target));
  const response = await fetch('/api/issues', {method: 'POST', body});
  const result = await response.json();
  if (response.ok) {
    document.getElementById('form-message').textContent = `Thanks! Issue #${result.id} has been reported.`;
    event.target.reset(); await loadIssues(); document.getElementById('issues').scrollIntoView({behavior: 'smooth'});
  } else document.getElementById('form-message').textContent = result.error || 'Could not submit the report.';
});
loadIssues();
