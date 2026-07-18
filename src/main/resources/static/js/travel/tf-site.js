/* =============================================================================
   tf-site.js — ASG Travel public site helpers (enquiry submission + toast).
   ============================================================================= */
(function () {
  'use strict';

  function csrfHeaders() {
    var h = { 'Content-Type': 'application/json' };
    var t = document.querySelector('meta[name="_csrf"]');
    var hn = document.querySelector('meta[name="_csrf_header"]');
    if (t && hn && t.content && hn.content) h[hn.content] = t.content;
    return h;
  }

  var toastTimer = null;
  function tfToast(msg, isError) {
    var el = document.getElementById('tfToast');
    if (!el) return;
    el.textContent = msg || '';
    el.classList.toggle('error', !!isError);
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove('show'); }, 3500);
  }

  // Bind enquiry button immediately — script is at bottom of <body>, DOM is ready.
  // (Do NOT use DOMContentLoaded — if that event fires before this script loads,
  //  the listener never runs and the button does nothing.)
  (function bindEnquiryBtn() {
    var btn = document.getElementById('tfEnquireBtn');
    if (btn) {
      btn.addEventListener('click', function () {
        var type = btn.getAttribute('data-type');
        var refId = btn.getAttribute('data-ref-id');
        if (type && refId) window.tfSubmitEnquiry(type, Number(refId));
      });
    }
  })();

  window.tfSubmitEnquiry = function (type, refId) {
    var msgBox = document.getElementById('tfEnquiryMsg');
    msgBox.innerHTML = '';

    var pax = parseInt(document.getElementById('tfPax').value, 10) || 1;
    var name = document.getElementById('tfName').value.trim();
    var phone = document.getElementById('tfPhone').value.trim();
    var email = document.getElementById('tfEmail').value.trim();
    var message = document.getElementById('tfMessage').value.trim();

    if (!name || !phone) {
      msgBox.innerHTML = '<div style="color:var(--tf-coral);font-size:.85rem">Please enter your name and phone number.</div>';
      return;
    }

    var btn = document.getElementById('tfEnquireBtn');
    btn.disabled = true;
    var originalText = btn.textContent;
    btn.textContent = 'Sending…';

    fetch('/travel-site/enquire', {
      method: 'POST',
      credentials: 'same-origin',
      headers: csrfHeaders(),
      body: JSON.stringify({
        type: type,
        referenceId: refId,
        paxCount: pax,
        fullName: name,
        phone: phone,
        email: email,
        message: message
      })
    })
      .then(function (r) { return r.json(); })
      .then(function (d) {
        btn.disabled = false;
        btn.textContent = originalText;
        if (d.success) {
          window.location.href = '/travel-site/enquiry/success/' + encodeURIComponent(d.bookingNo);
        } else {
          msgBox.innerHTML = '<div style="color:var(--tf-coral);font-size:.85rem">' + (d.message || 'Could not submit enquiry.') + '</div>';
          tfToast(d.message || 'Could not submit enquiry.', true);
        }
      })
      .catch(function () {
        btn.disabled = false;
        btn.textContent = originalText;
        msgBox.innerHTML = '<div style="color:var(--tf-coral);font-size:.85rem">Something went wrong. Please try again.</div>';
      });
  };

  window.tfToast = tfToast;
})();
