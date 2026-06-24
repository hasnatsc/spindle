package com.asg.spindleserp.dashboard;

import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * HrmModuleDashboardService
 *
 * Provides full data payload for GET /hrm/dashboard/summary.
 *
 * Response shape:
 * {
 *   hrm: {
 *     activeEmployees, inactiveEmployees, onLeaveToday, presentToday, absentToday,
 *     pendingLeaves, deptCount, designationCount,
 *     currentPayrollStatus, currentPayrollMonth,
 *     mtdPayrollNet, mtdPayrollGross
 *   },
 *   genderBreakdown:    [ {gender, count} ]
 *   employeeTypeBreakdown: [ {employee_type, count} ]
 *   deptHeadcount:      [ {dept_name, count} ] — top 10 departments
 *   designationHeadcount: [ {designation_name, count} ] — top 10
 *   pendingLeaveRequests: [ {employeeName, leaveType, startDate, endDate, totalDays} ]
 *   recentPayrollRuns:  [ last 6 payroll runs ]
 *   attendanceSummary:  [ {att_date, present, absent, leave, late} ] — last 7 days
 *   newJoinees:         [ employees who joined this month ]
 *   monthlyHeadcount:   [ {month, active_count} ] — 12 months (approx from joining date)
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrmModuleDashboardService {

    private final JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> summary() {
        Long   orgId    = SecurityHelper.requireOrgId();
        String today    = LocalDate.now().toString();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            _loadHrmKpis(result, orgId, today, mtdStart);
            _loadGenderBreakdown(result, orgId);
            _loadEmployeeTypeBreakdown(result, orgId);
            _loadDeptHeadcount(result, orgId);
            _loadDesignationHeadcount(result, orgId);
            _loadPendingLeaves(result, orgId);
            _loadRecentPayrollRuns(result, orgId);
            _loadAttendanceSummary(result, orgId, today);
            _loadNewJoinees(result, orgId, mtdStart);
        } catch (Exception e) {
            log.error("HrmDashboard summary error orgId={}", orgId, e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MASTER HRM KPIs
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadHrmKpis(Map<String, Object> result, Long orgId, String today, String mtdStart) {
        String sql = """
            SELECT
              COUNT(*) FILTER (WHERE status = 'ACTIVE')        AS active_employees,
              COUNT(*) FILTER (WHERE status != 'ACTIVE')       AS inactive_employees,
              (SELECT COUNT(*) FROM hrm_attendances a
               JOIN hrm_employees e2 ON e2.id = a.employee_id
               WHERE e2.organization_id = ?
                 AND a.att_date = ?::date AND a.status = 'PRESENT')  AS present_today,
              (SELECT COUNT(*) FROM hrm_attendances a
               JOIN hrm_employees e2 ON e2.id = a.employee_id
               WHERE e2.organization_id = ?
                 AND a.att_date = ?::date AND a.status = 'ABSENT')   AS absent_today,
              (SELECT COUNT(*) FROM hrm_employee_leaves l
               JOIN hrm_employees e2 ON e2.id = l.employee_id
               WHERE e2.organization_id = ?
                 AND l.status = 'APPROVED'
                 AND ?::date BETWEEN l.start_date AND l.end_date)    AS on_leave_today,
              (SELECT COUNT(*) FROM hrm_employee_leaves l
               JOIN hrm_employees e2 ON e2.id = l.employee_id
               WHERE e2.organization_id = ? AND l.status = 'PENDING') AS pending_leaves,
              COUNT(DISTINCT department_id) FILTER (WHERE status='ACTIVE') AS dept_count,
              COUNT(DISTINCT designation_id)                              AS designation_count,
              COUNT(*) FILTER (WHERE joining_date >= ?::date)            AS joined_mtd
            FROM hrm_employees
            WHERE organization_id = ?
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                orgId, today, orgId, today, orgId, today, orgId,
                mtdStart, orgId);

        // Payroll
        String payrollSql = """
            SELECT payroll_month, status,
                   COALESCE(total_net, 0)   AS total_net,
                   COALESCE(total_gross, 0) AS total_gross
            FROM hrm_payroll_runs
            WHERE organization_id = ?
            ORDER BY run_date DESC LIMIT 1
            """;
        List<Map<String, Object>> payrollRows = jdbc.queryForList(payrollSql, orgId);

        Map<String, Object> hrm = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            hrm.put("activeEmployees",   toLong(r, "active_employees"));
            hrm.put("inactiveEmployees", toLong(r, "inactive_employees"));
            hrm.put("presentToday",      toLong(r, "present_today"));
            hrm.put("absentToday",       toLong(r, "absent_today"));
            hrm.put("onLeaveToday",      toLong(r, "on_leave_today"));
            hrm.put("pendingLeaves",     toLong(r, "pending_leaves"));
            hrm.put("deptCount",         toLong(r, "dept_count"));
            hrm.put("designationCount",  toLong(r, "designation_count"));
            hrm.put("joinedMTD",         toLong(r, "joined_mtd"));
        }
        if (!payrollRows.isEmpty()) {
            Map<String, Object> r = payrollRows.get(0);
            hrm.put("currentPayrollStatus", Objects.toString(r.get("status"), "NONE"));
            hrm.put("currentPayrollMonth",  Objects.toString(r.get("payroll_month"), "—"));
            hrm.put("mtdPayrollNet",        toBD(r, "total_net"));
            hrm.put("mtdPayrollGross",      toBD(r, "total_gross"));
        } else {
            hrm.put("currentPayrollStatus", "NONE");
            hrm.put("currentPayrollMonth",  "—");
            hrm.put("mtdPayrollNet",        BigDecimal.ZERO);
            hrm.put("mtdPayrollGross",      BigDecimal.ZERO);
        }
        result.put("hrm", hrm);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. GENDER BREAKDOWN
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadGenderBreakdown(Map<String, Object> result, Long orgId) {
        result.put("genderBreakdown", jdbc.queryForList("""
            SELECT gender, COUNT(*) AS count
            FROM hrm_employees
            WHERE organization_id = ? AND status = 'ACTIVE'
            GROUP BY gender ORDER BY count DESC
            """, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. EMPLOYEE TYPE BREAKDOWN
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadEmployeeTypeBreakdown(Map<String, Object> result, Long orgId) {
        result.put("employeeTypeBreakdown", jdbc.queryForList("""
            SELECT employee_type, COUNT(*) AS count
            FROM hrm_employees
            WHERE organization_id = ? AND status = 'ACTIVE'
            GROUP BY employee_type ORDER BY count DESC
            """, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. DEPARTMENT HEADCOUNT — top 10
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadDeptHeadcount(Map<String, Object> result, Long orgId) {
        result.put("deptHeadcount", jdbc.queryForList("""
            SELECT d.name AS dept_name, COUNT(e.id) AS count
            FROM hrm_employees e
            JOIN org_departments d ON d.id = e.department_id
            WHERE e.organization_id = ? AND e.status = 'ACTIVE'
            GROUP BY d.id, d.name
            ORDER BY count DESC
            LIMIT 10
            """, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. DESIGNATION HEADCOUNT — top 10
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadDesignationHeadcount(Map<String, Object> result, Long orgId) {
        result.put("designationHeadcount", jdbc.queryForList("""
            SELECT d.designation_name, COUNT(e.id) AS count
            FROM hrm_employees e
            JOIN hrm_designations d ON d.id = e.designation_id
            WHERE e.organization_id = ? AND e.status = 'ACTIVE'
            GROUP BY d.id, d.designation_name
            ORDER BY count DESC
            LIMIT 10
            """, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. PENDING LEAVE REQUESTS
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadPendingLeaves(Map<String, Object> result, Long orgId) {
        result.put("pendingLeaveRequests", jdbc.queryForList("""
            SELECT (e.first_name || ' ' || e.last_name) AS employee_name,
                   e.employee_code,
                   l.leave_type,
                   TO_CHAR(l.start_date, 'DD-Mon-YYYY') AS start_date,
                   TO_CHAR(l.end_date,   'DD-Mon-YYYY') AS end_date,
                   l.total_days
            FROM hrm_employee_leaves l
            JOIN hrm_employees e ON e.id = l.employee_id
            WHERE e.organization_id = ? AND l.status = 'PENDING'
            ORDER BY l.start_date
            LIMIT 15
            """, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. RECENT PAYROLL RUNS
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadRecentPayrollRuns(Map<String, Object> result, Long orgId) {
        result.put("recentPayrollRuns", jdbc.queryForList("""
            SELECT payroll_month, status, employee_count,
                   COALESCE(total_gross, 0) AS total_gross,
                   COALESCE(total_deductions, 0) AS total_deductions,
                   COALESCE(total_net, 0) AS total_net,
                   TO_CHAR(run_date, 'DD-Mon-YYYY') AS run_date
            FROM hrm_payroll_runs
            WHERE organization_id = ?
            ORDER BY run_date DESC
            LIMIT 6
            """, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. ATTENDANCE SUMMARY — last 7 days
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadAttendanceSummary(Map<String, Object> result, Long orgId, String today) {
        result.put("attendanceSummary", jdbc.queryForList("""
            SELECT TO_CHAR(a.att_date, 'DD-Mon') AS att_date,
                   COUNT(*) FILTER (WHERE a.status = 'PRESENT') AS present,
                   COUNT(*) FILTER (WHERE a.status = 'ABSENT')  AS absent,
                   COUNT(*) FILTER (WHERE a.status = 'LEAVE')   AS on_leave,
                   COUNT(*) FILTER (WHERE a.status = 'LATE')    AS late
            FROM hrm_attendances a
            JOIN hrm_employees e ON e.id = a.employee_id
            WHERE e.organization_id = ?
              AND a.att_date >= (?::date - 6)
              AND a.att_date <= ?::date
            GROUP BY a.att_date
            ORDER BY a.att_date
            """, orgId, today, today));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. NEW JOINEES THIS MONTH
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadNewJoinees(Map<String, Object> result, Long orgId, String mtdStart) {
        result.put("newJoinees", jdbc.queryForList("""
            SELECT (e.first_name || ' ' || e.last_name) AS employee_name,
                   e.employee_code,
                   d.name AS department_name,
                   des.designation_name,
                   TO_CHAR(e.joining_date, 'DD-Mon-YYYY') AS joining_date,
                   e.employee_type
            FROM hrm_employees e
            JOIN org_departments d ON d.id = e.department_id
            JOIN hrm_designations des ON des.id = e.designation_id
            WHERE e.organization_id = ?
              AND e.joining_date >= ?::date
            ORDER BY e.joining_date DESC
            """, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    private Long toLong(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }
    private BigDecimal toBD(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
