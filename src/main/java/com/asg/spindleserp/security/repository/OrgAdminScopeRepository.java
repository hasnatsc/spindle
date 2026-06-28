package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.OrgAdminScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface OrgAdminScopeRepository extends JpaRepository<OrgAdminScope, Long> {

    /** Active org-admin grants for a specific user — used at login / context switch. */
    @Query("""
        SELECT oas.organization.id FROM OrgAdminScope oas
        WHERE oas.user.id = :userId AND oas.active = true
        """)
    Set<Long> findAdminOrgIdsByUserId(@Param("userId") Long userId);

    /** All active org-admin grants for an org — for the admin management page. */
    List<OrgAdminScope> findByOrganizationIdAndActiveTrue(Long orgId);

    Optional<OrgAdminScope> findByUserIdAndOrganizationId(Long userId, Long orgId);

    boolean existsByUserIdAndOrganizationIdAndActiveTrue(Long userId, Long orgId);
}
