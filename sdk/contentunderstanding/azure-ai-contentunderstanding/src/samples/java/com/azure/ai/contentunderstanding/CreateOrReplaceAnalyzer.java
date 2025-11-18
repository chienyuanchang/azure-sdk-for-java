// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeInput;
import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.ai.contentunderstanding.models.ContentAnalyzer;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerAnalyzeOperationStatus;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerConfig;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerOperationStatus;
import com.azure.ai.contentunderstanding.models.ContentField;
import com.azure.ai.contentunderstanding.models.ContentFieldDefinition;
import com.azure.ai.contentunderstanding.models.ContentFieldSchema;
import com.azure.ai.contentunderstanding.models.ContentFieldType;
import com.azure.ai.contentunderstanding.models.GenerationMethod;
import com.azure.ai.contentunderstanding.models.MediaContent;
import com.azure.ai.contentunderstanding.models.NumberField;
import com.azure.ai.contentunderstanding.models.StringField;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.Configuration;
import com.azure.core.util.polling.SyncPoller;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample for creating a custom analyzer using the CreateOrReplace API.
 *
 * Prerequisites:
 *     - Azure subscription
 *     - Azure Content Understanding resource
 *     - Java 8 or later
 *
 * Setup:
 *     Set the following environment variables or update the main method:
 *     - AZURE_CONTENT_UNDERSTANDING_ENDPOINT (required)
 *     - AZURE_CONTENT_UNDERSTANDING_KEY (optional - DefaultAzureCredential will be used if not set)
 *
 * To run:
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.CreateOrReplaceAnalyzer"
 *
 * This sample demonstrates:
 * 1. Authenticate with Azure AI Content Understanding
 * 2. Create a custom analyzer with field schema using object model
 * 3. Wait for analyzer creation to complete
 * 4. Clean up by deleting the created analyzer
 */
public class CreateOrReplaceAnalyzer {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Create Custom Analyzer");
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

            // Step 3: Define the custom analyzer
            System.out.println("Step 3: Defining custom analyzer...");

            // Generate a unique analyzer ID using timestamp
            // Note: Analyzer IDs cannot contain hyphens
            String analyzerId = "sdk_sample_custom_analyzer_" + System.currentTimeMillis() / 1000;
            System.out.println("  Analyzer ID: " + analyzerId);

            // Create field schema with custom fields
            Map<String, ContentFieldDefinition> fields = new HashMap<>();
            fields.put("company_name", new ContentFieldDefinition()
                .setType(ContentFieldType.STRING)
                .setMethod(GenerationMethod.EXTRACT)
                .setDescription("Name of the company"));
            fields.put("total_amount", new ContentFieldDefinition()
                .setType(ContentFieldType.NUMBER)
                .setMethod(GenerationMethod.EXTRACT)
                .setDescription("Total amount on the document"));

            ContentFieldSchema fieldSchema = new ContentFieldSchema()
                .setName("company_schema")
                .setDescription("Schema for extracting company information")
                .setFields(fields);

            // Create analyzer configuration
            ContentAnalyzerConfig config = new ContentAnalyzerConfig()
                .setEnableFormula(true)
                .setEnableLayout(true)
                .setEnableOcr(true)
                .setEstimateFieldSourceAndConfidence(true)
                .setReturnDetails(true);

            // Create the custom analyzer object
            // Note: Use "prebuilt-document" as the base analyzer for custom document analyzers
            // (not "prebuilt-documentAnalyzer" which is a different prebuilt)
            ContentAnalyzer customAnalyzer = new ContentAnalyzer()
                .setBaseAnalyzerId("prebuilt-document")
                .setDescription("Custom analyzer for extracting company information")
                .setConfig(config)
                .setFieldSchema(fieldSchema);

            // Add model mappings for completion and embedding models (required for custom analyzers)
            Map<String, String> models = new HashMap<>();
            models.put("completion", "gpt-4o-mini");
            models.put("embedding", "text-embedding-3-large");
            customAnalyzer.setModels(models);

            System.out.println("  Analyzer configuration:");
            System.out.println("    Base Analyzer: " + customAnalyzer.getBaseAnalyzerId());
            System.out.println("    Description: " + customAnalyzer.getDescription());
            System.out.println("    Fields: " + fieldSchema.getFields().size());
            System.out.println("    Models: " + customAnalyzer.getModels().size());
            System.out.println();

            // Step 4: Create the analyzer
            System.out.println("Step 4: Creating custom analyzer...");
            System.out.println("  This may take a few moments...");

