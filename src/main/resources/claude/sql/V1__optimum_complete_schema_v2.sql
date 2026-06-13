-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║  OPTIMUM ERP  ·  GENERIC EDITION  ·  v2.0                                  ║
-- ║  Package   : com.hasnat.optimum                                              ║
-- ║  Database  : PostgreSQL 15+                                                  ║
-- ║  Modules   : 15  (Yarn/Spinning REMOVED, Generic Production ADDED)          ║
-- ║  Tables    : 103  Indexes: ~220  Views: 8                                    ║
-- ║                                                                              ║
-- ║  CHANGES FROM v1:                                                            ║
-- ║  ✗ REMOVED  yrn_types, yrn_counts, yarn_products, yarn_blend_items          ║
-- ║  ✗ REMOVED  prd_orders, prd_recipes, prd_recipe_items, prd_recipe_item_lots ║
-- ║  ✗ REMOVED  fiber-specific columns from inv_items                            ║
-- ║  ✗ REMOVED  yarn QC columns from global_inv_lots                            ║
-- ║  ✓ UPDATED  item_type enum → generic (RAW_MATERIAL, FINISHED_GOOD, etc.)    ║
-- ║  ✓ ADDED    prd_bom, prd_bom_items (reusable Bill of Materials)             ║
-- ║  ✓ ADDED    prd_productions (generic work order + cost sheet)               ║
-- ║  ✓ ADDED    prd_production_inputs (materials consumed + lots)               ║
-- ║  ✓ ADDED    prd_production_outputs (finished goods produced + lots)         ║
-- ║  ✓ ADDED    hrm_cost_center_allocations (labor cost → production)           ║
-- ║  ✓ ADDED    v_production_cost_sheet, v_cogs_summary views                  ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 01 — CORE / SECURITY
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE org_organizations (
    id               BIGSERIAL    PRIMARY KEY,
    code             VARCHAR(50)  NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    name_bn          VARCHAR(200),
    about            TEXT,
    address          TEXT,
    city             VARCHAR(100),
    state            VARCHAR(100),
    country          VARCHAR(100),
    postal_code      VARCHAR(20),
    phone            VARCHAR(20),
    email            VARCHAR(100),
    website          VARCHAR(255),
    logo_url         VARCHAR(500),
    established_date DATE,
    tax_id           VARCHAR(50),
    vat_no           VARCHAR(50),
    bin_no           VARCHAR(50),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6)
);
CREATE INDEX idx_org_code   ON org_organizations(code);
CREATE INDEX idx_org_active ON org_organizations(is_active);

CREATE TABLE sec_roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    name_bn     VARCHAR(250),
    description VARCHAR(255),
    master_role VARCHAR(60),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(6),
    updated_at  TIMESTAMP(6)
);

CREATE TABLE sec_permissions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    url_pattern VARCHAR(255),
    http_method VARCHAR(10),
    category    VARCHAR(50),
    module      VARCHAR(80),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(6),
    updated_at  TIMESTAMP(6)
);
CREATE INDEX idx_perm_name   ON sec_permissions(name);
CREATE INDEX idx_perm_module ON sec_permissions(module);

CREATE TABLE sec_users (
    id                         BIGSERIAL    PRIMARY KEY,
    organization_id            BIGINT       NOT NULL REFERENCES org_organizations(id),
    username                   VARCHAR(80)  NOT NULL UNIQUE,
    email                      VARCHAR(150) NOT NULL UNIQUE,
    phone                      VARCHAR(30)  NOT NULL UNIQUE,
    password                   VARCHAR(255) NOT NULL,
    full_name                  VARCHAR(200),
    enabled                    BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked         BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_expired        BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired    BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted                    BOOLEAN      NOT NULL DEFAULT FALSE,
    default_dashboard          VARCHAR(30),
    last_login_at              TIMESTAMP(6),
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100),
    created_at                 TIMESTAMP(6),
    updated_at                 TIMESTAMP(6)
);
CREATE INDEX idx_user_org      ON sec_users(organization_id);
CREATE INDEX idx_user_username ON sec_users(username);
CREATE INDEX idx_user_email    ON sec_users(email);
CREATE INDEX idx_user_deleted  ON sec_users(deleted);

