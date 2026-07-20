/* ============================================================
   travel.js — Travel Module Shared Utilities
   Depends on: jQuery, Select2, secureFetch
   ============================================================ */
(function () {
  'use strict';

  var travel = window.travel = {};

  // ── Format money (BDT lakh/crore grouping) ────────────────
  travel.formatMoney = function (n) {
    n = Number(n) || 0;
    var neg = n < 0; n = Math.abs(n);
    var parts = n.toFixed(2).split('.');
    var intp = parts[0];
    var last3 = intp.length > 3 ? intp.slice(-3) : intp;
    var rest  = intp.length > 3 ? intp.slice(0, -3) : '';
    if (rest) last3 = rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + last3;
    return '৳' + (neg ? '-' : '') + last3 + '.' + parts[1];
  };

  // ── Escape HTML ──────────────────────────────────────────
  travel.esc = function (s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  };

  // ── Short date for activity feeds ─────────────────────────
  travel.shortDate = function (d) {
    if (!d) return '—';
    var dt = new Date(d);
    if (isNaN(dt.getTime())) return String(d);
    return dt.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }) + ' ' +
           dt.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  };

  // ── Today as ISO string ──────────────────────────────────
  travel.today = function () {
    return new Date().toISOString().substring(0, 10);
  };

  // ── View field (label + value pair for show/readonly) ────
  travel.viewField = function (label, value) {
    return '<div class="col-md-4 travel-view-field">' +
      '<div class="travel-view-label">' + travel.esc(label) + '</div>' +
      '<div class="travel-view-value">' + (value != null ? value : '—') + '</div></div>';
  };

  // ── Status badge HTML ────────────────────────────────────
  travel.statusBadge = function (status) {
    if (!status) return '<span class="travel-badge travel-badge-pending">—</span>';
    var s = status.toUpperCase().replace(/[\s-]+/g, '_');
    var cls = 'travel-badge-pending';
    if (/DRAFT/.test(s))                cls = 'travel-badge-draft';
    else if (/CONFIRMED/.test(s))       cls = 'travel-badge-confirmed';
    else if (/PARTIAL/.test(s))          cls = 'travel-badge-partial';
    else if (/PAID/.test(s))            cls = 'travel-badge-paid';
    else if (/CANCELLED/.test(s))       cls = 'travel-badge-cancelled';
    else if (/COMPLETED/.test(s))       cls = 'travel-badge-completed';
    else if (/PENDING/.test(s))         cls = 'travel-badge-pending';
    else if (/ACTIVE/.test(s))          cls = 'travel-badge-active';
    else if (/ISSUED/.test(s))          cls = 'travel-badge-issued';
    else if (/VOID/.test(s))            cls = 'travel-badge-void';
    else if (/REFUND/.test(s))          cls = 'travel-badge-refunded';
    else if (/APPROVED/.test(s))        cls = 'travel-badge-approved';
    else if (/REJECTED/.test(s))        cls = 'travel-badge-rejected';
    else if (/COLLECTED/.test(s))       cls = 'travel-badge-collected';
    else if (/SUBMITTED/.test(s))       cls = 'travel-badge-submitted';
    else if (/UNPAID/.test(s))          cls = 'travel-badge-unpaid';
    return '<span class="travel-badge ' + cls + '">' + travel.esc(status) + '</span>';
  };

  // ── Service type chip ────────────────────────────────────
  travel.typeChip = function (type) {
    if (!type) return '';
    return '<span class="travel-type-chip travel-type-' + travel.esc(type.toUpperCase()) + '">' +
      travel.esc(type) + '</span>';
  };

  // ── Init Select2 with AJAX search ─────────────────────────
  travel.initSelect2 = function (selector, url, placeholder, extraParams) {
    if (typeof jQuery === 'undefined' || !jQuery.fn.select2) return;
    var $el = jQuery(selector);
    if ($el.hasClass('select2-hidden-accessible')) $el.select2('destroy');
    var ajaxData = function (params) {
      var data = { search: params.term || '', page: params.page || 1 };
      if (extraParams) Object.assign(data, typeof extraParams === 'function' ? extraParams() : extraParams);
      return data;
    };
    $el.select2({
      width: '100%',
      placeholder: placeholder || 'Search…',
      allowClear: true,
      minimumInputLength: 0,
      ajax: {
        url: url,
        dataType: 'json',
        delay: 300,
        data: ajaxData,
        processResults: function (d) {
          return { results: (d.items || []).map(function (i) { return { id: i.id, text: i.text }; }) };
        },
        cache: true
      }
    });
    return $el;
  };

  // ── Init a simple DataTable ──────────────────────────────
  travel.initDataTable = function (selector, url, columns, opts) {
    if (typeof jQuery === 'undefined' || !jQuery.fn.DataTable) return null;
    var defaults = {
      processing: true,
      serverSide: false,
      responsive: true,
      pageLength: 25,
      language: {
        emptyTable: '<div class="travel-empty-state"><div class="travel-empty-icon"><i class="fa fa-inbox"></i></div><div class="travel-empty-title">No data found.</div></div>',
        processing: '<span class="spinner-border spinner-border-sm me-2"></span> Loading…'
      },
      ajax: { url: url, type: 'GET' },
      columns: columns,
      order: []
    };
    if (opts) Object.assign(defaults, opts);
    return jQuery(selector).DataTable(defaults);
  };

  // ── Open a view modal with fields ─────────────────────────
  travel.openViewModal = function (modalId, title, fieldsHtml, extraHtml) {
    var body = document.getElementById(modalId + 'ViewBody');
    if (!body) return;
    var html = '<div class="hs-box"><div class="row g-2">' + fieldsHtml + '</div>';
    if (extraHtml) html += extraHtml;
    html += '</div>';
    body.innerHTML = html;
    var el = document.getElementById(modalId + 'ViewModal');
    if (el && typeof bootstrap !== 'undefined') bootstrap.Modal.getOrCreateInstance(el).show();
  };

  // ── Reload a DataTable or table body ─────────────────────
  travel.reload = function (dataTableOrFn) {
    if (typeof dataTableOrFn === 'function') dataTableOrFn();
    else if (dataTableOrFn && dataTableOrFn.ajax) dataTableOrFn.ajax.reload(null, false);
  };

  // ── Flash a message (using Swal or fallback) ─────────────
  travel.toast = function (message, type) {
    type = type || 'success';
    if (typeof Swal !== 'undefined') {
      Swal.fire({ icon: type, title: message, timer: 1800, showConfirmButton: false, toast: true, position: 'top-end' });
    } else {
      alert(message);
    }
  };

  // ── Confirmation dialog ──────────────────────────────────
  travel.confirm = function (title, text, confirmColor, confirmText) {
    return Swal.fire({
      title: title || 'Are you sure?',
      text: text || '',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: confirmColor || '#dc3545',
      confirmButtonText: confirmText || 'Confirm'
    }).then(function (r) { return r.isConfirmed; });
  };

})();
