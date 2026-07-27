package com.asg.spindleserp.accounts.meta;

import com.asg.spindleserp.accounts.dto.ChartOfAccountSubDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub.SubAccountType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.asg.spindleserp.accounts.entity.ChartOfAccountSub.SubAccountType.*;

/**
 * SubAccountField — the single source of truth for the sub-ledger CRUD.
 *
 * <p>THE PROBLEM THIS SOLVES</p>
 * The previous design repeated every field four times: once in the entity, once
 * in the DTO, once in a {@code switch} inside the service, once as hand-written
 * markup in the Thymeleaf page, and again in a chain of {@code if/else if} in the
 * page's JavaScript. Eleven sub-types × ~90 columns meant that adding one MFS
 * field touched five files, and forgetting one of them failed silently — which is
 * exactly what happened to MOBILE_BANKING, CARD and WALLET: they had columns and
 * a discriminator but no service branch, so every value the user typed was
 * dropped on save.
 *
 * <p>THE DESIGN</p>
 * Each constant below declares a field ONCE: its JSON key, label, input widget,
 * layout width, validation hints, the set of sub-types it applies to, and typed
 * accessors for both sides of the DTO↔entity copy. Everything else is derived:
 * <ul>
 *   <li>the service copies exactly the fields the type declares — no switch;</li>
 *   <li>{@code /accounts/sub-accounts/meta} serves this as JSON;</li>
 *   <li>the page builds its form, its payload and its view modal from that JSON.</li>
 * </ul>
 * Adding a field to a type is now a one-line change here. Nothing else moves.
 *
 * <p>WHY TYPED ACCESSORS AND NOT REFLECTION</p>
 * Reflection would be shorter but would move every typo from compile time to a
 * 500 at runtime, and would need {@code setAccessible} against a Hibernate proxy.
 * The {@link Accessor} record keeps the wiring fully type-checked by javac: a
 * {@code BigDecimal} field cannot be wired to a {@code String} column.
 */
public enum SubAccountField {

    // ═══════════════════════════════════════════════════════════════════════
    // IDENTITY — every type
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Parent GL head. Deferred: needs ChartOfAccountRepository to resolve the FK,
     * and is org-validated in the service, so it carries no accessor.
     */
    MAIN_ACCOUNT_ID("mainAccountId", "Main Account", Input.AJAX_COA, Group.IDENTITY,
            8, null, null, true, all(), null),

    SUB_ACCOUNT_CODE("subAccountCode", "Code", Input.TEXT_UPPER, Group.IDENTITY,
            4, 50, null, false, all(),
            f(ChartOfAccountSub::getSubAccountCode, ChartOfAccountSub::setSubAccountCode,
                    ChartOfAccountSubDTO::getSubAccountCode, ChartOfAccountSubDTO::setSubAccountCode)),

    SUB_ACCOUNT_NAME("subAccountName", "Name", Input.TEXT, Group.IDENTITY,
            8, 200, null, true, all(),
            f(ChartOfAccountSub::getSubAccountName, ChartOfAccountSub::setSubAccountName,
                    ChartOfAccountSubDTO::getSubAccountName, ChartOfAccountSubDTO::setSubAccountName)),

    CURRENCY("currency", "Currency", Input.TEXT, Group.IDENTITY,
            4, 20, null, false, all(),
            f(ChartOfAccountSub::getCurrency, ChartOfAccountSub::setCurrency,
                    ChartOfAccountSubDTO::getCurrency, ChartOfAccountSubDTO::setCurrency)),

    OPENING_BALANCE("openingBalance", "Opening Balance", Input.AMOUNT, Group.IDENTITY,
            4, null, null, false, all(),
            f(ChartOfAccountSub::getOpeningBalance, ChartOfAccountSub::setOpeningBalance,
                    ChartOfAccountSubDTO::getOpeningBalance, ChartOfAccountSubDTO::setOpeningBalance)),

    DESCRIPTION("description", "Description", Input.TEXTAREA, Group.IDENTITY,
            12, 1000, null, false, all(),
            f(ChartOfAccountSub::getDescription, ChartOfAccountSub::setDescription,
                    ChartOfAccountSubDTO::getDescription, ChartOfAccountSubDTO::setDescription)),

    // ═══════════════════════════════════════════════════════════════════════
    // CONTACT — parties and instruments that have a human on the other end.
    // Deliberately NOT on CASH / WALLET: a petty-cash box has a custodian,
    // not a contact person, and rendering both was the noise being complained
    // about.
    // ═══════════════════════════════════════════════════════════════════════

    CONTACT_PERSON("contactPerson", "Contact Person", Input.TEXT, Group.CONTACT,
            4, 200, null, false,
            of(BANK, MOBILE_BANKING, CARD, CUSTOMER, SUPPLIER, EMPLOYEE, LC, INTER_COMPANY, GENERAL),
            f(ChartOfAccountSub::getContactPerson, ChartOfAccountSub::setContactPerson,
                    ChartOfAccountSubDTO::getContactPerson, ChartOfAccountSubDTO::setContactPerson)),

    CONTACT_PHONE("contactPhone", "Phone", Input.TEXT, Group.CONTACT,
            4, 20, null, false,
            of(BANK, MOBILE_BANKING, CARD, CUSTOMER, SUPPLIER, EMPLOYEE, LC, INTER_COMPANY, GENERAL),
            f(ChartOfAccountSub::getContactPhone, ChartOfAccountSub::setContactPhone,
                    ChartOfAccountSubDTO::getContactPhone, ChartOfAccountSubDTO::setContactPhone)),

