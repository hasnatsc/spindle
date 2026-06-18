package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository
        extends JpaRepository<Country, Long>,
                JpaSpecificationExecutor<Country> {

    Optional<Country> findByIsoCode(String isoCode);

    List<Country> findByActiveTrue();

    boolean existsByIsoCode(String isoCode);

    boolean existsByIsoCodeAndIdNot(String isoCode, Long id);
}
