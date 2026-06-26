package com.asg.spindleserp.hrm.service;

import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.hrm.dto.*;
import com.asg.spindleserp.hrm.entity.*;
import com.asg.spindleserp.hrm.repository.*;
import com.asg.spindleserp.organization.repository.*;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class HrmServiceImpl implements HrmService {

    private final DesignationRepository      designRepo;
    private final EmployeeRepository         empRepo;
    private final EmployeeAddressRepository  addrRepo;
    private final EmployeeSalaryRepository   salaryRepo;
    private final AttendanceRepository       attRepo;
    private final EmployeeLeaveRepository    leaveRepo;
    private final PayrollRunRepository       runRepo;
    private final PayrollRunLineRepository   lineRepo;
    private final DepartmentRepository       deptRepo;
    private final OrganizationRepository     orgRepo;
    private final UserRepository             userRepo;
    private final PayrollJournalService payrollJournalService;
    private final PayrollAccountMappingRepository payrollMappingRepo;
    private final JdbcTemplate               jdbcTemplate;

    // =========================================================================
    // DESIGNATION
    // =========================================================================

    @Override
    public DesignationDTO createDesignation(DesignationDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (designRepo.existsByOrganizationIdAndDesignationCode(orgId, dto.getDesignationCode().trim().toUpperCase()))
            throw new IllegalArgumentException("Designation code '" + dto.getDesignationCode() + "' already exists.");
        Designation e = Designation.builder()
            .designationCode(dto.getDesignationCode().trim().toUpperCase())
            .designationName(dto.getDesignationName().trim())
            .grade(dto.getGrade()).description(dto.getDescription())
            .isActive(Boolean.TRUE.equals(dto.getActive()))
            .build();
        e.setOrganization(orgRepo.getReferenceById(orgId));
        audit(e, true);
        return toDTO(designRepo.save(e));
    }

    @Override
    public DesignationDTO updateDesignation(Long id, DesignationDTO dto) {
        Designation e = findDesig(id);
        e.setDesignationName(dto.getDesignationName().trim());
        e.setGrade(dto.getGrade()); e.setDescription(dto.getDescription());
        audit(e, false);
        return toDTO(designRepo.save(e));
    }

    @Override @Transactional(readOnly = true)
    public DesignationDTO findDesignationById(Long id) { return toDTO(findDesig(id)); }

    @Override
    public void deleteDesignation(Long id) { designRepo.delete(findDesig(id)); }

    @Override
    public DesignationDTO toggleDesignation(Long id) {
        Designation e = findDesig(id);
        e.setActive(!e.isActive());
        return toDTO(designRepo.save(e));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse designationDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND d.organization_id=" + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList("d.designation_code","d.designation_name","d.grade"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY d.designation_code) AS sl,
                   COUNT(*) OVER () AS full_count,
                   d.id, d.designation_code, d.designation_name,
                   COALESCE(d.grade,'—') AS grade,
                   COALESCE(d.description,'—') AS description,
                   (SELECT COUNT(*) FROM hrm_employees e WHERE e.designation_id=d.id AND e.status='ACTIVE') AS emp_count,
                   CASE WHEN d.is_active
                       THEN '<span class="badge bg-success">Active</span>'
                       ELSE '<span class="badge bg-secondary">Inactive</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="desigEdit('   || d.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="desigToggle(' || d.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                   || '<a href="javascript:;" onclick="desigDelete(' || d.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM hrm_designations d %s ORDER BY d.designation_code OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> searchDesignations(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page-1)*sz;
        String sql = "SELECT id, designation_code, designation_name FROM hrm_designations WHERE is_active=true"
            + (orgId != null ? " AND organization_id=" + orgId : "")
            + (q != null && !q.isBlank() ? " AND (designation_code ILIKE '%" + q.replace("'","''") + "%' OR designation_name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY designation_code LIMIT " + (sz+1) + " OFFSET " + off;
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String,Object>> items = rows.stream().limit(sz).map(r ->
            Map.of("id", r.get("id"), "text", r.get("designation_code") + " — " + r.get("designation_name"))).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override
    public DesignationDTO toDTO(Designation e) {
        return DesignationDTO.builder()
            .id(e.getId()).designationCode(e.getDesignationCode()).designationName(e.getDesignationName())
            .grade(e.getGrade()).description(e.getDescription()).active(e.isActive())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
    }

    // =========================================================================
    // EMPLOYEE
    // =========================================================================

    @Override
    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String code = generateEmpCode(orgId);
        Employee e = buildEmployee(dto, new Employee());
        e.setEmployeeCode(code);
        e.setOrganization(orgRepo.getReferenceById(orgId));
        audit(e, true);
        return toDTO(empRepo.save(e));
    }

    @Override
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
        Employee e = findEmp(id);
        buildEmployee(dto, e);
        audit(e, false);
        return toDTO(empRepo.save(e));
    }

    @Override @Transactional(readOnly = true)
    public EmployeeDTO findEmployeeById(Long id) { return toDTO(findEmp(id)); }

    @Override
    public void deleteEmployee(Long id) {
        Employee e = findEmp(id);
        if (e.getStatus() == Employee.EmployeeStatus.ACTIVE)
            throw new IllegalStateException("Active employees cannot be deleted. Terminate or resign first.");
        empRepo.delete(e);
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse employeeDatatable(int draw, int start, int length, String search, String status, Long deptId) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND e.organization_id=" + orgId : "")
            + (status != null && !status.isBlank() ? " AND e.status='" + status + "'" : "")
            + (deptId != null ? " AND e.department_id=" + deptId : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "e.employee_code","e.first_name","e.last_name","e.email","e.phone","d.designation_name"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY e.employee_code) AS sl,
                   COUNT(*) OVER () AS full_count,
                   e.id, e.employee_code,
                   e.first_name || ' ' || e.last_name AS full_name,
                   d.name AS department_name, ds.designation_name,
                   COALESCE(e.email,'—') AS email, e.phone,
                   e.employee_type, e.status, e.joining_date,
                   COALESCE(e.gross_salary::text,'—') AS gross_salary,
                   COALESCE(e.work_location,'—') AS work_location,
                   CASE e.status
                       WHEN 'ACTIVE'      THEN '<span class="badge bg-success">Active</span>'
                       WHEN 'INACTIVE'    THEN '<span class="badge bg-secondary">Inactive</span>'
                       WHEN 'ON_LEAVE'    THEN '<span class="badge bg-info text-dark">On Leave</span>'
                       WHEN 'SUSPENDED'   THEN '<span class="badge bg-warning text-dark">Suspended</span>'
                       WHEN 'TERMINATED'  THEN '<span class="badge bg-danger">Terminated</span>'
                       WHEN 'RESIGNED'    THEN '<span class="badge bg-dark">Resigned</span>'
                       WHEN 'RETIRED'     THEN '<span class="badge bg-purple">Retired</span>'
                       ELSE '<span class="badge bg-light text-dark">' || e.status || '</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="empShow('   || e.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="empEdit('   || e.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="empSalary(' || e.id || ')" class="btn btn-white btn-sm" title="Salary"><i class="fas fa-money-bill text-primary"></i></a>'
                   || CASE WHEN e.status = 'ACTIVE' THEN
                       '<a href="javascript:;" onclick="empLeave('  || e.id || ')" class="btn btn-white btn-sm" title="Apply Leave"><i class="fas fa-calendar-minus text-orange"></i></a>'
                      ELSE '' END
                   || '<a href="javascript:;" onclick="empDelete('  || e.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM hrm_employees e
            JOIN org_departments d  ON d.id = e.department_id
            JOIN hrm_designations ds ON ds.id = e.designation_id
            %s ORDER BY e.employee_code OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> searchEmployees(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page-1)*sz;
        String sql = "SELECT id, employee_code, first_name, last_name FROM hrm_employees WHERE status='ACTIVE'"
            + (orgId != null ? " AND organization_id=" + orgId : "")
            + (q != null && !q.isBlank() ? " AND (employee_code ILIKE '%" + q.replace("'","''") + "%' OR first_name ILIKE '%" + q.replace("'","''") + "%' OR last_name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY employee_code LIMIT " + (sz+1) + " OFFSET " + off;
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String,Object>> items = rows.stream().limit(sz).map(r ->
            Map.of("id", r.get("id"), "text", r.get("employee_code") + " — " + r.get("first_name") + " " + r.get("last_name"))).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override
    public EmployeeDTO.SalaryDTO upsertSalary(Long empId, EmployeeDTO.SalaryDTO dto) {
        Employee emp = findEmp(empId);
        // Expire current salary
        salaryRepo.findByEmployeeIdAndIsCurrentTrue(empId).ifPresent(s -> { s.setCurrent(false); salaryRepo.save(s); });
        BigDecimal gross = nvl(dto.getBasicSalary()).add(nvl(dto.getHouseRent())).add(nvl(dto.getMedicalAllowance()))
            .add(nvl(dto.getTransportAllowance())).add(nvl(dto.getOtherAllowances()));
        BigDecimal deduc = nvl(dto.getIncomeTax()).add(nvl(dto.getProvidentFund())).add(nvl(dto.getOtherDeductions()));
        BigDecimal net   = gross.subtract(deduc).max(BigDecimal.ZERO);
        EmployeeSalary sal = EmployeeSalary.builder()
            .employee(emp)
            .effectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDate.now())
            .basicSalary(nvl(dto.getBasicSalary()))
            .houseRent(nvl(dto.getHouseRent())).medicalAllowance(nvl(dto.getMedicalAllowance()))
            .transportAllowance(nvl(dto.getTransportAllowance())).otherAllowances(nvl(dto.getOtherAllowances()))
            .grossSalary(gross)
            .incomeTax(nvl(dto.getIncomeTax())).providentFund(nvl(dto.getProvidentFund())).otherDeductions(nvl(dto.getOtherDeductions()))
            .netSalary(net).isCurrent(true).remarks(dto.getRemarks())
            .createdBy(SecurityHelper.currentUsername().orElse("system"))
            .build();
        // Sync to employee snapshot
        emp.setBasicSalary(sal.getBasicSalary()); emp.setGrossSalary(gross);
        empRepo.save(emp);
        return toSalaryDTO(salaryRepo.save(sal));
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeDTO.SalaryDTO> getSalaryHistory(Long empId) {
        return salaryRepo.findByEmployeeIdOrderByEffectiveDateDesc(empId).stream().map(this::toSalaryDTO).toList();
    }

    @Override
    public EmployeeDTO toDTO(Employee e) {
        EmployeeDTO d = EmployeeDTO.builder()
            .id(e.getId()).employeeCode(e.getEmployeeCode())
            .firstName(e.getFirstName()).lastName(e.getLastName())
            .email(e.getEmail()).phone(e.getPhone())
            .gender(e.getGender() != null ? e.getGender().name() : "MALE")
            .dateOfBirth(e.getDateOfBirth()).bloodGroup(e.getBloodGroup())
            .maritalStatus(e.getMaritalStatus()).nationalId(e.getNationalId())
            .passportNumber(e.getPassportNumber())
            .employeeType(e.getEmployeeType() != null ? e.getEmployeeType().name() : "PERMANENT")
            .status(e.getStatus() != null ? e.getStatus().name() : "ACTIVE")
            .joiningDate(e.getJoiningDate()).confirmationDate(e.getConfirmationDate())
            .probationEndDate(e.getProbationEndDate()).resignationDate(e.getResignationDate()).exitDate(e.getExitDate())
            .basicSalary(e.getBasicSalary()).grossSalary(e.getGrossSalary())
            .bankName(e.getBankName()).bankAccountNumber(e.getBankAccountNumber()).bankBranch(e.getBankBranch())
            .workLocation(e.getWorkLocation()).workShift(e.getWorkShift())
            .annualLeaveDays(e.getAnnualLeaveDays()).sickLeaveDays(e.getSickLeaveDays()).casualLeaveDays(e.getCasualLeaveDays())
            .emergencyContactName(e.getEmergencyContactName()).emergencyContactPhone(e.getEmergencyContactPhone())
            .emergencyContactRelation(e.getEmergencyContactRelation())
            .profilePicture(e.getProfilePicture()).notes(e.getNotes())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getDepartment()        != null) { d.setDepartmentId(e.getDepartment().getId()); d.setDepartmentDisplay(e.getDepartment().getName()); }
        if (e.getDesignation()       != null) { d.setDesignationId(e.getDesignation().getId()); d.setDesignationDisplay(e.getDesignation().getDesignationCode()+" — "+e.getDesignation().getDesignationName()); }
        if (e.getReportingManager()  != null) { d.setReportingManagerId(e.getReportingManager().getId()); d.setReportingManagerDisplay(e.getReportingManager().getFullName()); }
        if (e.getUser()              != null) { d.setUserId(e.getUser().getId()); d.setUserDisplay(e.getUser().getFullName() != null ? e.getUser().getFullName() : e.getUser().getUsername()); }
        // Addresses
        d.setAddresses(addrRepo.findByEmployeeId(e.getId()).stream().map(a -> EmployeeDTO.AddressDTO.builder()
            .id(a.getId()).addressType(a.getAddressType().name()).addressLine1(a.getAddressLine1())
            .addressLine2(a.getAddressLine2()).city(a.getCity()).district(a.getDistrict())
            .country(a.getCountry()).postalCode(a.getPostalCode()).isDefault(a.isDefault()).build()).toList());
        // Current salary
        salaryRepo.findByEmployeeIdAndIsCurrentTrue(e.getId()).ifPresent(s -> d.setCurrentSalary(toSalaryDTO(s)));
        return d;
    }

    // =========================================================================
    // ATTENDANCE
    // =========================================================================

    @Override
    public AttendanceDTO saveAttendance(AttendanceDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        Attendance att = attRepo.findByEmployeeIdAndAttDate(dto.getEmployeeId(), dto.getAttDate())
            .orElse(Attendance.builder().organizationId(orgId)
                .employee(empRepo.getReferenceById(dto.getEmployeeId())).build());
        att.setAttDate(dto.getAttDate());
        att.setCheckIn(dto.getCheckIn());
        att.setCheckOut(dto.getCheckOut());
        att.setWorkingHours(dto.getWorkingHours());
        att.setStatus(Attendance.AttendanceStatus.valueOf(dto.getStatus() != null ? dto.getStatus() : "PRESENT"));
        att.setSource(dto.getSource() != null ? dto.getSource() : "MANUAL");
        att.setRemarks(dto.getRemarks());
        att.setCreatedBy(SecurityHelper.currentUsername().orElse("system"));
        return toDTO(attRepo.save(att));
    }

    @Override @Transactional(readOnly = true)
    public AttendanceDTO findAttendanceById(Long id) { return toDTO(attRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Attendance #"+id+" not found."))); }

    @Override
    public void deleteAttendance(Long id) { attRepo.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public DataTableResponse attendanceDatatable(int draw, int start, int length, String search, String status, LocalDate dateFrom, LocalDate dateTo) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND a.organization_id=" + orgId : "")
            + (status != null && !status.isBlank() ? " AND a.status='" + status + "'" : "")
            + (dateFrom != null ? " AND a.att_date >= '" + dateFrom + "'" : "")
            + (dateTo   != null ? " AND a.att_date <= '" + dateTo   + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList("e.employee_code","e.first_name","e.last_name"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY a.att_date DESC, e.employee_code) AS sl,
                   COUNT(*) OVER () AS full_count,
                   a.id, e.employee_code,
                   e.first_name || ' ' || e.last_name AS full_name,
                   TO_CHAR(a.att_date,'DD-Mon-YYYY')  AS att_date,
                   COALESCE(a.check_in::text,'—')     AS check_in,
                   COALESCE(a.check_out::text,'—')    AS check_out,
                   COALESCE(a.working_hours::text,'—')AS working_hours,
                   a.status, a.source,
                   COALESCE(a.remarks,'—')            AS remarks,
                   CASE a.status
                       WHEN 'PRESENT'  THEN '<span class="badge bg-success">Present</span>'
                       WHEN 'ABSENT'   THEN '<span class="badge bg-danger">Absent</span>'
                       WHEN 'LATE'     THEN '<span class="badge bg-warning text-dark">Late</span>'
                       WHEN 'HALF_DAY' THEN '<span class="badge bg-info text-dark">Half Day</span>'
                       WHEN 'HOLIDAY'  THEN '<span class="badge bg-teal">Holiday</span>'
                       WHEN 'LEAVE'    THEN '<span class="badge bg-primary">Leave</span>'
                       WHEN 'WEEKEND'  THEN '<span class="badge bg-secondary">Weekend</span>'
                       ELSE '<span class="badge bg-light text-dark">' || a.status || '</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="attEdit('   || a.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="attDelete(' || a.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM hrm_attendances a
            JOIN hrm_employees e ON e.id = a.employee_id
            %s ORDER BY a.att_date DESC, e.employee_code OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public List<Map<String, Object>> monthlySummary(Long employeeId, String yearMonth) {
        LocalDate from = LocalDate.parse(yearMonth + "-01");
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        String sql = """
            SELECT TO_CHAR(a.att_date,'DD-Mon-YYYY') AS att_date, a.status,
                   COALESCE(a.check_in::text,'—') AS check_in,
                   COALESCE(a.check_out::text,'—') AS check_out,
                   COALESCE(a.working_hours::text,'—') AS working_hours
            FROM hrm_attendances a
            WHERE a.employee_id=? AND a.att_date BETWEEN ? AND ?
            ORDER BY a.att_date
            """;
        return jdbcTemplate.queryForList(sql, employeeId, from, to);
    }

    @Override
    public AttendanceDTO toDTO(Attendance e) {
        return AttendanceDTO.builder()
            .id(e.getId()).employeeId(e.getEmployee().getId())
            .employeeDisplay(e.getEmployee().getFullName())
            .attDate(e.getAttDate()).checkIn(e.getCheckIn()).checkOut(e.getCheckOut())
            .workingHours(e.getWorkingHours())
            .status(e.getStatus() != null ? e.getStatus().name() : "ABSENT")
            .source(e.getSource()).remarks(e.getRemarks())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .createdBy(e.getCreatedBy())
            .build();
    }

    // =========================================================================
    // LEAVE
    // =========================================================================

    @Override
    public EmployeeLeaveDTO applyLeave(EmployeeLeaveDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        // Calculate total days
        long days = dto.getStartDate().datesUntil(dto.getEndDate().plusDays(1)).count();
        EmployeeLeave leave = EmployeeLeave.builder()
            .employee(empRepo.getReferenceById(dto.getEmployeeId()))
            .leaveType(dto.getLeaveType())
            .startDate(dto.getStartDate()).endDate(dto.getEndDate())
            .totalDays(new BigDecimal(days))
            .reason(dto.getReason())
            .status(EmployeeLeave.LeaveStatus.PENDING)
            .build();
        leave.setOrganization(orgRepo.getReferenceById(orgId));
        audit(leave, true);
        return toDTO(leaveRepo.save(leave));
    }

    @Override @Transactional(readOnly = true)
    public EmployeeLeaveDTO findLeaveById(Long id) { return toDTO(findLeave(id)); }

    @Override
    public EmployeeLeaveDTO approveLeave(Long id) {
        EmployeeLeave leave = findLeave(id);
        if (leave.getStatus() != EmployeeLeave.LeaveStatus.PENDING)
            throw new IllegalStateException("Only PENDING leaves can be approved.");
        leave.setStatus(EmployeeLeave.LeaveStatus.APPROVED);
        leave.setApprovedBy(ContextProvider.getCurrentUsername());
        leave.setApprovedAt(LocalDateTime.now());
        // Mark attendance as LEAVE for each day
        Employee emp = leave.getEmployee();
        leave.getStartDate().datesUntil(leave.getEndDate().plusDays(1)).forEach(d ->
            attRepo.findByEmployeeIdAndAttDate(emp.getId(), d).ifPresentOrElse(a -> {
                a.setStatus(Attendance.AttendanceStatus.LEAVE); attRepo.save(a);
            }, () -> attRepo.save(Attendance.builder()
                .organizationId(emp.getOrganization().getId())
                .employee(emp).attDate(d)
                .status(Attendance.AttendanceStatus.LEAVE).source("SYSTEM").build()))
        );
        return toDTO(leaveRepo.save(leave));
    }

    @Override
    public EmployeeLeaveDTO rejectLeave(Long id, String reason) {
        EmployeeLeave leave = findLeave(id);
        leave.setStatus(EmployeeLeave.LeaveStatus.REJECTED);
        leave.setReason((leave.getReason() != null ? leave.getReason() + "\n" : "") + "[REJECTED] " + reason);
        return toDTO(leaveRepo.save(leave));
    }

    @Override
    public EmployeeLeaveDTO cancelLeave(Long id) {
        EmployeeLeave leave = findLeave(id);
        leave.setStatus(EmployeeLeave.LeaveStatus.CANCELLED);
        return toDTO(leaveRepo.save(leave));
    }

    @Override
    public void deleteLeave(Long id) {
        EmployeeLeave leave = findLeave(id);
        if (leave.getStatus() != EmployeeLeave.LeaveStatus.PENDING)
            throw new IllegalStateException("Only PENDING leaves can be deleted.");
        leaveRepo.delete(leave);
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse leaveDatatable(int draw, int start, int length, String search, String status) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND l.organization_id=" + orgId : "")
            + (status != null && !status.isBlank() ? " AND l.status='" + status + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList("e.employee_code","e.first_name","e.last_name","l.leave_type"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY l.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   l.id, e.employee_code,
                   e.first_name || ' ' || e.last_name AS full_name,
                   l.leave_type, l.total_days,
                   TO_CHAR(l.start_date,'DD-Mon-YYYY') AS start_date,
                   TO_CHAR(l.end_date,  'DD-Mon-YYYY') AS end_date,
                   l.status, COALESCE(l.reason,'—') AS reason,
                   COALESCE(l.approved_by,'—')        AS approved_by,
                   CASE l.status
                       WHEN 'PENDING'   THEN '<span class="badge bg-warning text-dark">Pending</span>'
                       WHEN 'APPROVED'  THEN '<span class="badge bg-success">Approved</span>'
                       WHEN 'REJECTED'  THEN '<span class="badge bg-danger">Rejected</span>'
                       WHEN 'CANCELLED' THEN '<span class="badge bg-secondary">Cancelled</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="leaveShow('   || l.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || CASE WHEN l.status = 'PENDING' THEN
                       '<a href="javascript:;" onclick="leaveApprove('|| l.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check text-success"></i></a>'
                       || '<a href="javascript:;" onclick="leaveReject('|| l.id || ')" class="btn btn-white btn-sm" title="Reject"><i class="fas fa-times text-danger"></i></a>'
                       || '<a href="javascript:;" onclick="leaveDelete('|| l.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                      ELSE '' END
                   || '</div>' AS actions
            FROM hrm_employee_leaves l
            JOIN hrm_employees e ON e.id = l.employee_id
            %s ORDER BY l.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EmployeeLeaveDTO toDTO(EmployeeLeave e) {
        EmployeeLeaveDTO d = EmployeeLeaveDTO.builder()
            .id(e.getId()).employeeId(e.getEmployee().getId())
            .employeeDisplay(e.getEmployee().getFullName())
            .leaveType(e.getLeaveType()).startDate(e.getStartDate()).endDate(e.getEndDate())
            .totalDays(e.getTotalDays()).reason(e.getReason())
            .status(e.getStatus() != null ? e.getStatus().name() : "PENDING")
            .approvedBy(e.getApprovedBy())
            .approvedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null)
            .approvalRequestId(e.getApprovalRequest() != null ? e.getApprovalRequest().getId() : null)
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        return d;
    }

    // =========================================================================
    // PAYROLL RUN
    // =========================================================================

    @Override
    public PayrollRunDTO calculatePayroll(String payrollMonth, LocalDate runDate) {
        Long orgId = ContextProvider.getOrganizationId();
        if (runRepo.findByOrganizationIdAndPayrollMonth(orgId, payrollMonth).isPresent())
            throw new IllegalStateException("Payroll already exists for " + payrollMonth);
        // Month boundaries for attendance lookup
        LocalDate from = LocalDate.parse(payrollMonth + "-01");
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        int calendarDays = (int) from.datesUntil(to.plusDays(1)).count();

        PayrollRun run = PayrollRun.builder()
            .payrollMonth(payrollMonth).runDate(runDate).status(PayrollRun.PayrollStatus.DRAFT).build();
        run.setOrganization(orgRepo.getReferenceById(orgId));
        audit(run, true);
        PayrollRun saved = runRepo.save(run);

        List<Employee> employees = empRepo.findByOrganizationIdAndStatus(orgId, Employee.EmployeeStatus.ACTIVE);
        BigDecimal totalGross=BigDecimal.ZERO, totalDeduc=BigDecimal.ZERO, totalNet=BigDecimal.ZERO;

        for (Employee emp : employees) {
            EmployeeSalary sal = salaryRepo.findByEmployeeIdAndIsCurrentTrue(emp.getId()).orElse(null);
            if (sal == null) continue;

            long presentDays = attRepo.countByEmployeeIdAndAttDateBetweenAndStatus(emp.getId(), from, to, Attendance.AttendanceStatus.PRESENT)
                + attRepo.countByEmployeeIdAndAttDateBetweenAndStatus(emp.getId(), from, to, Attendance.AttendanceStatus.LATE);
            long leaveDays   = attRepo.countByEmployeeIdAndAttDateBetweenAndStatus(emp.getId(), from, to, Attendance.AttendanceStatus.LEAVE);
            long absentDays  = attRepo.countByEmployeeIdAndAttDateBetweenAndStatus(emp.getId(), from, to, Attendance.AttendanceStatus.ABSENT);

            // Pro-rate if absent days exist (simple per-day deduction on net)
            BigDecimal gross  = nvl(sal.getGrossSalary());
            BigDecimal deduc  = nvl(sal.getIncomeTax()).add(nvl(sal.getProvidentFund())).add(nvl(sal.getOtherDeductions()));
            BigDecimal net    = gross.subtract(deduc).max(BigDecimal.ZERO);

            if (absentDays > 0 && calendarDays > 0) {
                BigDecimal perDay = net.divide(BigDecimal.valueOf(calendarDays), 4, RoundingMode.HALF_UP);
                net = net.subtract(perDay.multiply(BigDecimal.valueOf(absentDays))).max(BigDecimal.ZERO);
            }

            PayrollRunLine line = PayrollRunLine.builder()
                .payrollRun(saved).employee(emp)
                .basicSalary(nvl(sal.getBasicSalary())).houseRent(nvl(sal.getHouseRent()))
                .medicalAllowance(nvl(sal.getMedicalAllowance())).transportAllowance(nvl(sal.getTransportAllowance()))
                .otherAllowances(nvl(sal.getOtherAllowances())).grossSalary(gross)
                .incomeTax(nvl(sal.getIncomeTax())).providentFund(nvl(sal.getProvidentFund())).otherDeductions(nvl(sal.getOtherDeductions()))
                .netSalary(net.setScale(2, RoundingMode.HALF_UP))
                .workingDays((int) presentDays).leaveDays((int) leaveDays).absentDays((int) absentDays)
                .paymentStatus(PayrollRunLine.PaymentStatus.PENDING)
                .build();
            lineRepo.save(line);
            totalGross = totalGross.add(gross);
            totalDeduc = totalDeduc.add(deduc);
            totalNet   = totalNet.add(net);
        }
        saved.setTotalGross(totalGross.setScale(2, RoundingMode.HALF_UP));
        saved.setTotalDeductions(totalDeduc.setScale(2, RoundingMode.HALF_UP));
        saved.setTotalNet(totalNet.setScale(2, RoundingMode.HALF_UP));
        saved.setEmployeeCount(employees.size());
        saved.setStatus(PayrollRun.PayrollStatus.COMPLETED);
        return toDTO(runRepo.save(saved));
    }

    @Override @Transactional(readOnly = true)
    public PayrollRunDTO findPayrollRunById(Long id) { return toDTO(findRun(id)); }


// ─────────────────────────────────────────────────────────────────────────────
// Replace the existing approvePayrollRun method:
// ─────────────────────────────────────────────────────────────────────────────

    @Override
    public PayrollRunDTO approvePayrollRun(Long id) {
        PayrollRun run = findRun(id);
        if (run.getStatus() != PayrollRun.PayrollStatus.COMPLETED)
            throw new IllegalStateException("Only COMPLETED payroll runs can be approved.");

        // 1. Flip payroll status
        run.setStatus(PayrollRun.PayrollStatus.APPROVED);
        run.setApprovedBy(ContextProvider.getCurrentUsername());
        run.setApprovedAt(LocalDateTime.now());
        audit(run, false);
        PayrollRun saved = runRepo.save(run);

        // 2. Auto-generate GL journal entry
        try {
            JournalEntryMaster journal = payrollJournalService.generatePayrollJournal(saved);
            saved.setJournalEntry(journal);
            saved = runRepo.save(saved);
            log.info("Payroll run #{} approved. Journal entry {} created.", id, journal.getVoucherNo());
        } catch (Exception ex) {
            log.error("Payroll approved but journal entry failed for run #{}: {}", id, ex.getMessage());
            // Rethrow so the transaction rolls back — payroll stays COMPLETED, not APPROVED
            throw new IllegalStateException("Payroll approved but GL posting failed: " + ex.getMessage(), ex);
        }

        return toDTO(saved);
    }



// ─────────────────────────────────────────────────────────────────────────────
// Replace the existing markPayrollPaid method:
// ─────────────────────────────────────────────────────────────────────────────

    @Override
    public PayrollRunDTO markPayrollPaid(Long id) {
        PayrollRun run = findRun(id);
        if (run.getStatus() != PayrollRun.PayrollStatus.APPROVED)
            throw new IllegalStateException("Only APPROVED payroll runs can be marked as PAID.");

        run.setStatus(PayrollRun.PayrollStatus.PAID);

        // Mark all lines as PAID
        run.getLines().forEach(l -> {
            l.setPaymentStatus(PayrollRunLine.PaymentStatus.PAID);
            lineRepo.save(l);
        });

        audit(run, false);
        PayrollRun saved = runRepo.save(run);

        // Generate PAYMENT_VOUCHER: Dr Salary Payable → Cr Bank/Cash
        // (Optional: trigger payment journal if bank account is configured)
        // payrollJournalService.generatePaymentJournal(saved);

        log.info("Payroll run #{} marked as PAID.", id);
        return toDTO(saved);
    }

    @Override
    public PayrollRunDTO cancelPayrollRun(Long id) {
        PayrollRun run = findRun(id);
        if (run.getStatus() == PayrollRun.PayrollStatus.PAID)
            throw new IllegalStateException("PAID payroll runs cannot be cancelled.");
        run.setStatus(PayrollRun.PayrollStatus.CANCELLED);
        audit(run, false);
        return toDTO(runRepo.save(run));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse payrollDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND r.organization_id=" + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList("r.payroll_month","r.status"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY r.payroll_month DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   r.id, r.payroll_month,
                   TO_CHAR(r.run_date,'DD-Mon-YYYY') AS run_date,
                   r.status, r.employee_count,
                   r.total_gross, r.total_deductions, r.total_net,
                   COALESCE(r.approved_by,'—') AS approved_by,
                   TO_CHAR(r.created_at,'DD-Mon-YYYY') AS created_at,
                   r.journal_entry_id,
                  jem.voucher_no,
                   CASE r.status
                       WHEN 'DRAFT'       THEN '<span class="badge bg-secondary">Draft</span>'
                       WHEN 'PROCESSING'  THEN '<span class="badge bg-info text-dark">Processing</span>'
                       WHEN 'COMPLETED'   THEN '<span class="badge bg-primary">Computed</span>'
                       WHEN 'APPROVED'    THEN '<span class="badge bg-teal">Approved</span>'
                       WHEN 'PAID'        THEN '<span class="badge bg-success">Paid</span>'
                       WHEN 'CANCELLED'   THEN '<span class="badge bg-danger">Cancelled</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="prShow('    || r.id || ')" class="btn btn-white btn-sm" title="View Lines"><i class="fas fa-eye text-success"></i></a>'
                   || CASE WHEN r.status = 'COMPLETED' THEN
                       '<a href="javascript:;" onclick="prApprove('|| r.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check-double text-success"></i></a>'
                      ELSE '' END
                   || CASE WHEN r.status = 'APPROVED' THEN
                       '<a href="javascript:;" onclick="prPay('    || r.id || ')" class="btn btn-white btn-sm" title="Mark Paid"><i class="fas fa-money-check-alt text-primary"></i></a>'
                      ELSE '' END
                   || CASE WHEN r.status NOT IN ('PAID','CANCELLED') THEN
                       '<a href="javascript:;" onclick="prCancel(' || r.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fas fa-ban text-danger"></i></a>'
                      ELSE '' END
                   || '</div>' AS actions
            FROM hrm_payroll_runs r
                LEFT JOIN acc_journal_entry_master jem ON jem.id = r.journal_entry_id
            %s ORDER BY r.payroll_month DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public PayrollRunDTO toDTO(PayrollRun e) {
        PayrollRunDTO d = PayrollRunDTO.builder()
            .id(e.getId()).payrollMonth(e.getPayrollMonth()).runDate(e.getRunDate())
            .status(e.getStatus() != null ? e.getStatus().name() : "DRAFT")
            .totalGross(e.getTotalGross()).totalDeductions(e.getTotalDeductions()).totalNet(e.getTotalNet())
            .employeeCount(e.getEmployeeCount()).approvedBy(e.getApprovedBy())
            .approvedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null)
            .remarks(e.getRemarks())
            .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        List<PayrollRunLine> lines = lineRepo.findByPayrollRunId(e.getId());
        d.setLines(lines.stream().map(l -> PayrollRunDTO.LineDTO.builder()
            .id(l.getId())
            .employeeId(l.getEmployee().getId())
            .employeeCode(l.getEmployee().getEmployeeCode())
            .employeeName(l.getEmployee().getFullName())
            .departmentName(l.getEmployee().getDepartment() != null ? l.getEmployee().getDepartment().getName() : null)
            .designationName(l.getEmployee().getDesignation() != null ? l.getEmployee().getDesignation().getDesignationName() : null)
            .basicSalary(l.getBasicSalary()).houseRent(l.getHouseRent()).medicalAllowance(l.getMedicalAllowance())
            .transportAllowance(l.getTransportAllowance()).overtime(l.getOvertime()).otherAllowances(l.getOtherAllowances())
            .grossSalary(l.getGrossSalary()).incomeTax(l.getIncomeTax()).providentFund(l.getProvidentFund())
            .loanDeduction(l.getLoanDeduction()).otherDeductions(l.getOtherDeductions()).netSalary(l.getNetSalary())
            .workingDays(l.getWorkingDays()).leaveDays(l.getLeaveDays()).absentDays(l.getAbsentDays())
            .paymentStatus(l.getPaymentStatus() != null ? l.getPaymentStatus().name() : "PENDING")
            .build()).collect(Collectors.toList()));
        return d;
    }

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String f = orgId != null ? " AND organization_id=" + orgId : "";
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("totalActive",    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hrm_employees WHERE status='ACTIVE'" + f, Long.class));
        m.put("totalEmployees", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hrm_employees WHERE 1=1" + f, Long.class));
        m.put("onLeave",        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hrm_employees WHERE status='ON_LEAVE'" + f, Long.class));
        m.put("pendingLeaves",  jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hrm_employee_leaves WHERE status='PENDING'" + f, Long.class));
        String today = LocalDate.now().toString();
        m.put("presentToday",   orgId != null ? jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hrm_attendances WHERE organization_id=" + orgId + " AND att_date='" + today + "' AND status='PRESENT'", Long.class) : 0L);
        return m;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Employee buildEmployee(EmployeeDTO dto, Employee e) {
        e.setFirstName(dto.getFirstName().trim());
        e.setLastName(dto.getLastName().trim());
        e.setEmail(dto.getEmail());
        e.setPhone(dto.getPhone().trim());
        e.setGender(Employee.Gender.valueOf(dto.getGender() != null ? dto.getGender() : "MALE"));
        e.setDateOfBirth(dto.getDateOfBirth());
        e.setBloodGroup(dto.getBloodGroup()); e.setMaritalStatus(dto.getMaritalStatus());
        e.setNationalId(dto.getNationalId()); e.setPassportNumber(dto.getPassportNumber());
        e.setEmployeeType(Employee.EmployeeType.valueOf(dto.getEmployeeType() != null ? dto.getEmployeeType() : "PERMANENT"));
        e.setStatus(Employee.EmployeeStatus.valueOf(dto.getStatus() != null ? dto.getStatus() : "ACTIVE"));
        e.setJoiningDate(dto.getJoiningDate());
        e.setConfirmationDate(dto.getConfirmationDate()); e.setProbationEndDate(dto.getProbationEndDate());
        e.setResignationDate(dto.getResignationDate()); e.setExitDate(dto.getExitDate());
        e.setBasicSalary(dto.getBasicSalary()); e.setGrossSalary(dto.getGrossSalary());
        e.setBankName(dto.getBankName()); e.setBankAccountNumber(dto.getBankAccountNumber()); e.setBankBranch(dto.getBankBranch());
        e.setWorkLocation(dto.getWorkLocation()); e.setWorkShift(dto.getWorkShift());
        e.setAnnualLeaveDays(dto.getAnnualLeaveDays() != null ? dto.getAnnualLeaveDays() : 0);
        e.setSickLeaveDays(dto.getSickLeaveDays() != null ? dto.getSickLeaveDays() : 0);
        e.setCasualLeaveDays(dto.getCasualLeaveDays() != null ? dto.getCasualLeaveDays() : 0);
        e.setEmergencyContactName(dto.getEmergencyContactName());
        e.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        e.setEmergencyContactRelation(dto.getEmergencyContactRelation());
        e.setNotes(dto.getNotes());
        e.setDepartment(deptRepo.getReferenceById(dto.getDepartmentId()));
        e.setDesignation(designRepo.getReferenceById(dto.getDesignationId()));
        if (dto.getReportingManagerId() != null) e.setReportingManager(empRepo.getReferenceById(dto.getReportingManagerId()));
        else e.setReportingManager(null);
        return e;
    }

    private String generateEmpCode(Long orgId) {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(employee_code FROM 4) AS INTEGER)),0)+1 FROM hrm_employees WHERE organization_id=? AND employee_code ~ '^EMP'";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, orgId);
        return String.format("EMP%05d", next != null ? next : 1);
    }

    private EmployeeDTO.SalaryDTO toSalaryDTO(EmployeeSalary s) {
        return EmployeeDTO.SalaryDTO.builder()
            .id(s.getId()).effectiveDate(s.getEffectiveDate()).endDate(s.getEndDate())
            .basicSalary(s.getBasicSalary()).houseRent(s.getHouseRent())
            .medicalAllowance(s.getMedicalAllowance()).transportAllowance(s.getTransportAllowance())
            .otherAllowances(s.getOtherAllowances()).grossSalary(s.getGrossSalary())
            .incomeTax(s.getIncomeTax()).providentFund(s.getProvidentFund()).otherDeductions(s.getOtherDeductions())
            .netSalary(s.getNetSalary()).isCurrent(s.isCurrent()).remarks(s.getRemarks())
            .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
            .build();
    }

    private Designation findDesig(Long id) { return designRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Designation #"+id+" not found.")); }
    private Employee findEmp(Long id) { return empRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee #"+id+" not found.")); }
    private EmployeeLeave findLeave(Long id) { return leaveRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Leave #"+id+" not found.")); }
    private PayrollRun findRun(Long id) { return runRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Payroll run #"+id+" not found.")); }
    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private void audit(Object e, boolean isCreate) {
        String user = SecurityHelper.currentUsername().orElse("system");
        LocalDateTime now = LocalDateTime.now();
        if (e instanceof Designation d)  { if (isCreate) { d.setCreatedBy(user); d.setCreatedAt(now); } d.setUpdatedBy(user); d.setUpdatedAt(now); }
        else if (e instanceof Employee d){ if (isCreate) { d.setCreatedBy(user); d.setCreatedAt(now); } d.setUpdatedBy(user); d.setUpdatedAt(now); }
        else if (e instanceof EmployeeLeave d){ if (isCreate) { d.setCreatedBy(user); d.setCreatedAt(now); } d.setUpdatedBy(user); d.setUpdatedAt(now); }
        else if (e instanceof PayrollRun d){ if (isCreate) { d.setCreatedBy(user); d.setCreatedAt(now); } d.setUpdatedBy(user); d.setUpdatedAt(now); }
    }
}
