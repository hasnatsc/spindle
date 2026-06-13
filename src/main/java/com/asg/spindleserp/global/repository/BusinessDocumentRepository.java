package com.asg.spindleserp.global.repository;

import com.asg.spindleserp.common.enums.DocumentType;
import com.asg.spindleserp.global.entity.BusinessDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, Long>, JpaSpecificationExecutor<BusinessDocument> {
    Optional<BusinessDocument> findByDocumentNo(String documentNo);

    List<BusinessDocument> findByOrganizationIdAndDocumentTypeAndIsDeletedFalse(
            Long orgId, DocumentType type);

    List<BusinessDocument> findByParentDocumentIdAndIsDeletedFalse(Long parentId);

    @Query("SELECT bd FROM BusinessDocument bd WHERE bd.organization.id = :orgId " +
            "AND bd.documentType = :type AND bd.isDeleted = false " +
            "AND LOWER(bd.documentNo) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<BusinessDocument> search(@Param("orgId") Long orgId,
                                  @Param("type") DocumentType type,
                                  @Param("q") String q,
                                  Pageable pageable);
}
