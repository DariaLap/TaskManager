const API = "http://localhost:8080";
const $ = (id) => document.getElementById(id);

// formatting
const dtFmt = new Intl.DateTimeFormat('en-US', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' });
function fmtDate(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d)) return iso; // if the server returned a non-standard format, show as-is
    return dtFmt.format(d).replace(',', '');
}
function isoDurationToMinutes(iso) {
    if (!iso) return 0;
    if (typeof iso === 'number') return iso;
    if (iso.startsWith?.('PT')) {
        const m = /PT(?:(\d+)H)?(?:(\d+)M)?/i.exec(iso);
        const h = m && m[1] ? Number(m[1]) : 0;
        const mm = m && m[2] ? Number(m[2]) : 0;
        return h * 60 + mm;
    }
    return Number(iso) || 0;
}
function minutesToISODuration(min) { return `PT${Number(min||0)}M`; }
function humanDuration(min) {
    min = Number(min||0);
    const h = Math.floor(min/60), m = min%60;
    if (h && m) return `${h}\u00A0h ${m}\u00A0min`;
    if (h) return `${h}\u00A0h`;
    return `${m}\u00A0min`;
}
function toLocalInputValue(iso) { return iso ? iso.slice(0,16) : ''; }

// UI helpers
function showMsg(text, isErr=false) {
    const el = $('msg');
    el.textContent = text; el.className = isErr ? 'err' : 'ok';
}
function clearMsg(){ const el=$('msg'); el.textContent=''; el.className=''; }

// types
function detectType(obj) {
    if (obj && typeof obj === 'object') {
        if ('epicId' in obj) return 'SUBTASK';
        if ('subtasks' in obj) return 'EPIC';
    }
    return 'TASK';
}

// TABLE RENDER
function renderList(data, typeHint) {
    const tbody = $('taskTbody');
    tbody.innerHTML = '';

    const items = Array.isArray(data) ? data : Object.values(data);
    const filtered = typeHint ? items.filter(it => detectType(it) === typeHint) : items;

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td class="empty" colspan="8">No items yet.</td></tr>';
        return;
    }

    for (const item of filtered) {
        const t = detectType(item);
        const id = item.id ?? '';
        const startIso = item.startTime ?? '';
        const start = fmtDate(startIso);
        const durMin = isoDurationToMinutes(item.duration);
        const durText = durMin ? humanDuration(durMin) : '—';
        const status = item.status ?? '—';
        const name = item.name ?? '';
        const relation = t === 'SUBTASK' ? (item.epicId != null ? `Epic #${item.epicId}` : '—')
            : t === 'EPIC' ? (Array.isArray(item.subtasks) ? `${item.subtasks.length} subtask(s)` : '—')
                : '—';

        const tr = document.createElement('tr');
        tr.innerHTML = `
      <td><span class="chip type">${t}</span></td>
      <td>#${id}</td>
      <td title="${(item.description||'').replace(/"/g,'&quot;')}"><strong>${name}</strong></td>
      <td><span class="chip status ${status}">${status}</span></td>
      <td>${start}</td>
      <td>${durText}</td>
      <td>${relation}</td>
      <td class="actions">
        <button class="icon-btn" title="Edit">✏️</button>
        <button class="icon-btn danger" title="Delete">✖</button>
      </td>`;

        const [editBtn, delBtn] = tr.querySelectorAll('.actions .icon-btn');
        editBtn.onclick = () => openEdit(item);
        delBtn.onclick = () => deleteItem(t, id);

        tbody.appendChild(tr);
    }
}

// API helpers
async function fetchJson(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status} ${await res.text()}`);
    return res.json();
}

async function load(type) {
    clearMsg();
    let url;
    if (type === 'TASK') url = `${API}/tasks`;
    if (type === 'EPIC') url = `${API}/epics`;
    if (type === 'SUBTASK') url = `${API}/subtasks`;
    try {
        const data = await fetchJson(url);
        renderList(data, type);
    } catch (e) {
        console.error(e);
        showMsg(`Load error ${type}: ${e.message}`, true);
    }
}

async function loadAll() {
    try {
        const [tasksRes, epicsRes, subtasksRes] = await Promise.all([
            fetch(`${API}/tasks`).then(r=>r.json()),
            fetch(`${API}/epics`).then(r=>r.json()),
            fetch(`${API}/subtasks`).then(r=>r.json())
        ]);

        const norm = (x) => Array.isArray(x) ? x : Object.values(x || {});
        const tOnly = norm(tasksRes).filter(it => detectType(it) === 'TASK');
        const eOnly = norm(epicsRes).filter(it => detectType(it) === 'EPIC');
        const sOnly = norm(subtasksRes).filter(it => detectType(it) === 'SUBTASK');

        // duplicate protection if some endpoints return mixed types
        const unique = new Map();
        for (const it of [...tOnly, ...eOnly, ...sOnly]) {
            const key = `${detectType(it)}:${it.id}`;
            if (!unique.has(key)) unique.set(key, it);
        }
        const all = Array.from(unique.values());
        renderList(all);
    } catch (e) {
        console.error(e);
        showMsg(`Load error: ${e.message}`, true);
    }
}

// CRUD: delete
async function deleteItem(type, id) {
    clearMsg();
    let url;
    if (type === 'TASK') url = `${API}/tasks/${id}`;
    if (type === 'EPIC') url = `${API}/epics/${id}`;
    if (type === 'SUBTASK') url = `${API}/subtasks/${id}`;
    try {
        const res = await fetch(url, { method:'DELETE' });
        if (!res.ok) throw new Error(`HTTP ${res.status} ${await res.text()}`);
        showMsg(`${type} #${id} deleted ✔`);
        await load(type);
    } catch (e) {
        console.error(e);
        showMsg(`Delete error: ${e.message}`, true);
    }
}

