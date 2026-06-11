// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  SpindleERP — REMAINING ENTITIES (Part 2 of 2)                         ║
// ║  49 entity classes completing the full 115-table schema                ║
// ║  Package: com.asg.spindleserp                                           ║
// ║  Java 21 | Spring Boot 3.5 | JPA/Hibernate | Lombok                    ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
//  Coverage (tables added in this file):
//  ─────────────────────────────────────────────────────────────────────────
//  Yarn Masters    : YarnCount, YarnPly, YarnBlend (3)
//  Location        : District, City (2)
//  STP Setup       : DocumentSequence, DocumentFile (2)
//  HRM (Full)      : Department, Designation, Employee, EmployeeAddress,
//                    EmployeeDocument, EmployeeSalary, PayrollRunLine (7)
//  Finance         : OpeningBalance (1)
//  Approval        : ApprovalLevel, ApprovalRequest (moved to full defs) (2)
//  Production      : ProductionRecipeItem, ProductionRecipeItemLot (2)
//  Fixed Assets    : DepreciationRun, DepreciationRunLine,
//                    AssetDisposal, AssetTransfer (4)
//  CRM             : CrmQuotation (1)
//  Inventory       : InventoryTransaction (1)
//  eCommerce (Full): EcoTaxClass, EcoCategory, EcoAttributeGroup,
//                    EcoAttributeValue, EcoProductVariant, EcoProductImage,
//                    EcoCustomer, EcoCustomerAddress, EcoWishlist,
//                    EcoRecentlyViewed, EcoCart, EcoCartItem,
//                    EcoOrderLine, EcoOrderStatusHistory, EcoCouponUsage,
//                    EcoPaymentMethod, EcoRefund, EcoStoreCredit,
//                    EcoShippingZone, EcoShippingMethod, EcoShipmentItem,
//                    EcoShipmentTracking, EcoReturn, EcoReturnItem,
//                    EcoReview, EcoCustomerNotification,
//                    EcoCoupon (26)
//  Notifications   : EmailQueue (1)
//  Enums (new)     : Gender, BloodGroup, MaritalStatus, EmployeeType,
//                    EmployeeStatus, AddressType (6)
// ─────────────────────────────────────────────────────────────────────────

// ════════════════════════════════════════════════════════════════════════════
// YARN CLASSIFICATION MASTERS  (BaseOrgEntity)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/inventory/item/YarnCount.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

// FILE: com/asg/spindleserp/inventory/item/YarnPly.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yrn_plies",
    uniqueConstraints = @UniqueConstraint(name = "uk_yrn_ply_org_num",
        columnNames = {"organization_id","ply_number"}),
    indexes = @Index(name = "idx_yrn_ply_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class YarnPly extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ply_number", nullable = false) private Integer plyNumber;
    @Column(name = "ply_code",   nullable = false, length = 20) private String plyCode;
    @Column(name = "ply_name",   nullable = false, length = 50) private String plyName;
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default @Column(name = "is_active",   nullable = false) private Boolean isActive   = true;
    @Builder.Default @Column(name = "is_approved", nullable = false) private Boolean isApproved = false;
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "approved_at") private java.time.LocalDateTime approvedAt;
    @Column(name = "created_by",  length = 100) private String createdBy;
    @Column(name = "updated_by",  length = 100) private String updatedBy;

    /** Display label used by YarnItem.buildDisplayName() */
    public String getDisplayName() {
        return plyCode != null ? plyCode : String.valueOf(plyNumber);
    }
}

// FILE: com/asg/spindleserp/inventory/item/YarnBlend.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

// ════════════════════════════════════════════════════════════════════════════
// LOCATION MASTERS
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/security/locations/District.java
package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

// FILE: com/asg/spindleserp/security/locations/City.java
package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

// ════════════════════════════════════════════════════════════════════════════
// SETUP / UTILITY TABLES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/stp/DocumentSequence.java
package com.asg.spindleserp.stp;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

