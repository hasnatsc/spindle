package com.asg.spindleserp.organization.entity;

import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_context")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContext {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Long organizationId;
    private Long businessUnitId;
    private Long costCenterId;
    private Long warehouseId;

    @Column(length = 20)
    private String approvalDefaultView;
    private Boolean approvalDesktopNotification;
    private Boolean approvalEmailEnabled;
    private Boolean approvalPushEnabled;
    private Boolean approvalSmsEnabled;
    private Boolean approvalWhatsappEnabled;
    private Boolean approvalSoundEnabled;
    @Column(length = 20)
    private String approvalNotificationFrequency;
    private Integer approvalRefreshInterval;
    private Boolean showApprovalBadge;
    private Long lastViewedNotificationId;
}
