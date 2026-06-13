package com.asg.spindleserp.crm.repository;

import com.asg.spindleserp.crm.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByCustomerIdAndIsActiveTrue(Long customerId);

    Optional<Contact> findByCustomerIdAndIsPrimaryTrue(Long customerId);
}
