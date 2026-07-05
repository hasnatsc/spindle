# Spindle ERP — Seed & Migration SQL, Synced to Live Schema

Every file in this package was executed against a PostgreSQL 16 database built
from your live schema dump (the Hibernate-generated Spindle schema), **twice in
a row**, with zero errors and zero duplicate rows on the second pass. Silent
zero-row inserts (lookups that matched nothing) were also eliminated — row
counts now match the file contents exactly.

## Execution order (verified)

    00_seed_menu_permission_complete.sql
    01_seed_ecommerce_menu_permission.sql
    01_seed_org_setup_data.sql
    02_seed_hrm_data.sql
    02_seed_inventory_items.sql
    05_seed_chart_of_accounts.sql        (minimal COA — see V4 note)
    06_seed_setup_reference_data.sql
    07_seed_approval_configs.sql
    08_seed_fixed_assets.sql
    09_seed_crm_data.sql
    V101__seed_travel_menu_permission.sql
    V102__create_travel_phase2_packages_visa_tours.sql
    V103__seed_travel_phase2_menu_permission.sql
    V104__fix_travel_booking_notes_lob.sql   ← NEW migration
    V10__org_module_access.sql
    V2__sec_user_access_scopes.sql
    V4_accounts_operational_seed.sql     (requires the FULL chart of accounts)

Prerequisites: org_organizations row (code 'SE', id 1) and the sec_users
referenced by 07/09 (purchase.manager, gm, sales.manager, accounts.admin,
accountant, hrm.manager, commercial.manager, inventory.manager) — normally
created by SecurityDataInitializer. Missing users no longer break anything:
rows insert with NULL approver/assignee instead of silently vanishing.

## Changed files — what was wrong and what was fixed

### 07_seed_approval_configs.sql  (was: every apr_levels insert FAILED)
- `apr_levels.organization_id` is NOT NULL in the live schema but was never
  supplied → the whole transaction aborted. Added `organization_id = 1` to all
  10 level inserts.
- Approver lookups converted from INNER JOIN on sec_users to scalar
  subqueries: a missing username now yields approver_user_id = NULL instead of
  dropping the level row.
- apr_levels has no unique constraint → added NOT EXISTS guards
  (config + level_number) so re-runs are safe.

### 02_seed_inventory_items.sql  (was: items 7–10 silently skipped; UOM insert not re-runnable)
- Category `CAT-SPARE-MECH-BRG` was referenced by the four SKF bearing items
  but never created → 4 items silently dropped. Added the Bearings ITEM-layer
  category under CAT-SPARE-MECH. All 10 items now insert.
- The 37-row inv_item_uom insert had no ON CONFLICT → duplicate-key error on
  every re-run. Added `ON CONFLICT ON CONSTRAINT uq_uom_org_code DO NOTHING`.

### 08_seed_fixed_assets.sql  (was: 3 of 4 assets silently skipped)
- Lookups referenced org codes from a different seed universe. Remapped to the
  codes 01_seed_org_setup_data actually creates:
  CC-PRD-A→CC-PRD, CC-FIN→CC-ACC, CC-MFG→CC-SPN, DEPT-FIN→DEPT-ACC,
  WH-MFG-RM→WH-RM-01, WH-MFG-FG→WH-FG-01.
- fa_depreciation_runs (VALUES inserts, no unique key) duplicated on re-run →
  converted to guarded SELECT … WHERE NOT EXISTS; run lines likewise.

### 09_seed_crm_data.sql  (was: 2 leads, 1 opportunity, 2 activities silently skipped)
- `sales.executive` does not exist in the seeded users → every row joining on
  it vanished, cascading into missing opportunities and activities. All user
  lookups converted to scalar subqueries (nullable assigned_to_id), so the full
  CRM data set inserts regardless.
- crm_contacts / crm_activities / crm_customer_feedback have no unique keys →
  added NOT EXISTS guards for idempotency.

### 02_seed_hrm_data.sql
- Addresses and salaries duplicated on re-run (no unique keys) → NOT EXISTS
  guards added (per employee address_type / is_current salary).

### 01_seed_org_setup_data.sql
- `ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a` targeted a
  Hibernate-generated hash name that changes whenever the schema is
  regenerated → replaced with column-based `ON CONFLICT (code)`.

