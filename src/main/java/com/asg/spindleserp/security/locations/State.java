package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class State extends BaseReferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;
    @Column(length = 10)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
