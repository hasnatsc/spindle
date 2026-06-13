package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.TermsMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TermsMasterRepository extends JpaRepository<TermsMaster, Long> {
    List<TermsMaster> findByDocumentTypeAndIsActiveTrue(String documentType);
}
