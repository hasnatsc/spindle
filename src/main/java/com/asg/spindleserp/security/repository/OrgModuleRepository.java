package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.OrgModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface OrgModuleRepository extends JpaRepository<OrgModule, Long> {

    /** All module rows for one org — used by OrgModuleService and the admin UI. */
    List<OrgModule> findByOrganizationIdOrderByModuleKey(Long orgId);

    /** Only ACTIVE modules for one org — used by DynamicAuthorizationManager cache. */
    @Query("""
        SELECT om.moduleKey FROM OrgModule om
        WHERE om.organization.id = :orgId AND om.active = true
        """)
    Set<String> findActiveModuleKeysByOrgId(@Param("orgId") Long orgId);

    /**
     * All active modules across ALL orgs — loaded once per cache cycle.
     * Returns (orgId, moduleKey) pairs for the authorization cache.
     */
    @Query("""
        SELECT om FROM OrgModule om
        WHERE om.active = true
        """)
    List<OrgModule> findAllActive();

    Optional<OrgModule> findByOrganizationIdAndModuleKey(Long orgId, String moduleKey);

    boolean existsByOrganizationIdAndModuleKeyAndActiveTrue(Long orgId, String moduleKey);

    @Modifying
    @Query("""
        UPDATE OrgModule om SET om.active = :active,
            om.revokedBy = :actor, om.revokedAt = CURRENT_TIMESTAMP
        WHERE om.organization.id = :orgId AND om.moduleKey = :moduleKey
        """)
    int setModuleActive(@Param("orgId")      Long orgId,
                        @Param("moduleKey")  String moduleKey,
                        @Param("active")     boolean active,
                        @Param("actor")      String actor);
}
