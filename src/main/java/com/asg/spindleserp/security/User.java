package com.asg.spindleserp.security;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sec_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"roles", "organizations", "allowedBusinessUnits", "allowedWarehouses", "allowedCostCenters"})
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "first_name", length = 100)
    private String firstName;
    @Column(name = "last_name", length = 100)
    private String lastName;
    @Column(length = 100)
    private String email;
    @Column(length = 20)
    private String phone;
    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked = true;
    @Builder.Default
    @Column(name = "account_non_expired", nullable = false)
    private Boolean accountNonExpired = true;
    @Builder.Default
    @Column(name = "credentials_non_expired", nullable = false)
    private Boolean credentialsNonExpired = true;
    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sec_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_organizations",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "organization_id"))
    @Builder.Default
    private Set<Organization> organizations = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_business_units",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "business_unit_id"))
    @Builder.Default
    private Set<BusinessUnit> allowedBusinessUnits = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_warehouses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "warehouse_id"))
    @Builder.Default
    private Set<Warehouse> allowedWarehouses = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_cost_centers",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "cost_center_id"))
    @Builder.Default
    private Set<CostCenter> allowedCostCenters = new HashSet<>();

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
