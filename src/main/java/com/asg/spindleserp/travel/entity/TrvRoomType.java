package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "trv_room_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvRoomType extends BaseEntity {

    @Column(name = "room_type_name", nullable = false, length = 150)
    private String roomTypeName;

    @Column(name = "max_occupancy")
    private Integer maxOccupancy;

    @Column(name = "base_price", precision = 18, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "currency", length = 3)
    private String currency;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;
}
