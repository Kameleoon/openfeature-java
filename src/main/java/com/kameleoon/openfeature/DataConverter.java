package com.kameleoon.openfeature;

import com.kameleoon.data.Conversion;
import com.kameleoon.data.CustomData;
import com.kameleoon.data.Data;
import com.kameleoon.openfeature.dto.types.ConversionType;
import com.kameleoon.openfeature.dto.types.CustomDataType;
import com.kameleoon.openfeature.dto.types.DataType;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.Value;

import javax.json.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DataConverter is used to convert a data from OpenFeature to Kameleoon and back.
 */
public class DataConverter {

    /**
     * Dictionary which contains conversion methods by keys
     */
    private static final Map<String, Function<Value, Data>> conversionMethods = new HashMap<>();

    private static final Value emptyValue = new Value();

    static {
        conversionMethods.put(DataType.CONVERSION.getValue(), DataConverter::makeConversion);
        conversionMethods.put(DataType.CUSTOM_DATA.getValue(), DataConverter::makeCustomData);
    }

    private DataConverter() {
    }

    /**
     * The method for converting EvaluationContext data to Kameleoon SDK data types.
     */
    public static List<Data> toKameleoon(EvaluationContext context) {
        Map<String, Value> contextMap = context != null ? context.asMap() : null;
        if (contextMap == null || contextMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Data> data = new ArrayList<>(contextMap.size());
        for (Map.Entry<String, Value> entry : contextMap.entrySet()) {
            Value value = entry.getValue();
            List<Value> values = value.isStructure() ? Collections.singletonList(value) : value.asList();
            Function<Value, Data> conversionMethod = conversionMethods.get(entry.getKey());
            if (conversionMethod != null) {
                for (Value val : values) {
                    data.add(conversionMethod.apply(val));
                }
            }
        }
        return data;
    }

    /**
     * The method for converting Kameleoon objects to OpenFeature Value instances.
     */
    public static Value toOpenFeature(Object context) {
        Value value = emptyValue;
        if (context instanceof Value) {
            value = (Value) context;
        } else if (context instanceof Integer) {
            value = new Value((Integer) context);
        } else if (context instanceof Double) {
            value = new Value((Double) context);
        } else if (context instanceof Boolean) {
            value = new Value((Boolean) context);
        } else if (context instanceof String) {
            value = new Value((String) context);
        } else if (context instanceof JsonObject) {
            value = toOpenFeature((JsonObject) context);
        } else if (context instanceof JsonArray) {
            value = toOpenFeature((JsonArray) context);
        } else if (context instanceof JsonValue) {
            value = toOpenFeature((JsonValue) context);
        }
        return value;
    }

    /**
     * Converts a Kameleoon JsonObject to an OpenFeature Value instance.
     *
     * @param jsonObject the JsonObject to be converted
     * @return the converted OpenFeature Value instance
     */
    private static Value toOpenFeature(JsonObject jsonObject) {
        Map<String, Value> map = new HashMap<>();
        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            map.put(entry.getKey(), toOpenFeature(entry.getValue()));
        }
        return Value.objectToValue(map);
    }

    /**
     * Converts a Kameleoon JsonArray to an OpenFeature Value instance.
     *
     * @param jsonArray the JsonArray to be converted
     * @return the converted OpenFeature Value instance
     */
    private static Value toOpenFeature(JsonArray jsonArray) {
        List<Value> list = new ArrayList<>(jsonArray.size());
        for (JsonValue jsonValue : jsonArray) {
            list.add(toOpenFeature(jsonValue));
        }
        return new Value(list);
    }

    /**
     * Converts a Kameleoon JsonValue to an OpenFeature Value instance.
     *
     * @param jsonValue the JsonValue to be converted
     * @return the converted OpenFeature Value instance
     */
    private static Value toOpenFeature(JsonValue jsonValue) {
        Value value = null;
        switch (jsonValue.getValueType()) {
            case NUMBER:
                JsonNumber jsonNumber = (JsonNumber) jsonValue;
                if (jsonNumber.isIntegral()) {
                    value = new Value(jsonNumber.intValue());
                } else {
                    value = new Value(jsonNumber.doubleValue());
                }
                break;
            case STRING:
                JsonString jsonString = (JsonString) jsonValue;
                value = new Value(jsonString.getString());
                break;
            case TRUE:
                value = new Value(true);
                break;
            case FALSE:
                value = new Value(false);
                break;
            case NULL:
                break;
            default:
                throw new IllegalArgumentException("Unsupported JsonValue type: " + jsonValue.getValueType());
        }
        return value;
    }

    /**
     * Make Kameleoon {@link CustomData} from {@link Value}
     */
    private static CustomData makeCustomData(Value value) {
        CustomData customData = null;
        Structure structCustomData = value.asStructure();
        Integer index = structCustomData.getValue(CustomDataType.INDEX.getValue()).asInteger();
        if (index == null) {
            index = 0;
        }
        List<Value> values = Optional.ofNullable(structCustomData.getValue(CustomDataType.VALUES.getValue()))
                .map(val -> val.isList() ? val.asList() : Collections.singletonList(val))
                .orElse(null);
        if (values != null) {
            List<String> customDataValues = values.stream()
                    .map(Value::asString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            customData = new CustomData(index, customDataValues);
        } else {
            customData = new CustomData(index, Collections.emptyList());
        }
        return customData;
    }

    /**
     * Make Kameleoon {@link Conversion} from {@link Value}
     */
    private static Conversion makeConversion(Value value) {
        Structure structConversion = value.asStructure();
        Integer goalId = structConversion.getValue(ConversionType.GOAL_ID.getValue()).asInteger();
        if (goalId == null) {
            goalId = 0;
        }
        Value revenue = structConversion.getValue(ConversionType.REVENUE.getValue());
        return new Conversion(goalId, revenue != null ? revenue.asDouble().floatValue() : null, false);
    }
}