CREATE TABLE sec_user_roles (
    user_id BIGINT NOT NULL REFERENCES sec_users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sec_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sec_role_permissions (
    role_id       BIGINT NOT NULL REFERENCES sec_roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES sec_permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE app_menus (
    id                  BIGSERIAL    PRIMARY KEY,
    menu_code           VARCHAR(80)  NOT NULL UNIQUE,
    menu_name           VARCHAR(120) NOT NULL,
    menu_url            VARCHAR(300),
    icon                VARCHAR(100),
    parent_id           BIGINT,
    display_order       INT          NOT NULL DEFAULT 0,
    menu_type           VARCHAR(20)  NOT NULL DEFAULT 'LEAF'
        CONSTRAINT chk_menu_type CHECK (menu_type IN ('MODULE','GROUP','LEAF')),
    module_name         VARCHAR(80),
    required_permission VARCHAR(120),
    description         VARCHAR(255),
    target              VARCHAR(20)  DEFAULT '_self',
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    visible             BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMP(6),
    updated_at          TIMESTAMP(6)
);
CREATE INDEX idx_menu_parent ON app_menus(parent_id);
CREATE INDEX idx_menu_order  ON app_menus(display_order);
CREATE INDEX idx_menu_active ON app_menus(active, deleted);

CREATE TABLE sec_mrole_menus (
    id         BIGSERIAL PRIMARY KEY,
    role_id    BIGINT    NOT NULL REFERENCES sec_roles(id)   ON DELETE CASCADE,
    menu_id    BIGINT    NOT NULL REFERENCES app_menus(id)   ON DELETE CASCADE,
    can_view   BOOLEAN   NOT NULL DEFAULT TRUE,
    can_create BOOLEAN   NOT NULL DEFAULT FALSE,
    can_edit   BOOLEAN   NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    UNIQUE (role_id, menu_id)
);
CREATE INDEX idx_rma_role ON sec_mrole_menus(role_id);
CREATE INDEX idx_rma_menu ON sec_mrole_menus(menu_id);

CREATE TABLE sec_password_reset_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    token       VARCHAR(100) NOT NULL UNIQUE,
    user_id     BIGINT       UNIQUE REFERENCES sec_users(id),
    expires_at  TIMESTAMP(6) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    used_at     TIMESTAMP(6)
);
CREATE INDEX idx_prt_token   ON sec_password_reset_tokens(token);
CREATE INDEX idx_prt_expires ON sec_password_reset_tokens(expires_at);

CREATE TABLE spring_session (
    primary_id            CHAR(36)     NOT NULL PRIMARY KEY,
    session_id            CHAR(36)     NOT NULL UNIQUE,
    creation_time         BIGINT       NOT NULL,
    last_access_time      BIGINT       NOT NULL,
    max_inactive_interval INT          NOT NULL,
    expiry_time           BIGINT       NOT NULL,
    principal_name        VARCHAR(100)
);
CREATE INDEX spring_session_ix2 ON spring_session(expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session(principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36)     NOT NULL REFERENCES spring_session(primary_id) ON DELETE CASCADE,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA        NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 02 — LOCATION & REFERENCE MASTERS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE stp_currencies (
    id             BIGSERIAL    PRIMARY KEY,
    code           VARCHAR(3)   NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    symbol         VARCHAR(10),
    decimal_places INT          NOT NULL DEFAULT 2,
    active         BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE stp_location_countries (
    id          BIGSERIAL    PRIMARY KEY,
    currency_id BIGINT       NOT NULL REFERENCES stp_currencies(id),
    iso_code    VARCHAR(3)   NOT NULL UNIQUE,
    iso_code2   VARCHAR(2)   NOT NULL,
    name        VARCHAR(150) NOT NULL,
    name_native VARCHAR(150),
    phone_code  VARCHAR(10),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(6)
);

CREATE TABLE stp_location_states (
    id         BIGSERIAL    PRIMARY KEY,
    country_id BIGINT       NOT NULL REFERENCES stp_location_countries(id),
    code       VARCHAR(20)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_state_country ON stp_location_states(country_id);

CREATE TABLE stp_location_districts (
    id       BIGSERIAL    PRIMARY KEY,
    state_id BIGINT       NOT NULL REFERENCES stp_location_states(id),
    code     VARCHAR(30)  NOT NULL,
    name     VARCHAR(255) NOT NULL,
    active   BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_dist_state ON stp_location_districts(state_id);

CREATE TABLE stp_location_cities (
    id          BIGSERIAL    PRIMARY KEY,
    district_id BIGINT       NOT NULL REFERENCES stp_location_districts(id),
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE stp_location_areas (
    id      BIGSERIAL    PRIMARY KEY,
    city_id BIGINT       NOT NULL REFERENCES stp_location_cities(id),
    name    VARCHAR(255) NOT NULL,
    active  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE stp_banks (
    id                           BIGSERIAL    PRIMARY KEY,
    organization_id              BIGINT       NOT NULL REFERENCES org_organizations(id),
    bank_code                    VARCHAR(20)  NOT NULL,
    bank_name                    VARCHAR(200) NOT NULL,
    bank_name_local              VARCHAR(200),
    short_name                   VARCHAR(50),
    bank_type                    VARCHAR(30)  NOT NULL DEFAULT 'COMMERCIAL',
    swift_code                   VARCHAR(11),
    routing_number_prefix        VARCHAR(9),
    head_office_address          VARCHAR(500),
    head_office_city             VARCHAR(100),
    head_office_country          VARCHAR(100),
    head_office_phone            VARCHAR(50),
    head_office_email            VARCHAR(100),
    website                      VARCHAR(200),
    correspondent_bank_name      VARCHAR(200),
    correspondent_swift_code     VARCHAR(11),
    correspondent_account_number VARCHAR(50),
    supports_lc                  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by                   VARCHAR(100),
    updated_by                   VARCHAR(100),
    created_at                   TIMESTAMP(6),
    updated_at                   TIMESTAMP(6),
    UNIQUE (organization_id, bank_code)
);
CREATE INDEX idx_bank_org ON stp_banks(organization_id);

CREATE TABLE stp_document_sequences (
    id              BIGSERIAL   PRIMARY KEY,
    organization_id BIGINT      NOT NULL REFERENCES org_organizations(id),
    prefix          VARCHAR(20) NOT NULL,
    year_code       VARCHAR(6)  NOT NULL,
    last_seq        INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, prefix, year_code)
);
CREATE INDEX idx_docseq_org ON stp_document_sequences(organization_id);

CREATE TABLE stp_terms_master (
    id            BIGSERIAL    PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    document_type VARCHAR(50)  NOT NULL,
    is_active     BOOLEAN      DEFAULT TRUE,
    is_default    BOOLEAN      DEFAULT FALSE,
    sort_order    INT
);

CREATE TABLE stp_document_file (
    id                 BIGSERIAL    PRIMARY KEY,
    document_type      VARCHAR(50)  NOT NULL,
    reference_id       BIGINT       NOT NULL,
    file_name          VARCHAR(255),
    original_file_name VARCHAR(255),
    file_type          VARCHAR(100),
    file_path          VARCHAR(500),
    file_size          BIGINT,
    document_category  VARCHAR(200),
    remarks            VARCHAR(500),
    uploaded_at        TIMESTAMP(6),
    uploaded_by        VARCHAR(255)
);
CREATE INDEX idx_docfile_ref ON stp_document_file(document_type, reference_id);

CREATE TABLE com_hs_codes (
    id                         BIGSERIAL    PRIMARY KEY,
    organization_id            BIGINT       NOT NULL REFERENCES org_organizations(id),
    hs_code                    VARCHAR(20)  NOT NULL,
    description                VARCHAR(500) NOT NULL,
    short_description          VARCHAR(200),
    hs_type                    VARCHAR(20)  NOT NULL DEFAULT 'BOTH'
        CONSTRAINT chk_hs_type CHECK (hs_type IN ('EXPORT','IMPORT','BOTH')),
    vat_percent                NUMERIC(6,2),
    customs_duty_percent       NUMERIC(6,2),
    supplementary_duty_percent NUMERIC(6,2),
    ait_percent                NUMERIC(6,2),
    is_active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    is_bonded_allowed          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100),
    created_at                 TIMESTAMP(6),
    updated_at                 TIMESTAMP(6),
    UNIQUE (organization_id, hs_code)
);
CREATE INDEX idx_hscode_org ON com_hs_codes(organization_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 03 — INVENTORY MASTERS  (Generic — no fiber/yarn specifics)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE inv_item_categories (
    id                 BIGSERIAL    PRIMARY KEY,
    organization_id    BIGINT       NOT NULL REFERENCES org_organizations(id),
    parent_category_id BIGINT       REFERENCES inv_item_categories(id),
    category_code      VARCHAR(50)  NOT NULL,
    category_name      VARCHAR(100) NOT NULL,
    description        TEXT,
    -- Generic item type (industry-neutral)
    item_type          VARCHAR(30)
        CONSTRAINT chk_cat_itype CHECK (item_type IN (
            'RAW_MATERIAL','SEMI_FINISHED','FINISHED_GOOD',
            'SERVICE','SPARE_PART','CONSUMABLE','MRO','GENERAL','FIXED_ASSET')),
    layer_type         VARCHAR(20)  NOT NULL DEFAULT 'ITEM'
        CONSTRAINT chk_cat_layer CHECK (layer_type IN ('ROOT','GROUP','ITEM')),
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    created_at         TIMESTAMP(6),
    updated_at         TIMESTAMP(6),
    UNIQUE (organization_id, category_code)
);
CREATE INDEX idx_icat_org    ON inv_item_categories(organization_id);
CREATE INDEX idx_icat_parent ON inv_item_categories(parent_category_id);
CREATE INDEX idx_icat_type   ON inv_item_categories(item_type);

CREATE TABLE inv_item_uom (
    id                BIGSERIAL       PRIMARY KEY,
    organization_id   BIGINT          NOT NULL REFERENCES org_organizations(id),
    code              VARCHAR(20)     NOT NULL,
    name              VARCHAR(100)    NOT NULL,
    symbol            VARCHAR(20),
    category          VARCHAR(30)     NOT NULL
        CONSTRAINT chk_uom_cat CHECK (category IN (
            'WEIGHT','COUNT','LENGTH','VOLUME','AREA','PACKING','UNIT')),
    is_base_unit      BOOLEAN         NOT NULL DEFAULT FALSE,
    conversion_factor NUMERIC(12,6)   NOT NULL DEFAULT 1,
    active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6),
    updated_at        TIMESTAMP(6),
    UNIQUE (organization_id, code)
);
CREATE INDEX idx_uom_org ON inv_item_uom(organization_id);

CREATE TABLE inv_item_brands (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    brand_code      VARCHAR(30)  NOT NULL,
    brand_name      VARCHAR(150) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, brand_code)
);
CREATE INDEX idx_brand_org ON inv_item_brands(organization_id);

CREATE TABLE inv_item_models (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    brand_id        BIGINT       REFERENCES inv_item_brands(id),
    model_code      VARCHAR(30)  NOT NULL,
    model_name      VARCHAR(150) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, brand_id, model_code)
);
CREATE INDEX idx_model_org ON inv_item_models(organization_id);

CREATE TABLE inv_items (
    id                BIGSERIAL       PRIMARY KEY,
    organization_id   BIGINT          NOT NULL REFERENCES org_organizations(id),
    category_id       BIGINT          NOT NULL REFERENCES inv_item_categories(id),
    purchase_unit_id  BIGINT          NOT NULL REFERENCES inv_item_uom(id),
    sales_unit_id     BIGINT          NOT NULL REFERENCES inv_item_uom(id),
    operation_unit_id BIGINT          NOT NULL REFERENCES inv_item_uom(id),
    brand_id          BIGINT          REFERENCES inv_item_brands(id),
    model_id          BIGINT          REFERENCES inv_item_models(id),
    hs_code_id        BIGINT          REFERENCES com_hs_codes(id),
    origin_id         BIGINT          REFERENCES stp_location_countries(id),

    item_code         VARCHAR(50)     NOT NULL,
    item_name         VARCHAR(200)    NOT NULL,
    item_name_bn      VARCHAR(200),

    -- Generic item type (industry-neutral)
    item_type         VARCHAR(30)     NOT NULL DEFAULT 'GENERAL'
        CONSTRAINT chk_item_type CHECK (item_type IN (
            'RAW_MATERIAL','SEMI_FINISHED','FINISHED_GOOD',
            'SERVICE','SPARE_PART','CONSUMABLE','MRO','GENERAL','FIXED_ASSET')),

    sku               VARCHAR(100),
    barcode           VARCHAR(100),
    unit_of_measure   VARCHAR(20)     NOT NULL,    -- display code
    purchase_unit_code VARCHAR(20)    NOT NULL,
    sales_unit_code    VARCHAR(20)    NOT NULL,

    -- Costing
    cost_price        NUMERIC(12,4),
    unit_price        NUMERIC(12,4),
    tax_rate          NUMERIC(5,2),
    standard_cost     NUMERIC(12,4),               -- for COGS valuation

    -- Stock control
    minimum_stock     NUMERIC(12,3),
    maximum_stock     NUMERIC(12,3),
    reorder_level     NUMERIC(12,3),

    -- Generic production fields
    yield_percent     NUMERIC(5,2),               -- expected output %, e.g. 92%
    process_loss_pct  NUMERIC(5,2),               -- expected waste %

    -- Physical specs (generic)
    weight            NUMERIC(12,4),              -- unit weight (kg)
    volume            NUMERIC(12,4),              -- unit volume (L)
    dimensions        VARCHAR(100),               -- LxWxH

    -- Shelf life / expiry
    shelf_life_days   INT,
    expiry_date       DATE,
    has_lot_tracking  BOOLEAN         NOT NULL DEFAULT FALSE,
    has_serial        BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Asset fields (for FIXED_ASSET type)
    serial_number     VARCHAR(100),
    model_name        VARCHAR(100),
    manufacturer      VARCHAR(100),
    warranty_months   INT,
    depreciation_rate NUMERIC(5,2),

    -- Chemical / hazardous (for CONSUMABLE/MRO)
    cas_number        VARCHAR(50),
    is_hazardous      BOOLEAN         NOT NULL DEFAULT FALSE,
    safety_data_sheet VARCHAR(255),

    description       TEXT,
    internal_notes    TEXT,

    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    is_approved       BOOLEAN         NOT NULL DEFAULT FALSE,
    approved_by       VARCHAR(100),
    approved_at       TIMESTAMP(6),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    created_at        TIMESTAMP(6),
    updated_at        TIMESTAMP(6),
    UNIQUE (organization_id, item_code),
    UNIQUE (organization_id, item_name)
);
CREATE INDEX idx_item_org  ON inv_items(organization_id);
CREATE INDEX idx_item_type ON inv_items(item_type);
CREATE INDEX idx_item_cat  ON inv_items(category_id);
CREATE INDEX idx_item_active ON inv_items(is_active);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 04 — ORGANIZATION HIERARCHY
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE org_business_units (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, code)
);
CREATE INDEX idx_bu_org ON org_business_units(organization_id);

CREATE TABLE org_cost_centers (
    id                    BIGSERIAL    PRIMARY KEY,
    business_unit_id      BIGINT       NOT NULL REFERENCES org_business_units(id),
    parent_cost_center_id BIGINT       REFERENCES org_cost_centers(id),
    cost_center_code      VARCHAR(50)  NOT NULL UNIQUE,
    cost_center_name      VARCHAR(200) NOT NULL,
    cost_center_type      VARCHAR(20)
        CONSTRAINT chk_cc_type CHECK (cost_center_type IN (
            'DEPARTMENT','PROJECT','BRANCH','DIVISION','PRODUCT','SERVICE','PRODUCTION')),
    description           VARCHAR(1000),
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    created_at            TIMESTAMP(6),
    updated_at            TIMESTAMP(6)
);
CREATE INDEX idx_cc_bu     ON org_cost_centers(business_unit_id);
CREATE INDEX idx_cc_parent ON org_cost_centers(parent_cost_center_id);
CREATE INDEX idx_cc_type   ON org_cost_centers(cost_center_type);

CREATE TABLE org_warehouses (
    id               BIGSERIAL    PRIMARY KEY,
    business_unit_id BIGINT       NOT NULL REFERENCES org_business_units(id),
    warehouse_code   VARCHAR(50)  NOT NULL UNIQUE,
    warehouse_name   VARCHAR(200) NOT NULL,
    warehouse_type   VARCHAR(30)  NOT NULL DEFAULT 'GENERAL'
        CONSTRAINT chk_wh_type CHECK (warehouse_type IN (
            'RAW_MATERIAL','FINISHED_GOODS','WIP','GENERAL','SPARE_PART','REJECTED')),
    address          TEXT,
    manager_name     VARCHAR(100),
    contact_number   VARCHAR(20),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6)
);
CREATE INDEX idx_wh_bu ON org_warehouses(business_unit_id);

CREATE TABLE org_departments (
    id                   BIGSERIAL    PRIMARY KEY,
    organization_id      BIGINT       NOT NULL REFERENCES org_organizations(id),
    parent_department_id BIGINT       REFERENCES org_departments(id),
    head_employee_id     BIGINT,      -- deferred FK → hrm_employees
    cost_center_id       BIGINT       REFERENCES org_cost_centers(id),
    code                 VARCHAR(50)  UNIQUE,
    name                 VARCHAR(100) NOT NULL UNIQUE,
    description          VARCHAR(500),
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    created_at           TIMESTAMP(6),
    updated_at           TIMESTAMP(6)
);
CREATE INDEX idx_dept_org    ON org_departments(organization_id);
CREATE INDEX idx_dept_parent ON org_departments(parent_department_id);

CREATE TABLE user_context (
    user_id                         BIGINT NOT NULL PRIMARY KEY REFERENCES sec_users(id),
    organization_id                 BIGINT REFERENCES org_organizations(id),
    business_unit_id                BIGINT REFERENCES org_business_units(id),
    cost_center_id                  BIGINT REFERENCES org_cost_centers(id),
    warehouse_id                    BIGINT REFERENCES org_warehouses(id),
    approval_default_view           VARCHAR(20),
    approval_desktop_notification   BOOLEAN,
    approval_email_enabled          BOOLEAN,
    approval_push_enabled           BOOLEAN,
    approval_sms_enabled            BOOLEAN,
    approval_sound_enabled          BOOLEAN,
    approval_notification_frequency VARCHAR(20),
    approval_refresh_interval       INT,
    show_approval_badge             BOOLEAN,
    last_viewed_notification_id     BIGINT
);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 05 — GLOBAL INVENTORY (Lots, Stock)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE global_inv_lots (
    id                   BIGSERIAL       PRIMARY KEY,
    organization_id      BIGINT          NOT NULL REFERENCES org_organizations(id),
    item_id              BIGINT          NOT NULL REFERENCES inv_items(id),
    country_of_origin_id BIGINT          REFERENCES stp_location_countries(id),
    bank_id              BIGINT          REFERENCES stp_banks(id),
    supplier_id          BIGINT,         -- soft ref → acc_chart_of_accounts_sub
    production_order_id  BIGINT,         -- soft ref → prd_productions (deferred)
    version              BIGINT,         -- optimistic lock
    deleted              BOOLEAN         NOT NULL DEFAULT FALSE,

    lot_number           VARCHAR(100)    NOT NULL,
    -- Generic item type matching inv_items.item_type
    item_type            VARCHAR(30)     NOT NULL DEFAULT 'GENERAL'
        CONSTRAINT chk_lot_item_type CHECK (item_type IN (
            'RAW_MATERIAL','SEMI_FINISHED','FINISHED_GOOD',
            'SERVICE','SPARE_PART','CONSUMABLE','MRO','GENERAL','FIXED_ASSET')),
    status               VARCHAR(30)     NOT NULL DEFAULT 'AVAILABLE'
        CONSTRAINT chk_lot_status CHECK (status IN (
            'AVAILABLE','RESERVED','BLOCKED','QC_HOLD','EXPIRED','CONSUMED')),

    -- Generic lot attributes
    batch_no              VARCHAR(100),
    manufacturer_batch_no VARCHAR(100),
    serial_no             VARCHAR(100),   -- for serialized items
    bin_location          VARCHAR(100),
    shelf_location        VARCHAR(100),
    warehouse_location    VARCHAR(100),

    -- Dates
    received_date         DATE,
    manufacturing_date    DATE,
    production_date       DATE,
    expiry_date           DATE,

    -- Generic QC attributes (flexible)
    qc_grade              VARCHAR(50),   -- A, B, C or PASS/FAIL etc.
    qc_remarks            TEXT,
    qc_passed             BOOLEAN,
    qc_date               DATE,
    qc_by                 VARCHAR(100),

    -- Physical
    gross_weight          NUMERIC(12,3),
    net_weight            NUMERIC(12,3),
    unit_cost             NUMERIC(18,4),

    remarks               TEXT,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    created_at            TIMESTAMP(6),
    updated_at            TIMESTAMP(6)
);
CREATE INDEX idx_lot_item    ON global_inv_lots(item_id);
CREATE INDEX idx_lot_org     ON global_inv_lots(organization_id);
CREATE INDEX idx_lot_status  ON global_inv_lots(status);
CREATE INDEX idx_lot_number  ON global_inv_lots(lot_number);
CREATE INDEX idx_lot_deleted ON global_inv_lots(deleted);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 06 — FINANCE / ACCOUNTS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE acc_chart_of_accounts (
    id                 BIGSERIAL       PRIMARY KEY,
    organization_id    BIGINT          NOT NULL REFERENCES org_organizations(id),
    parent_account_id  BIGINT          REFERENCES acc_chart_of_accounts(id),
    account_code       VARCHAR(50)     NOT NULL UNIQUE,
    account_name       VARCHAR(200)    NOT NULL,
    account_type       VARCHAR(20)     NOT NULL
        CONSTRAINT chk_coa_type CHECK (account_type IN (
            'ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    account_nature     VARCHAR(20)     NOT NULL
        CONSTRAINT chk_coa_nature CHECK (account_nature IN ('DEBIT','CREDIT')),
    level              INT             NOT NULL DEFAULT 1,
    opening_balance    NUMERIC(18,2),
    current_balance    NUMERIC(18,2),
    currency           VARCHAR(10),
    description        VARCHAR(1000),
    is_active          BOOLEAN         NOT NULL DEFAULT TRUE,
    is_system          BOOLEAN         NOT NULL DEFAULT FALSE,
    is_control_account BOOLEAN         NOT NULL DEFAULT FALSE,
    allow_manual_entry BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    created_at         TIMESTAMP(6),
    updated_at         TIMESTAMP(6),
    UNIQUE (organization_id, account_code)
);
CREATE INDEX idx_coa_org    ON acc_chart_of_accounts(organization_id);
CREATE INDEX idx_coa_parent ON acc_chart_of_accounts(parent_account_id);
CREATE INDEX idx_coa_type   ON acc_chart_of_accounts(account_type);

-- STI sub-ledger: BANK | CASH | LC | CUSTOMER | SUPPLIER | EMPLOYEE | GENERAL | INTER_COMPANY
CREATE TABLE acc_chart_of_accounts_sub (
    sub_account_type         VARCHAR(31)  NOT NULL
        CONSTRAINT chk_sub_type CHECK (sub_account_type IN (
            'BANK','CASH','LC','CUSTOMER','SUPPLIER',
            'EMPLOYEE','GENERAL','INTER_COMPANY')),
    id                       BIGSERIAL    PRIMARY KEY,
    organization_id          BIGINT       NOT NULL REFERENCES org_organizations(id),
    main_account_id          BIGINT       NOT NULL REFERENCES acc_chart_of_accounts(id),
    sub_account_code         VARCHAR(50)  NOT NULL UNIQUE,
    sub_account_name         VARCHAR(200) NOT NULL,
    opening_balance          NUMERIC(18,2),
    current_balance          NUMERIC(18,2),
    currency                 VARCHAR(20),
    description              VARCHAR(1000),
    contact_person           VARCHAR(200),
    contact_phone            VARCHAR(20),
    contact_email            VARCHAR(100),
    address                  VARCHAR(500),
    city                     VARCHAR(50),
    state                    VARCHAR(50),
    country                  VARCHAR(50),
    postal_code              VARCHAR(20),
    tax_id                   VARCHAR(50),
    vat_registration_no      VARCHAR(50),
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    -- BANK-specific
    bank_id                  BIGINT       REFERENCES stp_banks(id),
    account_number           VARCHAR(50)  UNIQUE,
    account_title            VARCHAR(200),
    bank_name                VARCHAR(200),
    bank_account_type        VARCHAR(20),
    branch_name              VARCHAR(100),
    branch_code              VARCHAR(10),
    routing_number           VARCHAR(9),
    swift_code               VARCHAR(11),
    iban_number              VARCHAR(34),
    interest_rate            NUMERIC(8,4),
    overdraft_limit          NUMERIC(18,2),
    -- CASH-specific
    location                 VARCHAR(100),
    custodian                VARCHAR(100),
    maximum_limit            NUMERIC(18,2),
    minimum_limit            NUMERIC(18,2),
    requires_approval        BOOLEAN      NOT NULL DEFAULT FALSE,
    -- CUSTOMER-specific
    customer_code            VARCHAR(50),
    credit_limit             NUMERIC(18,2),
    payment_terms            VARCHAR(100),
    credit_days              INT,
    sales_representative     VARCHAR(100),
    customer_group           VARCHAR(50),
    is_export_customer       BOOLEAN      DEFAULT FALSE,
    -- SUPPLIER-specific
    supplier_code            VARCHAR(50),
    lead_time_days           INT,
    is_import_supplier       BOOLEAN      DEFAULT FALSE,
    preferred_currency       VARCHAR(3),
    -- LC-specific
    lc_number                VARCHAR(100) UNIQUE,
    manual_lc_number         VARCHAR(100),
    lc_type                  VARCHAR(30),
    lc_status                VARCHAR(30),
    transaction_currency     VARCHAR(20),
    lc_amount                NUMERIC(18,2),
    exchange_rate            NUMERIC(18,4),
    issue_date               DATE,
    expiry_date              DATE,
    shipment_date            DATE,
    payment_term             VARCHAR(30),
    shipment_mode            VARCHAR(20),
    margin_account_id        BIGINT       REFERENCES acc_chart_of_accounts(id),
    beneficiary_bank_id      BIGINT       REFERENCES stp_banks(id),
    buyer_bank_id            BIGINT       REFERENCES stp_banks(id),
    -- stub FKs
    customer_id              BIGINT,
    supplier_id              BIGINT,
    remarks                  VARCHAR(1000),
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    created_at               TIMESTAMP(6),
    updated_at               TIMESTAMP(6)
);
CREATE INDEX idx_sub_org  ON acc_chart_of_accounts_sub(organization_id);
CREATE INDEX idx_sub_type ON acc_chart_of_accounts_sub(sub_account_type);
CREATE INDEX idx_sub_main ON acc_chart_of_accounts_sub(main_account_id);
CREATE INDEX idx_sub_bank ON acc_chart_of_accounts_sub(bank_id);

CREATE TABLE acc_periods (
    id              BIGSERIAL       PRIMARY KEY,
    organization_id BIGINT          NOT NULL REFERENCES org_organizations(id),
    period_name     VARCHAR(50)     NOT NULL UNIQUE,
    period_type     VARCHAR(20)     NOT NULL
        CONSTRAINT chk_period_type CHECK (period_type IN (
            'DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY','CUSTOM')),
    fiscal_year     INT             NOT NULL,
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    description     VARCHAR(1000),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    is_closed       BOOLEAN         NOT NULL DEFAULT FALSE,
    closed_by       VARCHAR(100),
    closed_date     DATE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6)
);
CREATE INDEX idx_period_org ON acc_periods(organization_id);

CREATE TABLE acc_opening_balances (
    id                     BIGSERIAL       PRIMARY KEY,
    organization_id        BIGINT          NOT NULL REFERENCES org_organizations(id),
    account_id             BIGINT          NOT NULL REFERENCES acc_chart_of_accounts(id),
    accounting_period_id   BIGINT          NOT NULL REFERENCES acc_periods(id),
    opening_debit_balance  NUMERIC(18,2)   NOT NULL DEFAULT 0,
    opening_credit_balance NUMERIC(18,2)   NOT NULL DEFAULT 0,
    is_posted              BOOLEAN         NOT NULL DEFAULT FALSE,
    posted_by              VARCHAR(100),
    posted_date            DATE,
    remarks                VARCHAR(1000),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    created_at             TIMESTAMP(6),
    updated_at             TIMESTAMP(6)
);
CREATE INDEX idx_ob_org ON acc_opening_balances(organization_id);

CREATE TABLE acc_journal_entry_master (
    id              BIGSERIAL       PRIMARY KEY,
    organization_id BIGINT          REFERENCES org_organizations(id),
    voucher_no      VARCHAR(100),
    voucher_date    DATE,
    voucher_type    VARCHAR(30)
        CONSTRAINT chk_jem_type CHECK (voucher_type IN (
            'JOURNAL_VOUCHER','PURCHASE_VOUCHER','SALES_VOUCHER',
            'PAYMENT_VOUCHER','RECEIPT_VOUCHER','CONTRA_VOUCHER',
            'EXPENSE_VOUCHER','DEBIT_NOTE','CREDIT_NOTE','PRODUCTION_VOUCHER')),
    total_debit     NUMERIC(18,2),
    total_credit    NUMERIC(18,2),
    narration       VARCHAR(1000),
    reference_no    VARCHAR(100),
    is_posted       BOOLEAN         NOT NULL DEFAULT FALSE,
    posted_by       VARCHAR(100),
    posted_at       TIMESTAMP(6),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6)
);
CREATE INDEX idx_jem_org    ON acc_journal_entry_master(organization_id);
CREATE INDEX idx_jem_date   ON acc_journal_entry_master(voucher_date);
CREATE INDEX idx_jem_type   ON acc_journal_entry_master(voucher_type);
CREATE INDEX idx_jem_posted ON acc_journal_entry_master(is_posted);
CREATE INDEX idx_jem_no     ON acc_journal_entry_master(voucher_no);

CREATE TABLE acc_journal_entry_lines (
    id               BIGSERIAL       PRIMARY KEY,
    organization_id  BIGINT          NOT NULL REFERENCES org_organizations(id),
    journal_entry_id BIGINT          NOT NULL REFERENCES acc_journal_entry_master(id) ON DELETE CASCADE,
    account_id       BIGINT          NOT NULL REFERENCES acc_chart_of_accounts(id),
    sub_account_id   BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    cost_center_id   BIGINT          REFERENCES org_cost_centers(id),
    line_number      INT             NOT NULL,
    entry_type       VARCHAR(10)     NOT NULL
        CONSTRAINT chk_jel_type CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount           NUMERIC(18,2)   NOT NULL,
    narration        VARCHAR(500),
    reference_no     VARCHAR(100),
    tax_code         VARCHAR(20),
    is_tax_line      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6)
);
CREATE INDEX idx_jel_journal ON acc_journal_entry_lines(journal_entry_id);
CREATE INDEX idx_jel_account ON acc_journal_entry_lines(account_id);
CREATE INDEX idx_jel_sub     ON acc_journal_entry_lines(sub_account_id);
CREATE INDEX idx_jel_cc      ON acc_journal_entry_lines(cost_center_id);

