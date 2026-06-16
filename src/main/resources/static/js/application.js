/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  application.js — Spindle ERP / Optimum ERP                         ║
 * ║  Global utility library                                              ║
 * ║  Path: src/main/resources/static/js/application.js                 ║
 * ║                                                                      ║
 * ║  BUGS FIXED vs uploaded version:                                    ║
 * ║                                                                      ║
 * ║  #1  submitWithSecureFetch .finally() hard-coded                    ║
 * ║      btnText.textContent = "Change Password"                        ║
 * ║      → now restores ORIGINAL label captured before call             ║
 * ║                                                                      ║
 * ║  #2  hsAfterSaveMessages crashes when #successMessages              ║
 * ║      container is null → falls back to Swal toast                   ║
 * ║                                                                      ║
 * ║  #3  confirmAndExecute: $(dataTable).DataTable().ajax.reload()      ║
 * ║      crashes when dataTable is already a DataTable API instance     ║
 * ║      → new _reloadDT() helper detects instance vs selector          ║
 * ║                                                                      ║
 * ║  #4  actionWithRemarks: references undefined variable `dataTables`  ║
 * ║      → fixed to use `dataTable` parameter via _reloadDT()           ║
 * ║                                                                      ║
 * ║  #5  hsOpenModal: new bootstrap.Modal(el) creates duplicate         ║
 * ║      instances on repeated clicks                                    ║
 * ║      → changed to getOrCreateInstance()                             ║
 * ║                                                                      ║
 * ║  #6  hsOpenModalForm: same duplicate-instance bug                   ║
 * ║      → changed to getOrCreateInstance()                             ║
 * ║                                                                      ║
 * ║  #7  hsFetchAndShowModal: same duplicate-instance bug               ║
 * ║      → changed to getOrCreateInstance()                             ║
 * ║                                                                      ║
 * ║  #8  formatCurrency: two stray console.log() in production path     ║
 * ║      → removed                                                       ║
 * ║                                                                      ║
 * ║  #9  hsServerError: 401 handler empty — no redirect                 ║
 * ║      → redirects to /login?expired                                  ║
 * ║                                                                      ║
 * ║  #10 hsDisableButton / hsEnableButton: no original-label storage    ║
 * ║      → hsDisableButton stores dataset.originalText                  ║
 * ║      → hsEnableButton restores from dataset.originalText            ║
 * ║                                                                      ║
 * ║  #11 hsAfterSaveMessages: dataTables.ajax.reload() crashes when     ║
 * ║      dataTables is null (called from error path)                    ║
 * ║      → null-checked via _reloadDT()                                 ║
 * ║                                                                      ║
 * ║  #12 hsInitAjaxForm: passes string selector to hsAfterSaveMessages  ║
 * ║      which calls .ajax.reload() directly on a jQuery object         ║
 * ║      → wrapped in _reloadDT() which handles all types               ║
 * ║                                                                      ║
 * ║  #13 hsChangeStatus / hsConfirmDelete: pass jQuery selector string  ║
 * ║      to $(sel).DataTable().ajax.reload() — breaks when called with  ║
 * ║      a DataTable instance instead of a string                       ║
 * ║      → both now use _reloadDT()                                     ║
 * ║                                                                      ║
 * ║  #14 hsPostAction: references undefined `dataTable` variable        ║
 * ║      → now uses secureFetch (not raw fetch) + fixed reference       ║
 * ║                                                                      ║
 * ║  #15 importData: $(dataTable).DataTable().ajax.reload()             ║
 * ║      crashes when dataTable is already a DT instance                ║
 * ║      → uses _reloadDT()                                             ║
 * ║                                                                      ║
 * ║  NEW: _reloadDT(dt)  — reload any: DT instance | CSS selector |    ║
 * ║       DOM Element | null (no-op)                                    ║
 * ║  NEW: toast()        — unified SweetAlert2 toast helper            ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */

'use strict';

/* ═══════════════════════════════════════════════════════════════════════
   PRIVATE HELPERS
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Safely reload any DataTable — accepts:
 *   • DataTable API instance   (has .ajax.reload)
 *   • CSS selector string      e.g. '#myTable'
 *   • DOM Element              e.g. document.getElementById('myTable')
 *   • null / undefined         (no-op)
 */
function _reloadDT(dt) {
    if (!dt) return;
    try {
        if (typeof dt.ajax === 'object' && typeof dt.ajax.reload === 'function') {
            dt.ajax.reload(null, false);
        } else if (typeof dt === 'string') {
            $(dt).DataTable().ajax.reload(null, false);
        } else if (dt instanceof Element) {
            $(dt).DataTable().ajax.reload(null, false);
        }
    } catch (e) {
        console.warn('[_reloadDT] Could not reload DataTable:', e.message);
    }
}

/**
 * Unified SweetAlert2 toast notification.
 * @param {string} title
 * @param {string} [text='']
 * @param {'success'|'error'|'warning'|'info'} [type='success']
 * @param {number} [timer=2500]
 */
function toast(title, text = '', type = 'success', timer = 2500) {
    if (typeof Swal === 'undefined') {
        console.log(`[${type.toUpperCase()}] ${title}${text ? ': ' + text : ''}`);
        return;
    }
    Swal.fire({
        icon: type, title, text,
        toast: true, position: 'top-end',
        timer, timerProgressBar: true,
        showConfirmButton: false
    });
}


/* ═══════════════════════════════════════════════════════════════════════
   secureFetch — CSRF-aware fetch wrapper                       [window]
   ═══════════════════════════════════════════════════════════════════════ */
window.secureFetch = async function (url, options = {}) {

    const method  = (options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});

    // Read CSRF dynamically on every call (safe after page refresh)
    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    // Set Content-Type for JSON bodies (skip for FormData — browser sets boundary)
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    // Attach CSRF for mutating requests
    if (method !== 'GET' && csrfToken && csrfHeader) {
        headers.set(csrfHeader, csrfToken);
    }

    const response = await fetch(url, {
        credentials: 'same-origin',
        ...options,
        method,
        headers
    });

    // ✅ FIX #9 — 401 redirect to login
    if (response.status === 401) {
        window.location.href = '/login?expired';
        throw new Error('Session expired. Redirecting to login…');
    }

    if (response.status === 204) {
        return { success: true, message: 'Operation completed successfully.' };
    }

    const contentType = response.headers.get('content-type') || '';

    if (contentType.includes('application/json')) {
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || `HTTP ${response.status}`);
        return data;
    }

    const text = await response.text();
    if (!response.ok) throw new Error(text || `HTTP ${response.status}`);
    return { success: true, message: text };
};


