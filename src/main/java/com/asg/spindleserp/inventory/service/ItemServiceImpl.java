package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.inventory.dto.ItemDTO;
import com.asg.spindleserp.inventory.entity.*;
import com.asg.spindleserp.inventory.repository.*;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository         itemRepository;
    private final ItemCategoryRepository categoryRepository;
    private final ItemUomRepository      uomRepository;
    private final ItemBrandRepository    brandRepository;
    private final ItemModelRepository    modelRepository;
    private final JdbcTemplate           jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemDTO create(ItemDTO dto) {
        String code  = dto.getItemCode().trim().toUpperCase();

        if (itemRepository.existsByOrganizationIdAndItemName(ContextProvider.getOrganizationId(), dto.getItemName().trim()))
            throw new IllegalArgumentException("Item name '" + dto.getItemName().trim() + "' already exists.");

        ItemCategory cat    = resolveCategory(dto.getCategoryId());
        ItemUom      puUnit = resolveUom(dto.getPurchaseUnitId(), "Purchase unit");
        ItemUom      suUnit = resolveUom(dto.getSalesUnitId(),    "Sales unit");
        ItemUom      opUnit = resolveUom(dto.getOperationUnitId(), "Operation unit");

        Item entity = Item.builder()
                .category(cat)
                .purchaseUnit(puUnit)
                .salesUnit(suUnit)
                .operationUnit(opUnit)
                .brand(dto.getBrandId()   != null ? brandRepository.findById(dto.getBrandId()).orElse(null) : null)
                .model(dto.getModelId()   != null ? modelRepository.findById(dto.getModelId()).orElse(null) : null)
                .hsCodeId(dto.getHsCodeId())
                .originId(dto.getOriginId())
                .itemCode(code)
                .itemName(dto.getItemName().trim())
                .itemNameBn(dto.getItemNameBn())
                .itemType(resolveItemType(dto.getItemType()))
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .unitOfMeasure(opUnit.getCode())
                .purchaseUnitCode(puUnit.getCode())
                .salesUnitCode(suUnit.getCode())
                .costPrice(dto.getCostPrice())
                .unitPrice(dto.getUnitPrice())
                .taxRate(dto.getTaxRate())
                .standardCost(dto.getStandardCost())
                .minimumStock(dto.getMinimumStock())
                .maximumStock(dto.getMaximumStock())
                .reorderLevel(dto.getReorderLevel())
                .yieldPercent(dto.getYieldPercent())
                .processLossPct(dto.getProcessLossPct())
                .weight(dto.getWeight())
                .volume(dto.getVolume())
                .dimensions(dto.getDimensions())
                .shelfLifeDays(dto.getShelfLifeDays())
                .expiryDate(dto.getExpiryDate())
                .hasLotTracking(Boolean.TRUE.equals(dto.getHasLotTracking()))
                .hasSerial(Boolean.TRUE.equals(dto.getHasSerial()))
                .serialNumber(dto.getSerialNumber())
                .manufacturer(dto.getManufacturer())
                .warrantyMonths(dto.getWarrantyMonths())
                .depreciationRate(dto.getDepreciationRate())
                .casNumber(dto.getCasNumber())
                .isHazardous(Boolean.TRUE.equals(dto.getIsHazardous()))
                .safetyDataSheet(dto.getSafetyDataSheet())
                .description(dto.getDescription())
                .internalNotes(dto.getInternalNotes())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .isApproved(Boolean.TRUE.equals(dto.getIsApproved()))
                .build();
        return toDTO(itemRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemDTO update(Long id, ItemDTO dto) {
        Item   entity = findEntityById(id);
        String code   = dto.getItemCode().trim().toUpperCase();

        if (!entity.getItemName().equalsIgnoreCase(dto.getItemName().trim()) &&
                itemRepository.existsByOrganizationIdAndItemNameAndIdNot(ContextProvider.getOrganizationId(), dto.getItemName().trim(), id))
            throw new IllegalArgumentException("Item name '" + dto.getItemName().trim() + "' already exists.");

        ItemUom puUnit = resolveUom(dto.getPurchaseUnitId(), "Purchase unit");
        ItemUom suUnit = resolveUom(dto.getSalesUnitId(),    "Sales unit");
        ItemUom opUnit = resolveUom(dto.getOperationUnitId(), "Operation unit");

        entity.setCategory(resolveCategory(dto.getCategoryId()));
        entity.setPurchaseUnit(puUnit);
        entity.setSalesUnit(suUnit);
        entity.setOperationUnit(opUnit);
        entity.setBrand(dto.getBrandId() != null ? brandRepository.findById(dto.getBrandId()).orElse(null) : null);
        entity.setModel(dto.getModelId() != null ? modelRepository.findById(dto.getModelId()).orElse(null) : null);
        entity.setHsCodeId(dto.getHsCodeId());
        entity.setOriginId(dto.getOriginId());
        entity.setItemCode(code);
        entity.setItemName(dto.getItemName().trim());
        entity.setItemNameBn(dto.getItemNameBn());
        entity.setItemType(resolveItemType(dto.getItemType()));
        entity.setSku(dto.getSku());
        entity.setBarcode(dto.getBarcode());
        entity.setUnitOfMeasure(opUnit.getCode());
        entity.setPurchaseUnitCode(puUnit.getCode());
        entity.setSalesUnitCode(suUnit.getCode());
        entity.setCostPrice(dto.getCostPrice());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setTaxRate(dto.getTaxRate());
        entity.setStandardCost(dto.getStandardCost());
        entity.setMinimumStock(dto.getMinimumStock());
        entity.setMaximumStock(dto.getMaximumStock());
        entity.setReorderLevel(dto.getReorderLevel());
        entity.setYieldPercent(dto.getYieldPercent());
        entity.setProcessLossPct(dto.getProcessLossPct());
        entity.setWeight(dto.getWeight());
        entity.setVolume(dto.getVolume());
        entity.setDimensions(dto.getDimensions());
        entity.setShelfLifeDays(dto.getShelfLifeDays());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setHasLotTracking(Boolean.TRUE.equals(dto.getHasLotTracking()));
        entity.setHasSerial(Boolean.TRUE.equals(dto.getHasSerial()));
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setManufacturer(dto.getManufacturer());
        entity.setWarrantyMonths(dto.getWarrantyMonths());
        entity.setDepreciationRate(dto.getDepreciationRate());
        entity.setCasNumber(dto.getCasNumber());
        entity.setHazardous(Boolean.TRUE.equals(dto.getIsHazardous()));
        entity.setSafetyDataSheet(dto.getSafetyDataSheet());
        entity.setDescription(dto.getDescription());
        entity.setInternalNotes(dto.getInternalNotes());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        entity.setApproved(Boolean.TRUE.equals(dto.getIsApproved()));
        return toDTO(itemRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public ItemDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<ItemDTO> findAll() {
        return itemRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemDTO> findActiveByOrg(Long orgId) {
        return itemRepository.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemDTO> findByOrgAndType(Long orgId, ItemType type) {
        return itemRepository.findByOrganizationIdAndItemTypeAndIsActiveTrue(orgId, type).stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemDTO> findProductionInputs(Long orgId) {
        return itemRepository.findProductionInputItems(orgId).stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemDTO> findFinishedGoods(Long orgId) {
        return itemRepository.findFinishedItems(orgId).stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { itemRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public ItemDTO toggleStatus(Long id) {
        Item entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(itemRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "i.item_code", "i.item_name", "i.item_name_bn", "i.sku",
                        "i.barcode", "i.item_type", "cat.category_name",
                        "br.brand_name", "op.name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY i.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                i.id,
                i.item_code,
                i.item_name,
                i.item_type,
                cat.category_name,
                i.unit_of_measure,
                COALESCE(br.brand_name, '—')                AS brand_name,
                COALESCE(i.unit_price::text, '—')           AS unit_price,
                CASE WHEN i.is_approved
                    THEN '<span class="badge bg-success">Approved</span>'
                    ELSE '<span class="badge bg-secondary">Pending</span>'
                END AS approval_status,
                TO_CHAR(i.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN i.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="itemShow('   || i.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="itemEdit('   || i.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="itemToggle(' || i.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="itemDelete(' || i.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM inv_items i
            JOIN inv_item_categories cat ON cat.id  = i.category_id
            JOIN inv_item_uom        op  ON op.id   = i.operation_unit_id
            LEFT JOIN inv_item_brands br ON br.id   = i.brand_id
            %s
            ORDER BY i.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public ItemDTO toDTO(Item e) {
        return ItemDTO.builder()
                .id(e.getId())
                .categoryId(e.getCategory()     != null ? e.getCategory().getId()           : null)
                .categoryName(e.getCategory()   != null ? e.getCategory().getCategoryName() : null)
                .purchaseUnitId(e.getPurchaseUnit()     != null ? e.getPurchaseUnit().getId()   : null)
                .purchaseUnitCode(e.getPurchaseUnit()   != null ? e.getPurchaseUnit().getCode() : null)
                .salesUnitId(e.getSalesUnit()           != null ? e.getSalesUnit().getId()      : null)
                .salesUnitCode(e.getSalesUnit()         != null ? e.getSalesUnit().getCode()    : null)
                .operationUnitId(e.getOperationUnit()   != null ? e.getOperationUnit().getId()  : null)
                .operationUnitCode(e.getOperationUnit() != null ? e.getOperationUnit().getCode(): null)
                .brandId(e.getBrand()   != null ? e.getBrand().getId()       : null)
                .brandName(e.getBrand() != null ? e.getBrand().getBrandName() : null)
                .modelId(e.getModel()   != null ? e.getModel().getId()       : null)
                .modelName(e.getModel() != null ? e.getModel().getModelName() : null)
                .hsCodeId(e.getHsCodeId())
                .originId(e.getOriginId())
                .itemCode(e.getItemCode())
                .itemName(e.getItemName())
                .itemNameBn(e.getItemNameBn())
                .itemType(e.getItemType() != null ? e.getItemType().name() : "GENERAL")
                .unitOfMeasure(e.getUnitOfMeasure())
                .sku(e.getSku())
                .barcode(e.getBarcode())
                .costPrice(e.getCostPrice())
                .unitPrice(e.getUnitPrice())
                .taxRate(e.getTaxRate())
                .standardCost(e.getStandardCost())
                .minimumStock(e.getMinimumStock())
                .maximumStock(e.getMaximumStock())
                .reorderLevel(e.getReorderLevel())
                .yieldPercent(e.getYieldPercent())
                .processLossPct(e.getProcessLossPct())
                .weight(e.getWeight())
                .volume(e.getVolume())
                .dimensions(e.getDimensions())
                .shelfLifeDays(e.getShelfLifeDays())
                .expiryDate(e.getExpiryDate())
                .hasLotTracking(e.isHasLotTracking())
                .hasSerial(e.isHasSerial())
                .serialNumber(e.getSerialNumber())
                .manufacturer(e.getManufacturer())
                .warrantyMonths(e.getWarrantyMonths())
                .depreciationRate(e.getDepreciationRate())
                .casNumber(e.getCasNumber())
                .isHazardous(e.isHazardous())
                .safetyDataSheet(e.getSafetyDataSheet())
                .description(e.getDescription())
                .internalNotes(e.getInternalNotes())
                .active(e.isActive())
                .isApproved(e.isApproved())
                .approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private Item findEntityById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item #" + id + " not found."));
    }

    private ItemCategory resolveCategory(Long catId) {
        if (catId == null) throw new IllegalArgumentException("Category is required.");
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new IllegalArgumentException("Category #" + catId + " not found."));
    }

    private ItemUom resolveUom(Long uomId, String label) {
        if (uomId == null) throw new IllegalArgumentException(label + " is required.");
        return uomRepository.findById(uomId)
                .orElseThrow(() -> new IllegalArgumentException(label + " #" + uomId + " not found."));
    }

    private ItemType resolveItemType(String type) {
        if (type == null || type.isBlank()) return ItemType.GENERAL;
        try { return ItemType.valueOf(type.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid item type: '" + type + "'."); }
    }
}
