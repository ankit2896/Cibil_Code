package com.freecharge.cibil.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum CibilVariableImpact {
    HIGH_IMPACT("High Impact"),
    LOW_IMPACT("Low Impact"),
    MEDIUM_IMPACT("Medium Impact");

    @Getter
    private String name;
}
