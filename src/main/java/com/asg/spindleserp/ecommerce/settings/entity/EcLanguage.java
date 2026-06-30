package com.asg.spindleserp.ecommerce.settings.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_languages",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_language",
                columnNames = "language_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String languageCode;
    @Column(length = 100)
    private String languageName;
    @Column(length = 100)
    private String nativeName;

    @Builder.Default
    @Column(nullable = false)
    private boolean rtl = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean defaultLanguage = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
