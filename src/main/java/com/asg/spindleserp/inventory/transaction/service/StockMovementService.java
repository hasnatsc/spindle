package com.asg.spindleserp.inventory.transaction.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.transaction.dto.StockAdjustmentDTO;
import com.asg.spindleserp.inventory.transaction.dto.StockTransferDTO;

/**
 * StockMovementService
 *
 * Handles Stock Adjustment and Stock Transfer documents.
 * Both share the same global_business_documents table —
 * separated here for clarity and independent URLs.
 */
public interface StockMovementService {

    // ── ADJUSTMENT ────────────────────────────────────────────────────────────

    StockAdjustmentDTO createAdjustment(StockAdjustmentDTO dto);

    StockAdjustmentDTO updateAdjustment(Long id, StockAdjustmentDTO dto);

    StockAdjustmentDTO findAdjustmentById(Long id);

    /** Post (confirm) the adjustment — writes InventoryTransaction + updates StockBalance */
    StockAdjustmentDTO confirmAdjustment(Long id);

    void cancelAdjustment(Long id);

    void deleteAdjustment(Long id);

    DataTableResponse adjustmentDatatableList(int draw, int start, int length, String search);

    // ── TRANSFER ──────────────────────────────────────────────────────────────

    StockTransferDTO createTransfer(StockTransferDTO dto);

    StockTransferDTO updateTransfer(Long id, StockTransferDTO dto);

    StockTransferDTO findTransferById(Long id);

    /** Post the transfer — writes TRANSFER_OUT + TRANSFER_IN transactions */
    StockTransferDTO confirmTransfer(Long id);

    void cancelTransfer(Long id);

    void deleteTransfer(Long id);

    DataTableResponse transferDatatableList(int draw, int start, int length, String search);
}
