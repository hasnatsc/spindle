package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvDocumentRepository extends JpaRepository<TrvDocument, Long> {

    List<TrvDocument> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            TrvDocument.EntityType entityType, Long entityId);

    long countByEntityTypeAndEntityId(TrvDocument.EntityType entityType, Long entityId);
}
