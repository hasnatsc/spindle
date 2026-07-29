// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontCartService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import com.asg.spindleserp.ecommerce.EcInventoryService;
import com.asg.spindleserp.ecommerce.cart.entity.EcCart;
import com.asg.spindleserp.ecommerce.cart.entity.EcCartItem;
import com.asg.spindleserp.ecommerce.cart.repository.EcCartRepository;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductVariant;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcProductCatalogRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfCartDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * StorefrontCartService — EcCart for guests (session-keyed) and customers
 * (customer-keyed). On login the guest cart is merged into the customer's.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [HIGH] FIX 1 — CROSS-TENANT IDOR IN addItem()                         ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 * The old add-to-cart looked the product up by primary key and nothing else:
 *
 *     EcProductCatalog product = productRepository.findById(productId)
 *             .orElseThrow(() -> new IllegalArgumentException("Product not found."));
 *
 * POST /cart/add is permitAll (guests must be able to build a cart) and its body
 * is {"productId": N}. There was no organisation check, no published check, no
 * active check and no deleted check. So ANY anonymous visitor could:
 *
 *   1. Enumerate ids 1..N and add ANOTHER TENANT'S products to their cart. The
 *      response echoes the full cart back — product_title, slug, image URL,
 *      unit_price — so this is a complete dump of every other organisation's
 *      catalogue AND their pricing, from an unauthenticated endpoint, in a
 *      multi-tenant ERP. That is the single worst thing that can go wrong in a
 *      multi-tenant system.
 *   2. Add an UNPUBLISHED product — a draft, an embargoed launch, a
 *      deliberately-hidden SKU — and then check out and buy it.
 *   3. Add a SOFT-DELETED or INACTIVE product.
 *
 * The catalogue queries all enforce the org filter and
 * "published = true AND active = true AND deleted = false". The cart enforced
 * none of it — it trusted that a product id could only have come from a page
 * that had already applied those filters. The listing page is a UI, not an
 * access control, and an attacker does not use the UI.
 *
 * FIX: findByIdAndOrganizationIdAndPublishedTrueAndActiveTrueAndDeletedFalse().
 * If a product is not visible in the catalogue, it cannot enter a cart.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] FIX 2 — variantId was silently ignored when it did not match ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *     .variant(variantId != null ? findVariant(product, variantId) : null)
 *
 * findVariant() returns null when the id does not belong to the product. So
 * passing a variantId from a DIFFERENT product (or a nonexistent one) did not
 * fail — it quietly stored variant = null, and resolveSellingPrice() then fell
 * back to the BASE product price. If a variant is priced above the base item
 * (a larger size, a premium colourway — the normal reason variants exist), the
 * customer selects it, is charged the base price, and the order ships the
 * expensive variant. Now: an unknown variant is rejected outright.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] FIX 3 — quantity was completely unbounded                    ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *     BigDecimal qty = new BigDecimal(body.getOrDefault("quantity", "1").toString());
 *
 * Straight from the request body into a BigDecimal, with only a "<= 0 → 1"
 * clamp in addItem() and NO clamp at all in updateQuantity(). Consequences:
 *
 *   • "1E+40" is a perfectly valid BigDecimal. line_total = unit_price × 1e40
 *     overflows numeric(18,2) → the INSERT dies with a raw Postgres numeric
 *     overflow, whose message the controller then echoes to the browser.
 *   • Fractional quantities ("0.5") were accepted, then counted with
 *     .intValue() → 0. So the cart shows "0 items" while holding a priced line.
 *   • A guest could park 2,000,000,000 units of a product in a cart with a
 *     single request — an unauthenticated write amplification against
 *     ec_cart_items and, once stock enforcement is wired up, a free inventory
 *     denial-of-service.
 *
 * FIX: normalizeQuantity() — integer scale, 1 ≤ qty ≤ 999, rejected (not
 * silently coerced) when it is nonsense.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] FIX 4 — mergeGuestCartOnLogin() merged blindly                ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 * The merge copied every guest-cart line into the customer's cart without
 * re-checking any of it. A guest cart built before this deployment can contain
 * exactly the cross-tenant / unpublished lines that FIX 1 now prevents — and
 * the merge would happily launder them into an authenticated customer's cart
 * and on into checkout. Lines whose product is no longer visible to this
 * customer's organisation are now dropped at merge time, with a log line.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * PRESERVED — everything the v2/v3 versions got right
 * ══════════════════════════════════════════════════════════════════════════
 *   ✅ Price is ALWAYS resolved server-side from the product/variant. The client
 *      never supplies a price, and never could — this was already correct and is
 *      the single most important thing a cart can get right.
 *   ✅ peekCart() — read-only lookup that never creates a cart row, so anonymous
 *      crawlers no longer spawn one ec_carts row per visit. (v3 optimisation.)
 *   ✅ viewCart() / cartItemCount() use peekCart, not getOrCreateCart.
 *   ✅ Cart-item ownership: updateQuantity() and removeItem() resolve the item id
 *      INSIDE the caller's own cart (cart.getItems().stream()…), so one shopper
 *      can never touch another's line. This was already IDOR-safe.
 *   ✅ setCouponDiscount() / markOrdered() seams for StorefrontCheckoutService.
 *   ✅ The dead StockLedgerService.balanceByItem() call stays removed, and the
 *      ★ stock-blocking seam remains documented rather than silently dropped.
 *   ✅ Every public method signature is unchanged. Drop-in.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontCartService {

    private static final String SESSION_KEY = "SF_CART_SESSION_ID";

    private static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("2000");
    private static final BigDecimal DEFAULT_SHIPPING     = new BigDecimal("80");

    /** Per-line ceiling. Above this it is not a shopper, it is an attack or a typo. */
    private static final BigDecimal MAX_LINE_QUANTITY = new BigDecimal("999");

    private final EcCartRepository              cartRepository;
    private final EcProductCatalogRepository     productRepository;
    private final EcInventoryService             inventoryService;

    // ══════════════════════════════════════════════════════════════════════
    // GET / PEEK
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public EcCart getOrCreateCart(HttpServletRequest request, EcCustomer customer) {
        if (customer != null) {
            return cartRepository.findByCustomerIdAndCartStatus(customer.getId(), EcCart.CartStatus.ACTIVE)
                    .orElseGet(() -> cartRepository.save(EcCart.builder()
                            .customer(customer)
                            .cartStatus(EcCart.CartStatus.ACTIVE)
                            .build()));
        }
        String sid = getOrCreateSessionId(request);
        return cartRepository.findBySessionIdAndCartStatus(sid, EcCart.CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(EcCart.builder()
                        .sessionId(sid)
                        .cartStatus(EcCart.CartStatus.ACTIVE)
                        .build()));
    }

    /** Read-only cart lookup — never creates rows or sessions. (v3, retained.) */
    @Transactional(readOnly = true)
    public Optional<EcCart> peekCart(HttpServletRequest request, EcCustomer customer) {
        if (customer != null)
            return cartRepository.findByCustomerIdAndCartStatus(customer.getId(), EcCart.CartStatus.ACTIVE);

        HttpSession session = request.getSession(false);
        String sid = session != null ? (String) session.getAttribute(SESSION_KEY) : null;
        if (sid == null) return Optional.empty();
        return cartRepository.findBySessionIdAndCartStatus(sid, EcCart.CartStatus.ACTIVE);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADD ITEM
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public SfCartDTO addItem(HttpServletRequest request, EcCustomer customer,
                             Long productId, Long variantId, BigDecimal quantity) {

        BigDecimal qty = normalizeQuantity(quantity, BigDecimal.ONE);   // ✅ FIX 3

        // ✅ FIX 1 — org + published + active + not-deleted. NOT findById().
        EcProductCatalog product = loadStorefrontProduct(productId);

        // ✅ FIX 2 — an unknown/foreign variant is an error, not a silent null.
        EcProductVariant variant = resolveVariant(product, variantId);

        BigDecimal unitPrice = resolveSellingPrice(product, variant);
        BigDecimal available = resolveAvailableStock(product, variant);

        EcCart cart = getOrCreateCart(request, customer);

        Optional<EcCartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .filter(i -> sameVariant(i.getVariant(), variant))
                .findFirst();

        BigDecimal targetQty = existing.map(i -> i.getQuantity().add(qty)).orElse(qty);

        if (targetQty.compareTo(MAX_LINE_QUANTITY) > 0)
            throw new IllegalArgumentException(
                    "You can order at most " + MAX_LINE_QUANTITY.toPlainString() + " of this item.");

        if (available != null && targetQty.compareTo(available) > 0)
            throw new IllegalArgumentException(
                    "Only " + available.stripTrailingZeros().toPlainString() + " left in stock.");

        if (existing.isPresent()) {
            EcCartItem item = existing.get();
            item.setQuantity(targetQty);
            item.setUnitPrice(unitPrice);                       // re-price on every touch
            item.setLineTotal(unitPrice.multiply(targetQty));
        } else {
            cart.getItems().add(EcCartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .lineTotal(unitPrice.multiply(qty))
                    .build());
        }

        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    // ══════════════════════════════════════════════════════════════════════
    // UPDATE / REMOVE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * cartItemId is resolved INSIDE the caller's own cart, so one shopper can
     * never address another shopper's line. That was already correct and is
     * unchanged — only the quantity handling is hardened.
     */
    @Transactional
    public SfCartDTO updateQuantity(HttpServletRequest request, EcCustomer customer,
                                    Long cartItemId, BigDecimal newQty) {

        EcCart cart = getOrCreateCart(request, customer);
        EcCartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found."));

        // A zero/negative quantity is the UI's "remove" gesture — keep that.
        if (newQty == null || newQty.compareTo(BigDecimal.ZERO) <= 0) {
            cart.getItems().remove(item);
            recalculate(cart);
            return toDTO(cartRepository.save(cart));
        }

        BigDecimal qty = normalizeQuantity(newQty, null);            // ✅ FIX 3

        BigDecimal available = resolveAvailableStock(item.getProduct(), item.getVariant());
        if (available != null && qty.compareTo(available) > 0)
            throw new IllegalArgumentException(
                    "Only " + available.stripTrailingZeros().toPlainString() + " left in stock.");

        item.setQuantity(qty);
        item.setLineTotal(item.getUnitPrice().multiply(qty));

        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    @Transactional
    public SfCartDTO removeItem(HttpServletRequest request, EcCustomer customer, Long cartItemId) {
        EcCart cart = getOrCreateCart(request, customer);
        cart.getItems().removeIf(i -> i.getId().equals(cartItemId));
        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    // ══════════════════════════════════════════════════════════════════════
    // VIEW
    // ══════════════════════════════════════════════════════════════════════

    /**
     * v3 OPTIMISATION (retained) — read-only. Previously this called
     * getOrCreateCart(), so every anonymous visitor who opened /cart, or merely
     * hovered the header bag icon (which lazy-fetches /cart/view), spawned an
     * ec_carts row AND an HTTP session. Now it peeks; no cart → a transient
     * empty DTO, zero writes. Carts are created on the first real /cart/add.
     */
    @Transactional(readOnly = true)
    public SfCartDTO viewCart(HttpServletRequest request, EcCustomer customer) {
        return peekCart(request, customer).map(this::toDTO).orElseGet(StorefrontCartService::emptyCartDTO);
    }

    @Transactional(readOnly = true)
    public int cartItemCount(HttpServletRequest request, EcCustomer customer) {
        return peekCart(request, customer)
                .map(c -> c.getItems().stream().mapToInt(i -> i.getQuantity().intValue()).sum())
                .orElse(0);
    }

    private static SfCartDTO emptyCartDTO() {
        return SfCartDTO.builder()
                .totalItems(0)
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .couponDiscount(BigDecimal.ZERO)
                .shippingCharge(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // COUPON / ORDER SEAMS (called by StorefrontCheckoutService)
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public SfCartDTO setCouponDiscount(HttpServletRequest request, EcCustomer customer, BigDecimal discount) {
        EcCart cart = getOrCreateCart(request, customer);
        cart.setCouponDiscount(discount != null ? discount.max(BigDecimal.ZERO) : BigDecimal.ZERO);
        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    /** Flip the cart to ORDERED after successful order placement. */
    @Transactional
    public void markOrdered(EcCart cart) {
        cart.setCartStatus(EcCart.CartStatus.ORDERED);
        cartRepository.save(cart);
    }

    // ══════════════════════════════════════════════════════════════════════
    // MERGE GUEST CART ON LOGIN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIX 4 — every guest line is RE-VALIDATED against the customer's own
     * organisation before it is merged.
     *
     * A guest cart is client-influenced state that has been sitting in the DB.
     * Merging it blindly into an authenticated customer's cart would launder any
     * line that should not exist — including exactly the cross-tenant and
     * unpublished lines that FIX 1 now blocks at the door, but which may already
     * be sitting in guest carts created before this deployment.
     */
    @Transactional
    public void mergeGuestCartOnLogin(HttpServletRequest request, EcCustomer customer) {
        HttpSession session = request.getSession(false);
        if (session == null) return;

        String sid = (String) session.getAttribute(SESSION_KEY);
        if (sid == null) return;

        Optional<EcCart> guestCartOpt =
                cartRepository.findBySessionIdAndCartStatus(sid, EcCart.CartStatus.ACTIVE);
        if (guestCartOpt.isEmpty()) return;

        EcCart guestCart = guestCartOpt.get();
        if (guestCart.getItems().isEmpty()) {
            cartRepository.delete(guestCart);
            session.removeAttribute(SESSION_KEY);
            return;
        }

        EcCart customerCart = cartRepository
                .findByCustomerIdAndCartStatus(customer.getId(), EcCart.CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(EcCart.builder()
                        .customer(customer)
                        .cartStatus(EcCart.CartStatus.ACTIVE)
                        .build()));

        Long orgId = customer.getOrganizationId();
        int dropped = 0;

        for (EcCartItem guestItem : guestCart.getItems()) {

            // ✅ Re-validate: is this product still visible to THIS customer's org?
            boolean visible = guestItem.getProduct() != null
                    && productRepository
                        .findByIdAndOrganizationIdAndPublishedTrueAndActiveTrueAndDeletedFalse(
                                guestItem.getProduct().getId(), orgId)
                        .isPresent();

            if (!visible) {
                dropped++;
                continue;
            }

            Optional<EcCartItem> match = customerCart.getItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(guestItem.getProduct().getId()))
                    .filter(ci -> sameVariant(ci.getVariant(), guestItem.getVariant()))
                    .findFirst();

            if (match.isPresent()) {
                EcCartItem item = match.get();
                BigDecimal newQty = item.getQuantity().add(guestItem.getQuantity())
                        .min(MAX_LINE_QUANTITY);                       // ✅ cap on merge too
                item.setQuantity(newQty);
                item.setLineTotal(item.getUnitPrice().multiply(newQty));
            } else {
                BigDecimal qty = guestItem.getQuantity().min(MAX_LINE_QUANTITY);
                customerCart.getItems().add(EcCartItem.builder()
                        .cart(customerCart)
                        .product(guestItem.getProduct())
                        .variant(guestItem.getVariant())
                        .quantity(qty)
                        .unitPrice(guestItem.getUnitPrice())
                        .discountAmount(BigDecimal.ZERO)
                        .taxAmount(BigDecimal.ZERO)
                        .lineTotal(guestItem.getUnitPrice().multiply(qty))
                        .build());
            }
        }

        recalculate(customerCart);
        cartRepository.save(customerCart);
        cartRepository.delete(guestCart);
        session.removeAttribute(SESSION_KEY);

        if (dropped > 0) {
            log.warn("Guest-cart merge for customer #{}: dropped {} line(s) whose product is not " +
                     "visible to org {} (wrong organisation, unpublished, inactive or deleted).",
                     customer.getId(), dropped, orgId);
        }
        log.info("Merged guest cart sid={} into customer #{}", sid, customer.getId());
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIX 1 — the ONLY way a product may enter a cart.
     *
     * Deliberately gives the SAME "Product not available." message for
     * "does not exist", "belongs to another organisation", "not published",
     * "inactive" and "deleted". Distinguishing them would turn this endpoint
     * into an oracle that tells an attacker which product ids exist in other
     * tenants — which is most of the value they were after in the first place.
     */
    private EcProductCatalog loadStorefrontProduct(Long productId) {
        if (productId == null) throw new IllegalArgumentException("Product not available.");

        Long orgId = ContextProvider.getOrganizationId();
        if (orgId == null) {
            log.error("Add-to-cart with no organization context. StorefrontOrgContextFilter " +
                      "did not run for this path, or app.storefront.default-organization-id is unset.");
            throw new IllegalArgumentException("The store is not available right now.");
        }

        return productRepository
                .findByIdAndOrganizationIdAndPublishedTrueAndActiveTrueAndDeletedFalse(productId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Product not available."));
    }

    /** ✅ FIX 2 — an unknown or foreign variant id is rejected, never silently dropped. */
    private EcProductVariant resolveVariant(EcProductCatalog product, Long variantId) {
        if (variantId == null) return null;
        return product.getVariants().stream()
                .filter(v -> variantId.equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That option is no longer available."));
    }

    /**
     * ✅ FIX 3 — quantity is a whole number in [1, MAX_LINE_QUANTITY].
     *
     * @param fallback used when the input is null (add-to-cart defaults to 1);
     *                 pass null to reject a null input instead.
     */
    private static BigDecimal normalizeQuantity(BigDecimal raw, BigDecimal fallback) {
        if (raw == null) {
            if (fallback != null) return fallback;
            throw new IllegalArgumentException("Please enter a quantity.");
        }

        // Reject exponent-inflated values ("1E+40") before they can multiply into
        // a numeric(18,2) column and blow up with a raw Postgres overflow whose
        // message the controller would echo straight to the browser.
        if (raw.precision() - raw.scale() > 4)
            throw new IllegalArgumentException("Please enter a valid quantity.");

        BigDecimal qty = raw.setScale(0, RoundingMode.DOWN);   // whole units only

        if (qty.compareTo(BigDecimal.ONE) < 0)
            throw new IllegalArgumentException("Quantity must be at least 1.");
        if (qty.compareTo(MAX_LINE_QUANTITY) > 0)
            throw new IllegalArgumentException(
                    "You can order at most " + MAX_LINE_QUANTITY.toPlainString() + " of this item.");

        return qty;
    }

    private static boolean sameVariant(EcProductVariant a, EcProductVariant b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.getId().equals(b.getId());
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRICING
    // ══════════════════════════════════════════════════════════════════════

    /**
     * The price ALWAYS comes from the database, never from the request.
     * This was already correct in v2/v3 and is the most important property a
     * cart has — it is restated here so nobody "optimises" it away by accepting
     * a price from the client to save a lookup.
     */
    private BigDecimal resolveSellingPrice(EcProductCatalog product, EcProductVariant variant) {
        if (variant != null && variant.getSellingPrice() != null) return variant.getSellingPrice();
        return (product.getItem() != null && product.getItem().getUnitPrice() != null)
                ? product.getItem().getUnitPrice()
                : BigDecimal.ZERO;
    }

    /**
     * Returns the total available stock (quantity - reservedQuantity) across all
     * warehouses for the item underlying this product/variant.
     *
     * Both callers (addItem / updateQuantity) already handle the
     * "Only N left in stock." rejection path when this returns a non-null value.
     */
    private BigDecimal resolveAvailableStock(EcProductCatalog product, EcProductVariant variant) {
        Long itemId;
        if (variant != null && variant.getItem() != null) {
            itemId = variant.getItem().getId();
        } else if (product.getItem() != null) {
            itemId = product.getItem().getId();
        } else {
            return BigDecimal.ZERO;
        }
        BigDecimal avail = inventoryService.availableStockAcrossWarehouses(itemId);
        return avail != null ? avail : BigDecimal.ZERO;
    }

    // ══════════════════════════════════════════════════════════════════════
    // TOTALS
    // ══════════════════════════════════════════════════════════════════════

    private void recalculate(EcCart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(EcCartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream()
                .mapToInt(i -> i.getQuantity().intValue())
                .sum();

        BigDecimal shipping = subtotal.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : (subtotal.compareTo(FREE_SHIP_THRESHOLD) >= 0 ? BigDecimal.ZERO : DEFAULT_SHIPPING);

        cart.setSubtotal(subtotal);
        cart.setTotalItems(totalItems);
        cart.setShippingCharge(shipping);

        BigDecimal grand = subtotal
                .subtract(nvl(cart.getDiscountAmount()))
                .subtract(nvl(cart.getCouponDiscount()))
                .add(shipping)
                .add(nvl(cart.getTaxAmount()));

        cart.setGrandTotal(grand.max(BigDecimal.ZERO));
    }

    private static BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    // ══════════════════════════════════════════════════════════════════════
    // SESSION / MAPPING
    // ══════════════════════════════════════════════════════════════════════

    private String getOrCreateSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String sid = (String) session.getAttribute(SESSION_KEY);
        if (sid == null) {
            sid = UUID.randomUUID().toString();
            session.setAttribute(SESSION_KEY, sid);
        }
        return sid;
    }

    /** Same ordering as the card query: is_primary DESC, display_order, id. */
    private static String primaryImageUrl(EcProductCatalog product) {
        return product.getImages().stream()
                .sorted((a, b) -> {
                    int byPrimary = Boolean.compare(
                            Boolean.TRUE.equals(b.isPrimary()), Boolean.TRUE.equals(a.isPrimary()));
                    if (byPrimary != 0) return byPrimary;
                    int ao = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                    int bo = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                    if (ao != bo) return Integer.compare(ao, bo);
                    return Long.compare(a.getId(), b.getId());
                })
                .map(img -> img.getImageUrl())
                .findFirst()
                .orElse(null);
    }

    private SfCartDTO toDTO(EcCart cart) {
        List<SfCartDTO.SfCartItemDTO> items = cart.getItems().stream().map(i -> {
                BigDecimal itemAvail = resolveAvailableStock(i.getProduct(), i.getVariant());
                return SfCartDTO.SfCartItemDTO.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productTitle(i.getProduct().getProductTitle())
                        .productSlug(i.getProduct().getSlug())
                        .productImage(primaryImageUrl(i.getProduct()))
                        .variantId(i.getVariant() != null ? i.getVariant().getId() : null)
                        .variantName(i.getVariant() != null ? i.getVariant().getVariantName() : null)
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .availableStock(itemAvail)
                        .inStock(itemAvail.compareTo(BigDecimal.ZERO) > 0)
                        .build();
        }).toList();

        return SfCartDTO.builder()
                .id(cart.getId())
                .totalItems(cart.getTotalItems())
                .subtotal(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount())
                .couponDiscount(cart.getCouponDiscount())
                .shippingCharge(cart.getShippingCharge())
                .taxAmount(cart.getTaxAmount())
                .grandTotal(cart.getGrandTotal())
                .items(items)
                .build();
    }
}
