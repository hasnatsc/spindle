-- ================================================================
--  SpindleERP — BUDGET MODULE
--  Addendum to SpindleERP_Master_Schema.sql
--
--  PostgreSQL 15+  |  com.asg.spindleserp  |  June 2026
--
--  Tables (12):
--    bgt_fiscal_years          — Financial year master
--    bgt_budget_heads          — Hierarchical budget line classification
--    bgt_budgets               — Budget document header (annual/quarterly/monthly/project)
--    bgt_budget_lines          — Budget allocation per head + account + cost centre
--    bgt_budget_revisions      — Amendment history (audit trail)
--    bgt_budget_revision_lines — Line-level revision details
--    bgt_actuals               — Auto-fed actuals from GL journal entries
--    bgt_encumbrances          — Committed but not yet spent (PO / contract)
--    bgt_transfers             — Budget reallocation between lines
--    bgt_alerts                — Configurable over-spend / threshold alerts
--    bgt_approval_policies     — Who approves which budget type/amount
--    bgt_budget_notes          — Collaboration notes on budgets
--
--  Views (3):
--    v_budget_vs_actual        — Live budget utilisation per line
--    v_budget_variance         — Variance analysis with % consumed
--    v_encumbrance_summary     — Committed + actual vs budget remaining
--
--  Relationships:
--    bgt_budgets          → org_organizations
--    bgt_budgets          → org_business_units
--    bgt_budget_lines     → acc_chart_of_accounts
--    bgt_budget_lines     → acc_cost_centers
--    bgt_budget_lines     → org_departments
--    bgt_actuals          → acc_journal_entries  (auto-posted from GL)
--    bgt_encumbrances     → global_business_documents (PO / SR)
--    bgt_alerts           → sec_users  (notify)
-- ================================================================

-- ================================================================
--  TABLE 1 — FISCAL YEARS
-- ================================================================
CREATE TABLE bgt_fiscal_years (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    year_code           VARCHAR(20)     NOT NULL,    -- e.g. FY2025-26
    year_name           VARCHAR(100)    NOT NULL,    -- e.g. Financial Year 2025-2026
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | ACTIVE | LOCKED | CLOSED
    is_current          BOOLEAN         NOT NULL DEFAULT FALSE,
    closed_by           VARCHAR(100),
    closed_at           TIMESTAMPTZ,
    notes               TEXT,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, year_code),
    CONSTRAINT chk_fy_dates     CHECK (end_date > start_date),
    CONSTRAINT chk_fy_status    CHECK (status IN ('DRAFT','ACTIVE','LOCKED','CLOSED'))
);

-- ================================================================
--  TABLE 2 — BUDGET HEADS  (hierarchical classification tree)
-- ================================================================
CREATE TABLE bgt_budget_heads (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    parent_id           BIGINT          REFERENCES bgt_budget_heads(id),
    head_code           VARCHAR(50)     NOT NULL,
    head_name           VARCHAR(200)    NOT NULL,
    head_type           VARCHAR(30)     NOT NULL DEFAULT 'EXPENSE',
    -- REVENUE | EXPENSE | CAPEX | OPEX | PRODUCTION | HR | COMMERCIAL | OTHER
    description         TEXT,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    display_order       INT             NOT NULL DEFAULT 0,
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, head_code),
    CONSTRAINT chk_bgt_head_type CHECK (head_type IN (
        'REVENUE','EXPENSE','CAPEX','OPEX','PRODUCTION','HR','COMMERCIAL','OTHER'))
);

