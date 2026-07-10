/* =============================================================================
   storefront.js v3 — Color Admin theme edition.
   Global helpers shared by every storefront page (loaded in sf-base.html after
   the theme's vendor.min.js + app.min.js).

   Preserved v2 contract:
     sfFetch(url, opts)          CSRF-aware fetch → parsed JSON
     sfToast(msg, isError)       #sfToast bottom-centre toast
     sfUpdateCartBadge(n)        #sfCartCount header badge
     sfAddToCart(pId,vId,qty,btn)
     sfQtyStepper(input, delta)
     delegated [data-sf-add-to-cart] and [data-sf-wishlist] handlers
     #sfNewsletterForm submit → POST /newsletter/subscribe

   New in v3:
     Header cart dropdown lazy-loads GET /cart/view on first open and renders
     the theme's cart-item list (image / info / × remove) live.
   ============================================================================= */
(function () {
  'use strict';

  // ── core fetch ─────────────────────────────────────────────────────────────
  window.sfFetch = function (url, opts) {
    opts = opts || {};
    opts.credentials = 'same-origin';
    opts.headers = opts.headers || {};
    if (!opts.headers['Content-Type'] && opts.body) opts.headers['Content-Type'] = 'application/json';
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header && token.content && header.content) opts.headers[header.content] = token.content;
    return fetch(url, opts).then(function (r) { return r.json(); });
  };

  // ── toast ──────────────────────────────────────────────────────────────────
  var sfToastTimer = null;
  window.sfToast = function (msg, isError) {
    var el = document.getElementById('sfToast');
    if (!el) return;
    el.textContent = msg || '';
    el.classList.toggle('error', !!isError);
    el.classList.add('show');
    clearTimeout(sfToastTimer);
    sfToastTimer = setTimeout(function () { el.classList.remove('show'); }, 3200);
  };

  // ── header cart badge ──────────────────────────────────────────────────────
  window.sfUpdateCartBadge = function (count) {
    var badge = document.getElementById('sfCartCount');
    if (badge) badge.textContent = count > 99 ? '99+' : String(count);
  };

  // ── add to cart ────────────────────────────────────────────────────────────
  window.sfAddToCart = function (productId, variantId, quantity, btn) {
    if (btn) btn.disabled = true;
    return sfFetch('/cart/add', {
      method: 'POST',
      body: JSON.stringify({
        productId: productId,
        variantId: variantId || null,
        quantity: quantity || 1
      })
    }).then(function (d) {
      if (btn) btn.disabled = false;
      if (d.success) {
        sfToast(d.message || 'Added to cart.');
        if (d.cartCount != null) sfUpdateCartBadge(d.cartCount);
        sfCartDdLoaded = false; // dropdown re-fetches next open
      } else if (d.login) {
        window.location.href = '/account/login?redirect=' +
          encodeURIComponent(window.location.pathname + window.location.search);
      } else {
        sfToast(d.message || 'Could not add to cart.', true);
      }
      return d;
    }).catch(function () {
      if (btn) btn.disabled = false;
      sfToast('Could not add to cart.', true);
    });
  };

  // ── qty stepper (PDP + generic) ────────────────────────────────────────────
  window.sfQtyStepper = function (input, delta) {
    if (!input) return;
    input.value = Math.max(1, (parseInt(input.value, 10) || 1) + delta);
  };

  // ── header cart dropdown (theme dropdown-menu-cart, lazy-loaded) ───────────
  var sfCartDdLoaded = false;

  function sfEsc(parent, tag, className, text) {
    var el = document.createElement(tag);
    if (className) el.className = className;
    if (text != null) el.textContent = text;   // textContent — XSS-safe
    parent.appendChild(el);
    return el;
  }

  function sfRenderCartDropdown(cart) {
    var list = document.getElementById('sfCartDdItems');
    var title = document.getElementById('sfCartDdTitle');
    if (!list) return;
    list.innerHTML = '';

    var items = (cart && cart.items) || [];
    if (title) title.textContent = 'Shopping Bag (' + items.length + ')';
    if (cart && cart.totalItems != null) sfUpdateCartBadge(cart.totalItems);

    if (items.length === 0) {
      var li = document.createElement('li');
      var info = sfEsc(li, 'div', 'cart-item-info');
      sfEsc(info, 'h4', 'text-muted', 'Your bag is empty.');
      list.appendChild(li);
      return;
    }

    items.forEach(function (item) {
      var li = document.createElement('li');

      var imgWrap = sfEsc(li, 'div', 'cart-item-image');
      var img = document.createElement('img');
      img.src = item.productImage || 'https://placehold.co/80x100/f4f4f4/999999?text=ASG';
      img.alt = '';
      imgWrap.appendChild(img);

      var info = sfEsc(li, 'div', 'cart-item-info');
      sfEsc(info, 'h4', null, item.productTitle || 'Product');
      sfEsc(info, 'p', 'price',
        (item.quantity ? Math.round(item.quantity) + ' × ' : '') +
        '৳' + Math.round(item.unitPrice || 0).toLocaleString());

      var close = sfEsc(li, 'div', 'cart-item-close');
      var x = document.createElement('a');
      x.href = 'javascript:;';
      x.innerHTML = '&times;';
      x.addEventListener('click', function () {
        sfFetch('/cart/remove/' + item.id, { method: 'DELETE' }).then(function (d) {
          if (d.success) {
            sfRenderCartDropdown(d.cart);
            sfToast('Removed from cart.');
            // if the full cart page is open, keep it honest
            if (document.getElementById('checkout-cart')) location.reload();
          }
        });
      });
      close.appendChild(x);

      list.appendChild(li);
    });
  }

  function sfLoadCartDropdown() {
    if (sfCartDdLoaded) return;
    sfCartDdLoaded = true;
    sfFetch('/cart/view', { method: 'GET' })
      .then(function (d) { sfRenderCartDropdown(d.cart || d); })
      .catch(function () { sfCartDdLoaded = false; });
  }

  // ── boot ───────────────────────────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', function () {

    // badge count on every page
    sfFetch('/cart/count', { method: 'GET' })
      .then(function (d) { if (d && d.count != null) sfUpdateCartBadge(d.count); })
      .catch(function () { /* silent */ });

    // cart dropdown lazy-load on first hover/click
    var toggle = document.getElementById('sfCartDropdownToggle');
    if (toggle) {
      toggle.addEventListener('mouseenter', sfLoadCartDropdown);
      toggle.addEventListener('click', sfLoadCartDropdown);
    }

    // delegated: quick add-to-cart on product cards
    document.body.addEventListener('click', function (e) {
      var addBtn = e.target.closest('[data-sf-add-to-cart]');
      if (addBtn) {
        e.preventDefault();
        sfAddToCart(parseInt(addBtn.dataset.productId, 10), null, 1, addBtn);
        return;
      }

      // delegated: wishlist heart toggle
      var wishBtn = e.target.closest('[data-sf-wishlist]');
      if (wishBtn) {
        e.preventDefault();
        sfFetch('/wishlist/toggle', {
          method: 'POST',
          body: JSON.stringify({ productId: parseInt(wishBtn.dataset.productId, 10) })
        }).then(function (d) {
          if (d.login) {
            window.location.href = '/account/login?redirect=' +
              encodeURIComponent(window.location.pathname + window.location.search);
            return;
          }
          if (!d.success) { sfToast(d.message || 'Could not update wishlist.', true); return; }

          var icon = wishBtn.querySelector('i');
          if (d.added) {
            wishBtn.classList.add('active');
            if (icon) { icon.classList.remove('far'); icon.classList.add('fas'); }
            sfToast('Saved to wishlist.');
          } else {
            wishBtn.classList.remove('active');
            if (icon) { icon.classList.remove('fas'); icon.classList.add('far'); }
            sfToast('Removed from wishlist.');

            // on the wishlist page, remove the card in place
            var grid = document.getElementById('sfWishlistGrid');
            var col = wishBtn.closest('.sf-wish-col');
            if (grid && col) {
              col.remove();
              if (!grid.querySelector('.sf-wish-col')) location.reload();
            }
          }
        });
      }
    });

    // newsletter form (base layout #subscribe)
    var nlForm = document.getElementById('sfNewsletterForm');
    if (nlForm) {
      nlForm.addEventListener('submit', function (e) {
        e.preventDefault();
        var emailInput = document.getElementById('sfNewsletterEmail');
        var email = (emailInput.value || '').trim();
        if (!email) { sfToast('Please enter your email.', true); return; }
        sfFetch('/newsletter/subscribe', { method: 'POST', body: JSON.stringify({ email: email }) })
          .then(function (d) {
            sfToast(d.message || (d.success ? 'Subscribed!' : 'Could not subscribe.'), !d.success);
            if (d.success) emailInput.value = '';
          })
          .catch(function () { sfToast('Could not subscribe.', true); });
      });
    }
  });
})();
