package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * TrvDocument — generic polymorphic attachment.
 * entityType + entityId is a soft reference resolved by the service layer,
 * same pattern as trv_booking_services.referenceId, so one table serves
 * passenger passport scans, air ticket PDFs, visa scans, hotel/tour
 * confirmations, and supplier invoices without a table per entity.
 */
@Entity
@Table(name = "trv_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvDocument extends BaseEntity implements Serializable {

    public enum EntityType {
        BOOKING, PASSENGER, HOTEL_BOOKING, AIR_TICKET, VISA_APPLICATION,
        PACKAGE_BOOKING, TOUR_BOOKING, SUPPLIER_COST
    }

    public enum DocumentType {
        PASSPORT, VISA, TICKET, PHOTO, INVOICE, CONFIRMATION, RECEIPT, OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType = DocumentType.OTHER;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /** UUID-prefixed name actually written to disk — never trust/reuse the original name on disk. */
    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    /** Path relative to the configured upload root, e.g. "TRAVEL/PASSENGER/104/uuid_passport.pdf". */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "remarks", length = 500)
    private String remarks;

}
