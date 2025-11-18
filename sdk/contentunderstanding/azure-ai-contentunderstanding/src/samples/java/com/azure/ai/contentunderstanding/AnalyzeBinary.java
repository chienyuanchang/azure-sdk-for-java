// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerAnalyzeOperationStatus;
import com.azure.ai.contentunderstanding.models.DocumentContent;
import com.azure.ai.contentunderstanding.models.DocumentPage;
import com.azure.ai.contentunderstanding.models.DocumentTable;
import com.azure.ai.contentunderstanding.models.MediaContent;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.azure.core.util.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

/**
 * Sample for analyzing a PDF file using the prebuilt-documentSearch.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.AnalyzeBinary"
 */
public class AnalyzeBinary {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     * @throws IOException Exception thrown when there is an error reading the file.
     */
    public static void main(final String[] args) throws IOException {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Analyze Binary");
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

            // Step 3: Read the PDF file (use the single sample path under src/samples/resources)
            System.out.println("Step 3: Reading PDF file...");

            String pdfPath = "src/samples/resources/sample_invoice.pdf";
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                System.err.println("Error: Sample file not found: " + pdfFile.getAbsolutePath());
                System.err.println();
                System.err.println("Please ensure sample_invoice.pdf exists at the path above.");
                System.exit(1);
            }

            byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
            System.out.println("  File: " + pdfFile.getAbsolutePath());
            System.out.println("  Size: " + String.format("%,d", pdfBytes.length) + " bytes");
            System.out.println();

            // Step 4: Analyze document
            System.out.println("Step 4: Analyzing document...");
            System.out.println("  Analyzer: prebuilt-documentSearch");
            System.out.println("  Analyzing...");

            AnalyzeResult result;
            try {
                SyncPoller<ContentAnalyzerAnalyzeOperationStatus, AnalyzeResult> operation =
                    client.beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf",
                        BinaryData.fromBytes(pdfBytes));
                // SyncPoller does not expose getStatus() directly; poll once and print the poll response status.
                System.err.println(operation.poll().getStatus());
                result = operation.getFinalResult();
                System.out.println();
            } catch (Exception ex) {
                System.err.println("  Failed to analyze document: " + ex.getMessage());
                throw ex;
            }

            // Step 5: Display markdown content
            System.out.println("Step 5: Displaying markdown content...");
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

            // Step 6: Check if this is document content to access document-specific properties
            if (content instanceof DocumentContent) {
                DocumentContent documentContent = (DocumentContent) content;
                System.out.println("Step 6: Displaying document information...");
                System.out.println("  Document type: " + (documentContent.getMimeType() != null ? documentContent.getMimeType() : "(unknown)"));
                System.out.println("  Start page: " + documentContent.getStartPageNumber());
                System.out.println("  End page: " + documentContent.getEndPageNumber());
                System.out.println("  Total pages: " + (documentContent.getEndPageNumber() - documentContent.getStartPageNumber() + 1));
                System.out.println();

                // Check for pages
                List<DocumentPage> pages = documentContent.getPages();
                if (pages != null && !pages.isEmpty()) {
                    System.out.println("Step 7: Displaying page information...");
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
                    System.out.println("Step 8: Displaying table information...");
                    System.out.println("  Number of tables: " + tables.size());
                    int tableCounter = 1;
                    for (DocumentTable table : tables) {
                        System.out.println("  Table " + tableCounter + ": " + table.getRowCount() + " rows x " + table.getColumnCount() + " columns");
                        tableCounter++;
                    }
                    System.out.println();
                }
            } else {
                System.out.println("Step 6: Content Information:");
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
