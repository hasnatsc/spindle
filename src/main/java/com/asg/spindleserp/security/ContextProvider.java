package com.asg.spindleserp.security;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ContextProvider {

//    private static UserContextHolder holder;
//    private static OrganizationRepository orgRepo;
//    private static BusinessUnitRepository businessUnitRepo;
//    private static CostCenterRepository costCenterRepo;
//    private static WarehouseRepository warehouseRepo;
//    private static UserRepository userRepo;
//
//    private final BusinessUnitRepository injectedBusinessUnitRepo;
//    private final CostCenterRepository injectedCostCenterRepo;
//    private final WarehouseRepository injectedWarehouseRepo;
//    private final UserContextHolder injectedHolder;
//    private final OrganizationRepository injectedOrgRepo;
//    private final UserRepository injectedUserRepo;

    public ContextProvider(
//            UserContextHolder holder,
//            OrganizationRepository orgRepo,
//            BusinessUnitRepository businessUnitRepo,
//            CostCenterRepository costCenterRepo,
//            WarehouseRepository warehouseRepo,
//            UserRepository userRepo
            )
            {
//
//        this.injectedHolder = holder;
//        this.injectedOrgRepo = orgRepo;
//        this.injectedBusinessUnitRepo = businessUnitRepo;
//        this.injectedCostCenterRepo = costCenterRepo;
//        this.injectedWarehouseRepo = warehouseRepo;
//        this.injectedUserRepo = userRepo;
    }

    @PostConstruct
    public void init() {
//        ContextProvider.holder = injectedHolder;
//        ContextProvider.orgRepo = injectedOrgRepo;
//        ContextProvider.businessUnitRepo = injectedBusinessUnitRepo;
//        ContextProvider.costCenterRepo = injectedCostCenterRepo;
//        ContextProvider.warehouseRepo = injectedWarehouseRepo;
//        ContextProvider.userRepo = injectedUserRepo;
    }

    // ======================= ORGANIZATION =======================

//    public static Long getOrganizationId() {
//        UserContextDTO ctx = getContext();
//        return ctx != null ? ctx.getOrganizationId() : null;
//    }
//
//    public static Organization getOrganizationReference() {
//        Long id = getOrganizationId();
//        return id != null ? orgRepo.getReferenceById(id) : null;
//    }
//
//    // ======================= BUSINESS UNIT =======================
//
//    public static Long getBusinessUnitId() {
//        UserContextDTO ctx = getContext();
//        return ctx != null ? ctx.getBusinessUnitId() : null;
//    }
//
//    public static BusinessUnit getBusinessUnitReference() {
//        Long id = getBusinessUnitId();
//        return id != null ? businessUnitRepo.getReferenceById(id) : null;
//    }
//
//    // ======================= COST CENTER =======================
//
//    public static Long getCostCenterId() {
//        UserContextDTO ctx = getContext();
//        return ctx != null ? ctx.getCostCenterId() : null;
//    }
//
//    public static CostCenter getCostCenterReference() {
//        Long id = getCostCenterId();
//        return id != null ? costCenterRepo.getReferenceById(id) : null;
//    }
//
//    // ======================= WAREHOUSE =======================
//
//    public static Long getWarehouseId() {
//        UserContextDTO ctx = getContext();
//        return ctx != null ? ctx.getWarehouseId() : null;
//    }
//
//    public static Warehouse getWarehouseReference() {
//        return warehouseRepo.findById(Objects.requireNonNull(getWarehouseId())).orElseThrow(() -> new ValidationException("Warehouse not found: " + getWarehouseId()));
//    }
//
//    // ======================= USER =======================
//
//    /** Returns the user's PK from the session context. */
//    public static Long getUserId() {
//        UserContextDTO ctx = getContext();
//        return ctx != null ? ctx.getUserId() : null;
//    }
//
//    /**
//     * Alias used by ApprovalServiceImpl — identical to getUserId().
//     * Kept separate so call-sites read naturally.
//     */
//    public static Long getCurrentUserId() {
//        return getUserId();
//    }
//
//    public static String getUsername() {
//        UserContextDTO ctx = getContext();
//        return ctx != null ? ctx.getUsername() : null;
//    }
//
//    public static String getCurrentUsername() {
//        UserContextDTO ctx = getContext();
//        return ctx != null && ctx.getUsername() != null
//                ? ctx.getUsername()
//                : "SYSTEM";
//    }
//
//    /**
//     * Returns a JPA reference proxy — cheap, no SQL hit.
//     * Use when you only need the entity for a FK relationship.
//     */
//    public static User getUserReference() {
//        Long id = getUserId();
//        return id != null ? userRepo.getReferenceById(id) : null;
//    }
//
//    /**
//     * Returns a fully loaded User entity from the DB.
//     * Use when you need to read actual field values (e.g. username, roles).
//     * Called as getCurrentUser() by ApprovalServiceImpl.
//     */
//    public static User getCurrentUser() {
//        Long id = getUserId();
//        return id != null ? userRepo.findById(id).orElse(null) : null;
//    }
//
//    /** Backward-compat alias for getCurrentUser(). */
//    public static User getUser() {
//        return getCurrentUser();
//    }
//
//    /**
//     * Returns the names of all roles assigned to the current user.
//     * Used by ApprovalServiceImpl for inbox role-based filtering.
//     *
//     * Returns role names exactly as stored in the DB (e.g. "ROLE_MANAGER").
//     * Returns an empty list (never null) when the user is not loaded.
//     */
//    public static List<String> getCurrentUserRoles() {
//        Long id = getUserId();
//        if (id == null) return Collections.emptyList();
//
//        User user = userRepo.findByIdWithRoles(id).orElse(null);
//        if (user == null || user.getRoles() == null) return Collections.emptyList();
//
//        return user.getRoles().stream()
//                .map(Role::getName)
//                .collect(Collectors.toList());
//    }

    // ======================= PRIVATE =======================

//    private static UserContextDTO getContext() {
//        return holder != null ? holder.get() : null;
//    }
}
