package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {
    List<DocumentFile> findByDocumentTypeAndReferenceId(String docType, Long refId);

    void deleteByDocumentTypeAndReferenceId(String docType, Long refId);
}
