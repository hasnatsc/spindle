package com.asg.spindleserp.budget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetType {
    ANNUAL("BGT", "Annual Budget"),
    QUARTERLY("QBGT", "Quarterly Budget"),
    MONTHLY("MBGT", "Monthly Budget"),
    PROJECT("PBGT", "Project Budget"),
    DEPARTMENTAL("DBGT", "Departmental Budget"),
    CAPEX("CBGT", "Capital Expenditure Budget"),
    ROLLING("RBGT", "Rolling Budget");

    private final String code;
    private final String displayName;
}