CREATE TABLE acc_mapping (
    id                         BIGSERIAL    PRIMARY KEY,
    organization_id            BIGINT       NOT NULL REFERENCES org_organizations(id),
    mapping_code               VARCHAR(30)  NOT NULL,
    mapping_name               VARCHAR(200) NOT NULL,
    module_type                VARCHAR(50)  NOT NULL,
    transaction_type           VARCHAR(50)  NOT NULL,
    description                VARCHAR(500),
    voucher_type               VARCHAR(30),
    voucher_prefix             VARCHAR(20),
    default_narration_template VARCHAR(500),
    use_sub_ledger             BOOLEAN      NOT NULL DEFAULT FALSE,
    auto_post                  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    is_default                 BOOLEAN      NOT NULL DEFAULT FALSE,
    default_debit_account_id   BIGINT       REFERENCES acc_chart_of_accounts(id),
    default_credit_account_id  BIGINT       REFERENCES acc_chart_of_accounts(id),
    discount_account_id        BIGINT       REFERENCES acc_chart_of_accounts(id),
    tax_account_id             BIGINT       REFERENCES acc_chart_of_accounts(id),
    wip_account_id             BIGINT       REFERENCES acc_chart_of_accounts(id),
    cogs_account_id            BIGINT       REFERENCES acc_chart_of_accounts(id),
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100),
    created_at                 TIMESTAMP(6),
    updated_at                 TIMESTAMP(6),
    UNIQUE (organization_id, mapping_code)
);
CREATE INDEX idx_mapping_org ON acc_mapping(organization_id);

CREATE TABLE acc_mapping_details (
    id                  BIGSERIAL       PRIMARY KEY,
    accounts_mapping_id BIGINT          NOT NULL REFERENCES acc_mapping(id) ON DELETE CASCADE,
    account_id          BIGINT          REFERENCES acc_chart_of_accounts(id),
    cost_center_id      BIGINT          REFERENCES org_cost_centers(id),
    line_number         INT             NOT NULL,
    entry_name          VARCHAR(100),
    entry_type          VARCHAR(10)     NOT NULL
        CONSTRAINT chk_amd_type CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount_type         VARCHAR(30)     NOT NULL,
    percentage          NUMERIC(8,4),
    fixed_amount        NUMERIC(18,2),
    formula             VARCHAR(500),
    field_reference     VARCHAR(100),
    sort_order          INT             NOT NULL DEFAULT 0,
    skip_if_zero        BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMP(6),
    updated_at          TIMESTAMP(6)
);
CREATE INDEX idx_amd_mapping ON acc_mapping_details(accounts_mapping_id);

CREATE TABLE acc_policy (
    id                         BIGSERIAL       PRIMARY KEY,
    organization_id            BIGINT          NOT NULL REFERENCES org_organizations(id),
    accounts_mapping_id        BIGINT          REFERENCES acc_mapping(id),
    policy_code                VARCHAR(30)     NOT NULL,
    policy_name                VARCHAR(200)    NOT NULL,
    policy_type                VARCHAR(30)     NOT NULL,
    module_type                VARCHAR(30),
    voucher_prefix             VARCHAR(20),
    next_voucher_number        INT,
    auto_numbering             BOOLEAN         NOT NULL DEFAULT TRUE,
    auto_post                  BOOLEAN         NOT NULL DEFAULT FALSE,
    require_approval           BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    is_default                 BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100),
    created_at                 TIMESTAMP(6),
    updated_at                 TIMESTAMP(6),
    UNIQUE (organization_id, policy_code)
);
CREATE INDEX idx_policy_org ON acc_policy(organization_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 07 — APPROVAL ENGINE
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE apr_configs (
    id                      BIGSERIAL    PRIMARY KEY,
    organization_id         BIGINT       NOT NULL REFERENCES org_organizations(id),
    code                    VARCHAR(50)  NOT NULL UNIQUE,
    name                    VARCHAR(200) NOT NULL,
    description             VARCHAR(1000),
    document_type           VARCHAR(50)  NOT NULL,
    module                  VARCHAR(30)  NOT NULL,
    flow_type               VARCHAR(20)  NOT NULL DEFAULT 'SEQUENTIAL'
        CONSTRAINT chk_apr_flow CHECK (flow_type IN ('SEQUENTIAL','PARALLEL')),
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    min_amount              NUMERIC(18,2),
    max_amount              NUMERIC(18,2),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    created_at              TIMESTAMP(6),
    updated_at              TIMESTAMP(6)
);
CREATE INDEX idx_aprc_org ON apr_configs(organization_id);

CREATE TABLE apr_levels (
    id                 BIGSERIAL    PRIMARY KEY,
    approval_config_id BIGINT       NOT NULL REFERENCES apr_configs(id) ON DELETE CASCADE,
    approver_user_id   BIGINT       REFERENCES sec_users(id),
    level_number       INT          NOT NULL,
    level_name         VARCHAR(100) NOT NULL,
    description        VARCHAR(500),
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    can_delegate       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    created_at         TIMESTAMP(6),
    updated_at         TIMESTAMP(6)
);
CREATE INDEX idx_aprl_config ON apr_levels(approval_config_id);

CREATE TABLE apr_requests (
    id                        BIGSERIAL    PRIMARY KEY,
    organization_id           BIGINT       NOT NULL REFERENCES org_organizations(id),
    approval_config_id        BIGINT       REFERENCES apr_configs(id),
    current_approval_level_id BIGINT       REFERENCES apr_levels(id),
    current_approver_user_id  BIGINT       REFERENCES sec_users(id),
    requester_id              BIGINT       NOT NULL REFERENCES sec_users(id),
    document_type             VARCHAR(50)  NOT NULL,
    reference_id              BIGINT       NOT NULL,
    reference_number          VARCHAR(100) NOT NULL,
    document_date             DATE,
    document_amount           NUMERIC(18,2),
    document_summary          VARCHAR(500),
    current_level_number      INT          NOT NULL DEFAULT 1,
    total_levels              INT          NOT NULL DEFAULT 1,
    requester_name            VARCHAR(200),
    status                    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_aprr_status CHECK (status IN (
            'DRAFT','SUBMITTED','IN_APPROVAL','APPROVED','REJECTED',
            'RETURNED','CANCELLED','COMPLETED')),
    is_urgent                 BOOLEAN      NOT NULL DEFAULT FALSE,
    due_date                  DATE,
    final_remarks             VARCHAR(1000),
    completed_at              TIMESTAMP(6),
    created_by                VARCHAR(100),
    updated_by                VARCHAR(100),
    created_at                TIMESTAMP(6),
    updated_at                TIMESTAMP(6)
);
CREATE INDEX idx_aprr_org      ON apr_requests(organization_id);
CREATE INDEX idx_aprr_status   ON apr_requests(status);
CREATE INDEX idx_aprr_ref      ON apr_requests(reference_id, document_type);
CREATE INDEX idx_aprr_approver ON apr_requests(current_approver_user_id);

CREATE TABLE apr_histories (
    id                  BIGSERIAL    PRIMARY KEY,
    approval_request_id BIGINT       NOT NULL REFERENCES apr_requests(id),
    approval_level_id   BIGINT       REFERENCES apr_levels(id),
    actor_user_id       BIGINT       REFERENCES sec_users(id),
    level_number        INT          NOT NULL,
    level_name          VARCHAR(100) NOT NULL,
    actor_name          VARCHAR(150) NOT NULL,
    action              VARCHAR(30)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    comments            VARCHAR(2000),
    action_at           TIMESTAMP(6),
    created_at          TIMESTAMP(6)
);
CREATE INDEX idx_aprh_request ON apr_histories(approval_request_id);

CREATE TABLE apr_delegations (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    delegator_id    BIGINT       NOT NULL REFERENCES sec_users(id),
    delegate_id     BIGINT       NOT NULL REFERENCES sec_users(id),
    delegation_code VARCHAR(50)  NOT NULL UNIQUE,
    module          VARCHAR(30),
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    reason          VARCHAR(1000),
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
        CONSTRAINT chk_aprd_status CHECK (status IN (
            'SCHEDULED','ACTIVE','EXPIRED','REVOKED')),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6)
);
CREATE INDEX idx_aprdel_org ON apr_delegations(organization_id);