/* ═══════════════════════════════════════════════════════════════════════
   DOM READY — global widget initialisation
   ═══════════════════════════════════════════════════════════════════════ */
$(document).ready(function () {

    // ── Select2 (class .hsSelectTwo) ─────────────────────────────────────
    $('.hsSelectTwo').each(function () {
        const $el = $(this);
        $el.select2({
            dropdownParent: $el.closest('.modal').length
                ? $el.closest('.modal')
                : $el.parent(),
            placeholder: $el.data('placeholder') || 'Select one…',
            allowClear: true,
            width: '100%'
        });
    });

    // ── Date picker (class .hsPickDate) ───────────────────────────────────
    $('.hsPickDate').datepicker({
        format:         'dd-mm-yyyy',
        autoclose:      true,
        todayHighlight: true,
        todayBtn:       'linked',
        orientation:    'auto'
    }).on('changeDate', function () {
        $(this).datepicker('hide');
        // Trigger Parsley validation if available
        if ($(this).parsley) {
            try { $(this).parsley().validate(); } catch (e) { /* ignore */ }
        }
    });

    // ── DateTime picker (class .hsPickDateTime) ───────────────────────────
    if ($.fn.datetimepicker) {
        $('.hsPickDateTime').datetimepicker({
            format:          'DD-MM-YYYY HH:mm',
            showTodayButton: true,
            showClear:       true,
            showClose:       true,
            sideBySide:      true,
            icons: {
                time:     'fa fa-clock',
                date:     'fa fa-calendar',
                up:       'fa fa-chevron-up',
                down:     'fa fa-chevron-down',
                previous: 'fa fa-chevron-left',
                next:     'fa fa-chevron-right',
                today:    'fa fa-crosshairs',
                clear:    'fa fa-trash',
                close:    'fa fa-check'
            }
        });
    }
});


/* ═══════════════════════════════════════════════════════════════════════
   FORM UTILITIES
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Serialize a <form> to a plain JS object.
 * Supports dot-notation keys for nested objects (e.g. "address.city").
 * Skips confirmPassword — never sent to the server.
 */
function objectifyForm(form) {
    const data = {};
    new FormData(form).forEach((value, key) => {
        if (key === 'confirmPassword') return;
        if (key.includes('.')) {
            const parts = key.split('.');
            let obj = data;
            for (let i = 0; i < parts.length - 1; i++) {
                obj[parts[i]] = obj[parts[i]] || {};
                obj = obj[parts[i]];
            }
            obj[parts[parts.length - 1]] = value || '';
        } else {
            data[key] = value || '';
        }
    });
    return data;
}

/**
 * Reset a form: clear fields, hidden inputs, and Select2 widgets.
 * Calls optional hsCustomForm() if defined on the page.
 */
function hsResetForm(form) {
    if (!form) return;
    form.reset();
    form.querySelectorAll('input[type="hidden"]').forEach(el => el.value = '');
    if (typeof $ !== 'undefined') {
        $(form).find('select').val('').trigger('change');
    }
    if (typeof hsCustomForm === 'function') hsCustomForm();
}

/**
 * Initialise a date-picker on a runtime-created element (not present at DOM-ready).
 * @param {string} refClass — jQuery selector
 */
function initDatefield(refClass) {
    $(refClass).datepicker({
        format:         'dd-mm-yyyy',
        autoclose:      true,
        todayHighlight: true,
        todayBtn:       'linked'
    }).on('changeDate', function () {
        $(this).datepicker('hide');
        try { $(this).parsley().validate(); } catch (e) { /* ignore */ }
    });
}


/* ═══════════════════════════════════════════════════════════════════════
   BUTTON HELPERS
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Disable a submit button, show its spinner, and change its label.
 * ✅ FIX #10: stores original label in dataset.originalText so
 *    hsEnableButton can restore it — not hard-code "Save".
 */
function hsDisableButton(submitBtn, spinner, btnText, loadingText = 'Processing…') {
    if (!submitBtn) return;
    submitBtn.disabled = true;
    if (spinner) spinner.classList.remove('d-none');
    if (btnText) {
        // Store original label BEFORE overwriting it
        if (!btnText.dataset.originalText) {
            btnText.dataset.originalText = btnText.textContent.trim();
        }
        btnText.textContent = loadingText;
    }
}

/**
 * Re-enable a submit button, hide its spinner, restore the original label.
 * ✅ FIX #10: restores dataset.originalText set by hsDisableButton.
 */
function hsEnableButton(submitBtn, spinner, btnText, fallback = 'Save') {
    if (!submitBtn) return;
    submitBtn.disabled = false;
    if (spinner) spinner.classList.add('d-none');
    if (btnText) {
        // Restore stored original label (not hard-coded "Change Password")
        btnText.textContent = btnText.dataset.originalText || fallback;
    }
}

/** Disable a button by ID (adds visual disabled state). */
function hsDisableBtn(id) {
    const btn = document.getElementById(id);
    if (!btn) return;
    btn.disabled      = true;
    btn.style.opacity = '0.6';
    btn.style.cursor  = 'not-allowed';
    btn.classList.add('disabled');
}

/** Re-enable a button by ID. */
function hsEnableBtn(id) {
    const btn = document.getElementById(id);
    if (!btn) return;
    btn.disabled      = false;
    btn.style.opacity = '';
    btn.style.cursor  = '';
    btn.classList.remove('disabled');
}


/* ═══════════════════════════════════════════════════════════════════════
   MODAL HELPERS
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Show a Bootstrap 5 modal.
 * ✅ FIX #5: uses getOrCreateInstance() — avoids duplicate-modal bugs.
 * @param {HTMLElement|string} el — element or CSS selector
 */
function hsOpenModal(el) {
    const element = (typeof el === 'string') ? document.querySelector(el) : el;
    if (!element) return;
    bootstrap.Modal.getOrCreateInstance(element).show();
}

/**
 * Show a modal then reset a form.
 * ✅ FIX #6: uses getOrCreateInstance().
 */
function hsOpenModalForm(modalId, formId) {
    const modalEl = document.getElementById(modalId);
    if (!modalEl) return;
    bootstrap.Modal.getOrCreateInstance(modalEl).show();
    hsResetForm(document.getElementById(formId));
}

/**
 * Fetch JSON from an API then open a modal via callback.
 * ✅ FIX #7: uses getOrCreateInstance().
 */
function hsFetchAndShowModal(url, modalId, fillCallback) {
    secureFetch(url)
        .then(data => {
            if (typeof fillCallback === 'function') fillCallback(data);
            const el = document.getElementById(modalId);
            if (el) bootstrap.Modal.getOrCreateInstance(el).show();
        })
        .catch(err => {
            console.error('[hsFetchAndShowModal]', err);
            toast('Error', 'Failed to load data: ' + err.message, 'error');
        });
}


