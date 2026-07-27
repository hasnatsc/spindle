package com.asg.spindleserp.accounts.meta;

import com.asg.spindleserp.accounts.entity.*;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub.SubAccountType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * SubAccountTypeMeta — everything that varies per sub-ledger partition.
 *
 * <p>Three things used to be scattered and are now here:</p>
 * <ol>
 *   <li><b>Instantiation.</b> The old {@code instantiate()} switch had no case for
 *       MOBILE_BANKING, CARD or WALLET and fell through to {@code default ->
 *       new GeneralSubAccount()}. A bKash wallet was therefore written to the
 *       database with {@code sub_account_type = 'GENERAL'} and vanished from its
 *       own tab. {@link #newInstance} is exhaustive over the enum, so the compiler
 *       now refuses to let a new partition be added without a class behind it.</li>
 *   <li><b>Code prefixes</b> for auto-numbering.</li>
 *   <li><b>Grid columns.</b> The two most useful columns differ per type — an
 *       account number for a bank, a credit limit for a customer, an expiry date
 *       for an LC. They are declared as SQL expressions here and flow through to
 *       both the query and the DataTable header without touching either.</li>
 * </ol>
 */
public final class SubAccountTypeMeta {

    private SubAccountTypeMeta() {
    }

    /** One extra grid column: JSON key, header label, SQL expression, alignment. */
    public record Column(String key, String label, String sql, String align) {
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PARSING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Blank or unknown → null (meaning "all types"). Never throws, because this
     * value arrives from a query string and a 500 on a bad tab name helps nobody.
     */
    public static SubAccountType parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return SubAccountType.fromCode(raw);
    }

    /**
     * Strict version for write paths. A save must never guess which partition the
     * row belongs to — that is how rows end up in the wrong sub-ledger.
     */
    public static SubAccountType parseOrThrow(String raw) {
        SubAccountType t = parseOrNull(raw);
        if (t == null) {
            throw new IllegalArgumentException(
                    "Unknown sub-account type '" + raw + "'. Valid types: " + names());
        }
        return t;
    }

    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (SubAccountType t : SubAccountType.values()) out.add(t.name());
        return out;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INSTANTIATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Exhaustive switch — no {@code default} branch on purpose. Adding a value to
     * {@link SubAccountType} without a matching {@code @DiscriminatorValue} entity
     * is now a compile error rather than a silent mis-filing at runtime.
     */
    public static ChartOfAccountSub newInstance(SubAccountType type) {
        Supplier<ChartOfAccountSub> s = switch (type) {
            case CASH -> CashAccount::new;
            case BANK -> BankAccount::new;
            case MOBILE_BANKING -> MobileBankingAccount::new;
            case CARD -> CardAccount::new;
            case WALLET -> WalletAccount::new;
            case CUSTOMER -> CustomerAccount::new;
            case SUPPLIER -> SupplierAccount::new;
            case EMPLOYEE -> EmployeeSubAccount::new;
            case LC -> LetterOfCredit::new;
            case GENERAL -> GeneralSubAccount::new;
            case INTER_COMPANY -> InterCompanyAccount::new;
        };
        return s.get();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRESENTATION
    // ═══════════════════════════════════════════════════════════════════════

    public static String icon(SubAccountType type) {
        if (type == null) return "fa-layer-group";
        return switch (type) {
            case CASH -> "fa-vault";
            case BANK -> "fa-building-columns";
            case MOBILE_BANKING -> "fa-mobile-screen";
            case CARD -> "fa-credit-card";
            case WALLET -> "fa-wallet";
            case CUSTOMER -> "fa-user-tie";
            case SUPPLIER -> "fa-truck";
            case EMPLOYEE -> "fa-id-badge";
            case LC -> "fa-file-contract";
            case GENERAL -> "fa-circle-nodes";
            case INTER_COMPANY -> "fa-network-wired";
        };
    }

    public static String label(SubAccountType type) {
        return type == null ? "All Sub-Ledgers" : type.getLabel();
    }

    /** Prefix for auto-generated codes, e.g. CUS-0001. */
    public static String codePrefix(SubAccountType type) {
        return switch (type) {
            case CASH -> "CSH";
            case BANK -> "BNK";
            case MOBILE_BANKING -> "MFS";
            case CARD -> "CRD";
            case WALLET -> "WLT";
            case CUSTOMER -> "CUS";
            case SUPPLIER -> "SUP";
            case EMPLOYEE -> "EMP";
            case LC -> "LC";
            case GENERAL -> "GEN";
            case INTER_COMPANY -> "ICO";
        };
    }

    /**
     * The GL account nature this partition normally hangs off. Shown as a hint in
     * the main-account picker so a customer receivable is not accidentally parented
     * to an expense head.
     */
    public static String expectedAccountHint(SubAccountType type) {
        if (type == null) return "";
        return switch (type) {
            case CASH, BANK, MOBILE_BANKING, CARD, WALLET -> "Usually an ASSET head (Cash & Bank)";
            case CUSTOMER -> "Usually Accounts Receivable (ASSET)";
            case SUPPLIER -> "Usually Accounts Payable (LIABILITY)";
            case EMPLOYEE -> "Usually Advances / Salary Payable";
            case LC -> "Usually LC Margin or LC Liability";
            case INTER_COMPANY -> "Usually Inter-Company Current A/C";
            case GENERAL -> "";
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GRID COLUMNS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Two type-specific columns for the DataTable. Every expression is a constant
     * defined here — nothing from the request reaches the SQL string.
     */
    public static List<Column> extraColumns(SubAccountType type) {
        if (type == null) {
            return List.of(new Column("sub_account_type", "Type", "s.sub_account_type", "start"));
        }
        return switch (type) {
            case BANK -> List.of(
                    new Column("extra_a", "A/C Number", "COALESCE(s.account_number, '—')", "start"),
                    new Column("extra_b", "Branch", "COALESCE(s.branch_name, '—')", "start"));
            case CASH -> List.of(
                    new Column("extra_a", "Location", "COALESCE(s.location, '—')", "start"),
                    new Column("extra_b", "Custodian", "COALESCE(s.custodian, '—')", "start"));
            case MOBILE_BANKING -> List.of(
                    new Column("extra_a", "Provider", "COALESCE(s.mfs_provider, '—')", "start"),
                    new Column("extra_b", "Number", "COALESCE(s.mfs_account_number, '—')", "start"));
            case CARD -> List.of(
                    new Column("extra_a", "Network", "COALESCE(s.card_network, '—')", "start"),
                    new Column("extra_b", "Terminal", "COALESCE(s.terminal_id, '—')", "start"));
            case WALLET -> List.of(
                    new Column("extra_a", "Provider", "COALESCE(s.wallet_provider, '—')", "start"),
                    new Column("extra_b", "Wallet Type", "COALESCE(s.wallet_type, '—')", "start"));
            case CUSTOMER -> List.of(
                    new Column("extra_a", "Credit Limit", "COALESCE(TO_CHAR(s.credit_limit, 'FM999,999,999,990.00'), '—')", "end"),
                    new Column("extra_b", "Credit Days", "COALESCE(s.credit_days::text, '—')", "center"));
            case SUPPLIER -> List.of(
                    new Column("extra_a", "Lead Time", "COALESCE(s.lead_time_days::text || ' d', '—')", "center"),
                    new Column("extra_b", "Import", "CASE WHEN s.is_import_supplier THEN 'Yes' ELSE 'No' END", "center"));
            case LC -> List.of(
                    new Column("extra_a", "LC No", "COALESCE(s.lc_number, s.manual_lc_number, '—')", "start"),
                    new Column("extra_b", "Expiry", "COALESCE(TO_CHAR(s.expiry_date, 'DD-Mon-YYYY'), '—')", "center"));
            case EMPLOYEE, GENERAL, INTER_COMPANY -> List.of(
                    new Column("extra_a", "Contact", "COALESCE(s.contact_person, '—')", "start"),
                    new Column("extra_b", "Phone", "COALESCE(s.contact_phone, '—')", "start"));
        };
    }
}
