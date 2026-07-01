package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.TrvHotelDTO;
import com.asg.spindleserp.travel.dto.TrvMasterDataDTO;
import com.asg.spindleserp.travel.dto.TrvRoomTypeDTO;
import com.asg.spindleserp.travel.entity.*;
import com.asg.spindleserp.travel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class TravelMasterServiceImpl implements TravelMasterService {

    private final TrvHotelRepository         hotelRepo;
    private final TrvHotelCategoryRepository categoryRepo;
    private final TrvRoomTypeRepository      roomTypeRepo;
    private final TrvRoomFacilityRepository  facilityRepo;
    private final TrvMealPlanRepository      mealPlanRepo;
    private final TrvAirlineRepository       airlineRepo;
    private final TrvAirportRepository       airportRepo;
    private final TrvCabinClassRepository    cabinClassRepo;
    private final JdbcTemplate               jdbcTemplate;

    // =========================================================================
    // HOTELS
    // =========================================================================

    @Override
    public TrvHotelDTO saveHotel(TrvHotelDTO dto) {
        TrvHotel e = dto.getId() != null
            ? hotelRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Hotel #" + dto.getId() + " not found."))
            : TrvHotel.builder().organizationId(SecurityHelper.requireOrgId()).build();

        e.setHotelCode(dto.getHotelCode());
        e.setHotelName(dto.getHotelName());
        e.setCity(dto.getCity());
        e.setCountry(dto.getCountry());
        e.setAddress(dto.getAddress());
        e.setStarRating(dto.getStarRating());
        e.setContactPerson(dto.getContactPerson());
        e.setContactPhone(dto.getContactPhone());
        e.setContactEmail(dto.getContactEmail());
        e.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        e.setCategoryId(dto.getCategoryId());
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        TrvHotel saved = hotelRepo.save(e);
        return TrvHotelDTO.builder()
            .id(saved.getId()).hotelCode(saved.getHotelCode()).hotelName(saved.getHotelName())
            .city(saved.getCity()).country(saved.getCountry()).address(saved.getAddress())
            .starRating(saved.getStarRating()).contactPerson(saved.getContactPerson())
            .contactPhone(saved.getContactPhone()).contactEmail(saved.getContactEmail())
            .isActive(saved.getIsActive()).categoryId(saved.getCategoryId())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TrvHotelDTO findHotelById(Long id) {
        TrvHotel e = hotelRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Hotel #" + id + " not found."));
        String categoryName = e.getCategoryId() != null
            ? categoryRepo.findById(e.getCategoryId()).map(TrvHotelCategory::getCategoryName).orElse(null) : null;
        return TrvHotelDTO.builder()
            .id(e.getId()).hotelCode(e.getHotelCode()).hotelName(e.getHotelName())
            .city(e.getCity()).country(e.getCountry()).address(e.getAddress())
            .starRating(e.getStarRating()).contactPerson(e.getContactPerson())
            .contactPhone(e.getContactPhone()).contactEmail(e.getContactEmail())
            .isActive(e.getIsActive()).categoryId(e.getCategoryId()).categoryDisplay(categoryName)
            .build();
    }

    @Override
    public void deleteHotel(Long id) {
        if (!roomTypeRepo.findByHotelIdAndIsActiveTrue(id).isEmpty())
            throw new IllegalStateException("Cannot delete hotel with active room types. Deactivate room types first.");
        hotelRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listHotels(String search) {
        Long orgId = SecurityHelper.requireOrgId();
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT h.id,
                   ROW_NUMBER() OVER (ORDER BY h.id DESC)                           AS sl,
                   h.hotel_code, h.hotel_name, h.city, h.country, h.star_rating,
                   COALESCE(c.category_name, '—')                                   AS category_name,
                   h.contact_person, h.contact_phone, h.is_active,
                   (SELECT COUNT(*) FROM trv_room_types rt WHERE rt.hotel_id = h.id) AS room_type_count,
                   CASE WHEN h.is_active THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-secondary">Inactive</span>' END  AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="htlEdit('   || h.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="htlRooms('  || h.id || ',\\'' || replace(h.hotel_name,'''','') || '\\')" class="btn btn-white btn-sm" title="Room Types"><i class="fas fa-bed text-info"></i></a>'
                     || '<a href="javascript:;" onclick="htlDelete(' || h.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>'                                                    AS actions
            FROM   trv_hotels h
            LEFT   JOIN trv_hotel_categories c ON c.id = h.category_id
            WHERE  h.organization_id = ?
              AND  (h.hotel_name ILIKE ? OR h.hotel_code ILIKE ? OR h.city ILIKE ?)
            ORDER  BY h.id DESC
            """, orgId, like, like, like);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchHotels(String search) {
        Long orgId = SecurityHelper.requireOrgId();
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT id, hotel_code || ' — ' || hotel_name || COALESCE(' (' || city || ')', '') AS text
            FROM   trv_hotels
            WHERE  organization_id = ? AND is_active = true
              AND  (hotel_name ILIKE ? OR hotel_code ILIKE ?)
            ORDER  BY hotel_name LIMIT 20
            """, orgId, like, like);
    }

    // =========================================================================
    // ROOM TYPES
    // =========================================================================

    @Override
    public TrvRoomTypeDTO saveRoomType(TrvRoomTypeDTO dto) {
        TrvRoomType e = dto.getId() != null
            ? roomTypeRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Room type #" + dto.getId() + " not found."))
            : new TrvRoomType();
        e.setRoomTypeName(dto.getRoomTypeName());
        e.setMaxOccupancy(dto.getMaxOccupancy());
        e.setBasePrice(dto.getBasePrice());
        e.setCurrency(dto.getCurrency());
        e.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        e.setHotelId(dto.getHotelId());
        TrvRoomType saved = roomTypeRepo.save(e);
        return TrvRoomTypeDTO.builder()
            .id(saved.getId()).roomTypeName(saved.getRoomTypeName()).maxOccupancy(saved.getMaxOccupancy())
            .basePrice(saved.getBasePrice()).currency(saved.getCurrency()).isActive(saved.getIsActive())
            .hotelId(saved.getHotelId()).build();
    }

    @Override
    public void deleteRoomType(Long id) {
        facilityRepo.findByRoomTypeId(id).forEach(f -> facilityRepo.deleteById(f.getId()));
        roomTypeRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRoomTypes(Long hotelId) {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sl,
                   room_type_name, max_occupancy, base_price, currency, is_active,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="rtEdit('   || id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="rtDelete(' || id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_room_types WHERE hotel_id = ? ORDER BY id
            """, hotelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchRoomTypes(Long hotelId) {
        return jdbcTemplate.queryForList("""
            SELECT id, room_type_name || COALESCE(' — ৳' || base_price, '') AS text
            FROM   trv_room_types WHERE hotel_id = ? AND is_active = true ORDER BY room_type_name
            """, hotelId);
    }

    // =========================================================================
    // ROOM FACILITIES
    // =========================================================================

    @Override
    public TrvMasterDataDTO.RoomFacilityDTO saveFacility(TrvMasterDataDTO.RoomFacilityDTO dto) {
        TrvRoomFacility e = TrvRoomFacility.builder()
            .facilityName(dto.getFacilityName()).roomTypeId(dto.getRoomTypeId()).build();
        TrvRoomFacility saved = facilityRepo.save(e);
        return TrvMasterDataDTO.RoomFacilityDTO.builder()
            .id(saved.getId()).facilityName(saved.getFacilityName()).roomTypeId(saved.getRoomTypeId()).build();
    }

    @Override
    public void deleteFacility(Long id) { facilityRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listFacilities(Long roomTypeId) {
        return jdbcTemplate.queryForList("""
            SELECT id, facility_name FROM trv_room_facilities WHERE room_type_id = ? ORDER BY id
            """, roomTypeId);
    }

    // =========================================================================
    // HOTEL CATEGORIES
    // =========================================================================

    @Override
    public TrvMasterDataDTO.HotelCategoryDTO saveHotelCategory(TrvMasterDataDTO.HotelCategoryDTO dto) {
        TrvHotelCategory e = dto.getId() != null
            ? categoryRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Category #" + dto.getId() + " not found."))
            : new TrvHotelCategory();
        e.setCategoryName(dto.getCategoryName());
        e.setDescription(dto.getDescription());
        TrvHotelCategory saved = categoryRepo.save(e);
        return TrvMasterDataDTO.HotelCategoryDTO.builder()
            .id(saved.getId()).categoryName(saved.getCategoryName()).description(saved.getDescription()).build();
    }

    @Override
    public void deleteHotelCategory(Long id) { categoryRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listHotelCategories() {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sl, category_name, COALESCE(description,'—') AS description,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="hcatEdit('   || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="hcatDelete(' || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM trv_hotel_categories ORDER BY id
            """);
    }

    // =========================================================================
    // MEAL PLANS
    // =========================================================================

    @Override
    public TrvMasterDataDTO.MealPlanDTO saveMealPlan(TrvMasterDataDTO.MealPlanDTO dto) {
        TrvMealPlan e = dto.getId() != null
            ? mealPlanRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Meal plan #" + dto.getId() + " not found."))
            : new TrvMealPlan();
        e.setPlanCode(dto.getPlanCode());
        e.setPlanName(dto.getPlanName());
        e.setDescription(dto.getDescription());
        TrvMealPlan saved = mealPlanRepo.save(e);
        return TrvMasterDataDTO.MealPlanDTO.builder()
            .id(saved.getId()).planCode(saved.getPlanCode()).planName(saved.getPlanName())
            .description(saved.getDescription()).build();
    }

    @Override
    public void deleteMealPlan(Long id) { mealPlanRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMealPlans() {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sl, plan_code, plan_name, COALESCE(description,'—') AS description,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="mpEdit('   || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="mpDelete(' || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM trv_meal_plans ORDER BY id
            """);
    }

    // =========================================================================
    // AIRLINES
    // =========================================================================

    @Override
    public TrvMasterDataDTO.AirlineDTO saveAirline(TrvMasterDataDTO.AirlineDTO dto) {
        TrvAirline e = dto.getId() != null
            ? airlineRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Airline #" + dto.getId() + " not found."))
            : new TrvAirline();
        e.setAirlineCode(dto.getAirlineCode());
        e.setAirlineName(dto.getAirlineName());
        e.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        TrvAirline saved = airlineRepo.save(e);
        return TrvMasterDataDTO.AirlineDTO.builder()
            .id(saved.getId()).airlineCode(saved.getAirlineCode()).airlineName(saved.getAirlineName())
            .isActive(saved.getIsActive()).build();
    }

    @Override
    public void deleteAirline(Long id) { airlineRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAirlines() {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sl, airline_code, airline_name, is_active,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="alEdit('   || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="alDelete(' || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM trv_airlines ORDER BY id
            """);
    }

    // =========================================================================
    // AIRPORTS
    // =========================================================================

    @Override
    public TrvMasterDataDTO.AirportDTO saveAirport(TrvMasterDataDTO.AirportDTO dto) {
        TrvAirport e = dto.getId() != null
            ? airportRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Airport #" + dto.getId() + " not found."))
            : new TrvAirport();
        e.setAirportCode(dto.getAirportCode());
        e.setAirportName(dto.getAirportName());
        e.setCity(dto.getCity());
        e.setCountry(dto.getCountry());
        TrvAirport saved = airportRepo.save(e);
        return TrvMasterDataDTO.AirportDTO.builder()
            .id(saved.getId()).airportCode(saved.getAirportCode()).airportName(saved.getAirportName())
            .city(saved.getCity()).country(saved.getCountry()).build();
    }

    @Override
    public void deleteAirport(Long id) { airportRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAirports() {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sl, airport_code, airport_name,
                   COALESCE(city,'—') AS city, COALESCE(country,'—') AS country,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="apEdit('   || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="apDelete(' || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM trv_airports ORDER BY id
            """);
    }

    // =========================================================================
    // CABIN CLASSES
    // =========================================================================

    @Override
    public TrvMasterDataDTO.CabinClassDTO saveCabinClass(TrvMasterDataDTO.CabinClassDTO dto) {
        TrvCabinClass e = dto.getId() != null
            ? cabinClassRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Cabin class #" + dto.getId() + " not found."))
            : new TrvCabinClass();
        e.setClassCode(dto.getClassCode());
        e.setClassName(dto.getClassName());
        TrvCabinClass saved = cabinClassRepo.save(e);
        return TrvMasterDataDTO.CabinClassDTO.builder()
            .id(saved.getId()).classCode(saved.getClassCode()).className(saved.getClassName()).build();
    }

    @Override
    public void deleteCabinClass(Long id) { cabinClassRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCabinClasses() {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sl, class_code, class_name,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="ccEdit('   || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="ccDelete(' || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM trv_cabin_classes ORDER BY id
            """);
    }
}