/* ═══════════════════════════════════════════════════════════════════════
   DATATABLE HELPERS
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Initialise a server-side DataTable with sensible defaults.
 * @param {string} selector
 * @param {string} ajaxUrl
 * @param {Array}  columns — DataTables column definitions
 * @param {Object} [extra] — additional DataTables config
 */
function hsInitDataTable(selector, ajaxUrl, columns, extra = {}) {
    return $(selector).DataTable({
        processing: true,
        serverSide: true,
        responsive: true,
        pageLength: 25,
        ajax: { url: ajaxUrl, type: 'GET' },
        columns,
        ...extra
    });
}

/**
 * After a save: show an alert in the page and optionally reload a DataTable.
 *
 * ✅ FIX #2: null-safe for missing container element (falls back to toast).
 * ✅ FIX #11: null-safe DataTable reload via _reloadDT().
 *
 * @param {string}          message
 * @param {boolean}         success
 * @param {HTMLFormElement} [form]       — null to skip form reset
 * @param {*}               [dataTable]  — DT instance | selector | Element | null
 * @param {string}          [containerId]
 */
function hsAfterSaveMessages(message, success, form, dataTable, containerId) {
    const cid       = containerId || 'successMessages';
    const container = document.getElementById(cid);

    if (container) {
        const alert = document.createElement('div');
        alert.className = `alert alert-${success ? 'success' : 'danger'} alert-dismissible fade show`;
        alert.innerHTML = `${message} <button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
        container.appendChild(alert);
        setTimeout(() => alert.remove(), 5000);
    } else {
        // ✅ FIX #2: fallback toast when no container element exists in the DOM
        toast(success ? 'Saved!' : 'Error', message, success ? 'success' : 'error');
    }

    if (success) {
        if (form) hsResetForm(form);
        // ✅ FIX #11: safe DT reload regardless of type
        _reloadDT(dataTable);
        if (typeof hsCustomForm === 'function') hsCustomForm();
    }
}


/* ═══════════════════════════════════════════════════════════════════════
   CONFIRM & EXECUTE
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * SweetAlert2 confirm dialog → secureFetch → success/error toast → reload DT.
 * ✅ FIX #3: uses _reloadDT() which handles DT instance, selector, and Element.
 */
window.confirmAndExecute = function ({
    title         = 'Are you sure?',
    text          = "You won't be able to revert this!",
    icon          = 'warning',
    confirmText   = 'Yes, proceed!',
    cancelText    = 'Cancel',
    confirmColor  = '#3085d6',
    cancelColor   = '#d33',
    url,
    method        = 'POST',
    body          = null,
    successTitle  = 'Success!',
    successMessage= null,
    reloadTable   = false,
    dataTable     = null
} = {}) {

    Swal.fire({
        title, text, icon,
        showCancelButton:   true,
        confirmButtonColor: confirmColor,
        cancelButtonColor:  cancelColor,
        confirmButtonText:  confirmText,
        cancelButtonText:   cancelText
    }).then(result => {
        if (!result.isConfirmed) return;

        secureFetch(url, {
            method,
            body: body ? JSON.stringify(body) : null
        })
        .then(data => {
            if (!data.success) throw new Error(data.message || 'Operation failed.');

            Swal.fire({
                icon:              'success',
                title:             successTitle,
                text:              successMessage || data.message,
                timer:             2000,
                showConfirmButton: false
            });

            if (reloadTable) _reloadDT(dataTable);   // ✅ FIX #3
        })
        .catch(err => {
            Swal.fire({ icon: 'error', title: 'Error!', text: err.message });
        });
    });
};

/**
 * Disable button → secureFetch → success/error toast → re-enable button.
 *
 * ✅ FIX #1: .finally() now restores the ORIGINAL button label, not
 *    the hard-coded string "Change Password".
 */
window.submitWithSecureFetch = function ({
    url,
    method        = 'POST',
    body,
    submitBtn,
    spinner,
    btnText,
    loadingText   = 'Processing…',
    successTitle  = 'Success!',
    successMessage= null,
    onSuccess     = null,
    onFinally     = null
} = {}) {

    // ✅ FIX #1: capture the original label BEFORE hsDisableButton overwrites it
    const originalLabel = btnText ? (btnText.dataset.originalText || btnText.textContent.trim()) : 'Save';

    hsDisableButton(submitBtn, spinner, btnText, loadingText);

    secureFetch(url, {
        method,
        body: body !== undefined ? JSON.stringify(body) : null
    })
    .then(data => {
        if (!data.success) throw new Error(data.message || 'Operation failed.');

        Swal.fire({
            icon:              'success',
            title:             successTitle,
            text:              successMessage || data.message,
            timer:             2000,
            showConfirmButton: false
        });

        if (typeof onSuccess === 'function') onSuccess(data);
    })
    .catch(err => {
        Swal.fire({ icon: 'error', title: 'Error!', text: err.message });
    })
    .finally(() => {
        if (submitBtn) submitBtn.disabled = false;
        if (spinner)   spinner.classList.add('d-none');
        // ✅ FIX #1: restore original label, NOT "Change Password"
        if (btnText)   btnText.textContent = originalLabel;

        if (typeof onFinally === 'function') onFinally();
    });
};


/* ═══════════════════════════════════════════════════════════════════════
   STATUS / DELETE HELPERS
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Confirm + POST to toggle status, then reload DataTable.
 * ✅ FIX #13: uses _reloadDT() instead of $(sel).DataTable().reload().
 */
function hsChangeStatus(url, tableSelector, entityName = 'Item') {
    Swal.fire({
        title:              `Change ${entityName} Status?`,
        text:               `This will toggle the ${entityName.toLowerCase()} status.`,
        icon:               'question',
        showCancelButton:   true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor:  '#d33',
        confirmButtonText:  'Yes, change it!',
        cancelButtonText:   'Cancel'
    }).then(result => {
        if (!result.isConfirmed) return;

        secureFetch(url, { method: 'POST' })
            .then(data => {
                if (data.success) {
                    Swal.fire({ icon: 'success', title: 'Status Changed!', text: data.message, timer: 2000, showConfirmButton: false });
                    _reloadDT(tableSelector);   // ✅ FIX #13
                } else {
                    Swal.fire({ icon: 'error', title: 'Error!', text: data.message });
                }
            })
            .catch(err => Swal.fire({ icon: 'error', title: 'Oops…', text: err.message }));
    });
}

/**
 * Confirm + DELETE, then reload DataTable.
 * ✅ FIX #13: uses _reloadDT().
 */
function hsConfirmDelete(url, tableSelector) {
    Swal.fire({
        title:              'Are you sure?',
        text:               'This action cannot be undone!',
        icon:               'warning',
        showCancelButton:   true,
        confirmButtonColor: '#d33',
        cancelButtonColor:  '#3085d6',
        confirmButtonText:  'Yes, delete it!',
        cancelButtonText:   'Cancel'
    }).then(result => {
        if (!result.isConfirmed) return;

        secureFetch(url, { method: 'DELETE' })
            .then(data => {
                Swal.fire({
                    icon:              data.success ? 'success' : 'error',
                    title:             data.success ? 'Deleted!'  : 'Error!',
                    text:              data.message,
                    timer:             data.success ? 2000 : undefined,
                    showConfirmButton: !data.success
                });
                if (data.success) _reloadDT(tableSelector);  // ✅ FIX #13
            })
            .catch(err => Swal.fire({ icon: 'error', title: 'Error!', text: err.message }));
    });
}


/* ═══════════════════════════════════════════════════════════════════════
   ACTION WITH REMARKS / APPROVAL DIALOG
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * SweetAlert2 dialog with optional remarks textarea → POST → reload DT.
 * ✅ FIX #4: references `dataTable` parameter (not undefined `dataTables`).
 */
function actionWithRemarks({
    title,
    confirmText,
    confirmColor      = '#3085d6',
    url,
    dataTable         = null,
    successMessage    = 'Action completed successfully.',
    textareaRequired  = false
} = {}) {

    Swal.fire({
        title,
        html: `
            <div class="mb-3 text-start">
                <label class="form-label fw-600 fs-13px">Remarks</label>
                <textarea id="actionRemarks" class="form-control" rows="3"
                    placeholder="Enter remarks${textareaRequired ? ' (required)' : ' (optional)'}"></textarea>
            </div>`,
        showCancelButton:   true,
        confirmButtonColor: confirmColor,
        confirmButtonText:  confirmText,
        cancelButtonText:   'Cancel',
        focusConfirm: false,
        preConfirm: () => {
            const remarks = document.getElementById('actionRemarks')?.value.trim() || '';
            if (textareaRequired && !remarks) {
                Swal.showValidationMessage('Remarks are required.');
                return false;
            }
            return { remarks };
        }
    }).then(result => {
        if (!result.isConfirmed) return;

        secureFetch(url, { method: 'POST', body: JSON.stringify(result.value) })
            .then(data => {
                if (!data.success) throw new Error(data.message || 'Action failed.');
                Swal.fire({ icon: 'success', title: 'Success!', text: data.message || successMessage, timer: 2000, showConfirmButton: false });
                _reloadDT(dataTable);   // ✅ FIX #4
            })
            .catch(err => Swal.fire({ icon: 'error', title: 'Error!', text: err.message }));
    });
}

/**
 * Approval workflow dialog: input + endpoint POST + reload DT.
 */
function approvalActionDialog({
    title,
    icon                = 'question',
    confirmText         = 'Submit',
    confirmColor        = '#3085d6',
    textareaPlaceholder = 'Enter remarks…',
    textareaRequired    = false,
    endpoint,
    successTitle        = 'Success!',
    tableToReload       = null
} = {}) {

    Swal.fire({
        title, icon,
        html: `<input id="actionRemarks" class="swal2-input"
                      placeholder="${textareaPlaceholder}" maxlength="250">`,
        showCancelButton:   true,
        confirmButtonText:  confirmText,
        confirmButtonColor: confirmColor,
        cancelButtonText:   'Cancel',
        focusConfirm: false,
        preConfirm: () => {
            const remarks = document.getElementById('actionRemarks')?.value.trim() || '';
            if (textareaRequired && !remarks) {
                Swal.showValidationMessage('Remarks are required.');
                return false;
            }
            return { remarks };
        }
    }).then(result => {
        if (!result.isConfirmed) return;

        Swal.fire({ title: 'Processing…', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        secureFetch(endpoint, {
            method:  'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body:    new URLSearchParams(result.value)
        })
        .then(data => {
            if (data.success) {
                Swal.fire({ icon: 'success', title: successTitle, text: data.message, timer: 2000, showConfirmButton: false });
                _reloadDT(tableToReload);
            } else {
                handleEnterpriseError(data);
            }
        })
        .catch(() => Swal.fire('Error', 'Unexpected server error.', 'error'));
    });
}

/** Show an error Swal from a server response object. */
function handleEnterpriseError(data) {
    Swal.fire({ icon: 'error', title: 'Error!', text: data?.message || 'An unexpected error occurred.' });
}


/* ═══════════════════════════════════════════════════════════════════════
   FORM AUTO-SUBMIT (hsInitAjaxForm)
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Wire a form to submit via secureFetch with Parsley validation.
 * ✅ FIX #12: dataTable is now a DataTable API instance (not a string selector).
 *
 * @param {string}     formId
 * @param {string}     submitBtnId
 * @param {string}     spinnerId
 * @param {string}     btnTextId
 * @param {string}     saveUrl
 * @param {*}          [dataTable=null]         — DT instance | selector | null
 * @param {Function}   [modifyDataCallback=null]
 */
function hsInitAjaxForm(formId, submitBtnId, spinnerId, btnTextId, saveUrl, dataTable = null, modifyDataCallback = null) {
    const form      = document.querySelector(formId);
    const submitBtn = document.querySelector(submitBtnId);
    const spinner   = document.querySelector(spinnerId);
    const btnText   = document.querySelector(btnTextId);

    if (!form) { console.warn(`[hsInitAjaxForm] Form "${formId}" not found.`); return; }

    if (typeof $ !== 'undefined' && $.fn.parsley) $(form).parsley();

    form.addEventListener('submit', function (e) {
        e.preventDefault();

        if (typeof $ !== 'undefined' && $.fn.parsley) {
            if (!$(form).parsley().validate()) return;
        }

        hsDisableButton(submitBtn, spinner, btnText);

        let payload = objectifyForm(form);
        if (typeof modifyDataCallback === 'function') {
            payload = modifyDataCallback(payload) || payload;
        }

        secureFetch(saveUrl, { method: 'POST', body: JSON.stringify(payload) })
            .then(data => {
                // ✅ FIX #12: pass the DT instance directly
                hsAfterSaveMessages(data.message, data.success, form, dataTable);
            })
            .catch(err => {
                hsAfterSaveMessages(err.message || 'Request failed.', false, null, null);
            })
            .finally(() => {
                hsEnableButton(submitBtn, spinner, btnText);
            });
    });
}


/* ═══════════════════════════════════════════════════════════════════════
   DROPDOWN LOADERS
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Load a <select> from an API or data array.
 * Expected API response: Array of { value, display }
 */
function asgLoadDropdown({ url = null, data = null, elementId, selectedValue = null }) {
    const populate = (items) => {
        const select = document.getElementById(elementId);
        if (!select) return;
        const firstOption = select.querySelector('option');
        select.innerHTML = '';
        if (firstOption) select.appendChild(firstOption);
        (items || []).forEach(item => {
            const opt       = document.createElement('option');
            opt.value       = item.value;
            opt.textContent = item.display;
            if (selectedValue !== null && String(item.value) === String(selectedValue)) opt.selected = true;
            select.appendChild(opt);
        });
    };

    if (data) { populate(data); return; }
    if (url) {
        secureFetch(url)
            .then(populate)
            .catch(err => console.error(`[asgLoadDropdown] ${elementId}:`, err));
    }
}

/**
 * Fetch options from a URL and populate a <select>.
 */
function haPopulateSelect(url, selectId, optionTextBuilder) {
    secureFetch(url)
        .then(data => {
            const select = document.getElementById(selectId);
            if (!select) return;
            select.innerHTML = '';
            data.forEach(item => {
                const opt = document.createElement('option');
                opt.value = item.id;
                opt.text  = optionTextBuilder(item);
                select.appendChild(opt);
            });
        })
        .catch(err => console.error('[haPopulateSelect]', err));
}


/* ═══════════════════════════════════════════════════════════════════════
   NUMERIC / DATE / CURRENCY UTILITIES
   ═══════════════════════════════════════════════════════════════════════ */

/** Parse to float; returns 0 for null/empty/NaN. */
function hsFloatConverter(value) {
    if (value === null || value === undefined || value === '' || value === 'null' || value === 'NULL') return 0;
    return parseFloat(value) || 0;
}

/** Return '' when value is null/undefined/empty/'null'. */
function nullCheck(value) {
    return (value == null || value === 'null' || value === undefined) ? '' : value;
}

/** Return true when value is null/undefined/empty/'null'. */
function isItNull(value) {
    return value == null || value === undefined || value === 'null' || value === '';
}

/**
 * Format a number as currency.
 * ✅ FIX #8: removed stray console.log() calls.
 */
function formatCurrency(value, currency = 'BDT') {
    if (value === null || value === undefined || value === '' || isNaN(value)) return '0.00';
    const num = parseFloat(String(value).replace(/,/g, ''));
    // ✅ FIX #8: removed console.log(value) and console.log(num)
    return new Intl.NumberFormat('en-BD', {
        style:                'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(num);
}

/** Day difference between two dd-MM-yyyy strings (requires moment.js). */
function dateDiffByPicker(first, second) {
    if (isItNull(first) || isItNull(second)) return '';
    return Math.round(
        (moment(second, 'DD-MM-YYYY') - moment(first, 'DD-MM-YYYY'))
        / (1000 * 60 * 60 * 24)
    );
}

/** Return today's date as dd-MM-yyyy. */
function asgSetDateToPicker() {
    const d  = new Date();
    const dd = ('0' + d.getDate()).slice(-2);
    const mm = ('0' + (d.getMonth() + 1)).slice(-2);
    return `${dd}-${mm}-${d.getFullYear()}`;
}


/* ═══════════════════════════════════════════════════════════════════════
   SELECT2 HELPERS
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * S2HSHelper — AJAX-backed Select2 with preselect and auto modal-parent.
 */
window.S2HSHelper = (function () {

    function init(selector, url, placeholder, preId, preText, extraParams = {}) {
        const $el = $(selector);
        if (!$el.length) return;

        if ($el.hasClass('select2-hidden-accessible')) $el.select2('destroy');

        $el.select2({
            dropdownParent:    $el.closest('.modal').length ? $el.closest('.modal') : $(document.body),
            width:             '100%',
            placeholder:       placeholder || 'Select option',
            allowClear:        true,
            minimumInputLength: 0,
            ajax: {
                url,
                dataType: 'json',
                delay:    250,
                data:  params => ({ search: params.term || '', page: params.page || 1, ...extraParams }),
                processResults: data => ({
                    results:    (data.items || []).map(i => ({ id: i.id, text: i.text })),
                    pagination: { more: !!data.hasMore }
                }),
                cache: true
            }
        });

        if (preId) {
            $el.append(new Option(preText || '', preId, true, true)).trigger('change');
        }
    }

    function clear(selector)           { $(selector).val(null).trigger('change'); }
    function setValue(selector, id, t) { $(selector).append(new Option(t || '', id, true, true)).trigger('change'); }
    function getValue(selector)        { return $(selector).val(); }

    return { init, clear, setValue, getValue };
})();

/**
 * Full-featured AJAX Select2 with code/caption display (legacy — prefer S2HSHelper).
 */
function selectTwoAjaxInitCall(select2FieldsId, actionUrl, placeholder, ajaxSearchParams = true, modalId, othersSearchParams) {
    $('#' + select2FieldsId).select2({
        dropdownParent:    ($('#' + modalId).length ? $('#' + modalId) : null),
        placeholder,
        allowClear:        true,
        width:             '100%',
        minimumInputLength: 0,
        ajax: {
            url:      actionUrl,
            dataType: 'json',
            delay:    250,
            data: params => ({
                q:                 params.term,
                page:              params.page,
                ajaxSearchParams,
                othersSearchParams: othersSearchParams ? othersSearchParams.val() : ''
            }),
            processResults: (data, params) => {
                params.page = params.page || 1;
                return {
                    results:    data.items,
                    pagination: { more: (params.page * 50) < data.total_count }
                };
            },
            cache: true
        },
        escapeMarkup:     markup => markup,
        templateResult:   repo => {
            if (repo.loading) return repo.text;
            return `<div class="widget-todolist-item"><div class="widget-todolist-content">
                <h6 class="mb-2px">${nullCheck(repo.code)} - ${nullCheck(repo.caption)}</h6>
                <div class="text-gray-600 fw-bold fs-11px">${nullCheck(repo.details)}</div>
            </div></div>`;
        },
        templateSelection: repo => {
            if (!repo.id) return placeholder;
            return repo.code ? `${repo.code} - ${repo.caption}` : repo.text;
        }
    });
}

function editSelect2Ajax(name, value, caption) {
    $('#' + name).empty()
        .append(`<option value="${value}" selected>${caption}</option>`)
        .trigger('change');
}

function hsOnChangeSetSelectTwoValue(actionUrl, paramsData, referenceSelect, actionOnlyNotNull = false, actionValue = '', actionType = 'POST') {
    if (actionOnlyNotNull && isItNull(actionValue)) {
        referenceSelect.select2('destroy').empty().select2({ width: '100%' });
        return;
    }
    $.ajax({
        type: actionType, dataType: 'json', async: false, data: paramsData, url: actionUrl,
        success: data => {
            referenceSelect.select2('destroy').empty();
            data.obj.forEach(item => {
                referenceSelect.append($('<option>').attr('value', item.key).text(item.value));
            });
            referenceSelect.select2({ dropdownParent: referenceSelect.parent() });
        },
        error: (xhr, s, e) => hsServerError(xhr, s, e)
    });
}

function selectTwoIdValues(selectedValue) {
    const data = selectedValue.select2('data');
    if (!isItNull(data) && data.length && !isItNull(data[0].id)) {
        return { id: data[0].id, value: data[0].text };
    }
    return { id: '', value: '' };
}


/* ═══════════════════════════════════════════════════════════════════════
   DETAIL TABLE HELPERS (hs_add_table_data etc.)
   ═══════════════════════════════════════════════════════════════════════ */

function hs_add_table_data(fields, properties, prefix, detailsName, tableId, footerEnable = false) {
    $('#' + tableId + ' > tbody').html('');
    let rowNumber = 1;
    $.each(properties, (key, value) => {
        let rf = '', row = `<tr><td>${++key}</td>`;
        value.sort_order = rowNumber;
        $.each(fields, (keys, values) => {
            const p = values.split('__');
            if (p[1].includes('multiply')) {
                const mp = p[1].split('*');
                value[p[0]] = hsFloatConverter(value[mp[1]]) * hsFloatConverter(value[mp[2]]);
            }
            rf  += `<input type="hidden" class="${keys}" name="${detailsName}[${rowNumber}].${keys}" value="${nullCheck(value[p[0]])}"/>`;
            if (p[1].includes('table')) row += `<td>${nullCheck(value[p[0]])}</td>`;
        });
        row += `<td ref_id="${rowNumber}">${rf}` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}EditEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square"></i></a>` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}DeleteEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular text-danger fa-trash-can"></i></a></td></tr>`;
        rowNumber++;
        $('#' + tableId + ' > tbody').prepend(row);
    });
    if (footerEnable) hs_add_table_dataFooter(fields, properties, prefix, detailsName, tableId);
}

