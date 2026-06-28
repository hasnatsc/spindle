package com.asg.spindleserp.crm.service;

import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.crm.dto.*;
import com.asg.spindleserp.crm.entity.*;
import com.asg.spindleserp.crm.repository.*;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.security.repository.UserRepository;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CrmServiceImpl implements CrmService {

    private final LeadRepository             leadRepo;
    private final OpportunityRepository      oppRepo;
    private final ContactRepository          contactRepo;
    private final CrmActivityRepository      actRepo;
    private final CustomerFeedbackRepository feedRepo;
    private final ChartOfAccountSubRepository subRepo;
    private final UserRepository             userRepo;
    private final OrganizationRepository     orgRepo;
    private final DocumentSequenceService    seqService;
    private final JdbcTemplate               jdbcTemplate;

    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");

    // =========================================================================
    // LEADS
    // =========================================================================

    @Override
    public LeadDTO createLead(LeadDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String leadNo = seqService.nextDocumentNumber(orgId, "LEAD", LocalDate.now().format(YY));
        Lead entity = Lead.builder()
            .leadNo(leadNo)
            .companyName(dto.getCompanyName())
            .contactName(dto.getContactName().trim())
            .contactEmail(dto.getContactEmail())
            .contactPhone(dto.getContactPhone())
            .designation(dto.getDesignation())
            .country(dto.getCountry())
            .city(dto.getCity())
            .source(dto.getSource())
            .leadType(dto.getLeadType() != null ? dto.getLeadType() : "B2B")
            .productInterest(dto.getProductInterest())
            .estimatedQtyKg(dto.getEstimatedQtyKg())
            .status(Lead.LeadStatus.NEW)
            .remarks(dto.getRemarks())
            .build();
        entity.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getAssignedToId() != null) entity.setAssignedTo(userRepo.getReferenceById(dto.getAssignedToId()));
        audit(entity, true);
        return toDTO(leadRepo.save(entity));
    }

    @Override
    public LeadDTO updateLead(Long id, LeadDTO dto) {
        Lead entity = findLead(id);
        entity.setCompanyName(dto.getCompanyName());
        entity.setContactName(dto.getContactName().trim());
        entity.setContactEmail(dto.getContactEmail());
        entity.setContactPhone(dto.getContactPhone());
        entity.setDesignation(dto.getDesignation());
        entity.setCountry(dto.getCountry());
        entity.setCity(dto.getCity());
        entity.setSource(dto.getSource());
        entity.setLeadType(dto.getLeadType() != null ? dto.getLeadType() : "B2B");
        entity.setProductInterest(dto.getProductInterest());
        entity.setEstimatedQtyKg(dto.getEstimatedQtyKg());
        entity.setRemarks(dto.getRemarks());
        if (dto.getAssignedToId() != null) entity.setAssignedTo(userRepo.getReferenceById(dto.getAssignedToId()));
        else entity.setAssignedTo(null);
        audit(entity, false);
        return toDTO(leadRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public LeadDTO findLeadById(Long id) { return toDTO(findLead(id)); }

    @Override
    public void deleteLead(Long id) {
        Lead entity = findLead(id);
        if (entity.getStatus() == Lead.LeadStatus.CONVERTED)
            throw new IllegalStateException("Converted leads cannot be deleted.");
        leadRepo.delete(entity);
    }

    @Override
    public LeadDTO updateLeadStatus(Long id, String newStatus) {
        Lead entity = findLead(id);
        entity.setStatus(Lead.LeadStatus.valueOf(newStatus));
        audit(entity, false);
        return toDTO(leadRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse leadDatatable(int draw, int start, int length, String search, String status) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND l.organization_id = " + orgId : "")
            + (status != null && !status.isBlank() ? " AND l.status = '" + status + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "l.lead_no", "l.company_name", "l.contact_name",
                "l.contact_email", "l.contact_phone", "l.source", "l.status"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY l.id DESC)               AS sl,
                COUNT(*)     OVER ()                                  AS full_count,
                l.id, l.lead_no, l.company_name, l.contact_name,
                l.contact_email, l.contact_phone, l.country,
                l.source, l.lead_type,
                COALESCE(l.estimated_qty_kg::text,'—')               AS estimated_qty_kg,
                l.status,
                COALESCE(u.full_name, u.username, '—')               AS assigned_to,
                TO_CHAR(l.created_at,'DD-Mon-YYYY')                  AS created_at,
                COALESCE(l.product_interest,'—')                     AS product_interest,
                CASE l.status
                    WHEN 'NEW'         THEN '<span class="badge bg-secondary">New</span>'
                    WHEN 'CONTACTED'   THEN '<span class="badge bg-info text-dark">Contacted</span>'
                    WHEN 'QUALIFIED'   THEN '<span class="badge bg-primary">Qualified</span>'
                    WHEN 'UNQUALIFIED' THEN '<span class="badge bg-dark">Unqualified</span>'
                    WHEN 'CONVERTED'   THEN '<span class="badge bg-success">Converted</span>'
                    WHEN 'LOST'        THEN '<span class="badge bg-danger">Lost</span>'
                    WHEN 'DORMANT'     THEN '<span class="badge bg-warning text-dark">Dormant</span>'
                    ELSE '<span class="badge bg-light text-dark">' || l.status || '</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="leadShow('   || l.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="leadEdit('   || l.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="leadActivity(' || l.id || ')" class="btn btn-white btn-sm" title="Add Activity"><i class="fas fa-calendar-plus text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="leadConvert(' || l.id || ')" class="btn btn-white btn-sm" title="Convert to Opportunity"><i class="fas fa-arrow-circle-right text-teal"></i></a>'
                    || '<a href="javascript:;" onclick="leadDelete(' || l.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                       AS actions
            FROM crm_leads l
            LEFT JOIN sec_users u ON u.id = l.assigned_to_id
            %s
            ORDER BY l.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> searchLeads(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int pageSize = 30, offset = (page - 1) * pageSize;
        String sql = "SELECT id, lead_no, contact_name, company_name FROM crm_leads WHERE 1=1"
            + (orgId != null ? " AND organization_id = " + orgId : "")
            + (q != null && !q.isBlank() ? " AND (lead_no ILIKE '%" + q.replace("'","''") + "%' OR contact_name ILIKE '%" + q.replace("'","''") + "%' OR company_name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY id DESC LIMIT " + (pageSize + 1) + " OFFSET " + offset;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        boolean hasMore = rows.size() > pageSize;
        List<Map<String, Object>> items = rows.stream().limit(pageSize).map(r -> Map.of(
            "id",   r.get("id"),
            "text", r.get("lead_no") + " — " + r.get("contact_name") + (r.get("company_name") != null ? " (" + r.get("company_name") + ")" : "")
        )).toList();
        return Map.of("items", items, "hasMore", hasMore);
    }

    @Override
    public LeadDTO toDTO(Lead e) {
        LeadDTO d = LeadDTO.builder()
            .id(e.getId()).leadNo(e.getLeadNo())
            .companyName(e.getCompanyName()).contactName(e.getContactName())
            .contactEmail(e.getContactEmail()).contactPhone(e.getContactPhone())
            .designation(e.getDesignation()).country(e.getCountry()).city(e.getCity())
            .source(e.getSource()).leadType(e.getLeadType())
            .productInterest(e.getProductInterest()).estimatedQtyKg(e.getEstimatedQtyKg())
            .status(e.getStatus() != null ? e.getStatus().name() : "NEW")
            .remarks(e.getRemarks())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getAssignedTo() != null) { d.setAssignedToId(e.getAssignedTo().getId()); d.setAssignedToDisplay(e.getAssignedTo().getFullName() != null ? e.getAssignedTo().getFullName() : e.getAssignedTo().getUsername()); }
        if (e.getConvertedTo() != null) { d.setConvertedToId(e.getConvertedTo().getId()); d.setConvertedToDisplay(e.getConvertedTo().getSubAccountCode() + " — " + e.getConvertedTo().getSubAccountName()); }
        return d;
    }

    // =========================================================================
    // OPPORTUNITIES
    // =========================================================================

    @Override
    public OpportunityDTO createOpportunity(OpportunityDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String oppNo = seqService.nextDocumentNumber(orgId, "OPP", LocalDate.now().format(YY));
        Opportunity entity = Opportunity.builder()
            .opportunityNo(oppNo)
            .title(dto.getTitle().trim())
            .description(dto.getDescription())
            .stage(Opportunity.OpportunityStage.valueOf(dto.getOpportunityStage() != null ? dto.getOpportunityStage() : "PROSPECT"))
            .probability(dto.getProbability() != null ? dto.getProbability() : BigDecimal.ZERO)
            .estimatedValue(dto.getEstimatedValue())
            .currency(dto.getCurrency() != null ? dto.getCurrency() : "BDT")
            .expectedCloseDate(dto.getExpectedCloseDate())
            .actualCloseDate(dto.getActualCloseDate())
            .lostReason(dto.getLostReason())
            .remarks(dto.getRemarks())
            .build();
        entity.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getCustomerId() != null) entity.setCustomer(subRepo.getReferenceById(dto.getCustomerId()));
        if (dto.getLeadId() != null) entity.setLead(leadRepo.getReferenceById(dto.getLeadId()));
        if (dto.getAssignedToId() != null) entity.setAssignedTo(userRepo.getReferenceById(dto.getAssignedToId()));
        audit(entity, true);
        return toDTO(oppRepo.save(entity));
    }

    @Override
    public OpportunityDTO updateOpportunity(Long id, OpportunityDTO dto) {
        Opportunity entity = findOpp(id);
        entity.setTitle(dto.getTitle().trim());
        entity.setDescription(dto.getDescription());
        entity.setStage(Opportunity.OpportunityStage.valueOf(dto.getOpportunityStage() != null ? dto.getOpportunityStage() : entity.getStage().name()));
        entity.setProbability(dto.getProbability() != null ? dto.getProbability() : BigDecimal.ZERO);
        entity.setEstimatedValue(dto.getEstimatedValue());
        entity.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "BDT");
        entity.setExpectedCloseDate(dto.getExpectedCloseDate());
        entity.setLostReason(dto.getLostReason());
        entity.setRemarks(dto.getRemarks());
        if (dto.getCustomerId() != null) entity.setCustomer(subRepo.getReferenceById(dto.getCustomerId()));
        else entity.setCustomer(null);
        if (dto.getLeadId() != null) entity.setLead(leadRepo.getReferenceById(dto.getLeadId()));
        else entity.setLead(null);
        if (dto.getAssignedToId() != null) entity.setAssignedTo(userRepo.getReferenceById(dto.getAssignedToId()));
        else entity.setAssignedTo(null);
        audit(entity, false);
        return toDTO(oppRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public OpportunityDTO findOpportunityById(Long id) { return toDTO(findOpp(id)); }

    @Override
    public void deleteOpportunity(Long id) { oppRepo.delete(findOpp(id)); }

    @Override
    public OpportunityDTO updateStage(Long id, String stage, String lostReason) {
        Opportunity entity = findOpp(id);
        entity.setStage(Opportunity.OpportunityStage.valueOf(stage));
        if ("WON".equals(stage)) {
            entity.setActualCloseDate(LocalDate.now());
            entity.setProbability(BigDecimal.valueOf(100));
        } else if ("LOST".equals(stage)) {
            entity.setActualCloseDate(LocalDate.now());
            entity.setProbability(BigDecimal.ZERO);
            entity.setLostReason(lostReason);
        }
        audit(entity, false);
        return toDTO(oppRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse opportunityDatatable(int draw, int start, int length, String search, String stage) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND o.organization_id = " + orgId : "")
            + (stage != null && !stage.isBlank() ? " AND o.stage = '" + stage + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "o.opportunity_no", "o.title", "s.sub_account_code", "s.sub_account_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY o.id DESC)                         AS sl,
                COUNT(*)     OVER ()                                            AS full_count,
                o.id, o.opportunity_no, o.title, o.stage,
                COALESCE(s.sub_account_code || ' — ' || s.sub_account_name,'—') AS customer_name,
                COALESCE(o.estimated_value::text,'—')                           AS estimated_value,
                o.probability, o.currency,
                COALESCE(TO_CHAR(o.expected_close_date,'DD-Mon-YYYY'),'—')     AS expected_close_date,
                COALESCE(u.full_name, u.username,'—')                           AS assigned_to,
                TO_CHAR(o.created_at,'DD-Mon-YYYY')                             AS created_at,
                CASE o.stage
                    WHEN 'PROSPECT'     THEN '<span class="badge bg-secondary">Prospect</span>'
                    WHEN 'QUALIFIED'    THEN '<span class="badge bg-info text-dark">Qualified</span>'
                    WHEN 'PROPOSAL'     THEN '<span class="badge bg-primary">Proposal</span>'
                    WHEN 'NEGOTIATION'  THEN '<span class="badge bg-warning text-dark">Negotiation</span>'
                    WHEN 'WON'          THEN '<span class="badge bg-success">Won</span>'
                    WHEN 'LOST'         THEN '<span class="badge bg-danger">Lost</span>'
                    ELSE '<span class="badge bg-light text-dark">' || o.stage || '</span>'
                END AS stage_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="oppShow('     || o.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="oppEdit('     || o.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="oppActivity(' || o.id || ')" class="btn btn-white btn-sm" title="Add Activity"><i class="fas fa-calendar-plus text-primary"></i></a>'
                    || CASE WHEN o.stage NOT IN ('WON','LOST') THEN
                        '<a href="javascript:;" onclick="oppWon('  || o.id || ')" class="btn btn-white btn-sm" title="Mark Won"><i class="fas fa-trophy text-success"></i></a>'
                        || '<a href="javascript:;" onclick="oppLost(' || o.id || ')" class="btn btn-white btn-sm" title="Mark Lost"><i class="fas fa-times-circle text-danger"></i></a>'
                       ELSE '' END
                    || '<a href="javascript:;" onclick="oppDelete('   || o.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                                  AS actions
            FROM crm_opportunities o
            LEFT JOIN acc_chart_of_accounts_sub s ON s.id = o.customer_id
            LEFT JOIN sec_users u ON u.id = o.assigned_to_id
            %s
            ORDER BY o.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.getFirst().get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public List<Map<String, Object>> pipelineSummary() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String sql = """
            SELECT stage,
                   COUNT(*)                      AS count,
                   COALESCE(SUM(estimated_value),0) AS total_value,
                   COALESCE(SUM(estimated_value * probability / 100),0) AS weighted_value
            FROM crm_opportunities
            WHERE 1=1
            """ + (orgId != null ? " AND organization_id = " + orgId : "")
            + " GROUP BY stage ORDER BY CASE stage WHEN 'PROSPECT' THEN 1 WHEN 'QUALIFIED' THEN 2 WHEN 'PROPOSAL' THEN 3 WHEN 'NEGOTIATION' THEN 4 WHEN 'WON' THEN 5 WHEN 'LOST' THEN 6 ELSE 7 END";
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public OpportunityDTO toDTO(Opportunity e) {
        OpportunityDTO d = OpportunityDTO.builder()
            .id(e.getId()).opportunityNo(e.getOpportunityNo())
            .title(e.getTitle()).description(e.getDescription())
            .opportunityStage(e.getStage() != null ? e.getStage().name() : "PROSPECT")
            .probability(e.getProbability()).estimatedValue(e.getEstimatedValue())
            .currency(e.getCurrency())
            .expectedCloseDate(e.getExpectedCloseDate()).actualCloseDate(e.getActualCloseDate())
            .lostReason(e.getLostReason()).remarks(e.getRemarks())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getCustomer() != null) { d.setCustomerId(e.getCustomer().getId()); d.setCustomerDisplay(e.getCustomer().getSubAccountCode() + " — " + e.getCustomer().getSubAccountName()); }
        if (e.getLead() != null)     { d.setLeadId(e.getLead().getId()); d.setLeadDisplay(e.getLead().getLeadNo() + " — " + e.getLead().getContactName()); }
        if (e.getAssignedTo() != null) { d.setAssignedToId(e.getAssignedTo().getId()); d.setAssignedToDisplay(e.getAssignedTo().getFullName() != null ? e.getAssignedTo().getFullName() : e.getAssignedTo().getUsername()); }
        return d;
    }

    // =========================================================================
    // CONTACTS
    // =========================================================================

    @Override
    public ContactDTO createContact(ContactDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        Contact entity = Contact.builder()
            .firstName(dto.getFirstName().trim())
            .lastName(dto.getLastName())
            .designation(dto.getDesignation())
            .department(dto.getDepartment())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .mobile(dto.getMobile())
            .whatsapp(dto.getWhatsapp())
            .isPrimary(Boolean.TRUE.equals(dto.getPrimary()))
            .isActive(dto.getActive() == null || dto.getActive())
            .notes(dto.getNotes())
            .build();
        entity.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getCustomerId() != null) entity.setCustomer(subRepo.getReferenceById(dto.getCustomerId()));
        if (Boolean.TRUE.equals(dto.getPrimary())) clearOtherPrimary(dto.getCustomerId(), null);
        audit(entity, true);
        return toDTO(contactRepo.save(entity));
    }

    @Override
    public ContactDTO updateContact(Long id, ContactDTO dto) {
        Contact entity = findContact(id);
        entity.setFirstName(dto.getFirstName().trim());
        entity.setLastName(dto.getLastName());
        entity.setDesignation(dto.getDesignation());
        entity.setDepartment(dto.getDepartment());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setMobile(dto.getMobile());
        entity.setWhatsapp(dto.getWhatsapp());
        entity.setPrimary(Boolean.TRUE.equals(dto.getPrimary()));
        entity.setNotes(dto.getNotes());
        if (dto.getCustomerId() != null) entity.setCustomer(subRepo.getReferenceById(dto.getCustomerId()));
        if (Boolean.TRUE.equals(dto.getPrimary())) clearOtherPrimary(dto.getCustomerId(), id);
        audit(entity, false);
        return toDTO(contactRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public ContactDTO findContactById(Long id) { return toDTO(findContact(id)); }

    @Override
    public void deleteContact(Long id) { contactRepo.delete(findContact(id)); }

    @Override
    public ContactDTO toggleContact(Long id) {
        Contact entity = findContact(id);
        entity.setActive(!entity.isActive());
        return toDTO(contactRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse contactDatatable(int draw, int start, int length, String search, Long customerId) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND c.organization_id = " + orgId : "")
            + (customerId != null ? " AND c.customer_id = " + customerId : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "c.first_name", "c.last_name", "c.email", "c.phone",
                "c.designation", "s.sub_account_name", "s.sub_account_code"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY c.id DESC)                           AS sl,
                COUNT(*)     OVER ()                                              AS full_count,
                c.id,
                c.first_name || COALESCE(' ' || c.last_name,'')                  AS full_name,
                COALESCE(s.sub_account_code || ' — ' || s.sub_account_name,'—')  AS customer_name,
                COALESCE(c.designation,'—')  AS designation,
                COALESCE(c.department,'—')   AS department,
                COALESCE(c.email,'—')        AS email,
                COALESCE(c.phone, c.mobile,'—') AS phone,
                COALESCE(c.whatsapp,'—')     AS whatsapp,
                c.is_primary, c.is_active,
                CASE WHEN c.is_primary THEN '<span class="badge bg-primary">Primary</span>' ELSE '' END AS primary_badge,
                CASE WHEN c.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-secondary">Inactive</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ctShow('   || c.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="ctEdit('   || c.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="ctToggle(' || c.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="ctDelete(' || c.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                                   AS actions
            FROM crm_contacts c
            LEFT JOIN acc_chart_of_accounts_sub s ON s.id = c.customer_id
            %s
            ORDER BY c.is_primary DESC, c.first_name
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public ContactDTO toDTO(Contact e) {
        ContactDTO d = ContactDTO.builder()
            .id(e.getId())
            .firstName(e.getFirstName()).lastName(e.getLastName())
            .designation(e.getDesignation()).department(e.getDepartment())
            .email(e.getEmail()).phone(e.getPhone()).mobile(e.getMobile()).whatsapp(e.getWhatsapp())
            .primary(e.isPrimary()).active(e.isActive())
            .notes(e.getNotes())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getCustomer() != null) { d.setCustomerId(e.getCustomer().getId()); d.setCustomerDisplay(e.getCustomer().getSubAccountCode() + " — " + e.getCustomer().getSubAccountName()); }
        return d;
    }

    // =========================================================================
    // ACTIVITIES
    // =========================================================================

    @Override
    public CrmActivityDTO createActivity(CrmActivityDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        CrmActivity entity = CrmActivity.builder()
            .activityType(CrmActivity.ActivityType.valueOf(dto.getActivityType()))
            .subject(dto.getSubject().trim())
            .description(dto.getDescription())
            .activityDate(dto.getActivityDate())
            .durationMinutes(dto.getDurationMinutes())
            .outcome(dto.getOutcome())
            .nextAction(dto.getNextAction())
            .nextActionDate(dto.getNextActionDate())
            .status(dto.getStatus() != null ? CrmActivity.ActivityStatus.valueOf(dto.getStatus()) : CrmActivity.ActivityStatus.PLANNED)
            .build();
        entity.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getOpportunityId() != null) entity.setOpportunity(oppRepo.getReferenceById(dto.getOpportunityId()));
        if (dto.getLeadId()        != null) entity.setLead(leadRepo.getReferenceById(dto.getLeadId()));
        if (dto.getCustomerId()    != null) entity.setCustomer(subRepo.getReferenceById(dto.getCustomerId()));
        if (dto.getAssignedToId()  != null) entity.setAssignedTo(userRepo.getReferenceById(dto.getAssignedToId()));
        audit(entity, true);
        return toDTO(actRepo.save(entity));
    }

    @Override
    public CrmActivityDTO updateActivity(Long id, CrmActivityDTO dto) {
        CrmActivity entity = findAct(id);
        entity.setActivityType(CrmActivity.ActivityType.valueOf(dto.getActivityType()));
        entity.setSubject(dto.getSubject().trim());
        entity.setDescription(dto.getDescription());
        entity.setActivityDate(dto.getActivityDate());
        entity.setDurationMinutes(dto.getDurationMinutes());
        entity.setNextAction(dto.getNextAction());
        entity.setNextActionDate(dto.getNextActionDate());
        if (dto.getOpportunityId() != null) entity.setOpportunity(oppRepo.getReferenceById(dto.getOpportunityId()));
        if (dto.getAssignedToId()  != null) entity.setAssignedTo(userRepo.getReferenceById(dto.getAssignedToId()));
        audit(entity, false);
        return toDTO(actRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public CrmActivityDTO findActivityById(Long id) { return toDTO(findAct(id)); }

    @Override
    public void deleteActivity(Long id) { actRepo.delete(findAct(id)); }

    @Override
    public CrmActivityDTO completeActivity(Long id, String outcome, String nextAction, LocalDate nextActionDate) {
        CrmActivity entity = findAct(id);
        entity.setStatus(CrmActivity.ActivityStatus.COMPLETED);
        entity.setOutcome(outcome);
        entity.setNextAction(nextAction);
        entity.setNextActionDate(nextActionDate);
        audit(entity, false);
        return toDTO(actRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse activityDatatable(int draw, int start, int length, String search, String type, String status) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND a.organization_id = " + orgId : "")
            + (type   != null && !type.isBlank()   ? " AND a.activity_type = '" + type   + "'" : "")
            + (status != null && !status.isBlank() ? " AND a.status = '"        + status + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList("a.subject", "a.description", "s.sub_account_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY a.activity_date DESC, a.id DESC) AS sl,
                COUNT(*)     OVER ()                                          AS full_count,
                a.id, a.activity_type, a.subject, a.status,
                TO_CHAR(a.activity_date,'DD-Mon-YYYY')   AS activity_date,
                COALESCE(TO_CHAR(a.next_action_date,'DD-Mon-YYYY'),'—') AS next_action_date,
                COALESCE(a.outcome,'—')     AS outcome,
                COALESCE(a.next_action,'—') AS next_action,
                COALESCE(s.sub_account_name,l.contact_name,'—') AS related_to,
                COALESCE(u.full_name, u.username,'—')     AS assigned_to,
                TO_CHAR(a.created_at,'DD-Mon-YYYY')       AS created_at,
                a.duration_minutes,
                CASE a.status
                    WHEN 'PLANNED'   THEN '<span class="badge bg-info text-dark">Planned</span>'
                    WHEN 'COMPLETED' THEN '<span class="badge bg-success">Completed</span>'
                    WHEN 'CANCELLED' THEN '<span class="badge bg-secondary">Cancelled</span>'
                    ELSE '<span class="badge bg-light text-dark">' || a.status || '</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="actShow('     || a.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="actEdit('     || a.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || CASE WHEN a.status = 'PLANNED' THEN
                        '<a href="javascript:;" onclick="actComplete(' || a.id || ')" class="btn btn-white btn-sm" title="Complete"><i class="fas fa-check-circle text-primary"></i></a>'
                       ELSE '' END
                    || '<a href="javascript:;" onclick="actDelete('   || a.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                        AS actions
            FROM  crm_activities a
            LEFT  JOIN acc_chart_of_accounts_sub s ON s.id = a.customer_id
            LEFT  JOIN crm_leads l                 ON l.id = a.lead_id
            LEFT  JOIN sec_users u                     ON u.id = a.assigned_to_id
            %s
            ORDER BY a.activity_date DESC, a.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.getFirst().get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public CrmActivityDTO toDTO(CrmActivity e) {
        CrmActivityDTO d = CrmActivityDTO.builder()
            .id(e.getId())
            .activityType(e.getActivityType() != null ? e.getActivityType().name() : null)
            .subject(e.getSubject()).description(e.getDescription())
            .activityDate(e.getActivityDate()).durationMinutes(e.getDurationMinutes())
            .outcome(e.getOutcome()).nextAction(e.getNextAction()).nextActionDate(e.getNextActionDate())
            .status(e.getStatus() != null ? e.getStatus().name() : "PLANNED")
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getOpportunity() != null) { d.setOpportunityId(e.getOpportunity().getId()); d.setOpportunityDisplay(e.getOpportunity().getOpportunityNo() + " — " + e.getOpportunity().getTitle()); }
        if (e.getLead()        != null) { d.setLeadId(e.getLead().getId()); d.setLeadDisplay(e.getLead().getLeadNo() + " — " + e.getLead().getContactName()); }
        if (e.getCustomer()    != null) { d.setCustomerId(e.getCustomer().getId()); d.setCustomerDisplay(e.getCustomer().getSubAccountCode() + " — " + e.getCustomer().getSubAccountName()); }
        if (e.getAssignedTo()  != null) { d.setAssignedToId(e.getAssignedTo().getId()); d.setAssignedToDisplay(e.getAssignedTo().getFullName() != null ? e.getAssignedTo().getFullName() : e.getAssignedTo().getUsername()); }
        return d;
    }

    // =========================================================================
    // CUSTOMER FEEDBACK
    // =========================================================================

    @Override
    public CustomerFeedbackDTO createFeedback(CustomerFeedbackDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        CustomerFeedback entity = CustomerFeedback.builder()
            .feedbackDate(dto.getFeedbackDate() != null ? dto.getFeedbackDate() : LocalDate.now())
            .feedbackType(dto.getFeedbackType() != null ? dto.getFeedbackType() : "GENERAL")
            .rating(dto.getRating())
            .subject(dto.getSubject())
            .description(dto.getDescription())
            .status(CustomerFeedback.FeedbackStatus.OPEN)
            .build();
        entity.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getCustomerId() != null) entity.setCustomer(subRepo.getReferenceById(dto.getCustomerId()));
        audit(entity, true);
        return toDTO(feedRepo.save(entity));
    }

    @Override
    public CustomerFeedbackDTO updateFeedback(Long id, CustomerFeedbackDTO dto) {
        CustomerFeedback entity = findFeed(id);
        entity.setFeedbackDate(dto.getFeedbackDate() != null ? dto.getFeedbackDate() : entity.getFeedbackDate());
        entity.setFeedbackType(dto.getFeedbackType() != null ? dto.getFeedbackType() : "GENERAL");
        entity.setRating(dto.getRating());
        entity.setSubject(dto.getSubject());
        entity.setDescription(dto.getDescription());
        if (dto.getCustomerId() != null) entity.setCustomer(subRepo.getReferenceById(dto.getCustomerId()));
        audit(entity, false);
        return toDTO(feedRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public CustomerFeedbackDTO findFeedbackById(Long id) { return toDTO(findFeed(id)); }

    @Override
    public void deleteFeedback(Long id) {
        CustomerFeedback entity = findFeed(id);
        if (entity.getStatus() != CustomerFeedback.FeedbackStatus.OPEN)
            throw new IllegalStateException("Only OPEN feedback can be deleted.");
        feedRepo.delete(entity);
    }

    @Override
    public CustomerFeedbackDTO resolveFeedback(Long id, String resolution) {
        CustomerFeedback entity = findFeed(id);
        entity.setResolution(resolution);
        entity.setResolvedBy(ContextProvider.getCurrentUsername());
        entity.setResolvedAt(LocalDateTime.now());
        entity.setStatus(CustomerFeedback.FeedbackStatus.RESOLVED);
        audit(entity, false);
        return toDTO(feedRepo.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse feedbackDatatable(int draw, int start, int length, String search, String status, String type) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND f.organization_id = " + orgId : "")
            + (status != null && !status.isBlank() ? " AND f.status = '"        + status + "'" : "")
            + (type   != null && !type.isBlank()   ? " AND f.feedback_type = '" + type   + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "f.subject", "f.description", "s.sub_account_code", "s.sub_account_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY f.id DESC)                            AS sl,
                COUNT(*)     OVER ()                                               AS full_count,
                f.id, f.feedback_type,
                COALESCE(s.sub_account_code || ' — ' || s.sub_account_name,'—')   AS customer_name,
                TO_CHAR(f.feedback_date,'DD-Mon-YYYY')                             AS feedback_date,
                f.rating, COALESCE(f.subject,'—') AS subject,
                f.status,
                COALESCE(f.resolved_by,'—')                                        AS resolved_by,
                COALESCE(TO_CHAR(f.resolved_at,'DD-Mon-YYYY'),'—')                AS resolved_at,
                TO_CHAR(f.created_at,'DD-Mon-YYYY')                                AS created_at,
                CASE f.status
                    WHEN 'OPEN'        THEN '<span class="badge bg-danger">Open</span>'
                    WHEN 'IN_PROGRESS' THEN '<span class="badge bg-warning text-dark">In Progress</span>'
                    WHEN 'RESOLVED'    THEN '<span class="badge bg-success">Resolved</span>'
                    WHEN 'CLOSED'      THEN '<span class="badge bg-secondary">Closed</span>'
                    ELSE '<span class="badge bg-light text-dark">' || f.status || '</span>'
                END AS status_badge,
                CASE f.rating
                    WHEN 5 THEN '★★★★★'
                    WHEN 4 THEN '★★★★☆'
                    WHEN 3 THEN '★★★☆☆'
                    WHEN 2 THEN '★★☆☆☆'
                    WHEN 1 THEN '★☆☆☆☆'
                    ELSE '—'
                END AS rating_stars,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="fbShow('    || f.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="fbEdit('    || f.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || CASE WHEN f.status IN ('OPEN','IN_PROGRESS') THEN
                        '<a href="javascript:;" onclick="fbResolve(' || f.id || ')" class="btn btn-white btn-sm" title="Resolve"><i class="fas fa-check-circle text-primary"></i></a>'
                       ELSE '' END
                    || '<a href="javascript:;" onclick="fbDelete('  || f.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                       AS actions
            FROM  crm_customer_feedback f
            JOIN  acc_chart_of_accounts_sub s ON s.id = f.customer_id
            %s
            ORDER BY f.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public CustomerFeedbackDTO toDTO(CustomerFeedback e) {
        CustomerFeedbackDTO d = CustomerFeedbackDTO.builder()
            .id(e.getId())
            .feedbackDate(e.getFeedbackDate())
            .feedbackType(e.getFeedbackType())
            .rating(e.getRating())
            .subject(e.getSubject())
            .description(e.getDescription())
            .resolution(e.getResolution())
            .resolvedBy(e.getResolvedBy())
            .resolvedAt(e.getResolvedAt() != null ? e.getResolvedAt().toString() : null)
            .status(e.getStatus() != null ? e.getStatus().name() : "OPEN")
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getCustomer() != null) { d.setCustomerId(e.getCustomer().getId()); d.setCustomerDisplay(e.getCustomer().getSubAccountCode() + " — " + e.getCustomer().getSubAccountName()); }
        return d;
    }

    // =========================================================================
    // DASHBOARD SUMMARY
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String orgFilter = orgId != null ? " AND organization_id = " + orgId : "";
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openLeads",       jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_leads WHERE status NOT IN ('CONVERTED','LOST')" + orgFilter, Long.class));
        summary.put("openOpportunities",jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_opportunities WHERE stage NOT IN ('WON','LOST')" + orgFilter, Long.class));
        summary.put("openFeedback",    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_customer_feedback WHERE status IN ('OPEN','IN_PROGRESS')" + orgFilter, Long.class));
        summary.put("pendingActivities",jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_activities WHERE status = 'PLANNED'" + orgFilter, Long.class));
        summary.put("pipelineValue",   jdbcTemplate.queryForObject("SELECT COALESCE(SUM(estimated_value * probability / 100),0) FROM crm_opportunities WHERE stage NOT IN ('WON','LOST')" + orgFilter, BigDecimal.class));
        return summary;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Lead          findLead(Long id)    { return leadRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Lead #" + id + " not found.")); }
    private Opportunity   findOpp(Long id)     { return oppRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Opportunity #" + id + " not found.")); }
    private Contact       findContact(Long id) { return contactRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Contact #" + id + " not found.")); }
    private CrmActivity   findAct(Long id)     { return actRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Activity #" + id + " not found.")); }
    private CustomerFeedback findFeed(Long id) { return feedRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Feedback #" + id + " not found.")); }

    private void clearOtherPrimary(Long customerId, Long excludeId) {
        if (customerId == null) return;
        contactRepo.findByCustomerIdAndIsPrimaryTrue(customerId).ifPresent(c -> {
            if (!c.getId().equals(excludeId)) { c.setPrimary(false); contactRepo.save(c); }
        });
    }

    private void audit(Object entity, boolean isCreate) {
        String user = SecurityHelper.currentUsername().orElse("system");
        // BaseEntity audit fields set via @PrePersist / @PreUpdate in most entities;
        // for entities without it we set manually
        if (entity instanceof Lead e) {
            if (isCreate) { e.setCreatedBy(user); e.setCreatedAt(LocalDateTime.now()); }
            e.setUpdatedBy(user); e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof Opportunity e) {
            if (isCreate) { e.setCreatedBy(user); e.setCreatedAt(LocalDateTime.now()); }
            e.setUpdatedBy(user); e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof Contact e) {
            if (isCreate) { e.setCreatedBy(user); e.setCreatedAt(LocalDateTime.now()); }
            e.setUpdatedBy(user); e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof CrmActivity e) {
            if (isCreate) { e.setCreatedBy(user); e.setCreatedAt(LocalDateTime.now()); }
            e.setUpdatedBy(user); e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof CustomerFeedback e) {
            if (isCreate) { e.setCreatedBy(user); e.setCreatedAt(LocalDateTime.now()); }
            e.setUpdatedBy(user); e.setUpdatedAt(LocalDateTime.now());
        }
    }
}
