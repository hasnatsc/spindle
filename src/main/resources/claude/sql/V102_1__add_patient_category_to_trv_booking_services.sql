-- Run this manually only if Hibernate ddl-auto=update does not create the column.
ALTER TABLE trv_booking_services
    ADD COLUMN IF NOT EXISTS patient_category VARCHAR(20)
    -- Values: INFANT, CHILD, ADULT — validated at the front-end level.
    ;
