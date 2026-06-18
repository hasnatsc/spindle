package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository
        extends JpaRepository<Currency, Long>,
                JpaSpecificationExecutor<Currency> {

    Optional<Currency> findByCode(String code);

    List<Currency> findByActiveTrue();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