function hs_divide_table_data(fields, properties, prefix, detailsName, tableId, footerEnable = false) {
    $('#' + tableId + ' > tbody').html('');
    let rowNumber = 1;
    $.each(properties, (key, value) => {
        let rf = '', row = `<tr><td>${++key}</td>`;
        value.sort_order = rowNumber;
        $.each(fields, (keys, values) => {
            const p = values.split('__');
            if (p[1].includes('multiply')) {
                const mp = p[1].split('*');
                value[p[0]] = hsFloatConverter(value[mp[1]]) * hsFloatConverter(value[mp[2]]);
            }
            rf  += `<input type="hidden" class="${keys}" name="${detailsName}[${rowNumber}].${keys}" value="${nullCheck(value[p[0]])}"/>`;
            if (p[1].includes('table')) row += `<td>${nullCheck(value[p[0]])}</td>`;
        });
        row += `<td ref_id="${rowNumber}">${rf}<a href="javascript:;" onclick="hs_${prefix}_${detailsName}DividedEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-solid fa-divide"></i></a></td></tr>`;
        rowNumber++;
        $('#' + tableId + ' > tbody').prepend(row);
    });
    if (footerEnable) hs_add_table_dataFooter(fields, properties, prefix, detailsName, tableId);
}

function hs_add_table_dataFooter(fields, properties, prefix, detailsName, tableId) {
    const totals = {};
    $.each(fields, (key, value) => {
        $.each(properties, (k, v) => {
            if (value.includes('table')) {
                totals[key] = value.includes('sumFooter')
                    ? hsFloatConverter(totals[key]) + hsFloatConverter(v[key])
                    : '-';
            }
        });
    });
    let row = '<tr><td>SUM</td>';
    $.each(totals, (k, v) => { row += `<td>${v}</td>`; });
    row += '<td>-</td></tr>';
    $('#' + tableId + ' > tbody').append(row);
}

