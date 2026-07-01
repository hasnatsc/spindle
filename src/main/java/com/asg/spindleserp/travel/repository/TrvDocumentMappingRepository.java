package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvDocumentMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrvDocumentMappingRepository extends JpaRepository<TrvDocumentMapping, Long> {

    List<TrvDocumentMapping> findByOrganizationId(Long organizationId);

    Optional<TrvDocumentMapping> findByOrganizationIdAndTrvDocumentType(Long organizationId, String trvDocumentType);
}
