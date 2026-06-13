package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryMasterRepository extends JpaRepository<JournalEntryMaster, Long>, JpaSpecificationExecutor<JournalEntryMaster> {
    Optional<JournalEntryMaster> findByVoucherNo(String voucherNo);

    List<JournalEntryMaster> findByOrganizationIdAndIsPosted(Long orgId, boolean posted);

    long countByOrganizationIdAndIsPosted(Long orgId, boolean posted);
}
