package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.DocumentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {
    Optional<DocumentSequence> findByOrganizationIdAndPrefixAndYearCode(
            Long orgId, String prefix, String yearCode);

    @Modifying
    @Query("UPDATE DocumentSequence ds SET ds.lastSeq = ds.lastSeq + 1 " +
            "WHERE ds.organizationId = :orgId AND ds.prefix = :prefix AND ds.yearCode = :yearCode")
    void increment(@Param("orgId") Long orgId,
                   @Param("prefix") String prefix,
                   @Param("yearCode") String yearCode);
}
