package com.kameleoon.openfeature.dto.types;

/**
 * DataType is used to add different Kameleoon data types using
 * {@link dev.openfeature.sdk.EvaluationContext}.
 */
public enum DataType {
    CONVERSION("conversion"),
    CUSTOM_DATA("customData");

    private final String value;

    DataType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
