// Path: com/asg/spindleserp/ecommerce/repository/EcProductCatalogRepository.java
package com.asg.spindleserp.ecommerce.productSupport.repository;

import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EcProductCatalogRepository
        extends JpaRepository<EcProductCatalog, Long>,
                JpaSpecificationExecutor<EcProductCatalog> {

    boolean existsByOrganizationIdAndSlug(Long orgId, String slug);
    boolean existsByOrganizationIdAndSlugAndIdNot(Long orgId, String slug, Long id);
    boolean existsByOrganizationIdAndItemId(Long orgId, Long itemId);
    boolean existsByOrganizationIdAndItemIdAndIdNot(Long orgId, Long itemId, Long id);

    Optional<EcProductCatalog> findByOrganizationIdAndSlug(Long orgId, String slug);

    List<EcProductCatalog> findByOrganizationIdAndActiveTrue(Long orgId);
    List<EcProductCatalog> findByOrganizationIdAndActiveTrueAndPublishedTrue(Long orgId);
    List<EcProductCatalog> findByOrganizationIdAndActiveTrueAndFeaturedTrue(Long orgId);
}
