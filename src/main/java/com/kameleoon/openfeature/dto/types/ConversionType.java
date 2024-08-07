package com.kameleoon.openfeature.dto.types;

/**
 * ConversionType is used to add {@link com.kameleoon.data.Conversion} using
 * {@link dev.openfeature.sdk.EvaluationContext}.
 */
public enum ConversionType {
    GOAL_ID("goalId"),
    REVENUE("revenue");

    private final String value;

    ConversionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
