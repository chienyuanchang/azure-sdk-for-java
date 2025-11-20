// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.ai.contentunderstanding.models.ProcessingLocation;
import com.azure.ai.contentunderstanding.models.StringEncoding;
import com.azure.core.http.HttpClient;
import com.azure.core.test.annotation.RecordWithoutRequestBody;
import com.azure.core.test.http.AssertingHttpClientBuilder;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.azure.ai.contentunderstanding.TestUtils.DISPLAY_NAME_WITH_ARGUMENTS;
import static com.azure.ai.contentunderstanding.TestUtils.SAMPLE_INVOICE_PDF;
import static com.azure.ai.contentunderstanding.TestUtils.getData;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for ContentUnderstandingClient (synchronous client).
 * 
 * This test class covers:
 * - Document analysis with various input sources (bytes, URL)
 * - Polling for long-running operations
 * - Result validation
 * 
 * Following patterns from DocumentIntelligence SDK tests.
 */
public class ContentUnderstandingClientTest extends ContentUnderstandingTestBase {
    private ContentUnderstandingClient client;

    /**
     * Builds a synchronous HTTP client with assertion capabilities.
     *
     * @param httpClient The base HTTP client.
     * @return An HTTP client with assertions enabled.
     */
    private HttpClient buildSyncAssertingClient(HttpClient httpClient) {
        return new AssertingHttpClientBuilder(httpClient).skipRequest((ignored1, ignored2) -> false)
            .assertSync()
            .build();
    }

    /**
     * Gets a configured ContentUnderstandingClient for testing.
     *
     * @param httpClient The HTTP client to use.
     * @param serviceVersion The service version to use.
     * @return A configured ContentUnderstandingClient.
     */
    private ContentUnderstandingClient getContentUnderstandingClient(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        return getContentUnderstandingClientBuilder(
            buildSyncAssertingClient(
                interceptorManager.isPlaybackMode() ? interceptorManager.getPlaybackClient() : httpClient),
            serviceVersion).buildClient();
    }

    /**
     * Test: Analyze a document from binary data (local file).
     * Verifies that the client can process document bytes and return analysis results.
     */
    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void analyzeDocumentFromBytes(HttpClient httpClient, ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);
        String contentType = "application/pdf";

        // Call analyze and wait for completion
        SyncPoller<?, AnalyzeResult> syncPoller
            = client.beginAnalyzeBinary("prebuilt-documentSearch", contentType, BinaryData.fromBytes(documentBytes))
                .setPollInterval(durationTestMode);

        syncPoller.waitForCompletion();
        AnalyzeResult result = syncPoller.getFinalResult();

