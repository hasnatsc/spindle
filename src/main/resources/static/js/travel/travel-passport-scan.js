/* ═══════════════════════════════════════════════════════════════════════════
 * travel-passport-scan.js
 *
 * Provides the global helpers the Travel module uses to read a passport and
 * auto-fill a passenger form:
 *
 *   scanPassport(fieldMap, options)   ← travel-air-tickets.html already calls
 *                                       this; nothing defined it until now.
 *   trvAttachPassportImage(passengerId)
 *   trvPassportLastScan()
 *
 * HOW IT WORKS
 * ------------
 * OCR runs in the browser (Tesseract.js), not on the server. Two reasons:
 * server-side Tesseract needs native libraries on every deployment box, and
 * cloud OCR would push passport images — the most sensitive PII the travel
 * desk handles — outside ASG's network. Only the recognised MRZ *text* is
 * posted to /travel/passengers/passport/parse, where MrzParser validates the
 * ICAO check digits and returns clean typed fields. The image itself never
 * leaves this machine unless the operator explicitly attaches it.
 *
 * If a PassportOcrEngine bean is ever registered server-side, the "Scan on
 * server" button appears automatically — no change needed here.
 *
 * There is always a manual escape hatch: the two MRZ lines can be typed in.
 * Typing 88 characters takes about twenty seconds and is 100% accurate, which
 * beats fighting a bad photo.
 *
 * The modal is built by this script on first use, so the only thing a page
 * has to do is load the file:
 *     <script th:src="@{/js/travel-passport-scan.js}"></script>
 * ═══════════════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  /* Override before this script loads to serve Tesseract from your own host
     instead of the CDN (useful on an air-gapped intranet). */
  var TESSERACT_SRC = window.TRV_TESSERACT_SRC
    || 'https://cdn.jsdelivr.net/npm/tesseract.js@5.1.1/dist/tesseract.min.js';

  var PARSE_URL  = '/travel/passengers/passport/parse';
  var SCAN_URL   = '/travel/passengers/passport/scan';
  var CAPS_URL   = '/travel/passengers/passport/capabilities';
  var MRZ_CHARS  = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<';

  var _fieldMap    = {};
  var _opts        = {};
  var _lastScan    = null;
  var _lastFile    = null;
  var _serverOcr   = false;
  var _capsChecked = false;
  var _worker      = null;

  /* ── CSRF ──────────────────────────────────────────────────────────────── */
  function csrfHeaders() {
    var h = {};
    var tokenMeta  = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
      h[headerMeta.getAttribute('content')] = tokenMeta.getAttribute('content');
      return h;
    }
    var hidden = document.querySelector('input[name="_csrf"]');
    if (hidden) { h['X-CSRF-TOKEN'] = hidden.value; return h; }
    var m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    if (m) h['X-XSRF-TOKEN'] = decodeURIComponent(m[1]);
    return h;
  }

  function postJson(url, payload) {
    if (typeof window.secureFetch === 'function') {
      return window.secureFetch(url, { method: 'POST', body: JSON.stringify(payload) });
    }
    var headers = csrfHeaders();
    headers['Content-Type'] = 'application/json';
    return fetch(url, {
      method: 'POST', headers: headers, credentials: 'same-origin',
      body: JSON.stringify(payload)
    }).then(function (r) { return r.json(); });
  }

  /* Multipart deliberately does NOT go through secureFetch — that helper sets
     a JSON content type, which would strip the multipart boundary. */
  function postFile(url, file, fieldName) {
    var fd = new FormData();
    fd.append(fieldName || 'file', file);
    return fetch(url, {
      method: 'POST', headers: csrfHeaders(), credentials: 'same-origin', body: fd
    }).then(function (r) { return r.json(); });
  }

  /* ── Modal ─────────────────────────────────────────────────────────────── */
  function ensureModal() {
    if (document.getElementById('trvPassportModal')) return;

    var html =
      '<div tabindex="-1" id="trvPassportModal" class="modal fade">' +
        '<div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">' +
          '<div class="modal-content">' +
            '<div class="modal-header">' +
              '<h5 class="modal-title"><i class="fa fa-passport me-1 text-info"></i> Read Passport</h5>' +
              '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
            '</div>' +
            '<div class="modal-body">' +

              '<div class="alert alert-info py-2 px-3 small mb-3">' +
                'Photograph or scan the <strong>data page</strong> — the one with the photo. ' +
                'The two lines of <code>&lt;&lt;&lt;</code> characters along the bottom edge must be ' +
                'fully inside the frame, flat and in focus. That strip is the machine-readable zone; ' +
                'everything else on the page is ignored.' +
              '</div>' +

              '<div id="trvPpDrop" class="border border-2 border-dashed rounded p-4 text-center mb-3" ' +
                   'style="cursor:pointer;background:rgba(0,0,0,.02)">' +
                '<i class="fa fa-cloud-arrow-up fa-2x text-muted mb-2"></i>' +
                '<div class="fw-bold">Drop a passport image here, or click to choose</div>' +
                '<div class="small text-muted">JPG, PNG or WEBP — up to 15 MB</div>' +
                '<input type="file" id="trvPpFile" accept="image/*" class="d-none">' +
              '</div>' +

              '<div id="trvPpPreviewWrap" class="mb-3 d-none">' +
                '<div class="d-flex justify-content-between align-items-center mb-1">' +
                  '<strong class="small">Detected MRZ strip</strong>' +
                  '<div class="btn-group btn-group-sm">' +
                    '<button type="button" class="btn btn-outline-secondary" id="trvPpBandDown" title="Move the strip down">' +
                      '<i class="fa fa-chevron-down"></i></button>' +
                    '<button type="button" class="btn btn-outline-secondary" id="trvPpBandUp" title="Move the strip up">' +
                      '<i class="fa fa-chevron-up"></i></button>' +
                    '<button type="button" class="btn btn-outline-secondary" id="trvPpBandAll" title="Use the whole page">' +
                      '<i class="fa fa-expand"></i></button>' +
                  '</div>' +
                '</div>' +
                '<canvas id="trvPpCanvas" class="border rounded w-100" style="max-height:170px;object-fit:contain"></canvas>' +
              '</div>' +

              '<div class="d-flex gap-2 flex-wrap mb-3">' +
                '<button type="button" class="btn btn-info btn-sm" id="trvPpReadBtn" disabled>' +
                  '<span class="spinner-border spinner-border-sm me-2 d-none" id="trvPpSpin"></span>' +
                  '<i class="fa fa-wand-magic-sparkles me-1"></i> <span id="trvPpReadTxt">Read Passport</span>' +
                '</button>' +
                '<button type="button" class="btn btn-outline-secondary btn-sm d-none" id="trvPpServerBtn">' +
                  '<i class="fa fa-server me-1"></i> Scan on server' +
                '</button>' +
                '<button type="button" class="btn btn-outline-secondary btn-sm" id="trvPpManualBtn">' +
                  '<i class="fa fa-keyboard me-1"></i> Type the MRZ instead' +
                '</button>' +
              '</div>' +

              '<div id="trvPpProgress" class="progress mb-3 d-none" style="height:6px">' +
                '<div class="progress-bar progress-bar-striped progress-bar-animated" id="trvPpBar" style="width:0%"></div>' +
              '</div>' +

              '<div id="trvPpManualWrap" class="mb-3 d-none">' +
                '<label class="form-label small fw-bold mb-1">Machine-readable zone</label>' +
                '<textarea id="trvPpMrz" class="form-control font-monospace" rows="3" spellcheck="false" ' +
                  'style="letter-spacing:1px;font-size:12px" ' +
                  'placeholder="P&lt;BGDADNAN&lt;&lt;H&lt;M&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&#10;A162782095BGD9405262M34092461467277248&lt;&lt;&lt;&lt;22"></textarea>' +
                '<div class="form-text">Type the two bottom lines exactly as printed. Use <code>&lt;</code> for each arrow-like filler character. Corrections here are re-checked against the passport\'s own check digits.</div>' +
                '<button type="button" class="btn btn-primary btn-sm mt-2" id="trvPpParseBtn">' +
                  '<i class="fa fa-check me-1"></i> Read these lines</button>' +
              '</div>' +

              '<div id="trvPpResult" class="d-none">' +
                '<hr class="my-3">' +
                '<div id="trvPpAlerts"></div>' +
                '<div class="table-responsive">' +
                  '<table class="table table-sm table-bordered mb-2">' +
                    '<thead class="table-light"><tr>' +
                      '<th style="width:34px" class="text-center">' +
                        '<input type="checkbox" id="trvPpAll" class="form-check-input" checked title="Select all"></th>' +
                      '<th>Field</th><th>Value read from the passport</th>' +
                    '</tr></thead>' +
                    '<tbody id="trvPpFields"></tbody>' +
                  '</table>' +
                '</div>' +
                '<div class="form-check form-switch small">' +
                  '<input class="form-check-input" type="checkbox" id="trvPpAttach" checked>' +
                  '<label class="form-check-label" for="trvPpAttach">' +
                    'Also file this image against the passenger once saved</label>' +
                '</div>' +
              '</div>' +

            '</div>' +
            '<div class="modal-footer">' +
              '<button type="button" class="btn btn-success d-none" id="trvPpApplyBtn">' +
                '<i class="fa fa-check me-1"></i> Fill the form</button>' +
              '<button type="button" class="btn btn-white" data-bs-dismiss="modal">Cancel</button>' +
            '</div>' +
          '</div>' +
        '</div>' +
      '</div>';

    var host = document.createElement('div');
    host.innerHTML = html;
    document.body.appendChild(host.firstChild);
    wire();
  }

  /* ── Wiring ────────────────────────────────────────────────────────────── */
  var _bandTop = 0.66, _bandBottom = 1.0, _img = null;

  function wire() {
    var drop = document.getElementById('trvPpDrop');
    var file = document.getElementById('trvPpFile');

    drop.addEventListener('click', function () { file.click(); });
    drop.addEventListener('dragover', function (e) {
      e.preventDefault(); drop.classList.add('border-info');
    });
    drop.addEventListener('dragleave', function () { drop.classList.remove('border-info'); });
    drop.addEventListener('drop', function (e) {
      e.preventDefault(); drop.classList.remove('border-info');
      if (e.dataTransfer.files && e.dataTransfer.files.length) loadImage(e.dataTransfer.files[0]);
    });
    file.addEventListener('change', function () {
      if (file.files && file.files.length) loadImage(file.files[0]);
    });

    document.getElementById('trvPpBandUp').onclick = function () {
      _bandTop = Math.max(0, _bandTop - 0.06);
      _bandBottom = Math.min(1, _bandTop + 0.34); drawBand();
    };
    document.getElementById('trvPpBandDown').onclick = function () {
      _bandTop = Math.min(0.9, _bandTop + 0.06);
      _bandBottom = Math.min(1, _bandTop + 0.34); drawBand();
    };
    document.getElementById('trvPpBandAll').onclick = function () {
      _bandTop = 0; _bandBottom = 1; drawBand();
    };

    document.getElementById('trvPpReadBtn').onclick   = runClientOcr;
    document.getElementById('trvPpServerBtn').onclick = runServerOcr;
    document.getElementById('trvPpParseBtn').onclick  = function () {
      parseText(document.getElementById('trvPpMrz').value, 'MRZ_MANUAL');
    };
    document.getElementById('trvPpManualBtn').onclick = function () {
      var w = document.getElementById('trvPpManualWrap');
      w.classList.toggle('d-none');
      if (!w.classList.contains('d-none')) document.getElementById('trvPpMrz').focus();
    };
    document.getElementById('trvPpAll').onchange = function () {
      var on = this.checked;
      document.querySelectorAll('#trvPpFields input[type=checkbox]').forEach(function (c) { c.checked = on; });
    };
    document.getElementById('trvPpApplyBtn').onclick = applyToForm;

    /* Keep the MRZ textarea inside the ICAO alphabet as the operator types. */
    document.getElementById('trvPpMrz').addEventListener('input', function () {
      var clean = this.value.toUpperCase().split('\n').map(function (line) {
        return line.split('').filter(function (c) { return MRZ_CHARS.indexOf(c) >= 0; }).join('');
      }).join('\n');
      if (clean !== this.value) {
        var pos = this.selectionStart;
        this.value = clean;
        this.selectionStart = this.selectionEnd = pos;
      }
    });
  }

  /* ── Image handling ────────────────────────────────────────────────────── */
  function loadImage(f) {
    if (!f.type || f.type.indexOf('image/') !== 0) {
      toastErr('That is not an image file. Photograph or scan the passport data page as JPG or PNG.');
      return;
    }
    if (f.size > 15 * 1024 * 1024) {
      toastErr('That image is over 15 MB. Reduce the resolution and try again.');
      return;
    }
    _lastFile = f;
    var reader = new FileReader();
    reader.onload = function (ev) {
      var img = new Image();
      img.onload = function () {
        _img = img;
        _bandTop = 0.66; _bandBottom = 1.0;
        document.getElementById('trvPpPreviewWrap').classList.remove('d-none');
        drawBand();
        document.getElementById('trvPpReadBtn').disabled = false;
        if (_serverOcr) document.getElementById('trvPpServerBtn').classList.remove('d-none');
      };
      img.onerror = function () { toastErr('That image could not be opened.'); };
      img.src = ev.target.result;
    };
    reader.readAsDataURL(f);
  }

  /**
   * Renders the selected band into the canvas, upscaled and binarised.
   * Restricting OCR to the MRZ strip and pushing it to pure black-on-white is
   * where nearly all of the accuracy comes from — Tesseract does badly on a
   * full colour passport page and well on a clean two-line strip.
   */
  function drawBand() {
    if (!_img) return;
    var canvas = document.getElementById('trvPpCanvas');
    var sy = Math.floor(_img.height * _bandTop);
    var sh = Math.max(20, Math.floor(_img.height * (_bandBottom - _bandTop)));

    var targetW = 1600;
    var scale   = targetW / _img.width;
    canvas.width  = targetW;
    canvas.height = Math.max(40, Math.floor(sh * scale));

    var ctx = canvas.getContext('2d', { willReadFrequently: true });
    ctx.imageSmoothingQuality = 'high';
    ctx.drawImage(_img, 0, sy, _img.width, sh, 0, 0, canvas.width, canvas.height);

    var data = ctx.getImageData(0, 0, canvas.width, canvas.height);
    var px = data.data, i, lum;

    /* Pass 1 — luminance + 256-bin histogram. */
    var hist = new Uint32Array(256), gray = new Uint8ClampedArray(px.length / 4);
    for (i = 0; i < px.length; i += 4) {
      lum = (px[i] * 0.299 + px[i + 1] * 0.587 + px[i + 2] * 0.114) | 0;
      gray[i >> 2] = lum;
      hist[lum]++;
    }

    /* Pass 2 — Otsu's method picks the threshold, so a dim phone photo and a
       bright flatbed scan both end up as clean black text on white. */
    var total = gray.length, sum = 0, k;
    for (k = 0; k < 256; k++) sum += k * hist[k];
    var sumB = 0, wB = 0, wF = 0, best = 0, threshold = 128;
    for (k = 0; k < 256; k++) {
      wB += hist[k]; if (wB === 0) continue;
      wF = total - wB; if (wF === 0) break;
      sumB += k * hist[k];
      var mB = sumB / wB, mF = (sum - sumB) / wF;
      var between = wB * wF * (mB - mF) * (mB - mF);
      if (between > best) { best = between; threshold = k; }
    }

    for (i = 0; i < px.length; i += 4) {
      var v = gray[i >> 2] > threshold ? 255 : 0;
      px[i] = px[i + 1] = px[i + 2] = v;
      px[i + 3] = 255;
    }
    ctx.putImageData(data, 0, 0);
  }

  /* ── Client-side OCR ───────────────────────────────────────────────────── */
  function loadTesseract() {
    if (window.Tesseract) return Promise.resolve(window.Tesseract);
    return new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = TESSERACT_SRC;
      s.onload  = function () { resolve(window.Tesseract); };
      s.onerror = function () { reject(new Error('load-failed')); };
      document.head.appendChild(s);
    });
  }

  function runClientOcr() {
    if (!_img) return;
    busy(true, 'Loading reader…');

    loadTesseract().then(function (T) {
      busy(true, 'Reading…');
      var canvas = document.getElementById('trvPpCanvas');
      return T.recognize(canvas, 'eng', {
        logger: function (m) {
          if (m.status === 'recognizing text') setBar(Math.round(m.progress * 100));
        }
      }).then(function (res) {
        return T.createWorker ? res : res;
      });
    }).then(function (res) {
      var text = (res && res.data && res.data.text) || '';
      busy(false);
      if (!text.trim()) {
        toastErr('Nothing legible was found in that strip. Nudge the strip with the arrows, '
               + 'or type the two lines in manually.');
        openManual();
        return;
      }
      parseText(text, 'OCR_CLIENT');
    }).catch(function (err) {
      busy(false);
      if (err && err.message === 'load-failed') {
        toastErr('The passport reader could not be downloaded — this machine may be offline. '
               + 'Type the two MRZ lines in instead.');
      } else {
        toastErr('The image could not be read automatically. Type the two MRZ lines in instead.');
      }
      openManual();
    });
  }

  function runServerOcr() {
    if (!_lastFile) return;
    busy(true, 'Sending…');
    postFile(SCAN_URL, _lastFile).then(function (d) {
      busy(false);
      var scan = (d.obj && d.obj.defaultData) || d.data;
      if (!d.success || !scan) { toastErr(d.message || 'The server could not read that image.'); openManual(); return; }
      showResult(scan);
    }).catch(function () {
      busy(false);
      toastErr('The server could not be reached.');
    });
  }

  /* ── Parse ─────────────────────────────────────────────────────────────── */
  function parseText(text, source) {
    if (!text || !text.trim()) { toastErr('There is nothing to read yet.'); return; }
    busy(true, 'Checking…');
    postJson(PARSE_URL, { mrzText: text, source: source }).then(function (d) {
      busy(false);
      var scan = (d.obj && d.obj.defaultData) || d.data;
      if (!d.success || !scan || !scan.success) {
        toastErr((scan && scan.message) || d.message || 'The machine-readable zone could not be read.');
        openManual();
        if (scan && scan.mrzLine1) {
          document.getElementById('trvPpMrz').value =
            [scan.mrzLine1, scan.mrzLine2, scan.mrzLine3].filter(Boolean).join('\n');
        }
        return;
      }
      showResult(scan);
    }).catch(function () {
      busy(false);
      toastErr('The server could not be reached.');
    });
  }

  /* ── Result table ──────────────────────────────────────────────────────── */
  var ROWS = [
    { key: 'givenNames',      label: 'First Name',        target: 'firstName' },
    { key: 'surname',         label: 'Last Name',         target: 'lastName' },
    { key: 'suggestedTitle',  label: 'Title',             target: 'title' },
    { key: 'dateOfBirth',     label: 'Date of Birth',     target: 'dateOfBirth' },
    { key: 'gender',          label: 'Gender',            target: 'gender' },
    { key: 'suggestedPassengerType', label: 'Passenger Type', target: 'passengerType' },
    { key: 'passportNumber',  label: 'Passport No',       target: 'passportNumber' },
    { key: 'passportExpiry',  label: 'Passport Expiry',   target: 'passportExpiry' },
    { key: 'nationalityName', label: 'Nationality',       target: 'nationality' },
    { key: 'issuingCountry',  label: 'Issuing Country',   target: 'passportCountry' },
    { key: 'personalNumber',  label: 'Personal / NID No', target: 'personalNumber' },
    { key: 'mrzLine1',        label: 'MRZ line 1',        target: 'mrzLine1', hidden: true },
    { key: 'mrzLine2',        label: 'MRZ line 2',        target: 'mrzLine2', hidden: true },
    { key: 'mrzLine3',        label: 'MRZ line 3',        target: 'mrzLine3', hidden: true }
  ];

  function showResult(scan) {
    _lastScan = scan;

    var alerts = document.getElementById('trvPpAlerts');
    alerts.innerHTML = '';
    if (scan.checkDigitsValid) {
      alerts.insertAdjacentHTML('beforeend',
        '<div class="alert alert-success py-2 px-3 small mb-2">' +
        '<i class="fa fa-shield-halved me-1"></i> Every check digit on this passport verified — ' +
        'the fields below were read correctly.</div>');
    }
    (scan.warnings || []).forEach(function (w) {
      alerts.insertAdjacentHTML('beforeend',
        '<div class="alert alert-warning py-2 px-3 small mb-2"><i class="fa fa-triangle-exclamation me-1"></i> '
        + esc(w) + '</div>');
    });

    var body = document.getElementById('trvPpFields');
    body.innerHTML = '';
    ROWS.forEach(function (row) {
      if (row.hidden) return;
      var val = scan[row.key];
      if (val === null || val === undefined || val === '') return;
      if (!_fieldMap[row.target]) return;          // form has no such field
      body.insertAdjacentHTML('beforeend',
        '<tr>' +
          '<td class="text-center align-middle">' +
            '<input type="checkbox" class="form-check-input" data-target="' + row.target + '" ' +
                   'data-value="' + esc(String(val)) + '" checked></td>' +
          '<td class="align-middle small text-muted">' + row.label + '</td>' +
          '<td class="align-middle fw-bold">' + esc(String(val)) + '</td>' +
        '</tr>');
    });

    if (!body.children.length) {
      body.insertAdjacentHTML('beforeend',
        '<tr><td colspan="3" class="text-center text-muted py-3">' +
        'The passport was read, but none of its fields match this form.</td></tr>');
    }

    document.getElementById('trvPpResult').classList.remove('d-none');
    document.getElementById('trvPpApplyBtn').classList.remove('d-none');
  }

  /* ── Apply ─────────────────────────────────────────────────────────────── */
  function applyToForm() {
    var applied = 0;
    document.querySelectorAll('#trvPpFields input[type=checkbox]:checked').forEach(function (cb) {
      if (setField(_fieldMap[cb.dataset.target], cb.dataset.value)) applied++;
    });

    /* MRZ lines ride along silently — audit trail, never shown as a form row. */
    ['mrzLine1', 'mrzLine2', 'mrzLine3'].forEach(function (k) {
      if (_lastScan[k] && _fieldMap[k]) setField(_fieldMap[k], _lastScan[k]);
    });

    if (document.getElementById('trvPpAttach').checked && _lastFile) {
      window.__trvPendingPassportFile = _lastFile;
    } else {
      window.__trvPendingPassportFile = null;
    }

    if (typeof _opts.onApply === 'function') _opts.onApply(_lastScan, _lastFile);

    bootstrap.Modal.getOrCreateInstance(document.getElementById('trvPassportModal')).hide();

    if (typeof Swal !== 'undefined') {
      Swal.fire({
        icon: 'success', timer: 1600, showConfirmButton: false,
        title: applied + ' field' + (applied === 1 ? '' : 's') + ' filled in',
        text: _lastScan.checkDigitsValid
          ? 'Check digits verified. Review and save.'
          : 'Some check digits failed — please review every field before saving.'
      });
    }
  }

  function setField(id, value) {
    if (!id) return false;
    var el = typeof id === 'string' ? document.getElementById(id) : id;
    if (!el || value === undefined || value === null || value === '') return false;

    if (el.tagName === 'SELECT') {
      var matched = false;
      for (var i = 0; i < el.options.length; i++) {
        if (String(el.options[i].value).toUpperCase() === String(value).toUpperCase()) {
          el.selectedIndex = i; matched = true; break;
        }
      }
      if (!matched) return false;
    } else if (el.type === 'date') {
      el.value = String(value).substring(0, 10);   // the API always sends yyyy-MM-dd
    } else {
      el.value = value;
    }

    el.dispatchEvent(new Event('change', { bubbles: true }));
    el.dispatchEvent(new Event('input',  { bubbles: true }));
    if (window.jQuery) window.jQuery(el).trigger('change');   // Select2
    flash(el);
    return true;
  }

  function flash(el) {
    var original = el.style.backgroundColor;
    el.style.transition = 'background-color .4s';
    el.style.backgroundColor = 'rgba(25,135,84,.18)';
    setTimeout(function () { el.style.backgroundColor = original || ''; }, 1200);
  }

  /* ── Small helpers ─────────────────────────────────────────────────────── */
  function busy(on, label) {
    var btn  = document.getElementById('trvPpReadBtn');
    var spin = document.getElementById('trvPpSpin');
    var txt  = document.getElementById('trvPpReadTxt');
    var prog = document.getElementById('trvPpProgress');
    if (!btn) return;
    btn.disabled = on || !_img;
    spin.classList.toggle('d-none', !on);
    txt.textContent = on ? (label || 'Working…') : 'Read Passport';
    prog.classList.toggle('d-none', !on);
    if (!on) setBar(0);
  }
  function setBar(pct) {
    var bar = document.getElementById('trvPpBar');
    if (bar) bar.style.width = pct + '%';
  }
  function openManual() {
    document.getElementById('trvPpManualWrap').classList.remove('d-none');
  }
  function toastErr(msg) {
    if (typeof Swal !== 'undefined') Swal.fire('Passport', msg, 'warning');
    else alert(msg);
  }
  function esc(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  /* ── Capabilities probe (once per page) ────────────────────────────────── */
  function probeCaps() {
    if (_capsChecked) return;
    _capsChecked = true;
    fetch(CAPS_URL, { credentials: 'same-origin' })
      .then(function (r) { return r.json(); })
      .then(function (d) { _serverOcr = !!(d && d.serverOcr); })
      .catch(function () { _serverOcr = false; });
  }

  /* ═════════════════════════════════════════════════════════════════════════
   * PUBLIC API
   * ═══════════════════════════════════════════════════════════════════════ */

  /**
   * Opens the passport reader and fills the given form on apply.
   *
   * @param {Object} fieldMap  logical name → element id, e.g.
   *        { title:'apTitle', firstName:'apFirstName', lastName:'apLastName',
   *          dateOfBirth:'apDob', gender:'apGender', passengerType:'apPaxType',
   *          passportNumber:'apPassportNo', passportExpiry:'apPassportExp',
   *          nationality:'apNationality', passportCountry:'apPassportCountry',
   *          personalNumber:'apPersonalNo',
   *          mrzLine1:'apMrz1', mrzLine2:'apMrz2' }
   *        Only the keys present are touched, so a small form and the full
   *        Passengers form can share this one helper.
   *
   * @param {Object} [options]
   *        options.onApply  fn(scanResult, imageFile) after the fields are set
   *        options.scanBtn  the button that opened it (kept for callers that
   *                         already pass it; nothing is required of it)
   */
  window.scanPassport = function (fieldMap, options) {
    _fieldMap = fieldMap || {};
    _opts     = options  || {};
    probeCaps();
    ensureModal();

    _img = null; _lastFile = null; _lastScan = null;
    window.__trvPendingPassportFile = null;
    document.getElementById('trvPpFile').value = '';
    document.getElementById('trvPpPreviewWrap').classList.add('d-none');
    document.getElementById('trvPpResult').classList.add('d-none');
    document.getElementById('trvPpApplyBtn').classList.add('d-none');
    document.getElementById('trvPpManualWrap').classList.add('d-none');
    document.getElementById('trvPpServerBtn').classList.add('d-none');
    document.getElementById('trvPpMrz').value = '';
    document.getElementById('trvPpReadBtn').disabled = true;
    busy(false);

    bootstrap.Modal.getOrCreateInstance(document.getElementById('trvPassportModal')).show();
  };

  /**
   * Files the image the operator scanned against a saved passenger. Call it
   * right after a successful save, when the passenger id finally exists.
   * Silent no-op when nothing is pending, so it is safe to call every time.
   */
  window.trvAttachPassportImage = function (passengerId) {
    var f = window.__trvPendingPassportFile;
    if (!f || !passengerId) return Promise.resolve(null);
    window.__trvPendingPassportFile = null;
    return postFile('/travel/passengers/' + passengerId + '/passport-image', f)
      .then(function (d) {
        if (!d.success && typeof Swal !== 'undefined') {
          /* The passenger saved fine — only the attachment failed, so this is
             a note, not an error. */
          Swal.fire({ icon: 'info', title: 'Passenger saved',
                      text: 'The passport image could not be filed: ' + (d.message || 'unknown error'),
                      timer: 3500, showConfirmButton: false });
        }
        return d;
      })
      .catch(function () { return null; });
  };

  /** The most recent parsed scan — handy for custom form wiring. */
  window.trvPassportLastScan = function () { return _lastScan; };
})();
