package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.TermsMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TermsMasterRepository
        extends JpaRepository<TermsMaster, Long>,
                JpaSpecificationExecutor<TermsMaster> {

    List<TermsMaster> findByDocumentTypeAndIsActiveTrue(String documentType);

    List<TermsMaster> findByIsActiveTrueOrderBySortOrderAsc();

    boolean existsByTitleAndDocumentType(String title, String documentType);

    boolean existsByTitleAndDocumentTypeAndIdNot(String title, String documentType, Long id);
}
