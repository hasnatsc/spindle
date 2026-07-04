// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontCheckoutService.java
package com.asg.spindleserp.ecommerce.storefront.service;

//import com.asg.spindleserp.common.service.DocumentSequenceService;   // ★ adjust import if your DocumentSequenceService lives elsewhere
import com.asg.spindleserp.ecommerce.cart.entity.EcCart;
import com.asg.spindleserp.ecommerce.cart.entity.EcCartItem;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.customerSupport.repository.EcCustomerRepository;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import com.asg.spindleserp.ecommerce.order.entity.EcOrderItem;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfAddressDTO;
import com.asg.spindleserp.ecommerce.storefront.dto.SfCartDTO;
import com.asg.spindleserp.ecommerce.storefront.dto.SfCheckoutDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StorefrontCheckoutService — coupons + order placement (COD).
 *
 * Order placement writes: ec_orders (+cascaded ec_order_items via JPA),
 * ec_order_addresses, ec_order_status_history, ec_coupon_usage, ec_reward_points,
 * customer running totals, and flips the cart to ORDERED.
 *
 * ★ GL: the order's journal entry is intentionally NOT posted here — the existing
 *   eCommerce admin GL bridge posts on order confirmation, keeping storefront
 *   writes light and admin in control.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontCheckoutService {

    public static final String SESSION_COUPON = "SF_COUPON_CODE";

    private static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("2000");
    private static final BigDecimal DEFAULT_SHIPPING    = new BigDecimal("80");

    private final StorefrontCartService cartService;
    private final StorefrontAddressService addressService;
    private final EcOrderRepository orderRepository;
    private final EcCustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DocumentSequenceService documentSequenceService;

    // ═════════════════════════ COUPONS ═════════════════════════

    @Transactional
    public SfCartDTO applyCoupon(HttpServletRequest request, EcCustomer customer, String code) {
        SfCartDTO cart = cartService.viewCart(request, customer);
        if (cart.getItems() == null || cart.getItems().isEmpty())
            throw new IllegalArgumentException("Your cart is empty.");
        BigDecimal discount = validateCoupon(code, cart.getSubtotal(), customer != null ? customer.getId() : null);
        request.getSession(true).setAttribute(SESSION_COUPON, code.trim().toUpperCase());
        return cartService.setCouponDiscount(request, customer, discount);
    }

    @Transactional
    public SfCartDTO removeCoupon(HttpServletRequest request, EcCustomer customer) {
        HttpSession s = request.getSession(false);
        if (s != null) s.removeAttribute(SESSION_COUPON);
        return cartService.setCouponDiscount(request, customer, BigDecimal.ZERO);
    }

    /** Re-validates the session coupon for display; silently drops it if it's no longer valid. */
    public Map<String, Object> appliedCoupon(HttpServletRequest request, EcCustomer customer, BigDecimal subtotal) {
        HttpSession s = request.getSession(false);
        String code = s != null ? (String) s.getAttribute(SESSION_COUPON) : null;
        if (code == null) return null;
        try {
            BigDecimal d = validateCoupon(code, subtotal, customer != null ? customer.getId() : null);
            Map<String, Object> m = new HashMap<>();
            m.put("code", code);
            m.put("discount", d);
            return m;
        } catch (Exception e) {
            if (s != null) s.removeAttribute(SESSION_COUPON);
            return null;
        }
    }

    private BigDecimal validateCoupon(String rawCode, BigDecimal subtotal, Long customerId) {
        if (rawCode == null || rawCode.isBlank())
            throw new IllegalArgumentException("Please enter a coupon code.");
        Long orgId = ContextProvider.getOrganizationId();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, discount_type, discount_value, maximum_discount, minimum_order,
                       usage_limit, usage_per_customer, valid_from, valid_to, active
                FROM ec_coupon
                WHERE organization_id = ? AND UPPER(coupon_code) = UPPER(?)
                """, orgId, rawCode.trim());
        if (rows.isEmpty()) throw new IllegalArgumentException("Invalid coupon code.");
        Map<String, Object> c = rows.getFirst();

        if (!Boolean.TRUE.equals(c.get("active")))
            throw new IllegalArgumentException("This coupon is no longer active.");

        LocalDateTime now = LocalDateTime.now();
        Timestamp from = (Timestamp) c.get("valid_from");
        Timestamp to   = (Timestamp) c.get("valid_to");
        if (from != null && now.isBefore(from.toLocalDateTime()))
            throw new IllegalArgumentException("This coupon is not active yet.");
        if (to != null && now.isAfter(to.toLocalDateTime()))
            throw new IllegalArgumentException("This coupon has expired.");

        BigDecimal minOrder = toBD(c.get("minimum_order"));
        if (minOrder != null && minOrder.signum() > 0 && subtotal.compareTo(minOrder) < 0)
            throw new IllegalArgumentException("Minimum order of ৳" +
                    minOrder.stripTrailingZeros().toPlainString() + " is required for this coupon.");

        Long couponId = ((Number) c.get("id")).longValue();
        Integer usageLimit = c.get("usage_limit") != null ? ((Number) c.get("usage_limit")).intValue() : null;
        if (usageLimit != null) {
            Integer used = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ec_coupon_usage WHERE coupon_id = ?", Integer.class, couponId);
            if (used != null && used >= usageLimit)
                throw new IllegalArgumentException("This coupon has reached its usage limit.");
        }
        Integer perCustomer = c.get("usage_per_customer") != null ? ((Number) c.get("usage_per_customer")).intValue() : null;
        if (perCustomer != null && customerId != null) {
            Integer used = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ec_coupon_usage WHERE coupon_id = ? AND customer_id = ?",
                    Integer.class, couponId, customerId);
            if (used != null && used >= perCustomer)
                throw new IllegalArgumentException("You have already used this coupon.");
        }

        BigDecimal value = toBD(c.get("discount_value"));
        if (value == null) value = BigDecimal.ZERO;
        BigDecimal discount = "PERCENTAGE".equals(c.get("discount_type"))
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : value;
        BigDecimal max = toBD(c.get("maximum_discount"));
        if (max != null && max.signum() > 0 && discount.compareTo(max) > 0) discount = max;
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    // ═════════════════════════ PLACE ORDER ═════════════════════════

    @Transactional
    public String placeOrder(HttpServletRequest request, EcCustomer customer, SfCheckoutDTO dto) {
        if (customer == null) throw new IllegalArgumentException("Please sign in to place your order.");
        if (dto == null) throw new IllegalArgumentException("Invalid checkout request.");
        if (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isBlank()
                && !"COD".equalsIgnoreCase(dto.getPaymentMethod()))
            throw new IllegalArgumentException("Online payment is coming soon — please choose Cash on Delivery.");  // ★ payment gateway seam

        EcCart cart = cartService.getOrCreateCart(request, customer);
        if (cart.getItems() == null || cart.getItems().isEmpty())
            throw new IllegalArgumentException("Your cart is empty.");

        SfAddressDTO addr = resolveAddress(customer, dto);

        BigDecimal subtotal = cart.getItems().stream()
                .map(EcCartItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = resolveShippingCharge(subtotal, addr.getDistrict(), addr.getDivision());

        HttpSession session = request.getSession(false);
        String couponCode = session != null ? (String) session.getAttribute(SESSION_COUPON) : null;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Long couponId = null;
        if (couponCode != null) {
            couponDiscount = validateCoupon(couponCode, subtotal, customer.getId());
            couponId = jdbcTemplate.queryForObject(
                    "SELECT id FROM ec_coupon WHERE organization_id = ? AND UPPER(coupon_code) = UPPER(?)",
                    Long.class, ContextProvider.getOrganizationId(), couponCode);
        }
        BigDecimal grand = subtotal.subtract(couponDiscount).add(shipping).max(BigDecimal.ZERO);
        String orderNo   = documentSequenceService.nextDocumentNumber(ContextProvider.getOrganizationId(), "EC-ORDER", LocalDate.now().format(DateTimeFormatter.ofPattern("yy")));

        EcOrder order = EcOrder.builder()
                .orderNo(orderNo)
                .orderDate(LocalDateTime.now())
                .orderStatus(EcOrder.OrderStatus.PENDING)            // ★ adjust enum type names if they differ
                .paymentStatus(EcOrder.PaymentStatus.UNPAID)
                .shippingStatus(EcOrder.ShippingStatus.PENDING)
                .orderSource(EcOrder.OrderSource.WEB)
                .currencyCode("BDT")
                .exchangeRate(BigDecimal.ONE)
                .customer(customer)
                .cartId(cart.getId())
                .subtotal(subtotal)
                .productDiscount(BigDecimal.ZERO)
                .couponDiscount(couponDiscount)
                .taxAmount(BigDecimal.ZERO)
                .shippingCharge(shipping)
                .grandTotal(grand)
                .paidAmount(BigDecimal.ZERO)
                .dueAmount(grand)
                .customerNote(dto.getCustomerNote() != null ? dto.getCustomerNote().trim() : null)
                .active(true)
                .build();

        if (order.getOrderItems() == null) order.setOrderItems(new ArrayList<>());
        for (EcCartItem ci : cart.getItems()) {
            order.getOrderItems().add(EcOrderItem.builder()
                    .order(order)
                    .product(ci.getProduct())
                    .variant(ci.getVariant())
                    .item(ci.getVariant() != null && ci.getVariant().getItem() != null
                            ? ci.getVariant().getItem() : ci.getProduct().getItem())
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getUnitPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .lineTotal(ci.getLineTotal())
                    .build());
        }
        order = orderRepository.save(order);

        insertOrderAddress(order.getId(), "SHIPPING", addr, customer);
        insertOrderAddress(order.getId(), "BILLING",  addr, customer);
        jdbcTemplate.update("""
                INSERT INTO ec_order_status_history (order_id, status, changed_at, changed_by, remarks, ip_address)
                VALUES (?, 'PENDING', now(), ?, 'Order placed from storefront', ?)
                """, order.getId(), customer.getFullName(), clientIp(request));

        if (couponId != null) {
            jdbcTemplate.update("""
                    INSERT INTO ec_coupon_usage (coupon_id, customer_id, order_id, discount_amount, used_at)
                    VALUES (?, ?, ?, ?, now())
                    """, couponId, customer.getId(), order.getId(), couponDiscount);
        }

        if (Boolean.TRUE.equals(dto.getSaveAddress()) && dto.getAddressId() == null) {
            try { addressService.save(customer.getId(), addr); }
            catch (Exception e) { log.warn("Could not save checkout address: {}", e.getMessage()); }
        }

        cartService.markOrdered(cart);
        if (session != null) session.removeAttribute(SESSION_COUPON);

        // customer running totals + loyalty points
        customer.setTotalOrders(customer.getTotalOrders() + 1);
        customer.setTotalPurchase((customer.getTotalPurchase() != null
                ? customer.getTotalPurchase() : BigDecimal.ZERO).add(grand));
        int points = earnPoints(grand);
        if (points > 0) {
            customer.setRewardPoints(customer.getRewardPoints() + points);
            jdbcTemplate.update("""
                    INSERT INTO ec_reward_points
                        (customer_id, points, transaction_type, reference_type, reference_id, remarks, created_at)
                    VALUES (?, ?, 'EARN', 'EC_ORDER', ?, ?, now())
                    """, customer.getId(), points, order.getId(), "Earned on order " + orderNo);
        }
        customerRepository.save(customer);

        log.info("Storefront order {} placed by customer #{} — grand total ৳{}", orderNo, customer.getId(), grand);
        return orderNo;
    }

    // ═════════════════════════ HELPERS ═════════════════════════

    private SfAddressDTO resolveAddress(EcCustomer customer, SfCheckoutDTO dto) {
        if (dto.getAddressId() != null) {
            SfAddressDTO a = addressService.byId(customer.getId(), dto.getAddressId());   // customer-scoped lookup
            if (a == null) throw new IllegalArgumentException("The selected address was not found.");
            if (isBlank(a.getContactPerson())) a.setContactPerson(customer.getFullName());
            if (isBlank(a.getContactPhone()))  a.setContactPhone(customer.getPhone());
            return a;
        }
        if (isBlank(dto.getFullName()) || isBlank(dto.getPhone())
                || isBlank(dto.getAddressLine1()) || isBlank(dto.getDistrict()))
            throw new IllegalArgumentException("Please fill in your name, phone, address line and district.");
        return SfAddressDTO.builder()
                .addressType("SHIPPING")
                .contactPerson(dto.getFullName().trim())
                .contactPhone(dto.getPhone().trim())
                .addressLine1(dto.getAddressLine1().trim())
                .addressLine2(dto.getAddressLine2())
                .area(dto.getArea())
                .landmark(dto.getLandmark())
                .upazila(dto.getUpazila())
                .district(dto.getDistrict().trim())
                .division(dto.getDivision())
                .postCode(dto.getPostCode())
                .country("Bangladesh")
                .defaultShipping(false)
                .defaultBilling(false)
                .build();
    }

    /** Zone charge by district (then division); global free-over-2000 fallback. Note: ec_order_addresses uses "postcode". */
    private BigDecimal resolveShippingCharge(BigDecimal subtotal, String district, String division) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT shipping_charge, free_shipping, minimum_order_amount
                    FROM ec_shipping_zones
                    WHERE organization_id = ? AND active = true
                      AND (   (district IS NOT NULL AND LOWER(district) = LOWER(COALESCE(?, '')))
                           OR (district IS NULL AND division IS NOT NULL AND LOWER(division) = LOWER(COALESCE(?, ''))) )
                    ORDER BY district NULLS LAST, id
                    LIMIT 1
                    """, ContextProvider.getOrganizationId(), district, division);
            if (!rows.isEmpty()) {
                Map<String, Object> z = rows.getFirst();
                BigDecimal min = toBD(z.get("minimum_order_amount"));
                if (Boolean.TRUE.equals(z.get("free_shipping"))
                        && (min == null || subtotal.compareTo(min) >= 0)) return BigDecimal.ZERO;
                BigDecimal charge = toBD(z.get("shipping_charge"));
                if (charge != null) return charge;
            }
        } catch (Exception e) {
            log.warn("Shipping zone lookup failed, using default: {}", e.getMessage());
        }
        return subtotal.compareTo(FREE_SHIP_THRESHOLD) >= 0 ? BigDecimal.ZERO : DEFAULT_SHIPPING;
    }

    private void insertOrderAddress(Long orderId, String type, SfAddressDTO a, EcCustomer customer) {
        jdbcTemplate.update("""
                INSERT INTO ec_order_addresses
                    (order_id, address_type, full_name, phone, email, address_line1, address_line2,
                     landmark, upazila, district, division, postcode, country)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                orderId, type,
                a.getContactPerson(), a.getContactPhone(), customer.getEmail(),
                a.getAddressLine1(), a.getAddressLine2(), a.getLandmark(),
                a.getUpazila(), a.getDistrict(), a.getDivision(), a.getPostCode(),
                a.getCountry() != null ? a.getCountry() : "Bangladesh");
    }

    private int earnPoints(BigDecimal grand) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT earn_rate FROM ec_loyalty_programs WHERE organization_id = ? AND active = true ORDER BY id LIMIT 1",
                    ContextProvider.getOrganizationId());
            if (rows.isEmpty() || rows.getFirst().get("earn_rate") == null) return 0;
            BigDecimal rate = toBD(rows.getFirst().get("earn_rate"));
            return rate == null ? 0 : grand.multiply(rate).setScale(0, RoundingMode.DOWN).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private static BigDecimal toBD(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
