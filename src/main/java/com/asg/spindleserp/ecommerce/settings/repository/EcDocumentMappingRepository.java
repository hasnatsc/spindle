// Path: com/asg/spindleserp/ecommerce/settings/repository/EcDocumentMappingRepository.java
package com.asg.spindleserp.ecommerce.settings.repository;

import com.asg.spindleserp.ecommerce.settings.entity.EcDocumentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EcDocumentMappingRepository extends JpaRepository<EcDocumentMapping, Long> {

    Optional<EcDocumentMapping> findByOrganizationIdAndEcDocumentType(Long orgId, String ecDocumentType);
}
