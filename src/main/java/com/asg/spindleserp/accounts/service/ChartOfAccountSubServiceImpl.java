package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.ChartOfAccountSubDTO;
import com.asg.spindleserp.accounts.dto.SubAccountMetaDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub.SubAccountType;
import com.asg.spindleserp.accounts.meta.SubAccountField;
import com.asg.spindleserp.accounts.meta.SubAccountTypeMeta;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ChartOfAccountSubServiceImpl — one implementation, eleven partitions.
 *
 * <h3>What this replaces</h3>
 * <p>The previous version had a 70-line {@code switch} that copied fields type by
 * type. Beyond the maintenance cost, it had three defects that all produced
 * wrong data rather than errors:</p>
 * <ul>
 *   <li>MOBILE_BANKING, CARD and WALLET had no branch at all, so every field
 *       specific to them was discarded on save;</li>
 *   <li>{@code toDTO} never populated {@code subAccountType} (the line was
 *       commented out), which made the edit modal open with an empty type,
 *       hide all type sections, and then fail validation on save — editing any
 *       sub-account was impossible;</li>
 *   <li>{@code instantiate()} defaulted unknown types to {@code GeneralSubAccount},
 *       filing rows under the wrong discriminator.</li>
 * </ul>
 * <p>All three disappear when the field list is data rather than code:
 * {@link SubAccountField#applyToEntity} copies exactly what the type declares, and
 * {@link SubAccountTypeMeta#newInstance} is exhaustive over the enum.</p>
 *
 * <h3>Tenancy</h3>
 * <p>Every path resolves the organisation server-side from the session and loads by
 * {@code (id, orgId)}. The client's payload is never trusted for tenancy, and the
 * grid's SQL only ever interpolates values this class produced — the type filter
 * goes through the enum before it can reach a string.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChartOfAccountSubServiceImpl implements ChartOfAccountSubService {

    private final ChartOfAccountSubRepository subRepo;
    private final ChartOfAccountRepository coaRepo;
    private final BankRepository bankRepo;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Sort whitelist, keyed by the column key the grid actually renders.
     * <p>
     * Keying on the column <em>index</em> would break the moment a partition
     * declares a different number of type-specific columns — the All tab renders
     * one where a typed tab renders two, so index 3 is not the same column on both.
     */
    private static final Map<String, String> SORTABLE = Map.of(
            "sub_account_code", "s.sub_account_code",
            "sub_account_name", "s.sub_account_name",
            "sub_account_type", "s.sub_account_type",
            "main_account", "c.account_code",
            "current_balance", "s.current_balance",
            "created_at", "s.created_at");

    // ═════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public ChartOfAccountSubDTO create(ChartOfAccountSubDTO dto) {
        Long orgId = requireOrg();
        SubAccountType type = SubAccountTypeMeta.parseOrThrow(dto.getSubAccountType());

        ChartOfAccountSub entity = SubAccountTypeMeta.newInstance(type);
        String code = resolveCode(dto.getSubAccountCode(), orgId, type);

        if (subRepo.existsByOrganizationIdAndSubAccountCodeIgnoreCase(orgId, code)) {
            throw new IllegalArgumentException(
                    "Sub-account code '" + code + "' already exists in this organisation.");
        }
        entity.setSubAccountCode(code);

        populate(entity, dto, type, orgId);
        return toDTO(subRepo.save(entity));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public ChartOfAccountSubDTO update(Long id, ChartOfAccountSubDTO dto) {
        Long orgId = requireOrg();
        ChartOfAccountSub entity = findEntity(id, orgId);

        SubAccountType current = SubAccountTypeMeta.parseOrThrow(entity.getSubAccountTypeCode());
        SubAccountType requested = SubAccountTypeMeta.parseOrThrow(dto.getSubAccountType());

        // Single-table inheritance keeps the type in the discriminator column, and
        // JPA will not rewrite it on an existing row. Accepting the change here
        // would leave the row filed under its old partition holding the new
        // partition's data — worse than refusing.
        if (current != requested) {
            throw new IllegalArgumentException(
                    "Sub-account type cannot be changed from " + current.getLabel()
                            + " to " + requested.getLabel()
                            + ". Create a new sub-account under the correct type instead.");
        }

        String code = resolveCode(dto.getSubAccountCode(), orgId, current);
        if (subRepo.existsByOrganizationIdAndSubAccountCodeIgnoreCaseAndIdNot(orgId, code, id)) {
            throw new IllegalArgumentException(
                    "Sub-account code '" + code + "' already exists in this organisation.");
        }
        entity.setSubAccountCode(code);

        populate(entity, dto, current, orgId);
        return toDTO(subRepo.save(entity));
    }

    /**
     * The whole of the old {@code switch}, in four lines. Fields outside the type
     * are never touched, so a payload that smuggles {@code lcAmount} into a CASH
     * save cannot write it.
     */
    private void populate(ChartOfAccountSub entity, ChartOfAccountSubDTO dto,
                          SubAccountType type, Long orgId) {

        entity.setSubAccountName(trimOrNull(dto.getSubAccountName()));
        entity.setActive(!Boolean.FALSE.equals(dto.getActive()));

        SubAccountField.applyToEntity(type, dto, entity);

        resolveMainAccount(entity, dto, orgId);
        resolveBank(entity, dto, type, orgId);
        resolveSettlementAccount(entity, dto, type, orgId);
        applyTypeRules(entity, type);
        mirrorLegacyCodes(entity, type);
    }

    // ── FK resolution ─────────────────────────────────────────────────────────

    /**
     * The parent head is validated against the caller's organisation before the FK
     * is set. Without this check the payload could point a sub-ledger at another
     * tenant's GL account, and the resulting journal lines would post across the
     * tenancy boundary.
     */
    private void resolveMainAccount(ChartOfAccountSub entity, ChartOfAccountSubDTO dto, Long orgId) {
        if (dto.getMainAccountId() == null) {
            if (entity.getMainAccount() == null) {
                throw new IllegalArgumentException("Main account is required.");
            }
            return;
        }
        Integer hits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acc_chart_of_accounts WHERE id = ? AND organization_id = ?",
                Integer.class, dto.getMainAccountId(), orgId);
        if (hits == null || hits == 0) {
            throw new IllegalArgumentException("Selected main account is not available in this organisation.");
        }
        entity.setMainAccount(coaRepo.getReferenceById(dto.getMainAccountId()));
    }

    private void resolveBank(ChartOfAccountSub entity, ChartOfAccountSubDTO dto,
                             SubAccountType type, Long orgId) {
        if (type != SubAccountType.BANK) return;

        if (dto.getBankId() == null) {
            entity.setBank(null);
        } else {
            Integer hits = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM stp_banks WHERE id = ? AND organization_id = ?",
                    Integer.class, dto.getBankId(), orgId);
            if (hits == null || hits == 0) {
                throw new IllegalArgumentException("Selected bank is not available in this organisation.");
            }
            entity.setBank(bankRepo.getReferenceById(dto.getBankId()));
        }
        // Free-text bank name, kept separate from the Bank master FK.
        entity.setBankName(trimOrNull(dto.getBankNameManual()));
    }

    /**
     * A card terminal settles into a bank account, not into a supplier ledger.
     * Enforcing the partition here is what keeps the settlement sweep reconcilable.
     */
    private void resolveSettlementAccount(ChartOfAccountSub entity, ChartOfAccountSubDTO dto,
                                          SubAccountType type, Long orgId) {
        if (!SubAccountField.SETTLEMENT_ACCOUNT_ID.appliesTo(type)) return;
        Long target = dto.getSettlementAccountId();
        if (target == null) {
            entity.setSettlementAccountId(null);
            return;
        }
        Integer hits = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM acc_chart_of_accounts_sub
                        WHERE id = ? AND organization_id = ? AND sub_account_type = 'BANK'
                        """,
                Integer.class, target, orgId);
        if (hits == null || hits == 0) {
            throw new IllegalArgumentException(
                    "The settlement account must be an existing BANK sub-account in this organisation.");
        }
        entity.setSettlementAccountId(target);
    }

    // ── Per-type rules ────────────────────────────────────────────────────────

    private void applyTypeRules(ChartOfAccountSub entity, SubAccountType type) {
        switch (type) {
            case LC -> {
                if (isBlank(entity.getLcStatus())) entity.setLcStatus("OPEN");
                if (entity.getIssueDate() != null && entity.getExpiryDate() != null
                        && entity.getExpiryDate().isBefore(entity.getIssueDate())) {
                    throw new IllegalArgumentException("LC expiry date cannot be before the issue date.");
                }
            }
            case CASH -> {
                if (entity.getMaximumLimit() != null && entity.getMinimumLimit() != null
                        && entity.getMaximumLimit().compareTo(entity.getMinimumLimit()) < 0) {
                    throw new IllegalArgumentException("Maximum limit cannot be below the minimum limit.");
                }
            }
            case MOBILE_BANKING -> {
                if (isBlank(entity.getMfsProvider())) {
                    throw new IllegalArgumentException("MFS provider is required for a Mobile Banking account.");
                }
            }
            case CARD -> {
                if (isBlank(entity.getCardNetwork())) {
                    throw new IllegalArgumentException("Card network is required for a Card account.");
                }
            }
            default -> {
                // BANK, WALLET, CUSTOMER, SUPPLIER, EMPLOYEE, GENERAL, INTER_COMPANY
                // need nothing beyond the shared rules.
            }
        }
    }

    /**
     * {@code bank_account_code}, {@code cash_account_code}, {@code customer_code}
     * and {@code supplier_code} all predate {@code sub_account_code} and hold the
     * same value. Older reports still read them, so they are kept in step here
     * instead of being asked for a second time on the form — which is what the
     * duplicate-field complaint was about.
     */
    private void mirrorLegacyCodes(ChartOfAccountSub entity, SubAccountType type) {
        String code = entity.getSubAccountCode();
        switch (type) {
            case BANK -> entity.setBankAccountCode(code);
            case CASH -> entity.setCashAccountCode(code);
            case CUSTOMER -> entity.setCustomerCode(code);
            case SUPPLIER -> entity.setSupplierCode(code);
            default -> {
            }
        }
    }

    // ── Code generation ───────────────────────────────────────────────────────

    /**
     * Uses the typed code when given, otherwise mints {@code PREFIX-0001} scoped to
     * the organisation and the partition.
     * <p>
     * ★ SEAM — swap the body of {@link #nextSequentialCode} for
     * {@code documentSequenceService.nextDocumentNumber(...)} if you want these
     * codes to share the central sequence table. The local MAX() is safe under the
     * per-org unique index (a race loses the insert rather than duplicating a
     * code), but it does reuse numbers after a delete, which the central service
     * would not.
     */
    private String resolveCode(String supplied, Long orgId, SubAccountType type) {
        if (!isBlank(supplied)) return supplied.trim().toUpperCase();
        return nextSequentialCode(orgId, type);
    }

    private String nextSequentialCode(Long orgId, SubAccountType type) {
        String prefix = SubAccountTypeMeta.codePrefix(type) + "-";
        Integer max = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(NULLIF(regexp_replace(sub_account_code, '^.*-', ''), '')::int), 0)
                        FROM acc_chart_of_accounts_sub
                        WHERE organization_id = ?
                          AND sub_account_type = ?
                          AND sub_account_code ~ ('^' || ? || '[0-9]+$')
                        """,
                Integer.class, orgId, type.name(), prefix);
        return prefix + String.format("%04d", (max == null ? 0 : max) + 1);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // READ
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ChartOfAccountSubDTO findById(Long id) {
        return toDTO(findEntity(id, requireOrg()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountSubDTO> findAll() {
        return subRepo.findByOrganizationIdAndIsActiveTrue(requireOrg())
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountSubDTO> findByType(String subAccountType) {
        SubAccountType type = SubAccountTypeMeta.parseOrNull(subAccountType);
        Long orgId = requireOrg();
        List<ChartOfAccountSub> rows = (type == null)
                ? subRepo.findByOrganizationIdAndIsActiveTrue(orgId)
                : subRepo.findByOrganizationIdAndSubAccountType(orgId, type.name());
        return rows.stream().map(this::toDTO).toList();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TOGGLE / DELETE
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public ChartOfAccountSubDTO toggleStatus(Long id) {
        ChartOfAccountSub entity = findEntity(id, requireOrg());
        entity.setActive(!entity.isActive());
        return toDTO(subRepo.save(entity));
    }

    @Override
    public void delete(Long id) {
        ChartOfAccountSub entity = findEntity(id, requireOrg());

        long used = countReferences(id);
        if (used > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete '" + entity.getSubAccountName() + "' — " + used
                            + " posted record(s) reference this sub-account. Deactivate it instead.");
        }
        subRepo.delete(entity);
    }

    /**
     * Counts inbound references before deleting.
     * <p>
     * Without this the delete reaches PostgreSQL, the FK on
     * {@code acc_journal_entry_lines.sub_account_id} aborts it, and the user sees a
     * constraint name. Each table is counted separately and failures are swallowed
     * so the guard still works on an instance where the travel module has not been
     * migrated.
     */
    private long countReferences(Long id) {
        long total = 0;
        total += countIn("acc_journal_entry_lines", "sub_account_id", id);
        total += countIn("acc_payment_accounts", "sub_ledger_id", id);
        total += countIn("acc_chart_of_accounts_sub", "settlement_account_id", id);
        total += countIn("trv_bookings", "party_id", id);
        total += countIn("trv_booking_receipts", "sub_account_id", id);
        total += countIn("trv_payment_mode_accounts", "sub_account_id", id);
        return total;
    }

    private long countIn(String table, String column, Long id) {
        try {
            Long n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
            return n == null ? 0L : n;
        } catch (Exception e) {
            log.debug("Reference check skipped for {}.{}: {}", table, column, e.getMessage());
            return 0L;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DATATABLE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Server-side paged, filtered and sorted.
     * <p>
     * The page previously ran {@code serverSide: false} against an endpoint that
     * already applied {@code OFFSET/LIMIT}, so the browser received the first 25
     * rows and then paginated <em>those</em> — a customer list of 4,000 looked like
     * a list of 25, and the search box only searched what had already arrived.
     * <p>
     * The type filter is an enum name resolved before it reaches the string, and
     * the extra columns are constant SQL expressions from
     * {@link SubAccountTypeMeta}. The only caller-supplied value in the statement
     * is the search term, escaped by {@link CommonUtils#searchILike}.
     */
    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(String subAccountType, int draw, int start, int length,
                                           String search, String sortKey, String sortDir) {
        Long orgId = requireOrg();
        SubAccountType type = SubAccountTypeMeta.parseOrNull(subAccountType);

        StringBuilder extraSelect = new StringBuilder();
        for (SubAccountTypeMeta.Column col : SubAccountTypeMeta.extraColumns(type)) {
            extraSelect.append("                    ").append(col.sql())
                    .append(" AS ").append(col.key()).append(",\n");
        }

        String where = "WHERE s.organization_id = " + orgId
                + (type != null ? " AND s.sub_account_type = '" + type.name() + "'" : "")
                + CommonUtils.searchILike(search, Arrays.asList(
                "s.sub_account_code", "s.sub_account_name", "s.sub_account_type",
                "s.contact_person", "s.contact_phone", "s.account_number",
                "s.mfs_account_number", "s.lc_number",
                "c.account_code", "c.account_name"));

        String sortCol = resolveSort(sortKey, type);
        String direction = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        int safeLength = (length <= 0 || length > 500) ? 25 : length;
        int safeStart = Math.max(start, 0);

        String sql = String.format("""
                SELECT
                    ROW_NUMBER() OVER (ORDER BY %5$s %6$s, s.id ASC)  AS sl,
                    COUNT(*)    OVER ()                              AS full_count,
                    s.id,
                    s.sub_account_type AS row_type,
                %1$s    s.sub_account_code,
                    s.sub_account_name,
                    c.account_code || ' — ' || c.account_name        AS main_account,
                    COALESCE(s.currency, 'BDT')                      AS currency,
                    COALESCE(TO_CHAR(s.current_balance, 'FM999,999,999,990.00'), '—') AS current_balance,
                    TO_CHAR(s.created_at, 'DD-Mon-YYYY')             AS created_at,
                    CASE WHEN s.is_active
                        THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-danger">Inactive</span>'
                    END AS status,
                    '<div class="btn-group">'
                        || '<a href="javascript:;" onclick="subShow('   || s.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                        || '<a href="javascript:;" onclick="subEdit('   || s.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                        || '<a href="javascript:;" onclick="subToggle(' || s.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                        || '<a href="javascript:;" onclick="subDelete(' || s.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                        || '</div>'                                  AS actions
                FROM acc_chart_of_accounts_sub s
                JOIN acc_chart_of_accounts c ON c.id = s.main_account_id
                %2$s
                ORDER BY %5$s %6$s, s.id ASC
                OFFSET %3$d LIMIT %4$d
                """, extraSelect, where, safeStart, safeLength, sortCol, direction);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    /**
     * Maps a rendered column key to a SQL expression. Two layers of whitelist:
     * the fixed columns above, and the type's own extra columns — whose SQL is a
     * constant declared in {@link SubAccountTypeMeta}, never anything the caller
     * sent. An unrecognised key sorts by code rather than failing the request.
     */
    private String resolveSort(String sortKey, SubAccountType type) {
        if (sortKey == null || sortKey.isBlank()) return "s.sub_account_code";
        String fixed = SORTABLE.get(sortKey);
        if (fixed != null) return fixed;
        for (SubAccountTypeMeta.Column c : SubAccountTypeMeta.extraColumns(type)) {
            if (c.key().equals(sortKey)) return c.sql();
        }
        return "s.sub_account_code";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PICKERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Paged in the database.
     * <p>
     * The previous implementation loaded every active sub-account for the
     * organisation into a list, filtered it with a chain of {@code instanceof}
     * checks, and sliced the result — on every keystroke. On a customer master of
     * any size that is a full table read per character typed. It was also wrong:
     * MOBILE_BANKING, CARD and WALLET were all mapped to {@code instanceof
     * BankAccount}, so those pickers listed bank accounts.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> search(String q, String subAccountType, int page, int pageSize) {
        Long orgId = requireOrg();
        SubAccountType type = SubAccountTypeMeta.parseOrNull(subAccountType);

        int safePage = Math.max(page, 1);
        int safeSize = (pageSize <= 0 || pageSize > 100) ? 30 : pageSize;
        String query = q == null ? "" : q.trim();

        // One extra row tells us whether another page exists, without a COUNT.
        List<ChartOfAccountSub> rows = subRepo.searchForSelect(
                orgId, type == null ? "" : type.name(), query,
                PageRequest.of(safePage - 1, safeSize + 1));

        boolean hasMore = rows.size() > safeSize;
        if (hasMore) rows = rows.subList(0, safeSize);

        List<Map<String, Object>> items = rows.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("text", s.getSubAccountCode() + " — " + s.getSubAccountName());
            m.put("code", s.getSubAccountCode());
            m.put("name", s.getSubAccountName());
            m.put("phone", s.getContactPhone());
            m.put("subAccountType", s.getSubAccountTypeCode());
            return m;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", items);
        res.put("hasMore", hasMore);
        return res;
    }

    /**
     * Bank master picker.
     * <p>
     * The old page pointed its bank Select2 at {@code /banks/search}, which does not
     * exist — {@code BankController} exposes {@code /list}, {@code /active} and
     * {@code /lc-banks}. The dropdown was therefore permanently empty. Rather than
     * reshape the banks controller, the paged endpoint lives here next to its only
     * consumer.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchBanks(String q, int page, int pageSize) {
        Long orgId = requireOrg();
        int safePage = Math.max(page, 1);
        int safeSize = (pageSize <= 0 || pageSize > 100) ? 30 : pageSize;
        int offset = (safePage - 1) * safeSize;

        String where = "WHERE b.organization_id = " + orgId + " AND b.is_active = true"
                + CommonUtils.searchILike(q, Arrays.asList("b.bank_code", "b.bank_name", "b.short_name"));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format("""
                SELECT b.id,
                       b.bank_code,
                       b.bank_name,
                       COALESCE(b.swift_code, '') AS swift_code
                FROM stp_banks b
                %s
                ORDER BY b.bank_name ASC, b.id ASC
                OFFSET %d LIMIT %d
                """, where, offset, safeSize + 1));

        boolean hasMore = rows.size() > safeSize;
        if (hasMore) rows = rows.subList(0, safeSize);

        List<Map<String, Object>> items = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("text", r.get("bank_code") + " — " + r.get("bank_name"));
            m.put("swiftCode", r.get("swift_code"));
            return m;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", items);
        res.put("hasMore", hasMore);
        return res;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SCHEMA
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public SubAccountMetaDTO meta(String subAccountType) {
        SubAccountType type = SubAccountTypeMeta.parseOrNull(subAccountType);

        List<SubAccountMetaDTO.Group> groups = new ArrayList<>();
        if (type != null) {
            SubAccountField.groupedForType(type).forEach((group, fields) -> {
                List<SubAccountMetaDTO.Field> mapped = fields.stream()
                        .map(f -> SubAccountMetaDTO.Field.builder()
                                .key(f.key())
                                .label(f.label())
                                .input(f.input().name())
                                .width(f.width())
                                .maxLength(f.maxLength())
                                .options(f.options())
                                .required(f.required())
                                .build())
                        .toList();
                groups.add(SubAccountMetaDTO.Group.builder()
                        .title(group.title())
                        .icon(group.icon())
                        .fields(mapped)
                        .build());
            });
        }

        List<SubAccountMetaDTO.Column> columns = new ArrayList<>();
        columns.add(col("sl", "SL", "center", "1%", false));
        for (SubAccountTypeMeta.Column c : SubAccountTypeMeta.extraColumns(type)) {
            columns.add(col(c.key(), c.label(), c.align(), null, false));
        }
        columns.add(col("sub_account_code", "Code", "start", null, false));
        columns.add(col("sub_account_name", "Name", "start", null, false));
        columns.add(col("main_account", "Main Account", "start", null, false));
        columns.add(col("current_balance", "Balance", "end", null, false));
        columns.add(col("created_at", "Created", "center", null, false));
        columns.add(col("status", "Status", "center", null, true));
        columns.add(col("actions", "Actions", "center", "13%", true));

        return SubAccountMetaDTO.builder()
                .type(type == null ? "" : type.name())
                .label(SubAccountTypeMeta.label(type))
                .icon(SubAccountTypeMeta.icon(type))
                .mainAccountHint(SubAccountTypeMeta.expectedAccountHint(type))
                .creatable(type != null)
                .groups(groups)
                .columns(columns)
                .build();
    }

    private static SubAccountMetaDTO.Column col(String key, String label, String align,
                                                String width, boolean html) {
        return SubAccountMetaDTO.Column.builder()
                .key(key).label(label).align(align).width(width).html(html).build();
    }

    /** Tab strip with live counts, so an empty partition is obvious before it is opened. */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> typeSummary() {
        Long orgId = requireOrg();

        Map<String, Object> counts = new LinkedHashMap<>();
        jdbcTemplate.queryForList("""
                        SELECT sub_account_type, COUNT(*) AS n
                        FROM acc_chart_of_accounts_sub
                        WHERE organization_id = ?
                        GROUP BY sub_account_type
                        """, orgId)
                .forEach(r -> counts.put(String.valueOf(r.get("sub_account_type")), r.get("n")));

        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("type", "");
        all.put("label", "All");
        all.put("icon", SubAccountTypeMeta.icon(null));
        all.put("count", counts.values().stream()
                .mapToLong(v -> v instanceof Number n ? n.longValue() : 0L).sum());
        out.add(all);

        for (SubAccountType t : SubAccountType.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", t.name());
            m.put("label", t.getLabel());
            m.put("icon", SubAccountTypeMeta.icon(t));
            Object n = counts.get(t.name());
            m.put("count", n instanceof Number num ? num.longValue() : 0L);
            out.add(m);
        }
        return out;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAPPING
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Entity → DTO.
     * <p>
     * {@code subAccountType} is read through {@code getSubAccountTypeCode()}, which
     * prefers the persisted discriminator and falls back to the class annotation.
     * Reading it from {@code getClass()} alone returns the Hibernate proxy, which
     * carries no {@code @DiscriminatorValue} — that is why the field must not be
     * derived from the runtime class.
     */
    @Override
    public ChartOfAccountSubDTO toDTO(ChartOfAccountSub e) {
        ChartOfAccountSubDTO d = new ChartOfAccountSubDTO();

        d.setId(e.getId());
        d.setSubAccountCode(e.getSubAccountCode());
        d.setSubAccountName(e.getSubAccountName());
        d.setActive(e.isActive());
        d.setCurrentBalance(e.getCurrentBalance());

        String code = e.getSubAccountTypeCode();
        d.setSubAccountType(code);
        SubAccountType type = SubAccountTypeMeta.parseOrNull(code);
        d.setSubAccountTypeLabel(SubAccountTypeMeta.label(type));

        SubAccountField.applyToDto(e, d);

        if (e.getMainAccount() != null) {
            d.setMainAccountId(e.getMainAccount().getId());
            d.setMainAccountDisplay(e.getMainAccount().getAccountCode()
                    + " — " + e.getMainAccount().getAccountName());
        }
        if (e.getBank() != null) {
            d.setBankId(e.getBank().getId());
            d.setBankName(e.getBank().getBankName());
        }
        d.setBankNameManual(e.getBankName());

        d.setBankAccountCode(e.getBankAccountCode());
        d.setCashAccountCode(e.getCashAccountCode());
        d.setCustomerCode(e.getCustomerCode());
        d.setSupplierCode(e.getSupplierCode());

        if (e.getCardAcquirerBankId() != null) {
            d.setCardAcquirerBankName(lookupBankName(e.getCardAcquirerBankId()));
        }
        if (e.getSettlementAccountId() != null) {
            d.setSettlementAccountDisplay(lookupSubAccountDisplay(e.getSettlementAccountId()));
        }

        d.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        d.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedBy(e.getUpdatedBy());
        return d;
    }

    /** Stub FKs have no association to navigate, so the label is fetched directly. */
    private String lookupBankName(Long bankId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT bank_code || ' — ' || bank_name FROM stp_banks WHERE id = ?",
                    String.class, bankId);
        } catch (Exception e) {
            return null;
        }
    }

    private String lookupSubAccountDisplay(Long id) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT sub_account_code || ' — ' || sub_account_name
                            FROM acc_chart_of_accounts_sub WHERE id = ?
                            """,
                    String.class, id);
        } catch (Exception e) {
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private Long requireOrg() {
        return SecurityHelper.requireOrgId();
    }

    /**
     * Loads inside the tenant boundary. The "not found" message is identical
     * whether the row is missing or belongs to another organisation — telling the
     * caller which one it is would confirm that the id exists elsewhere.
     */
    private ChartOfAccountSub findEntity(Long id, Long orgId) {
        return subRepo.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-account #" + id + " not found."));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
