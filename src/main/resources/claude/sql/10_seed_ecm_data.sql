BEGIN;

-- ============================================================================
-- ROOT CATEGORIES
-- ============================================================================

INSERT INTO ec_categories
(
    category_code,
    category_name,
    slug,
    level_no,
    display_order,
    active,
    deleted,
    is_featured,
    is_menu,
    organization_id,
    created_at,
    updated_at,
    created_by,
    updated_by
)
VALUES
    ('ROOT-ELEC','Electronics','electronics',1,1,true,false,true,true,1,NOW(),NOW(),'system','system'),
    ('ROOT-FASH','Fashion','fashion',1,2,true,false,true,true,1,NOW(),NOW(),'system','system'),
    ('ROOT-HOME','Home & Kitchen','home-kitchen',1,3,true,false,true,true,1,NOW(),NOW(),'system','system'),
    ('ROOT-BEAUTY','Beauty','beauty',1,4,true,false,true,true,1,NOW(),NOW(),'system','system'),
    ('ROOT-SPORT','Sports','sports',1,5,true,false,false,true,1,NOW(),NOW(),'system','system'),
    ('ROOT-BOOK','Books','books',1,6,true,false,false,true,1,NOW(),NOW(),'system','system'),
    ('ROOT-GROC','Groceries','groceries',1,7,true,false,true,true,1,NOW(),NOW(),'system','system');

-- ============================================================================
-- ELECTRONICS
-- ============================================================================

INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,2,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('ELEC-MOBILE','Mobile Phones','mobile-phones',1),
             ('ELEC-LAPTOP','Laptops','laptops',2),
             ('ELEC-TABLET','Tablets','tablets',3),
             ('ELEC-TV','Television','television',4),
             ('ELEC-CAMERA','Camera','camera',5),
             ('ELEC-ACCESSORY','Accessories','accessories',6)
     ) v(code,name,slug,sort)
WHERE p.category_code='ROOT-ELEC';

-- Mobile Phones
INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,3,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('ELEC-MOBILE-ANDROID','Android Phones','android-phones',1),
             ('ELEC-MOBILE-IPHONE','iPhone','iphone',2),
             ('ELEC-MOBILE-FEATURE','Feature Phones','feature-phones',3)
     ) v(code,name,slug,sort)
WHERE p.category_code='ELEC-MOBILE';

-- ============================================================================
-- FASHION
-- ============================================================================

INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,2,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('FASH-MEN','Men','men',1),
             ('FASH-WOMEN','Women','women',2),
             ('FASH-KIDS','Kids','kids',3),
             ('FASH-SHOES','Shoes','shoes',4),
             ('FASH-BAGS','Bags','bags',5)
     ) v(code,name,slug,sort)
WHERE p.category_code='ROOT-FASH';

-- ============================================================================
-- HOME & KITCHEN
-- ============================================================================

INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,2,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('HOME-FURN','Furniture','furniture',1),
             ('HOME-KITCH','Kitchen','kitchen',2),
             ('HOME-DECOR','Home Decor','home-decor',3),
             ('HOME-LIGHT','Lighting','lighting',4)
     ) v(code,name,slug,sort)
WHERE p.category_code='ROOT-HOME';

-- ============================================================================
-- BEAUTY
-- ============================================================================

INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,2,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('BEAUTY-SKIN','Skin Care','skin-care',1),
             ('BEAUTY-HAIR','Hair Care','hair-care',2),
             ('BEAUTY-MAKEUP','Makeup','makeup',3),
             ('BEAUTY-PERFUME','Perfume','perfume',4)
     ) v(code,name,slug,sort)
WHERE p.category_code='ROOT-BEAUTY';

-- ============================================================================
-- SPORTS
-- ============================================================================

INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,2,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('SPORT-CRICKET','Cricket','cricket',1),
             ('SPORT-FOOTBALL','Football','football',2),
             ('SPORT-GYM','Fitness','fitness',3),
             ('SPORT-CYCLE','Cycling','cycling',4)
     ) v(code,name,slug,sort)
WHERE p.category_code='ROOT-SPORT';

-- ============================================================================
-- BOOKS
-- ============================================================================

INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,2,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('BOOK-ACADEMIC','Academic','academic',1),
             ('BOOK-NOVEL','Novels','novels',2),
             ('BOOK-RELIGION','Religion','religion',3),
             ('BOOK-CHILD','Children','children-books',4)
     ) v(code,name,slug,sort)
WHERE p.category_code='ROOT-BOOK';

-- ============================================================================
-- GROCERIES
-- ============================================================================

INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)

SELECT
    v.code,v.name,v.slug,2,p.id,
    v.sort,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
         CROSS JOIN
     (
         VALUES
             ('GROC-RICE','Rice','rice',1),
             ('GROC-OIL','Cooking Oil','cooking-oil',2),
             ('GROC-SPICE','Spices','spices',3),
             ('GROC-BEVERAGE','Beverages','beverages',4),
             ('GROC-SNACK','Snacks','snacks',5)
     ) v(code,name,slug,sort)
WHERE p.category_code='ROOT-GROC';

COMMIT;



BEGIN;

-- ============================================================================
-- MOBILE PHONES
-- ============================================================================
INSERT INTO ec_category_attributes
(attribute_name,attribute_label,data_type,display_order,
 filterable,searchable,sortable,is_required,active,category_id,created_at)
SELECT
    v.attribute_name,v.attribute_label,v.data_type,v.display_order,
    v.filterable,v.searchable,v.sortable,v.is_required,v.active,
    c.category_id,NOW()
FROM
    (
        VALUES
            ('brand','Brand','LIST',1,true,true,true,true,true),
            ('accessory_type','Accessory Type','LIST',2,true,true,false,true,true),
            ('color','Color','COLOR',3,true,false,false,false,true),
            ('compatibility','Compatibility','TEXT',4,true,true,false,false,true),
            ('warranty','Warranty (Months)','NUMBER',5,false,false,true,false,true)
    ) v(attribute_name,attribute_label,data_type,display_order,
        filterable,searchable,sortable,is_required,active)
        CROSS JOIN
    (
        SELECT id category_id
        FROM ec_categories
        WHERE category_code='ELEC-ACCESSORY'
    )c;


INSERT INTO ec_category_attributes
(attribute_name,attribute_label,data_type,display_order,
 filterable,searchable,sortable,is_required,active,category_id,created_at)
SELECT
    v.*,c.category_id,NOW()
FROM
    (
        VALUES
            ('brand','Brand','LIST',1,true,true,true,false,true),
            ('material','Material','LIST',2,true,false,false,false,true),
            ('capacity','Capacity','NUMBER',3,true,false,true,false,true),
            ('color','Color','COLOR',4,true,false,false,false,true),
            ('dishwasher_safe','Dishwasher Safe','BOOLEAN',5,true,false,false,false,true),
            ('microwave_safe','Microwave Safe','BOOLEAN',6,true,false,false,false,true)
    )v(attribute_name,attribute_label,data_type,display_order,
       filterable,searchable,sortable,is_required,active)
        CROSS JOIN
    (
        SELECT id category_id
        FROM ec_categories
        WHERE category_code='HOME-KITCH'
    )c;


INSERT INTO ec_category_attributes
(attribute_name,attribute_label,data_type,display_order,
 filterable,searchable,sortable,is_required,active,category_id,created_at)
SELECT
    v.*,c.category_id,NOW()
FROM
    (
        VALUES
            ('brand','Brand','LIST',1,true,true,true,false,true),
            ('material','Material','LIST',2,true,false,false,false,true),
            ('color','Color','COLOR',3,true,false,false,false,true),
            ('width','Width (cm)','NUMBER',4,false,false,true,false,true),
            ('height','Height (cm)','NUMBER',5,false,false,true,false,true),
            ('depth','Depth (cm)','NUMBER',6,false,false,true,false,true),
            ('assembly_required','Assembly Required','BOOLEAN',7,true,false,false,false,true)
    )v(attribute_name,attribute_label,data_type,display_order,
       filterable,searchable,sortable,is_required,active)
        CROSS JOIN
    (
        SELECT id category_id
        FROM ec_categories
        WHERE category_code='HOME-FURN'
    )c;


INSERT INTO ec_category_attributes
(attribute_name,attribute_label,data_type,display_order,
 filterable,searchable,sortable,is_required,active,category_id,created_at)
