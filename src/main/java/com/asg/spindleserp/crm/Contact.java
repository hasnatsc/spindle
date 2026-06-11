package com.asg.spindleserp.crm;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_contacts",
        indexes = {
                @Index(name = "idx_con_org", columnList = "organization_id"),
                @Index(name = "idx_con_customer", columnList = "customer_id"),
                @Index(name = "idx_con_lead", columnList = "lead_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private SubAccount customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    @Column(length = 100)
    private String email;
    @Column(length = 20)
    private String phone;
    @Column(length = 20)
    private String mobile;
    @Column(name = "job_title", length = 100)
    private String jobTitle;
    @Column(length = 100)
    private String department;
    @Builder.Default
    @Column(name = "is_primary_contact", nullable = false)
    private Boolean isPrimaryContact = false;
    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
