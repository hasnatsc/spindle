package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.DocumentSequenceDTO;
import com.asg.spindleserp.setup.entity.DocumentSequence;

import java.util.List;

public interface DocumentSequenceService {
    DocumentSequenceDTO create(DocumentSequenceDTO dto);
    DocumentSequenceDTO update(Long id, DocumentSequenceDTO dto);
    DocumentSequenceDTO findById(Long id);
    List<DocumentSequenceDTO> findAll();
    List<DocumentSequenceDTO> findByOrg(Long orgId);
    void delete(Long id);

    /**
     * Atomically increment lastSeq and return the next formatted document number.
     * Format: {PREFIX}-{YY}-{NNNNNN}
     */
    String nextDocumentNumber(Long orgId, String prefix, String yearCode);

    DataTableResponse datatableList(int draw, int start, int length, String search);
    DocumentSequenceDTO toDTO(DocumentSequence entity);
}
