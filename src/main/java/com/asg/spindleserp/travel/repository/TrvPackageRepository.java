package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrvPackageRepository extends JpaRepository<TrvPackage, Long> {
    List<TrvPackage> findByIsActiveTrue();
}
