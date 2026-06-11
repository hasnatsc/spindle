package com.asg.spindleserp.stp;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

/**
 * DocumentSequence — per-org, per-prefix, per-year-month counter.
 * Used by fn_next_doc_no() via JDBC and also accessible from JPA.
 * Always use fn_next_doc_no() stored function for thread-safe increment.
 */
@Entity
@Table(name = "stp_document_sequences",
        uniqueConstraints = @UniqueConstraint(name = "uk_doc_seq",
                columnNames = {"organization_id", "prefix", "year_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSequence implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 20)
    private String prefix;
    @Column(name = "year_month", nullable = false, length = 4)
    private String yearMonth;  // YYMM
    @Builder.Default
    @Column(name = "last_seq", nullable = false)
    private Integer lastSeq = 0;
}