CREATE TABLE apr_voucher (
    id                      BIGSERIAL    PRIMARY KEY,
    journal_entry_master_id BIGINT       NOT NULL UNIQUE REFERENCES acc_journal_entry_master(id),
    approval_level          INT          NOT NULL,
    approval_status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    approver_name           VARCHAR(100),
    approval_date           DATE,
    approval_remarks        VARCHAR(1000),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    created_at              TIMESTAMP(6),
    updated_at              TIMESTAMP(6)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 08 — GLOBAL DOCUMENTS (Purchase, Sales, Stock movements)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE global_business_documents (
    id                   BIGSERIAL       PRIMARY KEY,
    organization_id      BIGINT          NOT NULL REFERENCES org_organizations(id),
    approval_request_id  BIGINT          REFERENCES apr_requests(id),
    department_id        BIGINT          REFERENCES org_departments(id),
    parent_document_id   BIGINT          REFERENCES global_business_documents(id),
    party_id             BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    warehouse_id         BIGINT          REFERENCES org_warehouses(id),
    document_no          VARCHAR(100)    NOT NULL UNIQUE,
    document_no_manual   VARCHAR(100),
    document_date        DATE            NOT NULL,
    document_type        VARCHAR(50)     NOT NULL
        CONSTRAINT chk_gbd_type CHECK (document_type IN (
            -- Sales
            'SALES_QUOTATION','SALES_ORDER','DELIVERY_ORDER','DELIVERY_CHALLAN','SALES_INVOICE',
            -- Purchase
            'PURCHASE_REQUISITION','REQUEST_FOR_QUOTATION','COMPARATIVE_STATEMENT',
            'PURCHASE_ORDER','GOODS_RECEIPT_NOTE','PURCHASE_INVOICE',
            -- Stock
            'STORE_REQUISITION','MATERIAL_ISSUE','MATERIAL_RECEIVE',
            'STOCK_TRANSFER','STOCK_ADJUSTMENT',
            -- Production
            'PRODUCTION_ORDER','PRODUCTION_REQUISITION','PRODUCTION_MATERIAL_ISSUE','FINISHED_GOODS_RECEIVE',
            -- Commercial
            'EXPORT_PROFORMA_INVOICE','IMPORT_PROFORMA_INVOICE','LETTER_OF_CREDIT',
            -- Credit/Debit
            'DEBIT_NOTE','CREDIT_NOTE')),
    status               VARCHAR(30)     NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_gbd_status CHECK (status IN (
            'DRAFT','SUBMITTED','APPROVED','PROCESSING','PARTIAL',
            'PARTIALLY_CONVERTED','COMPLETED','REJECTED','RETURNED',
            'CANCELLED','CONVERTED')),
    approval_status      VARCHAR(30),
    currency             VARCHAR(20),
    exchange_rate        NUMERIC(18,4),
    subtotal_amount      NUMERIC(18,2),
    discount_amount      NUMERIC(18,2),
    tax_amount           NUMERIC(18,2),
    other_charges        NUMERIC(18,2),
    total_amount         NUMERIC(18,2),
    paid_amount          NUMERIC(18,2),
    due_amount           NUMERIC(18,2),
    stock_posted         BOOLEAN         NOT NULL DEFAULT FALSE,
    accounting_posted    BOOLEAN         NOT NULL DEFAULT FALSE,
    reference_no         VARCHAR(100),
    -- Shipping/export
    incoterms            VARCHAR(50),
    port_of_loading      VARCHAR(50),
    port_of_discharge    VARCHAR(50),
    vessel_name          VARCHAR(100),
    bl_number            VARCHAR(100),
    container_number     VARCHAR(100),
    -- Delivery
    challan_no           VARCHAR(100),
    vehicle_number       VARCHAR(100),
    driver_name          VARCHAR(100),
    delivery_address     VARCHAR(500),
    delivery_date        DATE,
    required_date        DATE,
    validity_date        DATE,
    contact_person       VARCHAR(100),
    contact_number       VARCHAR(20),
    terms_and_conditions TEXT,
    remarks              TEXT,
    is_deleted           BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMP(6),
    deleted_by           VARCHAR(100),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    created_at           TIMESTAMP(6),
    updated_at           TIMESTAMP(6)
);
CREATE INDEX idx_gbd_org     ON global_business_documents(organization_id);
CREATE INDEX idx_gbd_type    ON global_business_documents(document_type);
CREATE INDEX idx_gbd_status  ON global_business_documents(status);
CREATE INDEX idx_gbd_party   ON global_business_documents(party_id);
CREATE INDEX idx_gbd_parent  ON global_business_documents(parent_document_id);
CREATE INDEX idx_gbd_wh      ON global_business_documents(warehouse_id);
CREATE INDEX idx_gbd_date    ON global_business_documents(document_date);
CREATE INDEX idx_gbd_no      ON global_business_documents(document_no);
CREATE INDEX idx_gbd_deleted ON global_business_documents(is_deleted);

CREATE TABLE global_business_document_lines (
    id                 BIGSERIAL       PRIMARY KEY,
    organization_id    BIGINT          NOT NULL REFERENCES org_organizations(id),
    document_id        BIGINT          NOT NULL REFERENCES global_business_documents(id) ON DELETE CASCADE,
    item_id            BIGINT          NOT NULL REFERENCES inv_items(id),
    inventory_lot_id   BIGINT          REFERENCES global_inv_lots(id),
    source_line_id     BIGINT          REFERENCES global_business_document_lines(id),
    cost_center_id     BIGINT          REFERENCES org_cost_centers(id),
    line_number        INT             NOT NULL,
    item_code          VARCHAR(100),
    item_name          VARCHAR(500),
    description        VARCHAR(1000),
    unit_code          VARCHAR(20),
    quantity           NUMERIC(18,3)   NOT NULL,
    delivered_qty      NUMERIC(18,3),
    received_qty       NUMERIC(18,3),
    accepted_qty       NUMERIC(18,3),
    rejected_qty       NUMERIC(18,3),
    unit_price         NUMERIC(18,4),
    discount_amount    NUMERIC(18,2),
    tax_amount         NUMERIC(18,2),
    line_amount        NUMERIC(18,2),
    batch_number       VARCHAR(100),
    expected_date      DATE,
    quality_status     VARCHAR(30),
    quality_remarks    TEXT,
    remarks            TEXT,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    created_at         TIMESTAMP(6),
    updated_at         TIMESTAMP(6)
);
CREATE INDEX idx_gbdl_doc  ON global_business_document_lines(document_id);
CREATE INDEX idx_gbdl_item ON global_business_document_lines(item_id);
CREATE INDEX idx_gbdl_lot  ON global_business_document_lines(inventory_lot_id);

CREATE TABLE global_business_document_line_lots (
    id               BIGSERIAL       PRIMARY KEY,
    document_line_id BIGINT          NOT NULL REFERENCES global_business_document_lines(id) ON DELETE CASCADE,
    lot_id           BIGINT          NOT NULL REFERENCES global_inv_lots(id),
    quantity         NUMERIC(18,3)   NOT NULL,
    gross_weight     NUMERIC(12,3),
    net_weight       NUMERIC(12,3),
    unit_cost        NUMERIC(18,4),
    total_cost       NUMERIC(18,2),
    remarks          TEXT,
    created_at       TIMESTAMP(6),
    created_by       VARCHAR(100)
);
CREATE INDEX idx_gbdll_line ON global_business_document_line_lots(document_line_id);
CREATE INDEX idx_gbdll_lot  ON global_business_document_line_lots(lot_id);

CREATE TABLE global_inventory_stock_balances (
    id                    BIGSERIAL       PRIMARY KEY,
    item_id               BIGINT          NOT NULL REFERENCES inv_items(id),
    warehouse_id          BIGINT          NOT NULL REFERENCES org_warehouses(id),
    lot_id                BIGINT          REFERENCES global_inv_lots(id),
    quantity              NUMERIC(18,3)   NOT NULL DEFAULT 0,
    reserved_quantity     NUMERIC(18,3)   NOT NULL DEFAULT 0,
    gross_weight          NUMERIC(12,3),
    net_weight            NUMERIC(12,3),
    average_cost          NUMERIC(18,4),
    stock_value           NUMERIC(18,2),
    last_transaction_time TIMESTAMP(6),
    UNIQUE (item_id, warehouse_id, lot_id)
);
CREATE INDEX idx_stock_item ON global_inventory_stock_balances(item_id);
CREATE INDEX idx_stock_wh   ON global_inventory_stock_balances(warehouse_id);
CREATE INDEX idx_stock_lot  ON global_inventory_stock_balances(lot_id);

CREATE TABLE global_inventory_transactions (
    id                   BIGSERIAL       PRIMARY KEY,
    organization_id      BIGINT          NOT NULL,
    item_id              BIGINT          NOT NULL REFERENCES inv_items(id),
    warehouse_id         BIGINT          NOT NULL REFERENCES org_warehouses(id),
    lot_id               BIGINT          REFERENCES global_inv_lots(id),
    business_document_id BIGINT          NOT NULL REFERENCES global_business_documents(id),
    document_type        VARCHAR(50)     NOT NULL,
    movement_type        VARCHAR(50)     NOT NULL,
    transaction_date     DATE,
    quantity             NUMERIC(18,3)   NOT NULL,
    gross_weight         NUMERIC(12,3),
    net_weight           NUMERIC(12,3),
    unit_cost            NUMERIC(18,4),
    total_cost           NUMERIC(18,2),
    balance_after        NUMERIC(18,3),
    remarks              VARCHAR(255),
    created_at           TIMESTAMP(6)
);
CREATE INDEX idx_invtx_item ON global_inventory_transactions(item_id);
CREATE INDEX idx_invtx_wh   ON global_inventory_transactions(warehouse_id);
CREATE INDEX idx_invtx_doc  ON global_inventory_transactions(business_document_id);
CREATE INDEX idx_invtx_date ON global_inventory_transactions(transaction_date);
CREATE INDEX idx_invtx_org  ON global_inventory_transactions(organization_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 09 — COMMERCIAL / LC
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE com_commercial_invoice (
    id                BIGSERIAL       PRIMARY KEY,
    organization_id   BIGINT          REFERENCES org_organizations(id),
    lc_id             BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    party_id          BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    invoice_no        VARCHAR(255)    NOT NULL UNIQUE,
    invoice_date      DATE,
    invoice_type      VARCHAR(20)
        CONSTRAINT chk_ci_type CHECK (invoice_type IN ('EXPORT','IMPORT')),
    status            VARCHAR(20)
        CONSTRAINT chk_ci_status CHECK (status IN ('DRAFT','FINALIZED','POSTED','CANCELLED')),
    currency          VARCHAR(10),
    exchange_rate     NUMERIC(18,4),
    total_amount      NUMERIC(18,2),
    total_amount_bdt  NUMERIC(18,2),
    incoterms         VARCHAR(255),
    port_of_loading   VARCHAR(255),
    port_of_discharge VARCHAR(255),
    vessel_name       VARCHAR(255),
    bl_number         VARCHAR(255),
    container_no      VARCHAR(255),
    remarks           TEXT,
    created_at        TIMESTAMP(6),
    updated_at        TIMESTAMP(6)
);

CREATE TABLE com_commercial_invoice_item (
    id             BIGSERIAL       PRIMARY KEY,
    invoice_id     BIGINT          NOT NULL REFERENCES com_commercial_invoice(id),
    item_id        BIGINT          NOT NULL REFERENCES inv_items(id),
    quantity       NUMERIC(18,3)   NOT NULL,
    unit_price     NUMERIC(18,4)   NOT NULL,
    total_amount   NUMERIC(18,2)   NOT NULL,
    description    VARCHAR(500),
    unit           VARCHAR(20)
);

CREATE TABLE com_document_terms (
    id              BIGSERIAL    PRIMARY KEY,
    document_id     BIGINT       REFERENCES global_business_documents(id),
    invoice_id      BIGINT       REFERENCES com_commercial_invoice(id),
    global_terms_id BIGINT,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    sort_order      INT
);

CREATE TABLE com_lc_document_mapping (
    id               BIGSERIAL       PRIMARY KEY,
    lc_id            BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    document_id      BIGINT          REFERENCES global_business_documents(id),
    allocated_amount NUMERIC(18,2),
    utilized_amount  NUMERIC(18,2)
);

CREATE TABLE com_lc_settlement (
    id              BIGSERIAL       PRIMARY KEY,
    lc_id           BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    document_id     BIGINT          REFERENCES global_business_documents(id),
    settlement_date DATE,
    settlement_type VARCHAR(20),
    status          VARCHAR(20),
    amount_usd      NUMERIC(18,2),
    amount_bdt      NUMERIC(18,2),
    exchange_rate   NUMERIC(18,4),
    margin_used     NUMERIC(18,2),
    charges         NUMERIC(18,2),
    commission      NUMERIC(18,2),
    interest        NUMERIC(18,2),
    loan_amount     NUMERIC(18,2)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 10 — HRM
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE hrm_designations (
    id               BIGSERIAL    PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES org_organizations(id),
    designation_code VARCHAR(50)  NOT NULL,
    designation_name VARCHAR(200) NOT NULL,
    grade            VARCHAR(20),
    description      VARCHAR(500),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6),
    UNIQUE (organization_id, designation_code)
);
CREATE INDEX idx_desig_org ON hrm_designations(organization_id);

CREATE TABLE hrm_employees (
    id                         BIGSERIAL    PRIMARY KEY,
    organization_id            BIGINT       NOT NULL REFERENCES org_organizations(id),
    department_id              BIGINT       NOT NULL REFERENCES org_departments(id),
    designation_id             BIGINT       NOT NULL REFERENCES hrm_designations(id),
    reporting_manager_id       BIGINT       REFERENCES hrm_employees(id),
    user_id                    BIGINT       UNIQUE REFERENCES sec_users(id),
    employee_code              VARCHAR(50)  NOT NULL UNIQUE,
    first_name                 VARCHAR(100) NOT NULL,
    last_name                  VARCHAR(100) NOT NULL,
    email                      VARCHAR(100),
    phone                      VARCHAR(20)  NOT NULL UNIQUE,
    gender                     VARCHAR(20)  NOT NULL DEFAULT 'MALE'
        CONSTRAINT chk_emp_gender CHECK (gender IN ('MALE','FEMALE','OTHER')),
    date_of_birth              DATE         NOT NULL,
    blood_group                VARCHAR(10),
    marital_status             VARCHAR(20),
    national_id                VARCHAR(50)  UNIQUE,
    passport_number            VARCHAR(50)  UNIQUE,
    employee_type              VARCHAR(20)  NOT NULL DEFAULT 'PERMANENT'
        CONSTRAINT chk_emp_type CHECK (employee_type IN (
            'PERMANENT','CONTRACT','TEMPORARY','INTERN','PART_TIME','CONSULTANT')),
    status                     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT chk_emp_status CHECK (status IN (
            'ACTIVE','INACTIVE','ON_LEAVE','SUSPENDED',
            'TERMINATED','RESIGNED','RETIRED')),
    joining_date               DATE         NOT NULL,
    confirmation_date          DATE,
    probation_end_date         DATE,
    resignation_date           DATE,
    exit_date                  DATE,
    basic_salary               NUMERIC(12,2),
    gross_salary               NUMERIC(12,2),
    bank_name                  VARCHAR(50),
    bank_account_number        VARCHAR(50),
    bank_branch                VARCHAR(50),
    work_location              VARCHAR(50),
    work_shift                 VARCHAR(50),
    annual_leave_days          INT          DEFAULT 0,
    sick_leave_days            INT          DEFAULT 0,
    casual_leave_days          INT          DEFAULT 0,
    emergency_contact_name     VARCHAR(100),
    emergency_contact_phone    VARCHAR(20),
    emergency_contact_relation VARCHAR(100),
    profile_picture            VARCHAR(255),
    notes                      VARCHAR(1000),
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100),
    created_at                 TIMESTAMP(6),
    updated_at                 TIMESTAMP(6),
    UNIQUE (organization_id, employee_code)
);
CREATE INDEX idx_emp_org    ON hrm_employees(organization_id);
CREATE INDEX idx_emp_dept   ON hrm_employees(department_id);
CREATE INDEX idx_emp_desig  ON hrm_employees(designation_id);
CREATE INDEX idx_emp_mgr    ON hrm_employees(reporting_manager_id);
CREATE INDEX idx_emp_status ON hrm_employees(status);

-- Resolve deferred FK: org_departments.head_employee_id
ALTER TABLE org_departments
    ADD CONSTRAINT fk_dept_head
    FOREIGN KEY (head_employee_id) REFERENCES hrm_employees(id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE hrm_employee_addresses (
    id           BIGSERIAL    PRIMARY KEY,
    employee_id  BIGINT       NOT NULL REFERENCES hrm_employees(id) ON DELETE CASCADE,
    address_type VARCHAR(20)  NOT NULL
        CONSTRAINT chk_hea_type CHECK (address_type IN ('PRESENT','PERMANENT','OFFICE')),
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city          VARCHAR(100),
    district      VARCHAR(100),
    country       VARCHAR(100) DEFAULT 'Bangladesh',
    postal_code   VARCHAR(20),
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP(6)
);
CREATE INDEX idx_hea_emp ON hrm_employee_addresses(employee_id);

CREATE TABLE hrm_employee_documents (
    id                BIGSERIAL    PRIMARY KEY,
    employee_id       BIGINT       NOT NULL REFERENCES hrm_employees(id) ON DELETE CASCADE,
    document_type     VARCHAR(50)  NOT NULL,
    document_number   VARCHAR(100),
    file_url          VARCHAR(500),
    expiry_date       DATE,
    issue_date        DATE,
    issuing_authority VARCHAR(200),
    remarks           VARCHAR(500),
    uploaded_at       TIMESTAMP(6),
    uploaded_by       VARCHAR(100),
    created_at        TIMESTAMP(6)
);
CREATE INDEX idx_hed_emp ON hrm_employee_documents(employee_id);

CREATE TABLE hrm_attendances (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    employee_id     BIGINT       NOT NULL REFERENCES hrm_employees(id),
    att_date        DATE         NOT NULL,
    check_in        TIME,
    check_out       TIME,
    working_hours   NUMERIC(5,2),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ABSENT'
        CONSTRAINT chk_att_status CHECK (status IN (
            'PRESENT','ABSENT','LATE','HALF_DAY','HOLIDAY','LEAVE','WEEKEND')),
    source          VARCHAR(20)  DEFAULT 'MANUAL',
    remarks         VARCHAR(500),
    created_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (employee_id, att_date)
);
CREATE INDEX idx_att_emp  ON hrm_attendances(employee_id);
CREATE INDEX idx_att_date ON hrm_attendances(att_date);

CREATE TABLE hrm_employee_leaves (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    employee_id         BIGINT          NOT NULL REFERENCES hrm_employees(id),
    approval_request_id BIGINT          REFERENCES apr_requests(id),
    leave_type          VARCHAR(30)     NOT NULL,
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    total_days          NUMERIC(5,1)    NOT NULL,
    reason              TEXT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_leave_status CHECK (status IN (
            'PENDING','APPROVED','REJECTED','CANCELLED')),
    approved_by         VARCHAR(100),
    approved_at         TIMESTAMP(6),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMP(6),
    updated_at          TIMESTAMP(6)
);
CREATE INDEX idx_leave_emp    ON hrm_employee_leaves(employee_id);
CREATE INDEX idx_leave_status ON hrm_employee_leaves(status);

CREATE TABLE hrm_employee_salaries (
    id                  BIGSERIAL       PRIMARY KEY,
    employee_id         BIGINT          NOT NULL REFERENCES hrm_employees(id),
    effective_date      DATE            NOT NULL,
    end_date            DATE,
    basic_salary        NUMERIC(12,2)   NOT NULL,
    house_rent          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    medical_allowance   NUMERIC(12,2)   NOT NULL DEFAULT 0,
    transport_allowance NUMERIC(12,2)   NOT NULL DEFAULT 0,
    other_allowances    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    gross_salary        NUMERIC(12,2)   NOT NULL,
    income_tax          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    provident_fund      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    other_deductions    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    net_salary          NUMERIC(12,2)   NOT NULL,
    is_current          BOOLEAN         NOT NULL DEFAULT TRUE,
    remarks             VARCHAR(500),
    created_by          VARCHAR(100),
    created_at          TIMESTAMP(6)
);
CREATE INDEX idx_sal_emp     ON hrm_employee_salaries(employee_id);
CREATE INDEX idx_sal_current ON hrm_employee_salaries(is_current);

CREATE TABLE hrm_payroll_runs (
    id              BIGSERIAL       PRIMARY KEY,
    organization_id BIGINT          NOT NULL REFERENCES org_organizations(id),
    journal_entry_id BIGINT         REFERENCES acc_journal_entry_master(id),
    payroll_month   VARCHAR(7)      NOT NULL,   -- YYYY-MM
    run_date        DATE            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_pr_status CHECK (status IN (
            'DRAFT','PROCESSING','COMPLETED','APPROVED','PAID','CANCELLED')),
    total_gross     NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_deductions NUMERIC(18,2)  NOT NULL DEFAULT 0,
    total_net       NUMERIC(18,2)   NOT NULL DEFAULT 0,
    employee_count  INT             NOT NULL DEFAULT 0,
    approved_by     VARCHAR(100),
    approved_at     TIMESTAMP(6),
    remarks         TEXT,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, payroll_month)
);
CREATE INDEX idx_pr_org    ON hrm_payroll_runs(organization_id);
CREATE INDEX idx_pr_month  ON hrm_payroll_runs(payroll_month);
CREATE INDEX idx_pr_status ON hrm_payroll_runs(status);

CREATE TABLE hrm_payroll_run_lines (
    id                  BIGSERIAL       PRIMARY KEY,
    payroll_run_id      BIGINT          NOT NULL REFERENCES hrm_payroll_runs(id) ON DELETE CASCADE,
    employee_id         BIGINT          NOT NULL REFERENCES hrm_employees(id),
    cost_center_id      BIGINT          REFERENCES org_cost_centers(id),
    basic_salary        NUMERIC(12,2)   NOT NULL DEFAULT 0,
    house_rent          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    medical_allowance   NUMERIC(12,2)   NOT NULL DEFAULT 0,
    transport_allowance NUMERIC(12,2)   NOT NULL DEFAULT 0,
    overtime            NUMERIC(12,2)   NOT NULL DEFAULT 0,
    other_allowances    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    gross_salary        NUMERIC(12,2)   NOT NULL DEFAULT 0,
    income_tax          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    provident_fund      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    loan_deduction      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    other_deductions    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    net_salary          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    working_days        INT,
    leave_days          INT,
    absent_days         INT,
    payment_status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP(6)
);
CREATE INDEX idx_prl_run ON hrm_payroll_run_lines(payroll_run_id);
CREATE INDEX idx_prl_emp ON hrm_payroll_run_lines(employee_id);

-- ★ NEW: Employee cost center allocation for production labor costing
CREATE TABLE hrm_cost_center_allocations (
    id                 BIGSERIAL       PRIMARY KEY,
    organization_id    BIGINT          NOT NULL REFERENCES org_organizations(id),
    employee_id        BIGINT          NOT NULL REFERENCES hrm_employees(id),
    payroll_run_id     BIGINT          REFERENCES hrm_payroll_runs(id),
    cost_center_id     BIGINT          NOT NULL REFERENCES org_cost_centers(id),
    allocation_month   VARCHAR(7)      NOT NULL,   -- YYYY-MM
    gross_salary       NUMERIC(12,2)   NOT NULL,
    allocation_pct     NUMERIC(5,2)    NOT NULL DEFAULT 100,  -- % of time in this cost center
    allocated_amount   NUMERIC(12,2)   NOT NULL,              -- gross_salary × allocation_pct / 100
    remarks            VARCHAR(500),
    created_by         VARCHAR(100),
    created_at         TIMESTAMP(6),
    updated_at         TIMESTAMP(6),
    UNIQUE (employee_id, cost_center_id, allocation_month)
);
CREATE INDEX idx_hcca_emp    ON hrm_cost_center_allocations(employee_id);
CREATE INDEX idx_hcca_cc     ON hrm_cost_center_allocations(cost_center_id);
CREATE INDEX idx_hcca_month  ON hrm_cost_center_allocations(allocation_month);
CREATE INDEX idx_hcca_payrun ON hrm_cost_center_allocations(payroll_run_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 11 — GENERIC PRODUCTION  (replaces yarn-specific module)
-- ═══════════════════════════════════════════════════════════════════════════
--
--  Flow:
--    BOM (master template, optional)
--    ↓
--    Production Order (prd_productions)  ←─ Sales Order or standalone
--    ↓
--    Material Requisition / Issue  (uses global_business_documents)
--    ↓
--    prd_production_inputs  (actual materials consumed + lots)
--    ↓
--    prd_production_outputs (finished goods produced + lots)
--    ↓
--    Accounting journals (auto via acc_mapping: WIP Dr / RM Cr, FG Dr / WIP Cr)
--
--  Cost Sheet in prd_productions:
--    material_cost  = sum(prd_production_inputs.total_cost)
--    labor_cost     = from hrm_cost_center_allocations
--    overhead_cost  = manual entry or overhead allocation
--    total_cost     = material + labor + overhead
--    unit_cost      = total_cost / produced_quantity
-- ═══════════════════════════════════════════════════════════════════════════

-- Bill of Materials (reusable master template)
CREATE TABLE prd_bom (
    id                BIGSERIAL    PRIMARY KEY,
    organization_id   BIGINT       NOT NULL REFERENCES org_organizations(id),
    finished_item_id  BIGINT       NOT NULL REFERENCES inv_items(id),
    bom_code          VARCHAR(50)  NOT NULL,
    bom_name          VARCHAR(200) NOT NULL,
    bom_version       VARCHAR(20)  NOT NULL DEFAULT '1.0',
    output_quantity   NUMERIC(14,3) NOT NULL DEFAULT 1,  -- quantity produced per BOM run
    output_unit_id    BIGINT        NOT NULL REFERENCES inv_item_uom(id),
    yield_percent     NUMERIC(5,2)  NOT NULL DEFAULT 100, -- expected output %
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    is_default        BOOLEAN       NOT NULL DEFAULT FALSE,
    description       TEXT,
    notes             TEXT,
    approved_by       VARCHAR(100),
    approved_at       TIMESTAMP(6),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    created_at        TIMESTAMP(6),
    updated_at        TIMESTAMP(6),
    UNIQUE (organization_id, bom_code)
);
CREATE INDEX idx_bom_org  ON prd_bom(organization_id);
CREATE INDEX idx_bom_item ON prd_bom(finished_item_id);

CREATE TABLE prd_bom_items (
    id             BIGSERIAL       PRIMARY KEY,
    bom_id         BIGINT          NOT NULL REFERENCES prd_bom(id) ON DELETE CASCADE,
    raw_item_id    BIGINT          NOT NULL REFERENCES inv_items(id),
    line_number    INT             NOT NULL,
    quantity       NUMERIC(14,4)   NOT NULL,   -- quantity per BOM output_quantity
    unit_id        BIGINT          NOT NULL REFERENCES inv_item_uom(id),
    scrap_pct      NUMERIC(5,2)    NOT NULL DEFAULT 0,
    is_optional    BOOLEAN         NOT NULL DEFAULT FALSE,
    remarks        TEXT,
    created_by     VARCHAR(100),
    created_at     TIMESTAMP(6)
);
CREATE INDEX idx_bom_items_bom  ON prd_bom_items(bom_id);
CREATE INDEX idx_bom_items_item ON prd_bom_items(raw_item_id);

-- Production Order (generic work order + cost sheet)
CREATE TABLE prd_productions (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    bom_id              BIGINT          REFERENCES prd_bom(id),
    finished_item_id    BIGINT          NOT NULL REFERENCES inv_items(id),
    output_warehouse_id BIGINT          NOT NULL REFERENCES org_warehouses(id),
    cost_center_id      BIGINT          REFERENCES org_cost_centers(id),
    sales_order_id      BIGINT          REFERENCES global_business_documents(id),
    approval_request_id BIGINT          REFERENCES apr_requests(id),
    journal_entry_id    BIGINT          REFERENCES acc_journal_entry_master(id),

    production_no       VARCHAR(50)     NOT NULL,
    production_date     DATE            NOT NULL,
    planned_start_date  DATE,
    planned_end_date    DATE,
    actual_start_date   DATE,
    actual_end_date     DATE,

    -- Quantities
    planned_quantity    NUMERIC(14,3)   NOT NULL,
    produced_quantity   NUMERIC(14,3)   NOT NULL DEFAULT 0,
    rejected_quantity   NUMERIC(14,3)   NOT NULL DEFAULT 0,
    waste_quantity      NUMERIC(14,3)   NOT NULL DEFAULT 0,
    output_unit_id      BIGINT          NOT NULL REFERENCES inv_item_uom(id),

    -- ★ Cost Sheet — the key addition for COGS accuracy
    material_cost       NUMERIC(18,2)   NOT NULL DEFAULT 0,  -- auto from inputs
    labor_cost          NUMERIC(18,2)   NOT NULL DEFAULT 0,  -- from HRM allocation
    overhead_cost       NUMERIC(18,2)   NOT NULL DEFAULT 0,  -- manual or allocated
    other_cost          NUMERIC(18,2)   NOT NULL DEFAULT 0,  -- packaging, utilities etc.
    total_cost          NUMERIC(18,2)   NOT NULL DEFAULT 0,  -- sum of above
    unit_cost           NUMERIC(18,4)   NOT NULL DEFAULT 0,  -- total_cost / produced_qty

    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_prd2_status CHECK (status IN (
            'DRAFT','SUBMITTED','APPROVED','RELEASED',
            'IN_PROGRESS','COMPLETED','REJECTED','CANCELLED')),
    approval_status     VARCHAR(30),
    remarks             TEXT,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMP(6),
    updated_at          TIMESTAMP(6),
    UNIQUE (organization_id, production_no)
);
CREATE INDEX idx_prd2_org    ON prd_productions(organization_id);
CREATE INDEX idx_prd2_status ON prd_productions(status);
CREATE INDEX idx_prd2_item   ON prd_productions(finished_item_id);
CREATE INDEX idx_prd2_date   ON prd_productions(production_date);
CREATE INDEX idx_prd2_bom    ON prd_productions(bom_id);
CREATE INDEX idx_prd2_so     ON prd_productions(sales_order_id);

-- Resolve deferred FK: global_inv_lots.production_order_id → prd_productions
ALTER TABLE global_inv_lots
    ADD CONSTRAINT fk_lot_production
    FOREIGN KEY (production_order_id) REFERENCES prd_productions(id)
    DEFERRABLE INITIALLY DEFERRED;

-- Materials consumed in production
CREATE TABLE prd_production_inputs (
    id                  BIGSERIAL       PRIMARY KEY,
    production_id       BIGINT          NOT NULL REFERENCES prd_productions(id) ON DELETE CASCADE,
    raw_item_id         BIGINT          NOT NULL REFERENCES inv_items(id),
    lot_id              BIGINT          REFERENCES global_inv_lots(id),
    warehouse_id        BIGINT          NOT NULL REFERENCES org_warehouses(id),
    bom_item_id         BIGINT          REFERENCES prd_bom_items(id),
    line_number         INT             NOT NULL,
    planned_quantity    NUMERIC(14,3),
    actual_quantity     NUMERIC(14,3)   NOT NULL,
    unit_id             BIGINT          NOT NULL REFERENCES inv_item_uom(id),
    unit_cost           NUMERIC(18,4)   NOT NULL DEFAULT 0,
    total_cost          NUMERIC(18,2)   NOT NULL DEFAULT 0,
    scrap_quantity      NUMERIC(14,3)   NOT NULL DEFAULT 0,
    remarks             TEXT,
    created_by          VARCHAR(100),
    created_at          TIMESTAMP(6)
);
CREATE INDEX idx_prdi_prod  ON prd_production_inputs(production_id);
CREATE INDEX idx_prdi_item  ON prd_production_inputs(raw_item_id);
CREATE INDEX idx_prdi_lot   ON prd_production_inputs(lot_id);

-- Finished goods produced
CREATE TABLE prd_production_outputs (
    id                  BIGSERIAL       PRIMARY KEY,
    production_id       BIGINT          NOT NULL REFERENCES prd_productions(id) ON DELETE CASCADE,
    finished_item_id    BIGINT          NOT NULL REFERENCES inv_items(id),
    lot_id              BIGINT          REFERENCES global_inv_lots(id),
    warehouse_id        BIGINT          NOT NULL REFERENCES org_warehouses(id),
    line_number         INT             NOT NULL,
    quantity            NUMERIC(14,3)   NOT NULL,
    rejected_quantity   NUMERIC(14,3)   NOT NULL DEFAULT 0,
    unit_id             BIGINT          NOT NULL REFERENCES inv_item_uom(id),
    unit_cost           NUMERIC(18,4)   NOT NULL DEFAULT 0,  -- = production.unit_cost
    total_cost          NUMERIC(18,2)   NOT NULL DEFAULT 0,  -- quantity × unit_cost
    batch_no            VARCHAR(100),
    remarks             TEXT,
    created_by          VARCHAR(100),
    created_at          TIMESTAMP(6)
);
CREATE INDEX idx_prdo_prod ON prd_production_outputs(production_id);
CREATE INDEX idx_prdo_item ON prd_production_outputs(finished_item_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 12 — FIXED ASSETS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE fa_asset_categories (
    id                        BIGSERIAL    PRIMARY KEY,
    organization_id           BIGINT       NOT NULL REFERENCES org_organizations(id),
    parent_id                 BIGINT       REFERENCES fa_asset_categories(id),
    code                      VARCHAR(50)  NOT NULL,
    name                      VARCHAR(200) NOT NULL,
    description               TEXT,
    default_dep_method        VARCHAR(30)  NOT NULL DEFAULT 'STRAIGHT_LINE'
        CONSTRAINT chk_fac_method CHECK (default_dep_method IN (
            'STRAIGHT_LINE','DECLINING_BALANCE','UNITS_OF_PRODUCTION')),
    default_useful_life_years INT,
    default_dep_rate          NUMERIC(5,2),
    default_residual_pct      NUMERIC(5,2),
    gl_asset_account_id       BIGINT       REFERENCES acc_chart_of_accounts(id),
    gl_dep_exp_account_id     BIGINT       REFERENCES acc_chart_of_accounts(id),
    gl_accum_dep_account_id   BIGINT       REFERENCES acc_chart_of_accounts(id),
    gl_disposal_account_id    BIGINT       REFERENCES acc_chart_of_accounts(id),
    is_active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by                VARCHAR(100),
    updated_by                VARCHAR(100),
    created_at                TIMESTAMP(6),
    updated_at                TIMESTAMP(6),
    UNIQUE (organization_id, code)
);
CREATE INDEX idx_fac_org    ON fa_asset_categories(organization_id);
CREATE INDEX idx_fac_parent ON fa_asset_categories(parent_id);

CREATE TABLE fa_assets (
    id                       BIGSERIAL       PRIMARY KEY,
    organization_id          BIGINT          NOT NULL REFERENCES org_organizations(id),
    asset_category_id        BIGINT          NOT NULL REFERENCES fa_asset_categories(id),
    department_id            BIGINT          REFERENCES org_departments(id),
    cost_center_id           BIGINT          REFERENCES org_cost_centers(id),
    warehouse_id             BIGINT          REFERENCES org_warehouses(id),
    responsible_employee_id  BIGINT          REFERENCES hrm_employees(id),
    supplier_id              BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    linked_grn_id            BIGINT          REFERENCES global_business_documents(id),
    asset_code               VARCHAR(50)     NOT NULL,
    asset_name               VARCHAR(200)    NOT NULL,
    description              TEXT,
    serial_number            VARCHAR(100),
    model                    VARCHAR(100),
    manufacturer             VARCHAR(100),
    acquisition_date         DATE            NOT NULL,
    capitalisation_date      DATE,
    purchase_cost            NUMERIC(18,2)   NOT NULL,
    installation_cost        NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_cost               NUMERIC(18,2)   GENERATED ALWAYS AS (purchase_cost + installation_cost) STORED,
    currency                 VARCHAR(3)      NOT NULL DEFAULT 'BDT',
    depreciation_method      VARCHAR(30)     NOT NULL DEFAULT 'STRAIGHT_LINE',
    useful_life_years        INT,
    residual_value           NUMERIC(18,2)   NOT NULL DEFAULT 0,
    depreciation_rate        NUMERIC(5,2),
    depreciation_start_date  DATE,
    accumulated_depreciation NUMERIC(18,2)   NOT NULL DEFAULT 0,
    current_book_value       NUMERIC(18,2),
    last_dep_run_date        DATE,
    location                 VARCHAR(200),
    status                   VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT chk_fa_status CHECK (status IN (
            'ACTIVE','DISPOSED','TRANSFERRED','SOLD','WRITTEN_OFF','UNDER_MAINTENANCE')),
    condition                VARCHAR(20)     DEFAULT 'GOOD',
    warranty_expiry_date     DATE,
    insurance_policy_no      VARCHAR(100),
    insurance_expiry_date    DATE,
    barcode                  VARCHAR(100),
    notes                    TEXT,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    created_at               TIMESTAMP(6),
    updated_at               TIMESTAMP(6),
    UNIQUE (organization_id, asset_code)
);
CREATE INDEX idx_fa_org    ON fa_assets(organization_id);
CREATE INDEX idx_fa_cat    ON fa_assets(asset_category_id);
CREATE INDEX idx_fa_status ON fa_assets(status);

CREATE TABLE fa_depreciation_runs (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    journal_entry_id    BIGINT          REFERENCES acc_journal_entry_master(id),
    run_date            DATE            NOT NULL,
    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,
    run_type            VARCHAR(20)     NOT NULL DEFAULT 'MONTHLY',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    total_assets        INT             NOT NULL DEFAULT 0,
    total_depreciation  NUMERIC(18,2)   NOT NULL DEFAULT 0,
    posted_by           VARCHAR(100),
    posted_at           TIMESTAMP(6),
    created_by          VARCHAR(100),
    created_at          TIMESTAMP(6),
    updated_at          TIMESTAMP(6)
);
CREATE INDEX idx_fdr_org ON fa_depreciation_runs(organization_id);

CREATE TABLE fa_depreciation_run_lines (
    id                  BIGSERIAL       PRIMARY KEY,
    depreciation_run_id BIGINT          NOT NULL REFERENCES fa_depreciation_runs(id) ON DELETE CASCADE,
    asset_id            BIGINT          NOT NULL REFERENCES fa_assets(id),
    depreciation_method VARCHAR(30)     NOT NULL,
    opening_book_value  NUMERIC(18,2)   NOT NULL,
    depreciation_amount NUMERIC(18,2)   NOT NULL,
    closing_book_value  NUMERIC(18,2)   NOT NULL,
    rate_applied        NUMERIC(5,2),
    notes               TEXT,
    created_at          TIMESTAMP(6)
);
CREATE INDEX idx_fdrl_run   ON fa_depreciation_run_lines(depreciation_run_id);
CREATE INDEX idx_fdrl_asset ON fa_depreciation_run_lines(asset_id);

CREATE TABLE fa_asset_disposals (
    id                          BIGSERIAL       PRIMARY KEY,
    organization_id             BIGINT          NOT NULL REFERENCES org_organizations(id),
    asset_id                    BIGINT          NOT NULL REFERENCES fa_assets(id),
    journal_entry_id            BIGINT          REFERENCES acc_journal_entry_master(id),
    disposal_date               DATE            NOT NULL,
    disposal_type               VARCHAR(30)     NOT NULL
        CONSTRAINT chk_fad_type CHECK (disposal_type IN (
            'SALE','WRITE_OFF','TRANSFER','SCRAP','DONATION')),
    disposal_value              NUMERIC(18,2)   NOT NULL DEFAULT 0,
    book_value_at_disposal      NUMERIC(18,2)   NOT NULL,
    accumulated_dep_at_disposal NUMERIC(18,2)   NOT NULL,
    gain_loss                   NUMERIC(18,2),
    buyer_name                  VARCHAR(200),
    reason                      TEXT,
    approved_by                 VARCHAR(100),
    approved_at                 TIMESTAMP(6),
    created_by                  VARCHAR(100),
    created_at                  TIMESTAMP(6)
);
CREATE INDEX idx_fad_asset ON fa_asset_disposals(asset_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 13 — BUDGET
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE bgt_fiscal_years (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    year_code       VARCHAR(20)  NOT NULL,
    year_name       VARCHAR(100) NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_bfy_status CHECK (status IN ('DRAFT','ACTIVE','LOCKED','CLOSED')),
    is_current      BOOLEAN      NOT NULL DEFAULT FALSE,
    closed_by       VARCHAR(100),
    closed_at       TIMESTAMP(6),
    notes           TEXT,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, year_code)
);
CREATE INDEX idx_bfy_org ON bgt_fiscal_years(organization_id);

CREATE TABLE bgt_budget_heads (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    parent_id       BIGINT       REFERENCES bgt_budget_heads(id),
    head_code       VARCHAR(50)  NOT NULL,
    head_name       VARCHAR(200) NOT NULL,
    head_type       VARCHAR(30)  NOT NULL DEFAULT 'EXPENSE'
        CONSTRAINT chk_bbh_type CHECK (head_type IN (
            'REVENUE','EXPENSE','CAPEX','OPEX','PRODUCTION','HR','COMMERCIAL','OTHER')),
    description     TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order   INT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, head_code)
);
CREATE INDEX idx_bbh_org ON bgt_budget_heads(organization_id);

CREATE TABLE bgt_budgets (
    id                        BIGSERIAL       PRIMARY KEY,
    organization_id           BIGINT          NOT NULL REFERENCES org_organizations(id),
    business_unit_id          BIGINT          REFERENCES org_business_units(id),
    fiscal_year_id            BIGINT          NOT NULL REFERENCES bgt_fiscal_years(id),
    approval_request_id       BIGINT          REFERENCES apr_requests(id),
    budget_no                 VARCHAR(50)     NOT NULL,
    budget_name               VARCHAR(200)    NOT NULL,
    budget_type               VARCHAR(30)     NOT NULL DEFAULT 'ANNUAL'
        CONSTRAINT chk_bgt_type CHECK (budget_type IN (
            'ANNUAL','QUARTERLY','MONTHLY','PROJECT','DEPARTMENTAL','CAPEX','ROLLING')),
    period_start              DATE            NOT NULL,
    period_end                DATE            NOT NULL,
    currency                  VARCHAR(3)      NOT NULL DEFAULT 'BDT',
    total_budgeted            NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_revised             NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_actual              NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_committed           NUMERIC(18,2)   NOT NULL DEFAULT 0,
    total_available           NUMERIC(18,2)   NOT NULL DEFAULT 0,
    status                    VARCHAR(30)     NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_bgt_status CHECK (status IN (
            'DRAFT','SUBMITTED','IN_APPROVAL','APPROVED',
            'ACTIVE','LOCKED','CLOSED','REJECTED','RETURNED')),
    over_spend_policy         VARCHAR(20)     NOT NULL DEFAULT 'WARN'
        CONSTRAINT chk_bgt_policy CHECK (over_spend_policy IN ('ALLOW','WARN','BLOCK')),
    alert_threshold_pct       NUMERIC(5,2)    NOT NULL DEFAULT 80,
    created_by                VARCHAR(100),
    updated_by                VARCHAR(100),
    created_at                TIMESTAMP(6),
    updated_at                TIMESTAMP(6),
    UNIQUE (organization_id, budget_no)
);
CREATE INDEX idx_bgt_org    ON bgt_budgets(organization_id);
CREATE INDEX idx_bgt_fy     ON bgt_budgets(fiscal_year_id);
CREATE INDEX idx_bgt_status ON bgt_budgets(status);

CREATE TABLE bgt_budget_lines (
    id               BIGSERIAL       PRIMARY KEY,
    budget_id        BIGINT          NOT NULL REFERENCES bgt_budgets(id) ON DELETE CASCADE,
    budget_head_id   BIGINT          NOT NULL REFERENCES bgt_budget_heads(id),
    account_id       BIGINT          REFERENCES acc_chart_of_accounts(id),
    cost_center_id   BIGINT          REFERENCES org_cost_centers(id),
    department_id    BIGINT          REFERENCES org_departments(id),
    line_number      INT             NOT NULL,
    description      VARCHAR(500),
    original_amount  NUMERIC(18,2)   NOT NULL DEFAULT 0,
    revised_amount   NUMERIC(18,2)   NOT NULL DEFAULT 0,
    actual_amount    NUMERIC(18,2)   NOT NULL DEFAULT 0,
    committed_amount NUMERIC(18,2)   NOT NULL DEFAULT 0,
    available_amount NUMERIC(18,2)   GENERATED ALWAYS AS
                         (revised_amount - actual_amount - committed_amount) STORED,
    jan_amount NUMERIC(18,2) NOT NULL DEFAULT 0, feb_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    mar_amount NUMERIC(18,2) NOT NULL DEFAULT 0, apr_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    may_amount NUMERIC(18,2) NOT NULL DEFAULT 0, jun_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    jul_amount NUMERIC(18,2) NOT NULL DEFAULT 0, aug_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    sep_amount NUMERIC(18,2) NOT NULL DEFAULT 0, oct_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    nov_amount NUMERIC(18,2) NOT NULL DEFAULT 0, dec_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    notes            TEXT,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6)
);
CREATE INDEX idx_bbl_budget ON bgt_budget_lines(budget_id);
CREATE INDEX idx_bbl_head   ON bgt_budget_lines(budget_head_id);

CREATE TABLE bgt_actuals (
    id                    BIGSERIAL       PRIMARY KEY,
    organization_id       BIGINT          NOT NULL REFERENCES org_organizations(id),
    budget_id             BIGINT          NOT NULL REFERENCES bgt_budgets(id),
    budget_line_id        BIGINT          NOT NULL REFERENCES bgt_budget_lines(id),
    journal_entry_id      BIGINT          NOT NULL REFERENCES acc_journal_entry_master(id),
    journal_entry_line_id BIGINT          REFERENCES acc_journal_entry_lines(id),
    source_document_type  VARCHAR(50),
    source_document_id    BIGINT,
    source_document_no    VARCHAR(100),
    transaction_date      DATE            NOT NULL,
    debit_amount          NUMERIC(18,2)   NOT NULL DEFAULT 0,
    credit_amount         NUMERIC(18,2)   NOT NULL DEFAULT 0,
    net_amount            NUMERIC(18,2)   NOT NULL DEFAULT 0,
    narration             VARCHAR(500),
    created_by            VARCHAR(100),
    created_at            TIMESTAMP(6)
);
CREATE INDEX idx_ba_budget  ON bgt_actuals(budget_id);
CREATE INDEX idx_ba_line    ON bgt_actuals(budget_line_id);
CREATE INDEX idx_ba_journal ON bgt_actuals(journal_entry_id);

CREATE TABLE bgt_encumbrances (
    id                    BIGSERIAL       PRIMARY KEY,
    organization_id       BIGINT          NOT NULL REFERENCES org_organizations(id),
    budget_id             BIGINT          NOT NULL REFERENCES bgt_budgets(id),
    budget_line_id        BIGINT          NOT NULL REFERENCES bgt_budget_lines(id),
    source_document_id    BIGINT          NOT NULL REFERENCES global_business_documents(id),
    source_document_type  VARCHAR(50)     NOT NULL,
    source_document_no    VARCHAR(100)    NOT NULL,
    committed_amount      NUMERIC(18,2)   NOT NULL,
    released_amount       NUMERIC(18,2)   NOT NULL DEFAULT 0,
    outstanding_amount    NUMERIC(18,2)   GENERATED ALWAYS AS (committed_amount - released_amount) STORED,
    commitment_date       DATE            NOT NULL,
    status                VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
        CONSTRAINT chk_be_status CHECK (status IN (
            'OPEN','PARTIAL','FULLY_RELEASED','CANCELLED')),
    notes                 TEXT,
    created_by            VARCHAR(100),
    created_at            TIMESTAMP(6),
    updated_at            TIMESTAMP(6)
);
CREATE INDEX idx_be_budget ON bgt_encumbrances(budget_id);
CREATE INDEX idx_be_status ON bgt_encumbrances(status);

CREATE TABLE bgt_budget_notes (
    id          BIGSERIAL    PRIMARY KEY,
    budget_id   BIGINT       NOT NULL REFERENCES bgt_budgets(id) ON DELETE CASCADE,
    author_id   BIGINT       REFERENCES sec_users(id),
    note_text   TEXT         NOT NULL,
    is_internal BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  VARCHAR(100),
    created_at  TIMESTAMP(6)
);
CREATE INDEX idx_bbn_budget ON bgt_budget_notes(budget_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 14 — CRM
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE crm_leads (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    assigned_to_id  BIGINT       REFERENCES sec_users(id),
    converted_to_id BIGINT       REFERENCES acc_chart_of_accounts_sub(id),
    lead_no         VARCHAR(50)  NOT NULL,
    company_name    VARCHAR(200),
    contact_name    VARCHAR(200) NOT NULL,
    contact_email   VARCHAR(100),
    contact_phone   VARCHAR(20),
    designation     VARCHAR(100),
    country         VARCHAR(100),
    city            VARCHAR(100),
    source          VARCHAR(50),
    lead_type       VARCHAR(20)  NOT NULL DEFAULT 'B2B',
    product_interest TEXT,
    status          VARCHAR(30)  NOT NULL DEFAULT 'NEW'
        CONSTRAINT chk_crl_status CHECK (status IN (
            'NEW','CONTACTED','QUALIFIED','UNQUALIFIED',
            'CONVERTED','LOST','DORMANT')),
    remarks         TEXT,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    UNIQUE (organization_id, lead_no)
);
CREATE INDEX idx_crl_org    ON crm_leads(organization_id);
CREATE INDEX idx_crl_status ON crm_leads(status);

CREATE TABLE crm_opportunities (
    id                  BIGSERIAL       PRIMARY KEY,
    organization_id     BIGINT          NOT NULL REFERENCES org_organizations(id),
    customer_id         BIGINT          REFERENCES acc_chart_of_accounts_sub(id),
    lead_id             BIGINT          REFERENCES crm_leads(id),
    assigned_to_id      BIGINT          REFERENCES sec_users(id),
    opportunity_no      VARCHAR(50)     NOT NULL,
    title               VARCHAR(200)    NOT NULL,
    description         TEXT,
    stage               VARCHAR(30)     NOT NULL DEFAULT 'PROSPECT'
        CONSTRAINT chk_cro_stage CHECK (stage IN (
            'PROSPECT','QUALIFIED','PROPOSAL','NEGOTIATION','WON','LOST')),
    probability         NUMERIC(5,2)    NOT NULL DEFAULT 0,
    estimated_value     NUMERIC(18,2),
    currency            VARCHAR(3)      NOT NULL DEFAULT 'BDT',
    expected_close_date DATE,
    actual_close_date   DATE,
    lost_reason         VARCHAR(500),
    remarks             TEXT,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    created_at          TIMESTAMP(6),
    updated_at          TIMESTAMP(6),
    UNIQUE (organization_id, opportunity_no)
);
CREATE INDEX idx_cro_org   ON crm_opportunities(organization_id);
CREATE INDEX idx_cro_stage ON crm_opportunities(stage);

CREATE TABLE crm_activities (
    id               BIGSERIAL    PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES org_organizations(id),
    opportunity_id   BIGINT       REFERENCES crm_opportunities(id),
    lead_id          BIGINT       REFERENCES crm_leads(id),
    customer_id      BIGINT       REFERENCES acc_chart_of_accounts_sub(id),
    assigned_to_id   BIGINT       REFERENCES sec_users(id),
    activity_type    VARCHAR(30)  NOT NULL
        CONSTRAINT chk_cra_type CHECK (activity_type IN (
            'CALL','EMAIL','MEETING','VISIT','DEMO',
            'QUOTATION','FOLLOW_UP','NOTE','OTHER')),
    subject          VARCHAR(200) NOT NULL,
    description      TEXT,
    activity_date    DATE         NOT NULL,
    duration_minutes INT,
    outcome          VARCHAR(500),
    next_action      VARCHAR(500),
    next_action_date DATE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PLANNED'
        CONSTRAINT chk_cra_status CHECK (status IN ('PLANNED','COMPLETED','CANCELLED')),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6)
);
CREATE INDEX idx_cra_org  ON crm_activities(organization_id);
CREATE INDEX idx_cra_opp  ON crm_activities(opportunity_id);
CREATE INDEX idx_cra_date ON crm_activities(activity_date);

CREATE TABLE crm_contacts (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES org_organizations(id),
    customer_id     BIGINT       REFERENCES acc_chart_of_accounts_sub(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100),
    designation     VARCHAR(100),
    department      VARCHAR(100),
    email           VARCHAR(100),
    phone           VARCHAR(20),
    mobile          VARCHAR(20),
    whatsapp        VARCHAR(20),
    is_primary      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    notes           TEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6)
);
CREATE INDEX idx_crc_customer ON crm_contacts(customer_id);

