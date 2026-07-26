/* ============================================================
   travel-ocr.js — Passport OCR scanning & passenger auto-fill
   Depends on: jQuery, secureFetch, Swal (SweetAlert2)
   ============================================================ */
(function () {
  'use strict';

  const OCR_URL = '/travel/ocr/passport';

  /**
   * Open a file picker for a passport image, upload it to the OCR endpoint,
   * and populate the given form fields with the extracted data.
   *
   * @param {object} fieldMap — mapping of OCR field names → DOM element IDs
   *   {
   *     firstName:     'apFirstName',
   *     lastName:      'apLastName',
   *     dateOfBirth:   'apDob',
   *     passportNumber:'apPassportNo',
   *     passportExpiry:'apPassportExp',
   *     nationality:   'apNationality'
   *   }
   * @param {object} [opts] — additional options
   *   @param {string}  [opts.accept]     — file accept filter (default: '.jpg,.jpeg,.png,.tif,.tiff,.webp')
   *   @param {Function} [opts.onComplete] — callback after fields are populated
   *   @param {HTMLElement} [opts.scanBtn] — the button that triggered the scan (for loading state)
   */
  window.scanPassport = function (fieldMap, opts) {
    opts = opts || {};
    var accept = opts.accept || '.jpg,.jpeg,.png,.tif,.tiff,.webp';

    // Create a hidden file input
    var fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = accept;
    fileInput.style.display = 'none';
    document.body.appendChild(fileInput);

    fileInput.addEventListener('change', function () {
      if (!fileInput.files.length) {
        document.body.removeChild(fileInput);
        return;
      }

      var file = fileInput.files[0];
      var maxSize = 15 * 1024 * 1024; // 15 MB
      if (file.size > maxSize) {
        Swal.fire('File Too Large', 'Please select an image under 15 MB.', 'warning');
        document.body.removeChild(fileInput);
        return;
      }

      var btn = opts.scanBtn;
      var originalHtml = btn ? btn.innerHTML : null;
      if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Scanning…';
      }

      var formData = new FormData();
      formData.append('file', file);

      secureFetch(OCR_URL, { method: 'POST', body: formData })
        .then(function (res) {
          if (res.success && res.data) {
            var d = res.data;

            // Auto-fill form fields
            if (fieldMap.firstName && d.firstName) {
              var el = document.getElementById(fieldMap.firstName);
              if (el) el.value = toTitleCase(d.firstName);
            }
            if (fieldMap.lastName && d.lastName) {
              var el = document.getElementById(fieldMap.lastName);
              if (el) el.value = toTitleCase(d.lastName);
            }
            if (fieldMap.dateOfBirth && d.dateOfBirth) {
              var el = document.getElementById(fieldMap.dateOfBirth);
              if (el) el.value = d.dateOfBirth;
            }
            if (fieldMap.passportNumber && d.passportNumber) {
              var el = document.getElementById(fieldMap.passportNumber);
              if (el) el.value = d.passportNumber.toUpperCase();
            }
            if (fieldMap.passportExpiry && d.passportExpiry) {
              var el = document.getElementById(fieldMap.passportExpiry);
              if (el) el.value = d.passportExpiry;
            }
            if (fieldMap.nationality && d.nationality) {
              var el = document.getElementById(fieldMap.nationality);
              if (el) el.value = d.nationality;
            }

            var populated = [];
            if (d.firstName || d.lastName) populated.push('name');
            if (d.dateOfBirth) populated.push('date of birth');
            if (d.passportNumber) populated.push('passport no');
            if (d.nationality) populated.push('nationality');
            if (d.passportExpiry) populated.push('expiry');

            var msg = populated.length
              ? 'Extracted: ' + populated.join(', ') + '. Please verify before saving.'
              : 'Could not extract data from this image. Try a clearer scan.';
            Swal.fire({
              icon: populated.length ? 'success' : 'info',
              title: populated.length ? 'Passport Scanned' : 'Scan Result',
              text: msg,
              timer: populated.length ? 3000 : 4000,
              showConfirmButton: true
            });

            if (opts.onComplete) opts.onComplete(d);
          } else {
            Swal.fire('Scan Failed', res.message || 'Could not read passport. Try a clearer image.', 'error');
          }
        })
        .catch(function (err) {
          Swal.fire('Error', err.message || 'OCR request failed.', 'error');
        })
        .finally(function () {
          if (btn && originalHtml !== null) {
            btn.disabled = false;
            btn.innerHTML = originalHtml;
          }
          document.body.removeChild(fileInput);
        });
    });

    fileInput.click();
  };

  /**
   * Create a "Scan Passport" button element.
   *
   * @param {object} fieldMap — same as scanPassport fieldMap
   * @param {object} [opts]   — button options
   *   @param {string} [opts.className] — CSS classes (default: 'btn btn-sm btn-outline-info')
   *   @param {string} [opts.label]     — button text (default: 'Scan Passport')
   * @returns {HTMLElement} the button element
   */
  window.createScanButton = function (fieldMap, opts) {
    opts = opts || {};
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = opts.className || 'btn btn-sm btn-outline-info';
    btn.innerHTML = '<i class="fa fa-passport me-1"></i> ' + (opts.label || 'Scan Passport');
    btn.title = 'Upload a passport image to auto-fill passenger details';
    btn.onclick = function () {
      scanPassport(fieldMap, { scanBtn: btn });
    };
    return btn;
  };

  /** Convert a string to Title Case. */
  function toTitleCase(str) {
    if (!str) return str;
    return str.toLowerCase()
      .split(/\s+/)
      .map(function (w) { return w.charAt(0).toUpperCase() + w.slice(1); })
      .join(' ');
  }

})();
