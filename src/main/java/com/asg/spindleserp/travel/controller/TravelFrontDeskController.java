// Path: com/asg/spindleserp/travel/controller/TravelFrontDeskController.java
package com.asg.spindleserp.travel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * TravelFrontDeskController — the internal walk-in "front desk" console.
 *
 * A single quick-booking screen for agents handling a walk-in customer:
 * pick a customer, add one or more service lines (Hotel / Air / Package /
 * Tour / Visa), see a running quote, then Save as Draft or Save & Confirm —
 * a speed-optimized front end over the same booking cycle as /travel/bookings.
 *
 * Backend-free: every action calls REST endpoints that already exist —
 *   Customer  GET  /accounts/sub-accounts/search?subAccountType=CUSTOMER
 *   Hotel     GET  /travel/hotels/search
 *   Package   GET  /travel/packages/search
 *   Tour      GET  /travel/tours/search
 *   Save      POST /travel/bookings/save        → { success, id, bookingNo, message }
 *   Confirm   POST /travel/bookings/confirm/{id}
 *   Feed      GET  /travel/bookings/list        (DataTable → { data:[...] })
 *
 * Notes on the wiring (verified against the travel model):
 *   • partyId on TrvBookingDTO is a CUSTOMER sub-account id
 *     (acc_chart_of_accounts_sub), resolved via the shared
 *     /accounts/sub-accounts/search Select2 — not a raw CRM party id.
 *   • TrvBookingService.ServiceType supports all five values
 *     {HOTEL, AIR, PACKAGE, TOUR, VISA}; the JS derives the header
 *     bookingType {PACKAGE|HOTEL|AIR|COMBINED} from the line mix.
 *   • Air/Visa are description-only at the counter (referenceId stays null);
 *     detailed ticketing / visa fulfilment happens on their own ops screens
 *     after the booking is confirmed.
 *   • Confirm requires a party server-side, so walk-in guests (name/phone
 *     folded into remarks) can Save as Draft but not Save & Confirm until a
 *     registered customer is attached.
 */
@Controller
@RequestMapping("/travel/frontdesk")
@RequiredArgsConstructor
public class TravelFrontDeskController {

    @GetMapping
    public String frontDeskPage(Model model) {
        model.addAttribute("activePage", "travel-frontdesk");
        model.addAttribute("pageTitle", "Front Desk");
        return "travel/travel-frontdesk";
    }
}
