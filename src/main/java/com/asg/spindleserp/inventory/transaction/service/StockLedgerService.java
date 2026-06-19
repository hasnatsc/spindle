package com.asg.spindleserp.inventory.transaction.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.transaction.dto.StockBalanceDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * StockLedgerService
 *
 * Read-only reporting on stock positions and transaction history.
 * All queries run directly via JdbcTemplate for performance.
 */
public interface StockLedgerService {

    /** DataTable-ready ledger of all InventoryTransaction rows for an item */
    DataTableResponse ledgerByItem(Long itemId, LocalDate from, LocalDate to,
                                   int draw, int start, int length, String search);

    /** DataTable-ready ledger for a warehouse */
    DataTableResponse ledgerByWarehouse(Long warehouseId, LocalDate from, LocalDate to,
                                        int draw, int start, int length, String search);

    /** Full ledger for the current org — used on the main stock page */
    DataTableResponse ledgerAll(Long orgId, LocalDate from, LocalDate to,
                                int draw, int start, int length, String search);

    /** Current stock balance per item × warehouse × lot */
    DataTableResponse balanceDatatable(Long orgId, Long warehouseId, Long itemId,
                                       int draw, int start, int length, String search);

    /** Current balance for a single item across all warehouses */
    List<StockBalanceDTO> balanceByItem(Long itemId);

    /** Current balance for a single warehouse across all items */
    List<StockBalanceDTO> balanceByWarehouse(Long warehouseId);

    /** Available qty for a specific item in a specific warehouse (for validation) */
    java.math.BigDecimal availableQty(Long itemId, Long warehouseId, Long lotId);
}
