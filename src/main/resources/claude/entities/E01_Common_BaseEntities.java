// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E01  Common Base + Enums  (v2 Generic Edition)            ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: common/entity/BaseEntity.java ──────────────────────────────────────
package com.hasnat.optimum.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseEntity {

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    protected String updatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;
}

// ── FILE: common/enums/ItemType.java ─────────────────────────────────────────
// ★ UPDATED v2: Fully generic — removed FIBER/YARN/FABRICS, CHEMICALS
package com.hasnat.optimum.common.enums;

public enum ItemType {
    RAW_MATERIAL,    // Direct production input (flour, steel, fabric, chemicals)
    SEMI_FINISHED,   // Intermediate / WIP (dough, cut fabric, sub-assemblies)
    FINISHED_GOOD,   // Sellable output (biscuit, garment, furniture)
    SERVICE,         // Non-physical (consulting, freight, labour)
    SPARE_PART,      // Machine / equipment spare parts
    CONSUMABLE,      // Low-value non-inventory (gloves, tape, oil, packaging)
    MRO,             // Maintenance, Repair & Operations supplies
    GENERAL,         // Miscellaneous / uncategorised
    FIXED_ASSET      // Capitalised plant & equipment
}

// ── FILE: common/enums/DocumentType.java ─────────────────────────────────────
// ★ UPDATED v2: Added PRODUCTION_ORDER and related production document types
package com.hasnat.optimum.common.enums;

public enum DocumentType {
    // Purchase cycle
    PURCHASE_REQUISITION,
    REQUEST_FOR_QUOTATION,
    COMPARATIVE_STATEMENT,
    PURCHASE_ORDER,
    GOODS_RECEIPT_NOTE,
    PURCHASE_INVOICE,
    // Sales cycle
    SALES_QUOTATION,
    SALES_ORDER,
    DELIVERY_ORDER,
    DELIVERY_CHALLAN,
    SALES_INVOICE,
    // Stock movements
    STORE_REQUISITION,
    MATERIAL_ISSUE,
    MATERIAL_RECEIVE,
    STOCK_TRANSFER,
    STOCK_ADJUSTMENT,
    // ★ Generic Production
    PRODUCTION_ORDER,
    PRODUCTION_REQUISITION,
    PRODUCTION_MATERIAL_ISSUE,
    FINISHED_GOODS_RECEIVE,
    // Credit / Debit
    DEBIT_NOTE,
    CREDIT_NOTE,
    // Commercial
    EXPORT_PROFORMA_INVOICE,
    IMPORT_PROFORMA_INVOICE,
    LETTER_OF_CREDIT
}

// ── FILE: common/enums/VoucherType.java ──────────────────────────────────────
// ★ UPDATED v2: Added PRODUCTION_VOUCHER
package com.hasnat.optimum.common.enums;

public enum VoucherType {
    JOURNAL_VOUCHER,
    PURCHASE_VOUCHER,
    SALES_VOUCHER,
    PAYMENT_VOUCHER,
    RECEIPT_VOUCHER,
    CONTRA_VOUCHER,
    EXPENSE_VOUCHER,
    DEBIT_NOTE,
    CREDIT_NOTE,
    PRODUCTION_VOUCHER   // ★ NEW – for production cost journal entries
}

// ── FILE: common/enums/MovementType.java ─────────────────────────────────────
package com.hasnat.optimum.common.enums;

public enum MovementType {
    // Inbound
    PURCHASE_RECEIPT,
    PRODUCTION_RECEIPT,
    TRANSFER_IN,
    ADJUSTMENT_IN,
    RETURN_FROM_CUSTOMER,
    // Outbound
    SALES_ISSUE,
    PRODUCTION_MATERIAL_ISSUE,
    TRANSFER_OUT,
    ADJUSTMENT_OUT,
    RETURN_TO_SUPPLIER,
    STORE_ISSUE
}

// ── FILE: common/enums/ApprovalStatus.java ───────────────────────────────────
package com.hasnat.optimum.common.enums;

public enum ApprovalStatus {
    DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, REJECTED, RETURNED, CANCELLED
}

// ── FILE: common/dto/ApiResponse.java ────────────────────────────────────────
package com.hasnat.optimum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private Obj<T>  obj;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Obj<T> {
        private T defaultData;
    }

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return ApiResponse.<T>builder()
            .success(true).message(msg)
            .obj(Obj.<T>builder().defaultData(data).build())
            .build();
    }

    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder().success(false).message(msg).build();
    }
}

// ── FILE: common/dto/DataTableResponse.java ──────────────────────────────────
package com.hasnat.optimum.common.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DataTableResponse {
    private long draw;
    private long recordsTotal;
    private long recordsFiltered;
    private List<Map<String, Object>> data;
    private String error;

    public static DataTableResponse of(long draw, long total, long filtered,
                                       List<Map<String, Object>> rows) {
        DataTableResponse r = new DataTableResponse();
        r.draw = draw; r.recordsTotal = total;
        r.recordsFiltered = filtered; r.data = rows;
        return r;
    }

    public static DataTableResponse error(long draw, String msg) {
        DataTableResponse r = new DataTableResponse();
        r.draw = draw; r.error = msg;
        return r;
    }
}