            ContentAnalyzer result = null;
            boolean created = false;
            try {
                SyncPoller<ContentAnalyzerOperationStatus, ContentAnalyzer> operation =
                    client.beginCreateOrReplace(analyzerId, customAnalyzer);
                
                result = operation.getFinalResult();
                created = true;
                System.out.println("  ✅ Analyzer '" + analyzerId + "' created successfully!");
                System.out.println("  Status: " + result.getStatus());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                System.out.println("  Created at: " + result.getCreatedAt().format(formatter) + " UTC");
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to create analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 5: Use the analyzer to analyze an invoice
            if (created && result != null) {
                System.out.println("Step 5: Using the custom analyzer to analyze an invoice...");
                String fileUrl = "https://github.com/Azure-Samples/azure-ai-content-understanding-python/raw/refs/heads/main/data/invoice.pdf";
                System.out.println("  URL: " + fileUrl);
                System.out.println("  Analyzing...");

                try {
                    AnalyzeInput input = new AnalyzeInput().setUrl(fileUrl);
                    SyncPoller<ContentAnalyzerAnalyzeOperationStatus, AnalyzeResult> analyzeOperation =
                        client.beginAnalyze(analyzerId, null, null, Arrays.asList(input), null);

                    AnalyzeResult analyzeResult = analyzeOperation.getFinalResult();
                    System.out.println("  ✅ Analysis completed successfully!");
                    System.out.println();

                    // Display extracted custom fields
                    if (analyzeResult.getContents() != null && !analyzeResult.getContents().isEmpty()) {
                        MediaContent content = analyzeResult.getContents().get(0);
                        if (content.getFields() != null && !content.getFields().isEmpty()) {
                            System.out.println("  📋 Extracted Custom Fields:");
                            System.out.println("  " + repeat("-", 38));

                            // Extract the custom fields we defined
                            ContentField companyNameField = content.getFields().get("company_name");
                            if (companyNameField instanceof StringField) {
                                String companyName = ((StringField) companyNameField).getValueString();
                                System.out.println("    Company Name: " + (companyName != null ? companyName : "(not found)"));
                            }

                            ContentField totalAmountField = content.getFields().get("total_amount");
                            if (totalAmountField instanceof NumberField) {
                                Double totalAmount = ((NumberField) totalAmountField).getValueNumber();
                                System.out.println("    Total Amount: " + (totalAmount != null ? String.format("%.2f", totalAmount) : "(not found)"));
                            }

                            System.out.println();
                        } else {
                            System.out.println("  No fields extracted");
                            System.out.println();
                        }
                    }
                } catch (HttpResponseException ex) {
                    System.err.println("  Failed to analyze with custom analyzer: " + ex.getMessage());
                    System.err.println("  Status: " + ex.getResponse().getStatusCode());
                    // Continue to cleanup even if analysis fails
                }
            }

            // Step 6: Clean up (delete the created analyzer)
            if (created && result != null) {
                System.out.println("Step 6: Cleaning up (deleting analyzer)...");
                try {
                    client.delete(analyzerId);
                    System.out.println("  ✅ Analyzer '" + analyzerId + "' deleted successfully!");
                    System.out.println();
                } catch (HttpResponseException ex) {
                    System.err.println("  Failed to delete analyzer: " + ex.getMessage());
                    System.err.println("  Status: " + ex.getResponse().getStatusCode());
                    // Don't throw - cleanup failure shouldn't fail the sample
                }
            }

            System.out.println("=============================================================");
            System.out.println("✓ Sample completed successfully");
            System.out.println("=============================================================");
            System.out.println();
            System.out.println("This sample demonstrated:");
            System.out.println("  1. Creating a custom analyzer with field schema");
            System.out.println("  2. Using the custom analyzer to extract structured fields");
            System.out.println("  3. Cleaning up by deleting the analyzer");
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("  - To retrieve analyzers: see ListAnalyzers sample");
            System.out.println("  - To analyze with prebuilt analyzers: see AnalyzeBinary or AnalyzeUrl samples");
        } catch (HttpResponseException ex) {
            if (ex.getResponse().getStatusCode() == 401) {
                System.err.println();
                System.err.println("✗ Authentication failed");
                System.err.println("  Error: " + ex.getMessage());
                System.err.println("  Please check your credentials and ensure they are valid.");
                System.exit(1);
            } else {
                System.err.println();
                System.err.println("✗ Service request failed");
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                System.err.println("  Message: " + ex.getMessage());
                System.exit(1);
            }
        } catch (Exception ex) {
            System.err.println();
            System.err.println("✗ An unexpected error occurred");
            System.err.println("  Error: " + ex.getMessage());
            System.err.println("  Type: " + ex.getClass().getName());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
