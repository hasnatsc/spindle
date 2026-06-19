package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.AccountingPeriodDTO;
import com.asg.spindleserp.accounts.entity.AccountingPeriod;
import com.asg.spindleserp.accounts.repository.AccountingPeriodRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private final AccountingPeriodRepository periodRepo;
    private final JdbcTemplate               jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public AccountingPeriodDTO create(AccountingPeriodDTO dto) {
        if (dto.getEndDate() != null && dto.getStartDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        AccountingPeriod entity = AccountingPeriod.builder()
                .periodName(dto.getPeriodName().trim())
                .periodType(AccountingPeriod.PeriodType.valueOf(dto.getPeriodType()))
                .fiscalYear(dto.getFiscalYear())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .description(dto.getDescription())
                .isActive(Boolean.TRUE.equals(dto.getActive()))
                .isClosed(false)
                .build();
        return toDTO(periodRepo.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public AccountingPeriodDTO update(Long id, AccountingPeriodDTO dto) {
        AccountingPeriod entity = findEntityById(id);
        if (entity.isClosed()) throw new IllegalStateException("Closed periods cannot be edited.");
        if (dto.getEndDate() != null && dto.getStartDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        entity.setPeriodName(dto.getPeriodName().trim());
        entity.setPeriodType(AccountingPeriod.PeriodType.valueOf(dto.getPeriodType()));
        entity.setFiscalYear(dto.getFiscalYear());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setDescription(dto.getDescription());
        entity.setActive(Boolean.TRUE.equals(dto.getActive()));
        return toDTO(periodRepo.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AccountingPeriodDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingPeriodDTO> findAll() {
        return periodRepo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingPeriodDTO> findActiveByOrg(Long orgId) {
        return periodRepo.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public AccountingPeriodDTO toggleStatus(Long id) {
        AccountingPeriod entity = findEntityById(id);
        if (entity.isClosed()) throw new IllegalStateException("Closed period status cannot be toggled.");
        entity.setActive(!entity.isActive());
        return toDTO(periodRepo.save(entity));
    }

    // ── CLOSE ─────────────────────────────────────────────────────────────────

    @Override
    public AccountingPeriodDTO closePeriod(Long id) {
        AccountingPeriod entity = findEntityById(id);
        if (entity.isClosed()) throw new IllegalStateException("Period is already closed.");
        entity.setClosed(true);
        entity.setClosedBy(ContextProvider.getCurrentUsername());
        entity.setClosedDate(LocalDate.now());
        entity.setActive(false);
        return toDTO(periodRepo.save(entity));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        AccountingPeriod entity = findEntityById(id);
        if (entity.isClosed()) throw new IllegalStateException("Closed periods cannot be deleted.");
        periodRepo.delete(entity);
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        String where = "WHERE 1=1"
                + (orgId != null ? " AND p.organization_id = " + orgId : "")
                + CommonUtils.searchILike(search, Arrays.asList(
                        "p.period_name", "p.period_type", "p.fiscal_year::text"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY p.fiscal_year DESC, p.start_date DESC) AS sl,
                COUNT(*)     OVER ()                                                AS full_count,
                p.id,
                p.period_name,
                p.period_type,
                p.fiscal_year,
                TO_CHAR(p.start_date, 'DD-Mon-YYYY')  AS start_date,
                TO_CHAR(p.end_date,   'DD-Mon-YYYY')  AS end_date,
                CASE WHEN p.is_closed
                    THEN '<span class="badge bg-dark">Closed</span>'
                    WHEN p.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-secondary">Inactive</span>'
                END AS status,
                COALESCE(p.closed_by, '—')            AS closed_by,
                COALESCE(TO_CHAR(p.closed_date, 'DD-Mon-YYYY'), '—') AS closed_date,
                TO_CHAR(p.created_at, 'DD-Mon-YYYY')  AS created_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="periodShow('   || p.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="periodEdit('   || p.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || CASE WHEN NOT p.is_closed THEN
                        '<a href="javascript:;" onclick="periodClose(' || p.id || ')" class="btn btn-white btn-sm" title="Close Period"><i class="fas fa-lock text-danger"></i></a>'
                       ELSE '' END
                    || '<a href="javascript:;" onclick="periodToggle(' || p.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="periodDelete(' || p.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                        AS actions
            FROM acc_periods p
            %s
            ORDER BY p.fiscal_year DESC, p.start_date DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public AccountingPeriodDTO toDTO(AccountingPeriod e) {
        return AccountingPeriodDTO.builder()
                .id(e.getId())
                .periodName(e.getPeriodName())
                .periodType(e.getPeriodType() != null ? e.getPeriodType().name() : null)
                .fiscalYear(e.getFiscalYear())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .description(e.getDescription())
                .active(e.isActive())
                .closed(e.isClosed())
                .closedBy(e.getClosedBy())
                .closedDate(e.getClosedDate())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private AccountingPeriod findEntityById(Long id) {
        return periodRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Period #" + id + " not found."));
    }
}
