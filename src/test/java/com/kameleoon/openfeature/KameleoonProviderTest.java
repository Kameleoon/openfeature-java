package com.kameleoon.openfeature;

import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonClientConfig;
import com.kameleoon.KameleoonClientFactory;
import com.kameleoon.KameleoonException;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class KameleoonProviderTest {

    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String SITE_CODE = "siteCode";
    private static final String FLAG_KEY = "flagKey";

    private KameleoonClientConfig config;
    private KameleoonClient clientMock;
    private KameleoonResolver resolverMock;
    private KameleoonProvider provider;
    private CompletableFuture<Void> readyFuture;

    @Before
    public void setUp() {
        clientMock = mock(KameleoonClient.class);
        readyFuture = new CompletableFuture<>();
        resolverMock = mock(KameleoonResolver.class);
        when(clientMock.waitInit()).thenReturn(readyFuture);
        try {
            config = new KameleoonClientConfig.Builder()
                    .clientId(CLIENT_ID)
                    .clientSecret(CLIENT_SECRET)
                    .build();
            provider = new KameleoonProvider(SITE_CODE, clientMock, resolverMock);
        } catch (KameleoonException.ConfigCredentialsInvalid e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = ProviderNotReadyError.class)
    public void initWithInvalidSiteCodeThrowsFeatureProviderException() {
        String siteCode = "";
        new KameleoonProvider(siteCode, config);
    }

    @Test
    public void getMetadataReturnsCorrectMetadata() {
        // Act
        Metadata metadata = provider.getMetadata();

        // Assert
        assertEquals("Kameleoon Provider", metadata.getName());
    }

    private <T> void setupResolverMock(T defaultValue, T expectedValue) {
        when(resolverMock.resolve(FLAG_KEY, defaultValue, null))
                .thenReturn(ProviderEvaluation.<T>builder().value(expectedValue).build());
    }

    private <T> void assertResult(ProviderEvaluation<T> result, T expectedValue) {
        assertEquals(expectedValue, result.getValue());
        assertNull(result.getErrorCode());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void resolveBooleanValueReturnsCorrectValue() {
        // Arrange
        boolean defaultValue = false;
        boolean expectedValue = true;
        setupResolverMock(defaultValue, expectedValue);

        // Act
        ProviderEvaluation<Boolean> result =
                provider.getBooleanEvaluation(FLAG_KEY, defaultValue, null);

        // Assert
        assertResult(result, expectedValue);
    }

    @Test
    public void resolveDoubleValueReturnsCorrectValue() {
        // Arrange
        double defaultValue = 0.5;
        double expectedValue = 2.5;
        setupResolverMock(defaultValue, expectedValue);

        // Act
        ProviderEvaluation<Double> result =
                provider.getDoubleEvaluation(FLAG_KEY, defaultValue, null);

        // Assert
        assertResult(result, expectedValue);
    }

    @Test
    public void resolveIntegerValueReturnsCorrectValue() {
        // Arrange
        int defaultValue = 1;
        int expectedValue = 2;
        setupResolverMock(defaultValue, expectedValue);

        // Act
        ProviderEvaluation<Integer> result =
                provider.getIntegerEvaluation(FLAG_KEY, defaultValue, null);

        // Assert
        assertResult(result, expectedValue);
    }

    @Test
    public void resolveStringValueReturnsCorrectValue() throws Exception {
        // Arrange
        String defaultValue = "1";
        String expectedValue = "2";
        setupResolverMock(defaultValue, expectedValue);

        // Act
        ProviderEvaluation<String> result =
                provider.getStringEvaluation(FLAG_KEY, defaultValue, null);

        // Assert
        assertResult(result, expectedValue);
    }

    @Test
    public void resolveStructureValueReturnsCorrectValue() {
        // Arrange
        Object defaultObjectValue = new Value("default");
        Value defaultValue = new Value("default");
        Object expectedResult = new Value("expected");
        Object mockResult = "expected";
        when(resolverMock.resolve(FLAG_KEY, defaultObjectValue, null))
                .thenReturn(ProviderEvaluation.builder().value(mockResult).build());

        // Act
        ProviderEvaluation<Value> result =
                provider.getObjectEvaluation(FLAG_KEY, defaultValue, null);

        // Assert
        assertEquals(expectedResult, result.getValue());
        assertNull(result.getErrorCode());
        assertNull(result.getErrorMessage());
    }


    @Test
    public void resolveStructureValueReturnsDefaultValue() {
        // Arrange
        Object defaultObjectValue = new Value("default");
        Value defaultValue = new Value("default");
        Value expectedResult = new Value("default");
        when(resolverMock.resolve(FLAG_KEY, defaultObjectValue, null))
                .thenReturn(ProviderEvaluation.builder().value(defaultValue).build());

        // Act
        ProviderEvaluation<Value> result =
                provider.getObjectEvaluation(FLAG_KEY, defaultValue, null);

        // Assert
        assertEquals(expectedResult, result.getValue());
        assertNull(result.getErrorCode());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void readyProviderStatus() {
        // Arrange
        readyFuture.complete(null);

        // Assert
        assertEquals(ProviderState.READY, provider.getState());
    }

    @Test
    public void notReadyProviderStatus() {
        // Assert
        assertEquals(ProviderState.NOT_READY, provider.getState());
    }

    @Test
    public void errorProviderStatus() {
        // Arrange
        readyFuture.completeExceptionally(new KameleoonException.SDKNotReady(""));

        // Assert
        assertEquals(ProviderState.NOT_READY, provider.getState());
    }

    @Test
    public void shutdownForgetSiteCode() throws Exception {
        // Arrange
        KameleoonProvider provider = new KameleoonProvider(SITE_CODE, config);
        KameleoonClient clientFirst = provider.getClient();
        KameleoonClient clientToCheck = KameleoonClientFactory.create(SITE_CODE, config);

        // Act
        provider.shutdown();
        KameleoonProvider providerSecond = new KameleoonProvider(SITE_CODE, config);
        KameleoonClient clientSecond = providerSecond.getClient();

        // Assert
        assertSame(clientToCheck, clientFirst);
        assertNotSame(clientFirst, clientSecond);
    }
}
