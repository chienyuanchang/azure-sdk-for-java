// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.test.TestMode;
import com.azure.core.test.models.TestProxySanitizer;
import com.azure.core.test.models.TestProxySanitizerType;
import com.azure.core.test.utils.MockTokenCredential;
import com.azure.core.util.Configuration;
import com.azure.core.util.CoreUtils;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.AzurePipelinesCredential;
import com.azure.identity.AzurePipelinesCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.junit.jupiter.params.provider.Arguments;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.azure.core.test.TestProxyTestBase.AZURE_TEST_SERVICE_VERSIONS_VALUE_ALL;
import static com.azure.core.test.TestProxyTestBase.getHttpClients;

/**
 * Contains helper methods for generating inputs for test methods
 */
public final class TestUtils {
    private static final String REDACTED_VALUE = "REDACTED";
    private static final String URL_REGEX = "(?<=http://|https://)([^/?]+)";

    // Duration
    public static final Duration ONE_NANO_DURATION = Duration.ofMillis(1);
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(5);
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public static final String DISPLAY_NAME_WITH_ARGUMENTS = "{displayName} with [{arguments}]";
    public static final String[] REMOVE_SANITIZER_ID = { "AZSDK2003", "AZSDK2030" };

    // Local test files
    static final String SAMPLE_INVOICE_PDF = "sample_invoice.pdf";

    static final Configuration GLOBAL_CONFIGURATION = Configuration.getGlobalConfiguration();
    public static final String AZURE_CONTENTUNDERSTANDING_ENDPOINT_CONFIGURATION
        = GLOBAL_CONFIGURATION.get("CONTENTUNDERSTANDING_ENDPOINT");
    public static final String AZURE_CONTENTUNDERSTANDING_ENDPOINT
        = GLOBAL_CONFIGURATION.get("AZURE_CONTENT_UNDERSTANDING_ENDPOINT");
    public static final String AZURE_CONTENTUNDERSTANDING_KEY
        = GLOBAL_CONFIGURATION.get("AZURE_CONTENT_UNDERSTANDING_KEY");
    public static final String STORAGE_ACCOUNT_NAME = GLOBAL_CONFIGURATION.get("STORAGE_ACCOUNT_NAME");
    public static final String TRAINING_DATA_CONTAINER_NAME = GLOBAL_CONFIGURATION.get("TRAINING_DATA_CONTAINER_NAME");

    public static final String LOCAL_FILE_PATH = "src/test/resources/";
    static final String URL_TEST_FILE_FORMAT = "https://raw.githubusercontent.com/Azure/azure-sdk-for-java/"
        + "main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/test/resources/";

    private TestUtils() {
    }

    /**
     * Constructs a URL for a test file from the remote repository.
     *
     * @param fileName The name of the test file.
     * @return The URL to the test file.
     */
    static String urlSource(String fileName) {
        return URL_TEST_FILE_FORMAT + fileName;
    }

