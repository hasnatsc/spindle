#!/bin/bash
set -e
FILE="src/main/resources/claude/sql/V13__seed_accounts_operational.sql"

# Backup
cp "$FILE" "$FILE.bak"

echo "1. Header update"
sed -i 's/--  Spindle ERP  —  Accounts Operational Seed  v4\.0/--  Spindle ERP  —  Accounts Operational Seed v5.0/' "$FILE"
sed -i 's/--  File   : V4_accounts_operational_seed.sql/--  File   : V13__seed_accounts_operational.sql/' "$FILE"
sed -i 's/Fiscal Year 2024-25 (12 monthly + 1 yearly)/Fiscal Years 2024-25 and 2025-26/' "$FILE"

echo "2. tax_type -> tax_code"
sed -i 's/    amount_type, tax_type, tax_rate, account_id,/    amount_type, tax_code, tax_rate, account_id,/g' "$FILE"
sed -i 's/    amount_type, tax_type, account_id,/    amount_type, tax_code, account_id,/g' "$FILE"

echo "3. Remove sub_account_id from opening balances"
sed -i 's/INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,/INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id,/g' "$FILE"
sed -i 's/AND ob\.sub_account_id IS NULL/AND 1=1/g' "$FILE"

echo "4. Add organization_id to mapping_details columns"
sed -i 's/    amount_type, account_id, is_active, is_optional, is_tax_entry,/    amount_type, organization_id, account_id, is_active, is_optional, is_tax_entry,/g' "$FILE"
sed -i 's/    amount_type, tax_code, tax_rate, account_id,/    amount_type, tax_code, tax_rate, organization_id, account_id,/g' "$FILE"
sed -i 's/    amount_type, tax_code, account_id,/    amount_type, tax_code, organization_id, account_id,/g' "$FILE"

echo "5. Fix policy INSERT columns (remove 15 phantom columns)"
# Use perl for multiline policy column replacement
perl -i -0777 -pe 's/INSERT INTO acc_policy \(organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at\)/INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, allow_negative_amount,
    allow_reversal, allow_zero_amount, approval_threshold,
    auto_numbering, voucher_prefix, number_padding, next_voucher_number,
    require_approval, auto_post, allow_edit_after_post,
    default_narration_template, description, created_by, created_at, updated_at)/g' "$FILE"

echo ""
echo "=== Current state ==="
grep -c "tax_type" "$FILE"
grep -c "sub_account_id" "$FILE" | tr -d '\n'
echo " (sub_account_id)"
FISCAL=$(grep -c "FISCAL_YEAR" "$FILE")
echo "$FISCAL (FISCAL_YEAR)"
wc -l "$FILE"
echo ""
echo "Backup at $FILE.bak"