SELECT
    v.*,c.category_id,NOW()
FROM
    (
        VALUES
            ('brand','Brand','LIST',1,true,true,true,true,true),
            ('skin_type','Skin Type','LIST',2,true,false,false,false,true),
            ('volume','Volume (ml)','NUMBER',3,true,false,true,true,true),
            ('organic','Organic','BOOLEAN',4,true,false,false,false,true),
            ('expiry_date','Expiry Date','DATE',5,false,false,false,false,true)
    )v(attribute_name,attribute_label,data_type,display_order,
       filterable,searchable,sortable,is_required,active)
        CROSS JOIN
    (
        SELECT id category_id
        FROM ec_categories
        WHERE category_code='BEAUTY-SKIN'
    )c;


INSERT INTO ec_category_attributes
(attribute_name,attribute_label,data_type,display_order,
 filterable,searchable,sortable,is_required,active,category_id,created_at)
SELECT
    v.*,c.category_id,NOW()
FROM
    (
        VALUES
            ('brand','Brand','LIST',1,true,true,true,false,true),
            ('weight','Weight (gm)','NUMBER',2,true,false,true,true,true),
            ('country_of_origin','Country of Origin','TEXT',3,true,true,false,false,true),
            ('organic','Organic','BOOLEAN',4,true,false,false,false,true),
            ('expiry_date','Expiry Date','DATE',5,false,false,false,true,true)
    )v(attribute_name,attribute_label,data_type,display_order,
       filterable,searchable,sortable,is_required,active)
        CROSS JOIN
    (
        SELECT id category_id
        FROM ec_categories
        WHERE category_code='GROC-RICE'
    )c;


INSERT INTO ec_category_attributes
(attribute_name,attribute_label,data_type,display_order,
 filterable,searchable,sortable,is_required,active,category_id,created_at)
SELECT
    v.*,c.category_id,NOW()
FROM
    (
        VALUES
            ('author','Author','TEXT',1,true,true,true,true,true),
            ('publisher','Publisher','TEXT',2,true,true,false,false,true),
            ('language','Language','LIST',3,true,false,false,false,true),
            ('edition','Edition','TEXT',4,true,true,false,false,true),
            ('isbn','ISBN','TEXT',5,false,true,true,false,true),
            ('pages','Pages','NUMBER',6,false,false,true,false,true)
    )v(attribute_name,attribute_label,data_type,display_order,
       filterable,searchable,sortable,is_required,active)
        CROSS JOIN
    (
        SELECT id category_id
        FROM ec_categories
        WHERE category_code='BOOK-NOVEL'
    )c;

INSERT INTO ec_category_attributes
(attribute_name,attribute_label,data_type,display_order,
 filterable,searchable,sortable,is_required,active,category_id,created_at)
SELECT
    v.*,c.category_id,NOW()
FROM
    (
        VALUES
            ('brand','Brand','LIST',1,true,true,true,false,true),
            ('sport_type','Sport Type','LIST',2,true,true,false,true,true),
            ('size','Size','LIST',3,true,false,false,false,true),
            ('color','Color','COLOR',4,true,false,false,false,true),
            ('material','Material','LIST',5,true,false,false,false,true)
    )v(attribute_name,attribute_label,data_type,display_order,
       filterable,searchable,sortable,is_required,active)
        CROSS JOIN
    (
        SELECT id category_id
        FROM ec_categories
        WHERE category_code='SPORT-CRICKET'
    )c;


BEGIN;


BEGIN;