    CONTACT_EMAIL("contactEmail", "Email", Input.EMAIL, Group.CONTACT,
            4, 100, null, false,
            of(BANK, MOBILE_BANKING, CARD, CUSTOMER, SUPPLIER, EMPLOYEE, LC, INTER_COMPANY, GENERAL),
            f(ChartOfAccountSub::getContactEmail, ChartOfAccountSub::setContactEmail,
                    ChartOfAccountSubDTO::getContactEmail, ChartOfAccountSubDTO::setContactEmail)),

    ADDRESS("address", "Address", Input.TEXT, Group.CONTACT,
            12, 500, null, false,
            of(CUSTOMER, SUPPLIER, EMPLOYEE, INTER_COMPANY, GENERAL),
            f(ChartOfAccountSub::getAddress, ChartOfAccountSub::setAddress,
                    ChartOfAccountSubDTO::getAddress, ChartOfAccountSubDTO::setAddress)),

    CITY("city", "City", Input.TEXT, Group.CONTACT,
            3, 50, null, false, of(CUSTOMER, SUPPLIER, EMPLOYEE, INTER_COMPANY),
            f(ChartOfAccountSub::getCity, ChartOfAccountSub::setCity,
                    ChartOfAccountSubDTO::getCity, ChartOfAccountSubDTO::setCity)),

    STATE("state", "State / Division", Input.TEXT, Group.CONTACT,
            3, 50, null, false, of(CUSTOMER, SUPPLIER, EMPLOYEE, INTER_COMPANY),
            f(ChartOfAccountSub::getState, ChartOfAccountSub::setState,
                    ChartOfAccountSubDTO::getState, ChartOfAccountSubDTO::setState)),

    COUNTRY("country", "Country", Input.TEXT, Group.CONTACT,
            3, 50, null, false, of(CUSTOMER, SUPPLIER, EMPLOYEE, INTER_COMPANY),
            f(ChartOfAccountSub::getCountry, ChartOfAccountSub::setCountry,
                    ChartOfAccountSubDTO::getCountry, ChartOfAccountSubDTO::setCountry)),

    POSTAL_CODE("postalCode", "Postal Code", Input.TEXT, Group.CONTACT,
            3, 20, null, false, of(CUSTOMER, SUPPLIER, EMPLOYEE, INTER_COMPANY),
            f(ChartOfAccountSub::getPostalCode, ChartOfAccountSub::setPostalCode,
                    ChartOfAccountSubDTO::getPostalCode, ChartOfAccountSubDTO::setPostalCode)),

    TAX_ID("taxId", "Tax ID / e-TIN", Input.TEXT, Group.CONTACT,
            4, 50, null, false, of(CUSTOMER, SUPPLIER, INTER_COMPANY),
            f(ChartOfAccountSub::getTaxId, ChartOfAccountSub::setTaxId,
                    ChartOfAccountSubDTO::getTaxId, ChartOfAccountSubDTO::setTaxId)),

    VAT_REG_NO("vatRegistrationNo", "VAT / BIN No", Input.TEXT, Group.CONTACT,
            4, 50, null, false, of(CUSTOMER, SUPPLIER, INTER_COMPANY),
            f(ChartOfAccountSub::getVatRegistrationNo, ChartOfAccountSub::setVatRegistrationNo,
                    ChartOfAccountSubDTO::getVatRegistrationNo, ChartOfAccountSubDTO::setVatRegistrationNo)),

    // ═══════════════════════════════════════════════════════════════════════
    // BANK
    // ═══════════════════════════════════════════════════════════════════════

    /** Deferred — resolved through BankRepository in the service. */
    BANK_ID("bankId", "Bank", Input.AJAX_BANK, Group.BANK,
            6, null, null, false, of(BANK), null),

    ACCOUNT_NUMBER("accountNumber", "A/C Number", Input.TEXT, Group.BANK,
            6, 50, null, false, of(BANK),
            f(ChartOfAccountSub::getAccountNumber, ChartOfAccountSub::setAccountNumber,
                    ChartOfAccountSubDTO::getAccountNumber, ChartOfAccountSubDTO::setAccountNumber)),

    ACCOUNT_TITLE("accountTitle", "A/C Title", Input.TEXT, Group.BANK,
            6, 200, null, false, of(BANK),
            f(ChartOfAccountSub::getAccountTitle, ChartOfAccountSub::setAccountTitle,
                    ChartOfAccountSubDTO::getAccountTitle, ChartOfAccountSubDTO::setAccountTitle)),

    BANK_ACCOUNT_TYPE("bankAccountType", "A/C Type", Input.SELECT, Group.BANK,
            6, 20, "CURRENT,SAVINGS,STD,FDR,LOAN,OD", false, of(BANK),
            f(ChartOfAccountSub::getBankAccountType, ChartOfAccountSub::setBankAccountType,
                    ChartOfAccountSubDTO::getBankAccountType, ChartOfAccountSubDTO::setBankAccountType)),

    BRANCH_NAME("branchName", "Branch Name", Input.TEXT, Group.BANK,
            4, 100, null, false, of(BANK),
            f(ChartOfAccountSub::getBranchName, ChartOfAccountSub::setBranchName,
                    ChartOfAccountSubDTO::getBranchName, ChartOfAccountSubDTO::setBranchName)),

    BRANCH_CODE("branchCode", "Branch Code", Input.TEXT, Group.BANK,
            4, 10, null, false, of(BANK),
            f(ChartOfAccountSub::getBranchCode, ChartOfAccountSub::setBranchCode,
                    ChartOfAccountSubDTO::getBranchCode, ChartOfAccountSubDTO::setBranchCode)),

    BRANCH_PHONE("branchPhone", "Branch Phone", Input.TEXT, Group.BANK,
            4, 20, null, false, of(BANK),
            f(ChartOfAccountSub::getBranchPhone, ChartOfAccountSub::setBranchPhone,
                    ChartOfAccountSubDTO::getBranchPhone, ChartOfAccountSubDTO::setBranchPhone)),

