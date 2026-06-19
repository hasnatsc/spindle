package com.asg.spindleserp.inventory.transaction.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.DocumentType;
import com.asg.spindleserp.common.enums.MovementType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.global.entity.*;
import com.asg.spindleserp.global.repository.*;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.inventory.repository.ItemRepository;
import com.asg.spindleserp.inventory.transaction.dto.StockAdjustmentDTO;
import com.asg.spindleserp.inventory.transaction.dto.StockTransferDTO;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.entity.Warehouse;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.organization.repository.WarehouseRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

    private final BusinessDocumentRepository     docRepo;
    private final BusinessDocumentLineRepository lineRepo;
    private final InventoryTransactionRepository txRepo;
    private final InventoryStockBalanceRepository balanceRepo;
    private final InventoryLotRepository         lotRepo;
    private final ItemRepository                 itemRepo;
    private final WarehouseRepository            warehouseRepo;
    private final OrganizationRepository         orgRepo;
    private final DocumentSequenceService        seqService;
    private final JdbcTemplate                   jdbcTemplate;

    private static final DateTimeFormatter YY_FMT = DateTimeFormatter.ofPattern("yy-MM");

    // ═════════════════════════════════════════════════════════════════════════
    // STOCK ADJUSTMENT
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public StockAdjustmentDTO createAdjustment(StockAdjustmentDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (orgId == null)
            throw new IllegalArgumentException("Organization context is required to create adjustment.");
        Organization org  = orgRepo.findById(orgId).orElseThrow(() -> new IllegalArgumentException("Organisation not found."));
        Warehouse    wh   = resolveWarehouse(dto.getWarehouseId());
        if (!org.equals(wh.getOrganization()))
            throw new IllegalArgumentException("Selected warehouse does not belong to your organization.");
        String       docNo = seqService.nextDocumentNumber(orgId, "ADJ", LocalDate.now().format(YY_FMT));

        BusinessDocument doc = BusinessDocument.builder()
                .organization(org)
                .warehouse(wh)
                .documentNo(docNo)
                .documentDate(dto.getDocumentDate())
                .documentType(DocumentType.STOCK_ADJUSTMENT)
                .status("DRAFT")
                .referenceNo(dto.getReferenceNo())
                .remarks(dto.getRemarks())
                .createdBy(SecurityHelper.currentUsername().orElse("system"))
                .updatedBy(SecurityHelper.currentUsername().orElse("system"))
                .build();

        buildAdjustmentLines(doc, dto.getLines(), orgId);
        return toAdjustmentDTO(docRepo.save(doc));
    }

    @Override
    public StockAdjustmentDTO updateAdjustment(Long id, StockAdjustmentDTO dto) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_ADJUSTMENT);
        guardDraft(doc);

        doc.setWarehouse(resolveWarehouse(dto.getWarehouseId()));
        doc.setDocumentDate(dto.getDocumentDate());
        doc.setReferenceNo(dto.getReferenceNo());
        doc.setRemarks(dto.getRemarks());
        doc.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        doc.getLines().clear();
        buildAdjustmentLines(doc, dto.getLines(), doc.getOrganization().getId());
        return toAdjustmentDTO(docRepo.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentDTO findAdjustmentById(Long id) {
        return toAdjustmentDTO(findDoc(id, DocumentType.STOCK_ADJUSTMENT));
    }

    @Override
    public StockAdjustmentDTO confirmAdjustment(Long id) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_ADJUSTMENT);
        guardDraft(doc);

        for (BusinessDocumentLine line : doc.getLines()) {
            MovementType mt = MovementType.valueOf(
                    line.getRemarks() != null && line.getRemarks().startsWith("ADJ_OUT")
                            ? "ADJUSTMENT_OUT" : "ADJUSTMENT_IN");
            postInventoryTransaction(doc, line, mt, doc.getWarehouse());
        }

        doc.setStatus("CONFIRMED");
        doc.setStockPosted(true);
        doc.setUpdatedAt(LocalDateTime.now());
        return toAdjustmentDTO(docRepo.save(doc));
    }

    @Override
    public void cancelAdjustment(Long id) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_ADJUSTMENT);
        guardDraft(doc);
        doc.setStatus("CANCELLED");
        doc.setUpdatedAt(LocalDateTime.now());
        docRepo.save(doc);
    }

    @Override
    public void deleteAdjustment(Long id) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_ADJUSTMENT);
        if ("CONFIRMED".equals(doc.getStatus()))
            throw new IllegalArgumentException("Cannot delete a confirmed adjustment. Cancel it first.");
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        doc.setDeletedBy(SecurityHelper.currentUsername().orElse("system"));
        docRepo.save(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse adjustmentDatatableList(int draw, int start, int length, String search) {
        return docDatatable(draw, start, length, search, "STOCK_ADJUSTMENT", "adjShow", "adjEdit", "adjDelete");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STOCK TRANSFER
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public StockTransferDTO createTransfer(StockTransferDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (orgId == null)
            throw new IllegalArgumentException("Organization context is required to create adjustment.");
        Organization org  = orgRepo.findById(orgId).orElseThrow(() -> new IllegalArgumentException("Organisation not found."));
        Warehouse    dest = resolveWarehouse(dto.getWarehouseId());
        Warehouse    src  = resolveWarehouse(dto.getSourceWarehouseId());
        if (dest.getId().equals(src.getId()))
            throw new IllegalArgumentException("Source and destination warehouses must be different.");
        if (org.getId().equals(src.getId()))
            throw new IllegalArgumentException("Source and destination warehouses must be different.");
        if (!org.equals(dest.getOrganization()))
            throw new IllegalArgumentException("Selected warehouse does not belong to your organization.");
        if (!org.equals(src.getOrganization()))
            throw new IllegalArgumentException("Selected warehouse does not belong to your organization.");

        String docNo = seqService.nextDocumentNumber(orgId, "STR", LocalDate.now().format(YY_FMT));

        BusinessDocument doc = BusinessDocument.builder()
                .organization(org)
                .warehouse(dest)                          // destination in header
                .documentNo(docNo)
                .documentDate(dto.getDocumentDate())
                .documentType(DocumentType.STOCK_TRANSFER)
                .status("DRAFT")
                .referenceNo(dto.getReferenceNo())
                .vehicleNumber(dto.getVehicleNumber())
                .driverName(dto.getDriverName())
                .remarks(dto.getRemarks())
                .createdBy(SecurityHelper.currentUsername().orElse("system"))
                .updatedBy(SecurityHelper.currentUsername().orElse("system"))
                .build();

        buildTransferLines(doc, dto.getLines(), orgId, src.getWarehouseCode());
        return toTransferDTO(docRepo.save(doc), src);
    }

    @Override
    public StockTransferDTO updateTransfer(Long id, StockTransferDTO dto) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_TRANSFER);
        guardDraft(doc);

        Warehouse dest = resolveWarehouse(dto.getWarehouseId());
        Warehouse src  = resolveWarehouse(dto.getSourceWarehouseId());
        if (dest.getId().equals(src.getId()))
            throw new IllegalArgumentException("Source and destination warehouses must be different.");

        doc.setWarehouse(dest);
        doc.setDocumentDate(dto.getDocumentDate());
        doc.setReferenceNo(dto.getReferenceNo());
        doc.setVehicleNumber(dto.getVehicleNumber());
        doc.setDriverName(dto.getDriverName());
        doc.setRemarks(dto.getRemarks());
        doc.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        doc.getLines().clear();
        buildTransferLines(doc, dto.getLines(), doc.getOrganization().getId(), src.getWarehouseCode());
        return toTransferDTO(docRepo.save(doc), src);
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransferDTO findTransferById(Long id) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_TRANSFER);
        // Source WH is stored in remarks prefix of first line
        Warehouse srcWh = null;
        if (!doc.getLines().isEmpty()) {
            String r = doc.getLines().get(0).getRemarks();
            if (r != null && r.startsWith("SRC_WH:")) {
                String srcCode = r.split(":")[1].split("\\|")[0];
                srcWh = warehouseRepo.findAll().stream()
                        .filter(w -> w.getWarehouseCode().equals(srcCode)).findFirst().orElse(null);
            }
        }
        return toTransferDTO(doc, srcWh);
    }

    @Override
    public StockTransferDTO confirmTransfer(Long id) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_TRANSFER);
        guardDraft(doc);

        // Source WH is embedded in first line remarks: "SRC_WH:WHCode|..."
        Warehouse dest = doc.getWarehouse();
        for (BusinessDocumentLine line : doc.getLines()) {
            String r = line.getRemarks() != null ? line.getRemarks() : "";
            String srcCode = r.startsWith("SRC_WH:") ? r.split(":")[1].split("\\|")[0] : null;
            Warehouse src = srcCode != null
                    ? warehouseRepo.findAll().stream().filter(w -> w.getWarehouseCode().equals(srcCode)).findFirst().orElse(dest)
                    : dest;

            // Validate available stock
            BigDecimal avail = availableForLine(line, src.getId());
            if (avail.compareTo(line.getQuantity()) < 0)
                throw new IllegalArgumentException(
                        "Insufficient stock for item '" + line.getItemCode() + "' in warehouse '" + src.getWarehouseCode()
                        + "'. Available: " + avail + ", Required: " + line.getQuantity());

            postInventoryTransaction(doc, line, MovementType.TRANSFER_OUT, src);
            postInventoryTransaction(doc, line, MovementType.TRANSFER_IN,  dest);
        }

        doc.setStatus("CONFIRMED");
        doc.setStockPosted(true);
        doc.setUpdatedAt(LocalDateTime.now());
        return toTransferDTO(docRepo.save(doc), null);
    }

    @Override
    public void cancelTransfer(Long id) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_TRANSFER);
        guardDraft(doc);
        doc.setStatus("CANCELLED");
        doc.setUpdatedAt(LocalDateTime.now());
        docRepo.save(doc);
    }

    @Override
    public void deleteTransfer(Long id) {
        BusinessDocument doc = findDoc(id, DocumentType.STOCK_TRANSFER);
        if ("CONFIRMED".equals(doc.getStatus()))
            throw new IllegalArgumentException("Cannot delete a confirmed transfer. Cancel it first.");
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        doc.setDeletedBy(SecurityHelper.currentUsername().orElse("system"));
        docRepo.save(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse transferDatatableList(int draw, int start, int length, String search) {
        return docDatatable(draw, start, length, search, "STOCK_TRANSFER", "trShow", "trEdit", "trDelete");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SHARED STOCK ENGINE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Posts one InventoryTransaction and updates InventoryStockBalance atomically.
     * Inbound movements increase balance; outbound decrease.
     */
    private void postInventoryTransaction(BusinessDocument doc,
                                          BusinessDocumentLine line,
                                          MovementType movementType,
                                          Warehouse warehouse) {
        boolean isInbound = isInbound(movementType);
        BigDecimal qty = isInbound ? line.getQuantity() : line.getQuantity().negate();

        // Upsert stock balance
        InventoryLot lot = line.getInventoryLot();
        Long lotId = lot != null ? lot.getId() : null;

        InventoryStockBalance balance = balanceRepo
                .findByItemIdAndWarehouseIdAndLotId(line.getItem().getId(), warehouse.getId(), lotId)
                .orElseGet(() -> InventoryStockBalance.builder()
                        .item(line.getItem())
                        .warehouse(warehouse)
                        .lot(lot)
                        .quantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .build());

        BigDecimal newQty = balance.getQuantity().add(qty);
        if (!isInbound && newQty.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException(
                    "Negative stock would result for item '" + line.getItemCode()
                    + "' in warehouse '" + warehouse.getWarehouseCode() + "'.");

        balance.setQuantity(newQty);
        balance.setLastTransactionTime(LocalDateTime.now());
        if (line.getUnitPrice() != null && isInbound) {
            updateAverageCost(balance, qty, line.getUnitPrice());
        }
        balanceRepo.save(balance);

        // Write ledger entry
        InventoryTransaction tx = InventoryTransaction.builder()
                .organizationId(doc.getOrganization().getId())
                .item(line.getItem())
                .warehouse(warehouse)
                .lot(lot)
                .businessDocument(doc)
                .documentType(doc.getDocumentType().name())
                .movementType(movementType)
                .transactionDate(doc.getDocumentDate())
                .quantity(line.getQuantity())
                .unitCost(line.getUnitPrice())
                .totalCost(line.getLineAmount())
                .balanceAfter(newQty)
                .remarks(line.getRemarks())
                .build();
        txRepo.save(tx);
    }

    private void updateAverageCost(InventoryStockBalance balance, BigDecimal inQty, BigDecimal inCost) {
        if (inQty.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal oldQty   = balance.getQuantity().subtract(inQty);
        BigDecimal oldCost  = balance.getAverageCost() != null ? balance.getAverageCost() : BigDecimal.ZERO;
        BigDecimal newTotalValue = oldQty.multiply(oldCost).add(inQty.multiply(inCost));
        BigDecimal newQty   = balance.getQuantity();
        if (newQty.compareTo(BigDecimal.ZERO) > 0)
            balance.setAverageCost(newTotalValue.divide(newQty, 4, java.math.RoundingMode.HALF_UP));
    }

    private boolean isInbound(MovementType mt) {
        return mt == MovementType.ADJUSTMENT_IN
            || mt == MovementType.TRANSFER_IN
            || mt == MovementType.PURCHASE_RECEIPT
            || mt == MovementType.PRODUCTION_RECEIPT
            || mt == MovementType.RETURN_FROM_CUSTOMER;
    }

    private BigDecimal availableForLine(BusinessDocumentLine line, Long warehouseId) {
        Long lotId = line.getInventoryLot() != null ? line.getInventoryLot().getId() : null;
        return balanceRepo.findByItemIdAndWarehouseIdAndLotId(line.getItem().getId(), warehouseId, lotId)
                .map(b -> b.getQuantity().subtract(b.getReservedQuantity()))
                .orElse(BigDecimal.ZERO);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LINE BUILDERS
    // ═════════════════════════════════════════════════════════════════════════

    private void buildAdjustmentLines(BusinessDocument doc,
                                      List<StockAdjustmentDTO.LineDTO> lineDTOs,
                                      Long orgId) {
        int lineNo = 1;
        for (StockAdjustmentDTO.LineDTO ld : lineDTOs) {
            Item item = resolveItem(ld.getItemId());
            InventoryLot lot = ld.getLotId() != null
                    ? lotRepo.findById(ld.getLotId()).orElse(null) : null;

            BigDecimal lineAmt = ld.getUnitCost() != null && ld.getQuantity() != null
                    ? ld.getUnitCost().multiply(ld.getQuantity()) : BigDecimal.ZERO;

            // Store movement direction in remarks for confirm step
            String remarks = ld.getMovementType() + "|" + (ld.getRemarks() != null ? ld.getRemarks() : "");

            BusinessDocumentLine line = BusinessDocumentLine.builder()
                    .organizationId(orgId)
                    .document(doc)
                    .item(item)
                    .inventoryLot(lot)
                    .lineNumber(lineNo++)
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .unitCode(item.getUnitOfMeasure())
                    .quantity(ld.getQuantity())
                    .unitPrice(ld.getUnitCost())
                    .lineAmount(lineAmt)
                    .remarks(remarks)
                    .build();
            doc.getLines().add(line);
        }
    }

    private void buildTransferLines(BusinessDocument doc,
                                    List<StockTransferDTO.LineDTO> lineDTOs,
                                    Long orgId,
                                    String sourceWhCode) {
        int lineNo = 1;
        for (StockTransferDTO.LineDTO ld : lineDTOs) {
            Item item = resolveItem(ld.getItemId());
            InventoryLot lot = ld.getLotId() != null
                    ? lotRepo.findById(ld.getLotId()).orElse(null) : null;

            // Encode source WH in remarks: "SRC_WH:WHCode|user-remarks"
            String remarks = "SRC_WH:" + sourceWhCode + "|" + (ld.getRemarks() != null ? ld.getRemarks() : "");

            BusinessDocumentLine line = BusinessDocumentLine.builder()
                    .organizationId(orgId)
                    .document(doc)
                    .item(item)
                    .inventoryLot(lot)
                    .lineNumber(lineNo++)
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .unitCode(item.getUnitOfMeasure())
                    .quantity(ld.getQuantity())
                    .remarks(remarks)
                    .build();
            doc.getLines().add(line);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DATATABLE
    // ═════════════════════════════════════════════════════════════════════════

    private DataTableResponse docDatatable(int draw, int start, int length, String search, String docType, String fnShow, String fnEdit, String fnDelete) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        Long warehouseId = ContextProvider.getWarehouseId();
        String where = "WHERE d.document_type = '" + docType + "' AND d.is_deleted = false AND d.organization_id = " + warehouseId + " "
                + (orgId != null ? " AND d.organization_id = " + orgId : "")
                + CommonUtils.searchILike(search, Arrays.asList("d.document_no", "d.reference_no", "w.warehouse_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY d.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                d.id,
                d.document_no,
                d.document_date,
                COALESCE(w.warehouse_name, '—')             AS warehouse_name,
                d.status,
                COALESCE(d.reference_no, '—')               AS reference_no,
                (SELECT COUNT(*) FROM global_business_document_lines l WHERE l.document_id = d.id) AS line_count,
                TO_CHAR(d.created_at, 'DD-Mon-YYYY')         AS created_at,
                d.created_by,
                CASE d.status
                    WHEN 'CONFIRMED'  THEN '<span class="badge bg-success">Confirmed</span>'
                    WHEN 'CANCELLED'  THEN '<span class="badge bg-danger">Cancelled</span>'
                    ELSE                   '<span class="badge bg-secondary">Draft</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="%s(' || d.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || CASE WHEN d.status = 'DRAFT' THEN
                         '<a href="javascript:;" onclick="%s(' || d.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                       ELSE '' END
                    || CASE WHEN d.status = 'DRAFT' THEN
                         '<a href="javascript:;" onclick="%s(' || d.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                       ELSE '' END
                    || '</div>'                              AS actions
            FROM  global_business_documents d
            LEFT  JOIN org_warehouses w ON w.id = d.warehouse_id
            %s
            ORDER BY d.id DESC
            OFFSET %d LIMIT %d
            """, fnShow, fnEdit, fnDelete, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.getFirst().get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAPPING
    // ═════════════════════════════════════════════════════════════════════════

    private StockAdjustmentDTO toAdjustmentDTO(BusinessDocument doc) {
        List<StockAdjustmentDTO.LineDTO> lines = new ArrayList<>();
        for (BusinessDocumentLine l : doc.getLines()) {
            String raw = l.getRemarks() != null ? l.getRemarks() : "";
            String mt  = raw.startsWith("ADJUSTMENT") ? raw.split("\\|")[0] : "ADJUSTMENT_IN";
            String rem = raw.contains("|") ? raw.substring(raw.indexOf("|") + 1) : "";
            lines.add(StockAdjustmentDTO.LineDTO.builder()
                    .id(l.getId()).lineNumber(l.getLineNumber())
                    .itemId(l.getItem().getId())
                    .itemCode(l.getItemCode()).itemName(l.getItemName()).unitCode(l.getUnitCode())
                    .lotId(l.getInventoryLot() != null ? l.getInventoryLot().getId() : null)
                    .lotNumber(l.getInventoryLot() != null ? l.getInventoryLot().getLotNumber() : null)
                    .movementType(mt).quantity(l.getQuantity())
                    .unitCost(l.getUnitPrice()).lineAmount(l.getLineAmount())
                    .remarks(rem).build());
        }
        return StockAdjustmentDTO.builder()
                .id(doc.getId()).documentNo(doc.getDocumentNo())
                .warehouseId(doc.getWarehouse() != null ? doc.getWarehouse().getId() : null)
                .warehouseName(doc.getWarehouse() != null ? doc.getWarehouse().getWarehouseName() : null)
                .documentDate(doc.getDocumentDate()).status(doc.getStatus())
                .referenceNo(doc.getReferenceNo()).remarks(doc.getRemarks())
                .lines(lines)
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
                .createdBy(doc.getCreatedBy()).updatedBy(doc.getUpdatedBy()).build();
    }

    private StockTransferDTO toTransferDTO(BusinessDocument doc, Warehouse srcWh) {
        List<StockTransferDTO.LineDTO> lines = new ArrayList<>();
        for (BusinessDocumentLine l : doc.getLines()) {
            String raw = l.getRemarks() != null ? l.getRemarks() : "";
            String rem = raw.contains("|") ? raw.substring(raw.indexOf("|") + 1) : raw;
            lines.add(StockTransferDTO.LineDTO.builder()
                    .id(l.getId()).lineNumber(l.getLineNumber())
                    .itemId(l.getItem().getId())
                    .itemCode(l.getItemCode()).itemName(l.getItemName()).unitCode(l.getUnitCode())
                    .lotId(l.getInventoryLot() != null ? l.getInventoryLot().getId() : null)
                    .lotNumber(l.getInventoryLot() != null ? l.getInventoryLot().getLotNumber() : null)
                    .quantity(l.getQuantity()).remarks(rem).build());
        }
        return StockTransferDTO.builder()
                .id(doc.getId()).documentNo(doc.getDocumentNo())
                .warehouseId(doc.getWarehouse() != null ? doc.getWarehouse().getId() : null)
                .warehouseName(doc.getWarehouse() != null ? doc.getWarehouse().getWarehouseName() : null)
                .sourceWarehouseId(srcWh != null ? srcWh.getId() : null)
                .sourceWarehouseName(srcWh != null ? srcWh.getWarehouseName() : null)
                .documentDate(doc.getDocumentDate()).status(doc.getStatus())
                .referenceNo(doc.getReferenceNo())
                .vehicleNumber(doc.getVehicleNumber()).driverName(doc.getDriverName())
                .remarks(doc.getRemarks()).lines(lines)
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
                .createdBy(doc.getCreatedBy()).updatedBy(doc.getUpdatedBy()).build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private BusinessDocument findDoc(Long id, DocumentType type) {
        return docRepo.findById(id).filter(d -> d.getDocumentType() == type)
                .orElseThrow(() -> new IllegalArgumentException("Document #" + id + " not found."));
    }

    private void guardDraft(BusinessDocument doc) {
        if (!"DRAFT".equals(doc.getStatus()))
            throw new IllegalArgumentException("Only DRAFT documents can be modified. Current status: " + doc.getStatus());
    }

    private Warehouse resolveWarehouse(Long whId) {
        if (whId == null) throw new IllegalArgumentException("Warehouse is required.");
        return warehouseRepo.findById(whId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse #" + whId + " not found."));
    }

    private Item resolveItem(Long itemId) {
        if (itemId == null) throw new IllegalArgumentException("Item is required.");
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item #" + itemId + " not found."));
    }
}
