package com.asg.spindleserp.hrm.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.hrm.dto.*;
import com.asg.spindleserp.hrm.service.HrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HrmController — all HRM module pages and REST endpoints.
 * <p>
 * Pages:
 * GET /hrm/designations    → hrm/hrm-designations.html
 * GET /hrm/employees       → hrm/hrm-employees.html
 * GET /hrm/attendance      → hrm/hrm-attendance.html
 * GET /hrm/leaves          → hrm/hrm-leaves.html
 * GET /hrm/payroll         → hrm/hrm-payroll.html
 * <p>
 * JS prefixes:
 * desig* | emp* | att* | leave* | pr*
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HrmController {

    private final HrmService hrmService;

    // ── Pages ──────────────────────────────────────────────────────────────────


    @GetMapping("/hrm")
    public String hrmHome(Model m) {
        return "redirect:/hrm/dashboard";
    }


    @GetMapping("/hrm/designations")
    public String designationsPage(Model m) {
        m.addAttribute("activePage", "hrm-designations");
        return "hrm/hrm-designations";
    }

    @GetMapping("/hrm/employees")
    public String employeesPage(Model m) {
        m.addAttribute("activePage", "hrm-employees");
        return "hrm/hrm-employees";
    }

    @GetMapping("/hrm/attendance")
    public String attendancePage(Model m) {
        m.addAttribute("activePage", "hrm-attendance");
        return "hrm/hrm-attendance";
    }

    @GetMapping("/hrm/leaves")
    public String leavesPage(Model m) {
        m.addAttribute("activePage", "hrm-leaves");
        return "hrm/hrm-leaves";
    }

    @GetMapping("/hrm/payroll")
    public String payrollPage(Model m) {
        m.addAttribute("activePage", "hrm-payroll");
        return "hrm/hrm-payroll";
    }

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @GetMapping("/hrm/dashboard/summary")
    @ResponseBody
    public Map<String, Object> summary() {
        return hrmService.dashboardSummary();
    }

    // ── Designations ───────────────────────────────────────────────────────────

    @GetMapping("/hrm/designations/list")
    @ResponseBody
    public DataTableResponse desigList(@RequestParam(defaultValue = "1") int draw,
                                       @RequestParam(defaultValue = "0") int start, @RequestParam(defaultValue = "25") int length,
                                       @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return hrmService.designationDatatable(draw, start, length, search);
    }

    @GetMapping("/hrm/designations/show/{id}")
    @ResponseBody
    public Map<String, Object> desigShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", hrmService.findDesignationById(id)));
    }

    @PostMapping("/hrm/designations/save")
    @ResponseBody
    public Map<String, Object> desigSave(@RequestBody @Valid DesignationDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) {
                hrmService.updateDesignation(dto.getId(), dto);
                return "Designation updated.";
            } else {
                hrmService.createDesignation(dto);
                return "Designation created.";
            }
        });
    }

    @PostMapping("/hrm/designations/toggle/{id}")
    @ResponseBody
    public Map<String, Object> desigToggle(@PathVariable Long id) {
        return ok(() -> {
            DesignationDTO d = hrmService.toggleDesignation(id);
            return "Designation " + (Boolean.TRUE.equals(d.getActive()) ? "activated" : "deactivated") + ".";
        });
    }

    @DeleteMapping("/hrm/designations/delete/{id}")
    @ResponseBody
    public Map<String, Object> desigDelete(@PathVariable Long id) {
        return ok(() -> {
            hrmService.deleteDesignation(id);
            return "Designation deleted.";
        });
    }

    @GetMapping("/hrm/designations/search")
    @ResponseBody
    public Map<String, Object> desigSearch(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "1") int page) {
        return hrmService.searchDesignations(search, page);
    }

    // ── Employees ──────────────────────────────────────────────────────────────

    @GetMapping("/hrm/employees/list")
    @ResponseBody
    public DataTableResponse empList(@RequestParam(defaultValue = "1") int draw,
                                     @RequestParam(defaultValue = "0") int start, @RequestParam(defaultValue = "25") int length,
                                     @RequestParam(value = "search[value]", defaultValue = "") String search,
                                     @RequestParam(defaultValue = "") String status,
                                     @RequestParam(required = false) Long deptId) {
        return hrmService.employeeDatatable(draw, start, length, search, status, deptId);
    }

    @GetMapping("/hrm/employees/show/{id}")
    @ResponseBody
    public Map<String, Object> empShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", hrmService.findEmployeeById(id)));
    }

    @PostMapping("/hrm/employees/save")
    @ResponseBody
    public Map<String, Object> empSave(@RequestBody @Valid EmployeeDTO dto) {
        return ok(() -> {
            EmployeeDTO saved;
            if (dto.getId() != null) {
                saved = hrmService.updateEmployee(dto.getId(), dto);
                return "Employee updated.";
            } else {
                saved = hrmService.createEmployee(dto);
                return "Employee " + saved.getEmployeeCode() + " registered.";
            }
        });
    }

    @DeleteMapping("/hrm/employees/delete/{id}")
    @ResponseBody
    public Map<String, Object> empDelete(@PathVariable Long id) {
        return ok(() -> {
            hrmService.deleteEmployee(id);
            return "Employee deleted.";
        });
    }

    @GetMapping("/hrm/employees/search")
    @ResponseBody
    public Map<String, Object> empSearch(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "1") int page) {
        return hrmService.searchEmployees(search, page);
    }

    @PostMapping("/hrm/employees/{id}/salary")
    @ResponseBody
    public Map<String, Object> empSalary(@PathVariable Long id, @RequestBody EmployeeDTO.SalaryDTO dto) {
        return ok(() -> {
            EmployeeDTO.SalaryDTO saved = hrmService.upsertSalary(id, dto);
            return "Salary updated.";
        });
    }

    @GetMapping("/hrm/employees/{id}/salary/history")
    @ResponseBody
    public List<EmployeeDTO.SalaryDTO> empSalaryHistory(@PathVariable Long id) {
        return hrmService.getSalaryHistory(id);
    }

    // ── Attendance ─────────────────────────────────────────────────────────────

    @GetMapping("/hrm/attendance/list")
    @ResponseBody
    public DataTableResponse attList(@RequestParam(defaultValue = "1") int draw,
                                     @RequestParam(defaultValue = "0") int start, @RequestParam(defaultValue = "25") int length,
                                     @RequestParam(value = "search[value]", defaultValue = "") String search,
                                     @RequestParam(defaultValue = "") String status,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return hrmService.attendanceDatatable(draw, start, length, search, status, dateFrom, dateTo);
    }

    @GetMapping("/hrm/attendance/show/{id}")
    @ResponseBody
    public Map<String, Object> attShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", hrmService.findAttendanceById(id)));
    }

    @PostMapping("/hrm/attendance/save")
    @ResponseBody
    public Map<String, Object> attSave(@RequestBody @Valid AttendanceDTO dto) {
        return ok(() -> {
            hrmService.saveAttendance(dto);
            return "Attendance saved.";
        });
    }

    @DeleteMapping("/hrm/attendance/delete/{id}")
    @ResponseBody
    public Map<String, Object> attDelete(@PathVariable Long id) {
        return ok(() -> {
            hrmService.deleteAttendance(id);
            return "Attendance deleted.";
        });
    }

    @GetMapping("/hrm/attendance/monthly-summary")
    @ResponseBody
    public List<Map<String, Object>> attMonthlySummary(@RequestParam Long employeeId, @RequestParam String yearMonth) {
        return hrmService.monthlySummary(employeeId, yearMonth);
    }

    // ── Leaves ─────────────────────────────────────────────────────────────────

    @GetMapping("/hrm/leaves/list")
    @ResponseBody
    public DataTableResponse leaveList(@RequestParam(defaultValue = "1") int draw,
                                       @RequestParam(defaultValue = "0") int start, @RequestParam(defaultValue = "25") int length,
                                       @RequestParam(value = "search[value]", defaultValue = "") String search,
                                       @RequestParam(defaultValue = "") String status) {
        return hrmService.leaveDatatable(draw, start, length, search, status);
    }

    @GetMapping("/hrm/leaves/show/{id}")
    @ResponseBody
    public Map<String, Object> leaveShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", hrmService.findLeaveById(id)));
    }

    @PostMapping("/hrm/leaves/save")
    @ResponseBody
    public Map<String, Object> leaveSave(@RequestBody @Valid EmployeeLeaveDTO dto) {
        return ok(() -> {
            hrmService.applyLeave(dto);
            return "Leave application submitted.";
        });
    }

    @PostMapping("/hrm/leaves/approve/{id}")
    @ResponseBody
    public Map<String, Object> leaveApprove(@PathVariable Long id) {
        return ok(() -> {
            hrmService.approveLeave(id);
            return "Leave approved. Attendance updated.";
        });
    }

    @PostMapping("/hrm/leaves/reject/{id}")
    @ResponseBody
    public Map<String, Object> leaveReject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ok(() -> {
            hrmService.rejectLeave(id, reason);
            return "Leave rejected.";
        });
    }

    @PostMapping("/hrm/leaves/cancel/{id}")
    @ResponseBody
    public Map<String, Object> leaveCancel(@PathVariable Long id) {
        return ok(() -> {
            hrmService.cancelLeave(id);
            return "Leave cancelled.";
        });
    }

    @DeleteMapping("/hrm/leaves/delete/{id}")
    @ResponseBody
    public Map<String, Object> leaveDelete(@PathVariable Long id) {
        return ok(() -> {
            hrmService.deleteLeave(id);
            return "Leave deleted.";
        });
    }

    // ── Payroll ────────────────────────────────────────────────────────────────

    @GetMapping("/hrm/payroll/list")
    @ResponseBody
    public DataTableResponse prList(@RequestParam(defaultValue = "1") int draw,
                                    @RequestParam(defaultValue = "0") int start, @RequestParam(defaultValue = "25") int length,
                                    @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return hrmService.payrollDatatable(draw, start, length, search);
    }

    @GetMapping("/hrm/payroll/show/{id}")
    @ResponseBody
    public Map<String, Object> prShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", hrmService.findPayrollRunById(id)));
    }
    @PostMapping("/hrm/payroll/calculate")
    @ResponseBody
    public Map<String, Object> prCalculate(
            @RequestParam String payrollMonth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate) {
        return ok(() -> {
            PayrollRunDTO dto = hrmService.calculatePayroll(payrollMonth, runDate);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Payroll calculated for " + dto.getEmployeeCount() + " employees. Total net: " + dto.getTotalNet());
            result.put("defaultData", dto);
            return result;
        });
    }

    @PostMapping("/hrm/payroll/approve/{id}")
    @ResponseBody
    public Map<String, Object> prApprove(@PathVariable Long id) {
        return ok(() -> {
            hrmService.approvePayrollRun(id);
            return "Payroll run approved.";
        });
    }

    @PostMapping("/hrm/payroll/pay/{id}")
    @ResponseBody
    public Map<String, Object> prPay(@PathVariable Long id) {
        return ok(() -> {
            hrmService.markPayrollPaid(id);
            return "Payroll marked as PAID.";
        });
    }

    @PostMapping("/hrm/payroll/cancel/{id}")
    @ResponseBody
    public Map<String, Object> prCancel(@PathVariable Long id) {
        return ok(() -> {
            hrmService.cancelPayrollRun(id);
            return "Payroll run cancelled.";
        });
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String, Object> ok(Checked a) {
        Map<String, Object> res = new HashMap<>();
        try {
            Object r = a.run();
            res.put("success", true);
            if (r instanceof String msg) res.put("message", msg);
            else if (r instanceof Map<?, ?> m) res.put("obj", m);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @FunctionalInterface
    interface Checked {
        Object run() throws Exception;
    }
}
