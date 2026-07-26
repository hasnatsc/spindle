package com.asg.spindleserp.common.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PaymentMode — how money physically moved.
 * <p>
 * A payment mode NEVER identifies a ledger by itself. It only narrows the search
 * to an {@link AccountCategory}; the actual posting target is resolved through:
 *
 * <pre>
 *   PaymentMode  →  AccountCategory  →  PaymentAccount  →  ChartOfAccount (+ ChartOfAccountSub)
 * </pre>
 * <p>
 * This is what lets ASG hold "bKash Merchant", "bKash Personal" and "Nagad Salary"
 * simultaneously without any business logic knowing they exist.
 * <p>
 * Persisted with {@code @Enumerated(EnumType.STRING)} — never by ordinal.
 * Declaration order below is the display order used by Select2 dropdowns.
 */
@Getter
public enum PaymentMode {

    CASH            (AccountCategory.CASH,           "Cash",            false),
    BANK            (AccountCategory.BANK,           "Bank",            false),
    BKASH           (AccountCategory.MOBILE_BANKING, "bKash",           true),
    NAGAD           (AccountCategory.MOBILE_BANKING, "Nagad",           true),
    ROCKET          (AccountCategory.MOBILE_BANKING, "Rocket",          true),
    CARD            (AccountCategory.CARD,           "Card",            true),
    CHEQUE          (AccountCategory.BANK,           "Cheque",          true),
    ONLINE_TRANSFER (AccountCategory.BANK,           "Online Transfer", true),
    WALLET          (AccountCategory.WALLET,         "Wallet",          true);

    private final AccountCategory accountCategory;

    /** Human label for dropdowns / vouchers. */
    private final String label;

    /**
     * True when the voucher must carry an instrument reference before it can be POSTED:
     * cheque number, MFS TrxID, or card authorisation code.
     * Enforced in the service layer, not by the DB.
     */
    private final boolean requireReference;

    PaymentMode(AccountCategory accountCategory, String label, boolean requireReference) {
        this.accountCategory = accountCategory;
        this.label = label;
        this.requireReference = requireReference;
    }

    public boolean isCash() {
        return accountCategory == AccountCategory.CASH;
    }

    /** bKash / Nagad / Rocket — Bangladesh MFS rails. */
    public boolean isMobileFinancialService() {
        return accountCategory == AccountCategory.MOBILE_BANKING;
    }

    /** Anything that ultimately hits a bank ledger (BANK, CHEQUE, ONLINE_TRANSFER). */
    public boolean isBankBacked() {
        return accountCategory == AccountCategory.BANK;
    }

    /** Cheque is the only mode that needs chequeNumber + chequeDate on the voucher. */
    public boolean isChequeInstrument() {
        return this == CHEQUE;
    }

    /**
     * Null-safe, case-insensitive parse that also absorbs the legacy free-text values
     * previously stored in {@code acc_journal_entry_master.payment_mode}
     * ("BANK_TRANSFER", "ONLINE", "MOBILE", "MFS", "bkash", "b-kash", ...).
     *
     * @return the matching mode, or {@code null} when the input is blank/unmappable.
     */
    public static PaymentMode fromCode(String code) {
        if (code == null) return null;
        String k = code.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (k.isEmpty()) return null;

        // legacy aliases — keep in sync with V310 migration remap block
        switch (k) {
            case "BANK_TRANSFER", "BANKTRANSFER", "TRANSFER", "NEFT", "RTGS", "EFT" -> k = "BANK";
            case "ONLINE", "ONLINE_BANKING", "INTERNET_BANKING", "BEFTN"            -> k = "ONLINE_TRANSFER";
            case "B_KASH", "BIKASH"                                                 -> k = "BKASH";
            case "DBBL_ROCKET"                                                      -> k = "ROCKET";
            // Generic MFS labels carry no provider. They land on BKASH so the
            // AccountCategory is right (MOBILE_BANKING); the actual ledger still
            // comes from the chosen PaymentAccount. Flagged in README §4.
            case "MOBILE", "MOBILE_BANKING", "MFS"                                  -> k = "BKASH";
            case "DEBIT_CARD", "CREDIT_CARD", "VISA", "MASTERCARD", "POS"           -> k = "CARD";
            case "E_WALLET", "EWALLET"                                              -> k = "WALLET";
            default -> { /* fall through to valueOf */ }
        }
        for (PaymentMode m : values()) {
            if (m.name().equals(k)) return m;
        }
        return null;
    }

    /** All modes belonging to one category — used to filter PaymentAccount pickers. */
    public static List<PaymentMode> byCategory(AccountCategory category) {
        List<PaymentMode> out = new ArrayList<>();
        if (category == null) return out;
        for (PaymentMode m : values()) {
            if (m.accountCategory == category) out.add(m);
        }
        return out;
    }

    /**
     * Ledger family a mode settles into. Each value maps 1:1 (by name) onto a
     * {@code ChartOfAccountSub.SubAccountType} discriminator, so a resolver can
     * go straight from category to the correct sub-ledger partition.
     */
    @Getter
    public enum AccountCategory {

        CASH           ("CASH",           "Cash In Hand"),
        BANK           ("BANK",           "Bank Accounts"),
        MOBILE_BANKING ("MOBILE_BANKING", "Mobile Banking"),
        CARD           ("CARD",           "Card Accounts"),
        WALLET         ("WALLET",         "Wallets");

        /**
         * Discriminator value in {@code acc_chart_of_accounts_sub.sub_account_type}.
         * Deliberately a String, not a direct enum reference, so that
         * {@code common.enums} never has to depend on {@code accounts.entity}.
         */
        private final String subAccountTypeCode;

        /** Suggested Chart-of-Account head name for this family. */
        private final String defaultLedgerName;

        AccountCategory(String subAccountTypeCode, String defaultLedgerName) {
            this.subAccountTypeCode = subAccountTypeCode;
            this.defaultLedgerName = defaultLedgerName;
        }

        public static AccountCategory fromCode(String code) {
            if (code == null) return null;
            String k = code.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            for (AccountCategory c : values()) {
                if (c.name().equals(k)) return c;
            }
            return null;
        }
    }
}
