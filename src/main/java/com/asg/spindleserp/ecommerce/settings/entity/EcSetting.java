package com.asg.spindleserp.ecommerce.settings.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

// =============================================================================
// EC SETTINGS  (key-value store per org)
// =============================================================================

@Entity
@Table(name = "ec_settings",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_setting",
                columnNames = {"organization_id", "setting_group", "setting_key"}),
        indexes = @Index(name = "idx_ec_setting_group", columnList = "organization_id,setting_group"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcSetting extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100) private String settingGroup;
    @Column(length = 150) private String settingKey;
    @Column(columnDefinition = "text") private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DataType dataType;

    @Column(columnDefinition = "text") private String description;
    @Builder.Default @Column(nullable = false) private boolean editable = true;

    public enum DataType { STRING, NUMBER, BOOLEAN, JSON }
}