package com.asg.spindleserp.fixedassets.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.fixedassets.dto.*;
import com.asg.spindleserp.fixedassets.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * FixedAssetService
 *
 * Covers:
 *  Asset Category CRUD
 *  Asset Master CRUD
 *  Depreciation Run — calculate, preview, post, reverse
 *  Asset Disposal — sale, write-off, transfer, scrap, donation
 */
public interface FixedAssetService {

    // ── Asset Category ────────────────────────────────────────────────────

    AssetCategoryDTO createCategory(AssetCategoryDTO dto);
    AssetCategoryDTO updateCategory(Long id, AssetCategoryDTO dto);
    AssetCategoryDTO findCategoryById(Long id);
    List<AssetCategoryDTO> findAllCategories();
    AssetCategoryDTO toggleCategoryStatus(Long id);
    void deleteCategory(Long id);
    DataTableResponse categoryDatatable(int draw, int start, int length, String search);
    /** AJAX Select2 for category picker */
    Map<String, Object> searchCategories(String q, int page);
    AssetCategoryDTO toDTO(AssetCategory entity);

    // ── Asset Master ──────────────────────────────────────────────────────

    AssetDTO createAsset(AssetDTO dto);
    AssetDTO updateAsset(Long id, AssetDTO dto);
    AssetDTO findAssetById(Long id);
    void deleteAsset(Long id);
    DataTableResponse assetDatatable(int draw, int start, int length, String search, String status);
    /** AJAX Select2 for asset picker (used in depreciation / disposal) */
    Map<String, Object> searchAssets(String q, int page);
    AssetDTO toDTO(Asset entity);

    // ── Depreciation Run ──────────────────────────────────────────────────

    /**
     * Calculates depreciation for all ACTIVE assets in the org.
     * Saves as DRAFT run with computed lines. Does NOT update asset balances yet.
     */
    DepreciationRunDTO calculateDepreciation(DepreciationRunDTO dto);

    /** Post a COMPLETED run: updates Asset.accumulatedDepreciation + bookValue, posts GL journal */
    DepreciationRunDTO postDepreciationRun(Long id);

    /** Reverse a POSTED run: reverses asset balance changes and GL entry */
    DepreciationRunDTO reverseDepreciationRun(Long id);

    DepreciationRunDTO findRunById(Long id);
    DataTableResponse runDatatable(int draw, int start, int length, String search);
    DepreciationRunDTO toDTO(DepreciationRun entity);

    // ── Asset Disposal ────────────────────────────────────────────────────

    /**
     * Records disposal and:
     *  - Marks asset status = DISPOSED / SOLD / TRANSFERRED / WRITTEN_OFF / SCRAP
     *  - Calculates gain/loss
     *  - Posts GL journal entry
     */
    AssetDisposalDTO disposeAsset(AssetDisposalDTO dto);

    AssetDisposalDTO findDisposalByAsset(Long assetId);
    DataTableResponse disposalDatatable(int draw, int start, int length, String search);
    AssetDisposalDTO toDTO(AssetDisposal entity);
}
