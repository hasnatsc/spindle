/* ═══════════════════════════════════════════════════════════════════════
   FILE: src/main/resources/static/js/dashboard/admin-flags.js
   Spindle ERP — Admin Flags & Navigation Command Center (page JS)

   Loaded AFTER dashboard-core.js by the layout's pageJs slot.
   Consumes window.SpindleDash { apiFetch, MENU_TREE }.

   DATA WIRING
     GET /dashboard/admin-flags   → flags[] (see FLAG SCHEMA below)
     GET /dashboard/erp-summary   → module KPI numbers (same endpoint the
                                     main control tower already uses)
   Falls back to demo data if either call fails, so the page always renders.
   ═══════════════════════════════════════════════════════════════════════ */
(function(){
'use strict';

var apiFetch  = window.SpindleDash.apiFetch;
var MENU_TREE = window.SpindleDash.MENU_TREE;

/* ════════════════════════════════════════════════════════════════════
   FLAG SCHEMA (demo fallback) — replace via GET /dashboard/admin-flags
   Each flag: {id, severity, icon, label, value, sub, module, url}
   severity ∈ critical | warning | info | good
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
   RENDER: Flag cards
   ════════════════════════════════════════════════════════════════════ */
function renderFlags(flags){
  var host = document.getElementById('flagGrid');
  var counts = {critical:0, warning:0, info:0, good:0};
  var html = '';
  flags.forEach(function(f){
    counts[f.sev] = (counts[f.sev]||0) + 1;
    html += '<div class="flag-card sev-'+f.sev+'" onclick="location.href=\''+f.url+'\'">' +
              '<div class="flag-ic"><i class="fa '+f.icon+'"></i></div>' +
              '<div class="flag-body">' +
                '<div class="flag-label">'+f.label+'</div>' +
                '<div class="flag-val">'+f.value+'</div>' +
                '<div class="flag-sub">'+f.sub+'</div>' +
              '</div>' +
              '<div class="flag-module"><i class="fa '+SEV_ICON[f.sev]+'"></i></div>' +
            '</div>';
  });
  host.innerHTML = html;
  document.getElementById('flagTotalCnt').textContent = flags.length + ' flags';
  document.getElementById('stripCrit').textContent = counts.critical||0;
  document.getElementById('stripWarn').textContent = counts.warning||0;
  document.getElementById('stripInfo').textContent = counts.info||0;
  document.getElementById('stripGood').textContent = counts.good||0;

  var badgeN = (counts.critical||0) + (counts.warning||0);
  var sbBadge = document.getElementById('sbFlagBadge');
  var tbBadge = document.getElementById('tbAprBadge');
  if(badgeN>0){
    if(sbBadge){ sbBadge.textContent = badgeN; sbBadge.style.display=''; }
    if(tbBadge){ tbBadge.textContent = badgeN; tbBadge.style.display=''; }
  } else {
    if(sbBadge) sbBadge.style.display='none';
    if(tbBadge) tbBadge.style.display='none';
  }
}

/* ════════════════════════════════════════════════════════════════════
   RENDER: Module command-center cards
   ════════════════════════════════════════════════════════════════════ */
function renderModules(kpiData, flags){
  var host = document.getElementById('modGrid');
  var html = '';
  MENU_TREE.forEach(function(mod){
    var flagCount = flags.filter(function(f){ return f.module===mod.code || f.module===shortCode(mod.code); }).length;
    var kpis = MODULE_KPIS[mod.code] || ['—','—','—'];
    var kd = (kpiData && kpiData[mod.code]) || {};
    var links = [];
    mod.groups.forEach(function(g){ g.items.forEach(function(it){ links.push(it); }); });
    // show first 6 links, directory has the rest
    var shown = links.slice(0,6);

    html += '<div class="mod-card">' +
      '<div class="mod-head">' +
        '<div class="mod-ic" style="--mod-color:'+mod.color+'"><i class="fa '+mod.icon+'"></i></div>' +
        '<div><div class="mod-title">'+mod.name+'</div><div class="mod-desc">'+links.length+' screens</div></div>' +
        '<div class="mod-flagbadge '+(flagCount>0?'attn':'ok')+'">'+(flagCount>0?flagCount+' flags':'OK')+'</div>' +
      '</div>' +
      '<div class="mod-kpis" style="--mod-color:'+mod.color+'">' +
        kpis.map(function(k){
          var val = kd[k] !== undefined ? kd[k] : '—';
          return '<div class="mod-kpi"><div class="v">'+val+'</div><div class="l">'+k+'</div></div>';
        }).join('') +
      '</div>' +
      '<div class="mod-links">' +
        shown.map(function(it){ return '<a href="'+it.u+'">'+it.n+'</a>'; }).join('') +
        (links.length>6 ? '<a href="#dirCol_'+mod.code+'" onclick="event.preventDefault();scrollToDir(\''+mod.code+'\')">+'+(links.length-6)+' more…</a>' : '') +
      '</div>' +
      '<div class="mod-foot"><a href="'+mod.url+'" style="--mod-color:'+mod.color+'">Open '+mod.name+' <i class="fa fa-arrow-right"></i></a></div>' +
    '</div>';
  });
  host.innerHTML = html;
}
function shortCode(code){
  var map = {FINANCE_ACCOUNTS:'FINANCE', PURCHASE_SUPPLIER:'PURCHASE', INVENTORY_WAREHOUSE:'INVENTORY',
             SALES_CUSTOMER_OPERATIONS:'SALES', CORE_SECURITY:'CORE'};
  return map[code] || code;
}
window.scrollToDir = function(modCode){
  var el = document.getElementById('dirCol_'+modCode);
  if(el) el.scrollIntoView({behavior:'smooth', block:'start'});
};

/* ════════════════════════════════════════════════════════════════════
   RENDER: Full navigation directory
   ════════════════════════════════════════════════════════════════════ */
function renderDirectory(){
  var host = document.getElementById('dirGrid');
  var html = '';
  var total = 0;
  MENU_TREE.forEach(function(mod){
    html += '<div class="dir-col" id="dirCol_'+mod.code+'">';
    html += '<div class="dir-mod" style="--dm-color:'+mod.color+'"><i class="fa '+mod.icon+'"></i> '+mod.name+'</div>';
    mod.groups.forEach(function(g){
      html += '<div class="dir-group" data-text="'+g.name.toLowerCase()+'">'+g.name+'</div>';
      g.items.forEach(function(it){
        total++;
        html += '<a class="dir-link" href="'+it.u+'" data-text="'+(it.n+' '+it.u+' '+mod.name+' '+g.name).toLowerCase()+'">' +
                  '<span>'+it.n+'</span><span class="path">'+it.u+'</span></a>';
      });
    });
    html += '</div>';
  });
  host.innerHTML = html;
  document.getElementById('dirTotalCnt').textContent = total + ' screens';
  document.getElementById('dirVisibleCnt').textContent = 'Showing all '+total+' screens';
}
function filterDirectory(q){
  q = (q||'').toLowerCase().trim();
  var links = document.querySelectorAll('.dir-link');
  var visible = 0;
  links.forEach(function(l){
    var match = !q || l.getAttribute('data-text').indexOf(q) !== -1;
    l.classList.toggle('dir-hide', !match);
    if(match) visible++;
  });
  document.querySelectorAll('.dir-group').forEach(function(g){
    // hide group label if all following links until next group are hidden
    var next = g.nextElementSibling; var anyVisible = false;
    while(next && !next.classList.contains('dir-group')){
      if(!next.classList.contains('dir-hide')) anyVisible = true;
      next = next.nextElementSibling;
    }
    g.classList.toggle('dir-hide', !anyVisible);
  });
  document.getElementById('dirVisibleCnt').textContent = q ? ('Showing '+visible+' of '+document.querySelectorAll('.dir-link').length+' screens') : ('Showing all '+document.querySelectorAll('.dir-link').length+' screens');
}
window.filterDirectory = filterDirectory;

/* ════════════════════════════════════════════════════════════════════
   LOAD ALL — flags + module KPIs
   ════════════════════════════════════════════════════════════════════ */
function _render(flagsPayload, kpiPayload){
  var flags = (flagsPayload && flagsPayload.flags) || DEMO_FLAGS;
  renderFlags(flags);
  renderModules(kpiPayload, flags);
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
   PAGE BOOT
   ════════════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', function(){
  renderDirectory();
  _loadAll();
  setInterval(_loadAll, 180000);
});

})();
