package com.kameleoon.openfeature.dto.types;

/**
 * CustomDataType is used to add {@link com.kameleoon.data.CustomData} using
 * {@link dev.openfeature.sdk.EvaluationContext}.
 */
public enum CustomDataType {
    INDEX("index"),
    VALUES("values");

    private final String value;

    CustomDataType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