CREATE TABLE crm_customer_feedback (
    id                   BIGSERIAL    PRIMARY KEY,
    organization_id      BIGINT       NOT NULL REFERENCES org_organizations(id),
    customer_id          BIGINT       NOT NULL REFERENCES acc_chart_of_accounts_sub(id),
    business_document_id BIGINT       REFERENCES global_business_documents(id),
    feedback_date        DATE         NOT NULL DEFAULT CURRENT_DATE,
    feedback_type        VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    rating               INT          CONSTRAINT chk_crf_rating CHECK (rating BETWEEN 1 AND 5),
    subject              VARCHAR(200),
    description          TEXT,
    resolution           TEXT,
    resolved_by          VARCHAR(100),
    resolved_at          TIMESTAMP(6),
    status               VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
        CONSTRAINT chk_crf_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    created_by           VARCHAR(100),
    created_at           TIMESTAMP(6),
    updated_at           TIMESTAMP(6)
);
CREATE INDEX idx_crf_customer ON crm_customer_feedback(customer_id);
CREATE INDEX idx_crf_status   ON crm_customer_feedback(status);

-- ═══════════════════════════════════════════════════════════════════════════
-- MODULE 15 — NOTIFICATIONS & AUDIT
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE ntf_notifications (
    id                BIGSERIAL    PRIMARY KEY,
    organization_id   BIGINT       NOT NULL REFERENCES org_organizations(id),
    recipient_id      BIGINT       NOT NULL REFERENCES sec_users(id),
    notification_type VARCHAR(30)  NOT NULL DEFAULT 'IN_APP'
        CONSTRAINT chk_ntf_type CHECK (notification_type IN (
            'IN_APP','EMAIL','SMS','PUSH','WHATSAPP')),
    category          VARCHAR(50),
    title             VARCHAR(200) NOT NULL,
    message           TEXT,
    link              VARCHAR(500),
    reference_type    VARCHAR(50),
    reference_id      BIGINT,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at           TIMESTAMP(6),
    sent_at           TIMESTAMP(6),
    delivery_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP(6),
    updated_at        TIMESTAMP(6)
);
CREATE INDEX idx_ntf_org       ON ntf_notifications(organization_id);
CREATE INDEX idx_ntf_recipient ON ntf_notifications(recipient_id);
CREATE INDEX idx_ntf_read      ON ntf_notifications(is_read);
CREATE INDEX idx_ntf_created   ON ntf_notifications(created_at DESC);

