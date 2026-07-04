// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontAddressService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import com.asg.spindleserp.ecommerce.storefront.dto.SfAddressDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * StorefrontAddressService — customer address book (ec_customer_addresses) via JDBC.
 * Every read/write is scoped by customer_id — a customer can never touch another
 * customer's addresses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontAddressService {

    private final JdbcTemplate jdbcTemplate;

    private static final String COLS = """
            id, address_type, contact_person, contact_phone, address_line1, address_line2,
            area, landmark, upazila, district, division, post_code, country,
            default_shipping, default_billing
            """;

    private final RowMapper<SfAddressDTO> mapper = (ResultSet rs, int i) -> map(rs);

    @Transactional(readOnly = true)
    public List<SfAddressDTO> list(Long customerId) {
        return jdbcTemplate.query(
                "SELECT " + COLS + " FROM ec_customer_addresses WHERE customer_id = ? AND active = true " +
                "ORDER BY default_shipping DESC, id DESC", mapper, customerId);
    }

    @Transactional(readOnly = true)
    public SfAddressDTO byId(Long customerId, Long id) {
        List<SfAddressDTO> l = jdbcTemplate.query(
                "SELECT " + COLS + " FROM ec_customer_addresses WHERE id = ? AND customer_id = ? AND active = true",
                mapper, id, customerId);
        return l.isEmpty() ? null : l.getFirst();
    }

    @Transactional
    public Long save(Long customerId, SfAddressDTO d) {
        if (isBlank(d.getAddressLine1()) || isBlank(d.getDistrict()))
            throw new IllegalArgumentException("Address line and district are required.");

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ec_customer_addresses WHERE customer_id = ? AND active = true",
                Integer.class, customerId);
        boolean makeDefault = (existing == null || existing == 0) || Boolean.TRUE.equals(d.getDefaultShipping());
        if (makeDefault)
            jdbcTemplate.update("UPDATE ec_customer_addresses SET default_shipping = false, default_billing = false WHERE customer_id = ?", customerId);

        return jdbcTemplate.queryForObject("""
                INSERT INTO ec_customer_addresses
                    (customer_id, active, address_type, contact_person, contact_phone,
                     address_line1, address_line2, area, landmark, upazila, district, division,
                     post_code, country, default_shipping, default_billing, created_at)
                VALUES (?, true, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                RETURNING id
                """, Long.class,
                customerId,
                d.getAddressType() != null && !d.getAddressType().isBlank() ? d.getAddressType() : "HOME",
                trim(d.getContactPerson()), trim(d.getContactPhone()),
                trim(d.getAddressLine1()), trim(d.getAddressLine2()), trim(d.getArea()), trim(d.getLandmark()),
                trim(d.getUpazila()), trim(d.getDistrict()), trim(d.getDivision()),
                trim(d.getPostCode()), d.getCountry() != null ? d.getCountry() : "Bangladesh",
                makeDefault, makeDefault);
    }

    @Transactional
    public void delete(Long customerId, Long id) {
        int n = jdbcTemplate.update(
                "UPDATE ec_customer_addresses SET active = false, default_shipping = false, default_billing = false " +
                "WHERE id = ? AND customer_id = ?", id, customerId);
        if (n == 0) throw new IllegalArgumentException("Address not found.");
    }

    @Transactional
    public void setDefault(Long customerId, Long id) {
        Integer owns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ec_customer_addresses WHERE id = ? AND customer_id = ? AND active = true",
                Integer.class, id, customerId);
        if (owns == null || owns == 0) throw new IllegalArgumentException("Address not found.");
        jdbcTemplate.update("UPDATE ec_customer_addresses SET default_shipping = false, default_billing = false WHERE customer_id = ?", customerId);
        jdbcTemplate.update("UPDATE ec_customer_addresses SET default_shipping = true, default_billing = true WHERE id = ?", id);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────
    private static SfAddressDTO map(ResultSet rs) throws SQLException {
        return SfAddressDTO.builder()
                .id(rs.getLong("id"))
                .addressType(rs.getString("address_type"))
                .contactPerson(rs.getString("contact_person"))
                .contactPhone(rs.getString("contact_phone"))
                .addressLine1(rs.getString("address_line1"))
                .addressLine2(rs.getString("address_line2"))
                .area(rs.getString("area"))
                .landmark(rs.getString("landmark"))
                .upazila(rs.getString("upazila"))
                .district(rs.getString("district"))
                .division(rs.getString("division"))
                .postCode(rs.getString("post_code"))
                .country(rs.getString("country"))
                .defaultShipping(rs.getBoolean("default_shipping"))
                .defaultBilling(rs.getBoolean("default_billing"))
                .build();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trim(String s)     { return s == null ? null : s.trim(); }
}
