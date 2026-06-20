package com.asg.spindleserp.production.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.MovementType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.global.entity.*;
import com.asg.spindleserp.global.repository.*;
import com.asg.spindleserp.inventory.repository.*;
import com.asg.spindleserp.organization.repository.*;
import com.asg.spindleserp.production.dto.*;
import com.asg.spindleserp.production.entity.*;
import com.asg.spindleserp.production.repository.*;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductionServiceImpl implements ProductionService {

    private final BomRepository              bomRepo;
    private final BomItemRepository          bomItemRepo;
    private final ProductionRepository       prodRepo;
    private final ProductionInputRepository  inputRepo;
    private final ProductionOutputRepository outputRepo;

    // Inventory / stock engine
    private final InventoryStockBalanceRepository balanceRepo;
    private final InventoryTransactionRepository  txRepo;
    private final InventoryLotRepository          lotRepo;
    private final ItemRepository                  itemRepo;
    private final ItemUomRepository               uomRepo;

    // Org
    private final OrganizationRepository  orgRepo;
    private final WarehouseRepository     whRepo;
    private final CostCenterRepository    ccRepo;

    private final DocumentSequenceService seqService;
    private final JdbcTemplate            jdbcTemplate;

    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");

    // =========================================================================
    // BOM
    // =========================================================================

    @Override
    public BomDTO createBom(BomDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (bomRepo.existsByOrganizationIdAndBomCode(orgId, dto.getBomCode().trim().toUpperCase()))
            throw new IllegalArgumentException("BOM code '" + dto.getBomCode() + "' already exists.");
        Bom bom = buildBomHeader(dto, new Bom());
        bom.setOrganization(orgRepo.getReferenceById(orgId));
        Bom saved = bomRepo.save(bom);
        syncBomItems(dto.getItems(), saved);
        return toDTO(saved);
    }

    @Override
    public BomDTO updateBom(Long id, BomDTO dto) {
        Bom bom = findBom(id);
        buildBomHeader(dto, bom);
        Bom saved = bomRepo.save(bom);
        syncBomItems(dto.getItems(), saved);
        return toDTO(saved);
    }

    @Override @Transactional(readOnly = true)
    public BomDTO findBomById(Long id) { return toDTO(findBom(id)); }

    @Override
    public void deleteBom(Long id) {
        Bom bom = findBom(id);
        if (!bom.getItems().isEmpty())
            throw new IllegalStateException("Cannot delete BOM with items. Remove items first.");
        bomRepo.delete(bom);
    }

    @Override
    public BomDTO toggleBom(Long id) {
        Bom bom = findBom(id);
        bom.setActive(!bom.isActive());
        return toDTO(bomRepo.save(bom));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse bomDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND b.organization_id=" + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList("b.bom_code","b.bom_name","i.item_name","i.item_code"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY b.bom_code) AS sl,
                   COUNT(*) OVER () AS full_count,
                   b.id, b.bom_code, b.bom_name, b.bom_version,
                   i.item_code || ' — ' || i.item_name AS finished_item,
                   b.output_quantity,
                   u.code AS output_unit,
                   b.yield_percent,
                   (SELECT COUNT(*) FROM prd_bom_items bi WHERE bi.bom_id=b.id) AS item_count,
                   CASE WHEN b.is_default THEN '<span class="badge bg-primary">Default</span>' ELSE '' END AS default_badge,
                   CASE WHEN b.is_active
                       THEN '<span class="badge bg-success">Active</span>'
                       ELSE '<span class="badge bg-secondary">Inactive</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="bomShow('   || b.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="bomEdit('   || b.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="bomToggle(' || b.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                   || '<a href="javascript:;" onclick="bomDelete(' || b.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '<a href="javascript:;" onclick="bomCreateProduction(' || b.id || ')" class="btn btn-white btn-sm" title="Create Production Order"><i class="fas fa-industry text-teal"></i></a>'
                   || '</div>' AS actions
            FROM prd_bom b
            JOIN inv_items i ON i.id = b.finished_item_id
            JOIN inv_item_uom u ON u.id = b.output_unit_id
            %s ORDER BY b.bom_code OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> searchBoms(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page-1)*sz;
        String sql = "SELECT b.id, b.bom_code, b.bom_name, i.item_name FROM prd_bom b JOIN inv_items i ON i.id=b.finished_item_id WHERE b.is_active=true"
            + (orgId != null ? " AND b.organization_id=" + orgId : "")
            + (q != null && !q.isBlank() ? " AND (b.bom_code ILIKE '%" + q.replace("'","''") + "%' OR b.bom_name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY b.bom_code LIMIT " + (sz+1) + " OFFSET " + off;
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String,Object>> items = rows.stream().limit(sz).map(r ->
            Map.of("id",r.get("id"),"text",r.get("bom_code")+" — "+r.get("bom_name")+" ("+r.get("item_name")+")")).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override
    public BomDTO toDTO(Bom e) {
        BomDTO d = BomDTO.builder()
            .id(e.getId()).bomCode(e.getBomCode()).bomName(e.getBomName()).bomVersion(e.getBomVersion())
            .outputQuantity(e.getOutputQuantity()).yieldPercent(e.getYieldPercent())
            .active(e.isActive()).isDefault(e.isDefault())
            .description(e.getDescription()).notes(e.getNotes())
            .approvedBy(e.getApprovedBy())
            .approvedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null)
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getFinishedItem() != null) { d.setFinishedItemId(e.getFinishedItem().getId()); d.setFinishedItemDisplay(e.getFinishedItem().getItemCode()+" — "+e.getFinishedItem().getItemName()); }
        if (e.getOutputUnit()   != null) { d.setOutputUnitId(e.getOutputUnit().getId()); d.setOutputUnitDisplay(e.getOutputUnit().getCode()); }
        List<BomItem> bomItems = bomItemRepo.findByBomIdOrderByLineNumber(e.getId());
        d.setItems(bomItems.stream().map(bi -> {
            BomDTO.ItemDTO ld = BomDTO.ItemDTO.builder()
                .id(bi.getId()).lineNumber(bi.getLineNumber())
                .rawItemId(bi.getRawItem().getId())
                .rawItemDisplay(bi.getRawItem().getItemCode()+" — "+bi.getRawItem().getItemName())
                .unitId(bi.getUnit().getId()).unitDisplay(bi.getUnit().getCode())
                .quantity(bi.getQuantity()).scrapPct(bi.getScrapPct())
                .isOptional(bi.isOptional()).remarks(bi.getRemarks())
                .build();
            return ld;
        }).collect(Collectors.toList()));
        return d;
    }

    // =========================================================================
    // PRODUCTION WORK ORDER
    // =========================================================================

    @Override
    public ProductionDTO createProduction(ProductionDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String no = seqService.nextDocumentNumber(orgId, "PRD", LocalDate.now().format(YY));
        Production prod = buildProdHeader(dto, new Production());
        prod.setProductionNo(no);
        prod.setStatus(Production.ProductionStatus.DRAFT);
        setAudit(prod, true);
        Production saved = prodRepo.save(prod);
        syncInputs(dto.getInputs(), saved);
        syncOutputs(dto.getOutputs(), saved);
        return toDTO(saved);
    }

    @Override
    public ProductionDTO updateProduction(Long id, ProductionDTO dto) {
        Production prod = findProd(id);
        guardEditable(prod);
        buildProdHeader(dto, prod);
        setAudit(prod, false);
        Production saved = prodRepo.save(prod);
        syncInputs(dto.getInputs(), saved);
        syncOutputs(dto.getOutputs(), saved);
        return toDTO(saved);
    }

    @Override @Transactional(readOnly = true)
    public ProductionDTO findProductionById(Long id) { return toDTO(findProd(id)); }

    @Override
    public void deleteProduction(Long id) {
        Production prod = findProd(id);
        guardEditable(prod);
        prodRepo.delete(prod);
    }

    @Override public ProductionDTO submitProduction(Long id)   { return changeStatus(id, Production.ProductionStatus.SUBMITTED); }
    @Override public ProductionDTO approveProduction(Long id)  { return changeStatus(id, Production.ProductionStatus.APPROVED); }
    @Override public ProductionDTO releaseProduction(Long id)  { return changeStatus(id, Production.ProductionStatus.RELEASED); }
    @Override public ProductionDTO startProduction(Long id)    {
        Production prod = changeStatusEntity(id, Production.ProductionStatus.IN_PROGRESS);
        if (prod.getActualStartDate() == null) prod.setActualStartDate(LocalDate.now());
        return toDTO(prodRepo.save(prod));
    }

    @Override
    public ProductionDTO rejectProduction(Long id, String remarks) {
        Production prod = findProd(id);
        prod.setStatus(Production.ProductionStatus.REJECTED);
        prod.setRemarks((prod.getRemarks() != null ? prod.getRemarks()+"\n" : "") + "[REJECTED] " + remarks);
        setAudit(prod, false);
        return toDTO(prodRepo.save(prod));
    }

    @Override
    public ProductionDTO cancelProduction(Long id, String remarks) {
        Production prod = findProd(id);
        if (prod.getStatus() == Production.ProductionStatus.COMPLETED)
            throw new IllegalStateException("Completed production orders cannot be cancelled.");
        prod.setStatus(Production.ProductionStatus.CANCELLED);
        prod.setRemarks((prod.getRemarks() != null ? prod.getRemarks()+"\n" : "") + "[CANCELLED] " + remarks);
        setAudit(prod, false);
        return toDTO(prodRepo.save(prod));
    }

    /**
     * Complete a production order:
     * 1. Sum material cost from inputs
     * 2. Compute total/unit cost
     * 3. Post PRODUCTION_ISSUE (raw material OUT) for each input
     * 4. Create InventoryLot for each output
     * 5. Post FINISHED_GOODS_RECEIVE (FG IN) for each output
     */
    @Override
    public ProductionDTO completeProduction(Long id) {
        Production prod = findProd(id);
        if (prod.getStatus() != Production.ProductionStatus.IN_PROGRESS &&
            prod.getStatus() != Production.ProductionStatus.RELEASED)
            throw new IllegalStateException("Production must be IN_PROGRESS or RELEASED to complete.");

        Long orgId = prod.getOrganization().getId();

        // 1. Sum material cost
        BigDecimal materialCost = inputRepo.sumTotalCostByProduction(id);
        prod.setMaterialCost(materialCost);

        // 2. Compute totalCost and unitCost
        BigDecimal totalCost = materialCost
            .add(CommonUtils.nvl(prod.getLaborCost(), BigDecimal.ZERO))
            .add(CommonUtils.nvl(prod.getOverheadCost(), BigDecimal.ZERO))
            .add(CommonUtils.nvl(prod.getOtherCost(), BigDecimal.ZERO));
        prod.setTotalCost(totalCost);
        BigDecimal produced = CommonUtils.nvl(prod.getProducedQuantity(), BigDecimal.ZERO);
        if (produced.compareTo(BigDecimal.ZERO) > 0)
            prod.setUnitCost(totalCost.divide(produced, 4, RoundingMode.HALF_UP));
        prod.setActualEndDate(LocalDate.now());

        // 3. Post PRODUCTION_ISSUE for each input (raw material consumed OUT)
        List<ProductionInput> inputs = inputRepo.findByProductionIdOrderByLineNumber(id);
        for (ProductionInput inp : inputs) {
            InventoryStockBalance balance = balanceRepo
                .findByItemIdAndWarehouseIdAndLotId(inp.getRawItem().getId(), inp.getWarehouse().getId(),
                    inp.getLot() != null ? inp.getLot().getId() : null)
                .orElseThrow(() -> new IllegalStateException(
                    "No stock for " + inp.getRawItem().getItemCode() + " in warehouse " + inp.getWarehouse().getWarehouseCode()));
            BigDecimal newQty = balance.getQuantity().subtract(inp.getActualQuantity());
            if (newQty.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalStateException("Insufficient stock for " + inp.getRawItem().getItemCode());
            balance.setQuantity(newQty);
            balance.setLastTransactionTime(LocalDateTime.now());
            balanceRepo.save(balance);
            txRepo.save(InventoryTransaction.builder()
                .organizationId(orgId)
                .item(inp.getRawItem()).warehouse(inp.getWarehouse()).lot(inp.getLot())
                .movementType(MovementType.PRODUCTION_MATERIAL_ISSUE)
                .documentType("PRODUCTION_ORDER")
                .transactionDate(prod.getProductionDate())
                .quantity(inp.getActualQuantity())
                .unitCost(inp.getUnitCost()).totalCost(inp.getTotalCost())
                .balanceAfter(newQty)
                .remarks("Production issue: " + prod.getProductionNo())
                .build());
        }

        // 4+5. Create lot + post FINISHED_GOODS_RECEIVE for each output
        List<ProductionOutput> outputs = outputRepo.findByProductionIdOrderByLineNumber(id);
        for (ProductionOutput out : outputs) {
            // Create finished goods lot
            String lotNo = prod.getProductionNo() + "-L" + out.getLineNumber();
            InventoryLot lot = InventoryLot.builder()
                .organizationId(orgId)
                .item(out.getFinishedItem())
                .lotNumber(lotNo)
                .productionOrderId(id)
                .unitCost(prod.getUnitCost())
                .status(InventoryLot.LotStatus.valueOf("ACTIVE"))
                .receivedDate(LocalDate.now())
                .build();
            lot = lotRepo.save(lot);
            out.setLot(lot);
            out.setUnitCost(prod.getUnitCost());
            out.setTotalCost(prod.getUnitCost().multiply(out.getQuantity()).setScale(2, RoundingMode.HALF_UP));
            outputRepo.save(out);

            // Post FG receive
            InventoryLot finalLot = lot;
            InventoryStockBalance bal = balanceRepo
                .findByItemIdAndWarehouseIdAndLotId(out.getFinishedItem().getId(), out.getWarehouse().getId(), lot.getId())
                .orElseGet(() -> InventoryStockBalance.builder()
                    .item(out.getFinishedItem()).warehouse(out.getWarehouse()).lot(finalLot)
                    .quantity(BigDecimal.ZERO).reservedQuantity(BigDecimal.ZERO).build());
            BigDecimal newQty = bal.getQuantity().add(out.getQuantity());
            bal.setQuantity(newQty); bal.setAverageCost(prod.getUnitCost());
            bal.setLastTransactionTime(LocalDateTime.now());
            balanceRepo.save(bal);
            txRepo.save(InventoryTransaction.builder()
                .organizationId(orgId)
                .item(out.getFinishedItem()).warehouse(out.getWarehouse()).lot(lot)
                .movementType(MovementType.PRODUCTION_RECEIPT)
                .documentType("PRODUCTION_ORDER")
                .transactionDate(prod.getProductionDate())
                .quantity(out.getQuantity())
                .unitCost(prod.getUnitCost()).totalCost(out.getTotalCost())
                .balanceAfter(newQty)
                .remarks("Finished goods receive: " + prod.getProductionNo())
                .build());
        }

        prod.setStatus(Production.ProductionStatus.COMPLETED);
        setAudit(prod, false);
        return toDTO(prodRepo.save(prod));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse productionDatatable(int draw, int start, int length, String search, String status) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND p.organization_id=" + orgId : "")
            + (status != null && !status.isBlank() ? " AND p.status='" + status + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "p.production_no","i.item_code","i.item_name","p.status"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY p.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   p.id, p.production_no, p.status,
                   i.item_code || ' — ' || i.item_name AS finished_item,
                   p.planned_quantity, p.produced_quantity, p.rejected_quantity,
                   u.code AS output_unit,
                   TO_CHAR(p.production_date,'DD-Mon-YYYY') AS production_date,
                   COALESCE(TO_CHAR(p.planned_end_date,'DD-Mon-YYYY'),'—') AS planned_end_date,
                   COALESCE(b.bom_code,'—') AS bom_code,
                   COALESCE(p.unit_cost::text,'—') AS unit_cost,
                   COALESCE(p.total_cost::text,'0.00') AS total_cost,
                   CASE p.status
                       WHEN 'DRAFT'       THEN '<span class="badge bg-secondary">Draft</span>'
                       WHEN 'SUBMITTED'   THEN '<span class="badge bg-info text-dark">Submitted</span>'
                       WHEN 'APPROVED'    THEN '<span class="badge bg-teal">Approved</span>'
                       WHEN 'RELEASED'    THEN '<span class="badge bg-primary">Released</span>'
                       WHEN 'IN_PROGRESS' THEN '<span class="badge bg-warning text-dark">In Progress</span>'
                       WHEN 'COMPLETED'   THEN '<span class="badge bg-success">Completed</span>'
                       WHEN 'REJECTED'    THEN '<span class="badge bg-danger">Rejected</span>'
                       WHEN 'CANCELLED'   THEN '<span class="badge bg-secondary">Cancelled</span>'
                       ELSE '<span class="badge bg-light text-dark">' || p.status || '</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="prodShow('   || p.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                   || CASE WHEN p.status IN ('DRAFT','SUBMITTED','RETURNED') THEN
                       '<a href="javascript:;" onclick="prodEdit('   || p.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   END
                   || CASE WHEN p.status = 'DRAFT' THEN
                       '<a href="javascript:;" onclick="prodSubmit(' || p.id || ')" class="btn btn-white btn-sm" title="Submit"><i class="fas fa-paper-plane text-blue"></i></a>'
                   END
                   || CASE WHEN p.status = 'SUBMITTED' THEN
                       '<a href="javascript:;" onclick="prodApprove('|| p.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check-double text-success"></i></a>'
                   END
                   || CASE WHEN p.status = 'APPROVED' THEN
                       '<a href="javascript:;" onclick="prodRelease('|| p.id || ')" class="btn btn-white btn-sm" title="Release"><i class="fas fa-play-circle text-primary"></i></a>'
                   END
                   || CASE WHEN p.status IN ('RELEASED','IN_PROGRESS') THEN
                       '<a href="javascript:;" onclick="prodStart('  || p.id || ')" class="btn btn-white btn-sm" title="Start"><i class="fas fa-cog text-warning fa-spin" style="animation-duration:2s"></i></a>'
                       || '<a href="javascript:;" onclick="prodComplete('|| p.id || ')" class="btn btn-white btn-sm" title="Complete"><i class="fas fa-flag-checkered text-success"></i></a>'
                   END
                   || CASE WHEN p.status NOT IN ('COMPLETED','CANCELLED') THEN
                       '<a href="javascript:;" onclick="prodCancel(' || p.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fas fa-ban text-danger"></i></a>'
                   END
                   || '</div>' AS actions
            FROM prd_productions p
            JOIN inv_items i ON i.id = p.finished_item_id
            JOIN inv_item_uom u ON u.id = p.output_unit_id
            LEFT JOIN prd_bom b ON b.id = p.bom_id
            %s ORDER BY p.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> searchProductions(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page-1)*sz;
        String sql = "SELECT p.id, p.production_no, i.item_name FROM prd_productions p JOIN inv_items i ON i.id=p.finished_item_id WHERE p.status NOT IN ('CANCELLED','REJECTED')"
            + (orgId != null ? " AND p.organization_id=" + orgId : "")
            + (q != null && !q.isBlank() ? " AND (p.production_no ILIKE '%" + q.replace("'","''") + "%' OR i.item_name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY p.id DESC LIMIT " + (sz+1) + " OFFSET " + off;
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String,Object>> items = rows.stream().limit(sz).map(r ->
            Map.of("id",r.get("id"),"text",r.get("production_no")+" — "+r.get("item_name"))).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override @Transactional(readOnly = true)
    public ProductionDTO populateFromBom(Long bomId, Long outputWarehouseId, BigDecimal quantity) {
        Bom bom = findBom(bomId);
        BigDecimal ratio = quantity != null && bom.getOutputQuantity().compareTo(BigDecimal.ZERO) > 0
            ? quantity.divide(bom.getOutputQuantity(), 6, RoundingMode.HALF_UP)
            : BigDecimal.ONE;
        List<BomItem> bomItems = bomItemRepo.findByBomIdOrderByLineNumber(bomId);
        List<ProductionDTO.InputDTO> inputs = new ArrayList<>();
        int lineNo = 1;
        for (BomItem bi : bomItems) {
            BigDecimal needed = bi.getQuantity().multiply(ratio).setScale(4, RoundingMode.HALF_UP);
            inputs.add(ProductionDTO.InputDTO.builder()
                .lineNumber(lineNo++)
                .rawItemId(bi.getRawItem().getId())
                .rawItemDisplay(bi.getRawItem().getItemCode()+" — "+bi.getRawItem().getItemName())
                .unitId(bi.getUnit().getId()).unitDisplay(bi.getUnit().getCode())
                .bomItemId(bi.getId())
                .plannedQuantity(needed).actualQuantity(needed)
                .warehouseId(outputWarehouseId)
                .build());
        }
        ProductionDTO stub = ProductionDTO.builder()
            .bomId(bom.getId()).bomDisplay(bom.getBomCode()+" — "+bom.getBomName())
            .finishedItemId(bom.getFinishedItem().getId())
            .finishedItemDisplay(bom.getFinishedItem().getItemCode()+" — "+bom.getFinishedItem().getItemName())
            .outputUnitId(bom.getOutputUnit().getId()).outputUnitDisplay(bom.getOutputUnit().getCode())
            .outputWarehouseId(outputWarehouseId)
            .plannedQuantity(quantity != null ? quantity : bom.getOutputQuantity())
            .inputs(inputs)
            .build();
        return stub;
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String f = orgId != null ? " AND organization_id=" + orgId : "";
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("draft",      jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prd_productions WHERE status='DRAFT'" + f, Long.class));
        m.put("inProgress", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prd_productions WHERE status='IN_PROGRESS'" + f, Long.class));
        m.put("completed",  jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prd_productions WHERE status='COMPLETED'" + f, Long.class));
        m.put("activeBoms", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prd_bom WHERE is_active=true" + f, Long.class));
        m.put("totalBoms",  jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prd_bom WHERE 1=1" + f, Long.class));
        return m;
    }

    @Override
    public ProductionDTO toDTO(Production e) {
        ProductionDTO d = ProductionDTO.builder()
            .id(e.getId()).productionNo(e.getProductionNo())
            .productionDate(e.getProductionDate())
            .plannedStartDate(e.getPlannedStartDate()).plannedEndDate(e.getPlannedEndDate())
            .actualStartDate(e.getActualStartDate()).actualEndDate(e.getActualEndDate())
            .plannedQuantity(e.getPlannedQuantity())
            .producedQuantity(e.getProducedQuantity()).rejectedQuantity(e.getRejectedQuantity())
            .wasteQuantity(e.getWasteQuantity())
            .materialCost(e.getMaterialCost()).laborCost(e.getLaborCost())
            .overheadCost(e.getOverheadCost()).otherCost(e.getOtherCost())
            .totalCost(e.getTotalCost()).unitCost(e.getUnitCost())
            .status(e.getStatus() != null ? e.getStatus().name() : "DRAFT")
            .approvalStatus(e.getApprovalStatus()).remarks(e.getRemarks())
            .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getBom()            != null) { d.setBomId(e.getBom().getId()); d.setBomDisplay(e.getBom().getBomCode()+" — "+e.getBom().getBomName()); }
        if (e.getFinishedItem()   != null) { d.setFinishedItemId(e.getFinishedItem().getId()); d.setFinishedItemDisplay(e.getFinishedItem().getItemCode()+" — "+e.getFinishedItem().getItemName()); }
        if (e.getOutputWarehouse()!= null) { d.setOutputWarehouseId(e.getOutputWarehouse().getId()); d.setOutputWarehouseDisplay(e.getOutputWarehouse().getWarehouseCode()+" — "+e.getOutputWarehouse().getWarehouseName()); }
        if (e.getCostCenter()     != null) { d.setCostCenterId(e.getCostCenter().getId()); d.setCostCenterDisplay(e.getCostCenter().getCostCenterCode()+" — "+e.getCostCenter().getCostCenterName()); }
        if (e.getOutputUnit()     != null) { d.setOutputUnitId(e.getOutputUnit().getId()); d.setOutputUnitDisplay(e.getOutputUnit().getCode()); }
        if (e.getSalesOrder()     != null) { d.setSalesOrderId(e.getSalesOrder().getId()); d.setSalesOrderDisplay(e.getSalesOrder().getDocumentNo()); }
        // Inputs
        List<ProductionInput> inputs = inputRepo.findByProductionIdOrderByLineNumber(e.getId());
        d.setInputs(inputs.stream().map(inp -> ProductionDTO.InputDTO.builder()
            .id(inp.getId()).lineNumber(inp.getLineNumber())
            .rawItemId(inp.getRawItem().getId()).rawItemDisplay(inp.getRawItem().getItemCode()+" — "+inp.getRawItem().getItemName())
            .lotId(inp.getLot() != null ? inp.getLot().getId() : null)
            .lotDisplay(inp.getLot() != null ? inp.getLot().getLotNumber() : null)
            .warehouseId(inp.getWarehouse().getId()).warehouseDisplay(inp.getWarehouse().getWarehouseCode())
            .unitId(inp.getUnit().getId()).unitDisplay(inp.getUnit().getCode())
            .bomItemId(inp.getBomItem() != null ? inp.getBomItem().getId() : null)
            .plannedQuantity(inp.getPlannedQuantity()).actualQuantity(inp.getActualQuantity())
            .unitCost(inp.getUnitCost()).totalCost(inp.getTotalCost())
            .scrapQuantity(inp.getScrapQuantity()).remarks(inp.getRemarks())
            .build()).collect(Collectors.toList()));
        // Outputs
        List<ProductionOutput> outputs = outputRepo.findByProductionIdOrderByLineNumber(e.getId());
        d.setOutputs(outputs.stream().map(out -> ProductionDTO.OutputDTO.builder()
            .id(out.getId()).lineNumber(out.getLineNumber())
            .finishedItemId(out.getFinishedItem().getId()).finishedItemDisplay(out.getFinishedItem().getItemCode()+" — "+out.getFinishedItem().getItemName())
            .lotId(out.getLot() != null ? out.getLot().getId() : null)
            .lotDisplay(out.getLot() != null ? out.getLot().getLotNumber() : null)
            .warehouseId(out.getWarehouse().getId()).warehouseDisplay(out.getWarehouse().getWarehouseCode())
            .unitId(out.getUnit().getId()).unitDisplay(out.getUnit().getCode())
            .quantity(out.getQuantity()).rejectedQuantity(out.getRejectedQuantity())
            .unitCost(out.getUnitCost()).totalCost(out.getTotalCost())
            .batchNo(out.getBatchNo()).remarks(out.getRemarks())
            .build()).collect(Collectors.toList()));
        return d;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Bom buildBomHeader(BomDTO dto, Bom e) {
        e.setBomCode(dto.getBomCode().trim().toUpperCase());
        e.setBomName(dto.getBomName().trim());
        e.setBomVersion(dto.getBomVersion() != null ? dto.getBomVersion() : "1.0");
        e.setOutputQuantity(dto.getOutputQuantity() != null ? dto.getOutputQuantity() : BigDecimal.ONE);
        e.setYieldPercent(dto.getYieldPercent() != null ? dto.getYieldPercent() : new BigDecimal("100.00"));
        e.setActive(Boolean.TRUE.equals(dto.getActive()));
        e.setDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        e.setDescription(dto.getDescription()); e.setNotes(dto.getNotes());
        if (dto.getFinishedItemId() != null) e.setFinishedItem(itemRepo.getReferenceById(dto.getFinishedItemId()));
        if (dto.getOutputUnitId()   != null) e.setOutputUnit(uomRepo.getReferenceById(dto.getOutputUnitId()));
        setAudit(e, e.getId() == null);
        return e;
    }

    private void syncBomItems(List<BomDTO.ItemDTO> dtos, Bom parent) {
        if (dtos == null) return;
        parent.getItems().clear();
        int num = 1;
        for (BomDTO.ItemDTO ld : dtos) {
            if (ld.getRawItemId() == null) continue;
            BomItem bi = BomItem.builder()
                .bom(parent)
                .rawItem(itemRepo.getReferenceById(ld.getRawItemId()))
                .unit(uomRepo.getReferenceById(ld.getUnitId()))
                .lineNumber(ld.getLineNumber() != null ? ld.getLineNumber() : num)
                .quantity(nvl2(ld.getQuantity()))
                .scrapPct(ld.getScrapPct() != null ? ld.getScrapPct() : BigDecimal.ZERO)
                .isOptional(Boolean.TRUE.equals(ld.getIsOptional()))
                .remarks(ld.getRemarks())
                .createdBy(SecurityHelper.currentUsername().orElse("system"))
                .build();
            parent.getItems().add(bi);
            num++;
        }
        bomRepo.save(parent);
    }

    private Production buildProdHeader(ProductionDTO dto, Production e) {
        e.setProductionDate(dto.getProductionDate());
        e.setPlannedStartDate(dto.getPlannedStartDate()); e.setPlannedEndDate(dto.getPlannedEndDate());
        e.setActualStartDate(dto.getActualStartDate());   e.setActualEndDate(dto.getActualEndDate());
        e.setPlannedQuantity(dto.getPlannedQuantity());
        e.setProducedQuantity(nvl2(dto.getProducedQuantity()));
        e.setRejectedQuantity(nvl2(dto.getRejectedQuantity())); e.setWasteQuantity(nvl2(dto.getWasteQuantity()));
        e.setLaborCost(nvl2(dto.getLaborCost())); e.setOverheadCost(nvl2(dto.getOverheadCost())); e.setOtherCost(nvl2(dto.getOtherCost()));
        e.setRemarks(dto.getRemarks());
        if (dto.getBomId()             != null) e.setBom(bomRepo.getReferenceById(dto.getBomId()));
        if (dto.getFinishedItemId()    != null) e.setFinishedItem(itemRepo.getReferenceById(dto.getFinishedItemId()));
        if (dto.getOutputWarehouseId() != null) e.setOutputWarehouse(whRepo.getReferenceById(dto.getOutputWarehouseId()));
        if (dto.getCostCenterId()      != null) e.setCostCenter(ccRepo.getReferenceById(dto.getCostCenterId()));
        if (dto.getOutputUnitId()      != null) e.setOutputUnit(uomRepo.getReferenceById(dto.getOutputUnitId()));
        return e;
    }

    private void syncInputs(List<ProductionDTO.InputDTO> dtos, Production parent) {
        if (dtos == null) return;
        parent.getInputs().clear(); int num=1;
        for (ProductionDTO.InputDTO ld : dtos) {
            if (ld.getRawItemId()==null) continue;
            BigDecimal uc = nvl2(ld.getUnitCost()), ac = nvl2(ld.getActualQuantity());
            ProductionInput inp = ProductionInput.builder()
                .production(parent)
                .rawItem(itemRepo.getReferenceById(ld.getRawItemId()))
                .warehouse(whRepo.getReferenceById(ld.getWarehouseId()))
                .unit(uomRepo.getReferenceById(ld.getUnitId()))
                .lineNumber(ld.getLineNumber() != null ? ld.getLineNumber() : num)
                .plannedQuantity(ld.getPlannedQuantity()).actualQuantity(ac)
                .unitCost(uc).totalCost(uc.multiply(ac).setScale(2, RoundingMode.HALF_UP))
                .scrapQuantity(nvl2(ld.getScrapQuantity()))
                .remarks(ld.getRemarks())
                .createdBy(SecurityHelper.currentUsername().orElse("system"))
                .build();
            if (ld.getLotId()     != null) inp.setLot(lotRepo.getReferenceById(ld.getLotId()));
            if (ld.getBomItemId() != null) inp.setBomItem(bomItemRepo.getReferenceById(ld.getBomItemId()));
            parent.getInputs().add(inp);
            num++;
        }
        prodRepo.save(parent);
    }

    private void syncOutputs(List<ProductionDTO.OutputDTO> dtos, Production parent) {
        if (dtos == null || dtos.isEmpty()) {
            // Auto-create one output line for the finished item
            ProductionOutput out = ProductionOutput.builder()
                .production(parent).finishedItem(parent.getFinishedItem())
                .warehouse(parent.getOutputWarehouse()).unit(parent.getOutputUnit())
                .lineNumber(1)
                .quantity(nvl2(parent.getProducedQuantity()).compareTo(BigDecimal.ZERO)>0
                    ? parent.getProducedQuantity() : parent.getPlannedQuantity())
                .build();
            out.setCreatedBy(SecurityHelper.currentUsername().orElse("system"));
            parent.getOutputs().clear();
            parent.getOutputs().add(out);
            prodRepo.save(parent);
            return;
        }
        parent.getOutputs().clear(); int num=1;
        for (ProductionDTO.OutputDTO ld : dtos) {
            if (ld.getFinishedItemId()==null) continue;
            ProductionOutput out = ProductionOutput.builder()
                .production(parent)
                .finishedItem(itemRepo.getReferenceById(ld.getFinishedItemId()))
                .warehouse(whRepo.getReferenceById(ld.getWarehouseId()))
                .unit(uomRepo.getReferenceById(ld.getUnitId()))
                .lineNumber(ld.getLineNumber() != null ? ld.getLineNumber() : num)
                .quantity(nvl2(ld.getQuantity())).rejectedQuantity(nvl2(ld.getRejectedQuantity()))
                .unitCost(nvl2(ld.getUnitCost())).totalCost(nvl2(ld.getTotalCost()))
                .batchNo(ld.getBatchNo()).remarks(ld.getRemarks())
                .build();
            out.setCreatedBy(SecurityHelper.currentUsername().orElse("system"));
            if (ld.getLotId() != null) out.setLot(lotRepo.getReferenceById(ld.getLotId()));
            parent.getOutputs().add(out);
            num++;
        }
        prodRepo.save(parent);
    }

    private ProductionDTO changeStatus(Long id, Production.ProductionStatus s) {
        return toDTO(changeStatusEntity(id, s));
    }

    private Production changeStatusEntity(Long id, Production.ProductionStatus s) {
        Production p = findProd(id);
        p.setStatus(s);
        setAudit(p, false);
        return prodRepo.save(p);
    }

    private void guardEditable(Production p) {
        if (!Set.of(Production.ProductionStatus.DRAFT, Production.ProductionStatus.SUBMITTED,
                    Production.ProductionStatus.REJECTED).contains(p.getStatus()))
            throw new IllegalStateException("Production " + p.getProductionNo() + " is " + p.getStatus() + " and cannot be edited.");
    }

    private Bom findBom(Long id) { return bomRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("BOM #"+id+" not found.")); }
    private Production findProd(Long id) { return prodRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Production #"+id+" not found.")); }
    private BigDecimal nvl2(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private void setAudit(Object e, boolean isCreate) {
        String user = SecurityHelper.currentUsername().orElse("system");
        LocalDateTime now = LocalDateTime.now();
        if (e instanceof Bom b)       { if (isCreate) { b.setCreatedBy(user); b.setCreatedAt(now); } b.setUpdatedBy(user); b.setUpdatedAt(now); }
        else if (e instanceof Production p) { if (isCreate) { p.setCreatedBy(user); p.setCreatedAt(now); } p.setUpdatedBy(user); p.setUpdatedAt(now); }
    }

    private String nvl(String v) { return (v != null && !v.isBlank()) ? v : "—"; }
}
