// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E16  All Repositories  (v2 Generic Edition)               ║
// ║  Pattern: JpaRepository<Entity, Long> + JpaSpecificationExecutor         ║
// ║  ★ REMOVED: YarnTypeRepository, YarnCountRepository, YarnProductRepository║
// ║  ★ ADDED:   BomRepository, ProductionRepository,                         ║
// ║             ProductionInputRepository, ProductionOutputRepository         ║
// ║             CostCenterAllocationRepository                               ║
// ║  Total: 79 repositories                                                  ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ═══════════════════════════════════════════════════════════════════════════
// SECURITY
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.security.repository;
import com.hasnat.optimum.security.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    List<Role>     findByActiveTrue();
    boolean        existsByName(String name);
}

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    List<Permission>     findByModule(String module);
    List<Permission>     findByActiveTrue();
    boolean              existsByName(String name);
}

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean        existsByUsername(String username);
    boolean        existsByEmail(String email);
    boolean        existsByPhone(String phone);
    List<User>     findByOrganizationIdAndDeletedFalse(Long orgId);
    long           countByOrganizationIdAndDeletedFalse(Long orgId);
}

@Repository
public interface AppMenuRepository extends JpaRepository<AppMenu, Long> {
    List<AppMenu>    findByParentIdIsNullAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();
    List<AppMenu>    findByParentIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(Long parentId);
    Optional<AppMenu> findByMenuCode(String menuCode);
    List<AppMenu>    findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();
}

@Repository
public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Long> {
    List<RoleMenuAccess>    findByRoleId(Long roleId);
    Optional<RoleMenuAccess> findByRoleIdAndMenuId(Long roleId, Long menuId);
    void deleteByRoleId(Long roleId);
}

// ═══════════════════════════════════════════════════════════════════════════
// ORGANIZATION
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.organization.repository;
import com.hasnat.optimum.organization.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByCode(String code);
    List<Organization>     findByIsActiveTrue();
    boolean                existsByCode(String code);
}

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long> {
    List<BusinessUnit>    findByOrganizationIdAndIsActiveTrue(Long orgId);
    Optional<BusinessUnit> findByOrganizationIdAndCode(Long orgId, String code);
    boolean               existsByOrganizationIdAndCode(Long orgId, String code);
}

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long>, JpaSpecificationExecutor<CostCenter> {
    Optional<CostCenter> findByCostCenterCode(String code);
    List<CostCenter>     findByBusinessUnitIdAndIsActiveTrueOrderByCostCenterName(Long buId);
    List<CostCenter>     findByBusinessUnitOrganizationIdAndIsActiveTrue(Long orgId);
    List<CostCenter>     findByParentCostCenterIdIsNullAndBusinessUnitOrganizationId(Long orgId);
    boolean              existsByCostCenterCode(String code);
}

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse>    findByBusinessUnitIdAndIsActiveTrue(Long buId);
    List<Warehouse>    findByBusinessUnitOrganizationIdAndIsActiveTrue(Long orgId);
    Optional<Warehouse> findByWarehouseCode(String code);
    boolean            existsByWarehouseCode(String code);
}

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {
    List<Department>    findByOrganizationIdAndActiveTrue(Long orgId);
    Optional<Department> findByCode(String code);
    Optional<Department> findByName(String name);
    List<Department>    findByParentDepartmentIdIsNullAndOrganizationId(Long orgId);
    boolean             existsByCode(String code);
}

// ═══════════════════════════════════════════════════════════════════════════
// SETUP
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.setup.repository;
import com.hasnat.optimum.setup.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    Optional<Currency> findByCode(String code);
    List<Currency>     findByActiveTrue();
}

@Repository
public interface BankRepository extends JpaRepository<Bank, Long>, JpaSpecificationExecutor<Bank> {
    Optional<Bank> findByOrganizationIdAndBankCode(Long orgId, String bankCode);
    List<Bank>     findByOrganizationIdAndIsActiveTrue(Long orgId);
    List<Bank>     findByOrganizationIdAndIsActiveTrueAndSupportsLcTrue(Long orgId);
    boolean        existsByOrganizationIdAndBankCode(Long orgId, String bankCode);
}

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {
    Optional<DocumentSequence> findByOrganizationIdAndPrefixAndYearCode(
        Long orgId, String prefix, String yearCode);

    @Modifying
    @Query("UPDATE DocumentSequence ds SET ds.lastSeq = ds.lastSeq + 1 " +
           "WHERE ds.organizationId = :orgId AND ds.prefix = :prefix AND ds.yearCode = :yearCode")
    void increment(@Param("orgId") Long orgId,
                   @Param("prefix") String prefix,
                   @Param("yearCode") String yearCode);
}

