package com.asg.spindleserp.commercial.repository;

import com.asg.spindleserp.commercial.entity.CommercialInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommercialInvoiceRepository extends JpaRepository<CommercialInvoice, Long> {
    List<CommercialInvoice> findByOrganizationIdAndInvoiceType(Long orgId, CommercialInvoice.InvoiceType type);
    boolean existsByInvoiceNo(String invoiceNo);
}
