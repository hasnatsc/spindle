package com.asg.spindleserp.common.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataTableResponse {
    private long draw;
    private long recordsTotal;
    private long recordsFiltered;
    private List<Map<String, Object>> data;
    private String error;

    public static DataTableResponse of(long draw, long total, long filtered,
                                       List<Map<String, Object>> rows) {
        DataTableResponse r = new DataTableResponse();
        r.draw = draw;
        r.recordsTotal = total;
        r.recordsFiltered = filtered;
        r.data = rows;
        return r;
    }

    public static DataTableResponse error(long draw, String msg) {
        DataTableResponse r = new DataTableResponse();
        r.draw = draw;
        r.error = msg;
        return r;
    }
}