@Repository
public interface TermsMasterRepository extends JpaRepository<TermsMaster, Long> {
    List<TermsMaster> findByDocumentTypeAndIsActiveTrue(String documentType);
}

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {
    List<DocumentFile> findByDocumentTypeAndReferenceId(String docType, Long refId);
    void deleteByDocumentTypeAndReferenceId(String docType, Long refId);
}

// ═══════════════════════════════════════════════════════════════════════════
// INVENTORY  (★ YarnType/YarnCount/YarnProduct repositories REMOVED)
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.inventory.repository;
import com.hasnat.optimum.common.enums.ItemType;
import com.hasnat.optimum.inventory.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    List<ItemCategory>    findByOrganizationIdAndIsActiveTrue(Long orgId);
    List<ItemCategory>    findByOrganizationIdAndItemTypeAndIsActiveTrue(Long orgId, ItemType type);
    List<ItemCategory>    findByParentCategoryIdIsNullAndOrganizationId(Long orgId);
    Optional<ItemCategory> findByOrganizationIdAndCategoryCode(Long orgId, String code);
    boolean               existsByOrganizationIdAndCategoryCode(Long orgId, String code);
}

@Repository
public interface ItemUomRepository extends JpaRepository<ItemUom, Long> {
    List<ItemUom>    findByOrganizationIdAndActive(Long orgId, boolean active);
    Optional<ItemUom> findByOrganizationIdAndCode(Long orgId, String code);
    boolean          existsByOrganizationIdAndCode(Long orgId, String code);
}

@Repository
public interface ItemBrandRepository extends JpaRepository<ItemBrand, Long> {
    List<ItemBrand>    findByOrganizationIdAndIsActiveTrue(Long orgId);
    Optional<ItemBrand> findByOrganizationIdAndBrandCode(Long orgId, String code);
    boolean            existsByOrganizationIdAndBrandCode(Long orgId, String code);
}

@Repository
public interface ItemModelRepository extends JpaRepository<ItemModel, Long> {
    List<ItemModel> findByOrganizationIdAndIsActiveTrue(Long orgId);
    List<ItemModel> findByBrandIdAndIsActiveTrue(Long brandId);
    boolean         existsByOrganizationIdAndBrandIdAndModelCode(Long orgId, Long brandId, String code);
}

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    Optional<Item> findByOrganizationIdAndItemCode(Long orgId, String code);
    List<Item>     findByOrganizationIdAndItemTypeAndIsActiveTrue(Long orgId, ItemType type);
    boolean        existsByOrganizationIdAndItemCode(Long orgId, String code);
    boolean        existsByOrganizationIdAndItemName(Long orgId, String name);

    @Query("SELECT i FROM Item i WHERE i.organizationId = :orgId AND i.isActive = true " +
           "AND (LOWER(i.itemCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR   LOWER(i.itemName) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Item> search(@Param("orgId") Long orgId, @Param("q") String q, Pageable p);

    // Commonly needed for production BOM dropdowns — only raw materials and consumables
    @Query("SELECT i FROM Item i WHERE i.organizationId = :orgId AND i.isActive = true " +
           "AND i.itemType IN ('RAW_MATERIAL','SEMI_FINISHED','CONSUMABLE','MRO')")
    List<Item> findProductionInputItems(@Param("orgId") Long orgId);

    // Only finished goods — for production output and sales
    @Query("SELECT i FROM Item i WHERE i.organizationId = :orgId AND i.isActive = true " +
           "AND i.itemType IN ('FINISHED_GOOD','SEMI_FINISHED')")
    List<Item> findFinishedItems(@Param("orgId") Long orgId);
}

// ═══════════════════════════════════════════════════════════════════════════
// ACCOUNTS
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.accounts.repository;
import com.hasnat.optimum.accounts.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long>, JpaSpecificationExecutor<ChartOfAccount> {
    Optional<ChartOfAccount> findByAccountCode(String code);
    List<ChartOfAccount>     findByOrganizationIdAndIsActiveTrue(Long orgId);
    List<ChartOfAccount>     findByOrganizationIdAndAccountType(Long orgId, ChartOfAccount.AccountType type);
    List<ChartOfAccount>     findByParentAccountIdIsNullAndOrganizationId(Long orgId);
    boolean                  existsByAccountCode(String code);
    boolean                  existsByOrganizationIdAndAccountCode(Long orgId, String code);
}

