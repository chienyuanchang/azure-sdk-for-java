// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.test.models.BodilessMatcher;
import com.azure.core.test.TestMode;
import com.azure.core.test.TestProxyTestBase;
import com.azure.core.test.utils.MockTokenCredential;
import com.azure.core.util.Configuration;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.azure.ai.contentunderstanding.TestUtils.DEFAULT_POLL_INTERVAL;
import static com.azure.ai.contentunderstanding.TestUtils.ONE_NANO_DURATION;
import static com.azure.ai.contentunderstanding.TestUtils.REMOVE_SANITIZER_ID;
import static com.azure.ai.contentunderstanding.TestUtils.getTestProxySanitizers;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Base class for all Content Understanding tests. Provides common setup and configuration for test classes.
 */
public abstract class ContentUnderstandingTestBase extends TestProxyTestBase {
    /**
     * Duration to use in tests. Uses a small duration for playback mode to speed up tests,
     * and a larger duration for live mode to give the service time to process requests.
     */
    Duration durationTestMode;
    private boolean sanitizersRemoved = false;

    /**
     * Use duration of nearly zero value for PLAYBACK test mode, otherwise, use default duration value for LIVE mode.
     */
    @Override
    protected void beforeTest() {
        durationTestMode = interceptorManager.isPlaybackMode() ? ONE_NANO_DURATION : DEFAULT_POLL_INTERVAL;
    }

    /**
     * Creates a ContentUnderstandingClientBuilder with proper configuration for testing.
     *
     * @param httpClient The HTTP client to use.
     * @param serviceVersion The service version to use.
     * @return A configured ContentUnderstandingClientBuilder.
     */
    public ContentUnderstandingClientBuilder getContentUnderstandingClientBuilder(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        String endpoint = getEndpoint();

        Object credential = TestUtils.getTestTokenCredential(testContextManager.getTestMode());

        ContentUnderstandingClientBuilder builder = new ContentUnderstandingClientBuilder().endpoint(endpoint)
            .httpClient(interceptorManager.isPlaybackMode() ? interceptorManager.getPlaybackClient() : httpClient)
            .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
            .serviceVersion(serviceVersion);

        // Support both AzureKeyCredential (API key) and TokenCredential (Azure AD)
        if (credential instanceof AzureKeyCredential) {
            builder.credential((AzureKeyCredential) credential);
        } else {
            builder.credential((TokenCredential) credential);
        }

        if (interceptorManager.isPlaybackMode()) {
            setMatchers();
        } else if (interceptorManager.isRecordMode()) {
            builder.addPolicy(interceptorManager.getRecordPolicy());
        }
        if (!interceptorManager.isLiveMode() && !sanitizersRemoved) {
            interceptorManager.addSanitizers(getTestProxySanitizers());
            interceptorManager.removeSanitizers(REMOVE_SANITIZER_ID);
            sanitizersRemoved = true;
        }
        return builder;
    }

    /**
     * Sets matchers for test proxy to match requests in playback mode.
     */
    private void setMatchers() {
        interceptorManager.addMatchers(Collections.singletonList(new BodilessMatcher()));
    }

    /**
     * Gets the endpoint for the Content Understanding service from environment configuration.
     *
     * @return The service endpoint.
     */
    protected String getEndpoint() {
        // Try multiple environment variable names for endpoint
        String endpoint = Configuration.getGlobalConfiguration().get("CONTENTUNDERSTANDING_ENDPOINT");
        if (endpoint == null) {
            endpoint = Configuration.getGlobalConfiguration().get("AZURE_CONTENT_UNDERSTANDING_ENDPOINT");
        }
        if (endpoint == null) {
            // Use a default endpoint for testing (will be overridden in PLAYBACK mode)
            endpoint = "https://contentunderstanding.cognitiveservices.azure.com/";
        }
        return endpoint;
    }

    /**
     * Validates that analyze result is not null and contains expected data.
     *
     * @param analyzeResult The result from analyze operation.
     */
    static void validateAnalyzeResult(AnalyzeResult analyzeResult) {
        assertNotNull(analyzeResult, "Analyze result should not be null");
        assertNotNull(analyzeResult.getContents(), "Contents should not be null");
        Assertions.assertTrue(analyzeResult.getContents().size() > 0, "Contents should not be empty");
    }

    /**
     * Validates the structure of a content item in the analyze result.
     *
     * @param analyzeResult The result from analyze operation.
     */
    static void validateContentStructure(AnalyzeResult analyzeResult) {
        validateAnalyzeResult(analyzeResult);

        if (analyzeResult.getContents() != null) {
            analyzeResult.getContents().forEach(content -> {
                assertNotNull(content.getKind(), "Content kind should not be null");
                assertNotNull(content.getMimeType(), "MIME type should not be null");
            });
        }
    }
}
