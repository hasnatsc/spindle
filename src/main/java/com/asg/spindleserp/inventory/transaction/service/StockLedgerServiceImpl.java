package com.asg.spindleserp.inventory.transaction.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.global.repository.InventoryStockBalanceRepository;
import com.asg.spindleserp.inventory.transaction.dto.StockBalanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockLedgerServiceImpl implements StockLedgerService {

    private final InventoryStockBalanceRepository balanceRepo;
    private final JdbcTemplate                   jdbcTemplate;

    // ═════════════════════════════════════════════════════════════════════════
    // LEDGER QUERIES
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public DataTableResponse ledgerByItem(Long itemId, LocalDate from, LocalDate to,
                                          int draw, int start, int length, String search) {
        String extra = " AND t.item_id = " + itemId;
        return ledgerQuery(draw, start, length, search, null, from, to, extra);
    }

    @Override
    public DataTableResponse ledgerByWarehouse(Long warehouseId, LocalDate from, LocalDate to,
                                               int draw, int start, int length, String search) {
        String extra = " AND t.warehouse_id = " + warehouseId;
        return ledgerQuery(draw, start, length, search, null, from, to, extra);
    }

    @Override
    public DataTableResponse ledgerAll(Long orgId, LocalDate from, LocalDate to,
                                       int draw, int start, int length, String search) {
        return ledgerQuery(draw, start, length, search, orgId, from, to, "");
    }

    private DataTableResponse ledgerQuery(int draw, int start, int length, String search,
                                          Long orgId, LocalDate from, LocalDate to, String extraWhere) {
        String where = "WHERE 1=1"
                + (orgId != null ? " AND t.organization_id = " + orgId : "")
                + (from != null ? " AND t.transaction_date >= '" + from + "'" : "")
                + (to   != null ? " AND t.transaction_date <= '" + to   + "'" : "")
                + extraWhere
                + CommonUtils.searchILike(search, Arrays.asList(
                        "i.item_code", "i.item_name", "w.warehouse_name",
                        "t.movement_type", "d.document_no", "l.lot_number"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY t.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                t.id,
                d.document_no,
                t.document_type,
                t.movement_type,
                t.transaction_date,
                w.warehouse_name,
                i.item_code,
                i.item_name,
                i.unit_of_measure                            AS unit_code,
                COALESCE(l.lot_number, '—')                 AS lot_number,
                t.quantity,
                COALESCE(t.unit_cost::text, '—')            AS unit_cost,
                COALESCE(t.total_cost::text, '—')           AS total_cost,
                t.balance_after,
                COALESCE(t.remarks, '—')                    AS remarks,
                TO_CHAR(t.created_at, 'DD-Mon-YYYY HH24:MI') AS created_at,
                COALESCE(t.organization_id::text, '')        AS org_id
            FROM  global_inventory_transactions t
            JOIN  global_business_documents     d ON d.id   = t.business_document_id
            JOIN  inv_items                     i ON i.id   = t.item_id
            JOIN  org_warehouses                w ON w.id   = t.warehouse_id
            LEFT  JOIN global_inv_lots          l ON l.id   = t.lot_id
            %s
            ORDER BY t.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STOCK BALANCE
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public DataTableResponse balanceDatatable(Long orgId, Long warehouseId, Long itemId,
                                              int draw, int start, int length, String search) {
        String where = "WHERE 1=1"
                + (orgId      != null ? " AND i.organization_id = " + orgId      : "")
                + (warehouseId != null ? " AND s.warehouse_id = " + warehouseId  : "")
                + (itemId     != null ? " AND s.item_id = " + itemId             : "")
                + " AND s.quantity > 0"
                + CommonUtils.searchILike(search, Arrays.asList(
                        "i.item_code", "i.item_name", "w.warehouse_name", "l.lot_number"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY i.item_code, w.warehouse_name) AS sl,
                COUNT(*)     OVER ()                                        AS full_count,
                s.id,
                i.item_code,
                i.item_name,
                i.item_type,
                i.unit_of_measure                                           AS unit_code,
                w.warehouse_code,
                w.warehouse_name,
                COALESCE(l.lot_number, '—')                               AS lot_number,
                s.quantity,
                s.reserved_quantity,
                (s.quantity - s.reserved_quantity)                         AS available_quantity,
                COALESCE(s.average_cost::text, '—')                       AS average_cost,
                COALESCE(s.stock_value::text, '—')                        AS stock_value,
                TO_CHAR(s.last_transaction_time, 'DD-Mon-YYYY HH24:MI')  AS last_transaction_time
            FROM  global_inventory_stock_balances s
            JOIN  inv_items   i ON i.id = s.item_id
            JOIN  org_warehouses w ON w.id = s.warehouse_id
            LEFT  JOIN global_inv_lots l ON l.id = s.lot_id
            %s
            ORDER BY i.item_code, w.warehouse_name
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public List<StockBalanceDTO> balanceByItem(Long itemId) {
        String sql = """
            SELECT s.id, i.item_code, i.item_name, i.item_type, i.unit_of_measure AS unit_code,
                   w.warehouse_code, w.warehouse_name, l.lot_number,
                   s.quantity, s.reserved_quantity, (s.quantity - s.reserved_quantity) AS available_quantity,
                   s.average_cost, s.stock_value,
                   TO_CHAR(s.last_transaction_time, 'DD-Mon-YYYY HH24:MI') AS last_transaction_time
            FROM  global_inventory_stock_balances s
            JOIN  inv_items   i ON i.id = s.item_id
            JOIN  org_warehouses w ON w.id = s.warehouse_id
            LEFT  JOIN global_inv_lots l ON l.id = s.lot_id
            WHERE s.item_id = ? AND s.quantity > 0
            ORDER BY w.warehouse_name
            """;
        return jdbcTemplate.query(sql, (rs, n) -> StockBalanceDTO.builder()
                .id(rs.getLong("id"))
                .itemCode(rs.getString("item_code"))
                .itemName(rs.getString("item_name"))
                .itemType(rs.getString("item_type"))
                .unitCode(rs.getString("unit_code"))
                .warehouseCode(rs.getString("warehouse_code"))
                .warehouseName(rs.getString("warehouse_name"))
                .lotNumber(rs.getString("lot_number"))
                .quantity(rs.getBigDecimal("quantity"))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .availableQuantity(rs.getBigDecimal("available_quantity"))
                .averageCost(rs.getBigDecimal("average_cost"))
                .stockValue(rs.getBigDecimal("stock_value"))
                .lastTransactionTime(rs.getString("last_transaction_time"))
                .build(), itemId);
    }

    @Override
    public List<StockBalanceDTO> balanceByWarehouse(Long warehouseId) {
        String sql = """
            SELECT s.id, i.item_code, i.item_name, i.item_type, i.unit_of_measure AS unit_code,
                   w.warehouse_code, w.warehouse_name, l.lot_number,
                   s.quantity, s.reserved_quantity, (s.quantity - s.reserved_quantity) AS available_quantity,
                   s.average_cost, s.stock_value,
                   TO_CHAR(s.last_transaction_time, 'DD-Mon-YYYY HH24:MI') AS last_transaction_time
            FROM  global_inventory_stock_balances s
            JOIN  inv_items   i ON i.id = s.item_id
            JOIN  org_warehouses w ON w.id = s.warehouse_id
            LEFT  JOIN global_inv_lots l ON l.id = s.lot_id
            WHERE s.warehouse_id = ? AND s.quantity > 0
            ORDER BY i.item_code
            """;
        return jdbcTemplate.query(sql, (rs, n) -> StockBalanceDTO.builder()
                .id(rs.getLong("id"))
                .itemCode(rs.getString("item_code"))
                .itemName(rs.getString("item_name"))
                .itemType(rs.getString("item_type"))
                .unitCode(rs.getString("unit_code"))
                .warehouseCode(rs.getString("warehouse_code"))
                .warehouseName(rs.getString("warehouse_name"))
                .lotNumber(rs.getString("lot_number"))
                .quantity(rs.getBigDecimal("quantity"))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .availableQuantity(rs.getBigDecimal("available_quantity"))
                .averageCost(rs.getBigDecimal("average_cost"))
                .stockValue(rs.getBigDecimal("stock_value"))
                .lastTransactionTime(rs.getString("last_transaction_time"))
                .build(), warehouseId);
    }

    @Override
    public BigDecimal availableQty(Long itemId, Long warehouseId, Long lotId) {
        return balanceRepo.findByItemIdAndWarehouseIdAndLotId(itemId, warehouseId, lotId)
                .map(b -> b.getQuantity().subtract(b.getReservedQuantity()))
                .orElse(BigDecimal.ZERO);
    }
}
