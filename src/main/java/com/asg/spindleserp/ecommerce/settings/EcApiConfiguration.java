package com.asg.spindleserp.ecommerce.settings;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_api_configurations",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_api_config",
                columnNames = {"organization_id", "api_name"}),
        indexes = @Index(name = "idx_ec_apiconfig_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcApiConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String apiName;
    @Column(length = 700)
    private String apiUrl;
    @Column(columnDefinition = "text")
    private String apiKey;     // ENCRYPT IN PRODUCTION
    @Column(columnDefinition = "text")
    private String apiSecret;  // ENCRYPT IN PRODUCTION
    @Column(length = 200)
    private String username;
    @Column(columnDefinition = "text")
    private String password;   // ENCRYPT IN PRODUCTION
    @Column(length = 700)
    private String webhookUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean sandboxMode = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