    /**
     * Reads test data from a local file.
     *
     * @param fileName The name of the test file.
     * @return The file data as a byte array.
     * @throws RuntimeException if the file cannot be read.
     */
    static byte[] getData(String fileName) {
        try {
            return Files.readAllBytes(Paths.get(LOCAL_FILE_PATH + fileName));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Local file not found.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a stream of arguments that includes all combinations of eligible {@link HttpClient HttpClients} and
     * service versions that should be tested.
     *
     * @return A stream of HttpClient and service version combinations to test.
     */
    static Stream<Arguments> getTestParameters() {
        List<Arguments> argumentsList = new ArrayList<>();

        getHttpClients().forEach(httpClient -> Arrays.stream(ContentUnderstandingServiceVersion.values())
            .filter(TestUtils::shouldServiceVersionBeTested)
            .forEach(serviceVersion -> argumentsList.add(Arguments.of(httpClient, serviceVersion))));
        return argumentsList.stream();
    }

    /**
     * Returns whether the given service version match the rules of test framework.
     *
     * <ul>
     * <li>Using latest service version as default if no environment variable is set.</li>
     * <li>If it's set to ALL, all Service versions in {@link ContentUnderstandingServiceVersion} will be tested.</li>
     * <li>Otherwise, Service version string should match env variable.</li>
     * </ul>
     *
     * Environment values currently supported are: "ALL", "${version}".
     *
     * @param serviceVersion ServiceVersion needs to check
     * @return Boolean indicates whether filters out the service version or not.
     */
    private static boolean shouldServiceVersionBeTested(ContentUnderstandingServiceVersion serviceVersion) {
        String serviceVersionFromEnv
            = Configuration.getGlobalConfiguration().get("AZURE_CONTENTUNDERSTANDING_TEST_SERVICE_VERSIONS");
        if (CoreUtils.isNullOrEmpty(serviceVersionFromEnv)) {
            return ContentUnderstandingServiceVersion.getLatest().equals(serviceVersion);
        }
        if (AZURE_TEST_SERVICE_VERSIONS_VALUE_ALL.equalsIgnoreCase(serviceVersionFromEnv)) {
            return true;
        }
        String[] configuredServiceVersionList = serviceVersionFromEnv.split(",");
        return Arrays.stream(configuredServiceVersionList)
            .anyMatch(configuredServiceVersion -> serviceVersion.getVersion().equals(configuredServiceVersion.trim()));
    }

    /**
     * Gets the list of sanitizers for test proxy to redact sensitive data.
     *
     * @return List of test proxy sanitizers.
     */
    public static List<TestProxySanitizer> getTestProxySanitizers() {
        return Arrays.asList(
            new TestProxySanitizer("(?<=contentunderstanding/)([^?]+)", REDACTED_VALUE, TestProxySanitizerType.URL),
            new TestProxySanitizer("$..id", REDACTED_VALUE, TestProxySanitizerType.BODY_KEY),
            new TestProxySanitizer("$..resultUrl", REDACTED_VALUE, TestProxySanitizerType.BODY_KEY),
            new TestProxySanitizer("Location", URL_REGEX, REDACTED_VALUE, TestProxySanitizerType.HEADER),
            new TestProxySanitizer("$..data", REDACTED_VALUE, TestProxySanitizerType.BODY_KEY));
    }

    /**
     * Retrieve the appropriate TokenCredential or AzureKeyCredential based on the test mode.
     *
     * @param testMode The test mode.
     * @return The appropriate credential (TokenCredential or AzureKeyCredential)
     */
    public static Object getTestTokenCredential(TestMode testMode) {
        // If API key is provided, use it (preferred for testing)
        if (!CoreUtils.isNullOrEmpty(AZURE_CONTENTUNDERSTANDING_KEY)) {
            return new AzureKeyCredential(AZURE_CONTENTUNDERSTANDING_KEY);
        }

        // Otherwise, use TokenCredential for Azure AD authentication
        if (testMode == TestMode.LIVE) {
            return tryGetPipelineCredentialOrElse(() -> new AzureCliCredentialBuilder().build());
        } else if (testMode == TestMode.RECORD) {
            return tryGetPipelineCredentialOrElse(() -> new DefaultAzureCredentialBuilder().build());
        } else {
            return new MockTokenCredential();
        }
    }

    /**
     * Attempts to speculate an {@link AzurePipelinesCredential} from the environment if the running context is within
     * Azure DevOps. If not, returns the {@link TokenCredential} supplied by {@code orElse}.
     *
     * @param orElse Supplies the {@link TokenCredential} to return if not running in Azure DevOps.
     * @return The AzurePipelinesCredential if running in Azure DevOps, or the {@code orElse} credential.
     */
    @SuppressWarnings("deprecation")
    private static TokenCredential tryGetPipelineCredentialOrElse(Supplier<TokenCredential> orElse) {
        Configuration configuration = Configuration.getGlobalConfiguration().clone();
        String serviceConnectionId = configuration.get("AZURESUBSCRIPTION_SERVICE_CONNECTION_ID");
        String clientId = configuration.get("AZURESUBSCRIPTION_CLIENT_ID");
        String tenantId = configuration.get("AZURESUBSCRIPTION_TENANT_ID");
        String systemAccessToken = configuration.get("SYSTEM_ACCESSTOKEN");

        if (CoreUtils.isNullOrEmpty(serviceConnectionId)
            || CoreUtils.isNullOrEmpty(clientId)
            || CoreUtils.isNullOrEmpty(tenantId)
            || CoreUtils.isNullOrEmpty(systemAccessToken)) {
            return orElse.get();
        }

        return new AzurePipelinesCredentialBuilder().systemAccessToken(systemAccessToken)
            .clientId(clientId)
            .tenantId(tenantId)
            .serviceConnectionId(serviceConnectionId)
            .build();
    }
}
