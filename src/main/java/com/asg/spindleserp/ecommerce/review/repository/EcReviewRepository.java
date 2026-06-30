// Path: com/asg/spindleserp/ecommerce/repository/EcReviewRepository.java
package com.asg.spindleserp.ecommerce.review.repository;
import com.asg.spindleserp.ecommerce.review.entity.EcReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface EcReviewRepository extends JpaRepository<EcReview, Long> {
    List<EcReview> findByOrganizationIdOrderByIdDesc(Long orgId);
    List<EcReview> findByProductIdOrderByIdDesc(Long productId);
    List<EcReview> findByOrganizationIdAndReviewStatus(Long orgId, EcReview.ReviewStatus status);
}
