package com.asg.spindleserp.hrm.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.hrm.dto.*;
import com.asg.spindleserp.hrm.entity.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * HrmService — unified interface for the HRM module.
 *
 * Covers:
 *   Designation management
 *   Employee master (with address + salary sub-records)
 *   Attendance tracking
 *   Employee Leave management
 *   Payroll Run (calculate → approve → pay)
 */
public interface HrmService {

    // ── Designation ───────────────────────────────────────────────────────────

    DesignationDTO createDesignation(DesignationDTO dto);
    DesignationDTO updateDesignation(Long id, DesignationDTO dto);
    DesignationDTO findDesignationById(Long id);
    void           deleteDesignation(Long id);
    DesignationDTO toggleDesignation(Long id);
    DataTableResponse designationDatatable(int draw, int start, int length, String search);
    Map<String, Object> searchDesignations(String q, int page);
    DesignationDTO toDTO(Designation e);

    // ── Employee ──────────────────────────────────────────────────────────────

    EmployeeDTO createEmployee(EmployeeDTO dto);
    EmployeeDTO updateEmployee(Long id, EmployeeDTO dto);
    EmployeeDTO findEmployeeById(Long id);
    void        deleteEmployee(Long id);
    DataTableResponse employeeDatatable(int draw, int start, int length, String search, String status, Long deptId);
    Map<String, Object> searchEmployees(String q, int page);

    /** Update/create current salary record */
    EmployeeDTO.SalaryDTO upsertSalary(Long employeeId, EmployeeDTO.SalaryDTO dto);
    List<EmployeeDTO.SalaryDTO> getSalaryHistory(Long employeeId);

    EmployeeDTO toDTO(Employee e);

    // ── Attendance ────────────────────────────────────────────────────────────

    AttendanceDTO saveAttendance(AttendanceDTO dto);    // create or update (upsert)
    AttendanceDTO findAttendanceById(Long id);
    void          deleteAttendance(Long id);
    DataTableResponse attendanceDatatable(int draw, int start, int length, String search, String status, LocalDate dateFrom, LocalDate dateTo);
    /** Monthly summary for one employee */
    List<Map<String, Object>> monthlySummary(Long employeeId, String yearMonth);
    AttendanceDTO toDTO(Attendance e);

    // ── Employee Leave ────────────────────────────────────────────────────────

    EmployeeLeaveDTO applyLeave(EmployeeLeaveDTO dto);
    EmployeeLeaveDTO findLeaveById(Long id);
    EmployeeLeaveDTO approveLeave(Long id);
    EmployeeLeaveDTO rejectLeave(Long id, String reason);
    EmployeeLeaveDTO cancelLeave(Long id);
    void             deleteLeave(Long id);
    DataTableResponse leaveDatatable(int draw, int start, int length, String search, String status);
    EmployeeLeaveDTO toDTO(EmployeeLeave e);

    // ── Payroll Run ───────────────────────────────────────────────────────────

    /**
     * Calculate payroll for a month:
     *   - Pulls all ACTIVE employees' current salary records
     *   - Creates PayrollRun header + lines from salary data
     *   - Looks up attendance for working/absent/leave days
     *   - Saves as DRAFT; does NOT pay yet
     */
    PayrollRunDTO calculatePayroll(String payrollMonth, LocalDate runDate);

    PayrollRunDTO findPayrollRunById(Long id);
    PayrollRunDTO approvePayrollRun(Long id);
    PayrollRunDTO markPayrollPaid(Long id);
    PayrollRunDTO cancelPayrollRun(Long id);
    DataTableResponse payrollDatatable(int draw, int start, int length, String search);
    PayrollRunDTO toDTO(PayrollRun e);

    // ── Dashboard ─────────────────────────────────────────────────────────────

    Map<String, Object> dashboardSummary();
}
