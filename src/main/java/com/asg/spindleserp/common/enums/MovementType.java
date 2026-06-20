package com.asg.spindleserp.common.enums;

public enum MovementType {
    // Inbound
    PURCHASE_RECEIPT,
    SUPPLIER_RETURN,
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
