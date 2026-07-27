package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ChartOfAccountSubRepository.
 *
 * <p>The important addition is {@link #findByIdAndOrganizationId}. Every read and
 * write path used to load by primary key alone, so any authenticated user could
 * fetch, edit, deactivate or delete another organisation's sub-account by putting
 * its id in the URL — the same insecure-direct-object-reference pattern that was
 * fixed on the storefront cart. Loading by {@code (id, orgId)} makes the
 * tenant boundary a property of the query rather than something the service has
 * to remember to check.</p>
 *
 * <p>The type filter in {@link #searchForSelect} is now live. It was commented out
 * because {@code subAccountType} was not a mapped property; it is mapped read-only
 * on the entity, so JPQL can use it directly and the caller no longer has to pull
 * the whole table into memory and filter with {@code instanceof}.</p>
 */
@Repository
public interface ChartOfAccountSubRepository
        extends JpaRepository<ChartOfAccountSub, Long>, JpaSpecificationExecutor<ChartOfAccountSub> {

    // ── Tenant-safe lookups ───────────────────────────────────────────────────

    Optional<ChartOfAccountSub> findByIdAndOrganizationId(Long id, Long organizationId);

    List<ChartOfAccountSub> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<ChartOfAccountSub> findByOrganizationIdAndSubAccountType(Long orgId, String subAccountType);

    // ── Uniqueness (per organisation, never global) ────────────────────────────

    boolean existsByOrganizationIdAndSubAccountCodeIgnoreCase(Long orgId, String code);

    boolean existsByOrganizationIdAndSubAccountCodeIgnoreCaseAndIdNot(Long orgId, String code, Long id);

    /** Retained for callers outside this module that still look codes up globally. */
    Optional<ChartOfAccountSub> findBySubAccountCode(String code);

    // ── Select2 ───────────────────────────────────────────────────────────────

    /**
     * Paged picker search. Pass {@code type = ""} for all partitions.
     * <p>
     * The sentinel is an empty string rather than {@code null} on purpose:
     * Hibernate has to infer a SQL type for every bound parameter, and a bare
     * {@code :type IS NULL} on an untyped null is the kind of thing that works on
     * one dialect and throws {@code could not determine type} on another. An empty
     * string is unambiguous.
     */
    @Query("""
                SELECT s
                FROM ChartOfAccountSub s
                WHERE s.organization.id = :orgId
                  AND s.isActive = true
                  AND (:type = '' OR s.subAccountType = :type)
                  AND (
                        :q = ''
                     OR LOWER(s.subAccountCode) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(s.subAccountName) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
                ORDER BY s.subAccountCode ASC, s.id ASC
            """)
    List<ChartOfAccountSub> searchForSelect(@Param("orgId") Long orgId,
                                            @Param("type") String type,
                                            @Param("q") String q,
                                            org.springframework.data.domain.Pageable pageable);
}