@Repository
public interface ChartOfAccountSubRepository extends JpaRepository<ChartOfAccountSub, Long>, JpaSpecificationExecutor<ChartOfAccountSub> {
    Optional<ChartOfAccountSub> findBySubAccountCode(String code);
    List<ChartOfAccountSub>     findByOrganizationIdAndSubAccountTypeAndIsActiveTrue(Long orgId, String type);
    List<ChartOfAccountSub>     findByOrganizationIdAndIsActiveTrue(Long orgId);

    @Query("SELECT s FROM ChartOfAccountSub s WHERE s.organizationId = :orgId " +
           "AND s.subAccountType = :type AND s.isActive = true " +
           "AND (LOWER(s.subAccountCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR   LOWER(s.subAccountName) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<ChartOfAccountSub> search(@Param("orgId") Long orgId,
                                    @Param("type") String type,
                                    @Param("q") String q);
}

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {
    List<AccountingPeriod>    findByOrganizationIdAndIsActiveTrue(Long orgId);
    Optional<AccountingPeriod> findByOrganizationIdAndIsActiveTrueAndIsClosedFalse(Long orgId);
    List<AccountingPeriod>    findByOrganizationIdAndFiscalYear(Long orgId, int year);
}

@Repository
public interface JournalEntryMasterRepository extends JpaRepository<JournalEntryMaster, Long>, JpaSpecificationExecutor<JournalEntryMaster> {
    Optional<JournalEntryMaster> findByVoucherNo(String voucherNo);
    List<JournalEntryMaster>     findByOrganizationIdAndIsPosted(Long orgId, boolean posted);
    long                         countByOrganizationIdAndIsPosted(Long orgId, boolean posted);
}

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, Long> {
    List<JournalEntryLine> findByJournalEntryId(Long journalEntryId);
    List<JournalEntryLine> findByAccountId(Long accountId);
    List<JournalEntryLine> findBySubAccountId(Long subAccountId);
}

@Repository
public interface AccountsMappingRepository extends JpaRepository<AccountsMapping, Long> {
    Optional<AccountsMapping> findByOrganizationIdAndMappingCode(Long orgId, String code);
    List<AccountsMapping>     findByOrganizationIdAndModuleTypeAndIsActiveTrue(Long orgId, String module);
    Optional<AccountsMapping> findByOrganizationIdAndTransactionTypeAndIsDefaultTrue(Long orgId, String txType);
}

@Repository
public interface AccountsPolicyRepository extends JpaRepository<AccountsPolicy, Long> {
    Optional<AccountsPolicy> findByOrganizationIdAndPolicyCode(Long orgId, String code);
    Optional<AccountsPolicy> findByOrganizationIdAndModuleTypeAndIsDefaultTrue(Long orgId, String module);
}

// ═══════════════════════════════════════════════════════════════════════════
// APPROVAL
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.approval.repository;
import com.hasnat.optimum.approval.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ApprovalConfigRepository extends JpaRepository<ApprovalConfig, Long> {
    Optional<ApprovalConfig> findByCode(String code);
    List<ApprovalConfig>     findByOrganizationIdAndDocumentTypeAndIsActiveTrue(Long orgId, String docType);
}

@Repository
public interface ApprovalLevelRepository extends JpaRepository<ApprovalLevel, Long> {
    List<ApprovalLevel> findByApprovalConfigIdOrderByLevelNumberAsc(Long configId);
}

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long>, JpaSpecificationExecutor<ApprovalRequest> {
    Optional<ApprovalRequest> findByReferenceIdAndDocumentType(Long refId, String docType);
    List<ApprovalRequest>     findByCurrentApproverUserIdAndStatus(Long userId, String status);

    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.organizationId = :orgId " +
           "AND ar.status IN ('IN_APPROVAL','SUBMITTED') " +
           "AND ar.currentApproverUser.id = :userId")
    List<ApprovalRequest> findInbox(@Param("orgId") Long orgId, @Param("userId") Long userId);
}

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    List<ApprovalHistory> findByApprovalRequestIdOrderByActionAtDesc(Long requestId);
}

