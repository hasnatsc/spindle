package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPackageInclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvPackageInclusionRepository extends JpaRepository<TrvPackageInclusion, Long> {

    List<TrvPackageInclusion> findByPackageEntityId(Long packageId);
}
