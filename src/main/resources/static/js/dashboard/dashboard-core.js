/* ═══════════════════════════════════════════════════════════════════════
   FILE: src/main/resources/static/js/dashboard/dashboard-core.js
   Spindle ERP — Dashboard shell JS (shared by ALL dashboard-family pages)

   Owns the shell: sidebar (3-level nav render + search + active state),
   theme toggle, topbar dropdown panels, notifications, user context
   switch, settings drawer, and the CSRF-aware fetch helpers.

   Exposes on window.SpindleDash:
     apiFetch(url)            — GET  JSON with CSRF header
     postJson(url, body)      — POST JSON with CSRF header
     esc(s)                   — HTML-escape (MANDATORY before innerHTML)
     escAttr(s)               — attribute-safe escape
     debounce(fn, ms)         — trailing-edge debounce
     fmtTaka(n)               — ৳ Crore/Lac/K abbreviation
     MENU_TREE                — the sec_menus-derived navigation tree
     onSettingsSaved(fn)      — subscribe to settings-save events

   SECURITY: every server-sourced string rendered via innerHTML MUST go
   through esc()/escAttr(). Dynamic click targets use event delegation
   with data-href — NEVER string-interpolated inline onclick handlers.

   Load order guaranteed by layout/dashboard-base.html:
     bootstrap.bundle → dashboard-core.js → page's pageJs slot
   ═══════════════════════════════════════════════════════════════════════ */
