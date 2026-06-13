package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.AppMenu;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppMenuRepository extends JpaRepository<AppMenu, Long> {

    // Full menu tree for navigation building
    List<AppMenu> findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

    // Top-level MODULE entries only
    List<AppMenu> findByParentIdIsNullAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

    // Children of a given parent
    List<AppMenu> findByParentIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(Long parentId);

    Optional<AppMenu> findByMenuCode(String menuCode);

    boolean existsByMenuCode(String menuCode);
    boolean existsByMenuCodeAndIdNot(String menuCode, Long id);

    // Management: all menus including inactive
    List<AppMenu> findAllByOrderByDisplayOrderAsc();
}
