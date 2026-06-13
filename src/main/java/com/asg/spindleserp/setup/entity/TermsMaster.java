package com.asg.spindleserp.setup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_terms_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false, length = 50)
    private String documentType;

    @Builder.Default
    private boolean isActive = true;
    @Builder.Default
    private boolean isDefault = false;
    private Integer sortOrder;
}
