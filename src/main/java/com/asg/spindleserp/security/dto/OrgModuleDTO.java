package com.asg.spindleserp.security.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrgModuleDTO — returned by OrgModuleService for the module management UI.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgModuleDTO {

    private Long            id;
    private Long            organizationId;
    private String          organizationName;
    private String          moduleKey;          // e.g. "HRM"
    private String          moduleDisplayName;  // e.g. "Human Resources"
    private boolean         active;
    private String          grantedBy;
    private LocalDateTime   grantedAt;
    private String          revokedBy;
    private LocalDateTime   revokedAt;
    private String          notes;

    // ── Org-level summary (used on the org detail page) ───────────────────────
    /** Convenience list: ALL modules with their active/inactive state for one org. */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrgModuleSummary {
        private Long           organizationId;
        private String         organizationName;
        private List<OrgModuleDTO> modules;   // one entry per known ModuleKey
    }
}
