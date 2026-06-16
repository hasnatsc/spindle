/**
 * spindle-app.js
 * Spindle ERP — Shared JavaScript Utilities
 *
 * Place at: src/main/resources/static/js/spindle-app.js
 *
 * Provides:
 *   secureFetch()          — AJAX wrapper that injects CSRF token
 *   submitWithSecureFetch()— button-spinner + AJAX save helper
 *   confirmAndExecute()    — SweetAlert2 confirm + DELETE/POST
 *   hsDisableButton()      — spinner on a submit button
 *   hsEnableButton()       — restore a submit button
 *   hsAfterSaveMessages()  — toast + DataTable reload after save
 *   hsResetForm()          — reset a form cleanly
 *   objectifyForm()        — form → JS object
 */

/* ── CSRF ─────────────────────────────────────────────────────────────── */
function getCsrfToken() {
  const meta = document.querySelector('meta[name="_csrf"]');
  return meta ? meta.getAttribute('content') : null;
}
function getCsrfHeader() {
  const meta = document.querySelector('meta[name="_csrf_header"]');
  return meta ? meta.getAttribute('content') : 'X-XSRF-TOKEN';
}

/* ── secureFetch ─────────────────────────────────────────────────────── */
/**
 * Wrapper around fetch that:
 *  • Adds CSRF token header for non-GET requests
 *  • Adds Content-Type: application/json
 *  • Rejects with a user-friendly message on HTTP error
 *  • Rejects with session-expired message on 401
 *
 * @param {string} url
 * @param {object} options  — same as fetch() options
 * @returns {Promise<any>}  — parsed JSON
 */
function secureFetch(url, options = {}) {
  const method  = (options.method || 'GET').toUpperCase();
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };

  const token = getCsrfToken();
  if (token && method !== 'GET') {
    headers[getCsrfHeader()] = token;
  }

  return fetch(url, { ...options, headers })
    .then(res => {
      if (res.status === 401) {
        window.location.href = '/login?expired';
        return Promise.reject(new Error('Session expired. Redirecting to login…'));
      }
      if (!res.ok) return Promise.reject(new Error(`HTTP ${res.status}: ${res.statusText}`));
      return res.json();
    });
}

/* ── Button helpers ──────────────────────────────────────────────────── */
function hsDisableButton(btn, spinner, btnText, loadingText = 'Saving…') {
  if (!btn) return;
  btn.disabled = true;
  if (spinner)  spinner.classList.remove('d-none');
  if (btnText)  btnText.textContent = loadingText;
}

function hsEnableButton(btn, spinner, btnText, originalText = 'Save') {
  if (!btn) return;
  btn.disabled = false;
  if (spinner)  spinner.classList.add('d-none');
  if (btnText && btnText.dataset.originalText)
    btnText.textContent = btnText.dataset.originalText;
  else if (btnText)
    btnText.textContent = originalText;
}

/* ── hsResetForm ─────────────────────────────────────────────────────── */
function hsResetForm(form) {
  if (!form) return;
  form.reset();
  // Clear any validation states
  form.querySelectorAll('.is-invalid, .is-valid').forEach(el => {
    el.classList.remove('is-invalid', 'is-valid');
  });
  form.querySelectorAll('.invalid-feedback, .valid-feedback').forEach(el => el.remove());
}

/* ── objectifyForm ───────────────────────────────────────────────────── */
/**
 * Serialise a form into a plain JS object.
 * Does NOT handle multi-select or file inputs — handle those manually.
 */
function objectifyForm(form) {
  const obj = {};
  new FormData(form).forEach((value, key) => {
    if (key === 'confirmPassword') return; // never send to server
    obj[key] = value;
  });
  return obj;
}

/* ── hsAfterSaveMessages ─────────────────────────────────────────────── */
/**
 * Show a SweetAlert2 toast after a save attempt, then optionally reload a DataTable.
 *
 * @param {string}  message      — message to display
 * @param {boolean} success      — true = success toast, false = error toast
 * @param {Element} form         — form to reset on success (or null)
 * @param {object}  dataTable    — DataTables instance or TABLE element (or null)
 */