    BRANCH_ADDRESS("branchAddress", "Branch Address", Input.TEXT, Group.BANK,
            12, 200, null, false, of(BANK),
            f(ChartOfAccountSub::getBranchAddress, ChartOfAccountSub::setBranchAddress,
                    ChartOfAccountSubDTO::getBranchAddress, ChartOfAccountSubDTO::setBranchAddress)),

    ROUTING_NUMBER("routingNumber", "Routing No", Input.TEXT, Group.BANK,
            4, 9, null, false, of(BANK),
            f(ChartOfAccountSub::getRoutingNumber, ChartOfAccountSub::setRoutingNumber,
                    ChartOfAccountSubDTO::getRoutingNumber, ChartOfAccountSubDTO::setRoutingNumber)),

    SWIFT_CODE("swiftCode", "SWIFT Code", Input.TEXT_UPPER, Group.BANK,
            4, 11, null, false, of(BANK),
            f(ChartOfAccountSub::getSwiftCode, ChartOfAccountSub::setSwiftCode,
                    ChartOfAccountSubDTO::getSwiftCode, ChartOfAccountSubDTO::setSwiftCode)),

    IBAN_NUMBER("ibanNumber", "IBAN", Input.TEXT_UPPER, Group.BANK,
            4, 34, null, false, of(BANK),
            f(ChartOfAccountSub::getIbanNumber, ChartOfAccountSub::setIbanNumber,
                    ChartOfAccountSubDTO::getIbanNumber, ChartOfAccountSubDTO::setIbanNumber)),

    INTEREST_RATE("interestRate", "Interest Rate %", Input.RATE, Group.BANK,
            4, null, null, false, of(BANK),
            f(ChartOfAccountSub::getInterestRate, ChartOfAccountSub::setInterestRate,
                    ChartOfAccountSubDTO::getInterestRate, ChartOfAccountSubDTO::setInterestRate)),

    OVERDRAFT_LIMIT("overdraftLimit", "Overdraft Limit", Input.AMOUNT, Group.BANK,
            4, null, null, false, of(BANK),
            f(ChartOfAccountSub::getOverdraftLimit, ChartOfAccountSub::setOverdraftLimit,
                    ChartOfAccountSubDTO::getOverdraftLimit, ChartOfAccountSubDTO::setOverdraftLimit)),

    OVERDRAFT_RATE("overdraftInterestRate", "OD Interest %", Input.RATE, Group.BANK,
            4, null, null, false, of(BANK),
            f(ChartOfAccountSub::getOverdraftInterestRate, ChartOfAccountSub::setOverdraftInterestRate,
                    ChartOfAccountSubDTO::getOverdraftInterestRate, ChartOfAccountSubDTO::setOverdraftInterestRate)),

    // ═══════════════════════════════════════════════════════════════════════
    // CASH
    // ═══════════════════════════════════════════════════════════════════════

    CASH_ACCOUNT_TYPE("cashAccountType", "Cash Type", Input.SELECT, Group.CASH,
            4, 20, "PETTY_CASH,MAIN_CASH,TILL,SAFE,IMPREST", false, of(CASH),
            f(ChartOfAccountSub::getCashAccountType, ChartOfAccountSub::setCashAccountType,
                    ChartOfAccountSubDTO::getCashAccountType, ChartOfAccountSubDTO::setCashAccountType)),

    LOCATION("location", "Location", Input.TEXT, Group.CASH,
            4, 100, null, false, of(CASH),
            f(ChartOfAccountSub::getLocation, ChartOfAccountSub::setLocation,
                    ChartOfAccountSubDTO::getLocation, ChartOfAccountSubDTO::setLocation)),

    CUSTODIAN("custodian", "Custodian", Input.TEXT, Group.CASH,
            4, 100, null, false, of(CASH),
            f(ChartOfAccountSub::getCustodian, ChartOfAccountSub::setCustodian,
                    ChartOfAccountSubDTO::getCustodian, ChartOfAccountSubDTO::setCustodian)),

    CUSTODIAN_PHONE("custodianPhone", "Custodian Phone", Input.TEXT, Group.CASH,
            4, 20, null, false, of(CASH),
            f(ChartOfAccountSub::getCustodianPhone, ChartOfAccountSub::setCustodianPhone,
                    ChartOfAccountSubDTO::getCustodianPhone, ChartOfAccountSubDTO::setCustodianPhone)),

    CUSTODIAN_EMAIL("custodianEmail", "Custodian Email", Input.EMAIL, Group.CASH,
            4, 100, null, false, of(CASH),
            f(ChartOfAccountSub::getCustodianEmail, ChartOfAccountSub::setCustodianEmail,
                    ChartOfAccountSubDTO::getCustodianEmail, ChartOfAccountSubDTO::setCustodianEmail)),

    MAXIMUM_LIMIT("maximumLimit", "Maximum Limit", Input.AMOUNT, Group.CASH,
            4, null, null, false, of(CASH),
            f(ChartOfAccountSub::getMaximumLimit, ChartOfAccountSub::setMaximumLimit,
                    ChartOfAccountSubDTO::getMaximumLimit, ChartOfAccountSubDTO::setMaximumLimit)),

    MINIMUM_LIMIT("minimumLimit", "Minimum Limit", Input.AMOUNT, Group.CASH,
            4, null, null, false, of(CASH),
            f(ChartOfAccountSub::getMinimumLimit, ChartOfAccountSub::setMinimumLimit,
                    ChartOfAccountSubDTO::getMinimumLimit, ChartOfAccountSubDTO::setMinimumLimit)),

    APPROVAL_LIMIT("approvalLimit", "Approval Limit", Input.AMOUNT, Group.CASH,
            4, null, null, false, of(CASH),
            f(ChartOfAccountSub::getApprovalLimit, ChartOfAccountSub::setApprovalLimit,
                    ChartOfAccountSubDTO::getApprovalLimit, ChartOfAccountSubDTO::setApprovalLimit)),

