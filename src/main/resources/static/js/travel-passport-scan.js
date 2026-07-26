/* =============================================================================
 * travel-passport-scan.js — shared ICAO-9303 passport scanner  (v2)
 * =============================================================================
 * Used by travel-passengers.html (map prefix pg*) and the passenger sub-modal
 * on travel-air-tickets.html (map prefix ap*). Pages call:
 *
 *     scanPassport(SCAN_MAP);                  // open the scanner
 *     trvAttachPassportImage(passengerId);     // after a successful save
 *
 * How a photo becomes form fields
 * -------------------------------
 * 1. OCR runs IN THE BROWSER (Tesseract.js). The image never leaves this
 *    machine unless the operator ticks "also file this image".
 * 2. The scanner makes SEVERAL attempts over different crops of the bottom of
 *    the page, binarised and grayscale, all restricted to the 37 characters
 *    that can legally appear in an MRZ. Each attempt is scored locally with
 *    the real ICAO check digits — a built-in oracle for OCR correctness.
 * 3. MRZ line 2 (numbers) and line 1 (names) fail in different ways, so they
 *    are selected INDEPENDENTLY: line 2 from the attempt with the most
 *    passing check digits, line 1 from the attempt that preserved the most
 *    '<' fillers (binarisation eats the thin '<' strokes; grayscale keeps
 *    them). The composed pair goes to the server once, where MrzParser
 *    re-validates and repairs.
 * 4. After the verified MRZ fields are on screen, a slower unconstrained pass
 *    reads the PRINTED zones and fills what the MRZ cannot: issue date,
 *    issuing authority, place of birth, father's/mother's name, permanent
 *    address, emergency contact. Those rows are badged "printed" — they carry
 *    no check digits.
 *
 * Self-hosting: window.TRV_TESSERACT_SRC overrides the library URL, and
 * window.TRV_TESSERACT_PATHS = { workerPath, corePath, langPath, gzip }
 * overrides the worker / wasm / language-data locations for CSP-locked or
 * offline installations.
 * ========================================================================== */
