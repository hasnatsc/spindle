package com.asg.spindleserp.hrm.repository;

import com.asg.spindleserp.hrm.entity.EmployeeAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAddressRepository extends JpaRepository<EmployeeAddress, Long> {
    List<EmployeeAddress> findByEmployeeId(Long empId);

    Optional<EmployeeAddress> findByEmployeeIdAndAddressType(Long empId, EmployeeAddress.AddressType type);
}
