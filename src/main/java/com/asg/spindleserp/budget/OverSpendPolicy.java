package com.asg.spindleserp.budget;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OverSpendPolicy {
    ALLOW("Allow — track only; no restriction on over-spend"),
    WARN("Warn  — alert raised but transaction proceeds"),
    BLOCK("Block — transaction prevented when budget exhausted");

    private final String description;
}