### V102__create_travel_phase2_packages_visa_tours.sql  (was: FAILED on live DB)
- Dropped constraint `trv_bookingsvc_type_check`, but Hibernate named it
  `trv_booking_services_service_type_check` in the live DB → migration died on
  line 1. The check-swap is now a DO block that drops whichever name exists.
- All CREATE TABLE / CREATE INDEX now IF NOT EXISTS — the live schema already
  contains these tables, so the file works on both fresh and existing DBs.

### V104__fix_travel_booking_notes_lob.sql  (NEW)
- `trv_booking_notes.note_text` is `oid` in the live schema — the bare-@Lob
  bug (same one previously fixed on bgt_budget_notes). Migration converts
  oid → text safely (copies large-object content, unlinks orphaned LOs,
  preserves NOT NULL) and is a no-op if already text.
- Pair with the entity change:
  `@Lob @Column(name = "note_text", nullable = false, columnDefinition = "text")`.

### V4_accounts_operational_seed.sql  (was: FAILED; also targeted a stale entity shape)
- acc_mapping: NOT NULL `debit_control_type` / `credit_control_type` were
  missing from all 31 inserts → added, with per-mapping semantics
  (e.g. SUP_PAYMENT = SUPPLIER/BANK, CUST_RECEIPT = BANK/CUSTOMER,
  LC_PAY = LC/BANK, EXP_CLAIM = GENERAL/EMPLOYEE, default GENERAL/GENERAL).
- acc_policy: inserts used 15 columns that do not exist in the live table
  (future_dating_days, allow_edit, allow_delete, allow_reversal_approval,
  allow_direct_post, require_narration, min_narration_length,
  require_balanced_entry, require_reference, require_cost_center,
  restrict_to_open_period, numbering_reset, auto_post_on_approval,
  post_on_approval, apr_levels) → columns and their values pruned; everything
  the live entity supports is kept.
- acc_mapping_details: missing NOT NULL `organization_id` (added = 1);
  `tax_type` renamed to the live column `tax_code`.
- acc_opening_balances: `sub_account_id` column does not exist in the live
  table → removed from column lists, values, and NOT EXISTS predicates.
- SAFETY GUARD added at the top: the file aborts with a clear message unless
  account 1310 exists, because it targets the FULL chart of accounts. Running
  it against the minimal 05_seed COA would post sales to code 1200 — which is
  the *Fixed Assets group* there, not Accounts Receivable. Load the complete
  COA seed before this file (or keep the 05 mappings and skip V4).

## Verified unchanged (ran clean against the live schema, twice)

  00_seed_menu_permission_complete.sql   (177 menus, 0 orphans)
  01_seed_ecommerce_menu_permission.sql
  05_seed_chart_of_accounts.sql
  06_seed_setup_reference_data.sql
  V101__seed_travel_menu_permission.sql
  V103__seed_travel_phase2_menu_permission.sql
  V10__org_module_access.sql
  V2__sec_user_access_scopes.sql

## legacy/ — deprecated Optimum-era files (do not run on Spindle)

  V1__optimum_complete_schema_v2.sql   — old hand-written schema; the live
      Spindle schema is Hibernate-managed and differs substantially.
  V2__menu_permission_seed_v2.sql      — old M_* menu universe, superseded by
      00_seed (MOD_*/GRP_* codes). Fixed anyway to be valid SQL: added the now
      NOT NULL `deleted` column to all 25 menu inserts, and repaired a
      pre-existing typo where M_SET_BU's parent subquery was quoted as a
      string literal (would have failed on any schema).
  V3__accounts_menu_seed.sql           — same universe, same `deleted` fix.
  Note: these legacy files resolve parent menus with subqueries inside the
  same multi-row INSERT, so leaves whose group is created in the same
  statement end up with NULL parent_id. 00_seed does not have this problem —
  another reason it supersedes them.

## Validation method

Live schema dump → loaded into PostgreSQL 16 → bootstrap (org 'SE',
initializer users, full-COA stand-ins for V4) → all 17 files executed in
order with ON_ERROR_STOP, then executed again. Results: 0 errors both runs,
identical row counts after run 2 (fully idempotent), and all expected rows
present: 10 inv_items, 4 fa_assets, 4 depreciation lines, 4 leads,
3 opportunities, 4 activities, 10 apr_levels, 33 acc_mappings (all with
control types), 8 visa types, trv_booking_notes.note_text = text.