    /** Primitive on the entity — explicit lambda so a null DTO value cannot NPE on unboxing. */
    REQUIRES_APPROVAL("requiresApproval", "Requires Approval", Input.SWITCH, Group.CASH,
            4, null, null, false, of(CASH),
            f(ChartOfAccountSub::isRequiresApproval,
                    (e, v) -> e.setRequiresApproval(Boolean.TRUE.equals(v)),
                    ChartOfAccountSubDTO::getRequiresApproval, ChartOfAccountSubDTO::setRequiresApproval)),

    // ═══════════════════════════════════════════════════════════════════════
    // MOBILE_BANKING  (bKash / Nagad / Rocket / Upay)
    // Previously unreachable: no service branch existed, so every one of these
    // silently discarded its value on save.
    // ═══════════════════════════════════════════════════════════════════════

    MFS_PROVIDER("mfsProvider", "MFS Provider", Input.SELECT, Group.MFS,
            4, 30, "BKASH,NAGAD,ROCKET,UPAY,TAP,MCASH,SURECASH", false, of(MOBILE_BANKING),
            f(ChartOfAccountSub::getMfsProvider, ChartOfAccountSub::setMfsProvider,
                    ChartOfAccountSubDTO::getMfsProvider, ChartOfAccountSubDTO::setMfsProvider)),

    MFS_ACCOUNT_NUMBER("mfsAccountNumber", "Registered Number", Input.TEXT, Group.MFS,
            4, 20, null, false, of(MOBILE_BANKING),
            f(ChartOfAccountSub::getMfsAccountNumber, ChartOfAccountSub::setMfsAccountNumber,
                    ChartOfAccountSubDTO::getMfsAccountNumber, ChartOfAccountSubDTO::setMfsAccountNumber)),

    MFS_ACCOUNT_TYPE("mfsAccountType", "MFS A/C Type", Input.SELECT, Group.MFS,
            4, 20, "PERSONAL,AGENT,MERCHANT,DISBURSEMENT", false, of(MOBILE_BANKING),
            f(ChartOfAccountSub::getMfsAccountType, ChartOfAccountSub::setMfsAccountType,
                    ChartOfAccountSubDTO::getMfsAccountType, ChartOfAccountSubDTO::setMfsAccountType)),

    MERCHANT_NUMBER("merchantNumber", "Merchant Number", Input.TEXT, Group.MFS,
            4, 50, null, false, of(MOBILE_BANKING),
            f(ChartOfAccountSub::getMerchantNumber, ChartOfAccountSub::setMerchantNumber,
                    ChartOfAccountSubDTO::getMerchantNumber, ChartOfAccountSubDTO::setMerchantNumber)),

    MFS_SHORT_CODE("mfsShortCode", "Short Code", Input.TEXT, Group.MFS,
            4, 20, null, false, of(MOBILE_BANKING),
            f(ChartOfAccountSub::getMfsShortCode, ChartOfAccountSub::setMfsShortCode,
                    ChartOfAccountSubDTO::getMfsShortCode, ChartOfAccountSubDTO::setMfsShortCode)),

    MFS_CHARGE_RATE("mfsChargeRate", "Cash-out Charge %", Input.RATE, Group.MFS,
            4, null, null, false, of(MOBILE_BANKING),
            f(ChartOfAccountSub::getMfsChargeRate, ChartOfAccountSub::setMfsChargeRate,
                    ChartOfAccountSubDTO::getMfsChargeRate, ChartOfAccountSubDTO::setMfsChargeRate)),

    // ═══════════════════════════════════════════════════════════════════════
    // CARD (POS acquiring)
    // ═══════════════════════════════════════════════════════════════════════

    CARD_NETWORK("cardNetwork", "Card Network", Input.SELECT, Group.CARD,
            4, 30, "VISA,MASTERCARD,AMEX,NEXUS,UNIONPAY,DINERS", false, of(CARD),
            f(ChartOfAccountSub::getCardNetwork, ChartOfAccountSub::setCardNetwork,
                    ChartOfAccountSubDTO::getCardNetwork, ChartOfAccountSubDTO::setCardNetwork)),

    CARD_ACQUIRER_BANK_ID("cardAcquirerBankId", "Acquiring Bank", Input.AJAX_BANK, Group.CARD,
            4, null, null, false, of(CARD),
            f(ChartOfAccountSub::getCardAcquirerBankId, ChartOfAccountSub::setCardAcquirerBankId,
                    ChartOfAccountSubDTO::getCardAcquirerBankId, ChartOfAccountSubDTO::setCardAcquirerBankId)),

    TERMINAL_ID("terminalId", "Terminal ID", Input.TEXT, Group.CARD,
            4, 50, null, false, of(CARD),
            f(ChartOfAccountSub::getTerminalId, ChartOfAccountSub::setTerminalId,
                    ChartOfAccountSubDTO::getTerminalId, ChartOfAccountSubDTO::setTerminalId)),

    MERCHANT_ID("merchantId", "Merchant ID", Input.TEXT, Group.CARD,
            4, 50, null, false, of(CARD),
            f(ChartOfAccountSub::getMerchantId, ChartOfAccountSub::setMerchantId,
                    ChartOfAccountSubDTO::getMerchantId, ChartOfAccountSubDTO::setMerchantId)),

    POS_SERIAL_NUMBER("posSerialNumber", "POS Serial No", Input.TEXT, Group.CARD,
            4, 50, null, false, of(CARD),
            f(ChartOfAccountSub::getPosSerialNumber, ChartOfAccountSub::setPosSerialNumber,
                    ChartOfAccountSubDTO::getPosSerialNumber, ChartOfAccountSubDTO::setPosSerialNumber)),

