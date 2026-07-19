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

-- ============================================================================
-- APPENDED: Sub-categories, Products, Images, Variants, and Reviews
-- ============================================================================

BEGIN;

-- ============================================================================
-- 1. NEW SUB-CATEGORIES (level 3)
-- ============================================================================

-- ELEC-LAPTOP-DELL: Dell Laptops (under ELEC-LAPTOP)
INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ELEC-LAPTOP-DELL','Dell Laptops','dell-laptops',3,p.id,
    1,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
WHERE p.category_code='ELEC-LAPTOP'
ON CONFLICT ON CONSTRAINT uq_ec_cat_code DO NOTHING;

-- ELEC-LAPTOP-HP: HP Laptops (under ELEC-LAPTOP)
INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ELEC-LAPTOP-HP','HP Laptops','hp-laptops',3,p.id,
    2,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
WHERE p.category_code='ELEC-LAPTOP'
ON CONFLICT ON CONSTRAINT uq_ec_cat_code DO NOTHING;

-- FASH-MEN-CASUAL: Casual Wear (under FASH-MEN)
INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'FASH-MEN-CASUAL','Casual Wear','casual-wear',3,p.id,
    1,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
WHERE p.category_code='FASH-MEN'
ON CONFLICT ON CONSTRAINT uq_ec_cat_code DO NOTHING;