-- ================================================================
--  TABLE 3 — BUDGET HEADERS
-- ================================================================
CREATE TABLE bgt_budgets (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    business_unit_id    BIGINT          REFERENCES org_business_units(id),
    fiscal_year_id      BIGINT          NOT NULL REFERENCES bgt_fiscal_years(id),

    budget_no           VARCHAR(50)     NOT NULL,    -- e.g. BGT-2526-0001
    budget_name         VARCHAR(200)    NOT NULL,
    description         TEXT,

    budget_type         VARCHAR(30)     NOT NULL DEFAULT 'ANNUAL',
    -- ANNUAL | QUARTERLY | MONTHLY | PROJECT | DEPARTMENTAL | CAPEX | ROLLING

    period_type         VARCHAR(20)     NOT NULL DEFAULT 'ANNUAL',
    -- ANNUAL | Q1 | Q2 | Q3 | Q4 | JAN..DEC | PROJECT

    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,

    currency            CHAR(3)         NOT NULL DEFAULT 'BDT',
    exchange_rate       NUMERIC(18,4)   NOT NULL DEFAULT 1,

    -- ── Totals (auto-calculated from lines) ─────────────────────
    total_budgeted      NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_revised       NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_actual        NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_committed     NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_available     NUMERIC(18,2)   NOT NULL DEFAULT 0,

    -- ── Status & Approval ────────────────────────────────────────
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | SUBMITTED | IN_APPROVAL | APPROVED | ACTIVE | LOCKED | CLOSED | REJECTED | RETURNED

    approval_status     VARCHAR(30)     DEFAULT 'DRAFT',
    approval_request_id BIGINT          REFERENCES apr_requests(id),

    -- ── Over-spend policy ────────────────────────────────────────
    over_spend_policy   VARCHAR(20)     NOT NULL DEFAULT 'WARN',
    -- ALLOW | WARN | BLOCK
    -- ALLOW  = over-spend permitted without warning
    -- WARN   = over-spend allowed but triggers alert
    -- BLOCK  = transactions blocked when budget exhausted

    alert_threshold_pct NUMERIC(5,2)   DEFAULT 80,   -- alert at 80% consumed
    allow_inter_line_transfer BOOLEAN  NOT NULL DEFAULT FALSE,

    -- ── Meta ─────────────────────────────────────────────────────
    version             INT             NOT NULL DEFAULT 1,   -- increments on each revision
    is_template         BOOLEAN         NOT NULL DEFAULT FALSE,

    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    UNIQUE (organization_id, budget_no),
    CONSTRAINT chk_bgt_dates    CHECK (period_end >= period_start),
    CONSTRAINT chk_bgt_type     CHECK (budget_type IN (
        'ANNUAL','QUARTERLY','MONTHLY','PROJECT','DEPARTMENTAL','CAPEX','ROLLING')),
    CONSTRAINT chk_bgt_status   CHECK (status IN (
        'DRAFT','SUBMITTED','IN_APPROVAL','APPROVED','ACTIVE','LOCKED','CLOSED','REJECTED','RETURNED')),
    CONSTRAINT chk_bgt_policy   CHECK (over_spend_policy IN ('ALLOW','WARN','BLOCK'))
);

-- ================================================================
--  TABLE 4 — BUDGET LINES
-- ================================================================
CREATE TABLE bgt_budget_lines (
    id                  BIGSERIAL       PRIMARY KEY,
    budget_id           BIGINT          NOT NULL REFERENCES bgt_budgets(id) ON DELETE CASCADE,
    budget_head_id      BIGINT          NOT NULL REFERENCES bgt_budget_heads(id),
    account_id          BIGINT          REFERENCES acc_chart_of_accounts(id),
    cost_center_id      BIGINT          REFERENCES acc_cost_centers(id),
    department_id       BIGINT          REFERENCES org_departments(id),

    line_number         INT             NOT NULL,
    description         VARCHAR(500),

    -- ── Budget amounts ──────────────────────────────────────────
    original_amount     NUMERIC(18,2)   NOT NULL DEFAULT 0,   -- initial budget
    revised_amount      NUMERIC(18,2)   NOT NULL DEFAULT 0,   -- after revisions
    -- revised_amount = original_amount + SUM(revision deltas for this line)

    -- ── Actuals (updated by trigger/service from GL) ─────────────
    actual_amount       NUMERIC(18,2)   NOT NULL DEFAULT 0,
    committed_amount    NUMERIC(18,2)   NOT NULL DEFAULT 0,   -- from PO encumbrances
    available_amount    NUMERIC(18,2)   GENERATED ALWAYS AS
        (revised_amount - actual_amount - committed_amount) STORED,

    -- ── Monthly phasing (12 columns for monthly budget profiles) ─
    jan_amount          NUMERIC(18,2)   DEFAULT 0,
    feb_amount          NUMERIC(18,2)   DEFAULT 0,
    mar_amount          NUMERIC(18,2)   DEFAULT 0,
    apr_amount          NUMERIC(18,2)   DEFAULT 0,
    may_amount          NUMERIC(18,2)   DEFAULT 0,
    jun_amount          NUMERIC(18,2)   DEFAULT 0,
    jul_amount          NUMERIC(18,2)   DEFAULT 0,
    aug_amount          NUMERIC(18,2)   DEFAULT 0,
    sep_amount          NUMERIC(18,2)   DEFAULT 0,
    oct_amount          NUMERIC(18,2)   DEFAULT 0,
    nov_amount          NUMERIC(18,2)   DEFAULT 0,
    dec_amount          NUMERIC(18,2)   DEFAULT 0,
    -- SUM(jan..dec) should equal original_amount for annual budgets

    notes               TEXT,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    UNIQUE (budget_id, budget_head_id, account_id, cost_center_id, department_id)
);