@Repository
public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, Long> {
    List<ApprovalDelegation> findByDelegatorIdAndStatus(Long delegatorId, String status);
    List<ApprovalDelegation> findByDelegateIdAndStatus(Long delegateId, String status);
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOBAL DOCUMENTS + STOCK
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.global.repository;
import com.hasnat.optimum.common.enums.DocumentType;
import com.hasnat.optimum.global.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long>, JpaSpecificationExecutor<InventoryLot> {
    Optional<InventoryLot> findByOrganizationIdAndLotNumber(Long orgId, String lotNumber);
    List<InventoryLot>     findByItemIdAndStatus(Long itemId, String status);
    List<InventoryLot>     findByOrganizationIdAndItemIdAndDeletedFalse(Long orgId, Long itemId);
    boolean                existsByOrganizationIdAndLotNumber(Long orgId, String lotNumber);

    // ★ Find production lots (lots created from a production order)
    List<InventoryLot>     findByProductionOrderId(Long productionOrderId);
}

@Repository
public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, Long>, JpaSpecificationExecutor<BusinessDocument> {
    Optional<BusinessDocument> findByDocumentNo(String documentNo);
    List<BusinessDocument>     findByOrganizationIdAndDocumentTypeAndIsDeletedFalse(
                                   Long orgId, DocumentType type);
    List<BusinessDocument>     findByParentDocumentIdAndIsDeletedFalse(Long parentId);

    @Query("SELECT bd FROM BusinessDocument bd WHERE bd.organization.id = :orgId " +
           "AND bd.documentType = :type AND bd.isDeleted = false " +
           "AND LOWER(bd.documentNo) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<BusinessDocument> search(@Param("orgId") Long orgId,
                                   @Param("type") DocumentType type,
                                   @Param("q") String q,
                                   Pageable pageable);
}

@Repository
public interface BusinessDocumentLineRepository extends JpaRepository<BusinessDocumentLine, Long> {
    List<BusinessDocumentLine> findByDocumentIdOrderByLineNumber(Long docId);
    List<BusinessDocumentLine> findByItemId(Long itemId);
}

@Repository
public interface BusinessDocumentLineLotRepository extends JpaRepository<BusinessDocumentLineLot, Long> {
    List<BusinessDocumentLineLot> findByDocumentLineId(Long lineId);
    List<BusinessDocumentLineLot> findByLotId(Long lotId);
}

@Repository
public interface InventoryStockBalanceRepository extends JpaRepository<InventoryStockBalance, Long> {
    Optional<InventoryStockBalance> findByItemIdAndWarehouseIdAndLotId(
        Long itemId, Long warehouseId, Long lotId);
    List<InventoryStockBalance>     findByItemIdAndQuantityGreaterThan(Long itemId, java.math.BigDecimal qty);
    List<InventoryStockBalance>     findByWarehouseId(Long warehouseId);

    @Query("SELECT s FROM InventoryStockBalance s WHERE s.item.organizationId = :orgId AND s.quantity > 0")
    List<InventoryStockBalance> findAllPositiveByOrg(@Param("orgId") Long orgId);
}

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByItemIdOrderByCreatedAtDesc(Long itemId);
    List<InventoryTransaction> findByBusinessDocumentId(Long docId);
    List<InventoryTransaction> findByOrganizationIdAndTransactionDate(
        Long orgId, java.time.LocalDate date);
}

// ═══════════════════════════════════════════════════════════════════════════
// PRODUCTION  (★ COMPLETELY NEW — replaces yarn production repositories)
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.production.repository;
import com.hasnat.optimum.production.entity.*;
import com.hasnat.optimum.production.entity.Production.ProductionStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Repository
public interface BomRepository extends JpaRepository<Bom, Long>, JpaSpecificationExecutor<Bom> {
    Optional<Bom>  findByOrganizationIdAndBomCode(Long orgId, String code);
    List<Bom>      findByOrganizationIdAndIsActiveTrue(Long orgId);
    List<Bom>      findByFinishedItemIdAndIsActiveTrue(Long itemId);
    Optional<Bom>  findByFinishedItemIdAndIsDefaultTrue(Long itemId);
    boolean        existsByOrganizationIdAndBomCode(Long orgId, String code);
}

