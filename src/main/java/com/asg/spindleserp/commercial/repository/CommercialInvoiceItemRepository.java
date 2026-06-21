package com.asg.spindleserp.commercial.repository;

import com.asg.spindleserp.commercial.entity.CommercialInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommercialInvoiceItemRepository extends JpaRepository<CommercialInvoiceItem, Long> {
    List<CommercialInvoiceItem> findByInvoiceId(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