-- HOME-KITCHEN-APPL: Kitchen Appliances (under HOME-KITCH, level 2 under ROOT-HOME)
INSERT INTO ec_categories
(category_code,category_name,slug,level_no,parent_category_id,
 display_order,active,deleted,is_featured,is_menu,organization_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'HOME-KITCHEN-APPL','Kitchen Appliances','kitchen-appliances',3,p.id,
    1,true,false,false,true,1,
    NOW(),NOW(),'system','system'
FROM ec_categories p
WHERE p.category_code='HOME-KITCH'
ON CONFLICT ON CONSTRAINT uq_ec_cat_code DO NOTHING;

-- ============================================================================
-- 2. NEW INVENTORY BRANDS (inv_item_brands)
-- ============================================================================

INSERT INTO inv_item_brands
(brand_code, brand_name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('BRD-DELL',      'Dell',          'Dell Technologies laptops and computers',    TRUE, 1, NOW(), NOW(), 'system', 'system'),
    ('BRD-HP',        'HP',            'HP laptops and computers',                  TRUE, 1, NOW(), NOW(), 'system', 'system'),
    ('BRD-SONY',      'Sony',          'Sony electronics and televisions',           TRUE, 1, NOW(), NOW(), 'system', 'system'),
    ('BRD-PANASONIC', 'Panasonic',     'Panasonic home appliances and electronics',  TRUE, 1, NOW(), NOW(), 'system', 'system'),
    ('BRD-NIVEA',     'Nivea',         'Nivea skin care products',                   TRUE, 1, NOW(), NOW(), 'system', 'system')
ON CONFLICT ON CONSTRAINT uq_brand_org_code DO NOTHING;

-- ============================================================================
-- 3. NEW ITEM MODELS (inv_item_models)
-- ============================================================================

-- Dell models
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
    CROSS JOIN (
        VALUES
            ('INSPIRON-15','Inspiron 15','Dell Inspiron 15 Laptop'),
            ('INSPIRON-15-MAX','Inspiron 15 Max','Dell Inspiron 15 High-End Variant')
    ) v(model_code,model_name,description)
WHERE b.brand_code='BRD-DELL'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- HP models
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
    CROSS JOIN (
        VALUES
            ('PAVILION-14','Pavilion 14','HP Pavilion 14 Laptop')
    ) v(model_code,model_name,description)
WHERE b.brand_code='BRD-HP'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- Samsung Tab model (under existing brand BRD-SAMSUNG)
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
    CROSS JOIN (
        VALUES
            ('SM-X200','Galaxy Tab A8','Samsung Galaxy Tab A8 Tablet')
    ) v(model_code,model_name,description)
WHERE b.brand_code='BRD-SAMSUNG'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- Sony models
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
    CROSS JOIN (
        VALUES
            ('KD-43X75K','Bravia 43 4K','Sony Bravia 43 Inch 4K TV')
    ) v(model_code,model_name,description)
WHERE b.brand_code='BRD-SONY'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- Generic shirt models (under existing brand BRD-GEN)
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
    CROSS JOIN (
        VALUES
            ('CTN-SHIRT-S','Cotton Shirt S','Men Regular Fit Cotton Shirt Size S'),
            ('CTN-SHIRT-M','Cotton Shirt M','Men Regular Fit Cotton Shirt Size M'),
            ('CTN-SHIRT-L','Cotton Shirt L','Men Regular Fit Cotton Shirt Size L')
    ) v(model_code,model_name,description)
WHERE b.brand_code='BRD-GEN'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- Panasonic models
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
    CROSS JOIN (
        VALUES
            ('MX-AC400','Mixer Grinder 400W','Panasonic 400W Mixer Grinder with 3 Jars'),
            ('LED-DESK-01','LED Desk Lamp 12W','Panasonic 12W LED Desk Lamp with USB Port')
    ) v(model_code,model_name,description)
WHERE b.brand_code='BRD-PANASONIC'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- Nivea models
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
    CROSS JOIN (
        VALUES
            ('NIVEA-CREAM-50','Facial Cream 50ml','Nivea Soft Moisturizing Facial Cream 50ml')
    ) v(model_code,model_name,description)
WHERE b.brand_code='BRD-NIVEA'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- ============================================================================
-- 4. NEW INVENTORY ITEMS (inv_items)
-- ============================================================================

-- Dell Inspiron 15 (standard config -- used as base product item)
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-DELL-INSP15',
    'Dell Inspiron 15 Laptop',
    'Dell Inspiron 15 Intel Core i5 8GB RAM 256GB SSD',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    65000,65000,75000,
    5,50,10,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-DELL'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='INSPIRON-15'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-LAP'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Dell Inspiron 15 Max (premium config -- used as variant)
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-DELL-INSP15-MAX',
    'Dell Inspiron 15 (16GB/512GB)',
    'Dell Inspiron 15 Intel Core i7 16GB RAM 512GB SSD',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    78000,78000,89000,
    3,30,8,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-DELL'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='INSPIRON-15-MAX'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-LAP'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- HP Pavilion 14
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-HP-PAV14',
    'HP Pavilion 14 Laptop',
    'HP Pavilion 14 Intel Core i5 8GB RAM 512GB SSD',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    62000,62000,72000,
    5,50,10,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-HP'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='PAVILION-14'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-LAP'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Samsung Galaxy Tab A8
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-SAM-TABA8',
    'Samsung Galaxy Tab A8',
    'Samsung Galaxy Tab A8 10.5 Inch 64GB WiFi Tablet',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    25000,25000,30000,
    10,100,15,
    12,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-SAMSUNG'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='SM-X200'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-TAB'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Sony Bravia 43 Inch TV
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-SONY-BRA43',
    'Sony Bravia 43 Inch 4K TV',
    'Sony Bravia 43 Inch 4K Ultra HD Smart LED TV',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    55000,55000,65000,
    3,30,8,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-SONY'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='KD-43X75K'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-TV'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Men's Cotton Shirt (Size M -- default/parent item)
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-MEN-COTTON-M',
    'Men Cotton Shirt (M)',
    'Regular Fit Cotton Shirt for Men - Medium',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    800,800,1500,
    50,500,100,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-GEN'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='CTN-SHIRT-M'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-FASH-MENS'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Men's Cotton Shirt (Size S - variant)
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-MEN-COTTON-S',
    'Men Cotton Shirt (S)',
    'Regular Fit Cotton Shirt for Men - Small',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    800,800,1500,
    30,300,50,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-GEN'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='CTN-SHIRT-S'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-FASH-MENS'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Men's Cotton Shirt (Size L - variant)
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-MEN-COTTON-L',
    'Men Cotton Shirt (L)',
    'Regular Fit Cotton Shirt for Men - Large',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    800,800,1500,
    50,500,100,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-GEN'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='CTN-SHIRT-L'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-FASH-MENS'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Blender Machine
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-BLENDER',
    'Panasonic Mixer Grinder 400W',
    'Panasonic MX-AC400 Mixer Grinder with 3 Stainless Steel Jars',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    2500,2500,4000,
    10,200,20,
    12,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-PANASONIC'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='MX-AC400'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-HOME-KITCH'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- LED Desk Lamp
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-LED-LAMP',
    'Panasonic LED Desk Lamp 12W',
    'Panasonic LED Desk Lamp 12W with USB Charging Port and 3 Brightness Levels',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    800,800,1500,
    20,300,30,
    12,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-PANASONIC'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='LED-DESK-01'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-ACC'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- Facial Cream 50ml
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-FACIAL-CREAM',
    'Nivea Facial Cream 50ml',
    'Nivea Soft Moisturizing Facial Cream 50ml Tube with Vitamin E',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    350,350,650,
    30,500,50,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
    JOIN inv_item_brands b ON b.brand_code='BRD-NIVEA'
    JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='NIVEA-CREAM-50'
    JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-COS-CREAM'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- ============================================================================
-- 5. PRODUCT CATALOG (ec_product_catalog)
-- ============================================================================

-- 5a. Dell Inspiron 15 Laptop (under ELEC-LAPTOP-DELL)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'Dell Inspiron 15 Laptop',
    'dell-inspiron-15-laptop',
    'Powerful Dell Inspiron 15 with Intel Core i5 processor for work and entertainment.',
    'The Dell Inspiron 15 laptop features a 15.6-inch Full HD display, Intel Core i5 processor, 8GB DDR4 RAM, and 256GB SSD. Perfect for work, study, and entertainment with long battery life and premium build quality.',
    'Dell Inspiron 15 Laptop - Intel Core i5 8GB 256GB',
    'dell,inspiron 15,laptop,intel core i5,notebook',
    'Buy Dell Inspiron 15 laptop in Bangladesh at best price with official Dell warranty.',
    '24 Months Official Dell Warranty',
    'Free delivery within 2-5 business days across Bangladesh.',
    '7 days replacement against manufacturing defects.',
    1,5,
    true,false,true,true,true,
    true,true,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='ELEC-LAPTOP-DELL'
WHERE i.item_code='ITM-DELL-INSP15'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- 5b. HP Pavilion 14 Laptop (under ELEC-LAPTOP-HP)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'HP Pavilion 14 Laptop',
    'hp-pavilion-14-laptop',
    'Slim and stylish HP Pavilion 14 for everyday computing with powerful performance.',
    'The HP Pavilion 14 laptop features a 14-inch Full HD IPS display, Intel Core i5 processor, 8GB RAM, and 512GB SSD. Lightweight design at just 1.4kg, perfect for students and professionals on the go.',
    'HP Pavilion 14 Laptop - Intel Core i5 8GB 512GB',
    'hp,pavilion 14,laptop,intel core i5,ultrabook',
    'Buy HP Pavilion 14 laptop in Bangladesh with official warranty at best price.',
    '24 Months Official HP Warranty',
    'Free delivery within 2-5 business days across Bangladesh.',
    '7 days replacement against manufacturing defects.',
    1,5,
    true,false,true,true,true,
    true,true,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='ELEC-LAPTOP-HP'
WHERE i.item_code='ITM-HP-PAV14'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- 5c. Samsung Galaxy Tab A8 (under ELEC-TABLET)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'Samsung Galaxy Tab A8',
    'samsung-galaxy-tab-a8',
    'Samsung Galaxy Tab A8 with 10.5-inch display for entertainment and productivity.',
    'The Samsung Galaxy Tab A8 features a 10.5-inch TFT display, 64GB storage (expandable up to 1TB), 4GB RAM, and a 7040mAh long-lasting battery. Perfect for streaming, browsing, and light productivity tasks.',
    'Samsung Galaxy Tab A8 - 64GB WiFi 10.5 Inch',
    'samsung,galaxy tab a8,tablet,android tablet,10.5 inch',
    'Buy Samsung Galaxy Tab A8 in Bangladesh at the best price with official warranty.',
    '12 Months Official Samsung Warranty',
    'Free delivery within 2-5 business days across Bangladesh.',
    '7 days replacement against manufacturing defects.',
    1,5,
    true,false,true,false,true,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='ELEC-TABLET'
WHERE i.item_code='ITM-SAM-TABA8'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- 5d. Sony Bravia 43 Inch TV (under ELEC-TV)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'Sony Bravia 43 Inch 4K Ultra HD TV',
    'sony-bravia-43-inch-4k-tv',
    'Sony Bravia 43 Inch 4K Ultra HD Smart LED TV with Google TV.',
    'Experience stunning picture quality with the Sony Bravia 43-inch 4K Ultra HD Smart LED TV. Features Google TV, X1 4K HDR Processor, Dolby Vision, Dolby Atmos, and X-Balanced Speaker for an immersive cinematic experience.',
    'Sony Bravia 43 Inch 4K TV - Smart Google TV',
    'sony,bravia,43 inch,4k tv,smart tv,google tv,led tv',
    'Buy Sony Bravia 43-inch 4K TV in Bangladesh with official Sony warranty at best price.',
    '24 Months Official Sony Warranty',
    'Free home delivery within 3-7 business days.',
    '7 days replacement against manufacturing defects.',
    1,3,
    true,false,true,true,false,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='ELEC-TV'
WHERE i.item_code='ITM-SONY-BRA43'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- 5e. Men's Cotton Shirt (under FASH-MEN-CASUAL)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'Men Cotton Regular Fit Shirt',
    'men-cotton-regular-fit-shirt',
    'Comfortable 100% cotton regular fit shirt for everyday casual wear.',
    'Premium quality 100% cotton regular fit shirt for men. Soft breathable fabric, classic design with full sleeves and button-down collar. Suitable for both casual and semi-formal occasions. Available in multiple sizes.',
    'Men Cotton Regular Fit Shirt - Casual Wear',
    'men,cotton shirt,regular fit,casual shirt,cotton wear',
    'Buy men cotton regular fit shirt online in Bangladesh at best price.',
    'No warranty applicable.',
    'Delivery within 3-5 business days across Bangladesh.',
    '7 days exchange for size issues. Item must be unworn with tags.',
    1,10,
    true,false,false,true,true,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='FASH-MEN-CASUAL'
WHERE i.item_code='ITM-MEN-COTTON-M'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- 5f. Blender Machine (under HOME-KITCHEN-APPL)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'Panasonic Mixer Grinder 400W',
    'panasonic-mixer-grinder-400w',
    'Powerful 400W mixer grinder with 3 stainless steel jars for your kitchen.',
    'Panasonic MX-AC400 400W mixer grinder with 3 stainless steel jars (chutney jar, medium jar, large jar). Ideal for grinding spices, making smoothies, chutneys, and food preparation. Compact design with safety lock and overload protection.',
    'Panasonic Mixer Grinder 400W - 3 Stainless Steel Jars',
    'panasonic,mixer grinder,blender,kitchen appliance,juicer',
    'Buy Panasonic mixer grinder in Bangladesh at best price with official warranty.',
    '12 Months Official Panasonic Warranty',
    'Free delivery within 3-5 business days across Bangladesh.',
    '7 days replacement against manufacturing defects.',
    1,5,
    true,false,true,true,false,
    true,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='HOME-KITCHEN-APPL'
WHERE i.item_code='ITM-BLENDER'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- 5g. LED Desk Lamp (under ELEC-ACCESSORY)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'Panasonic LED Desk Lamp 12W',
    'panasonic-led-desk-lamp-12w',
    'Energy-efficient 12W LED desk lamp with USB charging port and 3 brightness levels.',
    'Panasonic LED desk lamp with 12W power consumption, 3 brightness levels (warm/natural/cool), adjustable arm, and built-in USB charging port. Eye-care technology with flicker-free lighting for comfortable reading, studying, and working.',
    'Panasonic LED Desk Lamp 12W - USB Charging Port',
    'panasonic,led desk lamp,study lamp,table lamp,eye care lamp',
    'Buy Panasonic LED desk lamp in Bangladesh at best price with official warranty.',
    '12 Months Official Panasonic Warranty',
    'Free delivery within 3-5 business days across Bangladesh.',
    '7 days replacement against manufacturing defects.',
    1,10,
    true,false,false,false,true,
    false,false,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='ELEC-ACCESSORY'
WHERE i.item_code='ITM-LED-LAMP'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- 5h. Facial Cream 50ml (under ROOT-BEAUTY)
INSERT INTO ec_product_catalog
(product_title,slug,short_description,description,
 seo_title,seo_keywords,seo_description,
 warranty_information,shipping_information,return_policy,
 minimum_order_qty,maximum_order_qty,
 active,deleted,featured,best_seller,trending,
 recommended,new_arrival,published,publish_date,
 organization_id,category_id,item_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'Nivea Soft Facial Cream 50ml',
    'nivea-soft-facial-cream-50ml',
    'Moisturizing facial cream with Vitamin E for soft and glowing skin.',
    'Nivea Soft facial cream enriched with Vitamin E and Jojoba Oil. Lightweight, non-greasy formula that keeps skin moisturized for 24 hours. Suitable for all skin types. Dermatologically tested.',
    'Nivea Soft Facial Cream 50ml - Vitamin E Moisturizer',
    'nivea,facial cream,moisturizer,skin care,beauty cream',
    'Buy Nivea facial cream 50ml in Bangladesh at best price. Official product guaranteed.',
    'No warranty applicable.',
    'Delivery within 2-4 business days across Bangladesh.',
    'No returns on opened beauty products for hygiene reasons.',
    1,20,
    true,false,false,true,true,
    true,true,true,NOW(),
    1,c.id,i.id,
    NOW(),NOW(),'system','system'
FROM inv_items i
    JOIN ec_categories c ON c.category_code='ROOT-BEAUTY'
WHERE i.item_code='ITM-FACIAL-CREAM'
ON CONFLICT ON CONSTRAINT uq_ec_prod_item DO NOTHING;

-- ============================================================================
-- 6. PRODUCT IMAGES (ec_product_images)
-- ============================================================================

-- Dell Inspiron 15 (2 images)
INSERT INTO ec_product_images
(product_id,image_url,thumbnail_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/dell-inspiron-15-front.jpg','https://images.example.com/dell-inspiron-15-front-thumb.jpg',true,1,'Dell Inspiron 15 Laptop Front View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='dell-inspiron-15-laptop';

INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/dell-inspiron-15-keyboard.jpg',false,2,'Dell Inspiron 15 Laptop Keyboard View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='dell-inspiron-15-laptop';

-- HP Pavilion 14 (2 images)
INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/hp-pavilion-14-front.jpg',true,1,'HP Pavilion 14 Laptop Front View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='hp-pavilion-14-laptop';

INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/hp-pavilion-14-side.jpg',false,2,'HP Pavilion 14 Laptop Side View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='hp-pavilion-14-laptop';

-- Samsung Galaxy Tab A8 (2 images)
INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/galaxy-tab-a8-front.jpg',true,1,'Samsung Galaxy Tab A8 Front View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='samsung-galaxy-tab-a8';

INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/galaxy-tab-a8-back.jpg',false,2,'Samsung Galaxy Tab A8 Back View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='samsung-galaxy-tab-a8';

-- Sony Bravia 43 Inch TV (2 images)
INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/sony-bravia-43-front.jpg',true,1,'Sony Bravia 43 Inch 4K TV Front View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='sony-bravia-43-inch-4k-tv';

INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/sony-bravia-43-remote.jpg',false,2,'Sony Bravia TV with Remote Control',true,NOW()
FROM ec_product_catalog p WHERE p.slug='sony-bravia-43-inch-4k-tv';

-- Men's Cotton Shirt (2 images)
INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/men-cotton-shirt-front.jpg',true,1,'Men Cotton Shirt Front View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='men-cotton-regular-fit-shirt';

INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/men-cotton-shirt-back.jpg',false,2,'Men Cotton Shirt Back View',true,NOW()
FROM ec_product_catalog p WHERE p.slug='men-cotton-regular-fit-shirt';

-- Blender Machine (1 image)
INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/panasonic-mixer-grinder.jpg',true,1,'Panasonic Mixer Grinder 400W with 3 Jars',true,NOW()
FROM ec_product_catalog p WHERE p.slug='panasonic-mixer-grinder-400w';

-- LED Desk Lamp (1 image)
INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/panasonic-led-desk-lamp.jpg',true,1,'Panasonic LED Desk Lamp 12W',true,NOW()
FROM ec_product_catalog p WHERE p.slug='panasonic-led-desk-lamp-12w';

-- Facial Cream 50ml (1 image)
INSERT INTO ec_product_images
(product_id,image_url,is_primary,display_order,alt_text,active,created_at)
SELECT p.id,'https://images.example.com/nivea-facial-cream.jpg',true,1,'Nivea Soft Facial Cream 50ml Tube',true,NOW()
FROM ec_product_catalog p WHERE p.slug='nivea-soft-facial-cream-50ml';

-- ============================================================================
-- 7. PRODUCT VARIANTS (ec_product_variants)
-- ============================================================================

-- Dell Inspiron 15: Color variant (Silver -- uses parent item)
INSERT INTO ec_product_variants
(product_id,item_id,variant_name,variant_code,color,size_name,selling_price,compare_price,active,deleted,created_at,created_by)
SELECT p.id,i.id,'Dell Inspiron 15 - Silver','DELL-INSP15-SLV','Silver','15.6 Inch',75000,82000,true,false,NOW(),'system'
FROM ec_product_catalog p, inv_items i
WHERE p.slug='dell-inspiron-15-laptop' AND i.item_code='ITM-DELL-INSP15'
ON CONFLICT ON CONSTRAINT uq_ec_variant_item DO NOTHING;

-- Dell Inspiron 15: Color variant (Carbon Black -- uses upgraded item)
INSERT INTO ec_product_variants
(product_id,item_id,variant_name,variant_code,color,size_name,selling_price,compare_price,active,deleted,created_at,created_by)
SELECT p.id,i.id,'Dell Inspiron 15 - Carbon Black 16GB','DELL-INSP15-BLK','Carbon Black','15.6 Inch',89000,95000,true,false,NOW(),'system'
FROM ec_product_catalog p, inv_items i
WHERE p.slug='dell-inspiron-15-laptop' AND i.item_code='ITM-DELL-INSP15-MAX'
ON CONFLICT ON CONSTRAINT uq_ec_variant_item DO NOTHING;

-- Men's Cotton Shirt: Size S
INSERT INTO ec_product_variants
(product_id,item_id,variant_name,variant_code,size_name,selling_price,active,deleted,created_at,created_by)
SELECT p.id,i.id,'Cotton Shirt - Size S','COTTON-SHIRT-S','S',1500,true,false,NOW(),'system'
FROM ec_product_catalog p, inv_items i
WHERE p.slug='men-cotton-regular-fit-shirt' AND i.item_code='ITM-MEN-COTTON-S'
ON CONFLICT ON CONSTRAINT uq_ec_variant_item DO NOTHING;

-- Men's Cotton Shirt: Size M (uses parent item)
INSERT INTO ec_product_variants
(product_id,item_id,variant_name,variant_code,size_name,selling_price,active,deleted,created_at,created_by)
SELECT p.id,i.id,'Cotton Shirt - Size M','COTTON-SHIRT-M','M',1500,true,false,NOW(),'system'
FROM ec_product_catalog p, inv_items i
WHERE p.slug='men-cotton-regular-fit-shirt' AND i.item_code='ITM-MEN-COTTON-M'
ON CONFLICT ON CONSTRAINT uq_ec_variant_item DO NOTHING;

-- Men's Cotton Shirt: Size L
INSERT INTO ec_product_variants
(product_id,item_id,variant_name,variant_code,size_name,selling_price,active,deleted,created_at,created_by)
SELECT p.id,i.id,'Cotton Shirt - Size L','COTTON-SHIRT-L','L',1500,true,false,NOW(),'system'
FROM ec_product_catalog p, inv_items i
WHERE p.slug='men-cotton-regular-fit-shirt' AND i.item_code='ITM-MEN-COTTON-L'
ON CONFLICT ON CONSTRAINT uq_ec_variant_item DO NOTHING;

-- ============================================================================
-- 8. REVIEW SUMMARY (ec_review_summary)
-- ============================================================================

-- Dell Inspiron 15: avg 4.1 (ratings: 2/3/15/45/35, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,4.1,2,3,15,45,35,92.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='dell-inspiron-15-laptop'
ON CONFLICT (product_id) DO NOTHING;

-- HP Pavilion 14: avg 3.9 (ratings: 3/5/20/40/32, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,3.9,3,5,20,40,32,88.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='hp-pavilion-14-laptop'
ON CONFLICT (product_id) DO NOTHING;

-- Samsung Galaxy Tab A8: avg 3.7 (ratings: 5/8/25/35/27, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,3.7,5,8,25,35,27,85.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='samsung-galaxy-tab-a8'
ON CONFLICT (product_id) DO NOTHING;

-- Sony Bravia 43" TV: avg 4.3 (ratings: 1/2/10/38/49, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,4.3,1,2,10,38,49,95.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='sony-bravia-43-inch-4k-tv'
ON CONFLICT (product_id) DO NOTHING;

-- Men's Cotton Shirt: avg 3.4 (ratings: 8/12/30/32/18, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,3.4,8,12,30,32,18,78.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='men-cotton-regular-fit-shirt'
ON CONFLICT (product_id) DO NOTHING;

-- Blender Machine: avg 4.0 (ratings: 3/4/18/42/33, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,4.0,3,4,18,42,33,90.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='panasonic-mixer-grinder-400w'
ON CONFLICT (product_id) DO NOTHING;

-- LED Desk Lamp: avg 4.2 (ratings: 1/3/12/40/44, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,4.2,1,3,12,40,44,93.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='panasonic-led-desk-lamp-12w'
ON CONFLICT (product_id) DO NOTHING;

-- Facial Cream 50ml: avg 4.4 (ratings: 1/1/8/35/55, 100 reviews)
INSERT INTO ec_review_summary
(product_id,average_rating,rating1,rating2,rating3,rating4,rating5,recommendation_pct,total_reviews,organization_id,created_at,updated_at)
SELECT p.id,4.4,1,1,8,35,55,96.0,100,1,NOW(),NOW()
FROM ec_product_catalog p WHERE p.slug='nivea-soft-facial-cream-50ml'
ON CONFLICT (product_id) DO NOTHING;

COMMIT;