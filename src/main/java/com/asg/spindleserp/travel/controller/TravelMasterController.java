package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvHotelDTO;
import com.asg.spindleserp.travel.dto.TrvMasterDataDTO;
import com.asg.spindleserp.travel.dto.TrvRoomTypeDTO;
import com.asg.spindleserp.travel.service.TravelMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TravelMasterController — hotels + room types + the six small lookup tables,
 * one controller for all of them (same pattern as CrmController).
 *
 * Pages:  GET /travel/hotels   → travel-hotels.html
 *         GET /travel/masters  → travel-masters.html (tabbed lookups)
 *
 * JS prefixes: htl* (hotel), rt* (room type), fac* (facility),
 *              hcat* (hotel category), mp* (meal plan), al* (airline),
 *              ap* (airport), cc* (cabin class)
 */
@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelMasterController {

    private final TravelMasterService masterService;

    // ── PAGES ─────────────────────────────────────────────────────────────────

    @GetMapping("/hotels")
    public String hotelsPage(Model model) {
        model.addAttribute("activePage", "travel-hotels");
        model.addAttribute("pageTitle",  "Hotels");
        return "travel/travel-hotels";
    }

    @GetMapping("/masters")
    public String mastersPage(Model model) {
        model.addAttribute("activePage", "travel-masters");
        model.addAttribute("pageTitle",  "Travel Master Data");
        return "travel/travel-masters";
    }

    // ── HOTELS ────────────────────────────────────────────────────────────────

    @GetMapping("/hotels/list")
    @ResponseBody
    public Map<String, Object> hotelList(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", masterService.listHotels(search));
    }

    @GetMapping("/hotels/search")
    @ResponseBody
    public Map<String, Object> hotelSearch(@RequestParam(defaultValue = "") String search) {
        return Map.of("items", masterService.searchHotels(search));
    }

    @GetMapping("/hotels/show/{id}")
    @ResponseBody
    public Map<String, Object> hotelShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", masterService.findHotelById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/hotels/save")
    @ResponseBody
    public Map<String, Object> hotelSave(@RequestBody @Valid TrvHotelDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvHotelDTO saved = masterService.saveHotel(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Hotel updated." : "Hotel created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/hotels/delete/{id}")
    @ResponseBody
    public Map<String, Object> hotelDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.deleteHotel(id); res.put("success", true); res.put("message", "Hotel deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── ROOM TYPES ────────────────────────────────────────────────────────────

    @GetMapping("/hotels/room-types/list")
    @ResponseBody
    public Map<String, Object> roomTypeList(@RequestParam Long hotelId) {
        return Map.of("data", masterService.listRoomTypes(hotelId));
    }

    @GetMapping("/hotels/room-types/search")
    @ResponseBody
    public Map<String, Object> roomTypeSearch(@RequestParam Long hotelId) {
        return Map.of("items", masterService.searchRoomTypes(hotelId));
    }

    @PostMapping("/hotels/room-types/save")
    @ResponseBody
    public Map<String, Object> roomTypeSave(@RequestBody @Valid TrvRoomTypeDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvRoomTypeDTO saved = masterService.saveRoomType(dto);
            res.put("success", true); res.put("id", saved.getId()); res.put("message", "Room type saved.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/hotels/room-types/delete/{id}")
    @ResponseBody
    public Map<String, Object> roomTypeDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.deleteRoomType(id); res.put("success", true); res.put("message", "Room type deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── HOTEL CATEGORIES ──────────────────────────────────────────────────────

    @GetMapping("/masters/hotel-categories/list")
    @ResponseBody
    public Map<String, Object> hotelCategoryList() { return Map.of("data", masterService.listHotelCategories()); }

    @PostMapping("/masters/hotel-categories/save")
    @ResponseBody
    public Map<String, Object> hotelCategorySave(@RequestBody @Valid TrvMasterDataDTO.HotelCategoryDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.saveHotelCategory(dto); res.put("success", true); res.put("message", "Saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/masters/hotel-categories/delete/{id}")
    @ResponseBody
    public Map<String, Object> hotelCategoryDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.deleteHotelCategory(id); res.put("success", true); res.put("message", "Deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── MEAL PLANS ────────────────────────────────────────────────────────────

    @GetMapping("/masters/meal-plans/list")
    @ResponseBody
    public Map<String, Object> mealPlanList() { return Map.of("data", masterService.listMealPlans()); }

    @PostMapping("/masters/meal-plans/save")
    @ResponseBody
    public Map<String, Object> mealPlanSave(@RequestBody @Valid TrvMasterDataDTO.MealPlanDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.saveMealPlan(dto); res.put("success", true); res.put("message", "Saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/masters/meal-plans/delete/{id}")
    @ResponseBody
    public Map<String, Object> mealPlanDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.deleteMealPlan(id); res.put("success", true); res.put("message", "Deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AIRLINES ──────────────────────────────────────────────────────────────

    @GetMapping("/masters/airlines/list")
    @ResponseBody
    public Map<String, Object> airlineList() { return Map.of("data", masterService.listAirlines()); }

    @PostMapping("/masters/airlines/save")
    @ResponseBody
    public Map<String, Object> airlineSave(@RequestBody @Valid TrvMasterDataDTO.AirlineDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.saveAirline(dto); res.put("success", true); res.put("message", "Saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/masters/airlines/delete/{id}")
    @ResponseBody
    public Map<String, Object> airlineDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.deleteAirline(id); res.put("success", true); res.put("message", "Deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AIRPORTS ──────────────────────────────────────────────────────────────

    @GetMapping("/masters/airports/list")
    @ResponseBody
    public Map<String, Object> airportList() { return Map.of("data", masterService.listAirports()); }

    @PostMapping("/masters/airports/save")
    @ResponseBody
    public Map<String, Object> airportSave(@RequestBody @Valid TrvMasterDataDTO.AirportDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.saveAirport(dto); res.put("success", true); res.put("message", "Saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/masters/airports/delete/{id}")
    @ResponseBody
    public Map<String, Object> airportDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.deleteAirport(id); res.put("success", true); res.put("message", "Deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── CABIN CLASSES ─────────────────────────────────────────────────────────

    @GetMapping("/masters/cabin-classes/list")
    @ResponseBody
    public Map<String, Object> cabinClassList() { return Map.of("data", masterService.listCabinClasses()); }

    @PostMapping("/masters/cabin-classes/save")
    @ResponseBody
    public Map<String, Object> cabinClassSave(@RequestBody @Valid TrvMasterDataDTO.CabinClassDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.saveCabinClass(dto); res.put("success", true); res.put("message", "Saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/masters/cabin-classes/delete/{id}")
    @ResponseBody
    public Map<String, Object> cabinClassDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { masterService.deleteCabinClass(id); res.put("success", true); res.put("message", "Deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
