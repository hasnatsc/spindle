package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.PostingTarget;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.entity.PaymentAccount;
import com.asg.spindleserp.accounts.repository.PaymentAccountRepository;
import com.asg.spindleserp.common.enums.PaymentMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * The single place that turns "how it was paid" into "which ledger to hit".
 * <p>
 * Sales, Purchase, Payroll, POS, Travel and HRM all call
 * {@link #resolve(Long, PaymentMode, Long)} and post against the returned
 * {@link PostingTarget}. None of them should ever branch on PaymentMode.
 * <p>
 * ★ orgId is a parameter rather than an internal SecurityHelper.requireOrgId()
 *   call so this class stays usable from scheduled jobs and integration tests.
 *   Controllers must pass SecurityHelper.requireOrgId() — never a client value.
 * <p>
 * ★ Exceptions are IllegalArgumentException / IllegalStateException. Swap for
 *   your project's business exception type if you have one.
 */
@Service
@RequiredArgsConstructor
public class PaymentAccountResolver {

    private final PaymentAccountRepository paymentAccountRepository;

    /** Which side of the cash flow a voucher sits on. */
    public enum Direction {
        /** RECEIPT_VOUCHER — money coming in. */
        MONEY_IN,
        /** PAYMENT_VOUCHER — money going out. */
        MONEY_OUT
    }

    // ── Resolution ────────────────────────────────────────────────────────────

    /**
     * Resolve GL coordinates for a payment.
     *
     * @param orgId            tenant, from SecurityHelper.requireOrgId()
     * @param mode             declared payment mode; may be null when paymentAccountId is given
     * @param paymentAccountId explicit account chosen by the user; when null the
     *                         default (or the only active account) for {@code mode} is used
     * @throws IllegalArgumentException when nothing usable can be resolved
     */
    @Transactional(readOnly = true)
    public PostingTarget resolve(Long orgId, PaymentMode mode, Long paymentAccountId) {
        PaymentAccount pa = load(orgId, mode, paymentAccountId);
        return toTarget(pa);
    }

    /**
     * Resolve and validate in one call — use this from voucher POST paths.
     *
     * @param amount    face amount, for limit checks; pass null to skip
     * @param reference instrument reference (TrxID / auth code); validated when required
     */
    @Transactional(readOnly = true)
    public PostingTarget resolveForPosting(Long orgId,
                                           PaymentMode mode,
                                           Long paymentAccountId,
                                           Direction direction,
                                           BigDecimal amount,
                                           String reference) {
        PaymentAccount pa = load(orgId, mode, paymentAccountId);
        assertUsable(pa, direction, amount, reference);
        return toTarget(pa);
    }

    @Transactional(readOnly = true)
    public PaymentAccount load(Long orgId, PaymentMode mode, Long paymentAccountId) {
        requireOrg(orgId);

        if (paymentAccountId != null) {
            PaymentAccount pa = paymentAccountRepository
                    .findByIdAndOrganizationId(paymentAccountId, orgId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payment account " + paymentAccountId + " not found for this organization."));

            if (!pa.isActive()) {
                throw new IllegalArgumentException(
                        "Payment account '" + pa.getPaymentAccountName() + "' is inactive.");
            }
            // A declared mode must not contradict the chosen account, but modes
            // inside the same category are interchangeable (BANK vs CHEQUE vs
            // ONLINE_TRANSFER all settle to the same bank ledger).
            if (mode != null && pa.getPaymentMode() != null
                    && mode.getAccountCategory() != pa.getPaymentMode().getAccountCategory()) {
                throw new IllegalArgumentException(
                        "Payment mode " + mode.getLabel() + " does not match account '"
                                + pa.getPaymentAccountName() + "' (" + pa.getPaymentMode().getLabel() + ").");
            }
            return pa;
        }

        if (mode == null) {
            throw new IllegalArgumentException("Either a payment mode or a payment account is required.");
        }

        // 1) explicit default for the mode
        var byDefault = paymentAccountRepository
                .findFirstByOrganizationIdAndPaymentModeAndIsDefaultTrueAndIsActiveTrue(orgId, mode);
        if (byDefault.isPresent()) {
            return byDefault.get();
        }

        // 2) exactly one active account for the mode — unambiguous, so use it
        List<PaymentAccount> candidates = paymentAccountRepository
                .findByOrganizationIdAndPaymentModeAndIsActiveTrueOrderBySortOrderAscPaymentAccountNameAsc(
                        orgId, mode);
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "No active payment account configured for " + mode.getLabel()
                            + ". Add one under Accounts → Payment Accounts.");
        }
        throw new IllegalArgumentException(
                candidates.size() + " payment accounts exist for " + mode.getLabel()
                        + ". Select one, or mark a default.");
    }

    private PostingTarget toTarget(PaymentAccount pa) {
        ChartOfAccount ledger = pa.getLedger();
        if (ledger == null || ledger.getId() == null) {
            throw new IllegalStateException(
                    "Payment account '" + pa.getPaymentAccountName() + "' has no ledger mapped.");
        }
        ChartOfAccountSub sub = pa.getSubLedger();
        ChartOfAccount charge = pa.getChargeAccount();

        return new PostingTarget(
                pa.getId(),
                ledger.getId(),
                sub != null ? sub.getId() : null,
                charge != null ? charge.getId() : null,
                pa.getPaymentMode(),
                pa.getPaymentAccountName()
        );
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Business rules that must hold before a voucher using this account can POST.
     * Deliberately separate from resolve() so DRAFT saves stay permissive.
     */
    public void assertUsable(PaymentAccount pa,
                             Direction direction,
                             BigDecimal amount,
                             String reference) {
        if (!pa.isActive()) {
            throw new IllegalArgumentException(
                    "Payment account '" + pa.getPaymentAccountName() + "' is inactive.");
        }
        if (direction == Direction.MONEY_IN && !pa.isAllowReceipt()) {
            throw new IllegalArgumentException(
                    "'" + pa.getPaymentAccountName() + "' is not enabled for receipts.");
        }
        if (direction == Direction.MONEY_OUT && !pa.isAllowPayment()) {
            throw new IllegalArgumentException(
                    "'" + pa.getPaymentAccountName() + "' is not enabled for payments.");
        }

        boolean referenceRequired = pa.isRequireReference()
                || (pa.getPaymentMode() != null && pa.getPaymentMode().isRequireReference());
        if (referenceRequired && (reference == null || reference.isBlank())) {
            String what = switch (pa.getPaymentMode() == null
                    ? PaymentMode.AccountCategory.BANK
                    : pa.getPaymentMode().getAccountCategory()) {
                case MOBILE_BANKING -> "a transaction ID (TrxID)";
                case CARD -> "an authorisation code";
                default -> "a payment reference";
            };
            throw new IllegalArgumentException(
                    "'" + pa.getPaymentAccountName() + "' requires " + what + ".");
        }

        if (amount != null && amount.signum() > 0
                && pa.getPerTransactionLimit() != null
                && pa.getPerTransactionLimit().signum() > 0
                && amount.compareTo(pa.getPerTransactionLimit()) > 0) {
            throw new IllegalArgumentException(
                    "Amount exceeds the per-transaction limit of "
                            + pa.getPerTransactionLimit().toPlainString()
                            + " on '" + pa.getPaymentAccountName() + "'.");
        }
    }

    /** True when the amount trips the account's approval threshold. */
    public boolean requiresApproval(PaymentAccount pa, BigDecimal amount) {
        if (pa.isRequireApproval()) return true;
        return amount != null
                && pa.getApprovalThreshold() != null
                && pa.getApprovalThreshold().signum() > 0
                && amount.compareTo(pa.getApprovalThreshold()) > 0;
    }

    // ── Lookups for pickers ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PaymentAccount> activeAccounts(Long orgId) {
        requireOrg(orgId);
        return paymentAccountRepository
                .findByOrganizationIdAndIsActiveTrueOrderBySortOrderAscPaymentAccountNameAsc(orgId);
    }

    @Transactional(readOnly = true)
    public List<PaymentAccount> accountsFor(Long orgId, PaymentMode mode) {
        requireOrg(orgId);
        if (mode == null) return activeAccounts(orgId);
        return paymentAccountRepository
                .findByOrganizationIdAndPaymentModeAndIsActiveTrueOrderBySortOrderAscPaymentAccountNameAsc(
                        orgId, mode);
    }

    @Transactional(readOnly = true)
    public List<PaymentAccount> accountsFor(Long orgId, PaymentMode.AccountCategory category) {
        requireOrg(orgId);
        if (category == null) return activeAccounts(orgId);
        return paymentAccountRepository
                .findByOrganizationIdAndAccountCategoryAndIsActiveTrueOrderBySortOrderAscPaymentAccountNameAsc(
                        orgId, category);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Save with the invariants applied: category derived from mode, code
     * uppercased and unique per org, sub-ledger partition checked, and at most
     * one default per mode.
     */
    @Transactional
    public PaymentAccount save(Long orgId, PaymentAccount entity) {
        requireOrg(orgId);
        entity.normalize();

        if (entity.getPaymentMode() == null) {
            throw new IllegalArgumentException("Payment mode is required.");
        }
        if (entity.getPaymentAccountCode() == null || entity.getPaymentAccountCode().isBlank()) {
            throw new IllegalArgumentException("Payment account code is required.");
        }
        if (entity.getLedger() == null || entity.getLedger().getId() == null) {
            throw new IllegalArgumentException("A ledger (Chart of Account) must be mapped.");
        }

        boolean duplicate = (entity.getId() == null)
                ? paymentAccountRepository.existsByOrganizationIdAndPaymentAccountCode(
                        orgId, entity.getPaymentAccountCode())
                : paymentAccountRepository.existsByOrganizationIdAndPaymentAccountCodeAndIdNot(
                        orgId, entity.getPaymentAccountCode(), entity.getId());
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Payment account code '" + entity.getPaymentAccountCode() + "' already exists.");
        }

        assertSubLedgerPartition(entity);

        PaymentAccount saved = paymentAccountRepository.save(entity);

        if (saved.isDefault()) {
            paymentAccountRepository.clearDefaultForMode(orgId, saved.getPaymentMode(), saved.getId());
        }
        return saved;
    }

    /**
     * The sub-ledger must live in the partition its category implies — a BKASH
     * account cannot point at a SUPPLIER sub-account. Silent mismatches here are
     * how sub-ledger balances quietly stop reconciling.
     */
    private void assertSubLedgerPartition(PaymentAccount entity) {
        ChartOfAccountSub sub = entity.getSubLedger();
        if (sub == null) return;

        String actual = sub.getSubAccountTypeCode();
        String expected = entity.getPaymentMode().getAccountCategory().getSubAccountTypeCode();

        if (actual != null && !actual.equals(expected)) {
            throw new IllegalArgumentException(
                    "Sub-ledger '" + sub.getSubAccountName() + "' is of type " + actual
                            + " but " + entity.getPaymentMode().getLabel()
                            + " requires a " + expected + " sub-account.");
        }
    }

    @Transactional
    public void delete(Long orgId, Long id) {
        requireOrg(orgId);
        PaymentAccount pa = paymentAccountRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Payment account not found."));
        if (pa.isSystem()) {
            throw new IllegalArgumentException("System payment accounts cannot be deleted.");
        }
        long used = paymentAccountRepository.countVouchersUsing(id);
        if (used > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete — " + used + " voucher(s) already reference this account. Deactivate it instead.");
        }
        paymentAccountRepository.delete(pa);
    }

    @Transactional
    public PaymentAccount toggleStatus(Long orgId, Long id) {
        requireOrg(orgId);
        PaymentAccount pa = paymentAccountRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Payment account not found."));
        pa.setActive(!pa.isActive());
        if (!pa.isActive()) {
            pa.setDefault(false);
        }
        return paymentAccountRepository.save(pa);
    }

    private void requireOrg(Long orgId) {
        if (orgId == null) {
            throw new IllegalStateException("Organization context is missing.");
        }
    }
}
