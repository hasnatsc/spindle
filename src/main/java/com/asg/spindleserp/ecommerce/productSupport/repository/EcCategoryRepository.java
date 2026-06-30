// Path: com/asg/spindleserp/ecommerce/repository/EcCategoryRepository.java
package com.asg.spindleserp.ecommerce.productSupport.repository;

import com.asg.spindleserp.ecommerce.productSupport.entity.EcCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcCategoryRepository extends JpaRepository<EcCategory, Long> {
    boolean existsByOrganizationIdAndCategoryCode(Long orgId, String code);
    boolean existsByOrganizationIdAndCategoryCodeAndIdNot(Long orgId, String code, Long id);
    boolean existsByOrganizationIdAndSlug(Long orgId, String slug);
    boolean existsByOrganizationIdAndSlugAndIdNot(Long orgId, String slug, Long id);

    @Query("SELECT c FROM EcCategory c WHERE c.organization.id = :orgId AND c.deleted = false ORDER BY c.levelNo, c.displayOrder")
    List<EcCategory> findAllByOrgNotDeleted(@Param("orgId") Long orgId);

    @Query("SELECT c FROM EcCategory c WHERE c.organization.id = :orgId AND c.active = true AND c.deleted = false ORDER BY c.levelNo, c.displayOrder")
    List<EcCategory> findActiveByOrg(@Param("orgId") Long orgId);

    @Query("SELECT c FROM EcCategory c WHERE c.organization.id = :orgId AND c.parentCategory IS NULL AND c.deleted = false ORDER BY c.displayOrder")
    List<EcCategory> findRootCategories(@Param("orgId") Long orgId);

    Optional<EcCategory> findByOrganizationIdAndSlug(Long orgId, String slug);
}
