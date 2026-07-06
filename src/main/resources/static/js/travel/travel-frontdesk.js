/* travel/travel-frontdesk.js
 * Walk-in booking console. Backend-free: talks only to endpoints that
 * already exist (see travel-frontdesk.html header for the full map).
 *
 * Depends on the app globals: jQuery, Select2, Swal, secureFetch,
 * hsDisableButton / hsEnableButton.
 */
(function () {
  'use strict';

  // ── Endpoints ──────────────────────────────────────────────────────────────
  var URL_CUSTOMER = '/accounts/sub-accounts/search';
  var URL_SAVE     = '/travel/bookings/save';
  var URL_CONFIRM  = '/travel/bookings/confirm/';   // + {id}
  var URL_FEED     = '/travel/bookings/list';

  // Per-type reference lookup config. AIR & VISA are description-only.
  var REF = {
    HOTEL:   { label: 'Hotel',   url: '/travel/hotels/search',   ph: 'Search hotel by name / code…' },
    PACKAGE: { label: 'Package', url: '/travel/packages/search', ph: 'Search package…' },
    TOUR:    { label: 'Tour',    url: '/travel/tours/search',    ph: 'Search tour…' },
    AIR:     { label: 'Air',     url: null, note: 'Air is captured as a description here; issue the ticket in Air Tickets after confirming.' },
    VISA:    { label: 'Visa',    url: null, note: 'Visa is captured as a description here; process it in Visa Applications after confirming.' }
  };

  // ── State ──────────────────────────────────────────────────────────────────
  var mode = 'registered';           // 'registered' | 'walkin'
  var currentType = 'HOTEL';
  var refSel = { id: null, text: '' }; // currently picked reference (hotel/pkg/tour)
  var cart = [];                       // [{ serviceType, referenceId, referenceText, description, qty, unitPrice, lineTotal }]

  // ── DOM helpers ─────────────────────────────────────────────────────────────
  function $id(id) { return document.getElementById(id); }
  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
  function today() { return new Date().toISOString().substring(0, 10); }

  // South-Asian (lakh/crore) grouping, e.g. 1,23,45,678.00
  function money(n) {
    n = Number(n) || 0;
    var neg = n < 0; n = Math.abs(n);
    var parts = n.toFixed(2).split('.');
    var intp = parts[0];
    var last3 = intp.length > 3 ? intp.slice(-3) : intp;
    var rest  = intp.length > 3 ? intp.slice(0, -3) : '';
    if (rest) last3 = rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + last3;
    return '৳' + (neg ? '-' : '') + last3 + '.' + parts[1];
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CUSTOMER MODE
  // ═══════════════════════════════════════════════════════════════════════════
  function initCustomerS2() {
    var $el = $('#fdPartyId');
    if ($el.hasClass('select2-hidden-accessible')) $el.select2('destroy');
    $el.select2({
      width: '100%', placeholder: '— Search customer —', allowClear: true, minimumInputLength: 0,
      ajax: {
        url: URL_CUSTOMER, dataType: 'json', delay: 250,
        data: function (p) { return { search: p.term || '', subAccountType: 'CUSTOMER', page: p.page || 1 }; },
        processResults: function (d, p) {
          p.page = p.page || 1;
          return {
            results: (d.items || []).map(function (i) { return { id: i.id, text: i.text }; }),
            pagination: { more: d.hasMore || false }
          };
        },
        cache: false
      }
    });
  }

  window.fdSetCustomerMode = function (m) {
    mode = m;
    var reg = (m === 'registered');
    $id('fdRegisteredFields').style.display = reg ? '' : 'none';
    $id('fdWalkinFields').style.display     = reg ? 'none' : '';
    $id('fdModeRegistered').classList.toggle('active', reg);
    $id('fdModeWalkin').classList.toggle('active', !reg);
  };

  // ═══════════════════════════════════════════════════════════════════════════
  // SERVICE TABS + REFERENCE LOOKUP
  // ═══════════════════════════════════════════════════════════════════════════
  function destroyRefS2() {
    var $el = $('#fdRefSelect');
    if ($el.hasClass('select2-hidden-accessible')) $el.select2('destroy');
    $el.empty();
  }

  function initRefS2(cfg) {
    var $el = $('#fdRefSelect');
    $el.select2({
      width: '100%', placeholder: cfg.ph || 'Search…', allowClear: true, minimumInputLength: 0,
      ajax: {
        url: cfg.url, dataType: 'json', delay: 250,
        data: function (p) { return { search: p.term || '', page: p.page || 1 }; },
        processResults: function (d) {
          return { results: (d.items || []).map(function (i) { return { id: i.id, text: i.text }; }) };
        },
        cache: false
      }
    });
    $el.off('select2:select select2:clear');
    $el.on('select2:select', function (e) {
      refSel = { id: e.params.data.id, text: e.params.data.text };
      var desc = $id('fdLineDesc');
      if (!desc.value.trim()) desc.value = refSel.text; // convenience prefill
    });
    $el.on('select2:clear', function () { refSel = { id: null, text: '' }; });
  }

  window.fdSwitchTab = function (btn) {
    document.querySelectorAll('#fdServiceTabs .nav-link').forEach(function (b) { b.classList.remove('active'); });
    btn.classList.add('active');
    currentType = btn.getAttribute('data-type');
    refSel = { id: null, text: '' };

    var cfg = REF[currentType];
    $id('fdRefLabel').textContent = cfg.label;
    destroyRefS2();

    if (cfg.url) {
      $id('fdRefWrap').style.display = '';
      $id('fdRefNote').style.display = 'none';
      initRefS2(cfg);
    } else {
      $id('fdRefWrap').style.display = 'none';
      $id('fdRefNote').style.display = '';
      $id('fdRefNoteText').textContent = cfg.note;
    }
  };

  // ═══════════════════════════════════════════════════════════════════════════
  // CART
  // ═══════════════════════════════════════════════════════════════════════════
  window.fdAddLine = function () {
    var qty   = parseFloat($id('fdLineQty').value);
    var price = parseFloat($id('fdLinePrice').value);
    var desc  = $id('fdLineDesc').value.trim();

    if (!(qty > 0)) { Swal.fire('Check quantity', 'Quantity must be greater than zero.', 'warning'); return; }
    if (!(price >= 0)) { Swal.fire('Check price', 'Unit price is not valid.', 'warning'); return; }

    var cfg = REF[currentType];
    if (!desc) desc = refSel.text || cfg.label; // never let a line be blank
    if (cfg.url && !refSel.id && !$id('fdLineDesc').value.trim()) {
      // reference lookup available but nothing picked and no manual description
      Swal.fire('Pick a ' + cfg.label.toLowerCase(), 'Choose a ' + cfg.label.toLowerCase() + ' or type a description.', 'warning');
      return;
    }

    cart.push({
      serviceType:   currentType,
      referenceId:   cfg.url ? refSel.id : null,
      referenceText: cfg.url ? refSel.text : '',
      description:   desc,
      qty:           qty,
      unitPrice:     price,
      lineTotal:     +(qty * price).toFixed(2)
    });

    // reset the line inputs
    $id('fdLineDesc').value = '';
    $id('fdLineQty').value = '1';
    $id('fdLinePrice').value = '0';
    refSel = { id: null, text: '' };
    if (cfg.url) $('#fdRefSelect').val(null).trigger('change');

    renderCart();
  };

  window.fdRemoveLine = function (idx) {
    cart.splice(idx, 1);
    renderCart();
  };

  function renderCart() {
    var body = $id('fdCartBody');
    if (!cart.length) {
      body.innerHTML = '<tr id="fdCartEmpty"><td colspan="6" class="text-center text-muted py-4">No services added yet.</td></tr>';
    } else {
      body.innerHTML = cart.map(function (l, i) {
        return '<tr>'
          + '<td><span class="fd-type-chip fd-type-' + esc(l.serviceType) + '">' + esc(l.serviceType) + '</span></td>'
          + '<td>' + esc(l.description) + '</td>'
          + '<td class="text-end">' + (Number(l.qty) || 0) + '</td>'
          + '<td class="text-end">' + money(l.unitPrice) + '</td>'
          + '<td class="text-end">' + money(l.lineTotal) + '</td>'
          + '<td class="text-end"><a href="javascript:;" class="text-danger" title="Remove" onclick="fdRemoveLine(' + i + ')"><i class="fa-regular fa-trash-can"></i></a></td>'
          + '</tr>';
      }).join('');
    }
    var subtotal = cart.reduce(function (s, l) { return s + (Number(l.lineTotal) || 0); }, 0);
    $id('fdSumCount').textContent    = cart.length;
    $id('fdSumSubtotal').textContent = money(subtotal);
    $id('fdSumTotal').textContent    = money(subtotal);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SAVE / CONFIRM
  // ═══════════════════════════════════════════════════════════════════════════
  // Header bookingType: single pure {HOTEL|AIR|PACKAGE} -> that type,
  // anything else (TOUR/VISA present, or a mix) -> COMBINED.
  function deriveBookingType() {
    var types = {};
    cart.forEach(function (l) { types[l.serviceType] = true; });
    var keys = Object.keys(types);
    if (keys.length === 1 && ['HOTEL', 'AIR', 'PACKAGE'].indexOf(keys[0]) !== -1) return keys[0];
    return 'COMBINED';
  }

  function partyId() {
    return mode === 'registered' ? ($('#fdPartyId').val() || null) : null;
  }

  function composeRemarks() {
    var base = ($id('fdRemarks').value || '').trim();
    if (mode === 'walkin') {
      var name  = $id('fdGuestName').value.trim();
      var phone = $id('fdGuestPhone').value.trim();
      var email = $id('fdGuestEmail').value.trim();
      var bits = [];
      if (name)  bits.push(name);
      if (phone) bits.push(phone);
      if (email) bits.push(email);
      if (bits.length) base = 'Walk-in: ' + bits.join(' / ') + (base ? '\n' + base : '');
    }
    return base || null;
  }

  function travelDate() {
    return (mode === 'registered' ? $id('fdTravelDate').value : $id('fdTravelDateW').value) || null;
  }

  function buildPayload() {
    return {
      id:              null,               // front desk always creates a fresh booking
      bookingType:     deriveBookingType(),
      bookingDate:     today(),
      travelStartDate: travelDate(),
      status:          'DRAFT',
      currency:        'BDT',
      partyId:         partyId(),
      remarks:         composeRemarks(),
      services: cart.map(function (l) {
        return {
          serviceType: l.serviceType,
          referenceId: l.referenceId || null,
          description: l.description,
          quantity:    l.qty,
          unitCost:    0,
          unitPrice:   l.unitPrice,
          lineTotal:   l.lineTotal
        };
      })
    };
  }

  function toggleBusy(busy) {
    var d = { btn: $id('fdSaveDraftBtn'),  sp: $id('fdDraftSpin'),  tx: $id('fdDraftTxt') };
    var c = { btn: $id('fdSaveConfirmBtn'), sp: $id('fdConfirmSpin'), tx: $id('fdConfirmTxt') };
    [d, c].forEach(function (o) {
      o.btn.disabled = busy;
      o.sp.classList.toggle('d-none', !busy);
    });
  }

  window.fdSave = function (confirmAfter) {
    if (!cart.length) { Swal.fire('Nothing to save', 'Add at least one service line first.', 'warning'); return; }

    if (mode === 'walkin' && !$id('fdGuestName').value.trim()) {
      Swal.fire('Guest name required', 'Enter the walk-in guest name.', 'warning'); return;
    }
    if (confirmAfter && !partyId()) {
      Swal.fire({
        icon: 'info', title: 'Registered customer needed',
        text: 'Confirming posts the booking to a customer account. Attach a registered customer, or use “Save as Draft”.'
      });
      return;
    }

    toggleBusy(true);
    var payload = buildPayload();

    secureFetch(URL_SAVE, { method: 'POST', body: JSON.stringify(payload) })
      .then(function (d) {
        if (!d.success) throw new Error(d.message || 'Save failed.');
        var id = d.id, no = d.bookingNo || ('#' + id);

        if (!confirmAfter) {
          Swal.fire({ icon: 'success', title: 'Draft saved', text: 'Booking ' + no + ' saved as draft.', timer: 2200, showConfirmButton: false });
          fdNew();
          loadRecent();
          return;
        }

        // second step: confirm
        return secureFetch(URL_CONFIRM + id, { method: 'POST' }).then(function (c) {
          if (!c.success) {
            // booking exists as a draft; surface the reason but don't wipe the cart
            Swal.fire({ icon: 'error', title: 'Saved as draft, not confirmed', text: c.message || 'Confirm failed.' });
            loadRecent();
            return;
          }
          Swal.fire({ icon: 'success', title: 'Confirmed 🎉', text: c.message || ('Booking ' + no + ' confirmed.'), timer: 2400, showConfirmButton: false });
          fdNew();
          loadRecent();
        });
      })
      .catch(function (err) {
        Swal.fire({ icon: 'error', title: 'Error', text: err.message || 'Request failed.' });
      })
      .finally(function () { toggleBusy(false); });
  };

  // Reset to a fresh booking
  window.fdNew = function () {
    cart = [];
    renderCart();
    $id('fdRemarks').value = '';
    $id('fdGuestName').value = '';
    $id('fdGuestPhone').value = '';
    $id('fdGuestEmail').value = '';
    $id('fdLineDesc').value = '';
    $id('fdLineQty').value = '1';
    $id('fdLinePrice').value = '0';
    $id('fdTravelDate').value = '';
    $id('fdTravelDateW').value = '';
    refSel = { id: null, text: '' };
    if ($('#fdPartyId').hasClass('select2-hidden-accessible')) $('#fdPartyId').val(null).trigger('change');
    if ($('#fdRefSelect').hasClass('select2-hidden-accessible')) $('#fdRefSelect').val(null).trigger('change');
  };

  // ═══════════════════════════════════════════════════════════════════════════
  // RECENT ACTIVITY FEED  (DataTable endpoint -> { data: [...] })
  // ═══════════════════════════════════════════════════════════════════════════
  function loadRecent() {
    var box = $id('fdRecentList');
    var url = URL_FEED + '?draw=1&start=0&length=8&' + encodeURIComponent('search[value]') + '=';
    secureFetch(url, { method: 'GET' })
      .then(function (d) {
        var rows = (d && d.data) || [];
        if (!rows.length) { box.innerHTML = '<p class="text-muted small mb-0">No recent bookings.</p>'; return; }
        box.innerHTML = rows.map(function (r) {
          var no    = esc(r.booking_no || ('#' + (r.id || '')));
          var cust  = esc(r.customer_name || '—');
          var date  = esc(r.booking_date || '');
          var badge = r.status_badge || '';   // server-generated trusted HTML
          var amt   = money(r.total_amount);
          return '<div class="fd-recent-item d-flex justify-content-between align-items-start">'
            + '<div class="pe-2">'
            +   '<div class="fd-recent-no">' + no + ' ' + badge + '</div>'
            +   '<div class="fd-recent-meta">' + cust + (date ? ' · ' + date : '') + '</div>'
            + '</div>'
            + '<div class="fd-recent-amount text-end">' + amt + '</div>'
            + '</div>';
        }).join('');
      })
      .catch(function () { box.innerHTML = '<p class="text-danger small mb-0">Could not load recent activity.</p>'; });
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CLOCK
  // ═══════════════════════════════════════════════════════════════════════════
  function tickClock() {
    var d = new Date();
    var opts = { weekday: 'short', day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', second: '2-digit' };
    $id('fdClock').textContent = d.toLocaleString('en-GB', opts);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INIT
  // ═══════════════════════════════════════════════════════════════════════════
  document.addEventListener('DOMContentLoaded', function () {
    initCustomerS2();
    fdSetCustomerMode('registered');
    // seed the reference lookup for the default (Hotel) tab
    initRefS2(REF.HOTEL);
    renderCart();
    loadRecent();
    tickClock();
    setInterval(tickClock, 1000);
  });

})();