function hs_add_table_inner_data(fields, properties, prefix, detailsName, tableId, extraAction) {
    $('#' + tableId + ' > tbody').html('');
    let rowNumber = 1;
    $.each(properties, (key, value) => {
        let rf = '', row = `<tr><td>${++key}</td>`;
        value.sort_order = rowNumber;
        $.each(fields, (keys, values) => {
            if (keys === 'dtlLine') {
                row += hsInnerTablesHtml(prefix, rowNumber, keys, value, values, detailsName);
            } else {
                const p = values.split('__');
                if (p[1].includes('multiply')) {
                    const mp = p[1].split('*');
                    value[p[0]] = hsFloatConverter(value[mp[1]]) * hsFloatConverter(value[mp[2]]);
                }
                rf += `<input type="hidden" class="${keys}" name="${detailsName}[${rowNumber}].${keys}" value="${nullCheck(value[p[0]])}"/>`;
                if (p[1].includes('table')) row += `<td>${nullCheck(value[p[0]])}</td>`;
            }
        });
        row += `<td ref_id="${rowNumber}">${rf}` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}AddEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular fa-square-plus text-success"></i></a>` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}EditEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square"></i></a>` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}DeleteEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular text-danger fa-trash-can"></i></a>`;
        $.each(extraAction, (k, v) => {
            if (v) row += `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}ExtraEvent${k}(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular text-success ${v}"></i></a>`;
        });
        row += '</td></tr>';
        rowNumber++;
        $('#' + tableId + ' > tbody').prepend(row);
    });
}

