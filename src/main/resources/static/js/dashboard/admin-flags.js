/* ═══════════════════════════════════════════════════════════════════════
   FILE: src/main/resources/static/js/dashboard/admin-flags.js
   Spindle ERP — Admin Flags & Navigation Command Center (page JS)

   Loaded AFTER dashboard-core.js by the layout's pageJs slot.
   Consumes window.SpindleDash { apiFetch, esc, escAttr, debounce,
   MENU_TREE, onSettingsSaved, getRefreshSeconds }.

   DATA WIRING
     GET /dashboard/admin-flags   → { flags:[...] }  (see FLAG SCHEMA)
     GET /dashboard/erp-summary   → module KPI numbers (same endpoint the
                                     main control tower already uses)
   Falls back to demo data if either call fails — a "demo data" badge is
   shown in the topbar strip so the state is never ambiguous.

   SECURITY: every field that can arrive from the server (label, value,
   sub, url, icon, module, severity) is escaped / validated before it
   touches innerHTML. Navigation is delegated via data-url — no inline
   onclick with interpolated strings. Only same-origin relative URLs
   are honoured.
   ═══════════════════════════════════════════════════════════════════════ */
(function(){
'use strict';

var D         = window.SpindleDash;
var apiFetch  = D.apiFetch;
var esc       = D.esc;
var escAttr   = D.escAttr;
var debounce  = D.debounce;
var MENU_TREE = D.MENU_TREE;

/* ════════════════════════════════════════════════════════════════════
   FLAG SCHEMA (demo fallback) — replace via GET /dashboard/admin-flags
   Each flag: {id, severity|sev, icon, label, value, sub, module, url}
   severity ∈ critical | warning | info | good   (both key names accepted)
   ════════════════════════════════════════════════════════════════════ */
var DEMO_FLAGS = [
  {sev:'critical', icon:'fa-hourglass-half', label:'Pending Approvals', value:'—', sub:'awaiting action', module:'CORE', url:'/pending-approval'},
  {sev:'critical', icon:'fa-file-invoice-dollar', label:'Overdue Receivables', value:'—', sub:'past due date', module:'FINANCE', url:'/financial-reports/receivable-aging'},
  {sev:'critical', icon:'fa-file-invoice', label:'Overdue Payables', value:'—', sub:'past due date', module:'FINANCE', url:'/financial-reports/payable-aging'},
  {sev:'critical', icon:'fa-ban', label:'Expired LCs', value:'—', sub:'export + import', module:'COMMERCIAL', url:'/lc-pi'},
  {sev:'warning', icon:'fa-clock', label:'LCs Expiring Soon', value:'—', sub:'within 15 days', module:'COMMERCIAL', url:'/lc-pi'},
  {sev:'warning', icon:'fa-file-circle-question', label:'PI Pending', value:'—', sub:'export + import', module:'COMMERCIAL', url:'/lc-pi'},
  {sev:'warning', icon:'fa-boxes-stacked', label:'Below Reorder Level', value:'—', sub:'items need restock', module:'INVENTORY', url:'/inventory-stocks'},
  {sev:'warning', icon:'fa-flask-vial', label:'QC Hold Lots', value:'—', sub:'blocked from issue', module:'INVENTORY', url:'/inventory-lots'},
  {sev:'warning', icon:'fa-book', label:'Unposted Vouchers', value:'—', sub:'journal entries', module:'FINANCE', url:'/journal-entries'},
  {sev:'warning', icon:'fa-chart-pie', label:'Over-Budget Lines', value:'—', sub:'exceeding allocation', module:'BUDGET', url:'/financial-analytics/dashboard'},
  {sev:'warning', icon:'fa-dolly', label:'GRN Pending', value:'—', sub:'against open POs', module:'PURCHASE', url:'/goods-receipt-notes'},
  {sev:'warning', icon:'fa-truck-ramp-box', label:'Delivery Pending', value:'—', sub:'against open SOs', module:'SALES', url:'/delivery-orders'},
  {sev:'info', icon:'fa-calendar-check', label:'Pending Leave Requests', value:'—', sub:'awaiting approval', module:'HRM', url:'/leave-applications'},
  {sev:'info', icon:'fa-money-check-dollar', label:'Payroll Run Status', value:'—', sub:'current cycle', module:'HRM', url:'/payroll-processing'},
  {sev:'info', icon:'fa-industry', label:'Production In Progress', value:'—', sub:'active orders', module:'PRODUCTION', url:'/production-orders'},
  {sev:'info', icon:'fa-clipboard-check', label:'Pending Production Approval', value:'—', sub:'orders + recipes', module:'PRODUCTION', url:'/production-orders'},
  {sev:'info', icon:'fa-building-shield', label:'Depreciation Due', value:'—', sub:'fixed assets this period', module:'FIXED_ASSETS', url:'/financial-analytics/dashboard'},
  {sev:'info', icon:'fa-list-check', label:'Audit Log Anomalies', value:'—', sub:'flagged entity changes', module:'CORE', url:'/approval-panel'},
  {sev:'good', icon:'fa-shield-check', label:'Active Users', value:'—', sub:'signed in this week', module:'CORE', url:'/users'},
  {sev:'good', icon:'fa-warehouse', label:'Active Warehouses', value:'—', sub:'across business units', module:'INVENTORY', url:'/warehouses'}
];

var SEV_ICON = {critical:'fa-triangle-exclamation', warning:'fa-circle-exclamation', info:'fa-circle-info', good:'fa-circle-check'};
var SEV_VALID = {critical:1, warning:1, info:1, good:1};
var FA_RE = /^fa-[a-z0-9-]+$/;

/* Normalize one flag from the API: accept sev|severity, whitelist icon
   and severity, allow only relative same-origin URLs.               */
function normFlag(f){
  var sev = String(f.sev || f.severity || 'info').toLowerCase();
  if(!SEV_VALID[sev]) sev = 'info';
  var icon = String(f.icon || 'fa-flag');
  if(!FA_RE.test(icon)) icon = 'fa-flag';
  var url = String(f.url || '');
  if(!/^\/[^\/]/.test(url)) url = '';
  return {sev:sev, icon:icon, label:f.label||'', value:(f.value===0?0:(f.value||'—')), sub:f.sub||'', module:f.module||'', url:url};
}

/* ════════════════════════════════════════════════════════════════════
   MODULE SUMMARY CONFIG — maps root module → KPI keys expected from
   /dashboard/erp-summary (same payload the main control tower uses)
   ════════════════════════════════════════════════════════════════════ */
var MODULE_KPIS = {
  CORE_SECURITY:['Users','Roles','Pending Apr'],
  HRM:['Employees','Present Today','On Leave'],
  PURCHASE_SUPPLIER:['Open POs','GRN Pending','MTD Value'],
  INVENTORY_WAREHOUSE:['SKUs','Warehouses','Low Stock'],
  SALES_CUSTOMER_OPERATIONS:['Open SOs','Unposted Inv','MTD Value'],
  FINANCE_ACCOUNTS:['Total AR','Total AP','Cash & Bank'],
  PRODUCTION:['Active Orders','MTD Output','BOMs'],
  COMMERCIAL:['Active LCs','LC Value','PI Pending'],
  REPORTS_ANALYTICS:['Reports','Dashboards','Exports']
};

/* ════════════════════════════════════════════════════════════════════
   STATE
   ════════════════════════════════════════════════════════════════════ */
var _flags = [];          /* normalized flags from last load        */
var _sevFilter = null;    /* null = all | 'critical' | 'warning'…   */
var _refreshTimer = null;
var _usingDemo = false;

/* ════════════════════════════════════════════════════════════════════
   SKELETONS — shown while the first fetch is in flight
   ════════════════════════════════════════════════════════════════════ */
function renderSkeletons(){
  var fg = document.getElementById('flagGrid');
  if(fg && !fg.children.length){
    var s = '';
    for(var i=0;i<8;i++) s += '<div class="flag-card skel-card"><div class="skel skel-ic"></div><div class="flag-body"><div class="skel skel-line w60"></div><div class="skel skel-line w40 tall"></div><div class="skel skel-line w70"></div></div></div>';
    fg.innerHTML = s;
  }
  var mg = document.getElementById('modGrid');
  if(mg && !mg.children.length){
    var m = '';
    for(var j=0;j<6;j++) m += '<div class="mod-card skel-card" style="min-height:220px"><div class="mod-head"><div class="skel skel-ic"></div><div style="flex:1"><div class="skel skel-line w50"></div><div class="skel skel-line w30"></div></div></div></div>';
    mg.innerHTML = m;
  }
}

/* ════════════════════════════════════════════════════════════════════
   RENDER: Flag cards (with severity filter + delegated navigation)
   ════════════════════════════════════════════════════════════════════ */
function renderFlags(){
  var host = document.getElementById('flagGrid');
  if(!host) return;
  var counts = {critical:0, warning:0, info:0, good:0};
  _flags.forEach(function(f){ counts[f.sev]++; });

  var shown = _sevFilter ? _flags.filter(function(f){ return f.sev===_sevFilter; }) : _flags;
  var html = '';
  shown.forEach(function(f){
    html += '<div class="flag-card sev-'+f.sev+'" role="link" tabindex="0"'+(f.url?' data-url="'+escAttr(f.url)+'"':'')+'>' +
              '<div class="flag-ic"><i class="fa '+f.icon+'"></i></div>' +
              '<div class="flag-body">' +
                '<div class="flag-label">'+esc(f.label)+'</div>' +
                '<div class="flag-val">'+esc(f.value)+'</div>' +
                '<div class="flag-sub">'+esc(f.sub)+'</div>' +
              '</div>' +
              '<div class="flag-module"><i class="fa '+SEV_ICON[f.sev]+'"></i></div>' +
            '</div>';
  });
  host.innerHTML = html || '<div class="flag-empty"><i class="fa fa-filter-circle-xmark"></i> No '+esc(_sevFilter||'')+' flags right now</div>';

  var cnt = document.getElementById('flagTotalCnt');
  if(cnt) cnt.textContent = _sevFilter ? (shown.length+' of '+_flags.length+' flags') : (_flags.length+' flags');
  setText('stripCrit', counts.critical); setText('stripWarn', counts.warning);
  setText('stripInfo', counts.info);     setText('stripGood', counts.good);

  /* strip chip active state */
  document.querySelectorAll('#tbStrip .strip-chip[data-sev]').forEach(function(ch){
    ch.classList.toggle('sel', ch.getAttribute('data-sev')===_sevFilter);
  });

  var badgeN = counts.critical + counts.warning;
  ['sbFlagBadge','tbAprBadge'].forEach(function(id){
    var b = document.getElementById(id);
    if(!b) return;
    if(badgeN>0){ b.textContent = badgeN; b.style.display=''; }
    else { b.style.display='none'; }
  });
}
function setText(id, v){ var el = document.getElementById(id); if(el) el.textContent = v||0; }

/* strip chip click → toggle severity filter */
window.filterFlagsBySev = function(sev){
  _sevFilter = (_sevFilter===sev) ? null : sev;
  renderFlags();
};
/* flag card navigation — delegated, keyboard-friendly */
document.addEventListener('click', function(e){
  var card = e.target.closest('.flag-card[data-url]');
  if(card) location.href = card.dataset.url;
});
document.addEventListener('keydown', function(e){
  if(e.key !== 'Enter' && e.key !== ' ') return;
  var card = e.target.closest && e.target.closest('.flag-card[data-url]');
  if(card && e.key === 'Enter'){ location.href = card.dataset.url; return; }
  var chip = e.target.closest && e.target.closest('.strip-chip[data-sev]');
  if(chip){ e.preventDefault(); window.filterFlagsBySev(chip.getAttribute('data-sev')); }
});

/* ════════════════════════════════════════════════════════════════════
   RENDER: Module command-center cards
   ════════════════════════════════════════════════════════════════════ */
function renderModules(kpiData){
  var host = document.getElementById('modGrid');
  if(!host) return;
  var html = '';
  MENU_TREE.forEach(function(mod){
    var flagCount = _flags.filter(function(f){ return f.module===mod.code || f.module===shortCode(mod.code); }).length;
    var kpis = MODULE_KPIS[mod.code] || ['—','—','—'];
    var kd = (kpiData && kpiData[mod.code]) || {};
    var links = [];
    mod.groups.forEach(function(g){ g.items.forEach(function(it){ links.push(it); }); });
    var shown = links.slice(0,6);

    html += '<div class="mod-card">' +
      '<div class="mod-head">' +
        '<div class="mod-ic" style="--mod-color:'+escAttr(mod.color)+'"><i class="fa '+escAttr(mod.icon)+'"></i></div>' +
        '<div><div class="mod-title">'+esc(mod.name)+'</div><div class="mod-desc">'+links.length+' screens</div></div>' +
        '<div class="mod-flagbadge '+(flagCount>0?'attn':'ok')+'">'+(flagCount>0?flagCount+' flags':'OK')+'</div>' +
      '</div>' +
      '<div class="mod-kpis" style="--mod-color:'+escAttr(mod.color)+'">' +
        kpis.map(function(k){
          var val = kd[k] !== undefined ? kd[k] : '—';
          return '<div class="mod-kpi"><div class="v">'+esc(val)+'</div><div class="l">'+esc(k)+'</div></div>';
        }).join('') +
      '</div>' +
      '<div class="mod-links">' +
        shown.map(function(it){ return '<a href="'+escAttr(it.u)+'">'+esc(it.n)+'</a>'; }).join('') +
        (links.length>6 ? '<a href="#" class="more-link" data-dir="'+escAttr(mod.code)+'">+'+(links.length-6)+' more…</a>' : '') +
      '</div>' +
      '<div class="mod-foot"><a href="'+escAttr(mod.url)+'" style="--mod-color:'+escAttr(mod.color)+'">Open '+esc(mod.name)+' <i class="fa fa-arrow-right"></i></a></div>' +
    '</div>';
  });
  host.innerHTML = html;
}
function shortCode(code){
  var map = {FINANCE_ACCOUNTS:'FINANCE', PURCHASE_SUPPLIER:'PURCHASE', INVENTORY_WAREHOUSE:'INVENTORY',
             SALES_CUSTOMER_OPERATIONS:'SALES', CORE_SECURITY:'CORE'};
  return map[code] || code;
}
document.addEventListener('click', function(e){
  var more = e.target.closest('.more-link[data-dir]');
  if(more){ e.preventDefault(); scrollToDir(more.dataset.dir); }
});
function scrollToDir(modCode){
  var el = document.getElementById('dirCol_'+modCode);
  if(el) el.scrollIntoView({behavior:'smooth', block:'start'});
}
window.scrollToDir = scrollToDir;

/* ════════════════════════════════════════════════════════════════════
   RENDER: Full navigation directory (static MENU_TREE — escaped anyway
   for defence in depth; module codes are attribute-escaped)
   ════════════════════════════════════════════════════════════════════ */
function renderDirectory(){
  var host = document.getElementById('dirGrid');
  if(!host) return;
  var html = '';
  var total = 0;
  MENU_TREE.forEach(function(mod){
    html += '<div class="dir-col" id="dirCol_'+escAttr(mod.code)+'">';
    html += '<div class="dir-mod" style="--dm-color:'+escAttr(mod.color)+'"><i class="fa '+escAttr(mod.icon)+'"></i> '+esc(mod.name)+'</div>';
    mod.groups.forEach(function(g){
      html += '<div class="dir-group" data-text="'+escAttr(g.name.toLowerCase())+'">'+esc(g.name)+'</div>';
      g.items.forEach(function(it){
        total++;
        html += '<a class="dir-link" href="'+escAttr(it.u)+'" data-text="'+escAttr((it.n+' '+it.u+' '+mod.name+' '+g.name).toLowerCase())+'">' +
                  '<span>'+esc(it.n)+'</span><span class="path">'+esc(it.u)+'</span></a>';
      });
    });
    html += '</div>';
  });
  host.innerHTML = html;
  setTextRaw('dirTotalCnt', total + ' screens');
  setTextRaw('dirVisibleCnt', 'Showing all '+total+' screens');
}
function setTextRaw(id, v){ var el = document.getElementById(id); if(el) el.textContent = v; }

var filterDirectory = debounce(function(q){
  q = (q||'').toLowerCase().trim();
  var links = document.querySelectorAll('.dir-link');
  var visible = 0;
  links.forEach(function(l){
    var match = !q || l.getAttribute('data-text').indexOf(q) !== -1;
    l.classList.toggle('dir-hide', !match);
    if(match) visible++;
  });
  document.querySelectorAll('.dir-group').forEach(function(g){
    var next = g.nextElementSibling; var anyVisible = false;
    while(next && !next.classList.contains('dir-group')){
      if(!next.classList.contains('dir-hide')) anyVisible = true;
      next = next.nextElementSibling;
    }
    g.classList.toggle('dir-hide', !anyVisible);
  });
  setTextRaw('dirVisibleCnt', q ? ('Showing '+visible+' of '+links.length+' screens') : ('Showing all '+links.length+' screens'));
}, 120);
window.filterDirectory = filterDirectory;

/* ════════════════════════════════════════════════════════════════════
   LOAD ALL — flags + module KPIs
   ════════════════════════════════════════════════════════════════════ */
function _render(flagsPayload, kpiPayload){
  _usingDemo = !(flagsPayload && flagsPayload.flags);
  _flags = ((flagsPayload && flagsPayload.flags) || DEMO_FLAGS).map(normFlag);
  renderFlags();
  renderModules(kpiPayload);

  var demoBadge = document.getElementById('stripDemoBadge');
  if(demoBadge) demoBadge.style.display = _usingDemo ? '' : 'none';

  var now = new Date();
  var upd = document.getElementById('tbLastUpdated');
  if(upd) upd.textContent =
    'Updated '+now.toLocaleDateString('en-BD')+' '+now.toLocaleTimeString('en-BD',{hour:'2-digit',minute:'2-digit'});
  var dt = document.getElementById('tbDate');
  if(dt) dt.textContent =
    now.toLocaleDateString('en-BD',{weekday:'short', year:'numeric', month:'short', day:'numeric'});
}
function _loadAll(){
  var upd = document.getElementById('tbLastUpdated');
  if(upd) upd.textContent = 'Refreshing…';
  Promise.all([
    apiFetch('/dashboard/admin-flags').catch(function(){ return null; }),
    apiFetch('/dashboard/erp-summary').catch(function(){ return null; })
  ]).then(function(results){
    _render(results[0], results[1]);
  });
}
window._loadAll = _loadAll;

/* ════════════════════════════════════════════════════════════════════
   AUTO-REFRESH — honours the drawer's "Auto-refresh interval" setting
   (0 = off), re-arms live when settings are saved, and pauses while
   the tab is hidden to save server round-trips.
   ════════════════════════════════════════════════════════════════════ */
function armRefresh(){
  if(_refreshTimer){ clearInterval(_refreshTimer); _refreshTimer = null; }
  var sec = D.getRefreshSeconds();
  if(sec > 0){
    _refreshTimer = setInterval(function(){
      if(!document.hidden) _loadAll();
    }, sec * 1000);
  }
}
D.onSettingsSaved(function(){ armRefresh(); });
document.addEventListener('visibilitychange', function(){
  if(!document.hidden) _loadAll();   /* catch up immediately on return */
});

/* ════════════════════════════════════════════════════════════════════
   PAGE BOOT
   ════════════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', function(){
  renderSkeletons();
  renderDirectory();
  _loadAll();
  armRefresh();
});

})();
