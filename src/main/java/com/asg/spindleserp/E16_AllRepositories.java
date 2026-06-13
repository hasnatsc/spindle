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
package com.asg.spindleserp;
import com.hasnat.optimum.security.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

// ═══════════════════════════════════════════════════════════════════════════
// ORGANIZATION
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.organization.repository;
import com.hasnat.optimum.organization.entity.*;

// ═══════════════════════════════════════════════════════════════════════════
// SETUP
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.setup.repository;
import com.hasnat.optimum.setup.entity.*;
import org.springframework.data.repository.query.Param;

// ═══════════════════════════════════════════════════════════════════════════
// INVENTORY  (★ YarnType/YarnCount/YarnProduct repositories REMOVED)
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.inventory.repository;
import com.hasnat.optimum.common.enums.ItemType;
import com.hasnat.optimum.inventory.entity.*;
import Repository;

// ═══════════════════════════════════════════════════════════════════════════
// ACCOUNTS
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.accounts.repository;
import com.hasnat.optimum.accounts.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import Repository;
import java.util.*;

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

// ═══════════════════════════════════════════════════════════════════════════
// APPROVAL
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.approval.repository;
import com.hasnat.optimum.approval.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import Repository;
import java.util.*;

// ═══════════════════════════════════════════════════════════════════════════
// GLOBAL DOCUMENTS + STOCK
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.global.repository;
import com.hasnat.optimum.common.enums.DocumentType;
import com.hasnat.optimum.global.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import Repository;
import java.util.*;

// ═══════════════════════════════════════════════════════════════════════════
// PRODUCTION  (★ COMPLETELY NEW — replaces yarn production repositories)
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.production.repository;
import com.hasnat.optimum.production.entity.*;
import com.hasnat.optimum.production.entity.Production.ProductionStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

// ═══════════════════════════════════════════════════════════════════════════
// HRM  (★ Added CostCenterAllocationRepository)
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.hrm.repository;
import com.hasnat.optimum.hrm.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import Repository;
import java.time.LocalDate;
import java.util.*;

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
import Repository;
import java.util.*;

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

// ═══════════════════════════════════════════════════════════════════════════
// BUDGET
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.budget.repository;
import com.hasnat.optimum.budget.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import Repository;
import java.util.*;

@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    Optional<FiscalYear> findByOrganizationIdAndYearCode(Long orgId, String code);
    Optional<FiscalYear> findByOrganizationIdAndIsCurrentTrue(Long orgId);
    List<FiscalYear>     findByOrganizationIdAndStatus(Long orgId, FiscalYear.FiscalYearStatus status);
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

// ═══════════════════════════════════════════════════════════════════════════
// CRM
// ═══════════════════════════════════════════════════════════════════════════
package com.hasnat.optimum.crm.repository;
import com.hasnat.optimum.crm.entity.*;
import org.springframework.data.jpa.repository.*;
import Repository;
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
import Repository;
import java.util.*;

