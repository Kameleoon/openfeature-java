package com.kameleoon.openfeature;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ProviderEvaluation;

/**
 * Resolver interface which contains method for evalutions based on provided data
 */
interface Resolver {
    <T> ProviderEvaluation<T> resolve(String flagKey, T defaultValue, EvaluationContext context);
}
