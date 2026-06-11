package com.asg.spindleserp.hrm.pims;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_documents",
        indexes = @Index(name = "idx_edoc_emp", columnList = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDocument implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // NID|PASSPORT|TIN|BIRTH_CERTIFICATE|CERTIFICATE|CONTRACT

    @Column(name = "file_url", length = 500)
    private String fileUrl;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(length = 500)
    private String remarks;
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
