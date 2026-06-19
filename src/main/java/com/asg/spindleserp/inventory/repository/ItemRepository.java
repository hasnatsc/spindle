package com.asg.spindleserp.inventory.repository;

import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.inventory.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository
        extends JpaRepository<Item, Long>,
                JpaSpecificationExecutor<Item> {

    Optional<Item> findByOrganizationIdAndItemCode(Long orgId, String code);

    List<Item> findByOrganizationIdAndItemTypeAndIsActiveTrue(Long orgId, ItemType type);

    List<Item> findByOrganizationIdAndIsActiveTrue(Long orgId);

    boolean existsByOrganizationIdAndItemCode(Long orgId, String code);

    boolean existsByOrganizationIdAndItemCodeAndIdNot(Long orgId, String code, Long id);

    boolean existsByOrganizationIdAndItemName(Long orgId, String name);

    boolean existsByOrganizationIdAndItemNameAndIdNot(Long orgId, String name, Long id);

    @Query("SELECT i FROM Item i WHERE i.organization.id = :orgId AND i.isActive = true " +
           "AND (LOWER(i.itemCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR   LOWER(i.itemName) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Item> search(@Param("orgId") Long orgId, @Param("q") String q, Pageable p);

    @Query("SELECT i FROM Item i WHERE i.organization.id = :orgId AND i.isActive = true " +
           "AND i.itemType IN ('RAW_MATERIAL','SEMI_FINISHED','CONSUMABLE','MRO')")
    List<Item> findProductionInputItems(@Param("orgId") Long orgId);

    @Query("SELECT i FROM Item i WHERE i.organization.id = :orgId AND i.isActive = true " +
           "AND i.itemType IN ('FINISHED_GOOD','SEMI_FINISHED')")
    List<Item> findFinishedItems(@Param("orgId") Long orgId);
}
