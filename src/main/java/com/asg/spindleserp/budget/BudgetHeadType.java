package com.asg.spindleserp.budget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetHeadType {
    REVENUE("Revenue"),
    EXPENSE("Expense"),
    CAPEX("Capital Expenditure"),
    OPEX("Operating Expenditure"),
    PRODUCTION("Production Cost"),
    HR("Human Resources"),
    COMMERCIAL("Commercial / Trade"),
    OTHER("Other");

    private final String displayName;
}
