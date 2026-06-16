package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.UserAccessScope;
import com.asg.spindleserp.security.entity.UserAccessScope.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserAccessScopeRepository extends JpaRepository<UserAccessScope, Long> {

    List<UserAccessScope> findByUserIdAndScopeType(Long userId, ScopeType scopeType);

    List<UserAccessScope> findByUserId(Long userId);

    @Query("SELECT s.referenceId FROM UserAccessScope s WHERE s.user.id = :userId AND s.scopeType = :type")
    Set<Long> findReferenceIdsByUserIdAndScopeType(@Param("userId") Long userId,
                                                    @Param("type") ScopeType type);

    @Modifying
    @Query("DELETE FROM UserAccessScope s WHERE s.user.id = :userId AND s.scopeType = :type")
    void deleteByUserIdAndScopeType(@Param("userId") Long userId,
                                     @Param("type") ScopeType type);

    @Modifying
    @Query("DELETE FROM UserAccessScope s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndScopeTypeAndReferenceId(Long userId, ScopeType type, Long refId);
}
