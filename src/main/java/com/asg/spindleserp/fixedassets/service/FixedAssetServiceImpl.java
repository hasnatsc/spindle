package com.asg.spindleserp.fixedassets.service;

import com.asg.spindleserp.accounts.entity.*;
import com.asg.spindleserp.accounts.repository.*;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.VoucherType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.fixedassets.dto.*;
import com.asg.spindleserp.fixedassets.entity.*;
import com.asg.spindleserp.fixedassets.repository.*;
import com.asg.spindleserp.organization.repository.*;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FixedAssetServiceImpl implements FixedAssetService {

    private final AssetRepository             assetRepo;
    private final AssetCategoryRepository     categoryRepo;
    private final AssetDisposalRepository     disposalRepo;
    private final DepreciationRunRepository   runRepo;
    private final DepreciationRunLineRepository lineRepo;
    private final ChartOfAccountRepository    coaRepo;
    private final JournalEntryMasterRepository masterRepo;
    private final OrganizationRepository      orgRepo;
    private final DepartmentRepository        deptRepo;
    private final CostCenterRepository        ccRepo;
    private final WarehouseRepository         whRepo;
    private final DocumentSequenceService     seqService;
    private final JdbcTemplate                jdbcTemplate;

    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");

    // =========================================================================
    // ASSET CATEGORY
    // =========================================================================

    @Override
    public AssetCategoryDTO createCategory(AssetCategoryDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (categoryRepo.existsByOrganizationIdAndCode(orgId, dto.getCode().trim().toUpperCase())) {
            throw new IllegalArgumentException("Category code '" + dto.getCode() + "' already exists.");
        }
        return toDTO(categoryRepo.save(buildCategory(dto, new AssetCategory())));
    }

    @Override
    public AssetCategoryDTO updateCategory(Long id, AssetCategoryDTO dto) {
        AssetCategory entity = findCategory(id);
        return toDTO(categoryRepo.save(buildCategory(dto, entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetCategoryDTO findCategoryById(Long id) { return toDTO(findCategory(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<AssetCategoryDTO> findAllCategories() {
        return categoryRepo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public AssetCategoryDTO toggleCategoryStatus(Long id) {
        AssetCategory e = findCategory(id);
        e.setActive(!e.isActive());
        return toDTO(categoryRepo.save(e));
    }

    @Override
    public void deleteCategory(Long id) {
        AssetCategory e = findCategory(id);
        if (assetRepo.countByOrganizationIdAndStatus(
                e.getOrganization().getId(), Asset.AssetStatus.ACTIVE) > 0) {
            throw new IllegalStateException("Cannot delete category with active assets.");
        }
        categoryRepo.delete(e);
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse categoryDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND c.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList("c.code", "c.name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY c.code)            AS sl,
                COUNT(*)     OVER ()                           AS full_count,
                c.id, c.code, c.name, c.default_dep_method,
                COALESCE(c.default_useful_life_years::text,'—') AS useful_life,
                COALESCE(c.default_dep_rate::text,'—')         AS dep_rate,
                COALESCE(p.name, '—')                          AS parent_name,
                CASE WHEN c.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-secondary">Inactive</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="catShow('  || c.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="catEdit('  || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="catToggle('|| c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="catDelete('|| c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                 AS actions
            FROM fa_asset_categories c
            LEFT JOIN fa_asset_categories p ON p.id = c.parent_id
            %s
            ORDER BY c.code
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchCategories(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int pageSize = 30, offset = (page - 1) * pageSize;
        String sql = "SELECT id, code, name FROM fa_asset_categories WHERE is_active = true"
            + (orgId != null ? " AND organization_id = " + orgId : "")
            + (q != null && !q.isBlank() ? " AND (code ILIKE '%" + q.replace("'","''") + "%' OR name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY code LIMIT " + (pageSize + 1) + " OFFSET " + offset;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        boolean hasMore = rows.size() > pageSize;
        List<Map<String, Object>> items = rows.stream().limit(pageSize).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   r.get("id"));
            m.put("text", r.get("code") + " — " + r.get("name"));
            m.put("code", r.get("code"));
            m.put("name", r.get("name"));
            return m;
        }).toList();
        return Map.of("items", items, "hasMore", hasMore);
    }

    // =========================================================================
    // ASSET MASTER
    // =========================================================================

    @Override
    public AssetDTO createAsset(AssetDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (assetRepo.existsByOrganizationIdAndAssetCode(orgId, dto.getAssetCode().trim())) {
            throw new IllegalArgumentException("Asset code '" + dto.getAssetCode() + "' already exists.");
        }
        Asset entity = buildAsset(dto, new Asset());
        // Auto-populate from category defaults if not set
        applyDefaultsFromCategory(entity, dto);
        // Compute initial book value
        BigDecimal total = (entity.getPurchaseCost() != null ? entity.getPurchaseCost() : BigDecimal.ZERO)
                .add(entity.getInstallationCost() != null ? entity.getInstallationCost() : BigDecimal.ZERO);
        entity.setCurrentBookValue(total.subtract(entity.getAccumulatedDepreciation()));
        return toDTO(assetRepo.save(entity));
    }

    @Override
    public AssetDTO updateAsset(Long id, AssetDTO dto) {
        Asset entity = findAsset(id);
        buildAsset(dto, entity);
        return toDTO(assetRepo.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetDTO findAssetById(Long id) { return toDTO(findAsset(id)); }

    @Override
    public void deleteAsset(Long id) {
        Asset entity = findAsset(id);
        if (entity.getStatus() == Asset.AssetStatus.ACTIVE) {
            throw new IllegalStateException("Active assets cannot be deleted. Dispose first.");
        }
        assetRepo.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse assetDatatable(int draw, int start, int length, String search, String status) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND a.organization_id = " + orgId : "")
            + (status != null && !status.isBlank() ? " AND a.status = '" + status + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "a.asset_code", "a.asset_name", "a.serial_number", "a.location",
                "c.code", "c.name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY a.asset_code)              AS sl,
                COUNT(*)     OVER ()                                   AS full_count,
                a.id, a.asset_code, a.asset_name, a.serial_number,
                c.code || ' — ' || c.name                              AS category_name,
                TO_CHAR(a.acquisition_date, 'DD-Mon-YYYY')             AS acquisition_date,
                COALESCE(a.purchase_cost::text, '0.00')                AS purchase_cost,
                COALESCE(a.current_book_value::text, '0.00')           AS book_value,
                COALESCE(a.accumulated_depreciation::text, '0.00')     AS accum_dep,
                a.depreciation_method,
                COALESCE(a.useful_life_years::text,'—')                AS useful_life,
                COALESCE(a.location,'—')                               AS location,
                a.status,
                CASE a.status
                    WHEN 'ACTIVE'            THEN '<span class="badge bg-success">Active</span>'
                    WHEN 'DISPOSED'          THEN '<span class="badge bg-secondary">Disposed</span>'
                    WHEN 'SOLD'              THEN '<span class="badge bg-info text-dark">Sold</span>'
                    WHEN 'WRITTEN_OFF'       THEN '<span class="badge bg-dark">Written Off</span>'
                    WHEN 'TRANSFERRED'       THEN '<span class="badge bg-warning text-dark">Transferred</span>'
                    WHEN 'UNDER_MAINTENANCE' THEN '<span class="badge bg-orange">Maintenance</span>'
                    ELSE '<span class="badge bg-light text-dark">' || a.status || '</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="assetShow('     || a.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="assetEdit('     || a.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || CASE WHEN a.status = ''ACTIVE'' THEN
                        '<a href="javascript:;" onclick="assetDispose(' || a.id || ')" class="btn btn-white btn-sm" title="Dispose / Transfer"><i class="fas fa-box-archive text-danger"></i></a>'
                       ELSE '' END
                    || '<a href="javascript:;" onclick="assetDelete('   || a.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                         AS actions
            FROM fa_assets a
            JOIN fa_asset_categories c ON c.id = a.asset_category_id
            %s
            ORDER BY a.asset_code
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchAssets(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int pageSize = 30, offset = (page - 1) * pageSize;
        String sql = "SELECT id, asset_code, asset_name FROM fa_assets WHERE status = 'ACTIVE'"
            + (orgId != null ? " AND organization_id = " + orgId : "")
            + (q != null && !q.isBlank() ? " AND (asset_code ILIKE '%" + q.replace("'","''") + "%' OR asset_name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY asset_code LIMIT " + (pageSize + 1) + " OFFSET " + offset;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        boolean hasMore = rows.size() > pageSize;
        List<Map<String, Object>> items = rows.stream().limit(pageSize).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   r.get("id"));
            m.put("text", r.get("asset_code") + " — " + r.get("asset_name"));
            m.put("code", r.get("asset_code"));
            m.put("name", r.get("asset_name"));
            return m;
        }).toList();
        return Map.of("items", items, "hasMore", hasMore);
    }

    // =========================================================================
    // DEPRECIATION RUN
    // =========================================================================

    @Override
    public DepreciationRunDTO calculateDepreciation(DepreciationRunDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();

        DepreciationRun run = DepreciationRun.builder()
            .runDate(dto.getRunDate())
            .periodStart(dto.getPeriodStart())
            .periodEnd(dto.getPeriodEnd())
            .runType(DepreciationRun.RunType.valueOf(dto.getRunType()))
            .status(DepreciationRun.RunStatus.DRAFT)
            .build();
        run.setOrganization(orgRepo.getReferenceById(orgId));

        // Compute period fraction (days in period / 365)
        long daysInPeriod = dto.getPeriodStart().until(dto.getPeriodEnd()).getDays() + 1;
        BigDecimal periodFraction = BigDecimal.valueOf(daysInPeriod)
            .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        List<Asset> assets = assetRepo.findByOrganizationIdAndStatus(orgId, Asset.AssetStatus.ACTIVE);
        BigDecimal total = BigDecimal.ZERO;
        List<DepreciationRunLine> lines = new ArrayList<>();

        for (Asset asset : assets) {
            if (asset.getDepreciationStartDate() != null
                    && asset.getDepreciationStartDate().isAfter(dto.getPeriodEnd())) continue;

            BigDecimal depAmount = computeDepreciation(asset, periodFraction, daysInPeriod);
            if (depAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Cap at remaining depreciable amount
            BigDecimal depreciableAmount = depreciableBase(asset);
            BigDecimal remaining = depreciableAmount.subtract(asset.getAccumulatedDepreciation());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
            depAmount = depAmount.min(remaining);

            BigDecimal opening = asset.getCurrentBookValue() != null
                ? asset.getCurrentBookValue() : totalCost(asset);
            BigDecimal closing = opening.subtract(depAmount).max(
                asset.getResidualValue() != null ? asset.getResidualValue() : BigDecimal.ZERO);

            DepreciationRunLine line = DepreciationRunLine.builder()
                .depreciationRun(run)
                .asset(asset)
                .depreciationMethod(asset.getDepreciationMethod().name())
                .openingBookValue(opening)
                .depreciationAmount(depAmount)
                .closingBookValue(closing)
                .rateApplied(asset.getDepreciationRate())
                .build();
            lines.add(line);
            total = total.add(depAmount);
        }

        run.setTotalAssets(lines.size());
        run.setTotalDepreciation(total.setScale(2, RoundingMode.HALF_UP));
        run.setStatus(DepreciationRun.RunStatus.COMPLETED);
        run.setLines(lines);
        for (DepreciationRunLine l : lines) l.setDepreciationRun(run);

        return toDTO(runRepo.save(run));
    }

    @Override
    public DepreciationRunDTO postDepreciationRun(Long id) {
        DepreciationRun run = findRun(id);
        if (run.getStatus() != DepreciationRun.RunStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED runs can be posted.");
        }
        Long orgId = run.getOrganization().getId();

        // Update each asset's accumulated depreciation and book value
        for (DepreciationRunLine line : run.getLines()) {
            Asset asset = line.getAsset();
            asset.setAccumulatedDepreciation(
                asset.getAccumulatedDepreciation().add(line.getDepreciationAmount()));
            asset.setCurrentBookValue(line.getClosingBookValue());
            asset.setLastDepRunDate(run.getPeriodEnd());
            assetRepo.save(asset);
        }

        // Post depreciation GL journal (JV)
        JournalEntryMaster journal = buildDepreciationJournal(run, orgId);
        masterRepo.save(journal);
        run.setJournalEntry(journal);
        run.setStatus(DepreciationRun.RunStatus.POSTED);
        run.setPostedBy(ContextProvider.getCurrentUsername());
        run.setPostedAt(LocalDateTime.now());

        return toDTO(runRepo.save(run));
    }

    @Override
    public DepreciationRunDTO reverseDepreciationRun(Long id) {
        DepreciationRun run = findRun(id);
        if (run.getStatus() != DepreciationRun.RunStatus.POSTED) {
            throw new IllegalStateException("Only POSTED runs can be reversed.");
        }
        // Undo asset balances
        for (DepreciationRunLine line : run.getLines()) {
            Asset asset = line.getAsset();
            asset.setAccumulatedDepreciation(
                asset.getAccumulatedDepreciation().subtract(line.getDepreciationAmount()));
            asset.setCurrentBookValue(line.getOpeningBookValue());
            assetRepo.save(asset);
        }
        run.setStatus(DepreciationRun.RunStatus.REVERSED);
        return toDTO(runRepo.save(run));
    }

    @Override
    @Transactional(readOnly = true)
    public DepreciationRunDTO findRunById(Long id) { return toDTO(findRun(id)); }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse runDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND r.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList("r.status", "r.run_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY r.id DESC)         AS sl,
                COUNT(*)     OVER ()                           AS full_count,
                r.id,
                TO_CHAR(r.run_date,     'DD-Mon-YYYY')         AS run_date,
                TO_CHAR(r.period_start, 'DD-Mon-YYYY')         AS period_start,
                TO_CHAR(r.period_end,   'DD-Mon-YYYY')         AS period_end,
                r.run_type, r.status, r.total_assets,
                COALESCE(r.total_depreciation::text,'0.00')    AS total_depreciation,
                COALESCE(r.posted_by,'—')                      AS posted_by,
                TO_CHAR(r.created_at,'DD-Mon-YYYY')            AS created_at,
                CASE r.status
                    WHEN 'DRAFT'      THEN '<span class="badge bg-secondary">Draft</span>'
                    WHEN 'COMPLETED'  THEN '<span class="badge bg-info text-dark">Computed</span>'
                    WHEN 'POSTED'     THEN '<span class="badge bg-success">Posted</span>'
                    WHEN 'REVERSED'   THEN '<span class="badge bg-warning text-dark">Reversed</span>'
                    ELSE '<span class="badge bg-light text-dark">' || r.status || '</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="runShow(' || r.id || ')" class="btn btn-white btn-sm" title="View Lines"><i class="fas fa-eye text-success"></i></a>'
                    || CASE WHEN r.status = ''COMPLETED'' THEN
                        '<a href="javascript:;" onclick="runPost(' || r.id || ')" class="btn btn-white btn-sm" title="Post"><i class="fas fa-check-circle text-primary"></i></a>'
                       ELSE '' END
                    || CASE WHEN r.status = ''POSTED'' THEN
                        '<a href="javascript:;" onclick="runReverse(' || r.id || ')" class="btn btn-white btn-sm" title="Reverse"><i class="fas fa-rotate-left text-danger"></i></a>'
                       ELSE '' END
                    || '</div>'                                 AS actions
            FROM fa_depreciation_runs r
            %s
            ORDER BY r.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.getFirst().get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // ASSET DISPOSAL
    // =========================================================================

    @Override
    public AssetDisposalDTO disposeAsset(AssetDisposalDTO dto) {
        Asset asset = findAsset(dto.getAssetId());
        if (asset.getStatus() != Asset.AssetStatus.ACTIVE) {
            throw new IllegalStateException("Asset " + asset.getAssetCode() + " is not ACTIVE.");
        }
        Long orgId = asset.getOrganization().getId();

        BigDecimal bookValue = asset.getCurrentBookValue() != null
            ? asset.getCurrentBookValue() : BigDecimal.ZERO;
        BigDecimal accumDep  = asset.getAccumulatedDepreciation();
        BigDecimal dispVal   = dto.getDisposalValue() != null ? dto.getDisposalValue() : BigDecimal.ZERO;
        BigDecimal gainLoss  = dispVal.subtract(bookValue);

        AssetDisposal disposal = AssetDisposal.builder()
            .organizationId(orgId)
            .asset(asset)
            .disposalDate(dto.getDisposalDate())
            .disposalType(AssetDisposal.DisposalType.valueOf(dto.getDisposalType()))
            .disposalValue(dispVal)
            .bookValueAtDisposal(bookValue)
            .accumulatedDepAtDisposal(accumDep)
            .gainLoss(gainLoss)
            .buyerName(dto.getBuyerName())
            .reason(dto.getReason())
            .approvedBy(dto.getApprovedBy())
            .createdBy(ContextProvider.getCurrentUsername())
            .build();

        if (dto.getTransferToDeptId() != null)
            disposal.setTransferToDept(deptRepo.getReferenceById(dto.getTransferToDeptId()));

        // Post disposal GL journal
        JournalEntryMaster journal = buildDisposalJournal(asset, disposal, gainLoss, orgId);
        masterRepo.save(journal);
        disposal.setJournalEntry(journal);

        disposalRepo.save(disposal);

        // Update asset status
        Asset.AssetStatus newStatus = switch (dto.getDisposalType()) {
            case "SALE"      -> Asset.AssetStatus.SOLD;
            case "WRITE_OFF" -> Asset.AssetStatus.WRITTEN_OFF;
            case "TRANSFER"  -> Asset.AssetStatus.TRANSFERRED;
            default          -> Asset.AssetStatus.DISPOSED;
        };
        asset.setStatus(newStatus);
        asset.setCurrentBookValue(BigDecimal.ZERO);
        assetRepo.save(asset);

        return toDTO(disposal);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetDisposalDTO findDisposalByAsset(Long assetId) {
        return disposalRepo.findByAssetId(assetId)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("No disposal record for asset #" + assetId));
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse disposalDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND d.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "a.asset_code", "a.asset_name", "d.disposal_type", "d.buyer_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY d.id DESC)          AS sl,
                COUNT(*)     OVER ()                            AS full_count,
                d.id, a.asset_code, a.asset_name, d.disposal_type,
                TO_CHAR(d.disposal_date,'DD-Mon-YYYY')          AS disposal_date,
                COALESCE(d.disposal_value::text,'0.00')         AS disposal_value,
                COALESCE(d.book_value_at_disposal::text,'0.00') AS book_value,
                COALESCE(d.gain_loss::text,'0.00')              AS gain_loss,
                COALESCE(d.buyer_name,'—')                      AS buyer_name,
                COALESCE(d.approved_by,'—')                     AS approved_by,
                TO_CHAR(d.created_at,'DD-Mon-YYYY')             AS created_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="dispShow(' || d.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '</div>'                                  AS actions
            FROM fa_asset_disposals d
            JOIN fa_assets a ON a.id = d.asset_id
            %s
            ORDER BY d.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // MAPPING
    // =========================================================================

    @Override
    public AssetCategoryDTO toDTO(AssetCategory e) {
        AssetCategoryDTO d = AssetCategoryDTO.builder()
            .id(e.getId())
            .code(e.getCode()).name(e.getName()).description(e.getDescription())
            .defaultDepMethod(e.getDefaultDepMethod() != null ? e.getDefaultDepMethod().name() : "STRAIGHT_LINE")
            .defaultUsefulLifeYears(e.getDefaultUsefulLifeYears())
            .defaultDepRate(e.getDefaultDepRate())
            .defaultResidualPct(e.getDefaultResidualPct())
            .active(e.isActive())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .createdBy(e.getCreatedBy())
            .build();
        if (e.getParent() != null) { d.setParentId(e.getParent().getId()); d.setParentName(e.getParent().getName()); }
        if (e.getGlAssetAccount()    != null) { d.setGlAssetAccountId(e.getGlAssetAccount().getId());    d.setGlAssetAccountDisplay(e.getGlAssetAccount().getAccountCode()+" — "+e.getGlAssetAccount().getAccountName()); }
        if (e.getGlDepExpAccount()   != null) { d.setGlDepExpAccountId(e.getGlDepExpAccount().getId());  d.setGlDepExpAccountDisplay(e.getGlDepExpAccount().getAccountCode()+" — "+e.getGlDepExpAccount().getAccountName()); }
        if (e.getGlAccumDepAccount() != null) { d.setGlAccumDepAccountId(e.getGlAccumDepAccount().getId()); d.setGlAccumDepAccountDisplay(e.getGlAccumDepAccount().getAccountCode()+" — "+e.getGlAccumDepAccount().getAccountName()); }
        if (e.getGlDisposalAccount() != null) { d.setGlDisposalAccountId(e.getGlDisposalAccount().getId()); d.setGlDisposalAccountDisplay(e.getGlDisposalAccount().getAccountCode()+" — "+e.getGlDisposalAccount().getAccountName()); }
        return d;
    }

    @Override
    public AssetDTO toDTO(Asset e) {
        AssetDTO d = AssetDTO.builder()
            .id(e.getId())
            .assetCode(e.getAssetCode()).assetName(e.getAssetName()).description(e.getDescription())
            .serialNumber(e.getSerialNumber()).model(e.getModel()).manufacturer(e.getManufacturer())
            .acquisitionDate(e.getAcquisitionDate()).capitalisationDate(e.getCapitalisationDate())
            .purchaseCost(e.getPurchaseCost()).installationCost(e.getInstallationCost())
            .currency(e.getCurrency()).exchangeRate(e.getExchangeRate())
            .depreciationMethod(e.getDepreciationMethod() != null ? e.getDepreciationMethod().name() : "STRAIGHT_LINE")
            .usefulLifeYears(e.getUsefulLifeYears()).residualValue(e.getResidualValue())
            .depreciationRate(e.getDepreciationRate()).depreciationStartDate(e.getDepreciationStartDate())
            .accumulatedDepreciation(e.getAccumulatedDepreciation())
            .currentBookValue(e.getCurrentBookValue()).lastDepRunDate(e.getLastDepRunDate())
            .location(e.getLocation()).status(e.getStatus() != null ? e.getStatus().name() : "ACTIVE")
            .condition(e.getCondition()).warrantyExpiryDate(e.getWarrantyExpiryDate())
            .insurancePolicyNo(e.getInsurancePolicyNo()).insuranceExpiryDate(e.getInsuranceExpiryDate())
            .barcode(e.getBarcode()).notes(e.getNotes())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getAssetCategory()         != null) { d.setAssetCategoryId(e.getAssetCategory().getId()); d.setAssetCategoryDisplay(e.getAssetCategory().getCode()+" — "+e.getAssetCategory().getName()); }
        if (e.getDepartment()            != null) { d.setDepartmentId(e.getDepartment().getId());        d.setDepartmentDisplay(e.getDepartment().getName()); }
        if (e.getCostCenter()            != null) { d.setCostCenterId(e.getCostCenter().getId());        d.setCostCenterDisplay(e.getCostCenter().getCostCenterName()); }
        if (e.getWarehouse()             != null) { d.setWarehouseId(e.getWarehouse().getId());          d.setWarehouseDisplay(e.getWarehouse().getWarehouseName()); }
        if (e.getSupplier()              != null) { d.setSupplierId(e.getSupplier().getId());            d.setSupplierDisplay(e.getSupplier().getSubAccountCode()+" — "+e.getSupplier().getSubAccountName()); }
        return d;
    }

    @Override
    public DepreciationRunDTO toDTO(DepreciationRun e) {
        DepreciationRunDTO d = DepreciationRunDTO.builder()
            .id(e.getId()).runDate(e.getRunDate()).periodStart(e.getPeriodStart()).periodEnd(e.getPeriodEnd())
            .runType(e.getRunType().name()).status(e.getStatus().name())
            .totalAssets(e.getTotalAssets()).totalDepreciation(e.getTotalDepreciation())
            .postedBy(e.getPostedBy())
            .postedAt(e.getPostedAt() != null ? e.getPostedAt().toString() : null)
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .createdBy(e.getCreatedBy())
            .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
            .build();
        if (e.getLines() != null) {
            d.setLines(e.getLines().stream().map(l -> DepreciationRunDTO.LineDTO.builder()
                .id(l.getId())
                .assetId(l.getAsset().getId())
                .assetCode(l.getAsset().getAssetCode())
                .assetName(l.getAsset().getAssetName())
                .depreciationMethod(l.getDepreciationMethod())
                .openingBookValue(l.getOpeningBookValue())
                .depreciationAmount(l.getDepreciationAmount())
                .closingBookValue(l.getClosingBookValue())
                .rateApplied(l.getRateApplied())
                .unitsProduced(l.getUnitsProduced())
                .notes(l.getNotes())
                .build()).toList());
        }
        return d;
    }

    @Override
    public AssetDisposalDTO toDTO(AssetDisposal e) {
        AssetDisposalDTO d = AssetDisposalDTO.builder()
            .id(e.getId())
            .assetId(e.getAsset().getId())
            .assetCode(e.getAsset().getAssetCode())
            .assetName(e.getAsset().getAssetName())
            .disposalDate(e.getDisposalDate())
            .disposalType(e.getDisposalType().name())
            .disposalValue(e.getDisposalValue())
            .bookValueAtDisposal(e.getBookValueAtDisposal())
            .accumulatedDepAtDisposal(e.getAccumulatedDepAtDisposal())
            .gainLoss(e.getGainLoss())
            .buyerName(e.getBuyerName()).reason(e.getReason()).approvedBy(e.getApprovedBy())
            .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
            .createdBy(e.getCreatedBy())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .build();
        if (e.getTransferToDept() != null) { d.setTransferToDeptId(e.getTransferToDept().getId()); d.setTransferToDeptDisplay(e.getTransferToDept().getName()); }
        return d;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /** Straight-Line, Declining-Balance, or Units-of-Production depreciation for one period */
    private BigDecimal computeDepreciation(Asset asset, BigDecimal periodFraction, long daysInPeriod) {
        AssetCategory.DepreciationMethod method = asset.getDepreciationMethod();
        BigDecimal depBase = depreciableBase(asset);

        return switch (method) {
            case STRAIGHT_LINE -> {
                // SL = (Cost - ResidualValue) / UsefulLifeYears  per year × periodFraction
                if (asset.getUsefulLifeYears() == null || asset.getUsefulLifeYears() <= 0) yield BigDecimal.ZERO;
                BigDecimal annualDep = depBase.divide(
                    BigDecimal.valueOf(asset.getUsefulLifeYears()), 10, RoundingMode.HALF_UP);
                yield annualDep.multiply(periodFraction).setScale(2, RoundingMode.HALF_UP);
            }
            case DECLINING_BALANCE -> {
                // DB = BookValue × Rate × periodFraction
                if (asset.getDepreciationRate() == null) yield BigDecimal.ZERO;
                BigDecimal bv = asset.getCurrentBookValue() != null ? asset.getCurrentBookValue() : depBase;
                BigDecimal rate = asset.getDepreciationRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                yield bv.multiply(rate).multiply(periodFraction).setScale(2, RoundingMode.HALF_UP);
            }
            case UNITS_OF_PRODUCTION -> BigDecimal.ZERO; // populated externally via API
        };
    }

    /** Depreciable base = totalCost - residualValue */
    private BigDecimal depreciableBase(Asset asset) {
        BigDecimal cost     = totalCost(asset);
        BigDecimal residual = asset.getResidualValue() != null ? asset.getResidualValue() : BigDecimal.ZERO;
        return cost.subtract(residual).max(BigDecimal.ZERO);
    }

    private BigDecimal totalCost(Asset asset) {
        return (asset.getPurchaseCost() != null ? asset.getPurchaseCost() : BigDecimal.ZERO)
            .add(asset.getInstallationCost() != null ? asset.getInstallationCost() : BigDecimal.ZERO);
    }

    /** Build GL journal for depreciation posting — DR: Dep Exp, CR: Accum Dep */
    private JournalEntryMaster buildDepreciationJournal(DepreciationRun run, Long orgId) {
        String year  = run.getPeriodEnd().format(YY);
        String vno   = seqService.nextDocumentNumber(orgId, "DEP", year);

        JournalEntryMaster jv = new JournalEntryMaster();
        jv.setOrganization(orgRepo.getReferenceById(orgId));
        jv.setVoucherNo(vno);
        jv.setVoucherType(VoucherType.JOURNAL_VOUCHER);
        jv.setVoucherDate(run.getPeriodEnd());
        jv.setVoucherStatus("POSTED");
        jv.setPosted(true);
        jv.setPostedBy(ContextProvider.getCurrentUsername());
        jv.setPostedAt(LocalDateTime.now());
        jv.setNarration("Depreciation run " + run.getPeriodStart() + " to " + run.getPeriodEnd());
        jv.setTotalDebit(run.getTotalDepreciation());
        jv.setTotalCredit(run.getTotalDepreciation());
        jv.setTotalAmount(run.getTotalDepreciation());
        jv.setAllocatedAmount(BigDecimal.ZERO);

        // Group lines by category GL accounts
        Map<Long, BigDecimal> depExpMap   = new LinkedHashMap<>();
        Map<Long, BigDecimal> accumDepMap = new LinkedHashMap<>();

        for (DepreciationRunLine line : run.getLines()) {
            AssetCategory cat = line.getAsset().getAssetCategory();
            if (cat.getGlDepExpAccount() != null) {
                depExpMap.merge(cat.getGlDepExpAccount().getId(), line.getDepreciationAmount(), BigDecimal::add);
            }
            if (cat.getGlAccumDepAccount() != null) {
                accumDepMap.merge(cat.getGlAccumDepAccount().getId(), line.getDepreciationAmount(), BigDecimal::add);
            }
        }

        int lineNo = 1;
        for (Map.Entry<Long, BigDecimal> entry : depExpMap.entrySet()) {
            JournalEntryLine gl = new JournalEntryLine();
            gl.setJournalEntry(jv);
            gl.setAccount(coaRepo.getReferenceById(entry.getKey()));
            gl.setLineNumber(lineNo++);
            gl.setEntryType(JournalEntryLine.EntryType.DEBIT);
            gl.setAmount(entry.getValue().setScale(2, RoundingMode.HALF_UP));
            gl.setNarration("Depreciation expense");
            jv.getLines().add(gl);
        }
        for (Map.Entry<Long, BigDecimal> entry : accumDepMap.entrySet()) {
            JournalEntryLine gl = new JournalEntryLine();
            gl.setJournalEntry(jv);
            gl.setAccount(coaRepo.getReferenceById(entry.getKey()));
            gl.setLineNumber(lineNo++);
            gl.setEntryType(JournalEntryLine.EntryType.CREDIT);
            gl.setAmount(entry.getValue().setScale(2, RoundingMode.HALF_UP));
            gl.setNarration("Accumulated depreciation");
            jv.getLines().add(gl);
        }
        return jv;
    }

    /**
     * Disposal GL journal:
     *   DR: Accumulated Depreciation (remove)
     *   DR: Loss on Disposal (if disposal < book value)
     *   CR: Asset Account (remove cost)
     *   CR: Gain on Disposal (if disposal > book value)
     *   DR: Cash/Receivable = disposal proceeds (if sale)
     */
    private JournalEntryMaster buildDisposalJournal(Asset asset, AssetDisposal disposal,
                                                     BigDecimal gainLoss, Long orgId) {
        String year = disposal.getDisposalDate().format(YY);
        String vno  = seqService.nextDocumentNumber(orgId, "DISP", year);

        JournalEntryMaster jv = new JournalEntryMaster();
        jv.setOrganization(orgRepo.getReferenceById(orgId));
        jv.setVoucherNo(vno);
        jv.setVoucherType(VoucherType.JOURNAL_VOUCHER);
        jv.setVoucherDate(disposal.getDisposalDate());
        jv.setVoucherStatus("POSTED");
        jv.setPosted(true);
        jv.setPostedBy(ContextProvider.getCurrentUsername());
        jv.setPostedAt(LocalDateTime.now());
        jv.setNarration("Asset disposal: " + asset.getAssetCode() + " — " + disposal.getDisposalType().name());
        jv.setAllocatedAmount(BigDecimal.ZERO);

        AssetCategory cat = asset.getAssetCategory();
        BigDecimal cost   = totalCost(asset);
        BigDecimal accum  = asset.getAccumulatedDepreciation();

        int lineNo = 1;
        // CR: Asset account for original cost
        if (cat.getGlAssetAccount() != null) {
            jv.getLines().add(glLine(jv, coaRepo.getReferenceById(cat.getGlAssetAccount().getId()),
                lineNo++, JournalEntryLine.EntryType.CREDIT, cost, "Remove asset cost"));
        }
        // DR: Accumulated depreciation
        if (cat.getGlAccumDepAccount() != null && accum.compareTo(BigDecimal.ZERO) > 0) {
            jv.getLines().add(glLine(jv, coaRepo.getReferenceById(cat.getGlAccumDepAccount().getId()),
                lineNo++, JournalEntryLine.EntryType.DEBIT, accum, "Clear accumulated depreciation"));
        }
        // Gain/Loss entries on disposal account
        if (cat.getGlDisposalAccount() != null) {
            if (gainLoss.compareTo(BigDecimal.ZERO) > 0) {
                // Gain — CR
                jv.getLines().add(glLine(jv, coaRepo.getReferenceById(cat.getGlDisposalAccount().getId()),
                    lineNo++, JournalEntryLine.EntryType.CREDIT, gainLoss, "Gain on disposal"));
            } else if (gainLoss.compareTo(BigDecimal.ZERO) < 0) {
                // Loss — DR
                jv.getLines().add(glLine(jv, coaRepo.getReferenceById(cat.getGlDisposalAccount().getId()),
                    lineNo++, JournalEntryLine.EntryType.DEBIT, gainLoss.negate(), "Loss on disposal"));
            }
        }
        BigDecimal totalDr = jv.getLines().stream().filter(l -> l.getEntryType() == JournalEntryLine.EntryType.DEBIT).map(JournalEntryLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCr = jv.getLines().stream().filter(l -> l.getEntryType() == JournalEntryLine.EntryType.CREDIT).map(JournalEntryLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        jv.setTotalDebit(totalDr);
        jv.setTotalCredit(totalCr);
        jv.setTotalAmount(totalDr.max(totalCr));
        return jv;
    }

    private JournalEntryLine glLine(JournalEntryMaster jv, ChartOfAccount acct,
                                    int lineNo, JournalEntryLine.EntryType type,
                                    BigDecimal amount, String narration) {
        JournalEntryLine l = new JournalEntryLine();
        l.setJournalEntry(jv);
        l.setAccount(acct);
        l.setLineNumber(lineNo);
        l.setEntryType(type);
        l.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        l.setNarration(narration);
        return l;
    }

    private void applyDefaultsFromCategory(Asset entity, AssetDTO dto) {
        if (entity.getAssetCategory() == null) return;
        AssetCategory cat = entity.getAssetCategory();
        if (dto.getDepreciationMethod() == null && cat.getDefaultDepMethod() != null)
            entity.setDepreciationMethod(cat.getDefaultDepMethod());
        if (dto.getUsefulLifeYears() == null && cat.getDefaultUsefulLifeYears() != null)
            entity.setUsefulLifeYears(cat.getDefaultUsefulLifeYears());
        if (dto.getDepreciationRate() == null && cat.getDefaultDepRate() != null)
            entity.setDepreciationRate(cat.getDefaultDepRate());
        if (dto.getResidualValue() == null && cat.getDefaultResidualPct() != null) {
            BigDecimal total = totalCost(entity);
            BigDecimal residual = total.multiply(cat.getDefaultResidualPct()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            entity.setResidualValue(residual);
        }
        if (entity.getDepreciationStartDate() == null)
            entity.setDepreciationStartDate(entity.getCapitalisationDate() != null
                ? entity.getCapitalisationDate() : entity.getAcquisitionDate());
    }

    private Asset buildAsset(AssetDTO dto, Asset e) {
        Long orgId = ContextProvider.getOrganizationId();
        e.setAssetCategory(categoryRepo.getReferenceById(dto.getAssetCategoryId()));
        if (e.getOrganization() == null) e.setOrganization(orgRepo.getReferenceById(orgId));
        e.setAssetCode(dto.getAssetCode().trim());
        e.setAssetName(dto.getAssetName().trim());
        e.setDescription(dto.getDescription());
        e.setSerialNumber(dto.getSerialNumber());
        e.setModel(dto.getModel());
        e.setManufacturer(dto.getManufacturer());
        e.setAcquisitionDate(dto.getAcquisitionDate());
        e.setCapitalisationDate(dto.getCapitalisationDate());
        e.setPurchaseCost(dto.getPurchaseCost());
        e.setInstallationCost(dto.getInstallationCost() != null ? dto.getInstallationCost() : BigDecimal.ZERO);
        e.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "BDT");
        e.setExchangeRate(dto.getExchangeRate() != null ? dto.getExchangeRate() : BigDecimal.ONE);
        e.setDepreciationMethod(dto.getDepreciationMethod() != null
            ? AssetCategory.DepreciationMethod.valueOf(dto.getDepreciationMethod())
            : AssetCategory.DepreciationMethod.STRAIGHT_LINE);
        e.setUsefulLifeYears(dto.getUsefulLifeYears());
        e.setResidualValue(dto.getResidualValue() != null ? dto.getResidualValue() : BigDecimal.ZERO);
        e.setDepreciationRate(dto.getDepreciationRate());
        e.setDepreciationStartDate(dto.getDepreciationStartDate());
        e.setAccumulatedDepreciation(dto.getAccumulatedDepreciation() != null ? dto.getAccumulatedDepreciation() : BigDecimal.ZERO);
        e.setCurrentBookValue(dto.getCurrentBookValue());
        e.setLocation(dto.getLocation());
        e.setStatus(dto.getStatus() != null ? Asset.AssetStatus.valueOf(dto.getStatus()) : Asset.AssetStatus.ACTIVE);
        e.setCondition(dto.getCondition() != null ? dto.getCondition() : "GOOD");
        e.setWarrantyExpiryDate(dto.getWarrantyExpiryDate());
        e.setInsurancePolicyNo(dto.getInsurancePolicyNo());
        e.setInsuranceExpiryDate(dto.getInsuranceExpiryDate());
        e.setBarcode(dto.getBarcode());
        e.setNotes(dto.getNotes());
        if (dto.getDepartmentId() != null) e.setDepartment(deptRepo.getReferenceById(dto.getDepartmentId()));
        if (dto.getCostCenterId() != null) e.setCostCenter(ccRepo.getReferenceById(dto.getCostCenterId()));
        if (dto.getWarehouseId() != null) e.setWarehouse(whRepo.getReferenceById(dto.getWarehouseId()));
        String user = SecurityHelper.currentUsername().orElse("system");
        if (e.getCreatedBy() == null) e.setCreatedBy(user);
        e.setUpdatedBy(user);
        return e;
    }

    private AssetCategory buildCategory(AssetCategoryDTO dto, AssetCategory e) {
        Long orgId = ContextProvider.getOrganizationId();
        if (e.getOrganization() == null) e.setOrganization(orgRepo.getReferenceById(orgId));
        e.setCode(dto.getCode().trim().toUpperCase());
        e.setName(dto.getName().trim());
        e.setDescription(dto.getDescription());
        e.setDefaultDepMethod(dto.getDefaultDepMethod() != null
            ? AssetCategory.DepreciationMethod.valueOf(dto.getDefaultDepMethod())
            : AssetCategory.DepreciationMethod.STRAIGHT_LINE);
        e.setDefaultUsefulLifeYears(dto.getDefaultUsefulLifeYears());
        e.setDefaultDepRate(dto.getDefaultDepRate());
        e.setDefaultResidualPct(dto.getDefaultResidualPct());
        e.setActive(Boolean.TRUE.equals(dto.getActive()));
        if (dto.getParentId() != null) e.setParent(categoryRepo.getReferenceById(dto.getParentId()));
        else e.setParent(null);
        if (dto.getGlAssetAccountId()    != null) e.setGlAssetAccount(coaRepo.getReferenceById(dto.getGlAssetAccountId()));
        if (dto.getGlDepExpAccountId()   != null) e.setGlDepExpAccount(coaRepo.getReferenceById(dto.getGlDepExpAccountId()));
        if (dto.getGlAccumDepAccountId() != null) e.setGlAccumDepAccount(coaRepo.getReferenceById(dto.getGlAccumDepAccountId()));
        if (dto.getGlDisposalAccountId() != null) e.setGlDisposalAccount(coaRepo.getReferenceById(dto.getGlDisposalAccountId()));
        return e;
    }

    private DepreciationRun findRun(Long id) {
        return runRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Depreciation run #" + id + " not found."));
    }

    private Asset findAsset(Long id) {
        return assetRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Asset #" + id + " not found."));
    }

    private AssetCategory findCategory(Long id) {
        return categoryRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Asset category #" + id + " not found."));
    }
}
