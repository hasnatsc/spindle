package com.asg.spindleserp.crm.repository;

import com.asg.spindleserp.crm.entity.CrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {
    List<CrmActivity> findByOpportunityIdOrderByActivityDateDesc(Long oppId);

    List<CrmActivity> findByAssignedToIdAndStatus(Long userId, CrmActivity.ActivityStatus status);
}
