package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.TrvVisaApplicationDTO;
import com.asg.spindleserp.travel.dto.TrvVisaTypeDTO;
import com.asg.spindleserp.travel.entity.TrvVisaApplication;
import com.asg.spindleserp.travel.entity.TrvVisaDocument;
import com.asg.spindleserp.travel.entity.TrvVisaType;
import com.asg.spindleserp.travel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TravelVisaServiceImpl implements TravelVisaService {

    private final TrvVisaTypeRepository        visaTypeRepo;
    private final TrvVisaApplicationRepository applicationRepo;
    private final TrvVisaDocumentRepository    documentRepo;
    private final TrvPassengerRepository       passengerRepo;
    private final JdbcTemplate                 jdbcTemplate;

    // =========================================================================
    // VISA TYPES
    // =========================================================================

    @Override
    public TrvVisaTypeDTO saveVisaType(TrvVisaTypeDTO dto) {
        TrvVisaType e = dto.getId() != null
            ? visaTypeRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Visa type #" + dto.getId() + " not found."))
            : new TrvVisaType();
        e.setCountry(dto.getCountry());
        e.setVisaCategory(dto.getVisaCategory());
        e.setProcessingDays(dto.getProcessingDays());
        e.setFeeAmount(dto.getFeeAmount());
        e.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "BDT");
        e.setDescription(dto.getDescription());
        TrvVisaType saved = visaTypeRepo.save(e);
        return TrvVisaTypeDTO.builder()
            .id(saved.getId()).country(saved.getCountry()).visaCategory(saved.getVisaCategory())
            .processingDays(saved.getProcessingDays()).feeAmount(saved.getFeeAmount())
            .currency(saved.getCurrency()).description(saved.getDescription())
            .build();
    }

    @Override
    public void deleteVisaType(Long id) { visaTypeRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listVisaTypes() {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY country, visa_category) AS sl,
                   country, visa_category, COALESCE(processing_days::text,'—') AS processing_days,
                   COALESCE(fee_amount::text,'0') AS fee_amount, currency,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="vtEdit('   || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="vtDelete(' || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM trv_visa_types ORDER BY country, visa_category
            """);
    }

    // =========================================================================
    // VISA APPLICATIONS
    // =========================================================================

    @Override
    public TrvVisaApplicationDTO saveApplication(TrvVisaApplicationDTO dto) {
        TrvVisaApplication e = dto.getId() != null
            ? applicationRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Visa application #" + dto.getId() + " not found."))
            : new TrvVisaApplication();

        e.setApplicationNumber(dto.getApplicationNumber());
        e.setSubmissionDate(dto.getSubmissionDate());
        e.setExpectedDate(dto.getExpectedDate());
        e.setApprovalDate(dto.getApprovalDate());
        if (e.getStatus() == null) e.setStatus(TrvVisaApplication.Status.PENDING);
        e.setFeeAmount(dto.getFeeAmount());
        e.setRemarks(dto.getRemarks());
        e.setBookingServiceId(dto.getBookingServiceId());
        e.setPassengerId(dto.getPassengerId());
        e.setVisaTypeId(dto.getVisaTypeId());
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        TrvVisaApplication saved = applicationRepo.save(e);

        documentRepo.findByVisaApplicationId(saved.getId()).forEach(d -> documentRepo.deleteById(d.getId()));
        if (dto.getDocuments() != null) {
            dto.getDocuments().forEach(d -> documentRepo.save(TrvVisaDocument.builder()
                .visaApplicationId(saved.getId())
                .documentName(d.getDocumentName())
                .isReceived(Boolean.TRUE.equals(d.getIsReceived()))
                .remarks(d.getRemarks())
                .build()));
        }
        return findApplicationById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TrvVisaApplicationDTO findApplicationById(Long id) {
        TrvVisaApplication e = applicationRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Visa application #" + id + " not found."));

        String passengerName = passengerRepo.findById(e.getPassengerId())
            .map(p -> p.getFirstName() + (p.getLastName() != null ? " " + p.getLastName() : "")).orElse(null);
        String visaTypeDisplay = visaTypeRepo.findById(e.getVisaTypeId())
            .map(v -> v.getCountry() + " — " + v.getVisaCategory()).orElse(null);

        TrvVisaApplicationDTO dto = TrvVisaApplicationDTO.builder()
            .id(e.getId()).applicationNumber(e.getApplicationNumber())
            .submissionDate(e.getSubmissionDate()).expectedDate(e.getExpectedDate()).approvalDate(e.getApprovalDate())
            .status(e.getStatus() != null ? e.getStatus().name() : null)
            .feeAmount(e.getFeeAmount()).remarks(e.getRemarks())
            .bookingServiceId(e.getBookingServiceId())
            .passengerId(e.getPassengerId()).passengerName(passengerName)
            .visaTypeId(e.getVisaTypeId()).visaTypeDisplay(visaTypeDisplay)
            .build();

        dto.setDocuments(documentRepo.findByVisaApplicationId(id).stream()
            .map(d -> TrvVisaApplicationDTO.DocumentDTO.builder()
                .id(d.getId()).documentName(d.getDocumentName())
                .isReceived(d.getIsReceived()).remarks(d.getRemarks())
                .build()).collect(Collectors.toList()));
        return dto;
    }

    @Override
    public void deleteApplication(Long id) {
        documentRepo.findByVisaApplicationId(id).forEach(d -> documentRepo.deleteById(d.getId()));
        applicationRepo.deleteById(id);
    }

    @Override
    public TrvVisaApplicationDTO changeApplicationStatus(Long id, String status) {
        TrvVisaApplication e = applicationRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Visa application #" + id + " not found."));
        e.setStatus(TrvVisaApplication.Status.valueOf(status));
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        applicationRepo.save(e);
        return findApplicationById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listApplications(String search) {
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT va.id, ROW_NUMBER() OVER (ORDER BY va.id DESC) AS sl,
                   b.booking_no, p.first_name || COALESCE(' ' || p.last_name,'') AS passenger_name,
                   vt.country || ' — ' || vt.visa_category AS visa_type,
                   COALESCE(va.application_number,'—') AS application_number,
                   TO_CHAR(va.submission_date,'DD-Mon-YYYY') AS submission_date,
                   TO_CHAR(va.expected_date,'DD-Mon-YYYY') AS expected_date,
                   CASE va.status
                       WHEN 'PENDING'   THEN '<span class="badge bg-secondary">Pending</span>'
                       WHEN 'SUBMITTED' THEN '<span class="badge bg-info">Submitted</span>'
                       WHEN 'APPROVED'  THEN '<span class="badge bg-success">Approved</span>'
                       WHEN 'REJECTED'  THEN '<span class="badge bg-danger">Rejected</span>'
                       WHEN 'COLLECTED' THEN '<span class="badge bg-dark">Collected</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="visaShow('   || va.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || '<a href="javascript:;" onclick="visaEdit('   || va.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="visaDelete(' || va.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_visa_applications va
            JOIN   trv_booking_services bs ON bs.id = va.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            JOIN   trv_passengers p ON p.id = va.passenger_id
            JOIN   trv_visa_types vt ON vt.id = va.visa_type_id
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR p.first_name ILIKE ? OR va.application_number ILIKE ?)
            ORDER  BY va.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like);
    }
}
