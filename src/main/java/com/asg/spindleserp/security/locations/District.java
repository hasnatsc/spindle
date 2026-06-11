package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District extends BaseReferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private State state;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "name_bn", length = 100)
    private String nameBn;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
