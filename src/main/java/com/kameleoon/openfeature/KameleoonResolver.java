package com.kameleoon.openfeature;

import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonException;
import com.kameleoon.data.Data;
import dev.openfeature.sdk.*;

import java.util.Map;

/**
 * KameleoonResolver makes evalutions based on provided data, conforms to Resolver interface
 */
class KameleoonResolver implements Resolver {

    private static final String VARIABLE_KEY = "variableKey";
    private final KameleoonClient client;

    KameleoonResolver(KameleoonClient client) {
        this.client = client;
    }

    /**
     * Main method for getting resolution details based on provided data.
     */
    @Override
    public <T> ProviderEvaluation<T> resolve(
            String flagKey, T defaultValue, EvaluationContext context) {
        try {
            // Get visitor code
            String visitorCode = context != null ? context.getTargetingKey() : null;
            if (visitorCode == null || visitorCode.isEmpty()) {
                return makeResolutionDetails(flagKey, defaultValue, null,
                        ErrorCode.TARGETING_KEY_MISSING,
                        "The TargetingKey is required in context and cannot be omitted.");
            }

            // Add targeting data from context to KameleoonClient by visitor code
            client.addData(visitorCode, DataConverter.toKameleoon(context).toArray(new Data[0]));

            // Get a variant
            String variant = client.getFeatureVariationKey(visitorCode, flagKey);

            // Get the all variables for the variant
            Map<String, Object> variables = client.getFeatureVariationVariables(flagKey, variant);

            // Get variableKey if it's provided in context or any first in variation.
            // It's the responsibility of the client to have only one variable per variation if
            // variableKey is not provided.
            String variableKey = getVariableKey(context, variables);

            // Try to get value by variable key
            Object value = variables.get(variableKey);

            if (variableKey == null || value == null) {
                return makeResolutionDetails(flagKey, defaultValue, variant,
                        ErrorCode.FLAG_NOT_FOUND,
                        makeErrorDescription(variant, variableKey));
            }

            // Check if the variable value has a required type
            if (!value.getClass().equals(defaultValue.getClass())) {
                return makeResolutionDetails(flagKey, defaultValue, variant,
                        ErrorCode.TYPE_MISMATCH,
                        "The type of value received is different from the requested value.");
            }

            @SuppressWarnings("unchecked")
            T typedValue = (T) value;
            return makeResolutionDetails(flagKey, typedValue, variant);
        } catch (KameleoonException.FeatureException exception) {
            return makeResolutionDetails(flagKey, defaultValue, null, ErrorCode.FLAG_NOT_FOUND,
                    exception.getMessage());
        } catch (KameleoonException.VisitorCodeInvalid exception) {
            return makeResolutionDetails(flagKey, defaultValue, null, ErrorCode.INVALID_CONTEXT,
                    exception.getMessage());
        } catch (Exception exception) {
            return makeResolutionDetails(flagKey, defaultValue, null, ErrorCode.GENERAL,
                    exception.getMessage());
        }
    }

    /**
     * Helper method to get the variable key from the context or variables map.
     */
    private static String getVariableKey(EvaluationContext context, Map<String, Object> variables) {
        String variableKey = null;
        if (context.getValue("variableKey") != null) {
            variableKey = context.getValue(VARIABLE_KEY).asString();
        }
        if (variableKey == null) {
            variableKey = variables.keySet().stream().findFirst().orElse(null);
        }
        return variableKey;
    }

    /**
     * Helper method to create a ResolutionDetails object.
     */
    private static <T> ProviderEvaluation<T> makeResolutionDetails(String flagKey, T value,
            String variant) {
        return ProviderEvaluation.<T>builder()
                .value(value)
                .variant(variant)
                .reason(Reason.STATIC.toString())
                .build();
    }

    private static <T> ProviderEvaluation<T> makeResolutionDetails(String flagKey, T value,
            String variant,
            ErrorCode errorCode, String errorMessage) {
        return ProviderEvaluation.<T>builder()
                .value(value)
                .variant(variant)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .reason(Reason.STATIC.toString())
                .build();
    }

    private static String makeErrorDescription(String variant, String variableKey) {
        return (variableKey == null || variableKey.isEmpty())
                ? String.format("The variation '%s' has no variables", variant)
                : String.format(
                        "The value for provided variable key '%s' isn't found in variation '%s'",
                        variableKey, variant);
    }
}