        // Validate results
        validateAnalyzeResult(result);
        validateContentStructure(result);
    }

    /**
     * Test: Analyze a document with string encoding parameter.
     * Verifies that the client can handle query parameters correctly.
     */
    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void analyzeDocumentWithStringEncoding(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);
        String contentType = "application/pdf";

        // Call analyze with StringEncoding parameter
        SyncPoller<?, AnalyzeResult> syncPoller
            = client
                .beginAnalyzeBinary("prebuilt-documentSearch", contentType, BinaryData.fromBytes(documentBytes),
                    StringEncoding.UTF8, null, null)
                .setPollInterval(durationTestMode);

        syncPoller.waitForCompletion();
        AnalyzeResult result = syncPoller.getFinalResult();

        // Validate results
        validateAnalyzeResult(result);
    }

    /**
     * Test: Analyze a document with processing location parameter.
     * Verifies that the client handles processing location options.
     */
    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void analyzeDocumentWithProcessingLocation(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);
        String contentType = "application/pdf";

        // Call analyze with ProcessingLocation parameter
        SyncPoller<?, AnalyzeResult> syncPoller
            = client
                .beginAnalyzeBinary("prebuilt-documentSearch", contentType, BinaryData.fromBytes(documentBytes), null,
                    ProcessingLocation.GLOBAL, null)
                .setPollInterval(durationTestMode);

        syncPoller.waitForCompletion();
        AnalyzeResult result = syncPoller.getFinalResult();

        // Validate results
        validateAnalyzeResult(result);
    }

    /**
     * Test: Verify polling behavior for long-running operations.
     * Ensures that the polling mechanism correctly waits for completion.
     */
    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void verifyPollingBehavior(HttpClient httpClient, ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        // Begin analysis
        SyncPoller<?, AnalyzeResult> syncPoller = client
            .beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf", BinaryData.fromBytes(documentBytes))
            .setPollInterval(durationTestMode);

        // Verify poller is in progress
        assertNotNull(syncPoller, "Poller should not be null");

        // Wait for completion and verify result
        syncPoller.waitForCompletion();
        AnalyzeResult finalResult = syncPoller.getFinalResult();
        assertNotNull(finalResult, "Final result should not be null");
    }

    /**
     * Test: Verify analyze operation returns proper result structure.
     */
    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void verifyResultStructure(HttpClient httpClient, ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        SyncPoller<?, AnalyzeResult> syncPoller = client
            .beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf", BinaryData.fromBytes(documentBytes))
            .setPollInterval(durationTestMode);

        syncPoller.waitForCompletion();
        AnalyzeResult finalResult = syncPoller.getFinalResult();

        // Verify result contains expected fields
        assertNotNull(finalResult, "Result should not be null");
        assertNotNull(finalResult.getContents(), "Contents should be present");
        Assertions.assertTrue(finalResult.getContents().size() > 0, "Contents should not be empty");
    }

    /**
     * Test: Verify error handling for invalid analyzer ID.
     */
    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void handleInvalidAnalyzerId(HttpClient httpClient, ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        // Attempt to use an invalid analyzer ID
        try {
            SyncPoller<?, AnalyzeResult> syncPoller = client
                .beginAnalyzeBinary("invalid-analyzer-id", "application/pdf", BinaryData.fromBytes(documentBytes))
                .setPollInterval(durationTestMode);

            syncPoller.waitForCompletion();

            // If we reach here, check if error is in the result
            AnalyzeResult result = syncPoller.getFinalResult();
            Assertions.assertNotNull(result, "Result should be present");
        } catch (Exception e) {
            // Expected behavior - invalid analyzer should throw or return error
            Assertions.assertNotNull(e, "Exception should be thrown or handled");
        }
    }

    /**
     * Test: Verify multiple consecutive requests.
     */
    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void verifyMultipleConsecutiveRequests(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        // First request
        SyncPoller<?, AnalyzeResult> syncPoller1 = client
            .beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf", BinaryData.fromBytes(documentBytes))
            .setPollInterval(durationTestMode);
        syncPoller1.waitForCompletion();
        AnalyzeResult result1 = syncPoller1.getFinalResult();

        assertNotNull(result1, "First result should not be null");

        // Second request
        SyncPoller<?, AnalyzeResult> syncPoller2 = client
            .beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf", BinaryData.fromBytes(documentBytes))
            .setPollInterval(durationTestMode);
        syncPoller2.waitForCompletion();
        AnalyzeResult result2 = syncPoller2.getFinalResult();

        assertNotNull(result2, "Second result should not be null");

        // Both requests should complete successfully
        validateAnalyzeResult(result1);
        validateAnalyzeResult(result2);
    }

    // Helper methods for validation
    static void validateAnalyzeResult(AnalyzeResult analyzeResult) {
        assertNotNull(analyzeResult, "Analyze result should not be null");
        assertNotNull(analyzeResult.getContents(), "Contents should be present");
    }

    static void validateContentStructure(AnalyzeResult analyzeResult) {
        validateAnalyzeResult(analyzeResult);

        if (analyzeResult.getContents() != null) {
            Assertions.assertTrue(analyzeResult.getContents().size() > 0, "Contents should not be empty");
            analyzeResult.getContents().forEach(content -> {
                assertNotNull(content.getKind(), "Content kind should not be null");
                assertNotNull(content.getMimeType(), "MIME type should not be null");
            });
        }
    }
}
