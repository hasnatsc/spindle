package com.asg.spindleserp.hrm.controller;

import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/hrm/dashboard")
@RequiredArgsConstructor
public class HrmDashboardController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public String page(Model m) {
        m.addAttribute("activePage", "hrm-dashboard");
        return "hrm/hrm-dashboard";
    }

    // ── Main KPIs ─────────────────────────────────────────────────────────────

    @GetMapping("/kpi")
    @ResponseBody
    public Map<String, Object> kpi() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String orgFilter = orgId != null ? " AND organization_id = " + orgId : "";
        String empFilter = orgId != null ? " AND e.organization_id = " + orgId : "";
        String today = LocalDate.now().toString();
        String currentMonth = YearMonth.now().toString(); // YYYY-MM

        Map<String, Object> m = new LinkedHashMap<>();

        // Employee counts
        m.put("totalActive",      q("SELECT COUNT(*) FROM hrm_employees WHERE status='ACTIVE'" + orgFilter));
        m.put("totalEmployees",   q("SELECT COUNT(*) FROM hrm_employees WHERE 1=1" + orgFilter));
        m.put("onLeave",          q("SELECT COUNT(*) FROM hrm_employees WHERE status='ON_LEAVE'" + orgFilter));
        m.put("suspended",        q("SELECT COUNT(*) FROM hrm_employees WHERE status='SUSPENDED'" + orgFilter));
        m.put("totalDepartments", orgId != null
                ? q("SELECT COUNT(DISTINCT department_id) FROM hrm_employees WHERE status='ACTIVE' AND organization_id=" + orgId)
                : q("SELECT COUNT(DISTINCT department_id) FROM hrm_employees WHERE status='ACTIVE'"));
        m.put("totalDesignations", orgId != null
                ? q("SELECT COUNT(DISTINCT designation_id) FROM hrm_employees WHERE status='ACTIVE' AND organization_id=" + orgId)
                : q("SELECT COUNT(DISTINCT designation_id) FROM hrm_employees WHERE status='ACTIVE'"));

        // Attendance today
        String attFilter = orgId != null ? " AND organization_id = " + orgId : "";
        m.put("presentToday",  q("SELECT COUNT(*) FROM hrm_attendances WHERE att_date='" + today + "' AND status='PRESENT'" + attFilter));
        m.put("absentToday",   q("SELECT COUNT(*) FROM hrm_attendances WHERE att_date='" + today + "' AND status='ABSENT'"  + attFilter));
        m.put("lateToday",     q("SELECT COUNT(*) FROM hrm_attendances WHERE att_date='" + today + "' AND status='LATE'"    + attFilter));
        m.put("leaveToday",    q("SELECT COUNT(*) FROM hrm_attendances WHERE att_date='" + today + "' AND status='LEAVE'"   + attFilter));

        // Leave applications
        String leaveFilter = orgId != null ? " AND organization_id = " + orgId : "";
        m.put("pendingLeaves",   q("SELECT COUNT(*) FROM hrm_employee_leaves WHERE status='PENDING'"  + leaveFilter));
        m.put("approvedLeaves",  q("SELECT COUNT(*) FROM hrm_employee_leaves WHERE status='APPROVED'" + leaveFilter));

        // Payroll current month
        String payFilter = orgId != null ? " AND organization_id = " + orgId : "";
        List<Map<String, Object>> payroll = jdbcTemplate.queryForList(
                "SELECT status, total_gross, total_net, total_deductions, employee_count FROM hrm_payroll_runs " +
                        "WHERE payroll_month='" + currentMonth + "'" + payFilter + " LIMIT 1");
        if (!payroll.isEmpty()) {
            m.put("payrollStatus",     payroll.get(0).get("status"));
            m.put("payrollGross",      payroll.get(0).get("total_gross"));
            m.put("payrollNet",        payroll.get(0).get("total_net"));
            m.put("payrollDeductions", payroll.get(0).get("total_deductions"));
            m.put("payrollEmpCount",   payroll.get(0).get("employee_count"));
        } else {
            m.put("payrollStatus", "NOT_CALCULATED");
            m.put("payrollGross", 0); m.put("payrollNet", 0);
            m.put("payrollDeductions", 0); m.put("payrollEmpCount", 0);
        }

        return m;
    }

    // ── Employee type breakdown ───────────────────────────────────────────────

    @GetMapping("/employee-types")
    @ResponseBody
    public List<Map<String, Object>> employeeTypes() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT employee_type, COUNT(*) AS cnt FROM hrm_employees " +
                        "WHERE status='ACTIVE'" + filter + " GROUP BY employee_type ORDER BY cnt DESC");
    }

    // ── Department headcount ─────────────────────────────────────────────────

    @GetMapping("/department-headcount")
    @ResponseBody
    public List<Map<String, Object>> departmentHeadcount() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND e.organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT d.name AS department_name, COUNT(e.id) AS cnt " +
                        "FROM hrm_employees e " +
                        "JOIN org_departments d ON d.id = e.department_id " +
                        "WHERE e.status = 'ACTIVE'" + filter +
                        " GROUP BY d.name ORDER BY cnt DESC LIMIT 10");
    }

    // ── Attendance trend (last 7 days) ────────────────────────────────────────

    @GetMapping("/attendance-trend")
    @ResponseBody
    public List<Map<String, Object>> attendanceTrend() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT TO_CHAR(att_date,'DD-Mon') AS att_day, " +
                        "SUM(CASE WHEN status='PRESENT' THEN 1 ELSE 0 END) AS present, " +
                        "SUM(CASE WHEN status='ABSENT'  THEN 1 ELSE 0 END) AS absent, " +
                        "SUM(CASE WHEN status='LATE'    THEN 1 ELSE 0 END) AS late, " +
                        "SUM(CASE WHEN status='LEAVE'   THEN 1 ELSE 0 END) AS on_leave " +
                        "FROM hrm_attendances " +
                        "WHERE att_date >= CURRENT_DATE - INTERVAL '6 days'" + filter +
                        " GROUP BY att_date ORDER BY att_date ASC");
    }

    // ── Monthly payroll trend (last 6 months) ────────────────────────────────

    @GetMapping("/payroll-trend")
    @ResponseBody
    public List<Map<String, Object>> payrollTrend() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT payroll_month, total_gross, total_net, total_deductions, employee_count, status " +
                        "FROM hrm_payroll_runs " +
                        "WHERE payroll_month >= TO_CHAR(CURRENT_DATE - INTERVAL '5 months','YYYY-MM')" + filter +
                        " ORDER BY payroll_month ASC");
    }

    // ── Leave type distribution ───────────────────────────────────────────────

    @GetMapping("/leave-distribution")
    @ResponseBody
    public List<Map<String, Object>> leaveDistribution() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT leave_type, COUNT(*) AS cnt, SUM(total_days) AS total_days " +
                        "FROM hrm_employee_leaves " +
                        "WHERE status='APPROVED' AND start_date >= DATE_TRUNC('year', CURRENT_DATE)" + filter +
                        " GROUP BY leave_type ORDER BY total_days DESC");
    }

    // ── Pending leaves list ───────────────────────────────────────────────────

    @GetMapping("/pending-leaves")
    @ResponseBody
    public List<Map<String, Object>> pendingLeaves() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND l.organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT e.employee_code, e.first_name || ' ' || e.last_name AS emp_name, " +
                        "l.leave_type, l.total_days, " +
                        "TO_CHAR(l.start_date,'DD-Mon-YYYY') AS start_date, " +
                        "TO_CHAR(l.end_date,'DD-Mon-YYYY') AS end_date, " +
                        "l.id " +
                        "FROM hrm_employee_leaves l " +
                        "JOIN hrm_employees e ON e.id = l.employee_id " +
                        "WHERE l.status = 'PENDING'" + filter +
                        " ORDER BY l.created_at ASC LIMIT 10");
    }

    // ── Gender distribution ───────────────────────────────────────────────────

    @GetMapping("/gender-distribution")
    @ResponseBody
    public List<Map<String, Object>> genderDistribution() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT gender, COUNT(*) AS cnt FROM hrm_employees " +
                        "WHERE status='ACTIVE'" + filter + " GROUP BY gender");
    }

    // ── Recent new joiners ────────────────────────────────────────────────────

    @GetMapping("/recent-joiners")
    @ResponseBody
    public List<Map<String, Object>> recentJoiners() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND e.organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT e.employee_code, e.first_name || ' ' || e.last_name AS emp_name, " +
                        "d.name AS dept_name, ds.designation_name, " +
                        "TO_CHAR(e.joining_date,'DD-Mon-YYYY') AS joining_date " +
                        "FROM hrm_employees e " +
                        "JOIN org_departments d ON d.id = e.department_id " +
                        "JOIN hrm_designations ds ON ds.id = e.designation_id " +
                        "WHERE e.joining_date >= CURRENT_DATE - INTERVAL '30 days'" + filter +
                        " ORDER BY e.joining_date DESC LIMIT 5");
    }

    // ── Gross salary range distribution ──────────────────────────────────────

    @GetMapping("/salary-distribution")
    @ResponseBody
    public List<Map<String, Object>> salaryDistribution() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String filter = orgId != null ? " AND e.organization_id = " + orgId : "";
        return jdbcTemplate.queryForList(
                "SELECT " +
                        "  CASE " +
                        "    WHEN s.gross_salary < 20000  THEN '< 20K' " +
                        "    WHEN s.gross_salary < 40000  THEN '20K–40K' " +
                        "    WHEN s.gross_salary < 60000  THEN '40K–60K' " +
                        "    WHEN s.gross_salary < 100000 THEN '60K–100K' " +
                        "    ELSE '> 100K' " +
                        "  END AS salary_range, " +
                        "  COUNT(*) AS cnt " +
                        "FROM hrm_employee_salaries s " +
                        "JOIN hrm_employees e ON e.id = s.employee_id " +
                        "WHERE s.is_current = true AND e.status = 'ACTIVE'" + filter +
                        " GROUP BY salary_range ORDER BY MIN(s.gross_salary)");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long q(String sql) {
        try {
            Long result = jdbcTemplate.queryForObject(sql, Long.class);
            return result != null ? result : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}