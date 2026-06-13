package com.asg.spindleserp.crm.repository;

import com.asg.spindleserp.crm.entity.CustomerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long>, JpaSpecificationExecutor<CustomerFeedback> {
    List<CustomerFeedback> findByCustomerIdAndStatus(Long customerId, CustomerFeedback.FeedbackStatus status);

    List<CustomerFeedback> findByOrganizationIdAndStatus(Long orgId, CustomerFeedback.FeedbackStatus status);
}
