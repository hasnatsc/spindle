#!/usr/bin/perl
use strict;
use warnings;

local $/ = undef;
my $content = <STDIN>;

# 1. Header update
$content =~ s/--  Spindle ERP  —  Accounts Operational Seed  v4\.0\n--  File   : V4_accounts_operational_seed\.sql/--  Spindle ERP  —  Accounts Operational Seed v5.0\n--  File   : V13__seed_accounts_operational.sql/;
$content =~ s/--    1\. acc_periods          – Fiscal Year 2024-25 \(12 monthly \+ 1 yearly\)/--    1. acc_periods          – Fiscal Years 2024-25 and 2025-26/;

# 2. Add FY 2025-26 periods after JUN-2025
$content =~ s/(WHERE NOT EXISTS \(SELECT 1 FROM acc_periods WHERE period_name = 'JUN-2025'\);\n)\n\n-- =============================================================================\n-- 2\. ACC_MAPPING/$1\n\n-- =============================================================================\n-- Fiscal Year 2025-2026 \(July 2025 – June 2026\)\n\nINSERT INTO acc_periods \(organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at\)\nSELECT 1, 'FY-2025-26', 'YEARLY', 2025, '2025-07-01', '2026-06-30', TRUE, FALSE, 'FY 2025-26', 'SYSTEM', NOW\(\), NOW\(\)\nWHERE NOT EXISTS \(SELECT 1 FROM acc_periods WHERE period_name = 'FY-2025-26'\);\n\nINSERT INTO acc_periods \(organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at\)\nSELECT 1, to_char\(d, 'Mon-YYYY'\), 'MONTHLY', extract\(year from d\), d, \(date_trunc\('month', d\) + interval '1 month' - interval '1 day'\)::date, TRUE, FALSE, to_char\(d, 'Mon YYYY'\), 'SYSTEM', NOW\(\), NOW\(\)\nFROM generate_series\('2025-07-01'::date, '2026-06-01'::date, '1 month'::interval\) d\nWHERE NOT EXISTS \(SELECT 1 FROM acc_periods WHERE period_name = to_char\(d, 'Mon-YYYY'\)\);\n\n-- =============================================================================\n-- 2. ACC_MAPPING/;

# 3. Fix tax_type -> tax_code (with tax_rate)
$content =~ s/    amount_type, tax_type, tax_rate, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/    amount_type, tax_code, tax_rate, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/g;

# 4. Fix tax_type -> tax_code (without tax_rate)
$content =~ s/    amount_type, tax_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/    amount_type, tax_code, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/g;

# 5. Remove sub_account_id from opening balances column list
$content =~ s/INSERT INTO acc_opening_balances \(organization_id, account_id, accounting_period_id, sub_account_id,/INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id,/g;

# 6. Remove NULL line after period subquery in opening balances
$content =~ s/(SELECT id FROM acc_periods WHERE period_name = '[^']+' AND organization_id = 1),\n    NULL,\n/$1,\n/g;

# 7. Fix ob.sub_account_id IS NULL
$content =~ s/AND ob\.sub_account_id IS NULL/AND 1=1/g;

# 8. Add organization_id to standard mapping_details column list
$content =~ s/    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/    amount_type, organization_id, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/g;

# 9. Add organization_id to tax_rate mapping_details column list
$content =~ s/    amount_type, tax_code, tax_rate, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/    amount_type, tax_code, tax_rate, organization_id, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/g;

# 10. Add organization_id to tax_code-only mapping_details column list
$content =~ s/    amount_type, tax_code, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/    amount_type, tax_code, organization_id, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,/g;

# 11. Add m.organization_id to mapping_details SELECT values - simple patterns
$content =~ s/^(\s+)'((?:SUBTOTAL|FORMULA|FULL_AMOUNT|BALANCE))',\n(\s+)\(SELECT id FROM acc_chart_of_accounts/$1'$2',\n$1m.organization_id,\n$3(SELECT id FROM acc_chart_of_accounts/gm;

# 12. Add m.organization_id after FIELD_VALUE without INCOME_TAX
$content =~ s/^(\s+)'FIELD_VALUE',\n(\s+)\(SELECT id FROM acc_chart_of_accounts/$1'FIELD_VALUE',\n$1m.organization_id,\n$2(SELECT id FROM acc_chart_of_accounts/gm;

# 13. Add m.organization_id after FIELD_VALUE with INCOME_TAX
$content =~ s/^(\s+)'FIELD_VALUE', 'INCOME_TAX',\n(\s+)\(SELECT id FROM acc_chart_of_accounts/$1'FIELD_VALUE', 'INCOME_TAX',\n$1m.organization_id,\n$2(SELECT id FROM acc_chart_of_accounts/gm;

# 14. Add m.organization_id after TAX_ONLY patterns
$content =~ s/^(\s+)'TAX_ONLY', '(INPUT_VAT|OUTPUT_VAT)', [0-9]+\.[0-9]+,\n(\s+)\(SELECT id FROM acc_chart_of_accounts/$&$1m.organization_id,\n/gm;

print $content;
