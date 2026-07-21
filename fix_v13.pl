#!/usr/bin/perl
use strict;
use warnings;
local $/ = undef;
my $c = <STDIN>;

# 1. Header
$c =~ s/--  Spindle ERP  —  Accounts Operational Seed  v4\.0\n--  File   : V4_accounts_operational_seed/--  Spindle ERP  —  Accounts Operational Seed v5.0\n--  File   : V13__seed_accounts_operational.sql/;
$c =~ s/--    1\. acc_periods          – Fiscal Year 2024-25 \(12 monthly \+ 1 yearly\)/--    1. acc_periods          – Fiscal Years 2024-25 and 2025-26/;

# 2. Add FY 2025-26 after JUN-2025
$c =~ s/(WHERE NOT EXISTS \(SELECT 1 FROM acc_periods WHERE period_name = 'JUN-2025'\);)\n\n\n-- ==/$1\n\n\n-- =============================================================================\n-- Fiscal Year 2025-2026 (July 2025 - June 2026)\n\nINSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)\nSELECT 1, 'FY-2025-26', 'YEARLY', 2025, '2025-07-01', '2026-06-30', TRUE, FALSE, 'FY 2025-26', 'SYSTEM', NOW(), NOW()\nWHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'FY-2025-26');\n\n-- Monthly periods 2025-2026\nINSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)\nSELECT 1, to_char(d,'Mon-YYYY'), 'MONTHLY', extract(year from d), d, (date_trunc('month', d) + interval '1 month' - interval '1 day')::date, TRUE, FALSE, to_char(d,'Mon YYYY'), 'SYSTEM', NOW(), NOW()\nFROM generate_series('2025-07-01'::date, '2026-06-01'::date, '1 month'::interval) d\nWHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = to_char(d,'Mon-YYYY'));\n\n\n-- ==/;

# 3. Remove sub_account_id from opening balances header
$c =~ s/INSERT INTO acc_opening_balances \(organization_id, account_id, accounting_period_id, sub_account_id,/INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id,/g;

# 4. Remove NULL value for sub_account_id in opening balances SELECT
$c =~ s/(SELECT id FROM acc_periods WHERE period_name = '[^']+' AND organization_id = 1),\n    NULL,\n/$1,\n/g;

# 5. Fix ob.sub_account_id IS NULL
$c =~ s/AND ob\.sub_account_id IS NULL/AND 1=1/g;

# 6. Fix tax_type -> tax_code (with tax_rate)
$c =~ s/    amount_type, tax_type, tax_rate, account_id,/    amount_type, tax_code, tax_rate, account_id,/g;

# 7. Fix tax_type -> tax_code (without tax_rate)
$c =~ s/    amount_type, tax_type, account_id,/    amount_type, tax_code, account_id,/g;

# 8. Add organization_id to standard mapping_details
$c =~ s/    amount_type, account_id, is_active, is_optional, is_tax_entry,/    amount_type, organization_id, account_id, is_active, is_optional, is_tax_entry,/g;

# 9. Add organization_id to tax_rate mapping_details
$c =~ s/    amount_type, tax_code, tax_rate, account_id,/    amount_type, tax_code, tax_rate, organization_id, account_id,/g;

# 10. Add organization_id to tax_code-only mapping_details
$c =~ s/    amount_type, tax_code, account_id,/    amount_type, tax_code, organization_id, account_id,/g;

# 11. Fix policy INSERT columns - remove 15 phantom columns
$c =~ s/INSERT INTO acc_policy \(organization_id, policy_code, policy_name, policy_type, module_type,\n    is_active, is_default, is_system,\n    allow_backdating, backdating_days, allow_future_dating, future_dating_days,\n    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,\n    allow_negative_amount, allow_zero_amount, allow_direct_post,\n    require_narration, min_narration_length, require_balanced_entry, require_reference,\n    require_cost_center, restrict_to_open_period,\n    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,\n    require_approval, auto_post, auto_post_on_approval, post_on_approval,\n    apr_levels, approval_threshold,\n    default_narration_template, description, created_by, created_at, updated_at\)/INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, allow_negative_amount,
    allow_reversal, allow_zero_amount, approval_threshold,
    auto_numbering, voucher_prefix, number_padding, next_voucher_number,
    require_approval, auto_post, allow_edit_after_post,
    default_narration_template, description, created_by, created_at, updated_at)/sg;

print $c;
