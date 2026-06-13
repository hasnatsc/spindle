package com.asg.spindleserp.global.repository;

import com.asg.spindleserp.global.entity.BusinessDocumentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessDocumentLineRepository extends JpaRepository<BusinessDocumentLine, Long> {
    List<BusinessDocumentLine> findByDocumentIdOrderByLineNumber(Long docId);

    List<BusinessDocumentLine> findByItemId(Long itemId);
}