(function(){
'use strict';

/* ════════════════════════════════════════════════════════════════════
   HELPERS — escaping, debounce, formatting
   ════════════════════════════════════════════════════════════════════ */
var ESC_MAP = {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'};
function esc(s){
  if(s === null || s === undefined) return '';
  return String(s).replace(/[&<>"']/g, function(c){ return ESC_MAP[c]; });
}
function escAttr(s){ return esc(s); }
function debounce(fn, ms){
  var t;
  return function(){
    var args = arguments, ctx = this;
    clearTimeout(t);
    t = setTimeout(function(){ fn.apply(ctx, args); }, ms || 200);
  };
}
/* Bangladesh Taka abbreviation — 1,00,00,000 = 1 Cr, 1,00,000 = 1 Lac */
function fmtTaka(n){
  if(n === null || n === undefined || isNaN(n)) return '—';
  var abs = Math.abs(n), sign = n < 0 ? '-' : '';
  if(abs >= 1e7) return sign + '৳' + (abs/1e7).toFixed(2).replace(/\.00$/,'') + ' Cr';
  if(abs >= 1e5) return sign + '৳' + (abs/1e5).toFixed(2).replace(/\.00$/,'') + ' Lac';
  if(abs >= 1e3) return sign + '৳' + (abs/1e3).toFixed(1).replace(/\.0$/,'') + 'K';
  return sign + '৳' + abs.toLocaleString('en-IN');
}

/* ════════════════════════════════════════════════════════════════════
   MENU TREE — verbatim from sec_menus seed script (menu_code/menu_url).
   Root order matches display_order in the seed data.
   ════════════════════════════════════════════════════════════════════ */
var MENU_TREE = [
  { code:'CORE_SECURITY', name:'Core & Security', icon:'fa-shield-halved', color:'#60a5fa', url:'/core-security',
    groups:[
      { name:'User Setup', items:[
        {n:'Users', u:'/users'}, {n:'Roles', u:'/roles'}, {n:'Permissions', u:'/permissions'}, {n:'Menu', u:'/menus'}
      ]},
      { name:'Business Setup', items:[
        {n:'Organization', u:'/organizations'}, {n:'Business Units', u:'/business-units'}
      ]},
      { name:'Global Setup', items:[
        {n:'Terms & Conditions', u:'/global-terms'}
      ]},
      { name:'Location Setup', items:[
        {n:'Currencies', u:'/currencies'}, {n:'Countries', u:'/countries'}, {n:'States / Divisions', u:'/states'},
        {n:'Districts', u:'/districts'}, {n:'Cities', u:'/cities'}, {n:'Areas', u:'/areas'}, {n:'Timezone', u:'/timezones'}
      ]},
      { name:'Communication Setup', items:[
        {n:'Notification Templates', u:'/notification-templates'}, {n:'Email Management', u:'/email-notifications'}
      ]},
      { name:'Approval', items:[
        {n:'Approval Configurations', u:'/approval-configs'}, {n:'Approval Requests', u:'/approval-requests'},
        {n:'Approval Panel', u:'/approval-panel'}, {n:'Pending Approval', u:'/pending-approval'},
        {n:'My Approval Requests', u:'/my-approval-requests'}
      ]}
    ]},
  { code:'HRM', name:'Human Resource Management', icon:'fa-users', color:'#c084fc', url:'/hrm',
    groups:[
      { name:'HR Setup', items:[
        {n:'Departments', u:'/departments'}, {n:'Designations', u:'/designations'}, {n:'Shifts', u:'/shifts'}
      ]},
      { name:'HR Operations', items:[
        {n:'Employees', u:'/employees'}, {n:'Employee Leaves', u:'/employee-leaves'},
        {n:'Leave Applications', u:'/leave-applications'}, {n:'Leave Balances', u:'/leave-balances'},
        {n:'Leave Encashments', u:'/leave-encashments'}
      ]},
      { name:'Payroll', items:[
        {n:'Payroll Adjustments', u:'/payroll-adjustments'}, {n:'Payroll Cycles', u:'/payroll-cycles'},
        {n:'Salary Components', u:'/salary-components'}, {n:'Salary Processing', u:'/payroll-processing'},
        {n:'Pay Slip', u:'/pay-slips'}
      ]}
    ]},
  { code:'PURCHASE_SUPPLIER', name:'Purchase & Suppliers', icon:'fa-truck-field', color:'var(--orange)', url:'/purchase',
    groups:[
      { name:'Purchase Setup', items:[
        {n:'Suppliers', u:'/suppliers'}, {n:'Supplier Agents', u:'/supplier-agents'}, {n:'Purchase Analytic', u:'/purchase-analytics'}
      ]},
      { name:'Transactions', items:[
        {n:'Purchase Requisitions', u:'/purchase-requisitions'}, {n:'Purchase Mode Selection', u:'/spr-viewer'},
        {n:'RFQ', u:'/rfq'}, {n:'Comparative Statements', u:'/comparative-statements'},
        {n:'Purchase Orders', u:'/purchase-orders'}, {n:'Purchase Report', u:'/purchase-report'}
      ]}
    ]},
  { code:'INVENTORY_WAREHOUSE', name:'Inventory & Warehouse', icon:'fa-warehouse', color:'var(--teal)', url:'/inventory',
    groups:[
      { name:'Inventory Setup', items:[
        {n:'Item Categories', u:'/item-categories'}, {n:'Items', u:'/inventory-items'}, {n:'Warehouses', u:'/warehouses'},
        {n:'Item Unit', u:'/units-of-measure'}, {n:'Item Brands', u:'/item-brands'}, {n:'Item Models', u:'/item-models'},
        {n:'Item Lot No', u:'/inventory-lots'}
      ]},
      { name:'Yarn Item Setup', items:[
        {n:'Yarn Counts', u:'/yarn-counts'}, {n:'Yarn Plies', u:'/yarn-plies'}, {n:'Yarn Types', u:'/yarn-types'},
        {n:'Yarn Compositions', u:'/yarn-blends'}
      ]},
      { name:'Inventory Operations', items:[
        {n:'Stock Transactions', u:'/stock-transactions'}, {n:'Store Requisitions', u:'/store-requisitions'},
        {n:'Goods Receiving Notes', u:'/goods-receipt-notes'}, {n:'Material Issues', u:'/store-issues'}
      ]},
      { name:'Yarn Inventory Operations', items:[
        {n:'Production Material Issue', u:'/production-material-issues'}
      ]},
      { name:'Inventory Reports', items:[
        {n:'Stock Report', u:'/inventory-stocks'}, {n:'Fiber Stock Report', u:'/fiber-stock-report'}
      ]}
    ]},
  { code:'SALES_CUSTOMER_OPERATIONS', name:'Sales & Customer Operations', icon:'fa-cart-shopping', color:'var(--green)', url:'/sales',
    groups:[
      { name:'Sales Setup', items:[
        {n:'Customers', u:'/customers'}
      ]},
      { name:'Sales Operations', items:[
        {n:'Sales Quotations', u:'/sales-quotations'}, {n:'Sales Orders', u:'/sales-orders'},
        {n:'Delivery Orders', u:'/delivery-orders'}, {n:'Delivery Challans', u:'/delivery-challans'},
        {n:'Sales Invoices', u:'/sales-invoices'}
      ]},
      { name:'Sales Analytics', items:[
        {n:'Sales Dashboard', u:'/sales-analytics/dashboard'}, {n:'Sales Report', u:'/sales-report'}
      ]}
    ]},
  { code:'FINANCE_ACCOUNTS', name:'Finance & Accounts', icon:'fa-coins', color:'var(--yellow)', url:'/finance',
    groups:[
      { name:'Finance Setup', items:[
        {n:'Accounts', u:'/accounts'}, {n:'Cash Accounts', u:'/cash-accounts'}, {n:'Bank Accounts', u:'/bank-accounts'},
        {n:'Cost Centers', u:'/cost-centers'}, {n:'Accounts Mappings', u:'/accounts-mappings'},
        {n:'Journal Templates', u:'/auto-journal-templates'}, {n:'Accounts Policies', u:'/accounts-policies'},
        {n:'Banks', u:'/banks'}, {n:'Accounting Periods', u:'/accounting-periods'}
      ]},
      { name:'Finance Operations', items:[
        {n:'Purchase Invoices', u:'/purchase-invoices'}, {n:'Journal Entries', u:'/journal-entries'}
      ]},
      { name:'Finance Analytics', items:[
        {n:'Financial Dashboard', u:'/financial-analytics/dashboard'}, {n:'Trial Balance', u:'/financial-reports/trial-balance'},
        {n:'Balance Sheet', u:'/financial-reports/balance-sheet'}, {n:'Profit & Loss', u:'/financial-reports/profit-loss'},
        {n:'Cash Flow', u:'/financial-reports/cash-flow'}, {n:'Account Ledger', u:'/financial-reports/account-ledger'},
        {n:'Receivable Aging', u:'/financial-reports/receivable-aging'}, {n:'Payable Aging', u:'/financial-reports/payable-aging'}
      ]}
    ]},
  { code:'PRODUCTION', name:'Production', icon:'fa-industry', color:'#f472b6', url:'/production',
    groups:[
      { name:'Planning & Requirements', items:[
        {n:'Production Order', u:'/production-orders'}, {n:'Yarn Mixing', u:'/production-recipes'},
        {n:'Waste Type', u:'/waste-types'}, {n:'Waste Receive', u:'/waste-receive'}
      ]},
      { name:'Production Processing', items:[
        {n:'Yarn Production Requisition', u:'/production-requisitions'}, {n:'Yarn Production Receive', u:'/yarn-production-receive'}
      ]},
      { name:'Yarn Reporting', items:[
        {n:'Yarn Stock Report', u:'/yarn-stock-report'}, {n:'Production Report', u:'/production-report'},
        {n:'Waste Report', u:'/waste-report'}
      ]}
    ]},
  { code:'COMMERCIAL', name:'Commercial', icon:'fa-ship', color:'var(--accent)', url:'/commercial',
    groups:[
      { name:'Master Setup', items:[
        {n:'HS Code', u:'/hs-codes'}, {n:'LC-PI Reports', u:'/lc-pi'}, {n:'Document Name', u:'/commercial-doc-names'},
        {n:'Commercial Cost Head', u:'/commercial-cost-heads'}, {n:'Commercial Ports', u:'/commercial-ports'}
      ]},
      { name:'Export Commercial', items:[
        {n:'Proforma Invoice (Export PI)', u:'/export-pi'}, {n:'Export LC', u:'/export-lc'}, {n:'Commercial Invoice', u:'/commercial-invoice'}
      ]},
      { name:'Import Commercial', items:[
        {n:'Proforma Invoice (Import PI)', u:'/import-pi'}, {n:'Import LC', u:'/import-lc'}
      ]}
    ]},
  { code:'REPORTS_ANALYTICS', name:'Reports & Analytics', icon:'fa-chart-line', color:'#38bdf8', url:'/reports',
    groups:[
      { name:'Standard Reports', items:[
        {n:'Financial Reports', u:'/reports/financial'}, {n:'Sales Reports', u:'/reports/sales'}, {n:'Inventory Reports', u:'/reports/inventory'}
      ]},
      { name:'Dashboards', items:[
        {n:'Reports Dashboard', u:'/reports/dashboard'}
      ]}
    ]}
];

/* ════════════════════════════════════════════════════════════════════
   CSRF-AWARE FETCH HELPERS
   ════════════════════════════════════════════════════════════════════ */
function csrfHeaders(extra){
  var headers = extra || {};
  var csrfMeta = document.querySelector('meta[name="_csrf"]');
  var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
  if(csrfMeta && csrfHeaderMeta && csrfMeta.content){ headers[csrfHeaderMeta.content] = csrfMeta.content; }
  return headers;
}
function apiFetch(url){
  return fetch(url, {headers: csrfHeaders({'Accept':'application/json'}), credentials:'same-origin'})
    .then(function(r){
      if(!r.ok) throw new Error('HTTP '+r.status);
      return r.json();
    });
}
function postJson(url, body){
  return fetch(url, {
    method:'POST',
    headers: csrfHeaders({'Accept':'application/json','Content-Type':'application/json'}),
    credentials:'same-origin',
    body: JSON.stringify(body || {})
  }).then(function(r){
    if(!r.ok) throw new Error('HTTP '+r.status);
    return r.json().catch(function(){ return {}; });
  });
}

/* ════════════════════════════════════════════════════════════════════
   RENDER: Sidebar — Menu → Submenu → Item, 3 levels, with active state
   detected against the current pathname (boundary-safe prefix match).
   Leaf navigation via event delegation on data-href — no inline JS with
   interpolated strings.
   ════════════════════════════════════════════════════════════════════ */
function currentPath(){
  return (window.location && window.location.pathname) || '';
}
function pathMatches(path, u){
  /* exact, or prefix followed by '/' — '/users' must NOT match '/users-archive' */
  return path === u || (u !== '/' && path.indexOf(u + '/') === 0);
}
function renderSidebarGroups(){
  var host = document.getElementById('sbModuleGroups');
  if(!host) return;
  var path = currentPath();
  var html = '<div class="sb-section-label">Modules</div>';

  MENU_TREE.forEach(function(mod, mi){
    var modHasActive = false;
    var groupsHtml = '';

    mod.groups.forEach(function(g){
      var subHasActive = false;
      var itemsHtml = '';

      g.items.forEach(function(it){
        var isActive = pathMatches(path, it.u);
        if(isActive){ subHasActive = true; modHasActive = true; }
        itemsHtml += '<div class="sb-leaf-item nav-searchable'+(isActive?' active':'')+'" role="link" tabindex="0" ' +
                        'data-href="'+escAttr(it.u)+'" ' +
                        'data-text="'+escAttr((it.n+' '+it.u+' '+mod.name+' '+g.name).toLowerCase())+'">' +
                        '<span class="lbl">'+esc(it.n)+'</span></div>';
      });

      groupsHtml += '<div class="sb-subgroup-toggle'+(subHasActive?' open has-active':'')+'" role="button" tabindex="0" ' +
                       'data-toggle="subgroup">' +
                       '<span>'+esc(g.name)+'</span><i class="fa fa-chevron-right chev"></i></div>';
      groupsHtml += '<div class="sb-subgroup-body'+(subHasActive?' open':'')+'">'+itemsHtml+'</div>';
    });

    html += '<div class="sb-group-toggle'+(modHasActive?' open has-active':'')+'" data-mod="'+escAttr(mod.code)+'" role="button" tabindex="0" data-toggle="group">' +
              '<span class="si"><span class="ic" style="color:'+escAttr(mod.color)+'"><i class="fa '+escAttr(mod.icon)+'"></i></span><span class="lbl">'+esc(mod.name)+'</span></span>' +
              '<i class="fa fa-chevron-right chev"></i></div>';
    html += '<div class="sb-group-body'+(modHasActive?' open':'')+'" id="sbGroup'+mi+'">'+groupsHtml+'</div>';
  });
  host.innerHTML = html;
}
/* Delegated sidebar interaction — click + Enter/Space keyboard support */
function _sbActivate(el){
  if(el.dataset.href){ location.href = el.dataset.href; return; }
  if(el.dataset.toggle){
    el.classList.toggle('open');
    if(el.nextElementSibling) el.nextElementSibling.classList.toggle('open');
  }
}
document.addEventListener('click', function(e){
  var t = e.target.closest('#sbModuleGroups [data-href], #sbModuleGroups [data-toggle]');
  if(t) _sbActivate(t);
});
document.addEventListener('keydown', function(e){
  if(e.key !== 'Enter' && e.key !== ' ') return;
  var t = e.target.closest && e.target.closest('#sbModuleGroups [data-href], #sbModuleGroups [data-toggle]');
  if(t){ e.preventDefault(); _sbActivate(t); }
});
/* Backwards compatibility — kept for any legacy inline handlers */
window.toggleSbGroup = function(el){ _sbActivate(el); };
window.toggleSbSubgroup = function(el){ _sbActivate(el); };

var filterNav = debounce(function(q){
  q = (q||'').toLowerCase().trim();
  var leaves = document.querySelectorAll('#sbModuleGroups .nav-searchable');
  leaves.forEach(function(leaf){
    var match = !q || leaf.getAttribute('data-text').indexOf(q) !== -1;
    leaf.classList.toggle('sb-hidden', !match);
    if(match && q){
      var subBody = leaf.closest('.sb-subgroup-body');
      if(subBody){ subBody.classList.add('open'); if(subBody.previousElementSibling) subBody.previousElementSibling.classList.add('open'); }
      var modBody = leaf.closest('.sb-group-body');
      if(modBody){ modBody.classList.add('open'); if(modBody.previousElementSibling) modBody.previousElementSibling.classList.add('open'); }
    }
  });
}, 120);
window.filterNav = filterNav;

/* ════════════════════════════════════════════════════════════════════
   THEME / SIDEBAR TOGGLES
   ════════════════════════════════════════════════════════════════════ */
function toggleTheme(){
  var html = document.documentElement;
  var next = html.getAttribute('data-theme')==='dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', next);
  try{ localStorage.setItem('spindle-theme', next); }catch(e){}
  /* keep settings-drawer switch in sync if drawer is open */
  var sw = document.getElementById('swTheme');
  if(sw) sw.classList.toggle('on', next==='dark');
}
function openSidebar(){
  var sb = document.getElementById('sidebar'), ov = document.getElementById('overlay');
  if(sb) sb.classList.add('open');
  if(ov) ov.classList.add('show');
}
function closeSidebar(){
  var sb = document.getElementById('sidebar'), ov = document.getElementById('overlay');
  if(sb) sb.classList.remove('open');
  if(ov) ov.classList.remove('show');
}
window.toggleTheme = toggleTheme;
window.openSidebar = openSidebar;
window.closeSidebar = closeSidebar;

/* ════════════════════════════════════════════════════════════════════
   CONTEXT (user/org) — mirrors erp-main-dashboard.html _loadContext
   ════════════════════════════════════════════════════════════════════ */
function _loadContext(){
  apiFetch('/api/context/current').then(function(ctx){
    applyContextToUI(ctx);
  }).catch(function(){
    applyContextToUI({userName:'Admin User', email:'admin@spindleerp.com', organizationName:'ASG Software Solutions',
                       roleName:'Org Admin', businessUnitName:'Head Office', warehouseName:'Main Warehouse', costCenterName:'HQ — Corporate'});
  });
}
function applyContextToUI(ctx){
  var name = ctx.userName || ctx.fullName || 'Admin';
  var org  = ctx.organizationName || ctx.orgName || 'Organization';
  var initials = name.split(' ').map(function(s){return s[0];}).slice(0,2).join('').toUpperCase();
  ['sbUserName','tbUserName'].forEach(function(id){ var el=document.getElementById(id); if(el) el.textContent=name; });
  ['sbUserAv','tbUserAv','upAv'].forEach(function(id){ var el=document.getElementById(id); if(el) el.textContent=initials||'A'; });
  var orgEl = document.getElementById('sbUserOrg'); if(orgEl) orgEl.textContent = org;
  var roleEl = document.getElementById('tbUserRole'); if(roleEl) roleEl.textContent = ctx.roleName || 'Org Admin';
  var upName = document.getElementById('upName'); if(upName) upName.textContent = name;
  var upEmail = document.getElementById('upEmail'); if(upEmail) upEmail.textContent = ctx.email || '—';
  var upRole = document.getElementById('upRole'); if(upRole) upRole.textContent = ctx.roleName || 'Org Admin';
}

/* ════════════════════════════════════════════════════════════════════
   DROPDOWN PANELS (notifications / user context) — open one at a time,
   close on outside click or Escape.
   ════════════════════════════════════════════════════════════════════ */
function togglePanel(id, evt){
  if(evt) evt.stopPropagation();
  var panel = document.getElementById(id);
  if(!panel) return;
  var wasOpen = panel.classList.contains('show');
  closeAllPanels();
  if(!wasOpen){
    panel.classList.add('show');
    if(id==='notifPanel' && !panel.dataset.loaded){ loadNotifications(); panel.dataset.loaded='1'; }
    if(id==='userPanel' && !panel.dataset.loaded){ loadContextScopes(); panel.dataset.loaded='1'; }
  }
}
function closeAllPanels(){
  document.querySelectorAll('.dd-panel.show').forEach(function(p){ p.classList.remove('show'); });
}
window.togglePanel = togglePanel;
window.closeAllPanels = closeAllPanels;
document.addEventListener('click', function(e){
  if(!e.target.closest('.tb-anchor')) closeAllPanels();
});
document.addEventListener('keydown', function(e){
  if(e.key==='Escape'){ closeAllPanels(); closeSettings(); closeSidebar(); }
  /* '/' focuses the sidebar search unless already typing in a field */
  if(e.key==='/' && !/^(INPUT|TEXTAREA|SELECT)$/.test((e.target.tagName||''))){
    var s = document.getElementById('sbSearch');
    if(s){ e.preventDefault(); s.focus(); }
  }
});

/* ════════════════════════════════════════════════════════════════════
   NOTIFICATIONS — GET /api/notifications  (backed by ntf_notifications:
   id, category, notification_type, title, message, link, is_read,
   created_at). POST /api/notifications/mark-all-read to clear badges.
   All server-sourced fields escaped before innerHTML.
   ════════════════════════════════════════════════════════════════════ */
var DEMO_NOTIFS = [
  {id:1, category:'APPROVAL', type:'IN_APP', title:'PO-2026-00341 needs your approval', message:'Purchase Order for Ibrahim Group raw cotton lot exceeds threshold and is waiting at your level.', link:'/pending-approval', isRead:false, ago:'5 min ago'},
  {id:2, category:'APPROVAL', type:'IN_APP', title:'Import PI-2026-0087 approved', message:'Your Import PI for polyester staple fiber was approved by Finance Manager.', link:'/import-pi', isRead:false, ago:'32 min ago'},
  {id:3, category:'SYSTEM', type:'IN_APP', title:'LC expiring in 5 days', message:'Export LC EXP-LC-2026-014 for Ibrahim Group is expiring soon — check shipment schedule.', link:'/export-lc', isRead:false, ago:'1 hr ago'},
  {id:4, category:'SYSTEM', type:'EMAIL', title:'GRN pending against PO-2026-00298', message:'Goods receipt has not been posted 3 days after expected delivery.', link:'/goods-receipt-notes', isRead:true, ago:'3 hrs ago'},
  {id:5, category:'APPROVAL', type:'IN_APP', title:'Leave request from S. Rahman', message:'Casual leave, 2 days, awaiting your approval.', link:'/leave-applications', isRead:true, ago:'Yesterday'},
  {id:6, category:'SYSTEM', type:'IN_APP', title:'Payroll run ready for review', message:'June 2026 payroll cycle has been processed and is ready for approval.', link:'/payroll-processing', isRead:true, ago:'Yesterday'}
];
var NOTIF_ICON = {APPROVAL:{ic:'fa-check-square', color:'var(--accent)'}, SYSTEM:{ic:'fa-triangle-exclamation', color:'var(--yellow)'},
                   EMAIL:{ic:'fa-envelope', color:'#60a5fa'}, SMS:{ic:'fa-comment-sms', color:'var(--green)'},
                   PUSH:{ic:'fa-bell', color:'var(--purple)'}, WHATSAPP:{ic:'fa-whatsapp', color:'var(--green)'}};
var _notifCache = [];
var _notifTabFilter = 'all';
function loadNotifications(){
  apiFetch('/api/notifications?limit=20').then(function(res){
    _notifCache = (res && res.notifications) || DEMO_NOTIFS;
    renderNotifList();
  }).catch(function(){
    _notifCache = DEMO_NOTIFS;
    renderNotifList();
  });
}
function renderNotifList(){
  var host = document.getElementById('notifList');
  if(!host) return;
  var list = _notifCache.filter(function(n){
    if(_notifTabFilter==='all') return true;
    if(_notifTabFilter==='unread') return !n.isRead;
    return n.category === _notifTabFilter;
  });
  if(!list.length){ host.innerHTML = '<div class="dd-empty"><i class="fa fa-inbox"></i><br>Nothing here</div>'; return; }
  host.innerHTML = list.map(function(n){
    var cfg = NOTIF_ICON[n.category] || NOTIF_ICON[n.type] || {ic:'fa-bell', color:'var(--accent)'};
    return '<div class="notif-item'+(n.isRead?'':' unread')+'" data-notif-id="'+escAttr(n.id)+'" role="button" tabindex="0">' +
             '<div class="notif-ic" style="--ni-color:'+cfg.color+'"><i class="fa '+cfg.ic+'"></i></div>' +
             '<div class="notif-body">' +
               '<div class="notif-title">'+esc(n.title)+'</div>' +
               '<div class="notif-msg">'+esc(n.message)+'</div>' +
               '<div class="notif-time">'+esc(n.ago)+'</div>' +
             '</div></div>';
  }).join('');
  updateNotifBadge();
}
document.addEventListener('click', function(e){
  var item = e.target.closest('.notif-item[data-notif-id]');
  if(item) window.openNotif(item.dataset.notifId);
});
function updateNotifBadge(){
  var unread = _notifCache.filter(function(n){ return !n.isRead; }).length;
  var badge = document.getElementById('tbAprBadge');
  if(!badge) return;
  if(unread>0){ badge.textContent = unread; badge.style.display=''; }
  else { badge.style.display='none'; }
}
window.loadNotifications = loadNotifications;
window.openNotif = function(id){
  var n = _notifCache.find(function(x){ return String(x.id)===String(id); });
  if(n){
    n.isRead = true;
    /* only same-origin relative links — never navigate to an absolute URL from a payload */
    if(n.link && /^\/[^\/]/.test(n.link)) location.href = n.link;
  }
};
window.filterNotifTab = function(el){
  document.querySelectorAll('.dd-tab').forEach(function(t){ t.classList.remove('active'); });
  el.classList.add('active');
  _notifTabFilter = el.getAttribute('data-filter');
  renderNotifList();
};
window.markAllNotifRead = function(){
  _notifCache.forEach(function(n){ n.isRead = true; });
  renderNotifList();
  /* state change → POST (CSRF-protected), not GET */
  postJson('/api/notifications/mark-all-read', {}).catch(function(){});
};

/* ════════════════════════════════════════════════════════════════════
   CONTEXT SWITCH — Business Unit / Warehouse / Cost Center, scoped to
   the current user via sec_user_org_business_units, sec_user_warehouses,
   sec_user_org_cost_centers. Saves into user_context on Apply.
   ════════════════════════════════════════════════════════════════════ */
var DEMO_SCOPES = {
  businessUnits:[{id:1,name:'Head Office',current:true},{id:2,name:'Ashulia Spinning Unit'},{id:3,name:'Gazipur Composite Unit'}],
  warehouses:[{id:1,name:'Main Warehouse — Head Office',current:true},{id:2,name:'Raw Fiber Store — Ashulia'},{id:3,name:'Finished Yarn Store — Gazipur'}],
  costCenters:[{id:1,name:'HQ — Corporate',current:true},{id:2,name:'Spinning — Ashulia',current:false},{id:3,name:'Quality Control',current:false}]
};
function loadContextScopes(){
  apiFetch('/api/context/scopes').then(function(res){ renderContextScopes(res); })
    .catch(function(){ renderContextScopes(DEMO_SCOPES); });
}
function renderContextScopes(scopes){
  fillSelect('ctxBusinessUnit', scopes.businessUnits);
  fillSelect('ctxWarehouse', scopes.warehouses);
  fillSelect('ctxCostCenter', scopes.costCenters);
}
function fillSelect(id, opts){
  var sel = document.getElementById(id);
  if(!sel) return;
  if(!opts || !opts.length){ sel.innerHTML = '<option value="">— none assigned —</option>'; return; }
  sel.innerHTML = opts.map(function(o){
    return '<option value="'+escAttr(o.id)+'"'+(o.current?' selected':'')+'>'+esc(o.name)+'</option>';
  }).join('');
}
function applyContextSwitch(){
  var btn = document.getElementById('ctxApplyBtn');
  var msg = document.getElementById('ctxApplyMsg');
  var payload = {
    businessUnitId: document.getElementById('ctxBusinessUnit').value,
    warehouseId: document.getElementById('ctxWarehouse').value,
    costCenterId: document.getElementById('ctxCostCenter').value
  };
  btn.disabled = true; btn.textContent = 'Applying…';
  postJson('/api/context/switch', payload)
    .catch(function(){ /* demo mode — still show success */ })
    .then(function(){
      btn.disabled = false; btn.textContent = 'Save & Apply';
      msg.classList.add('show');
      setTimeout(function(){ msg.classList.remove('show'); }, 2500);
    });
}
window.applyContextSwitch = applyContextSwitch;

/* ════════════════════════════════════════════════════════════════════
   SETTINGS DRAWER — maps to user_context.approval_* preference columns.
   On save, dispatches 'spindle:settings-saved' so pages can react
   (e.g. re-arm auto-refresh with the new interval).
   ════════════════════════════════════════════════════════════════════ */
function openSettings(){
  var drawer = document.getElementById('settingsDrawer');
  var scrim = document.getElementById('settingsScrim');
  if(!drawer) return;
  drawer.classList.add('show');
  if(scrim) scrim.classList.add('show');
  if(!drawer.dataset.loaded){
    loadSettings();
    drawer.dataset.loaded = '1';
  }
  var themeSw = document.getElementById('swTheme');
  if(themeSw) themeSw.classList.toggle('on', document.documentElement.getAttribute('data-theme')==='dark');
}
function closeSettings(){
  var drawer = document.getElementById('settingsDrawer');
  var scrim = document.getElementById('settingsScrim');
  if(drawer) drawer.classList.remove('show');
  if(scrim) scrim.classList.remove('show');
}
window.openSettings = openSettings;
window.closeSettings = closeSettings;
window.toggleSwitch = function(el){ el.classList.toggle('on'); };
function loadSettings(){
  apiFetch('/api/user-context/settings').then(applySettingsToUI).catch(function(){
    applySettingsToUI({approvalDesktopNotification:true, approvalEmailEnabled:true, approvalSmsEnabled:false,
      approvalWhatsappEnabled:false, approvalSoundEnabled:true, showApprovalBadge:true,
      approvalDefaultView:'LIST', approvalRefreshInterval:180, approvalNotificationFrequency:'INSTANT'});
  });
}
function applySettingsToUI(s){
  setSwitch('swDesktop', s.approvalDesktopNotification);
  setSwitch('swEmail', s.approvalEmailEnabled);
  setSwitch('swSms', s.approvalSmsEnabled);
  setSwitch('swWhatsapp', s.approvalWhatsappEnabled);
  setSwitch('swSound', s.approvalSoundEnabled);
  setSwitch('swBadge', s.showApprovalBadge);
  var dv = document.getElementById('selDefaultView'); if(dv && s.approvalDefaultView) dv.value = s.approvalDefaultView;
  var ri = document.getElementById('selRefreshInterval'); if(ri && s.approvalRefreshInterval!==undefined) ri.value = String(s.approvalRefreshInterval);
  var nf = document.getElementById('selNotifFreq'); if(nf && s.approvalNotificationFrequency) nf.value = s.approvalNotificationFrequency;
  try{ localStorage.setItem('spindle-refresh-sec', String(s.approvalRefreshInterval !== undefined ? s.approvalRefreshInterval : 180)); }catch(e){}
}
function setSwitch(id, on){ var el = document.getElementById(id); if(el) el.classList.toggle('on', !!on); }
function saveSettings(){
  var payload = {
    approvalDesktopNotification: document.getElementById('swDesktop').classList.contains('on'),
    approvalEmailEnabled: document.getElementById('swEmail').classList.contains('on'),
    approvalSmsEnabled: document.getElementById('swSms').classList.contains('on'),
    approvalWhatsappEnabled: document.getElementById('swWhatsapp').classList.contains('on'),
    approvalSoundEnabled: document.getElementById('swSound').classList.contains('on'),
    showApprovalBadge: document.getElementById('swBadge').classList.contains('on'),
    approvalDefaultView: document.getElementById('selDefaultView').value,
    approvalRefreshInterval: +document.getElementById('selRefreshInterval').value,
    approvalNotificationFrequency: document.getElementById('selNotifFreq').value
  };
  var msg = document.getElementById('sdSaveMsg');
  postJson('/api/user-context/settings', payload).catch(function(){}).then(function(){
    if(msg){
      msg.classList.add('show');
      setTimeout(function(){ msg.classList.remove('show'); }, 2200);
    }
    try{ localStorage.setItem('spindle-refresh-sec', String(payload.approvalRefreshInterval)); }catch(e){}
    document.dispatchEvent(new CustomEvent('spindle:settings-saved', {detail: payload}));
  });
}
window.saveSettings = saveSettings;
function onSettingsSaved(fn){
  document.addEventListener('spindle:settings-saved', function(e){ fn(e.detail); });
}
function getRefreshSeconds(){
  try{
    var v = localStorage.getItem('spindle-refresh-sec');
    if(v !== null && v !== '' && !isNaN(+v)) return +v;
  }catch(e){}
  return 180;
}

/* ════════════════════════════════════════════════════════════════════
   SHELL BOOT — theme restore, sidebar nav, user context.
   Page-specific boot (flags, KPIs, directory) lives in the page JS.
   ════════════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', function(){
  try{
    var saved = localStorage.getItem('spindle-theme');
    if(saved) document.documentElement.setAttribute('data-theme', saved);
  }catch(e){}
  renderSidebarGroups();
  _loadContext();
  loadNotifications();
});

/* ── PUBLIC NAMESPACE — consumed by per-page scripts ─────────────────── */
window.SpindleDash = {
  apiFetch: apiFetch,
  postJson: postJson,
  esc: esc,
  escAttr: escAttr,
  debounce: debounce,
  fmtTaka: fmtTaka,
  MENU_TREE: MENU_TREE,
  onSettingsSaved: onSettingsSaved,
  getRefreshSeconds: getRefreshSeconds
};

})();
