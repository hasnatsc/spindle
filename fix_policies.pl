#!/usr/bin/perl
use strict;
use warnings;

local $/ = undef;
my $content = <STDIN>;

# Fix POL_RV - Receipt Voucher
$content =~ s/
select 1, 'POL_RV', 'Receipt Voucher Policy', 'RECEIPT_VOUCHER', 'ACCOUNTS_RECEIVABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, (FALSE|TRUE),
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    (FALSE|TRUE), TRUE,
    TRUE, 'RV', 6, 'FISCAL_YEAR', 1,
    (TRUE|FALSE), FALSE, (TRUE|FALSE), (TRUE|FALSE),
    1, 0,
    '([^']+)',
    '([^']+)', 'SYSTEM', NOW\(\), NOW\(\)
WHERE NOT EXISTS \(SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_RV'\)
/select 1, 'POL_RV', 'Receipt Voucher Policy', 'RECEIPT_VOUCHER', 'ACCOUNTS_RECEIVABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, FALSE,
    TRUE, FALSE, 0,
    TRUE, 'RV', 6, 1,
    FALSE, FALSE, FALSE,
    '$1',
    '$2', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_RV')/ei;

# Fix POL_CV - Contra Voucher
$content =~ s/
select 1, 'POL_CV', 'Contra Voucher Policy', 'CONTRA_VOUCHER', 'ACCOUNTS',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, FALSE,
    FALSE, TRUE,
    TRUE, 'CV', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, FALSE,
    1, 0,
    '([^']+)',
    '([^']+)', 'SYSTEM', NOW\(\), NOW\(\)
WHERE NOT EXISTS \(SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_CV'\)
/select 1, 'POL_CV', 'Contra Voucher Policy', 'CONTRA_VOUCHER', 'ACCOUNTS',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, FALSE,
    TRUE, FALSE, 0,
    TRUE, 'CV', 6, 1,
    FALSE, FALSE, FALSE,
    '$1',
    '$2', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_CV')/ei;

# Fix POL_PURV - Purchase Voucher
$content =~ s/
select 1, 'POL_PURV', 'Purchase Voucher Policy', 'PURCHASE_VOUCHER', 'ACCOUNTS_PAYABLE',
    TRUE, TRUE, TRUE,
    TRUE, 15, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'PURV', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, TRUE,
    1, 0,
    '([^']+)',
    '([^']+)', 'SYSTEM', NOW\(\), NOW\(\)
WHERE NOT EXISTS \(SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_PURV'\)
/select 1, 'POL_PURV', 'Purchase Voucher Policy', 'PURCHASE_VOUCHER', 'ACCOUNTS_PAYABLE',
    TRUE, TRUE, TRUE,
    TRUE, 15, FALSE, FALSE,
    TRUE, FALSE, 0,
    TRUE, 'PURV', 6, 1,
    FALSE, FALSE, FALSE,
    '$1',
    '$2', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_PURV')/ei;

# Fix POL_SALV - Sales Voucher
$content =~ s/
select 1, 'POL_SALV', 'Sales Voucher Policy', 'SALES_VOUCHER', 'ACCOUNTS_RECEIVABLE',
    TRUE, TRUE, TRUE,
    TRUE, 30, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'SALV', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, TRUE,
    1, 0,
    '([^']+)',
    '([^']+)', 'SYSTEM', NOW\(\), NOW\(\)
WHERE NOT EXISTS \(SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_SALV'\)
/select 1, 'POL_SALV', 'Sales Voucher Policy', 'SALES_VOUCHER', 'ACCOUNTS_RECEIVABLE',
    TRUE, TRUE, TRUE,
    TRUE, 30, FALSE, FALSE,
    TRUE, FALSE, 0,
    TRUE, 'SALV', 6, 1,
    FALSE, FALSE, FALSE,
    '$1',
    '$2', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_SALV')/ei;

# Fix POL_DN - Debit Note
$content =~ s/
select 1, 'POL_DN', 'Debit Note Policy', 'DEBIT_NOTE', 'ACCOUNTS_PAYABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'DN', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, TRUE,
    1, 0,
    '([^']+)',
    '([^']+)', 'SYSTEM', NOW\(\), NOW\(\)
WHERE NOT EXISTS \(SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_DN'\)
/select 1, 'POL_DN', 'Debit Note Policy', 'DEBIT_NOTE', 'ACCOUNTS_PAYABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, FALSE,
    TRUE, FALSE, 0,
    TRUE, 'DN', 6, 1,
    FALSE, FALSE, FALSE,
    '$1',
    '$2', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_DN')/ei;

# Fix POL_CN - Credit Note
$content =~ s/
select 1, 'POL_CN', 'Credit Note Policy', 'CREDIT_NOTE', 'ACCOUNTS_RECEIVABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'CN', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, TRUE,
    1, 0,
    '([^']+)',
    '([^']+)', 'SYSTEM', NOW\(\), NOW\(\)
WHERE NOT EXISTS \(SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_CN'\)
/select 1, 'POL_CN', 'Credit Note Policy', 'CREDIT_NOTE', 'ACCOUNTS_RECEIVABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, FALSE,
    TRUE, FALSE, 0,
    TRUE, 'CN', 6, 1,
    FALSE, FALSE, FALSE,
    '$1',
    '$2', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_CN')/ei;

# Fix POL_EXP - Expense Voucher
$content =~ s/
select 1, 'POL_EXP', 'Expense Voucher Policy', 'EXPENSE_VOUCHER', 'GENERAL_LEDGER',
    TRUE, TRUE, TRUE,
    TRUE, 30, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, FALSE,
    FALSE, TRUE,
    TRUE, 'EXP', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, TRUE,
    1, 0,
    '([^']+)',
    '([^']+)', 'SYSTEM', NOW\(\), NOW\(\)
WHERE NOT EXISTS \(SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_EXP'\)
/select 1, 'POL_EXP', 'Expense Voucher Policy', 'EXPENSE_VOUCHER', 'GENERAL_LEDGER',
    TRUE, TRUE, TRUE,
    TRUE, 30, FALSE, FALSE,
    TRUE, FALSE, 0,
    TRUE, 'EXP', 6, 1,
    FALSE, FALSE, FALSE,
    '$1',
    '$2', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_EXP')/ei;

print $content;