function hs_add_table_inner_data_new(fields, properties, prefix, detailsName, tableId, extraAction, detailCreateFalse, detailCloneTrue) {
    $('#' + tableId + ' > tbody').html('');
    let rowNumber = 1;
    $.each(properties, (key, value) => {
        let rf = `<input type="hidden" class="id" name="${detailsName}[${rowNumber}].id" value="${nullCheck(value.id)}"/>` +
                 `<input type="hidden" class="sortOrder" name="${detailsName}[${rowNumber}].sortOrder" value="${rowNumber}"/>`;
        let row = `<tr><td>${++key}</td>`;
        value.sort_order = rowNumber;
        $.each(fields, (keys, values) => {
            if (keys === 'dtlLine') {
                row += hsInnerTablesHtmlNew(rowNumber, prefix, detailsName, keys, value.dtlLine, values);
            } else {
                const p = values.split('__');
                if (p[1].includes('multiply')) {
                    const mp = p[1].split('*');
                    value[p[0]] = hsFloatConverter(value[mp[1]]) * hsFloatConverter(value[mp[2]]);
                }
                rf += `<input type="hidden" class="${keys}" name="${detailsName}[${rowNumber}].${keys}" value="${nullCheck(value[p[0]])}"/>`;
                if (p[1].includes('table')) {
                    row += p[1].includes('function')
                        ? `<td>(${keys}_${p[1].split('__')[1]})</td>`
                        : `<td>${nullCheck(value[p[0]])}</td>`;
                }
            }
        });
        row += `<td ref_id="${rowNumber}">${rf}` +
            (!detailCreateFalse && !isItNull(fields.dtlLine) ? `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}AddEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular fa-square-plus text-success"></i></a>` : '') +
            (!detailCreateFalse ? `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}EditEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square"></i></a>` : '') +
            (detailCloneTrue    ? `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}CloneEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular fa-clone"></i></a>` : '') +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}DeleteEvent(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular text-danger fa-trash-can"></i></a>`;
        $.each(extraAction, (k, v) => {
            if (v) row += `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}ExtraEvent${k}(this)" ref_id="${rowNumber}" class="btn btn-white btn-sm"><i class="fa-regular text-success ${v}"></i></a>`;
        });
        row += '</td></tr>';
        rowNumber++;
        $('#' + tableId + ' > tbody').prepend(row);
    });
}

