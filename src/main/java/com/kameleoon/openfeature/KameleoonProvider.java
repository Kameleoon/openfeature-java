package com.kameleoon.openfeature;

import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonClientConfig;
import com.kameleoon.KameleoonClientFactory;
import com.kameleoon.KameleoonException;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The {@link KameleoonProvider} is an OpenFeature {@link FeatureProvider} implementation
 * for the Kameleoon SDK.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * KameleoonProvider provider;
 *
 * try {
 *     KameleoonClientConfig clientConfig = new KameleoonClientConfig.Builder()
 *             .clientId("clientId")
 *             .clientSecret("clientSecret")
 *             .topLevelDomain("topLevelDomain")
 *             .build();
 *
 *     provider = new KameleoonProvider("siteCode", clientConfig);
 * } catch (KameleoonException.ConfigCredentialsInvalid e) {
 *     throw new RuntimeException(e);
 * }
 *
 * OpenFeatureAPI.getInstance().setProvider(provider);
 *
 * Client client = OpenFeatureAPI.getInstance().getClient();
 * </pre>
 */
public final class KameleoonProvider implements FeatureProvider {

    /**
     * The value Anonymous is used when no Targeted Key provided with the {@link EvaluationContext}
     */
    private static final String META_NAME = "Kameleoon Provider";

    private final String siteCode;
    private final Resolver resolver;

    /**
     * Represents an instance of the KameleoonClient SDK.
     * <p>
     * This client instance provides additional functionalities beyond those available in
     * OpenFeature.
     * </p>
     */
    private final KameleoonClient client;

    /**
     * Create a new instance of the provider with the given siteCode and config.
     *
     * @param siteCode Code of the website you want to run experiments on. This unique code id can
     *        be found
     *        in our platform's back-office. This field is mandatory.
     * @param config Configuration SDK object.
     */
    public KameleoonProvider(String siteCode, KameleoonClientConfig config) {
        this(siteCode, makeKameleoonClient(siteCode, config));
    }

    /**
     * Internal constructor which accepts siteCode and KameleoonClient instance. This constructor is
     * highly used for testing purposes to provide a specified KameleoonClient object.
     */
    KameleoonProvider(String siteCode, KameleoonClient client, Resolver resolver) {
        this.client = client;
        this.siteCode = siteCode;
        this.resolver = resolver;
    }

    /**
     * Private constructor which accepts siteCode and KameleoonClient instance. Provide default
     * implementation
     * of IResolver interface (Resolver class).
     */
    private KameleoonProvider(String siteCode, KameleoonClient client) {
        this(siteCode, client, new KameleoonResolver(client));
    }

    /**
     * Helper method to create a new KameleoonClient instance with error checking and conversion
     * their types
     * from KameleoonClient SDK to OpenFeature.
     */
    private static KameleoonClient makeKameleoonClient(String siteCode,
            KameleoonClientConfig config) {
        try {
            return KameleoonClientFactory.create(siteCode, config);
        } catch (KameleoonException.SiteCodeIsEmpty ex) {
            throw new ProviderNotReadyError(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metadata getMetadata() {
        return () -> META_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String flagKey, Boolean defaultValue,
            EvaluationContext context) {
        return resolver.resolve(flagKey, defaultValue, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderEvaluation<String> getStringEvaluation(String flagKey, String defaultValue,
            EvaluationContext context) {
        return resolver.resolve(flagKey, defaultValue, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String flagKey, Integer defaultValue,
            EvaluationContext context) {
        return resolver.resolve(flagKey, defaultValue, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderEvaluation<Double> getDoubleEvaluation(String flagKey, Double defaultValue,
            EvaluationContext context) {
        return resolver.resolve(flagKey, defaultValue, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderEvaluation<Value> getObjectEvaluation(String flagKey, Value defaultValue,
            EvaluationContext context) {
        ProviderEvaluation<Object> providerEvaluation =
                resolver.resolve(flagKey, defaultValue, context);
        return ProviderEvaluation.<Value>builder()
                .value(DataConverter.toOpenFeature(providerEvaluation.getValue()))
                .variant(providerEvaluation.getVariant())
                .errorCode(providerEvaluation.getErrorCode())
                .errorMessage(providerEvaluation.getErrorMessage())
                .reason(providerEvaluation.getReason())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(EvaluationContext context) {
        try {
            client.waitInit().get();
        } catch (InterruptedException | ExecutionException exception) {
            throw new ProviderNotReadyError(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        KameleoonClientFactory.forget(siteCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderState getState() {
        CompletableFuture<Void> task = client.waitInit();
        return task.isDone() && !task.isCompletedExceptionally() ? ProviderState.READY
                : ProviderState.NOT_READY;
    }

    /**
     * Returns the KameleoonClient SDK instance.
     *
     * @return the {@link KameleoonClient} SDK instance
     */
    public KameleoonClient getClient() {
        return client;
    }
}
