package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, Long> {
    List<JournalEntryLine> findByJournalEntryId(Long journalEntryId);

    List<JournalEntryLine> findByAccountId(Long accountId);

    List<JournalEntryLine> findBySubAccountId(Long subAccountId);
}
