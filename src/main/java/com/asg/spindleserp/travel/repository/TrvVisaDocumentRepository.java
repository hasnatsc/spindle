package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvVisaDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvVisaDocumentRepository extends JpaRepository<TrvVisaDocument, Long> {

    List<TrvVisaDocument> findByVisaApplicationId(Long visaApplicationId);
}
