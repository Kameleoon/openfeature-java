package com.kameleoon.openfeature;

import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonException;
import dev.openfeature.sdk.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KameleoonResolverTest {

    @Test
    public void resolve_WithNullContext_ReturnsErrorForMissingTargetingKey() {
        // Arrange
        KameleoonClient clientMock = mock(KameleoonClient.class);
        KameleoonResolver resolver = new KameleoonResolver(clientMock);
        String flagKey = "testFlag";
        String defaultValue = "defaultValue";

        // Act
        ProviderEvaluation<String> result = resolver.resolve(flagKey, defaultValue, null);

        // Assert
        assertEquals(defaultValue, result.getValue());
        assertEquals(ErrorCode.TARGETING_KEY_MISSING, result.getErrorCode());
        assertEquals("The TargetingKey is required in context and cannot be omitted.",
                result.getErrorMessage());
        assertNull(result.getVariant());
    }

    private static Stream<Arguments> resolve_NoMatchVariables_ReturnsErrorForFlagNotFound_DataProvider() {
        return Stream.of(
                Arguments.of("on", false, new HashMap<String, Object>(),
                        "The variation 'on' has no variables"),
                Arguments.of("var", true, new HashMap<String, Object>() {
                    {
                        put("key", new Object());
                    }
                },
                        "The value for provided variable key 'variableKey' isn't found in variation 'var'"));
    }

    @ParameterizedTest
    @MethodSource("resolve_NoMatchVariables_ReturnsErrorForFlagNotFound_DataProvider")
    public void resolve_NoMatchVariable_ReturnsErrorForFlagNotFound(
            String variant, boolean addVariableKey, Map<String, Object> variables,
            String expectedErrorMessage) {
        // Arrange
        KameleoonClient clientMock = mock(KameleoonClient.class);
        try {
            when(clientMock.getFeatureVariationKey(anyString(), anyString())).thenReturn(variant);
            when(clientMock.getFeatureVariationVariables(anyString(), anyString()))
                    .thenReturn(variables);
        } catch (KameleoonException e) {
            throw new RuntimeException(e);
        }
        Resolver resolver = new KameleoonResolver(clientMock);
        String flagKey = "testFlag";
        int defaultValue = 42;

        Map<String, Value> values = new HashMap<>();
        if (addVariableKey) {
            values.put("variableKey", new Value("variableKey"));
        }
        EvaluationContext context = new ImmutableContext("testVisitor", values);

        // Act
        ProviderEvaluation<Integer> result = resolver.resolve(flagKey, defaultValue, context);

        // Assert
        assertEquals(defaultValue, result.getValue());
        assertEquals(ErrorCode.FLAG_NOT_FOUND, result.getErrorCode());
        assertEquals(expectedErrorMessage, result.getErrorMessage());
        assertEquals(variant, result.getVariant());
    }

    private static Stream<Arguments> resolve_MismatchType_ReturnsErrorTypeMismatch_DataProvider() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of("string"),
                Arguments.of(10.0));
    }

    @ParameterizedTest
    @MethodSource("resolve_MismatchType_ReturnsErrorTypeMismatch_DataProvider")
    public void resolve_MismatchType_ReturnsErrorTypeMismatch(Object returnValue) {
        // Arrange
        String variant = "on";
        KameleoonClient clientMock = mock(KameleoonClient.class);
        try {
            when(clientMock.getFeatureVariationKey(anyString(), anyString())).thenReturn(variant);
            when(clientMock.getFeatureVariationVariables(anyString(), anyString()))
                    .thenReturn(Collections.singletonMap("key", returnValue));
        } catch (KameleoonException e) {
            throw new RuntimeException(e);
        }
        KameleoonResolver resolver = new KameleoonResolver(clientMock);
        String flagKey = "testFlag";
        int defaultValue = 42;

        EvaluationContext context = new ImmutableContext("testVisitor");
        // Act
        ProviderEvaluation<Integer> result = resolver.resolve(flagKey, defaultValue, context);

        // Assert
        assertEquals(defaultValue, result.getValue());
        assertEquals(ErrorCode.TYPE_MISMATCH, result.getErrorCode());
        assertEquals("The type of value received is different from the requested value.",
                result.getErrorMessage());
        assertEquals(variant, result.getVariant());
    }

    private static Stream<Arguments> kameleoonException_DataProvider() {
        return Stream.of(
                Arguments.of(new KameleoonException.FeatureNotFound("featureException"),
                        ErrorCode.FLAG_NOT_FOUND),
                Arguments.of(new KameleoonException.VisitorCodeInvalid("visitorCodeInvalid"),
                        ErrorCode.INVALID_CONTEXT));
    }

    @ParameterizedTest
    @MethodSource("kameleoonException_DataProvider")
    public void resolve_KameleoonException_ReturnsErrorProperError(KameleoonException exception,
            ErrorCode errorCode) {
        // Arrange
        KameleoonClient clientMock = mock(KameleoonClient.class);
        try {
            when(clientMock.getFeatureVariationKey(anyString(), anyString())).thenThrow(exception);
        } catch (KameleoonException e) {
            throw new RuntimeException(e);
        }
        KameleoonResolver resolver = new KameleoonResolver(clientMock);
        String flagKey = "testFlag";
        int defaultValue = 42;

        EvaluationContext context = new ImmutableContext("testVisitor");

        // Act
        ProviderEvaluation<Integer> result = resolver.resolve(flagKey, defaultValue, context);

        // Assert
        assertEquals(defaultValue, result.getValue());
        assertEquals(errorCode, result.getErrorCode());
        assertEquals(exception.getMessage(), result.getErrorMessage());
        assertNull(result.getVariant());
    }

    private static Stream<Arguments> resolve_ReturnsResultDetails_DataProvider() {
        return Stream.of(
                Arguments.of(null, Collections.singletonMap("k", 10), 10, 9),
                Arguments.of(null, Collections.singletonMap("k1", "str"), "str", "st"),
                Arguments.of(null, Collections.singletonMap("k2", true), true, false),
                Arguments.of(null, Collections.singletonMap("k3", 10.0), 10.0, 11.0),
                Arguments.of("varKey", Collections.singletonMap("varKey", 10.0), 10.0, 11.0));
    }

    @ParameterizedTest
    @MethodSource("resolve_ReturnsResultDetails_DataProvider")
    public void resolve_ReturnsResultDetails(String variableKey, Map<String, Object> variables,
            Object expectedValue, Object defaultValue) {
        // Arrange
        String variant = "variant";
        KameleoonClient clientMock = mock(KameleoonClient.class);
        try {
            when(clientMock.getFeatureVariationKey(anyString(), anyString())).thenReturn(variant);
            when(clientMock.getFeatureVariationVariables(anyString(), anyString()))
                    .thenReturn(variables);
        } catch (KameleoonException e) {
            throw new RuntimeException(e);
        }
        KameleoonResolver resolver = new KameleoonResolver(clientMock);
        String flagKey = "testFlag";

        Map<String, Value> values = new HashMap<>();
        if (variableKey != null) {
            values.put("variableKey", new Value(variableKey));
        }
        EvaluationContext context = new ImmutableContext("testVisitor", values);

        // Act
        ProviderEvaluation<Object> result = resolver.resolve(flagKey, defaultValue, context);

        // Assert
        assertEquals(expectedValue, result.getValue());
        assertNull(result.getErrorCode());
        assertNull(result.getErrorMessage());
        assertEquals(variant, result.getVariant());
    }
}