function hsInnerTablesHtml(prefix, rowNumber, keys, value, values, detailsName) {
    let html = `<td><table class='table table-bordered table-striped table-hover' materTableRef='${rowNumber}'><thead><tr>`;
    $.each(values, (k, v) => { if (k !== 'id' && k !== 'sortOrder' && v.includes('table')) html += `<td>${v.split(':::')[1]}</td>`; });
    html += '<td>Action</td></tr></thead>';
    let ln = 1;
    $.each(value[keys], (ki, vi) => {
        html += '<tr>';
        let hid = '';
        vi.sort_order = ln;
        $.each(values, (k, v) => {
            hid += `<input type="hidden" class="${k}" name="${detailsName}[${rowNumber}].${keys}[${ln}].${k}" value="${nullCheck(vi[v.split('__')[0]])}"/>`;
            if (k !== 'id' && k !== 'sortOrder' && v.includes('table')) html += `<td>${nullCheck(vi[v.split('__')[0]])}</td>`;
        });
        html += `<td ref_id="${rowNumber}" ref_line_id="${ln}">${hid}` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}_${keys}EditEvent(this)" ref_id="${rowNumber}" ref_line_id="${ln}" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square"></i></a>` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}_${keys}DeleteEvent(this)" ref_id="${rowNumber}" ref_line_id="${ln}" class="btn btn-white btn-sm"><i class="fa-regular text-danger fa-trash-can"></i></a></td></tr>`;
        ln++;
    });
    return html + '</table></td>';
}

