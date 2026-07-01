package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_hotel_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "description", length = 500)
    private String description;
}
