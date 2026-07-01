package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvHotelDTO;
import com.asg.spindleserp.travel.dto.TrvMasterDataDTO;
import com.asg.spindleserp.travel.dto.TrvRoomTypeDTO;

import java.util.List;
import java.util.Map;

/**
 * TravelMasterService — hotels, room types, and the six small lookup tables
 * (categories, meal plans, airlines, airports, cabin classes, room facilities).
 * Consolidated in one service/controller pair, same pattern as CrmService
 * handling leads/opportunities/contacts/activities/feedback together.
 */
public interface TravelMasterService {

    // ── Hotels ────────────────────────────────────────────────────────────────
    TrvHotelDTO saveHotel(TrvHotelDTO dto);
    TrvHotelDTO findHotelById(Long id);
    void deleteHotel(Long id);
    List<Map<String, Object>> listHotels(String search);
    List<Map<String, Object>> searchHotels(String search); // for Select2

    // ── Room Types (nested under a hotel) ───────────────────────────────────
    TrvRoomTypeDTO saveRoomType(TrvRoomTypeDTO dto);
    void deleteRoomType(Long id);
    List<Map<String, Object>> listRoomTypes(Long hotelId);
    List<Map<String, Object>> searchRoomTypes(Long hotelId);

    // ── Room Facilities ──────────────────────────────────────────────────────
    TrvMasterDataDTO.RoomFacilityDTO saveFacility(TrvMasterDataDTO.RoomFacilityDTO dto);
    void deleteFacility(Long id);
    List<Map<String, Object>> listFacilities(Long roomTypeId);

    // ── Hotel Categories ─────────────────────────────────────────────────────
    TrvMasterDataDTO.HotelCategoryDTO saveHotelCategory(TrvMasterDataDTO.HotelCategoryDTO dto);
    void deleteHotelCategory(Long id);
    List<Map<String, Object>> listHotelCategories();

    // ── Meal Plans ────────────────────────────────────────────────────────────
    TrvMasterDataDTO.MealPlanDTO saveMealPlan(TrvMasterDataDTO.MealPlanDTO dto);
    void deleteMealPlan(Long id);
    List<Map<String, Object>> listMealPlans();

    // ── Airlines ──────────────────────────────────────────────────────────────
    TrvMasterDataDTO.AirlineDTO saveAirline(TrvMasterDataDTO.AirlineDTO dto);
    void deleteAirline(Long id);
    List<Map<String, Object>> listAirlines();

    // ── Airports ──────────────────────────────────────────────────────────────
    TrvMasterDataDTO.AirportDTO saveAirport(TrvMasterDataDTO.AirportDTO dto);
    void deleteAirport(Long id);
    List<Map<String, Object>> listAirports();

    // ── Cabin Classes ─────────────────────────────────────────────────────────
    TrvMasterDataDTO.CabinClassDTO saveCabinClass(TrvMasterDataDTO.CabinClassDTO dto);
    void deleteCabinClass(Long id);
    List<Map<String, Object>> listCabinClasses();
}