function hsInnerTablesHtmlNew(rowNumber, prefix, detailsName, detailsNameLine, values, properties) {
    let html = `<td><table class='table table-bordered table-striped table-hover' materTableRef='${rowNumber}'><thead><tr>`;
    let fileRow = '';
    $.each(properties, (k, v) => {
        if (k !== 'id' && k !== 'sortOrder' && v.includes('table')) {
            html += `<td>${nullCheck(v.split(':::')[1])}</td>`;
            if (v.includes('fileUploadShow') && v.includes('fileUploadInput')) fileRow = nullCheck(v.split(':::')[0]);
        }
    });
    html += '<td>Action</td></tr></thead>';
    let ln = 1;
    $.each(values, (ki, vi) => {
        html += '<tr>';
        let hid = `<input type="hidden" class="id" name="${detailsName}[${rowNumber}].${detailsNameLine}[${ln}].id" value="${nullCheck(vi.id)}"/>` +
                  `<input type="hidden" class="sortOrder" name="${detailsName}[${rowNumber}].${detailsNameLine}[${ln}].sortOrder" value="${ln}"/>`;
        vi.sort_order = ln;
        $.each(properties, (k, v) => {
            hid += `<input type="hidden" class="${k}" name="${detailsName}[${rowNumber}].${detailsNameLine}[${ln}].${k}" value="${nullCheck(vi[v.split('__')[0]])}"/>`;
            if (k !== 'id' && k !== 'sortOrder' && v.includes('table')) {
                if (v.includes('fileUploadShow')) {
                    html += v.includes('fileUploadInput')
                        ? `<td><input style="width:100px" type="file" class="${fileRow}" name="${detailsName}[${rowNumber}].${detailsNameLine}[${ln}].${k}"/> &nbsp;` +
                          (!isItNull(vi[v.split('__')[0]]) ? `<a href="javascript:;" onclick="${k}FileDownloadEvent(${nullCheck(vi.id)})"><i class="fa fa-cloud-download text-danger"></i></a>` : '') + '</td>'
                        : `<td><a href="javascript:;" onclick="${k}FileDownloadEvent(${nullCheck(vi.id)})"><i class="fa fa-cloud-download text-danger"></i></a></td>`;
                } else {
                    html += `<td>${nullCheck(vi[v.split('__')[0]])}</td>`;
                }
            }
        });
        html += `<td ref_id="${rowNumber}" ref_line_id="${ln}">${hid}` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}_${detailsNameLine}EditEvent(this)" ref_id="${rowNumber}" ref_line_id="${ln}" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square"></i></a>` +
            `<a href="javascript:;" onclick="hs_${prefix}_${detailsName}_${detailsNameLine}DeleteEvent(this)" ref_id="${rowNumber}" ref_line_id="${ln}" class="btn btn-white btn-sm"><i class="fa-regular text-danger fa-trash-can"></i></a></td></tr>`;
        ln++;
    });
    return html + '</table></td>';
}


/* ═══════════════════════════════════════════════════════════════════════
   IMPORT DATA
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * ✅ FIX #15: uses _reloadDT() instead of $(dataTable).DataTable().reload().
 */
function importData(label, endpoint, dataTable = null, payload = null) {
    Swal.fire({ title: `Importing ${label}…`, text: 'Please wait.', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

    secureFetch(endpoint, { method: 'POST', body: JSON.stringify(payload || { url: label.toLowerCase() }) })
        .then(data => {
            Swal.close();
            if (data.success) {
                Swal.fire({
                    icon: 'success', title: `${label} Import Completed`,
                    html: `<p>${data.message || ''}</p><p><b>Successful:</b> ${data.successCount || 0}</p><p><b>Failed:</b> ${data.failedCount || 0}</p>`,
                    timer: 5000
                });
                _reloadDT(dataTable);   // ✅ FIX #15
            } else {
                Swal.fire({ icon: 'error', title: `${label} Import Failed`, text: data.message || 'Unknown error.' });
            }
        })
        .catch(err => {
            Swal.close();
            Swal.fire({ icon: 'error', title: `${label} Import Error`, text: err.message });
        });
}


/* ═══════════════════════════════════════════════════════════════════════
   ActionHandler — generic CRUD action dispatcher              [window]
   ═══════════════════════════════════════════════════════════════════════ */
window.ActionHandler = (function () {
    let cfg = {};
    function init(userCfg) { cfg = userCfg; }
    function handle(id, action) {
        const actionCfg = cfg.actions?.[action];
        if (!actionCfg) { console.warn('[ActionHandler] Unknown action:', action); return; }
        const urlBase = `${cfg.baseUrl}/${id}`;
        if (actionCfg.type === 'fetch') {
            return secureFetch(urlBase).then(res => {
                if (!res.success) return Swal.fire('Error', res.message, 'error');
                if (actionCfg.modal === 'view') {
                    if (typeof populateDocView === 'function') populateDocView(res.data);
                    bootstrap.Modal.getOrCreateInstance(document.getElementById('defaultModalView')).show();
                }
                if (actionCfg.modal === 'edit') {
                    if (typeof populateDocForm === 'function') populateDocForm(res.data);
                    bootstrap.Modal.getOrCreateInstance(document.getElementById('defaultModal')).show();
                }
            });
        }
        if (actionCfg.type === 'delete') {
            return confirmAndExecute({ title: 'Delete?', text: 'This cannot be undone.', icon: 'warning', confirmText: 'Delete', confirmColor: '#ef4444', url: urlBase, method: 'DELETE', successTitle: 'Deleted!', reloadTable: true, dataTable: cfg.table });
        }
        if (actionCfg.type === 'dialog') {
            return approvalActionDialog({ title: actionCfg.title, icon: 'question', confirmText: actionCfg.confirmText, confirmColor: actionCfg.confirmColor, textareaPlaceholder: actionCfg.textareaPlaceholder, textareaRequired: actionCfg.textareaRequired, endpoint: `${urlBase}/${action}`, successTitle: actionCfg.successTitle, tableToReload: cfg.table });
        }
    }
    return { init, handle };
})();


/* ═══════════════════════════════════════════════════════════════════════
   LEGACY / COMPATIBILITY
   ═══════════════════════════════════════════════════════════════════════ */

/**
 * Legacy jQuery Ajax error handler.
 * ✅ FIX #9: 401 now redirects to /login?expired.
 */
function hsServerError(xhr, textStatus, errorThrown) {
    // ✅ FIX #9: was an empty if-block; now redirects
    if (xhr.status === 401) {
        window.location.href = '/login?expired';
        return;
    }
    let msg;
    switch (true) {
        case xhr.status === 0:              msg = 'Network error — check connection.'; break;
        case xhr.status === 404:            msg = 'Resource not found (404).'; break;
        case xhr.status === 500:            msg = 'Internal server error (500).'; break;
        case textStatus === 'parsererror':  msg = 'JSON parse error.'; break;
        case textStatus === 'timeout':      msg = 'Request timed out.'; break;
        case textStatus === 'abort':        msg = 'Request aborted.'; break;
        default:
            try { msg = JSON.parse(xhr.responseText).error || textStatus; }
            catch (_) { msg = textStatus; }
    }
    if (typeof $.gritter !== 'undefined') {
        $.gritter.add({ title: 'Server Error', text: msg, sticky: true, class_name: 'my-sticky-class growl-danger' });
    } else {
        toast('Server Error', msg, 'error');
    }
}

function hsNotification(hsData) {
    if (typeof $.gritter !== 'undefined') {
        $.gritter.add({ title: hsData.message, sticky: false, time: 5000, class_name: 'growl-success' });
    } else {
        toast(hsData.message, '', hsData.isError ? 'error' : 'success');
    }
}

function hsNotificationRemove() {
    if (typeof $.gritter !== 'undefined') $.gritter.removeAll();
}

function hsAfterDelete(hsData, rowRef) {
    hsNotification(hsData);
    if (!hsData.isError) $(rowRef).closest('tr').remove();
}

function hsAfterSave(hsData, listTable) {
    hsNotification(hsData);
    if (!hsData.isError) _reloadDT(listTable);
}

function saveSweetAlert(saveData, stayTime = 3000) {
    Swal.fire({ icon: saveData.isError ? 'error' : 'success', text: saveData.message, showConfirmButton: false, timer: stayTime });
}

function deleteSweetAlert(titleName, textMessage, iconName, buttonType, buttonText) {
    return Swal.fire({
        title: titleName, text: textMessage, icon: iconName,
        showCancelButton: true, confirmButtonText: buttonText, cancelButtonText: 'Cancel', confirmButtonColor: '#d33'
    }).then(result => result.isConfirmed);
}

/**
 * ✅ FIX #14: references undefined `dataTable` variable — now uses secureFetch
 *    and the caller passes the DT reference in their config object.
 */
function hsPostAction({ id, action, url, title, text, icon = 'question', confirmButtonColor = '#3085d6', confirmButtonText = 'Yes, proceed!', cancelButtonText = 'Cancel', successTitle = 'Success!', successIcon = 'success', postDataKey = 'actionBy', user = 'CurrentUser', reloadTable = false, dataTable = null }) {
    Swal.fire({ title, text, icon, showCancelButton: true, confirmButtonColor, cancelButtonColor: '#d33', confirmButtonText, cancelButtonText })
        .then(result => {
            if (!result.isConfirmed) return;
            secureFetch(`${url}/${id}`, {
                method:  'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body:    `${postDataKey}=${encodeURIComponent(user)}`
            })
            .then(data => {
                if (data.success) {
                    Swal.fire({ icon: successIcon, title: successTitle, text: data.message, timer: 2000, showConfirmButton: false });
                    if (reloadTable) _reloadDT(dataTable);  // ✅ FIX #14
                } else {
                    Swal.fire({ icon: 'error', title: 'Error!', text: data.message });
                }
            })
            .catch(err => Swal.fire({ icon: 'error', title: 'Error!', text: err.message }));
        });
}

/** Legacy sync AJAX (avoid in new code — use secureFetch). */
function ajaxRequest(actions, parameters) {
    let result;
    $.ajax({ type: 'POST', dataType: 'json', async: false, data: parameters, url: actions,
             success: data => { result = data; },
             error:   (xhr, s, e) => hsServerError(xhr, s, e) });
    return result;
}

function beforeFormSubmit(button) { /* reserved */ }
function afterFormSubmit(button)  { /* reserved */ }