@Repository
public interface BomItemRepository extends JpaRepository<BomItem, Long> {
    List<BomItem> findByBomIdOrderByLineNumber(Long bomId);
    List<BomItem> findByRawItemId(Long rawItemId);
}

@Repository
public interface ProductionRepository extends JpaRepository<Production, Long>, JpaSpecificationExecutor<Production> {
    Optional<Production> findByOrganizationIdAndProductionNo(Long orgId, String no);
    List<Production>     findByOrganizationIdAndStatus(Long orgId, ProductionStatus status);
    List<Production>     findByOrganizationIdAndFinishedItemId(Long orgId, Long itemId);
    List<Production>     findByCostCenterId(Long costCenterId);
    boolean              existsByOrganizationIdAndProductionNo(Long orgId, String no);

    // For labor cost proportioning
    @Query("SELECT COALESCE(SUM(p.producedQuantity), 0) FROM Production p " +
           "WHERE p.costCenter.id = :costCenterId " +
           "AND FUNCTION('TO_CHAR', p.productionDate, 'YYYY-MM') = :month " +
           "AND p.status = 'COMPLETED'")
    BigDecimal sumProducedQtyByCostCenterAndMonth(
        @Param("costCenterId") Long costCenterId,
        @Param("month") String month);
}

@Repository
public interface ProductionInputRepository extends JpaRepository<ProductionInput, Long> {
    List<ProductionInput> findByProductionIdOrderByLineNumber(Long productionId);
    List<ProductionInput> findByRawItemId(Long rawItemId);
    List<ProductionInput> findByLotId(Long lotId);

    // Sum material cost for a production — after all inputs recorded
    @Query("SELECT COALESCE(SUM(pi.totalCost), 0) FROM ProductionInput pi " +
           "WHERE pi.production.id = :productionId")
    java.math.BigDecimal sumTotalCostByProduction(@Param("productionId") Long productionId);
}

@Repository
public interface ProductionOutputRepository extends JpaRepository<ProductionOutput, Long> {
    List<ProductionOutput> findByProductionIdOrderByLineNumber(Long productionId);
    List<ProductionOutput> findByFinishedItemId(Long itemId);
    List<ProductionOutput> findByLotId(Long lotId);
}

// ═══════════════════════════════════════════════════════════════════════════
// HRM  (★ Added CostCenterAllocationRepository)
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.hrm.repository;
import com.hasnat.optimum.hrm.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {
    List<Designation>    findByOrganizationIdAndIsActiveTrue(Long orgId);
    Optional<Designation> findByOrganizationIdAndDesignationCode(Long orgId, String code);
    boolean              existsByOrganizationIdAndDesignationCode(Long orgId, String code);
}

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeCode(String code);
    Optional<Employee> findByPhone(String phone);
    Optional<Employee> findByNationalId(String nationalId);
    Optional<Employee> findByUserId(Long userId);
    List<Employee>     findByOrganizationIdAndStatus(Long orgId, Employee.EmployeeStatus status);
    List<Employee>     findByOrganizationIdAndDepartmentId(Long orgId, Long deptId);
    List<Employee>     findByReportingManagerId(Long managerId);
    long               countByOrganizationIdAndStatus(Long orgId, Employee.EmployeeStatus status);
    boolean            existsByEmployeeCode(String code);

    @Query("SELECT e FROM Employee e WHERE e.organizationId = :orgId AND e.status = 'ACTIVE' " +
           "AND (LOWER(e.firstName) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR   LOWER(e.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR   LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Employee> search(@Param("orgId") Long orgId, @Param("q") String q, Pageable p);
}

@Repository
public interface EmployeeAddressRepository extends JpaRepository<EmployeeAddress, Long> {
    List<EmployeeAddress>    findByEmployeeId(Long empId);
    Optional<EmployeeAddress> findByEmployeeIdAndAddressType(Long empId, EmployeeAddress.AddressType type);
}

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeIdAndAttDate(Long empId, LocalDate date);
    List<Attendance>     findByOrganizationIdAndAttDate(Long orgId, LocalDate date);
    List<Attendance>     findByEmployeeIdAndAttDateBetween(Long empId, LocalDate from, LocalDate to);
    long countByEmployeeIdAndAttDateBetweenAndStatus(
        Long empId, LocalDate from, LocalDate to, Attendance.AttendanceStatus status);
}

