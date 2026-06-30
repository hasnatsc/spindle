// Path: com/asg/spindleserp/ecommerce/service/EcReviewServiceImpl.java
package com.asg.spindleserp.ecommerce.review.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.review.dto.EcReviewDTO;
import com.asg.spindleserp.ecommerce.review.entity.EcReview;
import com.asg.spindleserp.ecommerce.review.repository.EcReviewRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j @Service @Transactional @RequiredArgsConstructor
public class EcReviewServiceImpl implements EcReviewService {

    private final EcReviewRepository reviewRepository;
    private final JdbcTemplate        jdbcTemplate;

    @Override @Transactional(readOnly = true)
    public EcReviewDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    public EcReviewDTO approve(Long id) {
        EcReview e = findEntityById(id);
        e.setReviewStatus(EcReview.ReviewStatus.APPROVED);
        e.setApprovedBy(SecurityHelper.currentUsername().orElse("system"));
        e.setApprovedAt(LocalDateTime.now());
        return toDTO(reviewRepository.save(e));
    }

    @Override
    public EcReviewDTO reject(Long id) {
        EcReview e = findEntityById(id);
        e.setReviewStatus(EcReview.ReviewStatus.REJECTED);
        return toDTO(reviewRepository.save(e));
    }

    @Override
    public EcReviewDTO hide(Long id) {
        EcReview e = findEntityById(id);
        e.setReviewStatus(EcReview.ReviewStatus.HIDDEN);
        return toDTO(reviewRepository.save(e));
    }

    @Override
    public void delete(Long id) { reviewRepository.delete(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE r.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "p.product_title", "c.full_name", "r.review_title", "r.review_status"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY r.id DESC) AS sl, COUNT(*) OVER () AS full_count,
                   r.id,
                   p.product_title,
                   COALESCE(c.full_name, '—') AS customer_name,
                   r.rating,
                   COALESCE(r.review_title, '—') AS review_title,
                   CASE WHEN r.verified_purchase THEN '<span class="badge bg-success">Verified</span>'
                        ELSE '<span class="badge bg-secondary">Unverified</span>' END AS verified_badge,
                   r.review_status,
                   r.likes_count, r.helpful_count,
                   TO_CHAR(r.created_at, 'DD-Mon-YYYY') AS created_at,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="ecrevShow('    || r.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="ecrevApprove(' || r.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check text-success"></i></a>'
                   || '<a href="javascript:;" onclick="ecrevReject('  || r.id || ')" class="btn btn-white btn-sm" title="Reject"><i class="fas fa-times text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="ecrevHide('    || r.id || ')" class="btn btn-white btn-sm" title="Hide"><i class="fas fa-eye-slash text-secondary"></i></a>'
                   || '<a href="javascript:;" onclick="ecrevDelete('  || r.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM ec_reviews r
            JOIN ec_product_catalog p ON p.id = r.product_id
            LEFT JOIN ec_customers  c ON c.id = r.customer_id
            %s ORDER BY r.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcReviewDTO toDTO(EcReview e) {
        List<String> imageUrls = e.getImages().stream()
                .map(img -> img.getImageUrl()).toList();
        return EcReviewDTO.builder()
                .id(e.getId())
                .productId(e.getProduct() != null ? e.getProduct().getId() : null)
                .productTitle(e.getProduct() != null ? e.getProduct().getProductTitle() : null)
                .customerId(e.getCustomer() != null ? e.getCustomer().getId() : null)
                .customerName(e.getCustomer() != null ? e.getCustomer().getFullName() : null)
                .orderItemId(e.getOrderItem() != null ? e.getOrderItem().getId() : null)
                .rating(e.getRating()).reviewTitle(e.getReviewTitle())
                .reviewText(e.getReviewText()).pros(e.getPros()).cons(e.getCons())
                .verifiedPurchase(e.isVerifiedPurchase()).recommendation(e.getRecommendation())
                .likesCount(e.getLikesCount()).dislikesCount(e.getDislikesCount())
                .helpfulCount(e.getHelpfulCount()).reportCount(e.getReportCount())
                .reviewStatus(e.getReviewStatus() != null ? e.getReviewStatus().name() : null)
                .approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null)
                .active(e.isActive()).imageUrls(imageUrls)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).build();
    }

    private EcReview findEntityById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review #" + id + " not found."));
    }
}