function hsAfterSaveMessages(message, success, form, dataTable) {
  const icon = success ? 'success' : 'error';

  if (typeof Swal !== 'undefined') {
    Swal.fire({
      icon,
      title: success ? 'Success!' : 'Error',
      text:  message || (success ? 'Saved successfully.' : 'An error occurred.'),
      timer: success ? 2200 : undefined,
      timerProgressBar: success,
      showConfirmButton: !success,
    });
  }

  if (success) {
    if (form) hsResetForm(form);
    if (dataTable) {
      // Accept either a DataTables API instance or a TABLE element
      const dtInstance = (dataTable instanceof Element)
        ? ($ && $(dataTable).DataTable ? $(dataTable).DataTable() : null)
        : dataTable;
      if (dtInstance && typeof dtInstance.ajax === 'object') dtInstance.ajax.reload(null, false);
    }
  }
}

/* ── submitWithSecureFetch ───────────────────────────────────────────── */
/**
 * A full save cycle: disable button → fetch → handle response → re-enable.
 *
 * Options:
 *   url, method, body        — request config
 *   submitBtn, spinner, btnText, loadingText
 *   successTitle, onSuccess
 */
function submitWithSecureFetch({
  url, method = 'POST', body,
  submitBtn, spinner, btnText, loadingText = 'Saving…',
  successTitle = 'Success!',
  onSuccess = null,
}) {
  if (!url) return;
  hsDisableButton(submitBtn, spinner, btnText, loadingText);

  secureFetch(url, {
    method,
    body: typeof body === 'string' ? body : JSON.stringify(body),
  })
  .then(data => {
    if (data.success) {
      if (typeof Swal !== 'undefined') {
        Swal.fire({ icon: 'success', title: successTitle, text: data.message, timer: 2000, timerProgressBar: true, showConfirmButton: false });
      }
      if (typeof onSuccess === 'function') onSuccess(data);
    } else {
      if (typeof Swal !== 'undefined') {
        Swal.fire({ icon: 'error', title: 'Error', text: data.message || 'An error occurred.' });
      }
    }
  })
  .catch(err => {
    if (typeof Swal !== 'undefined') {
      Swal.fire({ icon: 'error', title: 'Request Failed', text: err.message });
    }
  })
  .finally(() => hsEnableButton(submitBtn, spinner, btnText));
}

/* ── confirmAndExecute ───────────────────────────────────────────────── */
/**
 * Show a SweetAlert2 confirm dialog, then run a fetch, then reload DataTable.
 *
 * Options:
 *   title, text, icon, confirmText, confirmColor
 *   url, method
 *   successTitle, reloadTable, dataTable
 */
function confirmAndExecute({
  title = 'Are you sure?',
  text  = 'This action cannot be undone.',
  icon  = 'warning',
  confirmText  = 'Yes, proceed',
  confirmColor = '#2563eb',
  cancelText   = 'Cancel',
  url, method = 'POST',
  successTitle = 'Done!',
  reloadTable  = true,
  dataTable    = null,
}) {
  if (typeof Swal === 'undefined') return;

  Swal.fire({
    title, text, icon,
    showCancelButton: true,
    confirmButtonColor: confirmColor,
    confirmButtonText:  confirmText,
    cancelButtonText:   cancelText,
  }).then(result => {
    if (!result.isConfirmed) return;

    secureFetch(url, { method })
      .then(data => {
        Swal.fire({
          icon: data.success ? 'success' : 'error',
          title: data.success ? successTitle : 'Error',
          text:  data.message,
          timer: data.success ? 1800 : undefined,
          timerProgressBar: data.success,
          showConfirmButton: !data.success,
        });
        if (data.success && reloadTable && dataTable) {
          const dtInstance = (dataTable instanceof Element)
            ? ($ && $(dataTable).DataTable ? $(dataTable).DataTable() : null)
            : dataTable;
          if (dtInstance) dtInstance.ajax.reload(null, false);
        }
      })
      .catch(err => {
        Swal.fire({ icon: 'error', title: 'Request Failed', text: err.message });
      });
  });
}