    MDR_RATE("mdrRate", "MDR %", Input.RATE, Group.CARD,
            4, null, null, false, of(CARD),
            f(ChartOfAccountSub::getMdrRate, ChartOfAccountSub::setMdrRate,
                    ChartOfAccountSubDTO::getMdrRate, ChartOfAccountSubDTO::setMdrRate)),

    SETTLEMENT_DAYS("settlementDays", "Settlement T+N (days)", Input.NUMBER, Group.CARD,
            4, null, null, false, of(CARD),
            f(ChartOfAccountSub::getSettlementDays, ChartOfAccountSub::setSettlementDays,
                    ChartOfAccountSubDTO::getSettlementDays, ChartOfAccountSubDTO::setSettlementDays)),

    // ═══════════════════════════════════════════════════════════════════════
    // WALLET (closed-loop / store credit / gift card)
    // ═══════════════════════════════════════════════════════════════════════

    WALLET_PROVIDER("walletProvider", "Wallet Provider", Input.TEXT, Group.WALLET,
            4, 50, null, false, of(WALLET),
            f(ChartOfAccountSub::getWalletProvider, ChartOfAccountSub::setWalletProvider,
                    ChartOfAccountSubDTO::getWalletProvider, ChartOfAccountSubDTO::setWalletProvider)),

    WALLET_IDENTIFIER("walletIdentifier", "Wallet Identifier", Input.TEXT, Group.WALLET,
            4, 100, null, false, of(WALLET),
            f(ChartOfAccountSub::getWalletIdentifier, ChartOfAccountSub::setWalletIdentifier,
                    ChartOfAccountSubDTO::getWalletIdentifier, ChartOfAccountSubDTO::setWalletIdentifier)),

    WALLET_TYPE("walletType", "Wallet Type", Input.SELECT, Group.WALLET,
            4, 30, "PREPAID,STORE_CREDIT,LOYALTY,GIFT_CARD", false, of(WALLET),
            f(ChartOfAccountSub::getWalletType, ChartOfAccountSub::setWalletType,
                    ChartOfAccountSubDTO::getWalletType, ChartOfAccountSubDTO::setWalletType)),

    // ═══════════════════════════════════════════════════════════════════════
    // LIMITS & SETTLEMENT — shared by the three non-bank payment instruments.
    // Declared here rather than inside the MFS block so that a WALLET does not
    // render its daily limit under a heading reading "Mobile Banking Details".
    // ═══════════════════════════════════════════════════════════════════════

    DAILY_TXN_LIMIT("dailyTransactionLimit", "Daily Limit", Input.AMOUNT, Group.SETTLEMENT,
            4, null, null, false, of(MOBILE_BANKING, WALLET),
            f(ChartOfAccountSub::getDailyTransactionLimit, ChartOfAccountSub::setDailyTransactionLimit,
                    ChartOfAccountSubDTO::getDailyTransactionLimit, ChartOfAccountSubDTO::setDailyTransactionLimit)),

    /**
     * Where a CARD / MFS / WALLET balance finally sweeps. Points at a BANK
     * sub-account — the Select2 filters itself using the option string below,
     * so the user cannot wire a card terminal to a supplier ledger.
     */
    SETTLEMENT_ACCOUNT_ID("settlementAccountId", "Settles Into (Bank A/C)", Input.AJAX_SUB, Group.SETTLEMENT,
            4, null, "BANK", false, of(MOBILE_BANKING, CARD, WALLET),
            f(ChartOfAccountSub::getSettlementAccountId, ChartOfAccountSub::setSettlementAccountId,
                    ChartOfAccountSubDTO::getSettlementAccountId, ChartOfAccountSubDTO::setSettlementAccountId)),

    // ═══════════════════════════════════════════════════════════════════════
    // CUSTOMER
    // ═══════════════════════════════════════════════════════════════════════

    CREDIT_LIMIT("creditLimit", "Credit Limit", Input.AMOUNT, Group.CUSTOMER,
            4, null, null, false, of(CUSTOMER),
            f(ChartOfAccountSub::getCreditLimit, ChartOfAccountSub::setCreditLimit,
                    ChartOfAccountSubDTO::getCreditLimit, ChartOfAccountSubDTO::setCreditLimit)),

    CREDIT_DAYS("creditDays", "Credit Days", Input.NUMBER, Group.CUSTOMER,
            4, null, null, false, of(CUSTOMER),
            f(ChartOfAccountSub::getCreditDays, ChartOfAccountSub::setCreditDays,
                    ChartOfAccountSubDTO::getCreditDays, ChartOfAccountSubDTO::setCreditDays)),

    PAYMENT_TERMS("paymentTerms", "Payment Terms", Input.TEXT, Group.CUSTOMER,
            4, 100, null, false, of(CUSTOMER, SUPPLIER),
            f(ChartOfAccountSub::getPaymentTerms, ChartOfAccountSub::setPaymentTerms,
                    ChartOfAccountSubDTO::getPaymentTerms, ChartOfAccountSubDTO::setPaymentTerms)),

    SALES_REPRESENTATIVE("salesRepresentative", "Sales Representative", Input.TEXT, Group.CUSTOMER,
            4, 100, null, false, of(CUSTOMER),
            f(ChartOfAccountSub::getSalesRepresentative, ChartOfAccountSub::setSalesRepresentative,
                    ChartOfAccountSubDTO::getSalesRepresentative, ChartOfAccountSubDTO::setSalesRepresentative)),

    CUSTOMER_GROUP("customerGroup", "Customer Group", Input.TEXT, Group.CUSTOMER,
            4, 50, null, false, of(CUSTOMER),
            f(ChartOfAccountSub::getCustomerGroup, ChartOfAccountSub::setCustomerGroup,
                    ChartOfAccountSubDTO::getCustomerGroup, ChartOfAccountSubDTO::setCustomerGroup)),