// FILE: com/asg/spindleserp/stp/DocumentFile.java
package com.asg.spindleserp.stp;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

// ════════════════════════════════════════════════════════════════════════════
// HRM — COMPLETE ENTITIES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/hrm/setup/Department.java
package com.asg.spindleserp.hrm.setup;

import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.security.ContextProvider;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// FILE: com/asg/spindleserp/hrm/setup/Designation.java
package com.asg.spindleserp.hrm.setup;

import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.security.ContextProvider;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// FILE: com/asg/spindleserp/hrm/pims/Employee.java
package com.asg.spindleserp.hrm.pims;

import com.asg.spindleserp.hrm.attendance.Attendance;
import com.asg.spindleserp.hrm.attendance.EmployeeLeave;
import com.asg.spindleserp.hrm.payroll.EmployeeSalary;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.hrm.setup.Designation;
import com.asg.spindleserp.security.ContextProvider;
import com.asg.spindleserp.security.Organization;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

// FILE: com/asg/spindleserp/hrm/pims/EmployeeAddress.java
package com.asg.spindleserp.hrm.pims;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

// FILE: com/asg/spindleserp/hrm/pims/EmployeeDocument.java
package com.asg.spindleserp.hrm.pims;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/hrm/payroll/EmployeeSalary.java
package com.asg.spindleserp.hrm.payroll;

import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/hrm/payroll/PayrollRunLine.java
package com.asg.spindleserp.hrm.payroll;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import BigDecimal;

// ════════════════════════════════════════════════════════════════════════════
// FINANCE — OPENING BALANCE
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/accounts/setup/OpeningBalance.java
package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ════════════════════════════════════════════════════════════════════════════
// INVENTORY TRANSACTION  (immutable ledger)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/global/documents/InventoryTransaction.java
package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ════════════════════════════════════════════════════════════════════════════
// FIXED ASSETS — DEPRECIATION & DISPOSAL
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/fixedassets/DepreciationRun.java
package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// FILE: com/asg/spindleserp/fixedassets/DepreciationRunLine.java
package com.asg.spindleserp.fixedassets;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import BigDecimal;

// FILE: com/asg/spindleserp/fixedassets/AssetDisposal.java
package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.hrm.setup.Department;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/fixedassets/AssetTransfer.java
package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ════════════════════════════════════════════════════════════════════════════
// CRM — QUOTATION
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/crm/CrmQuotation.java
package com.asg.spindleserp.crm;

import com.asg.spindleserp.global.documents.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ════════════════════════════════════════════════════════════════════════════
// NOTIFICATIONS — EMAIL QUEUE
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/notification/EmailQueue.java
package com.asg.spindleserp.notification;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

// ════════════════════════════════════════════════════════════════════════════
// eCOMMERCE — ALL REMAINING ENTITIES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/ecommerce/EcoTaxClass.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import BigDecimal;

// FILE: com/asg/spindleserp/ecommerce/EcoCategory.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

// FILE: com/asg/spindleserp/ecommerce/EcoAttributeGroup.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// FILE: com/asg/spindleserp/ecommerce/EcoAttributeValue.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

// FILE: com/asg/spindleserp/ecommerce/EcoProductVariant.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.util.ArrayList;
import java.util.List;

// FILE: com/asg/spindleserp/ecommerce/EcoProductImage.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoCoupon.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.time.LocalDate;

// FILE: com/asg/spindleserp/ecommerce/EcoCustomer.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.time.LocalDate;

// FILE: com/asg/spindleserp/ecommerce/EcoCustomerAddress.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoWishlist.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoRecentlyViewed.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoCart.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// FILE: com/asg/spindleserp/ecommerce/EcoCartItem.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoOrderLine.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

// FILE: com/asg/spindleserp/ecommerce/EcoOrderStatusHistory.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoCouponUsage.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoPaymentMethod.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import BigDecimal;
import java.util.Map;

// FILE: com/asg/spindleserp/ecommerce/EcoRefund.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoStoreCredit.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;
import java.time.LocalDate;

