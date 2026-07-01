package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvSupplierCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvSupplierCostRepository extends JpaRepository<TrvSupplierCost, Long> {

    List<TrvSupplierCost> findByBookingServiceId(Long bookingServiceId);

    List<TrvSupplierCost> findBySupplierId(Long supplierId);
}
