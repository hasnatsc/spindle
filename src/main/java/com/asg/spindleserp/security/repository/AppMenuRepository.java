package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.AppMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppMenuRepository extends JpaRepository<AppMenu, Long> {
    List<AppMenu> findByParentIdIsNullAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

    List<AppMenu> findByParentIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(Long parentId);

    Optional<AppMenu> findByMenuCode(String menuCode);

    List<AppMenu> findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();
}