    LOYALTY_POINTS("loyaltyPoints", "Loyalty Points", Input.NUMBER, Group.CUSTOMER,
            4, null, null, false, of(CUSTOMER),
            f(ChartOfAccountSub::getLoyaltyPoints,
                    (e, v) -> e.setLoyaltyPoints(v != null ? v : 0),
                    ChartOfAccountSubDTO::getLoyaltyPoints, ChartOfAccountSubDTO::setLoyaltyPoints)),

    IS_EXPORT_CUSTOMER("isExportCustomer", "Export Customer", Input.SWITCH, Group.CUSTOMER,
            4, null, null, false, of(CUSTOMER),
            f(ChartOfAccountSub::getIsExportCustomer,
                    (e, v) -> e.setIsExportCustomer(Boolean.TRUE.equals(v)),
                    ChartOfAccountSubDTO::getIsExportCustomer, ChartOfAccountSubDTO::setIsExportCustomer)),

    // ═══════════════════════════════════════════════════════════════════════
    // SUPPLIER
    // ═══════════════════════════════════════════════════════════════════════

    LEAD_TIME_DAYS("leadTimeDays", "Lead Time (days)", Input.NUMBER, Group.SUPPLIER,
            4, null, null, false, of(SUPPLIER),
            f(ChartOfAccountSub::getLeadTimeDays, ChartOfAccountSub::setLeadTimeDays,
                    ChartOfAccountSubDTO::getLeadTimeDays, ChartOfAccountSubDTO::setLeadTimeDays)),

    PREFERRED_CURRENCY("preferredCurrency", "Preferred Currency", Input.TEXT_UPPER, Group.SUPPLIER,
            4, 3, null, false, of(SUPPLIER),
            f(ChartOfAccountSub::getPreferredCurrency, ChartOfAccountSub::setPreferredCurrency,
                    ChartOfAccountSubDTO::getPreferredCurrency, ChartOfAccountSubDTO::setPreferredCurrency)),

    IS_IMPORT_SUPPLIER("isImportSupplier", "Import Supplier", Input.SWITCH, Group.SUPPLIER,
            4, null, null, false, of(SUPPLIER),
            f(ChartOfAccountSub::getIsImportSupplier,
                    (e, v) -> e.setIsImportSupplier(Boolean.TRUE.equals(v)),
                    ChartOfAccountSubDTO::getIsImportSupplier, ChartOfAccountSubDTO::setIsImportSupplier)),

    CERTIFICATIONS("certifications", "Certifications (OEKO-TEX, BSCI…)", Input.TEXTAREA, Group.SUPPLIER,
            12, null, null, false, of(SUPPLIER),
            f(ChartOfAccountSub::getCertifications, ChartOfAccountSub::setCertifications,
                    ChartOfAccountSubDTO::getCertifications, ChartOfAccountSubDTO::setCertifications)),

    // ═══════════════════════════════════════════════════════════════════════
    // LC
    // ═══════════════════════════════════════════════════════════════════════

    LC_NUMBER("lcNumber", "LC Number", Input.TEXT, Group.LC,
            4, 100, null, false, of(LC),
            f(ChartOfAccountSub::getLcNumber, ChartOfAccountSub::setLcNumber,
                    ChartOfAccountSubDTO::getLcNumber, ChartOfAccountSubDTO::setLcNumber)),

    MANUAL_LC_NUMBER("manualLcNumber", "Manual LC No", Input.TEXT, Group.LC,
            4, 100, null, false, of(LC),
            f(ChartOfAccountSub::getManualLcNumber, ChartOfAccountSub::setManualLcNumber,
                    ChartOfAccountSubDTO::getManualLcNumber, ChartOfAccountSubDTO::setManualLcNumber)),

    LC_TYPE("lcType", "LC Type", Input.SELECT, Group.LC,
            4, 30, "BTB,MASTER,SIGHT,USANCE,RED_CLAUSE,STANDBY", false, of(LC),
            f(ChartOfAccountSub::getLcType, ChartOfAccountSub::setLcType,
                    ChartOfAccountSubDTO::getLcType, ChartOfAccountSubDTO::setLcType)),

    LC_STATUS("lcStatus", "LC Status", Input.SELECT, Group.LC,
            4, 30, "OPEN,SHIPPED,SETTLED,EXPIRED,CANCELLED", false, of(LC),
            f(ChartOfAccountSub::getLcStatus, ChartOfAccountSub::setLcStatus,
                    ChartOfAccountSubDTO::getLcStatus, ChartOfAccountSubDTO::setLcStatus)),