@Repository
public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long>, JpaSpecificationExecutor<EmployeeLeave> {
    List<EmployeeLeave> findByEmployeeIdAndStatus(Long empId, EmployeeLeave.LeaveStatus status);
    List<EmployeeLeave> findByOrganizationIdAndStatus(Long orgId, EmployeeLeave.LeaveStatus status);
}

@Repository
public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Long> {
    Optional<EmployeeSalary> findByEmployeeIdAndIsCurrentTrue(Long empId);
    List<EmployeeSalary>     findByEmployeeIdOrderByEffectiveDateDesc(Long empId);
}

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long>, JpaSpecificationExecutor<PayrollRun> {
    Optional<PayrollRun> findByOrganizationIdAndPayrollMonth(Long orgId, String month);
    List<PayrollRun>     findByOrganizationIdAndStatus(Long orgId, PayrollRun.PayrollStatus status);
}

@Repository
public interface PayrollRunLineRepository extends JpaRepository<PayrollRunLine, Long> {
    List<PayrollRunLine>    findByPayrollRunId(Long runId);
    Optional<PayrollRunLine> findByPayrollRunIdAndEmployeeId(Long runId, Long empId);
}

/** ★ NEW in v2: Labor cost → production cost center linking */
@Repository
public interface CostCenterAllocationRepository extends JpaRepository<CostCenterAllocation, Long> {
    List<CostCenterAllocation> findByCostCenterIdAndAllocationMonth(Long costCenterId, String month);
    List<CostCenterAllocation> findByPayrollRunId(Long payrollRunId);
    List<CostCenterAllocation> findByEmployeeIdAndAllocationMonth(Long empId, String month);

    // Total labor cost for a cost center in a given month — used by ProductionCostService
    @Query("SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM CostCenterAllocation a " +
           "WHERE a.costCenter.id = :costCenterId AND a.allocationMonth = :month")
    java.math.BigDecimal sumAllocatedAmountByCostCenterAndMonth(
        @Param("costCenterId") Long costCenterId,
        @Param("month") String month);
}

// ═══════════════════════════════════════════════════════════════════════════
// FIXED ASSETS
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.fixedassets.repository;
import com.hasnat.optimum.fixedassets.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    List<AssetCategory>    findByOrganizationIdAndIsActiveTrue(Long orgId);
    List<AssetCategory>    findByParentIdIsNullAndOrganizationId(Long orgId);
    Optional<AssetCategory> findByOrganizationIdAndCode(Long orgId, String code);
    boolean                existsByOrganizationIdAndCode(Long orgId, String code);
}

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
    Optional<Asset> findByOrganizationIdAndAssetCode(Long orgId, String code);
    List<Asset>     findByOrganizationIdAndStatus(Long orgId, Asset.AssetStatus status);
    long            countByOrganizationIdAndStatus(Long orgId, Asset.AssetStatus status);
    boolean         existsByOrganizationIdAndAssetCode(Long orgId, String code);
}

@Repository
public interface DepreciationRunRepository extends JpaRepository<DepreciationRun, Long> {
    List<DepreciationRun>    findByOrganizationIdAndStatus(Long orgId, DepreciationRun.RunStatus status);
    Optional<DepreciationRun> findTopByOrganizationIdAndStatusOrderByRunDateDesc(
        Long orgId, DepreciationRun.RunStatus status);
}

@Repository
public interface DepreciationRunLineRepository extends JpaRepository<DepreciationRunLine, Long> {
    List<DepreciationRunLine> findByDepreciationRunId(Long runId);
    List<DepreciationRunLine> findByAssetId(Long assetId);
}

@Repository
public interface AssetDisposalRepository extends JpaRepository<AssetDisposal, Long> {
    Optional<AssetDisposal> findByAssetId(Long assetId);
}

// ═══════════════════════════════════════════════════════════════════════════
// BUDGET
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.budget.repository;
import com.hasnat.optimum.budget.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    Optional<FiscalYear> findByOrganizationIdAndYearCode(Long orgId, String code);
    Optional<FiscalYear> findByOrganizationIdAndIsCurrentTrue(Long orgId);
    List<FiscalYear>     findByOrganizationIdAndStatus(Long orgId, FiscalYear.FiscalYearStatus status);
}

