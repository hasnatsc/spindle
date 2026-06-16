package com.asg.spindleserp.security.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleDTO {

    private Long id;

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name cannot exceed 100 characters")
    private String name;          // stored with ROLE_ prefix; UI strips it for display

    @Size(max = 250)
    private String nameBn;

    @Size(max = 255)
    private String description;

    @Size(max = 60)
    private String masterRole;

    @Builder.Default
    private boolean active = true;

    // Permissions assigned to this role (IDs for write, names for display)
    @Builder.Default
    private Set<Long>   permissionIds   = new HashSet<>();
    @Builder.Default
    private Set<String> permissionNames = new HashSet<>();

    private Integer permissionCount;
    private Integer userCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Display-friendly name (strips ROLE_ prefix) */
    public String getDisplayName() {
        return name != null ? name.replace("ROLE_", "") : "";
    }
}