    LC_AMOUNT("lcAmount", "LC Amount", Input.AMOUNT, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getLcAmount, ChartOfAccountSub::setLcAmount,
                    ChartOfAccountSubDTO::getLcAmount, ChartOfAccountSubDTO::setLcAmount)),

    TRANSACTION_CURRENCY("transactionCurrency", "Transaction Currency", Input.TEXT_UPPER, Group.LC,
            4, 20, null, false, of(LC),
            f(ChartOfAccountSub::getTransactionCurrency, ChartOfAccountSub::setTransactionCurrency,
                    ChartOfAccountSubDTO::getTransactionCurrency, ChartOfAccountSubDTO::setTransactionCurrency)),

    EXCHANGE_RATE("exchangeRate", "Exchange Rate", Input.RATE, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getExchangeRate, ChartOfAccountSub::setExchangeRate,
                    ChartOfAccountSubDTO::getExchangeRate, ChartOfAccountSubDTO::setExchangeRate)),

    ISSUE_DATE("issueDate", "Issue Date", Input.DATE, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getIssueDate, ChartOfAccountSub::setIssueDate,
                    ChartOfAccountSubDTO::getIssueDate, ChartOfAccountSubDTO::setIssueDate)),

    EXPIRY_DATE("expiryDate", "Expiry Date", Input.DATE, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getExpiryDate, ChartOfAccountSub::setExpiryDate,
                    ChartOfAccountSubDTO::getExpiryDate, ChartOfAccountSubDTO::setExpiryDate)),

    SHIPMENT_DATE("shipmentDate", "Shipment Date", Input.DATE, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getShipmentDate, ChartOfAccountSub::setShipmentDate,
                    ChartOfAccountSubDTO::getShipmentDate, ChartOfAccountSubDTO::setShipmentDate)),

    RECEIVING_DATE("receivingDate", "Receiving Date", Input.DATE, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getReceivingDate, ChartOfAccountSub::setReceivingDate,
                    ChartOfAccountSubDTO::getReceivingDate, ChartOfAccountSubDTO::setReceivingDate)),

    TENURE_DAYS("tenureDays", "Tenure (days)", Input.NUMBER, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getTenureDays, ChartOfAccountSub::setTenureDays,
                    ChartOfAccountSubDTO::getTenureDays, ChartOfAccountSubDTO::setTenureDays)),

    MASTER_LC_NO("masterLcNo", "Master LC No", Input.TEXT, Group.LC,
            4, 100, null, false, of(LC),
            f(ChartOfAccountSub::getMasterLcNo, ChartOfAccountSub::setMasterLcNo,
                    ChartOfAccountSubDTO::getMasterLcNo, ChartOfAccountSubDTO::setMasterLcNo)),

    MASTER_LC_DATE("masterLcDate", "Master LC Date", Input.DATE, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getMasterLcDate, ChartOfAccountSub::setMasterLcDate,
                    ChartOfAccountSubDTO::getMasterLcDate, ChartOfAccountSubDTO::setMasterLcDate)),

    BTB_LC_NO("btbLcNo", "BTB LC No", Input.TEXT, Group.LC,
            4, 100, null, false, of(LC),
            f(ChartOfAccountSub::getBtbLcNo, ChartOfAccountSub::setBtbLcNo,
                    ChartOfAccountSubDTO::getBtbLcNo, ChartOfAccountSubDTO::setBtbLcNo)),

    PAYMENT_TERM("paymentTerm", "Payment Term", Input.SELECT, Group.LC,
            4, 30, "AT_SIGHT,DEFERRED,USANCE,NEGOTIATION", false, of(LC),
            f(ChartOfAccountSub::getPaymentTerm, ChartOfAccountSub::setPaymentTerm,
                    ChartOfAccountSubDTO::getPaymentTerm, ChartOfAccountSubDTO::setPaymentTerm)),

    SHIPMENT_MODE("shipmentMode", "Shipment Mode", Input.SELECT, Group.LC,
            4, 20, "SEA,AIR,ROAD,RAIL,MULTIMODAL", false, of(LC),
            f(ChartOfAccountSub::getShipmentMode, ChartOfAccountSub::setShipmentMode,
                    ChartOfAccountSubDTO::getShipmentMode, ChartOfAccountSubDTO::setShipmentMode)),

    PARTIAL_SHIPMENT("partialShipmentAllowed", "Partial Shipment Allowed", Input.SWITCH, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getPartialShipmentAllowed,
                    (e, v) -> e.setPartialShipmentAllowed(Boolean.TRUE.equals(v)),
                    ChartOfAccountSubDTO::getPartialShipmentAllowed, ChartOfAccountSubDTO::setPartialShipmentAllowed)),

    BTMA_CERTIFICATE("btmaCertificateRequired", "BTMA Certificate Required", Input.SWITCH, Group.LC,
            4, null, null, false, of(LC),
            f(ChartOfAccountSub::getBtmaCertificateRequired,
                    (e, v) -> e.setBtmaCertificateRequired(Boolean.TRUE.equals(v)),
                    ChartOfAccountSubDTO::getBtmaCertificateRequired, ChartOfAccountSubDTO::setBtmaCertificateRequired)),

    TERMS_CONDITION("termsCondition", "Terms & Conditions", Input.TEXTAREA, Group.LC,
            12, null, null, false, of(LC),
            f(ChartOfAccountSub::getTermsCondition, ChartOfAccountSub::setTermsCondition,
                    ChartOfAccountSubDTO::getTermsCondition, ChartOfAccountSubDTO::setTermsCondition)),

    // ═══════════════════════════════════════════════════════════════════════
    // REMARKS — last field on every form
    // ═══════════════════════════════════════════════════════════════════════

    REMARKS("remarks", "Remarks", Input.TEXTAREA, Group.OTHER,
            12, 1000, null, false, all(),
            f(ChartOfAccountSub::getRemarks, ChartOfAccountSub::setRemarks,
                    ChartOfAccountSubDTO::getRemarks, ChartOfAccountSubDTO::setRemarks));

    // ═══════════════════════════════════════════════════════════════════════
    // STRUCTURE
    // ═══════════════════════════════════════════════════════════════════════

    /** Widget the page renders. The JS form builder switches on exactly this. */
    public enum Input {
        TEXT, TEXT_UPPER, EMAIL, TEXTAREA,
        NUMBER, AMOUNT, RATE, DATE,
        SELECT, SWITCH,
        AJAX_COA,   // → /accounts/chart-of-accounts/search
        AJAX_BANK,  // → /accounts/sub-accounts/banks
        AJAX_SUB    // → /accounts/sub-accounts/search?subAccountType={options}
    }

    /** Visual grouping — becomes one <h6> section per group on the form. */
    public enum Group {
        IDENTITY("Account Identity", "fa-circle-info"),
        CONTACT("Contact & Address", "fa-address-book"),
        BANK("Bank Details", "fa-building-columns"),
        CASH("Cash Box Details", "fa-vault"),
        MFS("Mobile Banking Details", "fa-mobile-screen"),
        CARD("Card / POS Details", "fa-credit-card"),
        WALLET("Wallet Details", "fa-wallet"),
        SETTLEMENT("Limits & Settlement", "fa-money-bill-transfer"),
        CUSTOMER("Customer Terms", "fa-user-tie"),
        SUPPLIER("Supplier Terms", "fa-truck"),
        LC("Letter of Credit", "fa-file-contract"),
        OTHER("Notes", "fa-note-sticky");

        private final String title;
        private final String icon;

        Group(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }

        public String title() { return title; }

        public String icon() { return icon; }
    }

    /**
     * Typed DTO↔entity wiring. Both directions are declared together so they can
     * never drift apart — the class of bug where a field was read into the DTO
     * but never written back on save.
     */
    public record Accessor<T>(
            Function<ChartOfAccountSub, T> entityGet,
            BiConsumer<ChartOfAccountSub, T> entitySet,
            Function<ChartOfAccountSubDTO, T> dtoGet,
            BiConsumer<ChartOfAccountSubDTO, T> dtoSet) {
    }

    private final String key;
    private final String label;
    private final Input input;
    private final Group group;
    private final int width;
    private final Integer maxLength;
    private final String options;
    private final boolean required;
    private final EnumSet<SubAccountType> types;
    private final Accessor<?> accessor;

    SubAccountField(String key, String label, Input input, Group group,
                    int width, Integer maxLength, String options, boolean required,
                    EnumSet<SubAccountType> types, Accessor<?> accessor) {
        this.key = key;
        this.label = label;
        this.input = input;
        this.group = group;
        this.width = width;
        this.maxLength = maxLength;
        this.options = options;
        this.required = required;
        this.types = types;
        this.accessor = accessor;
    }

    public String key() { return key; }

    public String label() { return label; }

    public Input input() { return input; }

    public Group group() { return group; }

    public int width() { return width; }

    public Integer maxLength() { return maxLength; }

    public String options() { return options; }

    public boolean required() { return required; }

    public boolean appliesTo(SubAccountType t) { return t != null && types.contains(t); }

    /** Null for MAIN_ACCOUNT_ID and BANK_ID — those need repositories, so the service owns them. */
    public Accessor<?> accessor() { return accessor; }

    // ═══════════════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════════════

    /** Ordered list of fields that belong to one sub-type. Declaration order is form order. */
    public static List<SubAccountField> forType(SubAccountType t) {
        List<SubAccountField> out = new ArrayList<>();
        for (SubAccountField f : values()) {
            if (f.appliesTo(t)) out.add(f);
        }
        return out;
    }

    /** Fields grouped by section, preserving declaration order inside each. */
    public static Map<Group, List<SubAccountField>> groupedForType(SubAccountType t) {
        Map<Group, List<SubAccountField>> out = new LinkedHashMap<>();
        for (SubAccountField f : forType(t)) {
            out.computeIfAbsent(f.group, g -> new ArrayList<>()).add(f);
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COPY — the only two methods the service needs
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * DTO → entity for every field the type declares. Fields outside the type are
     * left untouched, so a payload that smuggles {@code lcAmount} into a CASH save
     * cannot write it.
     * <p>
     * Blank strings collapse to null. That matters more than it looks: several of
     * these columns carry unique indexes, and PostgreSQL treats {@code ''} as a
     * real value while ignoring NULLs — so two accounts both saved with an empty
     * SWIFT code would collide on the second insert.
     */
    public static void applyToEntity(SubAccountType type,
                                     ChartOfAccountSubDTO dto,
                                     ChartOfAccountSub entity) {
        for (SubAccountField field : values()) {
            if (!field.appliesTo(type) || field.accessor == null) continue;
            copyIn(field, field.accessor, dto, entity);
        }
    }

    /**
     * Entity → DTO. Copies every field regardless of type so that the view modal
     * and any downstream consumer still see legacy values written before a type's
     * field set was narrowed.
     */
    public static void applyToDto(ChartOfAccountSub entity, ChartOfAccountSubDTO dto) {
        for (SubAccountField field : values()) {
            if (field.accessor == null) continue;
            copyOut(field.accessor, entity, dto);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void copyIn(SubAccountField field, Accessor<T> a,
                                   ChartOfAccountSubDTO dto, ChartOfAccountSub entity) {
        T value = a.dtoGet().apply(dto);
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (field.input == Input.TEXT_UPPER) trimmed = trimmed.toUpperCase();
            value = (T) (trimmed.isEmpty() ? null : trimmed);
        }
        a.entitySet().accept(entity, value);
    }

    private static <T> void copyOut(Accessor<T> a, ChartOfAccountSub entity, ChartOfAccountSubDTO dto) {
        a.dtoSet().accept(dto, a.entityGet().apply(entity));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTION HELPERS
    //
    // These are static METHODS, not static fields. Enum constant initialisers
    // may call static methods but may not read static fields of their own type
    // (JLS 8.9.2) — a field-based lookup table here would not compile.
    // ═══════════════════════════════════════════════════════════════════════

    private static <T> Accessor<T> f(Function<ChartOfAccountSub, T> entityGet,
                                     BiConsumer<ChartOfAccountSub, T> entitySet,
                                     Function<ChartOfAccountSubDTO, T> dtoGet,
                                     BiConsumer<ChartOfAccountSubDTO, T> dtoSet) {
        return new Accessor<>(entityGet, entitySet, dtoGet, dtoSet);
    }

    private static EnumSet<SubAccountType> of(SubAccountType... t) {
        EnumSet<SubAccountType> set = EnumSet.noneOf(SubAccountType.class);
        for (SubAccountType x : t) set.add(x);
        return set;
    }

    private static EnumSet<SubAccountType> all() {
        return EnumSet.allOf(SubAccountType.class);
    }
}