-- ================================================================
--  TABLE 5 — BUDGET REVISIONS (amendment header)
-- ================================================================
CREATE TABLE bgt_budget_revisions (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    budget_id           BIGINT          NOT NULL REFERENCES bgt_budgets(id),
    revision_no         VARCHAR(50)     NOT NULL,     -- e.g. BGT-REV-2526-0001
    revision_number     INT             NOT NULL,      -- 1, 2, 3 …
    revision_type       VARCHAR(30)     NOT NULL DEFAULT 'REALLOCATION',
    -- REALLOCATION | SUPPLEMENTARY | REDUCTION | TECHNICAL

    reason              TEXT            NOT NULL,
    justification       TEXT,
    total_increase      NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_decrease      NUMERIC(18,2)   NOT NULL DEFAULT 0,

    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | SUBMITTED | IN_APPROVAL | APPROVED | REJECTED

    approval_request_id BIGINT          REFERENCES apr_requests(id),
    approved_by         VARCHAR(100),
    approved_at         TIMESTAMPTZ,

    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    UNIQUE (budget_id, revision_number),
    CONSTRAINT chk_rev_type   CHECK (revision_type IN (
        'REALLOCATION','SUPPLEMENTARY','REDUCTION','TECHNICAL')),
    CONSTRAINT chk_rev_status CHECK (status IN (
        'DRAFT','SUBMITTED','IN_APPROVAL','APPROVED','REJECTED'))
);

-- ================================================================
--  TABLE 6 — BUDGET REVISION LINES
-- ================================================================
CREATE TABLE bgt_budget_revision_lines (
    id                  BIGSERIAL       PRIMARY KEY,
    revision_id         BIGINT          NOT NULL REFERENCES bgt_budget_revisions(id) ON DELETE CASCADE,
    budget_line_id      BIGINT          NOT NULL REFERENCES bgt_budget_lines(id),
    direction           CHAR(1)         NOT NULL,   -- '+' increase  |  '-' decrease
    change_amount       NUMERIC(18,2)   NOT NULL,
    opening_amount      NUMERIC(18,2)   NOT NULL,   -- revised_amount before this change
    closing_amount      NUMERIC(18,2)   NOT NULL,   -- revised_amount after this change
    reason              TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rev_direction CHECK (direction IN ('+','-'))
);

