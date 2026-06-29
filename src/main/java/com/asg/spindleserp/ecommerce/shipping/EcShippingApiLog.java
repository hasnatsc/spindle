package com.asg.spindleserp.ecommerce.shipping;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_shipping_api_logs",
        indexes = {
                @Index(name = "idx_ec_shipapi_ship", columnList = "shipping_id"),
                @Index(name = "idx_ec_shipapi_time", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcShippingApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_id")
    private EcShipping shipping;

    @Column(length = 100)
    private String apiName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String responsePayload;

    @Column(length = 50)
    private String responseCode;
    private Boolean success;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