@Repository
public interface BudgetHeadRepository extends JpaRepository<BudgetHead, Long> {
    List<BudgetHead>    findByOrganizationIdAndIsActiveTrue(Long orgId);
    List<BudgetHead>    findByOrganizationIdAndHeadType(Long orgId, BudgetHead.HeadType type);
    List<BudgetHead>    findByParentIdIsNullAndOrganizationId(Long orgId);
    Optional<BudgetHead> findByOrganizationIdAndHeadCode(Long orgId, String code);
    boolean             existsByOrganizationIdAndHeadCode(Long orgId, String code);
}

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long>, JpaSpecificationExecutor<Budget> {
    Optional<Budget> findByOrganizationIdAndBudgetNo(Long orgId, String no);
    List<Budget>     findByOrganizationIdAndStatus(Long orgId, Budget.BudgetStatus status);
    List<Budget>     findByFiscalYearIdAndOrganizationId(Long fyId, Long orgId);
    boolean          existsByOrganizationIdAndBudgetNo(Long orgId, String no);
}

@Repository
public interface BudgetLineRepository extends JpaRepository<BudgetLine, Long> {
    List<BudgetLine> findByBudgetIdOrderByLineNumber(Long budgetId);
    List<BudgetLine> findByBudgetIdAndBudgetHeadId(Long budgetId, Long headId);

    @Query(value = "SELECT id, available_amount FROM bgt_budget_lines WHERE budget_id = :budgetId",
           nativeQuery = true)
    List<Object[]> findAvailableAmounts(@Param("budgetId") Long budgetId);
}

@Repository
public interface BudgetActualRepository extends JpaRepository<BudgetActual, Long> {
    List<BudgetActual> findByBudgetLineId(Long lineId);
    List<BudgetActual> findByJournalEntryId(Long journalEntryId);
}

@Repository
public interface EncumbranceRepository extends JpaRepository<Encumbrance, Long> {
    List<Encumbrance> findByBudgetLineIdAndStatus(Long lineId, Encumbrance.EncumbranceStatus status);
    List<Encumbrance> findBySourceDocumentId(Long docId);

    @Query(value = "SELECT id, outstanding_amount FROM bgt_encumbrances " +
                   "WHERE budget_line_id = :lineId AND status IN ('OPEN','PARTIAL')",
           nativeQuery = true)
    List<Object[]> findOutstandingByLine(@Param("lineId") Long lineId);
}

@Repository
public interface BudgetNoteRepository extends JpaRepository<BudgetNote, Long> {
    List<BudgetNote> findByBudgetIdOrderByCreatedAtDesc(Long budgetId);
}

// ═══════════════════════════════════════════════════════════════════════════
// CRM
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.crm.repository;
import com.hasnat.optimum.crm.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    Optional<Lead> findByOrganizationIdAndLeadNo(Long orgId, String no);
    List<Lead>     findByOrganizationIdAndStatus(Long orgId, Lead.LeadStatus status);
    List<Lead>     findByAssignedToId(Long userId);
    boolean        existsByOrganizationIdAndLeadNo(Long orgId, String no);
}

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    Optional<Opportunity> findByOrganizationIdAndOpportunityNo(Long orgId, String no);
    List<Opportunity>     findByOrganizationIdAndStage(Long orgId, Opportunity.OpportunityStage stage);
    boolean               existsByOrganizationIdAndOpportunityNo(Long orgId, String no);
}

@Repository
public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {
    List<CrmActivity> findByOpportunityIdOrderByActivityDateDesc(Long oppId);
    List<CrmActivity> findByAssignedToIdAndStatus(Long userId, CrmActivity.ActivityStatus status);
}

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact>    findByCustomerIdAndIsActiveTrue(Long customerId);
    Optional<Contact> findByCustomerIdAndIsPrimaryTrue(Long customerId);
}

@Repository
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long>, JpaSpecificationExecutor<CustomerFeedback> {
    List<CustomerFeedback> findByCustomerIdAndStatus(Long customerId, CustomerFeedback.FeedbackStatus status);
    List<CustomerFeedback> findByOrganizationIdAndStatus(Long orgId, CustomerFeedback.FeedbackStatus status);
}

// ═══════════════════════════════════════════════════════════════════════════
// NOTIFICATION + AUDIT
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.notification.repository;
import com.hasnat.optimum.notification.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    long               countByRecipientIdAndIsReadFalse(Long userId);
    List<Notification> findByReferenceTypeAndReferenceId(String refType, Long refId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllReadByUser(Long userId);
}

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<AuditLog> findByOrganizationIdAndActionOrderByCreatedAtDesc(Long orgId, String action);
}
