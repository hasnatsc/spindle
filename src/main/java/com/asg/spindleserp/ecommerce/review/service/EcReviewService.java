// Path: com/asg/spindleserp/ecommerce/service/EcReviewService.java
package com.asg.spindleserp.ecommerce.review.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.review.dto.EcReviewDTO;
import com.asg.spindleserp.ecommerce.review.entity.EcReview;

import java.util.List;
public interface EcReviewService {
    EcReviewDTO findById(Long id);
    EcReviewDTO approve(Long id);
    EcReviewDTO reject(Long id);
    EcReviewDTO hide(Long id);
    void delete(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcReviewDTO toDTO(EcReview entity);
}
