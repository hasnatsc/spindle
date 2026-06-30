// Path: com/asg/spindleserp/ecommerce/dto/EcSettingDTO.java
package com.asg.spindleserp.ecommerce.settings.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcSettingDTO {
    private Long   id;
    @NotBlank @Size(max = 100)
    private String settingGroup;
    @NotBlank @Size(max = 150)
    private String settingKey;
    private String settingValue;
    private String dataType;     // STRING / NUMBER / BOOLEAN / JSON
    private String description;
    private Boolean editable;

    // For bulk save: list of settings in a group
    @Builder.Default
    private List<EcSettingDTO> settings = new ArrayList<>();
}
