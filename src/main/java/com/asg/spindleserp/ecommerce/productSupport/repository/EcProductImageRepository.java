// Path: com/asg/spindleserp/ecommerce/productSupport/repository/EcProductImageRepository.java
package com.asg.spindleserp.ecommerce.productSupport.repository;

import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EcProductImageRepository extends JpaRepository<EcProductImage, Long> {

    List<EcProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    Optional<EcProductImage> findByIdAndProductId(Long id, Long productId);

    long countByProductId(Long productId);

    /** Used to unset the previous primary image when a new one is marked primary. */
    List<EcProductImage> findByProductIdAndIsPrimaryTrue(Long productId);
}
