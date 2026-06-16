package com.asg.spindleserp.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DataTableResponse
 *
 * Standard server-side DataTables response envelope.
 * Package: com.asg.spindleserp.common.dto
 *
 * Used by:
 *   UserServiceImpl.datatableList()
 *   UserController.list()
 *
 * Factory method:
 *   DataTableResponse.of(draw, total, filtered, rows)
 *
 * JSON output matches DataTables server-side protocol:
 *   { "draw":1, "recordsTotal":100, "recordsFiltered":10, "data":[…] }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataTableResponse {

    private int draw;

    @JsonProperty("recordsTotal")
    private long recordsTotal;

    @JsonProperty("recordsFiltered")
    private long recordsFiltered;

    private List<Map<String, Object>> data;

    /** Standard factory — used by UserServiceImpl.datatableList() */
    public static DataTableResponse of(
            int draw,
            long total,
            long filtered,
            List<Map<String, Object>> rows) {
        return DataTableResponse.builder()
                .draw(draw)
                .recordsTotal(total)
                .recordsFiltered(filtered)
                .data(rows != null ? rows : Collections.emptyList())
                .build();
    }

    /** Empty response (used on error / no data). */
    public static DataTableResponse empty(int draw) {
        return of(draw, 0L, 0L, Collections.emptyList());
    }

    /** Error response with a message field appended. */
    public static DataTableResponse error(int draw, String message) {
        DataTableResponse r = empty(draw);
        r.setError(message);
        return r;
    }

    /** Optional error field (DataTables reads this on failure). */
    private String error;
}