// EDIT modal
function openEdit(item) {
    const modal = $('editModal');
    $('editId').value = item.id;
    $('editName').value = item.name ?? '';
    $('editDesc').value = item.description ?? '';
    $('editStatus').value = item.status ?? 'NEW';
    $('editDuration').value = isoDurationToMinutes(item.duration);
    $('editStart').value = toLocalInputValue(item.startTime);

    const type = item.type || (item.epicId != null ? 'SUBTASK' : (item.subtasks ? 'EPIC' : 'TASK'));
    $('editType').value = type;

    const epicRow = $('editEpicRow');
    if (type === 'SUBTASK') {
        epicRow.style.display = '';
        $('editEpicId').value = item.epicId ?? '';
        $('editTitle').textContent = `Edit SubTask #${item.id}`;
    } else {
        epicRow.style.display = 'none';
        $('editTitle').textContent = type === 'TASK' ? `Edit Task #${item.id}` : `Epic #${item.id} (editing disabled)`;
    }
    $('editModal').style.display = 'block';
}
function closeEdit(){ $('editModal').style.display='none'; }

async function submitEdit() {
    const id = $('editId').value;
    const type = $('editType').value;
    if (type === 'EPIC') { alert('Updating epics via API is not supported.'); return; }

    const payload = {
        id: Number(id),
        name: $('editName').value.trim(),
        description: $('editDesc').value.trim(),
        status: $('editStatus').value,
        duration: minutesToISODuration($('editDuration').value),
        startTime: $('editStart').value
    };
    let url = `${API}/tasks/${id}`;
    if (type === 'SUBTASK') { payload.epicId = Number($('editEpicId').value); url = `${API}/subtasks/${id}`; }

    try {
        const resp = await fetch(url, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
        if (!resp.ok) { const txt = await resp.text(); throw new Error(`${resp.status} ${txt}`); }
        closeEdit();
        await loadAll();
    } catch (e) {
        console.error('update error:', e);
        alert('Failed to update task: ' + e.message);
    }
}
// expose for inline onclick in HTML
window.submitEdit = submitEdit;
window.openEdit = openEdit;
window.closeEdit = closeEdit;

// Create
$('taskForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    clearMsg();
    const type = $('itemType').value;
    const name = $('name').value.trim();
    const description = $('desc').value.trim();
    if (!name || !description) { showMsg('Name and description are required', true); return; }

    let url, payload;
    if (type === 'EPIC') {
        url = `${API}/epics`;
        payload = { name, description };
    } else if (type === 'TASK') {
        url = `${API}/tasks`;
        payload = { name, description, status: $('status').value, duration: minutesToISODuration($('duration').value), startTime: $('start').value };
    } else {
        url = `${API}/subtasks`;
        const epicId = Number($('epicId').value);
        if (!Number.isFinite(epicId)) { showMsg('For SubTask, provide a valid Epic ID', true); return; }
        payload = { name, description, status: $('status').value, duration: minutesToISODuration($('duration').value), startTime: $('start').value, epicId };
    }

    try {
        const res = await fetch(url, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
        if (!res.ok) throw new Error(`HTTP ${res.status} ${await res.text()}`);
        showMsg(`${type} added ✔`);
        $('taskForm').reset();
        $('start').value = new Date().toISOString().slice(0,16);
        $('itemType').dispatchEvent(new Event('change'));
        await load(type === 'SUBTASK' ? 'SUBTASK' : type);
    } catch (e) {
        console.error(e);
        showMsg(`Add error: ${e.message}`, true);
    }
});

// Toggles
$('itemType').addEventListener('change', () => {
    const type = $('itemType').value; const isSub = type === 'SUBTASK'; const isEpic = type === 'EPIC';
    $('epicIdWrap').style.display = isSub ? 'block' : 'none';
    $('timingWrap').style.display = isEpic ? 'none' : 'block';
});

// Filters
$('btnViewTasks').onclick = () => load('TASK');
$('btnViewEpics').onclick = () => load('EPIC');
$('btnViewSubTasks').onclick = () => load('SUBTASK');
$('btnViewAll').onclick = () => loadAll();

// initial values
(function setNow(){ const now = new Date(); $('start').value = now.toISOString().slice(0,16); })();
$('itemType').dispatchEvent(new Event('change'));
// initial render
loadAll();