(function (window, document) {
  'use strict';

  var TESSERACT_SRC = window.TRV_TESSERACT_SRC
      || 'https://cdn.jsdelivr.net/npm/tesseract.js@5/dist/tesseract.min.js';

  var MRZ_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<';

  // ── state ─────────────────────────────────────────────────────────────────
  var _map = null;            // scanKey -> form element id (per page)
  var _opts = null;
  var _img = null;            // loaded Image
  var _imgFile = null;        // original File (for optional attach)
  var _pendingFile = null;    // set on Apply when "also file image" is ticked
  var _lastScan = null;       // last parse response merged with viz fields
  var _bandTop = 0.72, _bandBottom = 1.0;
  var _workerPromise = null;
  var _busy = false;

  // ── tiny utils ────────────────────────────────────────────────────────────
  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
  function el(id) { return document.getElementById(id); }
  function csrfHeaders() {
    var h = {};
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) h[header.getAttribute('content')] = token.getAttribute('content');
    return h;
  }
  function toastOk(msg)  { if (window.toast) window.toast('success', msg); }
  function toastErr(msg) { if (window.toast) window.toast('error', msg); else alert(msg); }

  // ── Tesseract loading & worker lifecycle ──────────────────────────────────
  var _libPromise = null;
  function loadTesseract() {
    if (window.Tesseract) return Promise.resolve(window.Tesseract);
    if (_libPromise) return _libPromise;
    _libPromise = new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = TESSERACT_SRC;
      s.onload = function () { resolve(window.Tesseract); };
      s.onerror = function () {
        _libPromise = null;
        reject(new Error('Could not load the OCR library. If this terminal is offline or the CSP '
          + 'blocks cdn.jsdelivr.net, self-host it and set window.TRV_TESSERACT_SRC — '
          + 'or just type the two MRZ lines below.'));
      };
      document.head.appendChild(s);
    });
    return _libPromise;
  }

  /**
   * One shared worker per modal session. The logger MUST be given to
   * createWorker — passing a function to recognize() throws a DataCloneError
   * in Tesseract.js v5+ because options are structured-cloned to the worker.
   */
  function getWorker() {
    if (_workerPromise) return _workerPromise;
    _workerPromise = loadTesseract().then(function (T) {
      var opts = Object.assign({
        logger: function (m) {
          if (m && m.status === 'recognizing text') setBar(Math.round((m.progress || 0) * 100));
        }
      }, window.TRV_TESSERACT_PATHS || {});
      return T.createWorker('eng', 1, opts);
    });
    return _workerPromise;
  }
  function killWorker() {
    if (!_workerPromise) return;
    var p = _workerPromise;
    _workerPromise = null;
    p.then(function (w) { try { w.terminate(); } catch (e) { /* already gone */ } })
     .catch(function () { /* never loaded */ });
  }
  function setMode(worker, mode) {
    return worker.setParameters(mode === 'mrz' ? {
      tessedit_char_whitelist: MRZ_CHARS,   // the single biggest accuracy lever
      tessedit_pageseg_mode: '6',           // one uniform block of text
      preserve_interword_spaces: '1'
    } : {
      tessedit_char_whitelist: '',          // printed zone needs lowercase etc.
      tessedit_pageseg_mode: '3',
      preserve_interword_spaces: '0'
    });
  }

  // ── image pipeline (crop → gray → upscale → optional Otsu) ────────────────
  function otsuThreshold(gray) {
    var hist = new Uint32Array(256), i, k;
    for (i = 0; i < gray.length; i++) hist[gray[i]]++;
    var total = gray.length, sum = 0;
    for (k = 0; k < 256; k++) sum += k * hist[k];
    var sumB = 0, wB = 0, best = 0, thr = 128;
    for (k = 0; k < 256; k++) {
      wB += hist[k]; if (!wB) continue;
      var wF = total - wB; if (!wF) break;
      sumB += k * hist[k];
      var mB = sumB / wB, mF = (sum - sumB) / wF, v = wB * wF * (mB - mF) * (mB - mF);
      if (v > best) { best = v; thr = k; }
    }
    return thr;
  }

  /** Draws a horizontal band of the image into a canvas, grayscale or binarised. */
  function renderBand(canvas, top, bottom, mode, targetW) {
    var sy = Math.floor(_img.naturalHeight * top);
    var sh = Math.max(8, Math.floor(_img.naturalHeight * (bottom - top)));
    var scale = Math.min(targetW / _img.naturalWidth, 4);   // never upscale past 4x
    var dw = Math.round(_img.naturalWidth * scale);
    var dh = Math.round(sh * scale);
    canvas.width = dw; canvas.height = dh;
    var ctx = canvas.getContext('2d', { willReadFrequently: true });
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = 'high';
    ctx.drawImage(_img, 0, sy, _img.naturalWidth, sh, 0, 0, dw, dh);

    var imgData = ctx.getImageData(0, 0, dw, dh);
    var d = imgData.data;
    var gray = new Uint8ClampedArray(dw * dh);
    for (var i = 0; i < gray.length; i++) {
      gray[i] = d[i * 4] * 0.299 + d[i * 4 + 1] * 0.587 + d[i * 4 + 2] * 0.114;
    }
    var thr = mode === 'bin' ? otsuThreshold(gray) : -1;
    for (i = 0; i < gray.length; i++) {
      var v = thr < 0 ? gray[i] : (gray[i] > thr ? 255 : 0);
      d[i * 4] = v; d[i * 4 + 1] = v; d[i * 4 + 2] = v; d[i * 4 + 3] = 255;
    }
    ctx.putImageData(imgData, 0, 0);
    return canvas;
  }

  // ── local ICAO scorer — the browser-side twin of MrzParser's core ─────────
  var TO_DIGIT = { O: '0', Q: '0', D: '0', U: '0', I: '1', L: '1', Z: '2',
                   A: '4', S: '5', G: '6', C: '6', T: '7', B: '8' };
  var CD_W = [7, 3, 1];
  function charVal(c) {
    if (c >= '0' && c <= '9') return c.charCodeAt(0) - 48;
    if (c >= 'A' && c <= 'Z') return c.charCodeAt(0) - 55;
    return 0;
  }
  function checkDigit(s) {
    var sum = 0;
    for (var i = 0; i < s.length; i++) sum += charVal(s[i]) * CD_W[i % 3];
    return sum % 10;
  }
  function foldNum(s) {
    var out = '';
    for (var i = 0; i < s.length; i++) out += TO_DIGIT[s[i]] || s[i];
    return out;
  }
  function normLines(text) {
    return String(text || '').toUpperCase().split(/\r?\n/)
      .map(function (l) {
        return l.replace(/[\u00AB\u00BB\u2039\u203A\u226A\u226B\u00A2{(]/g, '<')
                .replace(/[^A-Z0-9<]/g, '');
      })
      .filter(function (s) { return s.length >= 25; });
  }
  function pad44(s) { return s.length >= 44 ? s.slice(0, 44) : s + new Array(45 - s.length).join('<'); }

  /**
   * Scores one OCR attempt. digits/max are the passing ICAO check digits of
   * the best adjacent line pair; line 1 is judged separately by how many '<'
   * fillers survived (they are what OCR destroys first, and they are exactly
   * what the name split depends on).
   */
  function scoreAttempt(text) {
    var L = normLines(text);
    var best = { digits: -1, max: 5, natAlpha: false, line1: null, line1Angles: -1, line2: null, lineCount: L.length };
    for (var i = 0; i + 1 < L.length; i++) {
      var l2 = pad44(L[i + 1]);
      var dn = l2.slice(0, 9), dcd = l2[9];
      var dob = foldNum(l2.slice(13, 19)), bcd = l2[19];
      var exp = foldNum(l2.slice(21, 27)), ecd = l2[27];
      var per = l2.slice(28, 42), pcd = l2[42], ccd = l2[43];
      var digits = 0, max = 0;
      var chk = function (data, e) { max++; if (/\d/.test(e) && checkDigit(data) === +e) digits++; };
      chk(dn, dcd); chk(dob, bcd); chk(exp, ecd);
      if (!/^<+$/.test(per)) chk(per, pcd);
      chk(dn + dcd + dob + bcd + exp + ecd + per + pcd, ccd);
      var natAlpha = /^[A-Z]{3}$/.test(l2.slice(10, 13));
      if (digits > best.digits || (digits === best.digits && natAlpha && !best.natAlpha)) {
        var raw1 = L[i];
        best = {
          digits: digits, max: max, natAlpha: natAlpha,
          line2: l2, lineCount: L.length,
          line1: (raw1.length >= 40 && raw1.length <= 46) ? pad44(raw1) : null,
          line1Angles: -1
        };
        if (best.line1) best.line1Angles = (best.line1.slice(5).match(/</g) || []).length;
      }
    }
    return best;
  }

  // ── the attempts engine ───────────────────────────────────────────────────
  function buildAttempts() {
    var list = [
      { top: _bandTop, bottom: _bandBottom, mode: 'bin' },   // operator's band first
      { top: 0.80, bottom: 1.0, mode: 'bin' },
      { top: 0.86, bottom: 1.0, mode: 'bin' },
      { top: 0.80, bottom: 1.0, mode: 'gray' },              // gray keeps the '<'s
      { top: 0.72, bottom: 1.0, mode: 'gray' },
      { top: 0.72, bottom: 1.0, mode: 'bin' },
      { top: 0.55, bottom: 1.0, mode: 'bin' }                // generous last resort
    ];
    var seen = {}, out = [];
    list.forEach(function (a) {
      var key = a.top.toFixed(2) + '-' + a.bottom.toFixed(2) + '-' + a.mode;
      if (!seen[key]) { seen[key] = 1; out.push(a); }
    });
    return out;
  }

  function runOcr() {
    if (!_img) { toastErr('Load a passport photo first.'); return; }
    if (_busy) return;
    _busy = true;
    setButtons(false);
    showProgress(true);
    status('Warming up the reader…');

    var attempts = buildAttempts();
    var canvas = el('trvPpCanvas');
    var best2 = { digits: -1, max: 5, natAlpha: false, line2: null };
    var best1 = { angles: -1, line: null };
    var bestAnyText = '', bestAnyLines = -1;
    var grayTried = false;

    getWorker().then(function (worker) {
      return setMode(worker, 'mrz').then(function () { return worker; });
    }).then(function (worker) {
      var chain = Promise.resolve();
      attempts.forEach(function (att, idx) {
        chain = chain.then(function () {
          // A better line 2 AND a good line 1 already in hand? Stop early —
          // but only after at least one grayscale pass has had its shot at
          // the fillers.
          if (best2.digits === best2.max && grayTried && best1.angles >= 8) return;
          status('Pass ' + (idx + 1) + ' of ' + attempts.length
                 + (best2.digits > 0 ? ' — strip check ' + best2.digits + '/' + best2.max : ''));
          setBar(0);
          renderBand(canvas, att.top, att.bottom, att.mode, 1800);
          return worker.recognize(canvas).then(function (res) {
            if (att.mode === 'gray') grayTried = true;
            var text = (res && res.data && res.data.text) || '';
            var s = scoreAttempt(text);
            if (s.lineCount > bestAnyLines) { bestAnyLines = s.lineCount; bestAnyText = text; }
            if (s.line2 && (s.digits > best2.digits
                || (s.digits === best2.digits && s.natAlpha && !best2.natAlpha))) {
              best2 = s;
            }
            if (s.line1 && s.line1Angles > best1.angles) {
              best1 = { angles: s.line1Angles, line: s.line1 };
            }
          });
        });
      });
      return chain;
    }).then(function () {
      var mrzText;
      if (best2.line2) {
        mrzText = (best1.line ? best1.line : '') + (best1.line ? '\n' : '') + best2.line2;
      } else {
        mrzText = bestAnyText;   // let the server explain what is wrong
      }
      status('Validating check digits…');
      return serverParse(mrzText, 'OCR_CLIENT');
    }).then(function (scan) {
      _busy = false;
      setButtons(true);
      showProgress(false);
      status('');
      if (scan) {
        showResult(scan);
        runVizPass(scan);      // best-effort, appends rows when done
      }
    }).catch(function (err) {
      _busy = false;
      setButtons(true);
      showProgress(false);
      status('');
      toastErr(err && err.message ? err.message : 'The scan failed — you can type the MRZ manually.');
    });
  }

  /**
   * Printed-zone pass. Runs AFTER the verified MRZ result is on screen so the
   * slow full-page OCR never delays the fields that matter most. Two reads:
   * the whole page (personal-data labels) and the bottom half (data-page
   * print), concatenated for the server-side extractor.
   */
  function runVizPass(scan) {
    if (!_img || (_opts && _opts.skipViz)) return;
    addVizPendingRow();
    var work = document.createElement('canvas');
    getWorker().then(function (worker) {
      return setMode(worker, 'viz')
        .then(function () { return worker.recognize(renderBand(work, 0, 1, 'gray', 1400)); })
        .then(function (r1) {
          var t1 = (r1 && r1.data && r1.data.text) || '';
          return worker.recognize(renderBand(work, 0.55, 1, 'bin', 1800)).then(function (r2) {
            return t1 + '\n' + ((r2 && r2.data && r2.data.text) || '');
          });
        });
    }).then(function (vizText) {
      return fetch('/travel/passengers/passport/viz', {
        method: 'POST',
        headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders()),
        credentials: 'same-origin',
        body: JSON.stringify({
          vizText: vizText,
          dateOfBirth: scan.dateOfBirth || '',
          passportExpiry: scan.passportExpiry || ''
        })
      }).then(function (r) { return r.json(); });
    }).then(function (d) {
      removeVizPendingRow();
      if (d && d.success && d.fields) appendVizRows(d.fields);
    }).catch(function () {
      removeVizPendingRow();   // silent: viz is a bonus, never an error state
    });
  }

  function serverParse(mrzText, source) {
    return fetch('/travel/passengers/passport/parse', {
      method: 'POST',
      headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders()),
      credentials: 'same-origin',
      body: JSON.stringify({ mrzText: mrzText || '', source: source })
    }).then(function (r) { return r.json(); })
      .then(function (d) {
        var scan = (d && d.obj && d.obj.defaultData) || (d && d.data);
        if (!scan) { toastErr((d && d.message) || 'Unexpected server reply.'); return null; }
        if (!scan.success) { toastErr(scan.message || 'Could not read the MRZ.'); return null; }
        return scan;
      });
  }

  // ── result table ──────────────────────────────────────────────────────────
  var ROWS = [
    ['suggestedTitle',         'Title'],
    ['givenNames',             'First / Given Names'],
    ['surname',                'Last Name / Surname'],
    ['dateOfBirth',            'Date of Birth'],
    ['gender',                 'Gender'],
    ['suggestedPassengerType', 'Passenger Type'],
    ['passportNumber',         'Passport No'],
    ['passportExpiry',         'Passport Expiry'],
    ['nationalityName',        'Nationality'],
    ['issuingCountry',         'Issuing Country'],
    ['personalNumber',         'Personal / NID No']
  ];
  var VIZ_ROWS = [
    ['passportIssueDate',        'Date of Issue'],
    ['passportIssuingAuthority', 'Issuing Authority'],
    ['placeOfBirth',             'Place of Birth'],
    ['fatherName',               "Father's Name"],
    ['motherName',               "Mother's Name"],
    ['permanentAddress',         'Permanent Address'],
    ['emergencyContactName',     'Emergency Contact'],
    ['emergencyContactRelation', 'Emergency Relationship'],
    ['emergencyContactPhone',    'Emergency Phone']
  ];
  var HIDDEN_KEYS = ['mrzLine1', 'mrzLine2', 'mrzLine3'];

  /**
   * The page maps may use friendlier names than the scan DTO (firstName for
   * givenNames, title for suggestedTitle…). Resolve through this alias table
   * so both spellings work and a page never silently loses fields.
   */
  var KEY_ALIAS = {
    suggestedTitle:         'title',
    givenNames:             'firstName',
    surname:                'lastName',
    suggestedPassengerType: 'passengerType',
    nationalityName:        'nationality',
    issuingCountry:         'passportCountry'
  };
  function targetFor(key) {
    if (!_map) return null;
    return _map[key] || (KEY_ALIAS[key] ? _map[KEY_ALIAS[key]] : null) || null;
  }

  function rowHtml(key, label, value, badge) {
    return '<tr>'
      + '<td class="text-center align-middle" style="width:36px">'
      +   '<input type="checkbox" class="form-check-input" data-target="' + esc(key) + '"'
      +   ' data-value="' + esc(String(value)) + '" checked></td>'
      + '<td class="align-middle text-nowrap">' + esc(label)
      +   (badge ? ' <span class="badge bg-secondary bg-opacity-25 text-dark">printed</span>' : '')
      + '</td>'
      + '<td class="align-middle">' + esc(String(value)) + '</td>'
      + '</tr>';
  }

  function showResult(scan) {
    _lastScan = scan;
    var tbody = el('trvPpRows');
    var html = '';
    ROWS.forEach(function (r) {
      var val = scan[r[0]];
      if (val === null || val === undefined || val === '') return;
      if (!targetFor(r[0]) || !el(targetFor(r[0]))) return;   // page has no such field
      html += rowHtml(r[0], r[1], val, false);
    });
    tbody.innerHTML = html;
    el('trvPpResult').classList.remove('d-none');
    el('trvPpApplyBtn').disabled = html === '';

    var badge = el('trvPpBadge');
    if (scan.checkDigitsValid) {
      badge.className = 'badge bg-success';
      badge.textContent = 'All check digits verified';
    } else {
      badge.className = 'badge bg-warning text-dark';
      badge.textContent = 'Check digits failed — review highlighted fields';
    }
    var warn = el('trvPpWarnings');
    warn.innerHTML = (scan.warnings || []).map(function (w) {
      return '<div class="text-warning small"><i class="fa fa-triangle-exclamation me-1"></i>' + esc(w) + '</div>';
    }).join('');
  }

  function addVizPendingRow() {
    var tbody = el('trvPpRows');
    if (!tbody || el('trvPpVizPending')) return;
    var tr = document.createElement('tr');
    tr.id = 'trvPpVizPending';
    tr.innerHTML = '<td></td><td colspan="2" class="text-muted small">'
      + '<i class="fa fa-circle-notch fa-spin me-1"></i>Reading the printed page…</td>';
    tbody.appendChild(tr);
  }
  function removeVizPendingRow() {
    var tr = el('trvPpVizPending');
    if (tr) tr.remove();
  }

  function appendVizRows(fields) {
    var tbody = el('trvPpRows');
    if (!tbody) return;
    var added = 0;
    VIZ_ROWS.forEach(function (r) {
      var key = r[0], val = fields[key];
      if (val === null || val === undefined || val === '') return;
      if (!targetFor(key) || !el(targetFor(key))) return;
      if (tbody.querySelector('input[data-target="' + key + '"]')) return;   // never duplicate
      tbody.insertAdjacentHTML('beforeend', rowHtml(key, r[1], val, true));
      _lastScan[key] = val;
      added++;
    });
    if (added) el('trvPpApplyBtn').disabled = false;
  }

  // ── apply to form ─────────────────────────────────────────────────────────
  function setField(id, value) {
    if (!id) return false;
    var node = el(id);
    if (!node || value === undefined || value === null || value === '') return false;
    if (node.tagName === 'SELECT') {
      var matched = false;
      for (var i = 0; i < node.options.length; i++) {
        if (String(node.options[i].value).toUpperCase() === String(value).toUpperCase()) {
          node.selectedIndex = i; matched = true; break;
        }
      }
      if (!matched) return false;
      if (window.jQuery && window.jQuery(node).data('select2')) window.jQuery(node).trigger('change');
    } else if (node.type === 'date') {
      node.value = String(value).substring(0, 10);
    } else {
      node.value = value;
    }
    node.dispatchEvent(new Event('change', { bubbles: true }));
    flash(node);
    return true;
  }
  function flash(node) {
    node.classList.add('bg-warning', 'bg-opacity-25');
    setTimeout(function () { node.classList.remove('bg-warning', 'bg-opacity-25'); }, 1200);
  }

  function applyToForm() {
    if (!_lastScan) return;
    var applied = 0;
    var boxes = el('trvPpRows').querySelectorAll('input[type="checkbox"]:checked');
    boxes.forEach(function (cb) {
      if (setField(targetFor(cb.dataset.target), cb.dataset.value)) applied++;
    });
    HIDDEN_KEYS.forEach(function (k) {
      if (_lastScan[k] && targetFor(k)) setField(targetFor(k), _lastScan[k]);
    });
    _pendingFile = (el('trvPpAttach') && el('trvPpAttach').checked) ? _imgFile : null;
    toastOk(applied + ' field' + (applied === 1 ? '' : 's') + ' filled from the passport.');
    var modal = window.bootstrap && el('trvPpModal')
      ? window.bootstrap.Modal.getOrCreateInstance(el('trvPpModal')) : null;
    if (modal) modal.hide();
    if (_opts && typeof _opts.onApply === 'function') _opts.onApply(_lastScan);
  }

  // ── attach-after-save (called by the pages) ──────────────────────────────
  function trvAttachPassportImage(passengerId) {
    if (!_pendingFile || !passengerId) return Promise.resolve(null);
    var fd = new FormData();
    fd.append('file', _pendingFile);
    // Deliberately NOT secureFetch: it forces a JSON content type, which
    // strips the multipart boundary. CSRF headers are attached directly.
    return fetch('/travel/passengers/' + passengerId + '/passport-image', {
      method: 'POST',
      headers: csrfHeaders(),
      credentials: 'same-origin',
      body: fd
    }).then(function (r) { return r.json(); })
      .then(function (d) {
        _pendingFile = null;
        if (d && d.success) toastOk('Passport image filed against the passenger.');
        return d;
      })
      .catch(function () { _pendingFile = null; return null; });
  }

  // ── modal ─────────────────────────────────────────────────────────────────
  function ensureModal() {
    if (el('trvPpModal')) return;
    var div = document.createElement('div');
    div.innerHTML =
      '<div class="modal fade" id="trvPpModal" tabindex="-1">' +
      ' <div class="modal-dialog modal-lg modal-dialog-scrollable">' +
      '  <div class="modal-content">' +
      '   <div class="modal-header">' +
      '    <h5 class="modal-title"><i class="fa fa-passport me-2"></i>Scan Passport</h5>' +
      '    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
      '   </div>' +
      '   <div class="modal-body">' +
      '    <div id="trvPpDrop" class="border border-2 border-dashed rounded p-4 text-center mb-3" style="cursor:pointer">' +
      '      <i class="fa fa-cloud-arrow-up fa-2x mb-2 text-muted"></i>' +
      '      <div>Drop the passport photo here, click to browse, or paste it (Ctrl+V).</div>' +
      '      <div class="small text-muted mt-1">The photo is read in this browser — it is not uploaded unless you tick the box below.</div>' +
      '      <input type="file" id="trvPpFile" accept="image/*" class="d-none">' +
      '    </div>' +
      '    <div id="trvPpStage" class="d-none">' +
      '      <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">' +
      '        <button type="button" class="btn btn-theme btn-sm" id="trvPpReadBtn"><i class="fa fa-wand-magic-sparkles me-1"></i>Read Passport</button>' +
      '        <button type="button" class="btn btn-outline-theme btn-sm d-none" id="trvPpServerBtn"><i class="fa fa-server me-1"></i>Scan on Server</button>' +
      '        <div class="btn-group btn-group-sm ms-auto">' +
      '          <button type="button" class="btn btn-outline-secondary" id="trvPpBandUp" title="Strip covers more of the page">▲ wider</button>' +
      '          <button type="button" class="btn btn-outline-secondary" id="trvPpBandDown" title="Strip hugs the bottom lines">▼ tighter</button>' +
      '        </div>' +
      '      </div>' +
      '      <div id="trvPpStatus" class="small text-muted mb-1"></div>' +
      '      <div class="progress mb-2 d-none" id="trvPpProg" style="height:6px"><div class="progress-bar" id="trvPpBar" style="width:0%"></div></div>' +
      '      <canvas id="trvPpCanvas" class="w-100 border rounded bg-dark"></canvas>' +
      '      <div class="form-text">The reader tries several strips and picks the one whose ICAO check digits pass — nudge the band only if every pass fails.</div>' +
      '    </div>' +
      '    <hr>' +
      '    <details class="mb-3"><summary class="text-muted small">Type the two MRZ lines manually instead</summary>' +
      '      <textarea id="trvPpManualTxt" class="form-control font-monospace mt-2" rows="3" spellcheck="false" placeholder="P&lt;BGDSURNAME&lt;&lt;GIVEN&lt;NAMES&lt;&lt;…&#10;A123456785BGD…"></textarea>' +
      '      <button type="button" class="btn btn-outline-theme btn-sm mt-2" id="trvPpParseBtn">Parse MRZ Text</button>' +
      '    </details>' +
      '    <div id="trvPpResult" class="d-none">' +
      '      <div class="d-flex align-items-center mb-2">' +
      '        <span id="trvPpBadge" class="badge bg-success">All check digits verified</span>' +
      '      </div>' +
      '      <div id="trvPpWarnings" class="mb-2"></div>' +
      '      <div class="table-responsive">' +
      '        <table class="table table-sm align-middle mb-2">' +
      '          <thead><tr><th style="width:36px"></th><th>Field</th><th>Value from passport</th></tr></thead>' +
      '          <tbody id="trvPpRows"></tbody>' +
      '        </table>' +
      '      </div>' +
      '      <div class="form-check mb-2">' +
      '        <input class="form-check-input" type="checkbox" id="trvPpAttach" checked>' +
      '        <label class="form-check-label" for="trvPpAttach">Also file this image against the passenger after saving</label>' +
      '      </div>' +
      '    </div>' +
      '   </div>' +
      '   <div class="modal-footer">' +
      '    <button type="button" class="btn btn-default" data-bs-dismiss="modal">Close</button>' +
      '    <button type="button" class="btn btn-theme" id="trvPpApplyBtn" disabled><i class="fa fa-file-import me-1"></i>Fill the Form</button>' +
      '   </div>' +
      '  </div>' +
      ' </div>' +
      '</div>';
    document.body.appendChild(div.firstChild);
    wireModal();
  }

  function wireModal() {
    var drop = el('trvPpDrop'), file = el('trvPpFile');

    drop.addEventListener('click', function () { file.click(); });
    file.addEventListener('change', function () { if (file.files[0]) loadFile(file.files[0]); });
    drop.addEventListener('dragover', function (e) { e.preventDefault(); drop.classList.add('border-theme'); });
    drop.addEventListener('dragleave', function () { drop.classList.remove('border-theme'); });
    drop.addEventListener('drop', function (e) {
      e.preventDefault();
      drop.classList.remove('border-theme');
      if (e.dataTransfer.files[0]) loadFile(e.dataTransfer.files[0]);
    });
    document.addEventListener('paste', function (e) {
      var modal = el('trvPpModal');
      if (!modal || !modal.classList.contains('show')) return;
      var items = (e.clipboardData || {}).items || [];
      for (var i = 0; i < items.length; i++) {
        if (items[i].type && items[i].type.indexOf('image') === 0) {
          loadFile(items[i].getAsFile());
          break;
        }
      }
    });

    el('trvPpReadBtn').addEventListener('click', runOcr);
    el('trvPpParseBtn').addEventListener('click', function () {
      var text = el('trvPpManualTxt').value;
      if (!text.trim()) { toastErr('Type or paste the two MRZ lines first.'); return; }
      serverParse(text, 'MRZ_MANUAL').then(function (scan) {
        if (scan) { showResult(scan); if (_img) runVizPass(scan); }
      });
    });
    el('trvPpApplyBtn').addEventListener('click', applyToForm);

    el('trvPpBandUp').addEventListener('click', function () {
      _bandTop = Math.max(0, _bandTop - 0.06);
      preview();
    });
    el('trvPpBandDown').addEventListener('click', function () {
      _bandTop = Math.min(0.94, _bandTop + 0.06);
      preview();
    });

    el('trvPpServerBtn').addEventListener('click', function () {
      if (!_imgFile) return;
      var fd = new FormData();
      fd.append('file', _imgFile);
      setButtons(false);
      fetch('/travel/passengers/passport/scan', {
        method: 'POST', headers: csrfHeaders(), credentials: 'same-origin', body: fd
      }).then(function (r) { return r.json(); })
        .then(function (d) {
          setButtons(true);
          var scan = (d && d.obj && d.obj.defaultData) || (d && d.data);
          if (scan && scan.success) showResult(scan);   // server already merged the printed zone
          else toastErr((scan && scan.message) || (d && d.message) || 'Server scan failed.');
        })
        .catch(function () { setButtons(true); toastErr('Server scan failed.'); });
    });

    el('trvPpModal').addEventListener('hidden.bs.modal', function () {
      killWorker();          // frees ~100 MB of wasm memory
      _busy = false;
    });

    // Show the server button only when an OCR engine bean is registered.
    fetch('/travel/passengers/passport/capabilities', { credentials: 'same-origin' })
      .then(function (r) { return r.json(); })
      .then(function (d) {
        if (d && d.serverOcr) el('trvPpServerBtn').classList.remove('d-none');
      })
      .catch(function () { /* capability probe is optional */ });
  }

  function loadFile(f) {
    if (!f) return;
    _imgFile = f;
    var url = URL.createObjectURL(f);
    var img = new Image();
    img.onload = function () {
      _img = img;
      el('trvPpStage').classList.remove('d-none');
      preview();
      URL.revokeObjectURL(url);
    };
    img.onerror = function () { toastErr('That file could not be read as an image.'); };
    img.src = url;
  }

  function preview() {
    if (!_img) return;
    renderBand(el('trvPpCanvas'), _bandTop, _bandBottom, 'bin', 1400);
  }

  function status(text) {
    var s = el('trvPpStatus');
    if (s) s.textContent = text || '';
  }
  function setBar(pct) {
    var b = el('trvPpBar');
    if (b) b.style.width = Math.max(0, Math.min(100, pct)) + '%';
  }
  function showProgress(on) {
    var p = el('trvPpProg');
    if (p) p.classList.toggle('d-none', !on);
    if (!on) setBar(0);
  }
  function setButtons(enabled) {
    ['trvPpReadBtn', 'trvPpServerBtn', 'trvPpParseBtn'].forEach(function (id) {
      var b = el(id);
      if (b) b.disabled = !enabled;
    });
  }

  // ── public API ────────────────────────────────────────────────────────────
  function scanPassport(map, opts) {
    _map = map || {};
    _opts = opts || {};
    ensureModal();
    _lastScan = null;
    _pendingFile = null;
    el('trvPpResult').classList.add('d-none');
    el('trvPpRows').innerHTML = '';
    el('trvPpApplyBtn').disabled = true;
    el('trvPpManualTxt').value = '';
    status('');
    window.bootstrap.Modal.getOrCreateInstance(el('trvPpModal')).show();
  }

  window.scanPassport = scanPassport;
  window.trvAttachPassportImage = trvAttachPassportImage;

})(window, document);
