package com.kameleoon.openfeature;

import com.kameleoon.openfeature.dto.types.ConversionType;
import com.kameleoon.openfeature.dto.types.CustomDataType;
import com.kameleoon.openfeature.dto.types.DataType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TypesTest {

    @Test
    public void checkTypeValues_ProperValues() {
        // Assert
        assertEquals("conversion", DataType.CONVERSION.getValue());
        assertEquals("customData", DataType.CUSTOM_DATA.getValue());

        assertEquals("index", CustomDataType.INDEX.getValue());
        assertEquals("values", CustomDataType.VALUES.getValue());

        assertEquals("goalId", ConversionType.GOAL_ID.getValue());
        assertEquals("revenue", ConversionType.REVENUE.getValue());
    }
}
