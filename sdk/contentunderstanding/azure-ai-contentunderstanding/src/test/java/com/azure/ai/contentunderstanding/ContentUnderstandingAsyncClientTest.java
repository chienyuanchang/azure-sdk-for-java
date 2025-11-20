// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.ai.contentunderstanding.models.StringEncoding;
import com.azure.core.http.HttpClient;
import com.azure.core.test.annotation.RecordWithoutRequestBody;
import com.azure.core.test.http.AssertingHttpClientBuilder;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.PollerFlux;
import com.azure.core.util.polling.SyncPoller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.azure.ai.contentunderstanding.TestUtils.DISPLAY_NAME_WITH_ARGUMENTS;
import static com.azure.ai.contentunderstanding.TestUtils.SAMPLE_INVOICE_PDF;
import static com.azure.ai.contentunderstanding.TestUtils.getData;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for ContentUnderstandingAsyncClient (asynchronous client).
 */
public class ContentUnderstandingAsyncClientTest extends ContentUnderstandingTestBase {
    private ContentUnderstandingAsyncClient client;

    private HttpClient buildAsyncAssertingClient(HttpClient httpClient) {
        return new AssertingHttpClientBuilder(httpClient).skipRequest((ignored1, ignored2) -> false)
            .assertAsync()
            .build();
    }

    private ContentUnderstandingAsyncClient getContentUnderstandingAsyncClient(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        return getContentUnderstandingClientBuilder(
            buildAsyncAssertingClient(
                interceptorManager.isPlaybackMode() ? interceptorManager.getPlaybackClient() : httpClient),
            serviceVersion).buildAsyncClient();
    }

    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void analyzeDocumentFromBytesAsync(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingAsyncClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        PollerFlux<?, AnalyzeResult> pollerFlux = client.beginAnalyzeBinary("prebuilt-documentSearch",
            "application/pdf", BinaryData.fromBytes(documentBytes));

        SyncPoller<?, AnalyzeResult> syncPoller = pollerFlux.setPollInterval(durationTestMode).getSyncPoller();

        syncPoller.waitForCompletion();
        AnalyzeResult result = syncPoller.getFinalResult();

        validateAnalyzeResult(result);
        validateContentStructure(result);
    }

    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void analyzeDocumentWithStringEncodingAsync(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingAsyncClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        PollerFlux<?, AnalyzeResult> pollerFlux = client.beginAnalyzeBinary("prebuilt-documentSearch",
            "application/pdf", BinaryData.fromBytes(documentBytes), StringEncoding.UTF8, null, null);

        SyncPoller<?, AnalyzeResult> syncPoller = pollerFlux.setPollInterval(durationTestMode).getSyncPoller();

        syncPoller.waitForCompletion();
        AnalyzeResult result = syncPoller.getFinalResult();

        validateAnalyzeResult(result);
    }

    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void verifyAsyncPollingCompletion(HttpClient httpClient, ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingAsyncClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        SyncPoller<?, AnalyzeResult> syncPoller = client
            .beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf", BinaryData.fromBytes(documentBytes))
            .setPollInterval(durationTestMode)
            .getSyncPoller();

        syncPoller.waitForCompletion();
        AnalyzeResult result = syncPoller.getFinalResult();

        assertNotNull(result, "Final result should not be null");
        assertNotNull(result.getContents(), "Contents should be present");
    }

    @RecordWithoutRequestBody
    @ParameterizedTest(name = DISPLAY_NAME_WITH_ARGUMENTS)
    @MethodSource("com.azure.ai.contentunderstanding.TestUtils#getTestParameters")
    public void verifyMultipleConsecutiveAsyncRequests(HttpClient httpClient,
        ContentUnderstandingServiceVersion serviceVersion) {
        client = getContentUnderstandingAsyncClient(httpClient, serviceVersion);

        byte[] documentBytes = getData(SAMPLE_INVOICE_PDF);

        SyncPoller<?, AnalyzeResult> syncPoller1 = client
            .beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf", BinaryData.fromBytes(documentBytes))
            .setPollInterval(durationTestMode)
            .getSyncPoller();
        syncPoller1.waitForCompletion();
        AnalyzeResult result1 = syncPoller1.getFinalResult();

        assertNotNull(result1, "First result should not be null");

        SyncPoller<?, AnalyzeResult> syncPoller2 = client
            .beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf", BinaryData.fromBytes(documentBytes))
            .setPollInterval(durationTestMode)
            .getSyncPoller();
        syncPoller2.waitForCompletion();
        AnalyzeResult result2 = syncPoller2.getFinalResult();

        assertNotNull(result2, "Second result should not be null");

        validateAnalyzeResult(result1);
        validateAnalyzeResult(result2);
    }

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
