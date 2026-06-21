package com.asg.spindleserp.commercial.repository;

import com.asg.spindleserp.commercial.entity.DocumentTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentTermRepository extends JpaRepository<DocumentTerm, Long> {
    List<DocumentTerm> findByInvoiceIdOrderBySortOrder(Long invoiceId);
    List<DocumentTerm> findByInvoiceId(Long invoiceId);
    List<DocumentTerm> findByDocumentId(Long documentId);

    @Modifying
    @Query("DELETE FROM DocumentTerm t WHERE t.invoice.id = :invoiceId")
    void deleteByInvoiceId(Long invoiceId);
}
