package com.asg.spindleserp.production.repository;

import com.asg.spindleserp.production.entity.BomItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomItemRepository extends JpaRepository<BomItem, Long> {
    List<BomItem> findByBomIdOrderByLineNumber(Long bomId);

    List<BomItem> findByRawItemId(Long rawItemId);
}