CREATE TABLE sys_audit_log (
    id              BIGSERIAL    PRIMARY KEY,
    organization_id BIGINT       REFERENCES org_organizations(id),
    user_id         BIGINT       REFERENCES sec_users(id),
    username        VARCHAR(100),
    action          VARCHAR(50)  NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       BIGINT,
    entity_code     VARCHAR(100),
    old_values      TEXT,
    new_values      TEXT,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(500),
    session_id      VARCHAR(100),
    remarks         VARCHAR(500),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

CREATE TABLE sys_audit_log_default PARTITION OF sys_audit_log DEFAULT;

CREATE INDEX idx_sal_org     ON sys_audit_log(organization_id);
CREATE INDEX idx_sal_user    ON sys_audit_log(user_id);
CREATE INDEX idx_sal_entity  ON sys_audit_log(entity_type, entity_id);
CREATE INDEX idx_sal_created ON sys_audit_log(created_at DESC);
CREATE INDEX idx_sal_action  ON sys_audit_log(action);

-- ═══════════════════════════════════════════════════════════════════════════
-- VIEWS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_approval_inbox AS
SELECT ar.id, ar.organization_id, ar.document_type,
       ar.reference_id, ar.reference_number AS doc_no,
       ar.document_amount, ar.document_summary,
       ar.current_level_number, ar.total_levels,
       ar.current_approver_user_id, ar.requester_name,
       ar.is_urgent, ar.due_date, ar.status,
       ar.created_at AS submitted_at,
       u.username    AS approver_username,
       u.full_name   AS approver_full_name
FROM apr_requests ar
LEFT JOIN sec_users u ON u.id = ar.current_approver_user_id
WHERE ar.status IN ('IN_APPROVAL','SUBMITTED');

CREATE OR REPLACE VIEW v_stock_position AS
SELECT sb.item_id, ii.item_code, ii.item_name, ii.item_type,
       w.warehouse_code, w.warehouse_name, w.warehouse_type,
       l.lot_number, l.qc_grade, l.status AS lot_status,
       sb.quantity, sb.reserved_quantity,
       (sb.quantity - sb.reserved_quantity) AS available_quantity,
       sb.gross_weight, sb.net_weight,
       sb.average_cost, sb.stock_value,
       sb.last_transaction_time
FROM global_inventory_stock_balances sb
JOIN inv_items      ii ON ii.id = sb.item_id
JOIN org_warehouses w  ON w.id  = sb.warehouse_id
LEFT JOIN global_inv_lots l ON l.id = sb.lot_id
WHERE sb.quantity > 0;

CREATE OR REPLACE VIEW v_document_chain AS
SELECT child.id, child.document_no, child.document_type, child.status,
       child.total_amount, child.document_date, child.organization_id,
       parent.id          AS parent_id,
       parent.document_no AS parent_doc_no,
       parent.document_type AS parent_type
FROM global_business_documents child
LEFT JOIN global_business_documents parent ON parent.id = child.parent_document_id;

-- ★ NEW: Production cost sheet view
CREATE OR REPLACE VIEW v_production_cost_sheet AS
SELECT
    p.id AS production_id,
    p.organization_id,
    p.production_no,
    p.production_date,
    ii.item_code AS finished_item_code,
    ii.item_name AS finished_item_name,
    p.planned_quantity,
    p.produced_quantity,
    p.rejected_quantity,
    p.waste_quantity,
    -- Cost breakdown
    p.material_cost,
    p.labor_cost,
    p.overhead_cost,
    p.other_cost,
    p.total_cost,
    p.unit_cost,
    -- Efficiency
    CASE WHEN p.planned_quantity > 0
         THEN ROUND((p.produced_quantity / p.planned_quantity) * 100, 2)
         ELSE 0 END AS efficiency_pct,
    CASE WHEN p.total_cost > 0
         THEN ROUND((p.material_cost / p.total_cost) * 100, 2)
         ELSE 0 END AS material_cost_pct,
    p.status,
    cc.cost_center_name,
    w.warehouse_name AS output_warehouse
FROM prd_productions p
JOIN inv_items      ii ON ii.id = p.finished_item_id
JOIN org_warehouses w  ON w.id  = p.output_warehouse_id
LEFT JOIN org_cost_centers cc ON cc.id = p.cost_center_id;

-- ★ NEW: COGS summary (links sales → finished goods → production cost)
CREATE OR REPLACE VIEW v_cogs_summary AS
SELECT
    gbd.organization_id,
    gbd.document_no    AS sales_invoice_no,
    gbd.document_date  AS invoice_date,
    s.sub_account_name AS customer_name,
    gbdl.item_id,
    ii.item_code,
    ii.item_name,
    gbdl.quantity      AS sold_quantity,
    gbdl.unit_price,
    gbdl.line_amount   AS sales_amount,
    -- COGS from production unit cost
    l.unit_cost        AS production_unit_cost,
    gbdl.quantity * COALESCE(l.unit_cost, ii.standard_cost, 0) AS cogs_amount,
    gbdl.line_amount
        - (gbdl.quantity * COALESCE(l.unit_cost, ii.standard_cost, 0)) AS gross_profit
FROM global_business_documents gbd
JOIN global_business_document_lines gbdl ON gbdl.document_id = gbd.id
JOIN inv_items ii ON ii.id = gbdl.item_id
LEFT JOIN acc_chart_of_accounts_sub s ON s.id = gbd.party_id
LEFT JOIN global_inv_lots l ON l.id = gbdl.inventory_lot_id
WHERE gbd.document_type = 'SALES_INVOICE'
  AND gbd.is_deleted = FALSE;

CREATE OR REPLACE VIEW v_budget_vs_actual AS
SELECT b.organization_id, b.id AS budget_id, b.budget_no, b.budget_name,
       b.period_start, b.period_end, b.status AS budget_status,
       bl.id AS line_id,
       bh.head_code, bh.head_name, bh.head_type,
       acct.account_code, acct.account_name,
       cc.cost_center_code, cc.cost_center_name,
       d.name AS department_name,
       bl.original_amount, bl.revised_amount,
       bl.actual_amount, bl.committed_amount, bl.available_amount,
       CASE WHEN bl.revised_amount > 0
            THEN ROUND((bl.actual_amount / bl.revised_amount) * 100, 2)
            ELSE 0 END AS actual_pct,
       CASE WHEN bl.available_amount < 0 THEN 'OVER_BUDGET'
            WHEN (bl.actual_amount + bl.committed_amount) >=
                 bl.revised_amount * COALESCE(b.alert_threshold_pct, 80) / 100 THEN 'AT_RISK'
            ELSE 'ON_TRACK' END AS health_status
FROM bgt_budget_lines bl
JOIN bgt_budgets      b    ON b.id  = bl.budget_id
JOIN bgt_budget_heads bh   ON bh.id = bl.budget_head_id
LEFT JOIN acc_chart_of_accounts acct ON acct.id = bl.account_id
LEFT JOIN org_cost_centers      cc   ON cc.id   = bl.cost_center_id
LEFT JOIN org_departments       d    ON d.id    = bl.department_id;

CREATE OR REPLACE VIEW v_crm_pipeline AS
SELECT o.id, o.organization_id,
       s.sub_account_name AS customer_name,
       o.title, o.stage, o.estimated_value, o.currency,
       o.probability, o.expected_close_date,
       u.username AS assigned_to,
       ROUND(o.estimated_value * o.probability / 100, 2) AS weighted_value,
       (o.expected_close_date - CURRENT_DATE)::INT        AS days_to_close
FROM crm_opportunities o
LEFT JOIN acc_chart_of_accounts_sub s ON s.id = o.customer_id
LEFT JOIN sec_users u ON u.id = o.assigned_to_id
WHERE o.stage NOT IN ('WON','LOST');

CREATE OR REPLACE VIEW v_asset_register AS
SELECT a.organization_id, a.asset_code, a.asset_name,
       cat.name AS category, cat.code AS category_code,
       a.depreciation_method, a.useful_life_years,
       a.purchase_cost, a.total_cost,
       a.accumulated_depreciation,
       (a.total_cost - a.accumulated_depreciation) AS current_book_value,
       a.residual_value, a.acquisition_date,
       d.name AS department,
       cc.cost_center_name,
       a.status, a.condition, a.barcode
FROM fa_assets a
JOIN fa_asset_categories cat ON cat.id = a.asset_category_id
LEFT JOIN org_departments  d  ON d.id   = a.department_id
LEFT JOIN org_cost_centers cc ON cc.id  = a.cost_center_id
WHERE a.status = 'ACTIVE';

-- ★ Labor cost allocation view (for production cost sheet)
CREATE OR REPLACE VIEW v_labor_cost_by_month AS
SELECT
    hcca.organization_id,
    hcca.cost_center_id,
    cc.cost_center_name,
    hcca.allocation_month,
    COUNT(hcca.employee_id)         AS employee_count,
    SUM(hcca.gross_salary)          AS total_gross_salary,
    SUM(hcca.allocated_amount)      AS total_allocated_labor_cost
FROM hrm_cost_center_allocations hcca
JOIN org_cost_centers cc ON cc.id = hcca.cost_center_id
GROUP BY hcca.organization_id, hcca.cost_center_id, cc.cost_center_name, hcca.allocation_month;

-- ═══════════════════════════════════════════════════════════════════════════
-- SEED DATA
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO stp_currencies (code, name, symbol, decimal_places) VALUES
    ('BDT','Bangladeshi Taka','৳',2),
    ('USD','US Dollar','$',2),
    ('EUR','Euro','€',2),
    ('GBP','British Pound','£',2),
    ('CNY','Chinese Yuan','¥',2),
    ('INR','Indian Rupee','₹',2),
    ('JPY','Japanese Yen','¥',0),
    ('AED','UAE Dirham','د.إ',2),
    ('SAR','Saudi Riyal','﷼',2),
    ('SGD','Singapore Dollar','S$',2)
ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- SUMMARY
-- Total Tables  : 103
-- Total Indexes : ~220
-- Total Views   :   8  (6 existing + v_production_cost_sheet + v_cogs_summary
--                       + v_labor_cost_by_month)
-- Modules       :  15
--
-- REMOVED (vs v1):
--   yrn_types, yrn_counts, yarn_products, yarn_blend_items
--   prd_orders, prd_recipes, prd_recipe_items, prd_recipe_item_lots
--
-- ADDED (vs v1):
--   prd_bom, prd_bom_items
--   prd_productions, prd_production_inputs, prd_production_outputs
--   hrm_cost_center_allocations
--   v_production_cost_sheet, v_cogs_summary, v_labor_cost_by_month
-- ═══════════════════════════════════════════════════════════════════════════
