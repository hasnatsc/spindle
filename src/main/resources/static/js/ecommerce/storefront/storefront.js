// Path: src/main/resources/static/js/storefront.js
// Shared storefront behavior: cart count badge, add-to-cart, toast, qty steppers.
(function () {
  'use strict';

  function csrfHeaders() {
    var token = document.querySelector('meta[name="_csrf"]')?.content;
    var header = document.querySelector('meta[name="_csrf_header"]')?.content;
    var h = { 'Content-Type': 'application/json' };
    if (token && header) h[header] = token;
    return h;
  }

  window.sfFetch = function (url, opts) {
    opts = opts || {};
    opts.headers = Object.assign({}, csrfHeaders(), opts.headers || {});
    opts.credentials = 'same-origin';
    return fetch(url, opts).then(function (r) {
      if (!r.ok && r.status === 401) { window.location.href = '/account/login'; return Promise.reject('auth'); }
      return r.json();
    });
  };

  window.sfToast = function (message, isError) {
    var el = document.getElementById('sfToast');
    if (!el) return;
    el.textContent = message;
    el.className = 'sf-toast show' + (isError ? ' error' : '');
    clearTimeout(el._t);
    el._t = setTimeout(function () { el.classList.remove('show'); }, 2600);
  };

  function refreshCartCount() {
    var badge = document.getElementById('sfCartCount');
    if (!badge) return;
    sfFetch('/cart/count').then(function (d) {
      badge.textContent = d.count || 0;
      badge.style.display = d.count > 0 ? 'flex' : 'none';
    }).catch(function () {});
  }

  window.sfAddToCart = function (productId, variantId, quantity, btn) {
    if (btn) { btn.disabled = true; }
    sfFetch('/cart/add', {
      method: 'POST',
      body: JSON.stringify({ productId: productId, variantId: variantId || null, quantity: quantity || 1 })
    }).then(function (d) {
      if (d.success) {
        sfToast(d.message || 'Added to cart.');
        refreshCartCount();
      } else {
        sfToast(d.message || 'Could not add to cart.', true);
      }
    }).catch(function () { sfToast('Something went wrong.', true); })
      .finally(function () { if (btn) btn.disabled = false; });
  };

  window.sfQtyStepper = function (containerEl, onChange) {
    var input = containerEl.querySelector('input');
    var minus = containerEl.querySelector('.sf-qty-minus');
    var plus = containerEl.querySelector('.sf-qty-plus');
    minus.addEventListener('click', function () {
      var v = Math.max(1, parseInt(input.value || '1') - 1);
      input.value = v;
      if (onChange) onChange(v);
    });
    plus.addEventListener('click', function () {
      var v = parseInt(input.value || '1') + 1;
      input.value = v;
      if (onChange) onChange(v);
    });
    input.addEventListener('change', function () {
      var v = Math.max(1, parseInt(input.value || '1'));
      input.value = v;
      if (onChange) onChange(v);
    });
  };

  document.addEventListener('DOMContentLoaded', function () {
    refreshCartCount();

    // Generic accordion toggle (product detail page)
    document.querySelectorAll('.sf-accordion-head').forEach(function (head) {
      head.addEventListener('click', function () {
        head.closest('.sf-accordion-item').classList.toggle('open');
      });
    });

    // Generic [data-sf-add-to-cart] buttons (product cards quick-add)
    document.querySelectorAll('[data-sf-add-to-cart]').forEach(function (btn) {
      btn.addEventListener('click', function (e) {
        e.preventDefault();
        sfAddToCart(btn.dataset.productId, btn.dataset.variantId || null, 1, btn);
      });
    });
  });
})();
