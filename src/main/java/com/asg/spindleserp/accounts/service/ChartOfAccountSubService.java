package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.ChartOfAccountSubDTO;
import com.asg.spindleserp.accounts.dto.SubAccountMetaDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;
import java.util.Map;

/**
 * ChartOfAccountSubService — one CRUD for all eleven sub-ledger partitions.
 *
 * <p>The sub-type comes from {@code dto.getSubAccountType()} on create and is
 * immutable afterwards: single-table inheritance stores it in the discriminator
 * column, which JPA will not rewrite on an existing row. Silently accepting a type
 * change would leave the row in its original partition with the new type's data
 * in it, so {@link #update} rejects the attempt instead.</p>
 */
public interface ChartOfAccountSubService {

    // ── CRUD ──────────────────────────────────────────────────────────────────

    ChartOfAccountSubDTO create(ChartOfAccountSubDTO dto);

    ChartOfAccountSubDTO update(Long id, ChartOfAccountSubDTO dto);

    ChartOfAccountSubDTO findById(Long id);

    List<ChartOfAccountSubDTO> findAll();

    List<ChartOfAccountSubDTO> findByType(String subAccountType);

    ChartOfAccountSubDTO toggleStatus(Long id);

    void delete(Long id);

    // ── Grid ──────────────────────────────────────────────────────────────────

    /**
     * @param sortKey the rendered column key (e.g. {@code sub_account_name}), not a
     *                column index — index positions shift between partitions because
     *                each declares its own type-specific columns.
     */
    DataTableResponse datatableList(String subAccountType, int draw, int start, int length,
                                    String search, String sortKey, String sortDir);

    // ── Pickers ───────────────────────────────────────────────────────────────

    /** {@code {items:[{id,text,code,name,subAccountType}], hasMore}} */
    Map<String, Object> search(String q, String subAccountType, int page, int pageSize);

    /** Bank master picker — {@code /banks} has no paged search endpoint of its own. */
    Map<String, Object> searchBanks(String q, int page, int pageSize);

    // ── Schema ────────────────────────────────────────────────────────────────

    /** Form + grid schema for one partition. Pass blank for the All tab. */
    SubAccountMetaDTO meta(String subAccountType);

    /** Tab strip: every partition with its label, icon and current row count. */
    List<Map<String, Object>> typeSummary();

    // ── Mapping ───────────────────────────────────────────────────────────────

    ChartOfAccountSubDTO toDTO(ChartOfAccountSub entity);
}
