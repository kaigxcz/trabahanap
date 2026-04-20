// Injects notification bell into the sidebar and loads unread count
function initNav() {
    const sidebar = document.querySelector('aside');
    if (!sidebar) return;

    // Insert bell button before logout
    const logoutBtn = sidebar.querySelector('button[onclick="logout()"]');
    if (logoutBtn) {
        const bellWrapper = document.createElement('div');
        bellWrapper.className = 'relative mt-4';
        bellWrapper.innerHTML = `
            <button onclick="toggleNotifPanel()" class="w-full flex items-center space-x-3 px-4 py-3 rounded-2xl text-slate-400 hover:text-slate-600 hover:bg-slate-50 transition relative">
                <span class="text-lg">🔔</span>
                <span>Notifications</span>
                <span id="notif-badge" class="absolute left-7 top-2 bg-red-500 text-white text-[9px] font-bold w-4 h-4 rounded-full items-center justify-center hidden">0</span>
            </button>`;
        logoutBtn.parentNode.insertBefore(bellWrapper, logoutBtn);
    }

    // Notification panel
    const panel = document.createElement('div');
    panel.id = 'notif-panel';
    panel.className = 'fixed inset-0 z-40 hidden';
    panel.innerHTML = `
        <div class="absolute inset-0" onclick="toggleNotifPanel()"></div>
        <div class="absolute left-72 top-0 h-full w-80 bg-white shadow-2xl border-l border-slate-100 flex flex-col z-50">
            <div class="p-5 border-b border-slate-100 flex items-center justify-between">
                <h3 class="font-bold text-slate-900">Notifications</h3>
                <button onclick="toggleNotifPanel()" class="text-slate-400 hover:text-slate-600 text-xl leading-none">&times;</button>
            </div>
            <div id="notif-list" class="flex-1 overflow-y-auto p-3 space-y-1">
                <p class="text-slate-400 text-sm text-center py-4">Loading...</p>
            </div>
        </div>`;
    document.body.appendChild(panel);

    loadNotificationBell();
}

function toggleNotifPanel() {
    const panel = document.getElementById('notif-panel');
    const isHidden = panel.classList.contains('hidden');
    panel.classList.toggle('hidden');
    if (isHidden) loadNotificationDropdown();
}
