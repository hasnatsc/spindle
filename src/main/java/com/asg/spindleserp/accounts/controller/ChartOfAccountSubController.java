package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.dto.ChartOfAccountSubDTO;
import com.asg.spindleserp.accounts.dto.SubAccountMetaDTO;
import com.asg.spindleserp.accounts.meta.SubAccountTypeMeta;
import com.asg.spindleserp.accounts.service.ChartOfAccountSubService;
import com.asg.spindleserp.common.dto.DataTableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChartOfAccountSubController — {@code /accounts/sub-accounts}
 *
 * <h3>One page, eleven partitions</h3>
 * <p>There used to be six page handlers returning six templates
 * ({@code bank-accounts.html}, {@code cash-accounts.html}, …), each a near-copy of
 * the others, and five partitions had no page at all. All of them now render the
 * single dynamic {@code accounts/sub-accounts} template, which asks
 * {@code /meta?type=X} for its form and grid schema. The old URLs are kept so
 * existing menu entries, bookmarks and {@code sec_org_modules} URL rules keep
 * working.</p>
 *
 * <h3>Endpoints</h3>
 * <pre>
 * GET    /accounts/sub-accounts                    all partitions
 * GET    /accounts/sub-accounts/type/{type}        one partition
 * GET    /accounts/sub-accounts/meta?type=         form + grid schema
 * GET    /accounts/sub-accounts/types              tab strip with counts
 * GET    /accounts/sub-accounts/list               DataTable (server-side)
 * GET    /accounts/sub-accounts/show/{id}
 * POST   /accounts/sub-accounts/save
 * POST   /accounts/sub-accounts/toggle/{id}
 * DELETE /accounts/sub-accounts/delete/{id}
 * GET    /accounts/sub-accounts/search             sub-account picker
 * GET    /accounts/sub-accounts/banks              bank master picker
 * </pre>
 *
 * <p>The JS function prefix is {@code sub} for every partition. The old scheme gave
 * each type its own prefix ({@code bankEdit}, {@code custEdit}, {@code lcEdit}…),
 * which meant the grid's action buttons had to be rendered with a different
 * function name per tab and the page had to alias ten names onto one
 * implementation. One prefix, one implementation.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChartOfAccountSubController {

    private final ChartOfAccountSubService subService;

    private static final String VIEW = "accounts/sub-accounts";

    // ═════════════════════════════════════════════════════════════════════════
    // PAGES
    // ═════════════════════════════════════════════════════════════════════════

    @GetMapping("/accounts/sub-accounts")
    public String allSubPage(Model model) {
        return page(model, "", "sub-accounts");
    }

    /**
     * Generic per-partition route. Sits under {@code /type/} rather than directly
     * under {@code /accounts/sub-accounts/{type}} so it cannot shadow the literal
     * paths ({@code /list}, {@code /meta}, {@code /search}) on this same prefix.
     */
    @GetMapping("/accounts/sub-accounts/type/{type}")
    public String typedPage(@PathVariable String type, Model model) {
        var parsed = SubAccountTypeMeta.parseOrNull(type);
        return page(model, parsed == null ? "" : parsed.name(), parsed == null ? "sub-accounts" : parsed.name().toLowerCase().replace('_', '-') + "-accounts");
    }

    // ── Legacy URLs, preserved so menus and module rules keep resolving ────────

    @GetMapping("/accounts/bank-accounts")
    public String bankPage(Model model) {
        return page(model, "BANK", "bank-accounts");
    }

    @GetMapping("/accounts/cash-accounts")
    public String cashPage(Model model) {
        return page(model, "CASH", "cash-accounts");
    }

    @GetMapping("/accounts/customer-accounts")
    public String customerPage(Model model) {
        return page(model, "CUSTOMER", "customer-accounts");
    }

    @GetMapping("/accounts/supplier-accounts")
    public String supplierPage(Model model) {
        return page(model, "SUPPLIER", "supplier-accounts");
    }

    @GetMapping("/accounts/employee-accounts")
    public String employeePage(Model model) {
        return page(model, "EMPLOYEE", "employee-accounts");
    }

    @GetMapping("/accounts/mobile-banking-accounts")
    public String mfsPage(Model model) {
        return page(model, "MOBILE_BANKING", "mobile-banking-accounts");
    }

    @GetMapping("/accounts/card-accounts")
    public String cardPage(Model model) {
        return page(model, "CARD", "card-accounts");
    }

    @GetMapping("/accounts/wallet-accounts")
    public String walletPage(Model model) {
        return page(model, "WALLET", "wallet-accounts");
    }

    @GetMapping("/accounts/lc-accounts")
    public String lcPage(Model model) {
        return page(model, "LC", "lc-accounts");
    }

    private String page(Model model, String type, String activePage) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("subAccountType", type);
        return VIEW;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SCHEMA
    // ═════════════════════════════════════════════════════════════════════════

    @GetMapping("/accounts/sub-accounts/meta")
    @ResponseBody
    public SubAccountMetaDTO meta(@RequestParam(defaultValue = "") String type) {
        return subService.meta(type);
    }

    /** Tab strip: every partition with its label, icon and row count. */
    @GetMapping("/accounts/sub-accounts/types")
    @ResponseBody
    public List<Map<String, Object>> subAccountTypes() {
        return subService.typeSummary();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DATATABLE
    // ═════════════════════════════════════════════════════════════════════════

    @GetMapping("/accounts/sub-accounts/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1") int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "") String subAccountType,
            @RequestParam(defaultValue = "") String sortKey,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            return subService.datatableList(subAccountType, draw, start, length, search, sortKey, sortDir);
        } catch (Exception e) {
            log.warn("Sub-account grid failed: {}", e.getMessage());
            return DataTableResponse.error(draw, e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CRUD
    // ═════════════════════════════════════════════════════════════════════════

    @GetMapping("/accounts/sub-accounts/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        return ok(null, () -> Map.of("defaultData", subService.findById(id)));
    }

    @PostMapping("/accounts/sub-accounts/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ChartOfAccountSubDTO dto) {
        boolean isUpdate = dto.getId() != null;
        return ok(isUpdate ? "Account updated successfully." : "Account created successfully.",
                () -> Map.of("defaultData", isUpdate
                        ? subService.update(dto.getId(), dto)
                        : subService.create(dto)));
    }

    @PostMapping("/accounts/sub-accounts/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            ChartOfAccountSubDTO dto = subService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Account " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + ".");
            res.put("obj", Map.of("defaultData", dto));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", message(e));
        }
        return res;
    }

    @DeleteMapping("/accounts/sub-accounts/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        return ok("Account deleted.", () -> {
            subService.delete(id);
            return null;
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PICKERS
    // ═════════════════════════════════════════════════════════════════════════

    /** {@code ?search=abc&subAccountType=CUSTOMER&page=1} → {@code {items, hasMore}} */
    @GetMapping("/accounts/sub-accounts/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String subAccountType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize) {
        return subService.search(search, subAccountType, page, pageSize);
    }

    /**
     * Bank master picker for the BANK and CARD forms.
     * <p>
     * The page used to call {@code /banks/search}, which does not exist on
     * {@code BankController} — so the bank dropdown silently returned nothing on
     * every keystroke.
     */
    @GetMapping("/accounts/sub-accounts/banks")
    @ResponseBody
    public Map<String, Object> banks(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize) {
        return subService.searchBanks(search, page, pageSize);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RESPONSE ENVELOPE
    // ═════════════════════════════════════════════════════════════════════════

    @FunctionalInterface
    private interface Checked {
        Object run() throws Exception;
    }

    /**
     * {@code {success, message, obj}} — the shape {@code secureFetch} and
     * {@code hsAfterSaveMessages} expect.
     * <p>
     * Every handler used to repeat the same eight-line try/catch. Beyond the
     * duplication, each one returned {@code e.getMessage()} raw, which on a
     * constraint violation is a PostgreSQL index name. {@link #message} keeps
     * validation messages intact and replaces the rest with something a user can
     * act on, while the stack trace goes to the log where it belongs.
     */
    private Map<String, Object> ok(String successMessage, Checked action) {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            Object obj = action.run();
            res.put("success", true);
            if (successMessage != null) res.put("message", successMessage);
            if (obj != null) res.put("obj", obj);
        } catch (Exception e) {
            log.warn("Sub-account request failed", e);
            res.put("success", false);
            res.put("message", message(e));
        }
        return res;
    }

    /**
     * Bean-validation failures otherwise leave as a Spring 400 with its own body,
     * which {@code secureFetch} surfaces as a bare "Request failed" — the user is
     * told nothing about which field was wrong. Returning 200 with the same
     * {@code {success:false, message}} envelope as every other failure means the
     * page's existing error path shows the actual constraint message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, Object> onValidationError(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .collect(Collectors.joining(" "));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", false);
        res.put("message", detail.isBlank() ? "Please check the highlighted fields." : detail);
        return res;
    }

    private String message(Exception e) {
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return e.getMessage();
        }
        String raw = e.getMessage();
        if (raw != null && raw.toLowerCase().contains("uq_sub_org_code")) {
            return "That sub-account code is already used in this organisation.";
        }
        return "The request could not be completed. Please check the values and try again.";
    }
}