INSERT INTO ec_product_catalog
(
    product_title,
    slug,
    short_description,
    description,
    seo_title,
    seo_keywords,
    seo_description,
    warranty_information,
    shipping_information,
    return_policy,
    youtube_video,
    minimum_order_qty,
    maximum_order_qty,
    active,
    deleted,
    featured,
    best_seller,
    trending,
    recommended,
    new_arrival,
    published,
    publish_date,
    organization_id,
    category_id,
    item_id,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    i.item_name,
    lower(replace(i.item_name,' ','-')),
    i.description,
    i.description,
    i.item_name,
    'electronics,mobile,smartphone',
    i.description,
    concat(coalesce(i.warranty_months,12),' Months Official Warranty'),
    'Delivery within 2-5 business days.',
    '7 days replacement against manufacturing defects.',
    NULL,
    1,
    10,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    NOW(),
    1,
    c.id,
    i.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_items i
         JOIN ec_categories c
              ON c.category_code='ELEC-MOBILE'
WHERE i.item_code='ITM-A35-001'

UNION ALL

SELECT
    i.item_name,
    lower(replace(i.item_name,' ','-')),
    i.description,
    i.description,
    i.item_name,
    'electronics,mobile,smartphone',
    i.description,
    concat(coalesce(i.warranty_months,12),' Months Official Warranty'),
    'Delivery within 2-5 business days.',
    '7 days replacement against manufacturing defects.',
    NULL,
    1,
    10,
    TRUE,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    NOW(),
    1,
    c.id,
    i.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_items i
         JOIN ec_categories c
              ON c.category_code='ELEC-MOBILE'
WHERE i.item_code='ITM-M35-001'

UNION ALL

SELECT
    i.item_name,
    lower(replace(i.item_name,' ','-')),
    i.description,
    i.description,
    i.item_name,
    'electronics,air-conditioner,lg',
    i.description,
    concat(coalesce(i.warranty_months,24),' Months Official Warranty'),
    'Free home delivery.',
    'Replacement according to manufacturer policy.',
    NULL,
    1,
    5,
    TRUE,
    FALSE,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    1,
    c.id,
    i.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_items i
         JOIN ec_categories c
              ON c.category_code='ELEC-ACCESSORY'
WHERE i.item_code='ITM-LG-AC18'

UNION ALL

SELECT
    i.item_name,
    lower(replace(i.item_name,' ','-')),
    i.description,
    i.description,
    i.item_name,
    'electronics,refrigerator,lg',
    i.description,
    concat(coalesce(i.warranty_months,24),' Months Official Warranty'),
    'Free delivery.',
    'Manufacturer warranty applies.',
    NULL,
    1,
    5,
    TRUE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    TRUE,
    FALSE,
    TRUE,
    NOW(),
    1,
    c.id,
    i.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_items i
         JOIN ec_categories c
              ON c.category_code='ELEC-ACCESSORY'
WHERE i.item_code='ITM-LG-REF300'

ON CONFLICT ON CONSTRAINT uq_ec_prod_item
    DO NOTHING;

COMMIT;


INSERT INTO ec_product_catalog
(
    product_title,slug,short_description,description,
    seo_title,seo_keywords,seo_description,
    warranty_information,shipping_information,return_policy,
    minimum_order_qty,maximum_order_qty,
    active,deleted,featured,best_seller,trending,
    recommended,new_arrival,published,publish_date,
    organization_id,category_id,item_id,
    created_at,updated_at,created_by,updated_by
)
SELECT
    i.item_name,
    'samsung-galaxy-s24-ultra',
    'Premium Samsung flagship smartphone',
    i.description,
    'Samsung Galaxy S24 Ultra',
    'samsung,s24 ultra,android smartphone',
    'Samsung Galaxy S24 Ultra with advanced camera and AI features.',
    '12 Months Official Warranty',
    'Delivery within 2-5 business days',
    '7 Days Replacement Warranty',
    1,5,
    true,false,true,true,true,
    true,true,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
         JOIN ec_categories c ON c.category_code='ELEC-MOBILE'
WHERE i.item_code='ITM-S24U-001';


INSERT INTO ec_product_catalog
(
    product_title,slug,short_description,description,
    seo_title,seo_keywords,seo_description,
    warranty_information,shipping_information,return_policy,
    minimum_order_qty,maximum_order_qty,
    active,deleted,featured,best_seller,trending,
    recommended,new_arrival,published,publish_date,
    organization_id,category_id,item_id,
    created_at,updated_at,created_by,updated_by
)
SELECT
    i.item_name,
    'lg-front-load-washing-machine-8kg',
    'Automatic Front Load Washing Machine',
    i.description,
    i.item_name,
    'lg,washing machine,front load',
    i.description,
    '24 Months Warranty',
    'Free Home Delivery',
    'Manufacturer Warranty Policy',
    1,3,
    true,false,true,false,false,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
         JOIN ec_categories c ON c.category_code='ELEC-ACCESSORY'
WHERE i.item_code='ITM-LG-WM8KG';


INSERT INTO ec_product_catalog
(
    product_title,slug,short_description,description,
    seo_title,seo_keywords,seo_description,
    warranty_information,shipping_information,return_policy,
    minimum_order_qty,maximum_order_qty,
    active,deleted,featured,best_seller,trending,
    recommended,new_arrival,published,publish_date,
    organization_id,category_id,item_id,
    created_at,updated_at,created_by,updated_by
)
SELECT
    i.item_name,
    'skf-bearing-6203-2rs',
    'High quality SKF bearing',
    i.description,
    i.item_name,
    'skf,bearing,6203,industrial spare',
    i.description,
    'Manufacturer Warranty',
    'Nationwide Delivery',
    'Replacement for manufacturing defects',
    1,100,
    true,false,false,true,false,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
         JOIN ec_categories c ON c.category_code='SPARE-BEARING'
WHERE i.item_code='ITM-SKF-6203';


INSERT INTO ec_product_catalog
(
    product_title,slug,short_description,description,
    seo_title,seo_keywords,seo_description,
    warranty_information,shipping_information,return_policy,
    minimum_order_qty,maximum_order_qty,
    active,deleted,featured,best_seller,trending,
    recommended,new_arrival,published,publish_date,
    organization_id,category_id,item_id,
    created_at,updated_at,created_by,updated_by
)
SELECT
    i.item_name,
    'skf-bearing-6204-2rs',
    'SKF Deep Groove Ball Bearing',
    i.description,
    i.item_name,
    'skf,bearing,6204',
    i.description,
    'Manufacturer Warranty',
    'Nationwide Delivery',
    'Replacement Policy Applicable',
    1,100,
    true,false,false,true,false,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
         JOIN ec_categories c ON c.category_code='SPARE-BEARING'
WHERE i.item_code='ITM-SKF-6204';


INSERT INTO ec_product_catalog
(
    product_title,slug,short_description,description,
    seo_title,seo_keywords,seo_description,
    warranty_information,shipping_information,return_policy,
    minimum_order_qty,maximum_order_qty,
    active,deleted,featured,best_seller,trending,
    recommended,new_arrival,published,publish_date,
    organization_id,category_id,item_id,
    created_at,updated_at,created_by,updated_by
)
SELECT
    i.item_name,
    'skf-bearing-6206-2rs',
    'Industrial Deep Groove Ball Bearing',
    i.description,
    i.item_name,
    'skf,bearing,6206',
    i.description,
    'Manufacturer Warranty',
    'Nationwide Delivery',
    'Replacement Policy Applicable',
    1,100,
    true,false,false,true,false,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
         JOIN ec_categories c ON c.category_code='SPARE-BEARING'
WHERE i.item_code='ITM-SKF-6206';

-- ============================================================================
-- ROOT MENUS
-- ============================================================================

INSERT INTO ec_menus
(
    menu_name,
    menu_url,
    menu_icon,
    target,
    display_order,
    active,
    organization_id,
    created_at,
    updated_at,
    created_by,
    updated_by
)
VALUES
    ('Home','/','fa-solid fa-house','_self',1,true,1,NOW(),NOW(),'system','system'),
    ('Shop','/shop','fa-solid fa-store','_self',2,true,1,NOW(),NOW(),'system','system'),
    ('Brands','/brands','fa-solid fa-tags','_self',3,true,1,NOW(),NOW(),'system','system'),
    ('Deals','/deals','fa-solid fa-percent','_self',4,true,1,NOW(),NOW(),'system','system'),
    ('New Arrivals','/new-arrivals','fa-solid fa-star','_self',5,true,1,NOW(),NOW(),'system','system'),
    ('Blog','/blog','fa-solid fa-newspaper','_self',6,true,1,NOW(),NOW(),'system','system'),
    ('About Us','/about','fa-solid fa-circle-info','_self',7,true,1,NOW(),NOW(),'system','system'),
    ('Contact','/contact','fa-solid fa-envelope','_self',8,true,1,NOW(),NOW(),'system','system'),
    ('My Account','/account','fa-solid fa-user','_self',9,true,1,NOW(),NOW(),'system','system');

-- ============================================================================
-- SHOP CHILD MENUS
-- ============================================================================

INSERT INTO ec_menus
(
    menu_name,
    menu_url,
    menu_icon,
    target,
    display_order,
    active,
    organization_id,
    parent_menu_id,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    v.menu_name,
    v.menu_url,
    v.menu_icon,
    '_self',
    v.display_order,
    true,
    1,
    p.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM ec_menus p
         CROSS JOIN
     (
         VALUES
             ('Electronics','/category/electronics','fa-solid fa-mobile-screen',1),
             ('Fashion','/category/fashion','fa-solid fa-shirt',2),
             ('Home & Kitchen','/category/home-kitchen','fa-solid fa-couch',3),
             ('Beauty','/category/beauty','fa-solid fa-spa',4),
             ('Sports','/category/sports','fa-solid fa-football',5),
             ('Books','/category/books','fa-solid fa-book',6),
             ('Groceries','/category/groceries','fa-solid fa-cart-shopping',7)
     ) v(menu_name,menu_url,menu_icon,display_order)
WHERE p.menu_name='Shop';

-- ============================================================================
-- ELECTRONICS
-- ============================================================================

INSERT INTO ec_menus
(
    menu_name,menu_url,menu_icon,target,display_order,active,
    organization_id,parent_menu_id,created_at,updated_at,created_by,updated_by
)
SELECT
    v.menu_name,
    v.menu_url,
    NULL,
    '_self',
    v.display_order,
    true,
    1,
    p.id,
    NOW(),NOW(),'system','system'
FROM ec_menus p
         CROSS JOIN
     (
         VALUES
             ('Mobile Phones','/category/mobile-phones',1),
             ('Laptops','/category/laptops',2),
             ('Tablets','/category/tablets',3),
             ('Television','/category/television',4),
             ('Cameras','/category/cameras',5),
             ('Accessories','/category/accessories',6)
     ) v(menu_name,menu_url,display_order)
WHERE p.menu_name='Electronics';

-- ============================================================================
-- FASHION
-- ============================================================================

INSERT INTO ec_menus
(
    menu_name,menu_url,target,display_order,active,
    organization_id,parent_menu_id,created_at,updated_at,created_by,updated_by
)
SELECT
    v.menu_name,
    v.menu_url,
    '_self',
    v.display_order,
    true,
    1,
    p.id,
    NOW(),NOW(),'system','system'
FROM ec_menus p
         CROSS JOIN
     (
         VALUES
             ('Men','/category/men',1),
             ('Women','/category/women',2),
             ('Kids','/category/kids',3),
             ('Shoes','/category/shoes',4),
             ('Bags','/category/bags',5)
     ) v(menu_name,menu_url,display_order)
WHERE p.menu_name='Fashion';

-- ============================================================================
-- MY ACCOUNT
-- ============================================================================

INSERT INTO ec_menus
(
    menu_name,menu_url,target,display_order,active,
    organization_id,parent_menu_id,created_at,updated_at,created_by,updated_by
)
SELECT
    v.menu_name,
    v.menu_url,
    '_self',
    v.display_order,
    true,
    1,
    p.id,
    NOW(),NOW(),'system','system'
FROM ec_menus p
         CROSS JOIN
     (
         VALUES
             ('Profile','/account/profile',1),
             ('Orders','/account/orders',2),
             ('Wishlist','/wishlist',3),
             ('Addresses','/account/address',4),
             ('Change Password','/account/password',5),
             ('Logout','/logout',6)
     ) v(menu_name,menu_url,display_order)
WHERE p.menu_name='My Account';

-- ============================================================================
-- QUICK ACTIONS
-- ============================================================================

INSERT INTO ec_menus
(
    menu_name,
    menu_url,
    menu_icon,
    target,
    display_order,
    active,
    organization_id,
    created_at,
    updated_at,
    created_by,
    updated_by
)
VALUES
    ('Wishlist','/wishlist','fa-solid fa-heart','_self',20,true,1,NOW(),NOW(),'system','system'),
    ('Cart','/cart','fa-solid fa-cart-shopping','_self',21,true,1,NOW(),NOW(),'system','system'),
    ('Checkout','/checkout','fa-solid fa-credit-card','_self',22,true,1,NOW(),NOW(),'system','system');

COMMIT;