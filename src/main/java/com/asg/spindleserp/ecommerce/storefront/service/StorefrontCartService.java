// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontCartService.java
package com.asg.spindleserp.ecommerce.storefront.service;
import com.asg.spindleserp.ecommerce.cart.entity.EcCart;
import com.asg.spindleserp.ecommerce.cart.entity.EcCartItem;
import com.asg.spindleserp.ecommerce.cart.repository.EcCartRepository;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductVariant;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcProductCatalogRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfCartDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * StorefrontCartService — manages EcCart for both guests (session-id keyed)
 * and logged-in customers (customer-id keyed). On login, the guest cart's
 * items are merged into (or become) the customer's cart.
 *
 * v2 changes:
 *  - peekCart(): read-only lookup that never creates a cart — cartItemCount now
 *    uses it, so anonymous crawlers/bots no longer spawn a cart row per visit.
 *  - setCouponDiscount(): checkout/coupon seam — recalculates totals with the
 *    validated discount and persists.
 *  - markOrdered(): flips the cart to ORDERED after successful order placement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontCartService {

    private static final String SESSION_KEY = "SF_CART_SESSION_ID";
    private static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("2000");
    private static final BigDecimal DEFAULT_SHIPPING    = new BigDecimal("80");

    private final EcCartRepository cartRepository;
    private final EcProductCatalogRepository productRepository;

    // ── GET OR CREATE CART ───────────────────────────────────────────────────
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

    /** Read-only cart lookup — never creates rows or sessions. */
    @Transactional(readOnly = true)
    public Optional<EcCart> peekCart(HttpServletRequest request, EcCustomer customer) {
        if (customer != null)
            return cartRepository.findByCustomerIdAndCartStatus(customer.getId(), EcCart.CartStatus.ACTIVE);
        HttpSession session = request.getSession(false);
        String sid = session != null ? (String) session.getAttribute(SESSION_KEY) : null;
        if (sid == null) return Optional.empty();
        return cartRepository.findBySessionIdAndCartStatus(sid, EcCart.CartStatus.ACTIVE);
    }

    // ── ADD ITEM ─────────────────────────────────────────────────────────────
    @Transactional
    public SfCartDTO addItem(HttpServletRequest request, EcCustomer customer,
                             Long productId, Long variantId, BigDecimal quantity) {
        EcCart cart = getOrCreateCart(request, customer);
        EcProductCatalog product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        BigDecimal unitPrice = resolveSellingPrice(product, variantId);
        BigDecimal available = resolveAvailableStock(product, variantId);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) quantity = BigDecimal.ONE;

        Optional<EcCartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId) &&
                        ((variantId == null && i.getVariant() == null) ||
                         (variantId != null && i.getVariant() != null && variantId.equals(i.getVariant().getId()))))
                .findFirst();

        BigDecimal targetQty = quantity;
        if (existing.isPresent()) {
            targetQty = existing.get().getQuantity().add(quantity);
        }
        if (available != null && targetQty.compareTo(available) > 0)
            throw new IllegalArgumentException("Only " + available.stripTrailingZeros().toPlainString() + " left in stock.");

        if (existing.isPresent()) {
            EcCartItem item = existing.get();
            item.setQuantity(targetQty);
            item.setLineTotal(unitPrice.multiply(targetQty));
        } else {
            EcCartItem item = EcCartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variantId != null ? findVariant(product, variantId) : null)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .lineTotal(unitPrice.multiply(quantity))
                    .build();
            cart.getItems().add(item);
        }
        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    // ── UPDATE QUANTITY ──────────────────────────────────────────────────────
    @Transactional
    public SfCartDTO updateQuantity(HttpServletRequest request, EcCustomer customer, Long cartItemId, BigDecimal newQty) {
        EcCart cart = getOrCreateCart(request, customer);
        EcCartItem item = cart.getItems().stream().filter(i -> i.getId().equals(cartItemId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found."));

        if (newQty == null || newQty.compareTo(BigDecimal.ZERO) <= 0) {
            cart.getItems().remove(item);
        } else {
            BigDecimal available = resolveAvailableStock(item.getProduct(), item.getVariant() != null ? item.getVariant().getId() : null);
            if (available != null && newQty.compareTo(available) > 0)
                throw new IllegalArgumentException("Only " + available.stripTrailingZeros().toPlainString() + " left in stock.");
            item.setQuantity(newQty);
            item.setLineTotal(item.getUnitPrice().multiply(newQty));
        }
        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    // ── REMOVE ITEM ──────────────────────────────────────────────────────────
    @Transactional
    public SfCartDTO removeItem(HttpServletRequest request, EcCustomer customer, Long cartItemId) {
        EcCart cart = getOrCreateCart(request, customer);
        cart.getItems().removeIf(i -> i.getId().equals(cartItemId));
        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    // ── VIEW ─────────────────────────────────────────────────────────────────
    /**
     * v3 OPTIMIZATION — read-only view. Previously this called getOrCreateCart(),
     * so every anonymous visitor who opened /cart, or merely hovered the header
     * bag icon (which lazy-fetches /cart/view), spawned an ec_carts row AND an
     * HTTP session. Now it peeks; no cart → a transient empty DTO, zero writes.
     * Carts are only created on the first actual /cart/add.
     */
    @Transactional(readOnly = true)
    public SfCartDTO viewCart(HttpServletRequest request, EcCustomer customer) {
        return peekCart(request, customer).map(this::toDTO).orElseGet(StorefrontCartService::emptyCartDTO);
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

    @Transactional(readOnly = true)
    public int cartItemCount(HttpServletRequest request, EcCustomer customer) {
        return peekCart(request, customer)
                .map(c -> c.getItems().stream().mapToInt(i -> i.getQuantity().intValue()).sum())
                .orElse(0);
    }

    // ── COUPON SEAM (called by StorefrontCheckoutService) ────────────────────
    @Transactional
    public SfCartDTO setCouponDiscount(HttpServletRequest request, EcCustomer customer, BigDecimal discount) {
        EcCart cart = getOrCreateCart(request, customer);
        cart.setCouponDiscount(discount != null ? discount.max(BigDecimal.ZERO) : BigDecimal.ZERO);
        recalculate(cart);
        return toDTO(cartRepository.save(cart));
    }

    /** Flip cart to ORDERED after successful order placement. */
    @Transactional
    public void markOrdered(EcCart cart) {
        cart.setCartStatus(EcCart.CartStatus.ORDERED);
        cartRepository.save(cart);
    }

    // ── MERGE GUEST CART INTO CUSTOMER CART ON LOGIN ────────────────────────
    @Transactional
    public void mergeGuestCartOnLogin(HttpServletRequest request, EcCustomer customer) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        String sid = (String) session.getAttribute(SESSION_KEY);
        if (sid == null) return;

        Optional<EcCart> guestCartOpt = cartRepository.findBySessionIdAndCartStatus(sid, EcCart.CartStatus.ACTIVE);
        if (guestCartOpt.isEmpty()) return;
        EcCart guestCart = guestCartOpt.get();
        if (guestCart.getItems().isEmpty()) { cartRepository.delete(guestCart); return; }

        EcCart customerCart = cartRepository.findByCustomerIdAndCartStatus(customer.getId(), EcCart.CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(EcCart.builder().customer(customer).cartStatus(EcCart.CartStatus.ACTIVE).build()));

        for (EcCartItem gi : guestCart.getItems()) {
            Optional<EcCartItem> match = customerCart.getItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(gi.getProduct().getId()) &&
                            ((gi.getVariant() == null && ci.getVariant() == null) ||
                             (gi.getVariant() != null && ci.getVariant() != null && gi.getVariant().getId().equals(ci.getVariant().getId()))))
                    .findFirst();
            if (match.isPresent()) {
                BigDecimal newQty = match.get().getQuantity().add(gi.getQuantity());
                match.get().setQuantity(newQty);
                match.get().setLineTotal(match.get().getUnitPrice().multiply(newQty));
            } else {
                customerCart.getItems().add(EcCartItem.builder()
                        .cart(customerCart).product(gi.getProduct()).variant(gi.getVariant())
                        .quantity(gi.getQuantity()).unitPrice(gi.getUnitPrice())
                        .discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO)
                        .lineTotal(gi.getLineTotal()).build());
            }
        }
        recalculate(customerCart);
        cartRepository.save(customerCart);
        cartRepository.delete(guestCart);
        session.removeAttribute(SESSION_KEY);
        log.info("Merged guest cart sid={} into customer #{}", sid, customer.getId());
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────
    private String getOrCreateSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String sid = (String) session.getAttribute(SESSION_KEY);
        if (sid == null) {
            sid = UUID.randomUUID().toString();
            session.setAttribute(SESSION_KEY, sid);
        }
        return sid;
    }

    private void recalculate(EcCart cart) {
        BigDecimal subtotal = cart.getItems().stream().map(EcCartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = cart.getItems().stream().mapToInt(i -> i.getQuantity().intValue()).sum();
        BigDecimal shipping = subtotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : (subtotal.compareTo(FREE_SHIP_THRESHOLD) >= 0 ? BigDecimal.ZERO : DEFAULT_SHIPPING);

        cart.setSubtotal(subtotal);
        cart.setTotalItems(totalItems);
        cart.setShippingCharge(shipping);
        BigDecimal grand = subtotal.subtract(cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO)
                .subtract(cart.getCouponDiscount() != null ? cart.getCouponDiscount() : BigDecimal.ZERO)
                .add(shipping)
                .add(cart.getTaxAmount() != null ? cart.getTaxAmount() : BigDecimal.ZERO);
        cart.setGrandTotal(grand.max(BigDecimal.ZERO));
    }

    private BigDecimal resolveSellingPrice(EcProductCatalog product, Long variantId) {
        if (variantId != null) {
            EcProductVariant v = findVariant(product, variantId);
            if (v != null && v.getSellingPrice() != null) return v.getSellingPrice();
        }
        return product.getItem().getUnitPrice() != null ? product.getItem().getUnitPrice() : BigDecimal.ZERO;
    }

    /**
     * v3 OPTIMIZATION — the previous version ran stockLedgerService.balanceByItem()
     * on EVERY add/update and then discarded the result (always returned null).
     * That was one wasted ledger query per cart mutation. The dead call and the
     * StockLedgerService dependency are removed.
     *
     * ★ Stock-blocking seam: to enforce stock at add-to-cart time, sum available
     *   qty from StockLedgerService.balanceByItem(itemId) here (variant-aware via
     *   the variant's item), and return it — the callers already handle the
     *   "Only N left in stock" rejection path. Returning null = never block.
     */
    private BigDecimal resolveAvailableStock(EcProductCatalog product, Long variantId) {
        return null;
    }

    private EcProductVariant findVariant(EcProductCatalog product, Long variantId) {
        return product.getVariants().stream().filter(v -> v.getId().equals(variantId)).findFirst().orElse(null);
    }

    /** Same ordering as the card query: is_primary DESC, display_order, id. */
    private static String primaryImageUrl(EcProductCatalog product) {
        return product.getImages().stream()
                .sorted((a, b) -> {
                    int byPrimary = Boolean.compare(Boolean.TRUE.equals(b.isPrimary()), Boolean.TRUE.equals(a.isPrimary()));
                    if (byPrimary != 0) return byPrimary;
                    int ao = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                    int bo = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                    if (ao != bo) return Integer.compare(ao, bo);
                    return Long.compare(a.getId(), b.getId());
                })
                .map(img -> img.getImageUrl())
                .findFirst().orElse(null);
    }

    private SfCartDTO toDTO(EcCart cart) {
        List<SfCartDTO.SfCartItemDTO> items = cart.getItems().stream().map(i ->
            SfCartDTO.SfCartItemDTO.builder()
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
                .inStock(true)
                .build()
        ).toList();

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
