// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontWishlistService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/** StorefrontWishlistService — ec_wishlist via JDBC. Always scoped by customer_id. */
@Service
@RequiredArgsConstructor
public class StorefrontWishlistService {

    private final JdbcTemplate jdbcTemplate;

    /** @return true if the product is now in the wishlist, false if it was removed. */
    @Transactional
    public boolean toggle(Long customerId, Long productId) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM ec_wishlist WHERE customer_id = ? AND product_id = ?", customerId, productId);
        if (deleted > 0) return false;
        jdbcTemplate.update(
                "INSERT INTO ec_wishlist (customer_id, product_id, created_at) VALUES (?, ?, now()) " +
                "ON CONFLICT (customer_id, product_id) DO NOTHING", customerId, productId);
        return true;
    }

    @Transactional(readOnly = true)
    public Set<Long> productIds(Long customerId) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT product_id FROM ec_wishlist WHERE customer_id = ?", Long.class, customerId));
    }

    @Transactional(readOnly = true)
    public boolean contains(Long customerId, Long productId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ec_wishlist WHERE customer_id = ? AND product_id = ?",
                Integer.class, customerId, productId);
        return n != null && n > 0;
    }

    @Transactional(readOnly = true)
    public int count(Long customerId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ec_wishlist WHERE customer_id = ?", Integer.class, customerId);
        return n == null ? 0 : n;
    }
}
