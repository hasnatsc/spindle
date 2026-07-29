package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.enums.DocumentType;
import com.asg.spindleserp.common.enums.MovementType;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import com.asg.spindleserp.ecommerce.order.entity.EcOrderItem;
import com.asg.spindleserp.global.entity.*;
import com.asg.spindleserp.global.repository.*;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.entity.Warehouse;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.organization.repository.WarehouseRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * EcInventoryServiceImpl — core stock engine for ecommerce operations.
 *
 * Follows the same pattern as StockMovementServiceImpl.postInventoryTransaction()
 * and SalesServiceImpl.postStockTransaction().
 *
 * {@code BusinessDocument} records are created for physical movements
 * (SALES_ISSUE, RETURN_FROM_CUSTOMER) so that {@link InventoryTransaction}
 * has the required document FK. Reservation-only operations
 * (reserveStock / releaseReservation) update reservedQuantity directly
 * without creating InventoryTransaction rows.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EcInventoryServiceImpl implements EcInventoryService {

    private final InventoryStockBalanceRepository balanceRepo;
    private final InventoryTransactionRepository  txRepo;
    private final BusinessDocumentRepository       docRepo;
    private final WarehouseRepository              whRepo;
    private final OrganizationRepository           orgRepo;
    private final DocumentSequenceService          seqService;

    private static final DateTimeFormatter YY_FMT = DateTimeFormatter.ofPattern("yy");

    // ═════════════════════════════════════════════════════════════════════
    // READ-ONLY — available stock queries
    // ═════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public BigDecimal availableStockAcrossWarehouses(Long itemId) {
        if (itemId == null) return BigDecimal.ZERO;
        List<InventoryStockBalance> balances = balanceRepo
                .findByItemIdAndQuantityGreaterThan(itemId, BigDecimal.ZERO);
        return balances.stream()
                .map(b -> b.getQuantity().subtract(
                        b.getReservedQuantity() != null ? b.getReservedQuantity() : BigDecimal.ZERO))
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal availableStock(Long itemId, Long warehouseId, Long lotId) {
        return balanceRepo.findByItemIdAndWarehouseIdAndLotId(itemId, warehouseId, lotId)
                .map(b -> {
                    BigDecimal reserved = b.getReservedQuantity() != null ? b.getReservedQuantity() : BigDecimal.ZERO;
                    return b.getQuantity().subtract(reserved);
                })
                .orElse(BigDecimal.ZERO);
    }

    // ═════════════════════════════════════════════════════════════════════
    // RESERVE — CONFIRMED transition
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void reserveStock(EcOrder order) {
        if (order.isStockReserved()) {
            log.warn("Stock already reserved for order {}", order.getOrderNo());
            return;
        }

        Long whId = ContextProvider.getWarehouseId();
        if (whId == null)
            throw new IllegalArgumentException("Warehouse context is required to reserve stock. Set a default warehouse for your user.");

        for (EcOrderItem oi : order.getOrderItems()) {
            if (oi.getItem() == null) continue;
            Item item = oi.getItem();
            Long lotId = oi.getInventoryLot() != null ? oi.getInventoryLot().getId() : null;

            InventoryStockBalance balance = balanceRepo
                    .findByItemIdAndWarehouseIdAndLotId(item.getId(), whId, lotId)
                    .orElse(null);

            if (balance == null) {
                log.warn("No stock balance found for item '{}' in warehouse #{}. Reservation skipped for this line.",
                        item.getItemCode(), whId);
                continue;
            }

            BigDecimal currentReserved = balance.getReservedQuantity() != null
                    ? balance.getReservedQuantity() : BigDecimal.ZERO;
            BigDecimal available = balance.getQuantity().subtract(currentReserved);

            if (available.compareTo(oi.getQuantity()) < 0)
                throw new IllegalArgumentException(
                        "Insufficient stock for '" + item.getItemName() +
                        "'. Available: " + available.stripTrailingZeros().toPlainString() +
                        ", Ordered: " + oi.getQuantity().stripTrailingZeros().toPlainString());

            balance.setReservedQuantity(currentReserved.add(oi.getQuantity()));
            balance.setLastTransactionTime(LocalDateTime.now());
            balanceRepo.save(balance);

            log.debug("Reserved {} of item '{}' in warehouse #{}", oi.getQuantity(), item.getItemCode(), whId);
        }

        order.setStockReserved(true);
        log.info("Stock reserved for order {}", order.getOrderNo());
    }

    // ═════════════════════════════════════════════════════════════════════
    // DEDUCT — SHIPPED transition
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void deductStock(EcOrder order) {
        if (order.isStockPosted()) {
            log.warn("Stock already deducted for order {}", order.getOrderNo());
            return;
        }

        Long orgId = ContextProvider.getOrganizationId();
        if (orgId == null)
            throw new IllegalArgumentException("Organization context is required to deduct stock.");

        Long whId = ContextProvider.getWarehouseId();
        if (whId == null)
            throw new IllegalArgumentException("Warehouse context is required to deduct stock.");

        Organization org = orgRepo.getReferenceById(orgId);
        Warehouse wh = whRepo.getReferenceById(whId);

        // ── 1. Create ECOMMERCE_SALE BusinessDocument ──────────────────────
        String docNo = seqService.nextDocumentNumber(orgId, "ECSL",
                LocalDate.now().format(YY_FMT));

        BusinessDocument doc = BusinessDocument.builder()
                .organization(org)
                .documentNo(docNo)
                .documentDate(LocalDate.now())
                .documentType(DocumentType.ECOMMERCE_SALE)
                .status("CONFIRMED")
                .warehouse(wh)
                .referenceNo(order.getOrderNo())
                .remarks("Auto-created from ecommerce order: " + order.getOrderNo())
                .stockPosted(true)
                .build();

        // ── 2. Build lines from order items ────────────────────────────────
        int lineNo = 1;
        for (EcOrderItem oi : order.getOrderItems()) {
            if (oi.getItem() == null) continue;
            Item item = oi.getItem();

            BusinessDocumentLine line = BusinessDocumentLine.builder()
                    .organizationId(orgId)
                    .document(doc)
                    .item(item)
                    .lineNumber(lineNo++)
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .unitCode(item.getSalesUnitCode())
                    .quantity(oi.getQuantity())
                    .unitPrice(oi.getUnitPrice())
                    .lineAmount(oi.getLineTotal())
                    .remarks(oi.getProduct() != null ? oi.getProduct().getProductTitle() : null)
                    .build();
            doc.getLines().add(line);
        }

        doc = docRepo.save(doc);

        // ── 3. Post SALES_ISSUE for each line ──────────────────────────────
        for (BusinessDocumentLine line : doc.getLines()) {
            postSaleTransaction(doc, line, MovementType.SALES_ISSUE, wh);
        }

        // ── 4. Set warehouse on order items ────────────────────────────────
        for (EcOrderItem oi : order.getOrderItems()) {
            oi.setWarehouse(wh);
        }

        order.setStockPosted(true);
        log.info("Stock deducted for order {} — SALES_ISSUE document {} created", order.getOrderNo(), docNo);
    }

    // ═════════════════════════════════════════════════════════════════════
    // RELEASE RESERVATION — CANCELLED transition (pre-SHIPPED)
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void releaseReservation(EcOrder order) {
        if (!order.isStockReserved()) {
            log.warn("No stock reservation to release for order {}", order.getOrderNo());
            return;
        }

        Long whId = ContextProvider.getWarehouseId();
        if (whId == null) {
            log.warn("No warehouse context — cannot release reservation for order {}", order.getOrderNo());
            return;
        }

        for (EcOrderItem oi : order.getOrderItems()) {
            if (oi.getItem() == null) continue;
            Long lotId = oi.getInventoryLot() != null ? oi.getInventoryLot().getId() : null;

            balanceRepo.findByItemIdAndWarehouseIdAndLotId(oi.getItem().getId(), whId, lotId)
                    .ifPresent(balance -> {
                        BigDecimal current = balance.getReservedQuantity() != null
                                ? balance.getReservedQuantity() : BigDecimal.ZERO;
                        BigDecimal newReserved = current.subtract(oi.getQuantity());
                        balance.setReservedQuantity(newReserved.max(BigDecimal.ZERO));
                        balance.setLastTransactionTime(LocalDateTime.now());
                        balanceRepo.save(balance);
                    });
        }

        order.setStockReserved(false);
        log.info("Reservation released for order {}", order.getOrderNo());
    }

    // ═════════════════════════════════════════════════════════════════════
    // RESTOCK — RETURNED / REFUNDED transition
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void restock(EcOrder order) {
        if (!order.isStockPosted()) {
            log.warn("Stock was not deducted for order {} — nothing to restock", order.getOrderNo());
            return;
        }

        Long orgId = ContextProvider.getOrganizationId();
        if (orgId == null)
            throw new IllegalArgumentException("Organization context is required to restock.");

        Long whId = ContextProvider.getWarehouseId();
        if (whId == null)
            throw new IllegalArgumentException("Warehouse context is required to restock.");

        Organization org = orgRepo.getReferenceById(orgId);
        Warehouse wh = whRepo.getReferenceById(whId);

        // ── 1. Create CREDIT_NOTE BusinessDocument ────────────────────────
        String docNo = seqService.nextDocumentNumber(orgId, "CN",
                LocalDate.now().format(YY_FMT));

        BusinessDocument doc = BusinessDocument.builder()
                .organization(org)
                .documentNo(docNo)
                .documentDate(LocalDate.now())
                .documentType(DocumentType.CREDIT_NOTE)
                .status("CONFIRMED")
                .warehouse(wh)
                .referenceNo(order.getOrderNo() + "-RETURN")
                .remarks("Auto-created from ecommerce return: " + order.getOrderNo())
                .stockPosted(true)
                .build();

        // ── 2. Build lines from order items ────────────────────────────────
        int lineNo = 1;
        for (EcOrderItem oi : order.getOrderItems()) {
            if (oi.getItem() == null) continue;
            Item item = oi.getItem();

            BusinessDocumentLine line = BusinessDocumentLine.builder()
                    .organizationId(orgId)
                    .document(doc)
                    .item(item)
                    .lineNumber(lineNo++)
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .unitCode(item.getSalesUnitCode())
                    .quantity(oi.getQuantity())
                    .unitPrice(oi.getUnitPrice())
                    .lineAmount(oi.getLineTotal())
                    .remarks("Ecommerce return: " + order.getOrderNo())
                    .build();
            doc.getLines().add(line);
        }

        doc = docRepo.save(doc);

        // ── 3. Post RETURN_FROM_CUSTOMER for each line ─────────────────────
        for (BusinessDocumentLine line : doc.getLines()) {
            postSaleTransaction(doc, line, MovementType.RETURN_FROM_CUSTOMER, wh);
        }

        order.setStockPosted(false);
        log.info("Stock restocked for order {} — CREDIT_NOTE document {} created", order.getOrderNo(), docNo);
    }

    // ═════════════════════════════════════════════════════════════════════
    // CORE STOCK ENGINE
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Posts one InventoryTransaction and updates InventoryStockBalance atomically.
     *
     * For SALES_ISSUE (outbound): quantity is negated, reservedQuantity is also
     * decremented since the reservation is fulfilled.
     *
     * For RETURN_FROM_CUSTOMER (inbound): quantity is positive.
     */
    private void postSaleTransaction(BusinessDocument doc, BusinessDocumentLine line,
                                      MovementType movementType, Warehouse warehouse) {
        boolean isInbound = movementType == MovementType.RETURN_FROM_CUSTOMER;
        BigDecimal qtyChange = isInbound ? line.getQuantity() : line.getQuantity().negate();

        Long lotId = line.getInventoryLot() != null ? line.getInventoryLot().getId() : null;

        // ── Upsert stock balance ───────────────────────────────────────────
        InventoryStockBalance balance = balanceRepo
                .findByItemIdAndWarehouseIdAndLotId(line.getItem().getId(), warehouse.getId(), lotId)
                .orElseGet(() -> InventoryStockBalance.builder()
                        .item(line.getItem())
                        .warehouse(warehouse)
                        .quantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .build());

        BigDecimal newQty = balance.getQuantity().add(qtyChange);
        if (!isInbound && newQty.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException(
                    "Negative stock would result for item '" + line.getItemCode() +
                    "' in warehouse '" + warehouse.getWarehouseCode() + "'.");

        balance.setQuantity(newQty);
        balance.setLastTransactionTime(LocalDateTime.now());

        // For SALES_ISSUE: also decrement reservedQuantity (reservation fulfilled)
        if (!isInbound) {
            BigDecimal currentReserved = balance.getReservedQuantity() != null
                    ? balance.getReservedQuantity() : BigDecimal.ZERO;
            BigDecimal newReserved = currentReserved.subtract(line.getQuantity());
            balance.setReservedQuantity(newReserved.max(BigDecimal.ZERO));
        }

        // Update average cost for inbound movements
        if (isInbound && line.getUnitPrice() != null) {
            updateAverageCost(balance, line.getQuantity(), line.getUnitPrice());
        }

        balanceRepo.save(balance);

        // ── Write ledger entry ─────────────────────────────────────────────
        InventoryTransaction tx = InventoryTransaction.builder()
                .organizationId(doc.getOrganization().getId())
                .item(line.getItem())
                .warehouse(warehouse)
                .lot(line.getInventoryLot())
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

    /**
     * Weighted-average cost calculation for inbound stock.
     * Mirrors StockMovementServiceImpl.updateAverageCost().
     */
    private void updateAverageCost(InventoryStockBalance balance, BigDecimal inQty, BigDecimal inCost) {
        if (inQty.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal oldQty = balance.getQuantity().subtract(inQty);
        BigDecimal oldCost = balance.getAverageCost() != null ? balance.getAverageCost() : BigDecimal.ZERO;
        BigDecimal newTotalValue = oldQty.multiply(oldCost).add(inQty.multiply(inCost));
        BigDecimal newQty = balance.getQuantity();
        if (newQty.compareTo(BigDecimal.ZERO) > 0)
            balance.setAverageCost(newTotalValue.divide(newQty, 4, java.math.RoundingMode.HALF_UP));
    }
}
