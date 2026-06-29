package com.asg.spindleserp.ecommerce.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_order_notes",
        indexes = @Index(name = "idx_ec_ordernote_order", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcOrderNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NoteType noteType;

    @Column(columnDefinition = "text")
    private String note;
    @Column(length = 100)
    private String createdBy;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum NoteType {CUSTOMER, ADMIN, SYSTEM}
}
