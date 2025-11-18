// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.rest.RequestOptions;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Configuration;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sample for analyzing a PDF file and saving the raw JSON response.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.AnalyzeBinaryRawJson"
 *
 * IMPORTANT NOTES:
 * - The SDK returns analysis results with an object model, which is easier to navigate and retrieve
 *   the desired results compared to parsing raw JSON
 * - This sample is ONLY for demonstration purposes to show how to access raw JSON responses
 * - For production use, prefer the object model approach shown in:
 *   - AnalyzeBinary sample
 */
public class AnalyzeBinaryRawJson {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     * @throws IOException Exception thrown when there is an error reading the file or writing the output.
     */
    public static void main(final String[] args) throws IOException {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Analyze Binary (Raw JSON)");
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

            // Step 3: Read the PDF file
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

            // Step 4: Analyze document using protocol method to get raw response
            System.out.println("Step 4: Analyzing document...");
            System.out.println("  Analyzer: prebuilt-documentSearch");
            System.out.println("  Using protocol method to access raw JSON response");
            System.out.println("  Analyzing...");

            BinaryData responseData;
            try {
                // Use the protocol method to get raw response
                RequestOptions requestOptions = new RequestOptions();
                SyncPoller<BinaryData, BinaryData> operation =
                    client.beginAnalyzeBinary("prebuilt-documentSearch", "application/pdf",
                        BinaryData.fromBytes(pdfBytes), requestOptions);

                responseData = operation.getFinalResult();
                System.out.println("  Analysis completed successfully");
                System.out.println();
            } catch (Exception ex) {
                System.err.println("  Failed to analyze document: " + ex.getMessage());
                throw ex;
            }

            // Step 5: Parse and pretty-print the raw JSON
            System.out.println("Step 5: Processing raw JSON response...");

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            JsonNode jsonNode = objectMapper.readTree(responseData.toBytes());
            String prettyJson = objectMapper.writeValueAsString(jsonNode);

            // Create output directory if it doesn't exist
            File outputDir = new File("sample_output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Save to file
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String outputFileName = "analyze_result_" + LocalDateTime.now().format(formatter) + ".json";
            File outputFile = new File(outputDir, outputFileName);
            Files.write(outputFile.toPath(), prettyJson.getBytes());

            System.out.println("  Raw JSON response saved to: " + outputFile.getAbsolutePath());
            System.out.println("  File size: " + String.format("%,d", prettyJson.length()) + " characters");
            System.out.println();

            // Step 6: Display some key information from the response
            System.out.println("Step 6: Displaying key information from response...");
            JsonNode resultElement = jsonNode.get("result");

            if (resultElement != null && resultElement.has("analyzerId")) {
                System.out.println("  Analyzer ID: " + resultElement.get("analyzerId").asText());
            }

            if (resultElement != null && resultElement.has("contents") && resultElement.get("contents").isArray()) {
                JsonNode contentsElement = resultElement.get("contents");
                System.out.println("  Contents count: " + contentsElement.size());

                if (contentsElement.size() > 0) {
                    JsonNode firstContent = contentsElement.get(0);
                    if (firstContent.has("kind")) {
                        System.out.println("  Content kind: " + firstContent.get("kind").asText());
                    }
                    if (firstContent.has("mimeType")) {
                        System.out.println("  MIME type: " + firstContent.get("mimeType").asText());
                    }
                }
            }
            System.out.println();

            System.out.println("=============================================================");
            System.out.println("✓ Sample completed successfully");
            System.out.println("=============================================================");
            System.out.println();
            System.out.println("NOTE: For easier data access, prefer using the object model");
            System.out.println("      approach shown in the AnalyzeBinary sample instead of");
            System.out.println("      parsing raw JSON manually.");
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
