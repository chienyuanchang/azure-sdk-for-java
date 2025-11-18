// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeInput;
import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerAnalyzeOperationStatus;
import com.azure.ai.contentunderstanding.models.DocumentContent;
import com.azure.ai.contentunderstanding.models.DocumentPage;
import com.azure.ai.contentunderstanding.models.DocumentTable;
import com.azure.ai.contentunderstanding.models.MediaContent;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.SyncPoller;
import com.azure.core.util.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Sample for analyzing a document from a URL using the prebuilt-documentSearch.
 *
 * Prerequisites:
 *     - Azure subscription
 *     - Azure Content Understanding resource
 *     - Java 8 or later
 *
 * Setup:
 *     Set the following environment variables or update the main method:
 *     - AZURE_CONTENT_UNDERSTANDING_ENDPOINT (required)
 *     - AZURE_CONTENT_UNDERSTANDING_KEY (required for key authentication)
 *
 * To run:
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.AnalyzeUrl"
 */
public class AnalyzeUrl {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Analyze URL");
        System.out.println("=============================================================");
        System.out.println();

        try {
            // Step 1: Load configuration from environment variables / global configuration
            System.out.println("Step 1: Loading configuration...");
            String endpoint = Configuration.getGlobalConfiguration().get("AZURE_CONTENT_UNDERSTANDING_ENDPOINT");
            String apiKey = Configuration.getGlobalConfiguration().get("AZURE_CONTENT_UNDERSTANDING_KEY");

            if (endpoint == null || endpoint.trim().isEmpty()) {
                System.err.println("Error: AZURE_CONTENT_UNDERSTANDING_ENDPOINT is required.");
                System.err.println("Please set it in environment variables.");
                System.exit(1);
            }

            // Trim and validate endpoint
            endpoint = endpoint.trim();
            try {
                new URL(endpoint);
            } catch (MalformedURLException e) {
                System.err.println("Error: Invalid endpoint URL: " + endpoint);
                System.err.println("Endpoint must be a valid absolute URI (e.g., https://your-resource.cognitiveservices.azure.com/)");
                System.exit(1);
            }

            System.out.println("  Endpoint: " + endpoint);
            System.out.println();

            // Step 2: Create the client with appropriate authentication
            System.out.println("Step 2: Creating Content Understanding client...");
            ContentUnderstandingClient client;

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                System.out.println("  Authentication: API Key");
                client = new ContentUnderstandingClientBuilder()
                    .credential(new AzureKeyCredential(apiKey))
                    .endpoint(endpoint)
                    .buildClient();
            } else {
                System.err.println("Error: AZURE_CONTENT_UNDERSTANDING_KEY is required.");
                System.err.println("Please set it in environment variables.");
                System.exit(1);
                return;
            }
            System.out.println();

            // Step 3: Analyze document from URL
            System.out.println("Step 3: Analyzing document from URL...");
            String fileUrl = "https://github.com/Azure-Samples/azure-ai-content-understanding-python/raw/refs/heads/main/data/invoice.pdf";
            System.out.println("  URL: " + fileUrl);
            System.out.println("  Analyzer: prebuilt-documentSearch");
            System.out.println("  Analyzing...");

            AnalyzeResult result;
            try {
                AnalyzeInput analyzeInput = new AnalyzeInput().setUrl(fileUrl);
                SyncPoller<ContentAnalyzerAnalyzeOperationStatus, AnalyzeResult> operation =
                    client.beginAnalyze("prebuilt-documentSearch", null, null,
                        Collections.singletonList(analyzeInput), null);
                result = operation.getFinalResult();
                System.out.println("  Analysis completed successfully");
                System.out.println("  Result: AnalyzerId=" + result.getAnalyzerId() + ", Contents count=" + 
                    (result.getContents() != null ? result.getContents().size() : 0));
                System.out.println();
            } catch (Exception ex) {
                System.err.println("  Failed to analyze document: " + ex.getMessage());
                throw ex;
            }

            // Step 4: Display markdown content
            System.out.println("Step 4: Displaying markdown content...");
            System.out.println("=============================================================");

            // A PDF file has only one content element even if it contains multiple pages
            MediaContent content = null;
            if (result.getContents() == null || result.getContents().isEmpty()) {
                System.out.println("(No content returned from analysis)");
            } else {
                content = result.getContents().get(0);
                if (content != null && content.getMarkdown() != null && !content.getMarkdown().isEmpty()) {
                    System.out.println(content.getMarkdown());
                } else {
                    System.out.println("(No markdown content available)");
                }
            }

            System.out.println("=============================================================");
            System.out.println();

            // Step 5: Check if this is document content to access document-specific properties
            if (content instanceof DocumentContent) {
                DocumentContent documentContent = (DocumentContent) content;
                System.out.println("Step 5: Displaying document information...");
                System.out.println("  Document type: " + (documentContent.getMimeType() != null ? documentContent.getMimeType() : "(unknown)"));
                System.out.println("  Start page: " + documentContent.getStartPageNumber());
                System.out.println("  End page: " + documentContent.getEndPageNumber());
                System.out.println("  Total pages: " + (documentContent.getEndPageNumber() - documentContent.getStartPageNumber() + 1));
                System.out.println();

                // Check for pages
                List<DocumentPage> pages = documentContent.getPages();
                if (pages != null && !pages.isEmpty()) {
                    System.out.println("Step 6: Displaying page information...");
                    System.out.println("  Number of pages: " + pages.size());
                    for (DocumentPage page : pages) {
                        String unit = documentContent.getUnit() != null ? documentContent.getUnit().toString() : "units";
                        System.out.println("  Page " + page.getPageNumber() + ": " + page.getWidth() + " x " + page.getHeight() + " " + unit);
                    }
                    System.out.println();
                }

                // Check for tables
                List<DocumentTable> tables = documentContent.getTables();
                if (tables != null && !tables.isEmpty()) {
                    System.out.println("Step 7: Displaying table information...");
                    System.out.println("  Number of tables: " + tables.size());
                    int tableCounter = 1;
                    for (DocumentTable table : tables) {
                        System.out.println("  Table " + tableCounter + ": " + table.getRowCount() + " rows x " + table.getColumnCount() + " columns");
                        tableCounter++;
                    }
                    System.out.println();
                }
            } else {
                System.out.println("Step 5: Content Information:");
                System.out.println("  Not a document content type - document-specific information is not available");
                System.out.println();
            }

            System.out.println("=============================================================");
            System.out.println("✓ Sample completed successfully");
            System.out.println("=============================================================");
        } catch (Exception ex) {
            System.err.println();
            System.err.println("✗ An unexpected error occurred");
            System.err.println("  Error: " + ex.getMessage());
            System.err.println("  Type: " + ex.getClass().getName());
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