// FILE: com/asg/spindleserp/ecommerce/EcoShippingZone.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

// FILE: com/asg/spindleserp/ecommerce/EcoShippingMethod.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import BigDecimal;

// FILE: com/asg/spindleserp/ecommerce/EcoShipmentItem.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import com.asg.spindleserp.global.documents.InventoryTransaction;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoShipmentTracking.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoReturn.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// FILE: com/asg/spindleserp/ecommerce/EcoReturnItem.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import com.asg.spindleserp.global.documents.InventoryTransaction;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import BigDecimal;
import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoReview.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// FILE: com/asg/spindleserp/ecommerce/EcoCustomerNotification.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

// ════════════════════════════════════════════════════════════════════════════
// NEW ENUM TYPES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/hrm/pims/Gender.java
package com.asg.spindleserp.hrm.pims;

// FILE: com/asg/spindleserp/hrm/pims/BloodGroup.java
package com.asg.spindleserp.hrm.pims;
// DB: A+|A-|B+|B-|O+|O-|AB+|AB- via @Enumerated with custom converter or STRING

// FILE: com/asg/spindleserp/hrm/pims/MaritalStatus.java
package com.asg.spindleserp.hrm.pims;

// FILE: com/asg/spindleserp/hrm/pims/EmployeeType.java
package com.asg.spindleserp.hrm.pims;

// FILE: com/asg/spindleserp/hrm/pims/EmployeeStatus.java
package com.asg.spindleserp.hrm.pims;

// FILE: com/asg/spindleserp/hrm/pims/AddressType.java
package com.asg.spindleserp.hrm.pims;