-- ================================================================
--  TABLE 7 — ACTUALS  (auto-fed from GL journal entries)
-- ================================================================
--  Every posted journal entry that touches a cost centre or account
--  that has a budget line generates one row here.
--  The service layer calls fn_post_budget_actual() after GL posting.
CREATE TABLE bgt_actuals (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    budget_id           BIGINT          NOT NULL REFERENCES bgt_budgets(id),
    budget_line_id      BIGINT          NOT NULL REFERENCES bgt_budget_lines(id),
    journal_entry_id    BIGINT          NOT NULL REFERENCES acc_journal_entries(id),
    journal_entry_line_id BIGINT        REFERENCES acc_journal_entry_lines(id),
    -- Source document that created the GL entry
    source_document_type VARCHAR(50),   -- maps to DocumentType enum
    source_document_id   BIGINT,        -- polymorphic ref to global_business_documents.id
    source_document_no   VARCHAR(100),

    transaction_date    DATE            NOT NULL,
    debit_amount        NUMERIC(18,2)   NOT NULL DEFAULT 0,
    credit_amount       NUMERIC(18,2)   NOT NULL DEFAULT 0,
    net_amount          NUMERIC(18,2)   NOT NULL DEFAULT 0,  -- debit - credit
    narration           VARCHAR(500),

    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ================================================================
--  TABLE 8 — ENCUMBRANCES (committed but not yet spent)
-- ================================================================
--  Created when a Purchase Order or contract is APPROVED.
--  Released (or reduced) when the matching Purchase Invoice is posted.
CREATE TABLE bgt_encumbrances (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    budget_id           BIGINT          NOT NULL REFERENCES bgt_budgets(id),
    budget_line_id      BIGINT          NOT NULL REFERENCES bgt_budget_lines(id),

    -- Source document creating the commitment
    source_document_type VARCHAR(50)    NOT NULL,   -- PURCHASE_ORDER | SALES_ORDER | CONTRACT
    source_document_id   BIGINT         NOT NULL REFERENCES global_business_documents(id),
    source_document_no   VARCHAR(100)   NOT NULL,

    committed_amount    NUMERIC(18,2)   NOT NULL,
    released_amount     NUMERIC(18,2)   NOT NULL DEFAULT 0,
    outstanding_amount  NUMERIC(18,2)   GENERATED ALWAYS AS
        (committed_amount - released_amount) STORED,

    commitment_date     DATE            NOT NULL,
    expected_invoice_date DATE,

    status              VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    -- OPEN | PARTIAL | FULLY_RELEASED | CANCELLED

    notes               TEXT,
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_enc_status CHECK (status IN ('OPEN','PARTIAL','FULLY_RELEASED','CANCELLED'))
);

-- ================================================================
--  TABLE 9 — BUDGET TRANSFERS  (reallocation between lines)
-- ================================================================
CREATE TABLE bgt_transfers (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    budget_id           BIGINT          NOT NULL REFERENCES bgt_budgets(id),
    transfer_no         VARCHAR(50)     NOT NULL,
    from_line_id        BIGINT          NOT NULL REFERENCES bgt_budget_lines(id),
    to_line_id          BIGINT          NOT NULL REFERENCES bgt_budget_lines(id),
    transfer_amount     NUMERIC(18,2)   NOT NULL,
    reason              TEXT            NOT NULL,
    transfer_date       DATE            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    -- PENDING | APPROVED | REJECTED
    approved_by         VARCHAR(100),
    approved_at         TIMESTAMPTZ,
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    UNIQUE (organization_id, transfer_no),
    CONSTRAINT chk_xfr_diff_lines CHECK (from_line_id <> to_line_id),
    CONSTRAINT chk_xfr_positive   CHECK (transfer_amount > 0)
);

-- ================================================================
--  TABLE 10 — BUDGET ALERTS
-- ================================================================
CREATE TABLE bgt_alerts (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    budget_id           BIGINT          NOT NULL REFERENCES bgt_budgets(id),
    budget_line_id      BIGINT          REFERENCES bgt_budget_lines(id),  -- NULL = entire budget
    notify_user_id      BIGINT          REFERENCES sec_users(id),

    alert_type          VARCHAR(30)     NOT NULL,
    -- THRESHOLD_WARNING      = % consumed crossed threshold
    -- OVER_BUDGET            = actual + committed > revised
    -- ENCUMBRANCE_EXPIRY     = PO expected invoice date overdue
    -- BUDGET_EXPIRY          = fiscal year / budget period ending soon

    threshold_pct       NUMERIC(5,2),   -- for THRESHOLD_WARNING
    message             TEXT,
    triggered_at        TIMESTAMPTZ,
    is_resolved         BOOLEAN         NOT NULL DEFAULT FALSE,
    resolved_at         TIMESTAMPTZ,
    resolved_by         VARCHAR(100),
    notification_sent   BOOLEAN         NOT NULL DEFAULT FALSE,
    sent_at             TIMESTAMPTZ,

    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_alert_type CHECK (alert_type IN (
        'THRESHOLD_WARNING','OVER_BUDGET','ENCUMBRANCE_EXPIRY','BUDGET_EXPIRY'))
);

-- ================================================================
--  TABLE 11 — BUDGET APPROVAL POLICIES
-- ================================================================
CREATE TABLE bgt_approval_policies (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    policy_name         VARCHAR(100)    NOT NULL,
    budget_type         VARCHAR(30),    -- NULL = applies to all types
    min_amount          NUMERIC(18,2)   DEFAULT 0,
    max_amount          NUMERIC(18,2),  -- NULL = no upper limit
    approval_config_id  BIGINT          NOT NULL REFERENCES apr_configs(id),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ================================================================
--  TABLE 12 — BUDGET NOTES  (collaboration)
-- ================================================================
CREATE TABLE bgt_budget_notes (
    id                  BIGSERIAL       PRIMARY KEY,
    budget_id           BIGINT          NOT NULL REFERENCES bgt_budgets(id) ON DELETE CASCADE,
    noted_by_user_id    BIGINT          REFERENCES sec_users(id),
    note_type           VARCHAR(20)     NOT NULL DEFAULT 'COMMENT',
    -- COMMENT | QUERY | ACTION_ITEM | APPROVAL_NOTE
    note_text           TEXT            NOT NULL,
    is_internal         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ================================================================
--  INDEXES
-- ================================================================

CREATE INDEX idx_bgt_fy_org          ON bgt_fiscal_years(organization_id);
CREATE INDEX idx_bgt_fy_current      ON bgt_fiscal_years(organization_id, is_current) WHERE is_current = TRUE;

CREATE INDEX idx_bgt_head_org        ON bgt_budget_heads(organization_id);
CREATE INDEX idx_bgt_head_parent     ON bgt_budget_heads(parent_id);
CREATE INDEX idx_bgt_head_type       ON bgt_budget_heads(head_type);

CREATE INDEX idx_bgt_org             ON bgt_budgets(organization_id);
CREATE INDEX idx_bgt_fy              ON bgt_budgets(fiscal_year_id);
CREATE INDEX idx_bgt_bu              ON bgt_budgets(business_unit_id);
CREATE INDEX idx_bgt_status          ON bgt_budgets(status);
CREATE INDEX idx_bgt_type            ON bgt_budgets(budget_type);
CREATE INDEX idx_bgt_apr             ON bgt_budgets(approval_request_id);

CREATE INDEX idx_bgtl_budget         ON bgt_budget_lines(budget_id);
CREATE INDEX idx_bgtl_head           ON bgt_budget_lines(budget_head_id);
CREATE INDEX idx_bgtl_account        ON bgt_budget_lines(account_id);
CREATE INDEX idx_bgtl_cc             ON bgt_budget_lines(cost_center_id);
CREATE INDEX idx_bgtl_dept           ON bgt_budget_lines(department_id);

CREATE INDEX idx_bgt_rev_budget      ON bgt_budget_revisions(budget_id);
CREATE INDEX idx_bgt_rev_status      ON bgt_budget_revisions(status);
CREATE INDEX idx_bgt_revl_rev        ON bgt_budget_revision_lines(revision_id);

CREATE INDEX idx_bgt_act_budget      ON bgt_actuals(budget_id);
CREATE INDEX idx_bgt_act_line        ON bgt_actuals(budget_line_id);
CREATE INDEX idx_bgt_act_je          ON bgt_actuals(journal_entry_id);
CREATE INDEX idx_bgt_act_date        ON bgt_actuals(transaction_date);
CREATE INDEX idx_bgt_act_src         ON bgt_actuals(source_document_type, source_document_id);

CREATE INDEX idx_bgt_enc_budget      ON bgt_encumbrances(budget_id);
CREATE INDEX idx_bgt_enc_line        ON bgt_encumbrances(budget_line_id);
CREATE INDEX idx_bgt_enc_doc         ON bgt_encumbrances(source_document_id);
CREATE INDEX idx_bgt_enc_status      ON bgt_encumbrances(status);

CREATE INDEX idx_bgt_xfr_budget      ON bgt_transfers(budget_id);
CREATE INDEX idx_bgt_xfr_from        ON bgt_transfers(from_line_id);
CREATE INDEX idx_bgt_xfr_to          ON bgt_transfers(to_line_id);

CREATE INDEX idx_bgt_alert_budget    ON bgt_alerts(budget_id);
CREATE INDEX idx_bgt_alert_user      ON bgt_alerts(notify_user_id);
CREATE INDEX idx_bgt_alert_unsent    ON bgt_alerts(notification_sent, triggered_at) WHERE notification_sent = FALSE;

CREATE INDEX idx_bgt_notes_budget    ON bgt_budget_notes(budget_id);

-- ================================================================
--  VIEWS
-- ================================================================

-- ── 1. Budget vs Actual (live utilisation per line) ─────────────
CREATE OR REPLACE VIEW v_budget_vs_actual AS
SELECT
    b.organization_id,
    b.id                            AS budget_id,
    b.budget_no,
    b.budget_name,
    b.budget_type,
    b.period_start,
    b.period_end,
    b.status                        AS budget_status,
    bl.id                           AS line_id,
    bh.head_code,
    bh.head_name,
    bh.head_type,
    acct.account_code,
    acct.account_name,
    cc.code                         AS cost_center_code,
    cc.name                         AS cost_center_name,
    dept.name                       AS department_name,
    bl.original_amount,
    bl.revised_amount,
    bl.actual_amount,
    bl.committed_amount,
    bl.available_amount,
    CASE WHEN bl.revised_amount > 0
         THEN ROUND((bl.actual_amount / bl.revised_amount) * 100, 2)
         ELSE 0 END                 AS actual_utilisation_pct,
    CASE WHEN bl.revised_amount > 0
         THEN ROUND(((bl.actual_amount + bl.committed_amount) / bl.revised_amount) * 100, 2)
         ELSE 0 END                 AS total_utilisation_pct,
    CASE
        WHEN bl.available_amount < 0                        THEN 'OVER_BUDGET'
        WHEN bl.actual_amount + bl.committed_amount >=
             bl.revised_amount * COALESCE(b.alert_threshold_pct,80) / 100
                                                            THEN 'AT_RISK'
        ELSE                                                     'ON_TRACK'
    END                             AS health_status
FROM  bgt_budget_lines bl
JOIN  bgt_budgets      b    ON b.id    = bl.budget_id
JOIN  bgt_budget_heads bh   ON bh.id   = bl.budget_head_id
LEFT  JOIN acc_chart_of_accounts acct ON acct.id = bl.account_id
LEFT  JOIN acc_cost_centers      cc   ON cc.id   = bl.cost_center_id
LEFT  JOIN org_departments       dept ON dept.id  = bl.department_id;

-- ── 2. Budget Variance Analysis ─────────────────────────────────
CREATE OR REPLACE VIEW v_budget_variance AS
SELECT
    bva.organization_id,
    bva.budget_id,
    bva.budget_no,
    bva.budget_name,
    bva.period_start,
    bva.period_end,
    bva.head_code,
    bva.head_name,
    bva.head_type,
    bva.cost_center_code,
    bva.department_name,
    bva.original_amount,
    bva.revised_amount,
    bva.actual_amount,
    (bva.revised_amount - bva.original_amount) AS revision_delta,
    (bva.actual_amount  - bva.revised_amount)  AS variance_amount,
    -- Positive variance = over budget; Negative = under budget
    CASE WHEN bva.revised_amount <> 0
         THEN ROUND(((bva.actual_amount - bva.revised_amount)
                      / bva.revised_amount) * 100, 2)
         ELSE 0 END                            AS variance_pct,
    bva.health_status
FROM  v_budget_vs_actual bva;

-- ── 3. Encumbrance Summary ───────────────────────────────────────
CREATE OR REPLACE VIEW v_encumbrance_summary AS
SELECT
    enc.organization_id,
    enc.budget_id,
    enc.budget_line_id,
    bh.head_code,
    bh.head_name,
    enc.source_document_type,
    enc.source_document_no,
    enc.committed_amount,
    enc.released_amount,
    enc.outstanding_amount,
    enc.commitment_date,
    enc.expected_invoice_date,
    enc.status                                       AS encumbrance_status,
    CASE WHEN enc.expected_invoice_date < CURRENT_DATE
              AND enc.status = 'OPEN'
         THEN TRUE ELSE FALSE END                    AS is_overdue,
    gbd.party_id,
    sub.sub_account_name                             AS supplier_name
FROM  bgt_encumbrances enc
JOIN  bgt_budget_lines  bl  ON bl.id  = enc.budget_line_id
JOIN  bgt_budget_heads  bh  ON bh.id  = bl.budget_head_id
JOIN  global_business_documents gbd ON gbd.id = enc.source_document_id
LEFT  JOIN acc_chart_of_accounts_sub sub ON sub.id = gbd.party_id;

-- ================================================================
--  FUNCTIONS
-- ================================================================

-- ── Auto-update updated_at for budget tables ─────────────────────
DO $$
DECLARE tbl TEXT;
BEGIN
  FOREACH tbl IN ARRAY ARRAY[
    'bgt_fiscal_years','bgt_budget_heads','bgt_budgets',
    'bgt_budget_lines','bgt_budget_revisions',
    'bgt_encumbrances','bgt_approval_policies'
  ] LOOP
    EXECUTE format(
      'CREATE TRIGGER trg_%s_updated_at
       BEFORE UPDATE ON %s
       FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at()', tbl, tbl);
  END LOOP;
END;
$$;

-- ── Document sequence for budget numbers ─────────────────────────
-- Uses existing fn_next_doc_no() with prefix 'BGT'
-- Example: SELECT fn_next_doc_no(1, 'BGT', 4);  →  BGT-2506-0001

-- ── Post actual from GL journal entry ────────────────────────────
-- Called by BudgetService.postActual() after acc_journal_entries posting.
CREATE OR REPLACE FUNCTION fn_post_budget_actual(
    p_journal_entry_id       BIGINT,
    p_journal_entry_line_id  BIGINT,
    p_budget_line_id         BIGINT,
    p_net_amount             NUMERIC,
    p_source_doc_type        VARCHAR,
    p_source_doc_id          BIGINT,
    p_source_doc_no          VARCHAR,
    p_transaction_date       DATE,
    p_narration              VARCHAR,
    p_created_by             VARCHAR
) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    v_budget_id BIGINT;
    v_org_id    BIGINT;
BEGIN
    -- Resolve budget and org from line
    SELECT bl.budget_id, b.organization_id
    INTO   v_budget_id, v_org_id
    FROM   bgt_budget_lines bl
    JOIN   bgt_budgets      b  ON b.id = bl.budget_id
    WHERE  bl.id = p_budget_line_id;

    IF v_budget_id IS NULL THEN RETURN; END IF;

    -- Insert actual record
    INSERT INTO bgt_actuals (
        organization_id, budget_id, budget_line_id,
        journal_entry_id, journal_entry_line_id,
        source_document_type, source_document_id, source_document_no,
        transaction_date,
        debit_amount, credit_amount, net_amount,
        narration, created_by)
    VALUES (
        v_org_id, v_budget_id, p_budget_line_id,
        p_journal_entry_id, p_journal_entry_line_id,
        p_source_doc_type, p_source_doc_id, p_source_doc_no,
        COALESCE(p_transaction_date, CURRENT_DATE),
        GREATEST(p_net_amount, 0),
        GREATEST(-p_net_amount, 0),
        p_net_amount,
        p_narration, p_created_by);

    -- Update running actual on the budget line
    UPDATE bgt_budget_lines
    SET    actual_amount = actual_amount + p_net_amount,
           updated_at    = NOW()
    WHERE  id = p_budget_line_id;

    -- Update budget header total
    UPDATE bgt_budgets
    SET    total_actual   = total_actual + p_net_amount,
           total_available = total_revised - (total_actual + p_net_amount) - total_committed,
           updated_at     = NOW()
    WHERE  id = v_budget_id;

    -- Fire alert if threshold breached
    PERFORM fn_check_budget_threshold(p_budget_line_id);
END;
$$;

-- ── Create encumbrance from approved PO ──────────────────────────
CREATE OR REPLACE FUNCTION fn_create_encumbrance(
    p_budget_line_id     BIGINT,
    p_source_doc_id      BIGINT,
    p_source_doc_type    VARCHAR,
    p_source_doc_no      VARCHAR,
    p_committed_amount   NUMERIC,
    p_commitment_date    DATE,
    p_expected_inv_date  DATE,
    p_created_by         VARCHAR
) RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    v_budget_id BIGINT;
    v_org_id    BIGINT;
    v_enc_id    BIGINT;
BEGIN
    SELECT bl.budget_id, b.organization_id
    INTO   v_budget_id, v_org_id
    FROM   bgt_budget_lines bl
    JOIN   bgt_budgets b ON b.id = bl.budget_id
    WHERE  bl.id = p_budget_line_id;

    INSERT INTO bgt_encumbrances (
        organization_id, budget_id, budget_line_id,
        source_document_type, source_document_id, source_document_no,
        committed_amount, commitment_date, expected_invoice_date,
        status, created_by)
    VALUES (
        v_org_id, v_budget_id, p_budget_line_id,
        p_source_doc_type, p_source_doc_id, p_source_doc_no,
        p_committed_amount, p_commitment_date, p_expected_inv_date,
        'OPEN', p_created_by)
    RETURNING id INTO v_enc_id;

    -- Update committed on budget line
    UPDATE bgt_budget_lines
    SET    committed_amount = committed_amount + p_committed_amount,
           updated_at       = NOW()
    WHERE  id = p_budget_line_id;

    UPDATE bgt_budgets
    SET    total_committed  = total_committed + p_committed_amount,
           total_available  = total_revised - total_actual - (total_committed + p_committed_amount),
           updated_at       = NOW()
    WHERE  id = v_budget_id;

    RETURN v_enc_id;
END;
$$;

-- ── Release encumbrance on invoice posting ────────────────────────
CREATE OR REPLACE FUNCTION fn_release_encumbrance(
    p_encumbrance_id    BIGINT,
    p_release_amount    NUMERIC
) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    v_enc   bgt_encumbrances%ROWTYPE;
    v_new_released NUMERIC;
BEGIN
    SELECT * INTO v_enc FROM bgt_encumbrances WHERE id = p_encumbrance_id FOR UPDATE;
    v_new_released := v_enc.released_amount + p_release_amount;

    UPDATE bgt_encumbrances
    SET    released_amount = v_new_released,
           status          = CASE WHEN v_new_released >= v_enc.committed_amount
                                  THEN 'FULLY_RELEASED'
                                  ELSE 'PARTIAL' END,
           updated_at      = NOW()
    WHERE  id = p_encumbrance_id;

    -- Reduce committed on budget line
    UPDATE bgt_budget_lines
    SET    committed_amount = GREATEST(0, committed_amount - p_release_amount),
           updated_at       = NOW()
    WHERE  id = v_enc.budget_line_id;

    UPDATE bgt_budgets
    SET    total_committed  = GREATEST(0, total_committed - p_release_amount),
           total_available  = total_revised - total_actual
                              - GREATEST(0, total_committed - p_release_amount),
           updated_at       = NOW()
    WHERE  id = v_enc.budget_id;
END;
$$;

-- ── Threshold alert check ─────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_check_budget_threshold(
    p_budget_line_id BIGINT
) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    v_line  bgt_budget_lines%ROWTYPE;
    v_bgt   bgt_budgets%ROWTYPE;
    v_pct   NUMERIC;
BEGIN
    SELECT * INTO v_line FROM bgt_budget_lines WHERE id = p_budget_line_id;
    SELECT * INTO v_bgt  FROM bgt_budgets      WHERE id = v_line.budget_id;

    IF v_line.revised_amount = 0 THEN RETURN; END IF;

    v_pct := ((v_line.actual_amount + v_line.committed_amount) / v_line.revised_amount) * 100;

    -- OVER_BUDGET alert
    IF v_line.available_amount < 0 THEN
        INSERT INTO bgt_alerts (
            organization_id, budget_id, budget_line_id,
            alert_type, message, triggered_at)
        VALUES (
            v_bgt.organization_id, v_bgt.id, p_budget_line_id,
            'OVER_BUDGET',
            'Budget line exceeded. Available: ' || v_line.available_amount::TEXT,
            NOW())
        ON CONFLICT DO NOTHING;
    -- THRESHOLD alert
    ELSIF v_pct >= COALESCE(v_bgt.alert_threshold_pct, 80) THEN
        INSERT INTO bgt_alerts (
            organization_id, budget_id, budget_line_id,
            alert_type, threshold_pct, message, triggered_at)
        VALUES (
            v_bgt.organization_id, v_bgt.id, p_budget_line_id,
            'THRESHOLD_WARNING', v_pct,
            ROUND(v_pct,1) || '% of budget consumed',
            NOW())
        ON CONFLICT DO NOTHING;
    END IF;
END;
$$;

-- ================================================================
--  DocumentType enum extension  (add to Java enum)
-- ================================================================
-- Add these to DocumentType.java:
--   BUDGET("BGT",          "Budget"),
--   BUDGET_REVISION("BGTR","Budget Revision"),
--   BUDGET_TRANSFER("BGTT","Budget Transfer"),

-- ================================================================
--  ModuleType enum extension  (add to Java enum)
-- ================================================================
-- Add to ModuleType.java:
--   BUDGET_MANAGEMENT("Budget Management"),

-- ================================================================
--  SUMMARY
-- ================================================================
--
--  Tables       : 12  (bgt_*)
--  Indexes      : 25
--  Views        : 3   (v_budget_vs_actual, v_budget_variance, v_encumbrance_summary)
--  Functions    : 5   (fn_post_budget_actual, fn_create_encumbrance,
--                      fn_release_encumbrance, fn_check_budget_threshold,
--                      fn_set_updated_at trigger on 7 tables)
--  Triggers     : 7 tables auto-timestamped
--
--  Key design decisions:
--  ─────────────────────────────────────────────────────────────────
--  A  12-column monthly phasing on bgt_budget_lines — enables
--     profile-based monthly budget splits without extra rows.
--
--  B  available_amount is a GENERATED ALWAYS AS column —
--     always = revised - actual - committed; never stale.
--
--  C  bgt_actuals is immutable (no updated_at) — every GL posting
--     creates a new row; the running total on bgt_budget_lines is
--     the authoritative current balance.
--
--  D  Encumbrances integrate with the existing PO approval flow:
--     fn_create_encumbrance() is called by PurchaseOrderService.onApproved()
--     fn_release_encumbrance() is called by PurchaseInvoiceService.onApproved()
--
--  E  over_spend_policy on bgt_budgets controls system behaviour:
--     BLOCK  → BudgetService.checkBudgetAvailability() throws before PO save
--     WARN   → alert created but transaction proceeds
--     ALLOW  → no check performed
-- ================================================================
