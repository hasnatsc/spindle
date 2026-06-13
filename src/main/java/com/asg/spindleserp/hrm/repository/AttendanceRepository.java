package com.asg.spindleserp.hrm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeIdAndAttDate(Long empId, LocalDate date);

    List<Attendance> findByOrganizationIdAndAttDate(Long orgId, LocalDate date);

    List<Attendance> findByEmployeeIdAndAttDateBetween(Long empId, LocalDate from, LocalDate to);

    long countByEmployeeIdAndAttDateBetweenAndStatus(
            Long empId, LocalDate from, LocalDate to, Attendance.AttendanceStatus status);
}