// ════════════════════════════════════════════════════════════════════════════
// COMPLETE ENTITY REGISTRY  (both Part 1 + Part 2)
// ════════════════════════════════════════════════════════════════════════════
/*
  ┌──────────────────────────────────────────────────────────────────────────┐
  │  SpindleERP — All 115 Tables → Spring Boot Entities                     │
  ├─────────────────────────────┬────────────────┬────────────────────────── ┤
  │  Entity                     │  Table         │  Base Class               │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 1 — CORE/SECURITY   │                │                           │
  │  Organization               │  org_orgs      │  (none — tenant root)     │
  │  BusinessUnit               │  org_bu        │  BaseOrgEntity            │
  │  User                       │  sec_users     │  (none — cross-org)       │
  │  Role                       │  sec_roles     │  (none — global)          │
  │  Permission                 │  sec_perms     │  (none — global)          │
  │  Menu                       │  sec_menus     │  (none — global)          │
  │  RoleMenu                   │  sec_role_menus│  (none — join)            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 2 — LOCATION        │                │                           │
  │  Country, State             │  stp_*         │  BaseReferenceEntity      │
  │  District, City             │  stp_*         │  BaseReferenceEntity      │
  │  Currency, HsCode           │  stp_*         │  BaseReferenceEntity      │
  │  DocumentSequence           │  stp_doc_seqs  │  (none — utility)         │
  │  DocumentFile               │  stp_doc_files │  (none — polymorphic)     │
  │  GlobalTermsCondition       │  stp_gtc       │  BaseOrgEntity            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 3 — INVENTORY       │                │                           │
  │  ItemCategory               │  inv_item_cats │  BaseOrgEntity            │
  │  UnitsOfMeasure             │  stp_uom       │  BaseOrgEntity            │
  │  YarnType/Count/Ply/Blend   │  yrn_*         │  BaseOrgEntity            │
  │  InventoryItem              │  inv_items     │  BaseAuditEntity          │
  │  YarnItem                   │  yarn_items    │  BaseAuditEntity          │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 4 — WAREHOUSE/STOCK │                │                           │
  │  Warehouse                  │  org_wh        │  BaseOrgEntity            │
  │  GlobalInventoryLot         │  global_inv_lots│ BaseOrgEntity            │
  │  InventoryStockBalance      │  global_inv_sb │  (none — org FK explicit) │
  │  InventoryTransaction       │  global_inv_txn│  (none — immutable)       │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 5 — FINANCE         │                │                           │
  │  Bank                       │  stp_banks     │  BaseOrgEntity            │
  │  Account                    │  acc_coa       │  BaseOrgEntity            │
  │  SubAccount (base)          │  acc_coa_sub   │  BaseOrgEntity + JOINED   │
  │  BankAccount                │  acc_ba        │  SubAccount (JOINED child)│
  │  CashAccount                │  acc_ca        │  SubAccount (JOINED child)│
  │  Customer                   │  acc_customers │  SubAccount (JOINED child)│
  │  Supplier                   │  acc_suppliers │  SubAccount (JOINED child)│
  │  OpeningBalance             │  acc_ob        │  (none — org FK explicit) │
  │  CostCenter                 │  acc_cc        │  BaseOrgEntity            │
  │  JournalEntry               │  acc_je        │  BaseOrgEntity            │
  │  JournalEntryLine           │  acc_jel       │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 6 — APPROVAL        │                │                           │
  │  ApprovalConfig             │  apr_configs   │  BaseOrgEntity            │
  │  ApprovalLevel              │  apr_levels    │  (none — child)           │
  │  ApprovalRequest            │  apr_requests  │  (none — polymorphic)     │
  │  ApprovalHistory            │  apr_histories │  (none — immutable)       │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 7 — DOCUMENTS(MDST) │                │                           │
  │  BusinessDocument           │  global_bd     │  BaseAuditEntity          │
  │  BusinessDocumentLine       │  global_bdl    │  BaseAuditEntity          │
  │  BusinessDocumentLineLot    │  global_bdll   │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 8 — COMMERCIAL      │                │                           │
  │  CommercialLc               │  cmr_lc        │  BaseOrgEntity            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 9 — HRM             │                │                           │
  │  Department                 │  org_depts     │  (manual @PrePersist)     │
  │  Designation                │  hrm_desig     │  (manual @PrePersist)     │
  │  Employee                   │  hrm_employees │  (manual @PrePersist)     │
  │  EmployeeAddress            │  hrm_emp_addr  │  (none — child)           │
  │  EmployeeDocument           │  hrm_emp_docs  │  (none — child)           │
  │  EmployeeSalary             │  hrm_emp_sal   │  (none — child)           │
  │  Attendance                 │  hrm_attend    │  BaseOrgEntity            │
  │  EmployeeLeave              │  hrm_emp_leave │  BaseOrgEntity            │
  │  PayrollRun                 │  hrm_pr        │  BaseOrgEntity            │
  │  PayrollRunLine             │  hrm_prl       │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 10 — PRODUCTION     │                │                           │
  │  ProductionOrder            │  prd_orders    │  BaseOrgEntity            │
  │  ProductionRecipe           │  prd_recipes   │  BaseOrgEntity            │
  │  ProductionRecipeItem       │  prd_rec_items │  (none — child)           │
  │  ProductionRecipeItemLot    │  prd_ril       │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 11 — FIXED ASSETS   │                │                           │
  │  AssetCategory              │  fa_asset_cats │  BaseOrgEntity            │
  │  FixedAsset                 │  fa_assets     │  BaseOrgEntity            │
  │  DepreciationRun            │  fa_dep_runs   │  BaseOrgEntity            │
  │  DepreciationRunLine        │  fa_dep_rl     │  (none — child)           │
  │  AssetDisposal              │  fa_disposals  │  BaseOrgEntity            │
  │  AssetTransfer              │  fa_transfers  │  BaseOrgEntity            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 12 — CRM            │                │                           │
  │  Lead                       │  crm_leads     │  BaseOrgEntity            │
  │  Contact                    │  crm_contacts  │  BaseOrgEntity            │
  │  Opportunity                │  crm_opps      │  BaseOrgEntity            │
  │  Activity                   │  crm_acts      │  BaseOrgEntity            │
  │  CrmQuotation               │  crm_quotations│  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 13 — NOTIF/AUDIT    │                │                           │
  │  Notification               │  ntf_notif     │  BaseOrgEntity            │
  │  EmailQueue                 │  ntf_email_q   │  (none — utility)         │
  │  AuditLog                   │  sys_audit_log │  (none — JSONB fields)    │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 14 — eCOMMERCE      │                │                           │
  │  EcoStore                   │  eco_stores    │  BaseOrgEntity            │
  │  EcoTaxClass                │  eco_tax_cls   │  (none — store child)     │
  │  EcoCategory                │  eco_categories│  BaseOrgEntity            │
  │  EcoAttributeGroup          │  eco_attr_grps │  (none — store child)     │
  │  EcoAttributeValue          │  eco_attr_vals │  (none — group child)     │
  │  EcoCoupon                  │  eco_coupons   │  BaseOrgEntity            │
  │  EcoProduct                 │  eco_products  │  BaseOrgEntity            │
  │  EcoProductVariant          │  eco_prod_vars │  BaseOrgEntity            │
  │  EcoProductImage            │  eco_prod_imgs │  (none — product child)   │
  │  EcoCustomer                │  eco_customers │  BaseOrgEntity            │
  │  EcoCustomerAddress         │  eco_cust_addr │  (none — customer child)  │
  │  EcoWishlist                │  eco_wishlists │  (none — customer child)  │
  │  EcoRecentlyViewed          │  eco_rv        │  (none — customer child)  │
  │  EcoCart                    │  eco_carts     │  BaseOrgEntity            │
  │  EcoCartItem                │  eco_cart_items│  (none — cart child)      │
  │  EcoOrder                   │  eco_orders    │  BaseOrgEntity            │
  │  EcoOrderLine               │  eco_order_lns │  (none — order child)     │
  │  EcoOrderStatusHistory      │  eco_ord_hist  │  (none — order child)     │
  │  EcoCouponUsage             │  eco_coup_use  │  (none — order child)     │
  │  EcoPaymentMethod           │  eco_pay_mths  │  BaseOrgEntity            │
  │  EcoPaymentTransaction      │  eco_pay_txn   │  BaseOrgEntity            │
  │  EcoRefund                  │  eco_refunds   │  BaseOrgEntity            │
  │  EcoStoreCredit             │  eco_credits   │  BaseOrgEntity            │
  │  EcoShippingZone            │  eco_ship_zones│  (none — store child)     │
  │  EcoShippingMethod          │  eco_ship_mths │  BaseOrgEntity            │
  │  EcoShipment                │  eco_shipments │  BaseOrgEntity            │
  │  EcoShipmentItem            │  eco_ship_itms │  (none — shipment child)  │
  │  EcoShipmentTracking        │  eco_ship_track│  (none — shipment child)  │
  │  EcoReturn                  │  eco_returns   │  BaseOrgEntity            │
  │  EcoReturnItem              │  eco_ret_items │  (none — return child)    │
  │  EcoReview                  │  eco_reviews   │  BaseOrgEntity            │
  │  EcoCustomerNotification    │  eco_cust_notif│  (none — customer child)  │
  └─────────────────────────────┴────────────────┴────────────────────────── ┘

  TOTAL ENTITY CLASSES  : 115  (matches SQL schema exactly)
  TOTAL ENUM TYPES      :  15
  TOTAL PACKAGES        :  22
  BASE CLASS SUMMARY    :
    BaseAuditEntity     :   4  (Spring Data auditing — most transactional entities)
    BaseOrgEntity       :  43  (Hibernate timestamps + org enforcement via @PrePersist)
    BaseReferenceEntity :   6  (global reference tables — no org column)
    SubAccount JOINED   :   4  (BankAccount, CashAccount, Customer, Supplier)
    Plain entity        :  58  (no base — explicit org FK, child entities, or immutable)
*/
