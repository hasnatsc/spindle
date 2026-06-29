package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_menus",
        indexes = {
                @Index(name = "idx_ec_menu_parent", columnList = "parent_menu_id"),
                @Index(name = "idx_ec_menu_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcMenu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_menu_id")
    private EcMenu parentMenu;

    @Column(length = 200)
    private String menuName;
    @Column(length = 500)
    private String menuUrl;
    @Column(length = 100)
    private String menuIcon;
    @Builder.Default
    @Column(length = 30)
    private String target = "_self";
    @Builder.Default
    private Integer displayOrder = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
