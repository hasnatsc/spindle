package com.asg.spindleserp.global.repository;

import com.asg.spindleserp.global.entity.BusinessDocumentLineLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessDocumentLineLotRepository extends JpaRepository<BusinessDocumentLineLot, Long> {
    List<BusinessDocumentLineLot> findByDocumentLineId(Long lineId);

    List<BusinessDocumentLineLot> findByLotId(Long lotId);
}
