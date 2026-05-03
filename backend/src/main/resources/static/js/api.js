const API = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : window.location.origin + '/api';

function getToken() { return localStorage.getItem('jc_token'); }
function getHeaders() {
    return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + getToken() };
}

async function apiPost(path, body, auth = false) {
    const res = await fetch(API + path, {
        method: 'POST',
        headers: auth ? getHeaders() : { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    if (!res.ok && res.status !== 400 && res.status !== 401) throw new Error('Server error: ' + res.status);
    return res.json();
}

async function apiGet(path) {
    const res = await fetch(API + path, { headers: getHeaders() });
    if (!res.ok) throw new Error('API error ' + res.status + ' on ' + path);
    return res.json();
}

async function apiPut(path, body) {
    const res = await fetch(API + path, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(body)
    });
    return res.json();
}

async function apiDelete(path) {
    const res = await fetch(API + path, { method: 'DELETE', headers: getHeaders() });
    return res.json();
}

async function apiUpload(path, formData) {
    const res = await fetch(API + path, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + getToken() },
        body: formData
    });
    return res.json();
}

// Notification bell loader — call this on every page
async function loadNotificationBell() {
    try {
        const data = await apiGet('/notifications/unread-count');
        const badge = document.getElementById('notif-badge');
        const count = data.count || 0;
        if (badge) {
            badge.innerText = count;
            badge.style.display = count > 0 ? 'flex' : 'none';
        }
    } catch(e) { /* silent fail */ }
}

async function loadNotificationDropdown() {
    const list = document.getElementById('notif-list');
    if (!list) return;
    const notifs = await apiGet('/notifications');
    if (!notifs.length) {
        list.innerHTML = '<p class="text-slate-400 text-sm text-center py-4">No notifications yet.</p>';
        return;
    }
    list.innerHTML = notifs.map(n => `
        <div class="flex items-start gap-3 px-4 py-3 hover:bg-slate-50 rounded-xl transition ${n.read ? 'opacity-60' : ''}">
            <span class="text-lg mt-0.5">${n.type === 'application' ? '📄' : n.type === 'recommendation' ? '💡' : '🔔'}</span>
            <div class="flex-1">
                <p class="text-sm text-slate-700">${n.message}</p>
                <p class="text-xs text-slate-400 mt-0.5">${timeAgo(n.createdAt)}</p>
            </div>
            ${!n.read ? '<span class="w-2 h-2 bg-indigo-500 rounded-full mt-1.5 shrink-0"></span>' : ''}
        </div>`).join('');
    await apiPut('/notifications/read-all', {});
    const badge = document.getElementById('notif-badge');
    if (badge) badge.style.display = 'none';
}

function timeAgo(dateStr) {
    // Ensure UTC parsing — append Z if no timezone info present
    const normalized = dateStr && !dateStr.endsWith('Z') && !dateStr.includes('+') ? dateStr + 'Z' : dateStr;
    const diff = Math.floor((Date.now() - new Date(normalized)) / 1000);
    if (diff < 5) return 'just now';
    if (diff < 60) return diff + 's ago';
    if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
    if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
    return Math.floor(diff / 86400) + 'd ago';
}

// ── Toast system (replaces alert()) ──
function toast(message, type = 'info', duration = 3500) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }
    const icons = { success:'✅', error:'❌', info:'ℹ️', warning:'⚠️' };
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = `<span>${icons[type]}</span><span class="flex-1">${message}</span><button onclick="this.parentElement.remove()" style="background:none;border:none;cursor:pointer;opacity:0.5;font-size:1rem;padding:0;line-height:1">×</button>`;
    container.appendChild(el);
    setTimeout(() => {
        el.classList.add('hide');
        setTimeout(() => el.remove(), 300);
    }, duration);
}

// ── Confirm dialog (replaces confirm()) ──
function showConfirm(message, onConfirm) {
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(15,23,42,0.6);z-index:9998;display:flex;align-items:center;justify-content:center;backdrop-filter:blur(4px)';
    overlay.innerHTML = `
        <div style="background:white;border-radius:1.5rem;padding:2rem;max-width:22rem;width:90%;box-shadow:0 20px 60px rgba(0,0,0,0.2);animation:scaleIn 0.2s ease both">
            <p style="font-size:1rem;font-weight:700;color:#0f172a;margin-bottom:0.5rem">Are you sure?</p>
            <p style="font-size:0.875rem;color:#64748b;margin-bottom:1.5rem">${message}</p>
            <div style="display:flex;gap:0.75rem">
                <button id="confirm-cancel" style="flex:1;padding:0.75rem;border:1.5px solid #e2e8f0;border-radius:0.875rem;font-weight:600;cursor:pointer;background:white;color:#64748b">Cancel</button>
                <button id="confirm-ok" style="flex:1;padding:0.75rem;border:none;border-radius:0.875rem;font-weight:600;cursor:pointer;background:#ef4444;color:white">Confirm</button>
            </div>
        </div>`;
    document.body.appendChild(overlay);
    overlay.querySelector('#confirm-cancel').onclick = () => overlay.remove();
    overlay.querySelector('#confirm-ok').onclick = () => { overlay.remove(); onConfirm(); };
    overlay.onclick = e => { if (e.target === overlay) overlay.remove(); };
}

// ── Loading button helper ──
function setLoading(btn, loading, text = '') {
    if (loading) {
        btn._origText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = `<span class="spinner" style="display:inline-block;width:1rem;height:1rem;border:2px solid rgba(255,255,255,0.3);border-top-color:white;border-radius:50%;animation:spin 0.7s linear infinite"></span>${text ? ' ' + text : ''}`;
    } else {
        btn.disabled = false;
        btn.innerHTML = btn._origText || text;
    }
}

