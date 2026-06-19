package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.accounts.service.VoucherService;
import com.asg.spindleserp.accounts.service.VoucherServiceImpl;
import com.asg.spindleserp.common.dto.DataTableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VoucherController — one controller serves all four voucher pages.
 *
 * Pages:
 *   GET /accounts/journals          → journal-voucher.html
 *   GET /accounts/payment-vouchers  → payment-voucher.html
 *   GET /accounts/receipt-vouchers  → receipt-voucher.html
 *   GET /accounts/contra-vouchers   → contra-voucher.html
 *   GET /accounts/aging             → aging-report.html
 *
 * Shared REST (discriminated by voucherType param):
 *   GET    /accounts/vouchers/list?voucherType=JOURNAL_VOUCHER
 *   GET    /accounts/vouchers/show/{id}
 *   POST   /accounts/vouchers/save
 *   POST   /accounts/vouchers/post/{id}          body: { allocations: [...] }  (optional)
 *   POST   /accounts/vouchers/reverse/{id}?narration=
 *   DELETE /accounts/vouchers/delete/{id}
 *   GET    /accounts/vouchers/open-for-party?partyId=&partyType=
 *   GET    /accounts/aging/summary?partyType=SUPPLIER
 *   GET    /accounts/aging/detail?partyId=&partyType=
 *
 * JS prefix convention: jv / pv / rv / cv
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class VoucherController {

    // Inject the concrete impl directly — processAllocations and findEntityByIdPublic
    // are on the impl, not on the interface.
    private final VoucherServiceImpl voucherService;

    // ── Pages ─────────────────────────────────────────────────────────────────

    @GetMapping("/accounts/journals")
    public String journalPage(Model model) {
        model.addAttribute("activePage",  "journal-voucher");
        model.addAttribute("voucherType", "JOURNAL_VOUCHER");
        return "accounts/journal-voucher";
    }

    @GetMapping("/accounts/payment-vouchers")
    public String paymentPage(Model model) {
        model.addAttribute("activePage",  "payment-voucher");
        model.addAttribute("voucherType", "PAYMENT_VOUCHER");
        return "accounts/payment-voucher";
    }

    @GetMapping("/accounts/receipt-vouchers")
    public String receiptPage(Model model) {
        model.addAttribute("activePage",  "receipt-voucher");
        model.addAttribute("voucherType", "RECEIPT_VOUCHER");
        return "accounts/receipt-voucher";
    }

    @GetMapping("/accounts/contra-vouchers")
    public String contraPage(Model model) {
        model.addAttribute("activePage",  "contra-voucher");
        model.addAttribute("voucherType", "CONTRA_VOUCHER");
        return "accounts/contra-voucher";
    }

    @GetMapping("/accounts/aging")
    public String agingPage(Model model) {
        model.addAttribute("activePage", "aging-report");
        return "accounts/aging-report";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/accounts/vouchers/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "") String voucherType) {
        return voucherService.datatableList(voucherType, draw, start, length, search);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/accounts/vouchers/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", voucherService.findById(id)));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Save (draft) ──────────────────────────────────────────────────────────

    @PostMapping("/accounts/vouchers/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid VoucherDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            VoucherDTO saved = voucherService.save(dto);
            res.put("success", true);
            res.put("id", saved.getId());
            res.put("message", dto.getId() != null
                ? "Voucher updated successfully."
                : "Voucher saved as draft.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Post ──────────────────────────────────────────────────────────────────

    /**
     * Post a DRAFT voucher.
     * For Payment / Receipt vouchers, an optional JSON body with allocations is accepted:
     *   { "allocations": [{sourceVoucherId, allocatedAmount, discountAmount, ...}] }
     * For Journal / Contra vouchers the body can be empty or {}.
     */
    @PostMapping("/accounts/vouchers/post/{id}")
    @ResponseBody
    public Map<String, Object> post(@PathVariable Long id,
                                    @RequestBody(required = false) VoucherDTO allocationPayload) {
        Map<String, Object> res = new HashMap<>();
        try {
            VoucherDTO posted = voucherService.post(id);

            // Process allocations if provided (PV / RV only)
            if (allocationPayload != null
                    && allocationPayload.getAllocations() != null
                    && !allocationPayload.getAllocations().isEmpty()) {
                JournalEntryMaster entity = voucherService.findEntityByIdPublic(id);
                voucherService.processAllocations(entity, allocationPayload.getAllocations());
            }

            res.put("success",   true);
            res.put("voucherNo", posted.getVoucherNo());
            res.put("message",   "Voucher " + posted.getVoucherNo() + " posted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Reverse ───────────────────────────────────────────────────────────────

    @PostMapping("/accounts/vouchers/reverse/{id}")
    @ResponseBody
    public Map<String, Object> reverse(@PathVariable Long id,
                                       @RequestParam(defaultValue = "") String narration) {
        Map<String, Object> res = new HashMap<>();
        try {
            VoucherDTO rev = voucherService.reverse(id, narration);
            res.put("success", true);
            res.put("message", "Reversal voucher " + rev.getVoucherNo() + " created.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/accounts/vouchers/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            voucherService.delete(id);
            res.put("success", true);
            res.put("message", "Voucher deleted.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Open vouchers for allocation picker ───────────────────────────────────

    @GetMapping("/accounts/vouchers/open-for-party")
    @ResponseBody
    public List<Map<String, Object>> openForParty(@RequestParam Long partyId,
                                                   @RequestParam String partyType) {
        return voucherService.openVouchersForParty(partyId, partyType);
    }

    // ── Aging endpoints ───────────────────────────────────────────────────────

    @GetMapping("/accounts/aging/summary")
    @ResponseBody
    public DataTableResponse agingSummary(
            @RequestParam(defaultValue = "1")        int draw,
            @RequestParam(defaultValue = "0")        int start,
            @RequestParam(defaultValue = "25")       int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "SUPPLIER") String partyType) {
        return voucherService.agingSummary(partyType, draw, start, length, search);
    }

    @GetMapping("/accounts/aging/detail")
    @ResponseBody
    public List<Map<String, Object>> agingDetail(
            @RequestParam Long   partyId,
            @RequestParam(defaultValue = "SUPPLIER") String partyType) {
        return voucherService.agingDetail(partyId, partyType);
    }
}
