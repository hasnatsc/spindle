-- ============================================================================
-- Spindle ERP — Fix trv_booking_notes.note_text  oid → text
-- File: V104__fix_travel_booking_notes_lob.sql
--
-- WHY: The TrvBookingNote entity used a bare @Lob on note_text, so Hibernate
-- created a PostgreSQL `oid` (large-object) column instead of `text` — the
-- same bug previously fixed on bgt_budget_notes. An oid column stores a
-- pointer into pg_largeobject: unreadable in plain SQL, breaks dump/restore,
-- and leaks large objects when rows are deleted.
--
-- ENTITY FIX (apply together with this migration):
--     @Lob
--     @Column(name = "note_text", nullable = false, columnDefinition = "text")
--     private String noteText;
--
-- Idempotent: the DO block is a no-op if the column is already `text`.
-- Strategy: add a text column, copy content via UPDATE (subqueries are legal
-- there, unlike in ALTER ... USING), unlink the orphaned large objects, then
-- swap the columns.
-- ============================================================================

DO $$
DECLARE
    r record;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'trv_booking_notes'
          AND column_name = 'note_text'
          AND data_type = 'oid'
    ) THEN
        RAISE NOTICE 'trv_booking_notes.note_text is oid — converting to text';

        ALTER TABLE trv_booking_notes ADD COLUMN note_text_txt text;

        -- Copy LO content; dangling oids (large object already gone) become ''
        UPDATE trv_booking_notes n
        SET note_text_txt = CASE
            WHEN n.note_text IS NULL THEN NULL
            WHEN EXISTS (SELECT 1 FROM pg_largeobject_metadata m WHERE m.oid = n.note_text)
                THEN convert_from(lo_get(n.note_text), 'UTF8')
            ELSE ''
        END;

        -- Unlink the now-orphaned large objects
        FOR r IN
            SELECT DISTINCT n.note_text AS lo
            FROM trv_booking_notes n
            WHERE n.note_text IS NOT NULL
              AND EXISTS (SELECT 1 FROM pg_largeobject_metadata m WHERE m.oid = n.note_text)
        LOOP
            PERFORM lo_unlink(r.lo);
        END LOOP;

        -- Swap columns, preserving the NOT NULL contract
        ALTER TABLE trv_booking_notes DROP COLUMN note_text;
        ALTER TABLE trv_booking_notes RENAME COLUMN note_text_txt TO note_text;
        UPDATE trv_booking_notes SET note_text = '' WHERE note_text IS NULL;
        ALTER TABLE trv_booking_notes ALTER COLUMN note_text SET NOT NULL;
    END IF;
END $$;
