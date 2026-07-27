package com.asg.spindleserp.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * SubAccountMetaDTO — the form schema for one sub-ledger partition.
 *
 * <p>Served by {@code GET /accounts/sub-accounts/meta?type=CARD}. The page holds no
 * markup for any specific type; it asks for this and builds the modal, the save
 * payload, the view panel and the grid columns from what comes back. That is what
 * makes the CRUD dynamic: a field added to {@code SubAccountField} appears in the
 * UI on the next page load with no template or JavaScript change.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubAccountMetaDTO {

    /** Enum name, or "" for the All tab. */
    private String type;
    private String label;
    private String icon;
    /** Guidance shown under the main-account picker. */
    private String mainAccountHint;
    /** False for the All tab — you must pick a concrete type before saving. */
    private boolean creatable;

    private List<Group> groups;
    private List<Column> columns;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Group {
        private String title;
        private String icon;
        private List<Field> fields;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Field {
        /** JSON property on ChartOfAccountSubDTO — also the DOM id suffix. */
        private String key;
        private String label;
        /** TEXT | TEXT_UPPER | EMAIL | TEXTAREA | NUMBER | AMOUNT | RATE | DATE | SELECT | SWITCH | AJAX_COA | AJAX_BANK | AJAX_SUB */
        private String input;
        /** Bootstrap column span, 1–12. */
        private int width;
        private Integer maxLength;
        /** SELECT: comma-separated values. AJAX_SUB: the sub-type to filter on. */
        private String options;
        private boolean required;
    }

    /** A DataTable column. {@code key} matches a key in the row map from the query. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Column {
        private String key;
        private String label;
        /** start | center | end */
        private String align;
        private String width;
        private boolean html;
    }
}
