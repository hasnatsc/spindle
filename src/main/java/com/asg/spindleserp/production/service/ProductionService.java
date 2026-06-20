package com.asg.spindleserp.production.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.production.dto.*;
import com.asg.spindleserp.production.entity.*;

import java.util.List;
import java.util.Map;

/**
 * ProductionService — unified interface for the Production module.
 *
 * Covers:
 *   BOM (Bill of Materials) — master templates
 *   Production Work Orders  — DRAFT → SUBMITTED → APPROVED → RELEASED
 *                             → IN_PROGRESS → COMPLETED
 *                             (or REJECTED / CANCELLED)
 *
 * On COMPLETE:
 *   1. Sum ProductionInput.totalCost → Production.materialCost
 *   2. Recalculate totalCost, unitCost
 *   3. Post PRODUCTION_ISSUE stock transactions for each input (raw material OUT)
 *   4. Create InventoryLot for each output with unitCost = Production.unitCost
 *   5. Post FINISHED_GOODS_RECEIVE stock transactions for each output (FG IN)
 *   6. Update producedQuantity on the Production header
 */
public interface ProductionService {

    // ── BOM ───────────────────────────────────────────────────────────────────

    BomDTO createBom(BomDTO dto);
    BomDTO updateBom(Long id, BomDTO dto);
    BomDTO findBomById(Long id);
    void   deleteBom(Long id);
    BomDTO toggleBom(Long id);
    DataTableResponse bomDatatable(int draw, int start, int length, String search);
    Map<String, Object> searchBoms(String q, int page);
    BomDTO toDTO(Bom entity);

    // ── Production Work Order ─────────────────────────────────────────────────

    ProductionDTO createProduction(ProductionDTO dto);
    ProductionDTO updateProduction(Long id, ProductionDTO dto);
    ProductionDTO findProductionById(Long id);
    void          deleteProduction(Long id);

    // ── Status transitions ────────────────────────────────────────────────────

    ProductionDTO submitProduction(Long id);
    ProductionDTO approveProduction(Long id);
    ProductionDTO releaseProduction(Long id);
    ProductionDTO startProduction(Long id);
    ProductionDTO completeProduction(Long id);
    ProductionDTO rejectProduction(Long id, String remarks);
    ProductionDTO cancelProduction(Long id, String remarks);

    DataTableResponse productionDatatable(int draw, int start, int length, String search, String status);
    Map<String, Object> searchProductions(String q, int page);

    /**
     * Pre-fill production inputs from a BOM.
     * Called when the user selects a BOM on the form.
     */
    ProductionDTO populateFromBom(Long bomId, Long outputWarehouseId, java.math.BigDecimal quantity);

    // ── Dashboard ──────────────────────────────────────────────────────────────

    Map<String, Object> dashboardSummary();

    ProductionDTO toDTO(Production entity);
}
