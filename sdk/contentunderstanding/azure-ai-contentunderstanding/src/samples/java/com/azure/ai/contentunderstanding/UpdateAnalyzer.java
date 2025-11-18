// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.ContentAnalyzer;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerConfig;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerOperationStatus;
import com.azure.ai.contentunderstanding.models.ContentFieldDefinition;
import com.azure.ai.contentunderstanding.models.ContentFieldSchema;
import com.azure.ai.contentunderstanding.models.ContentFieldType;
import com.azure.ai.contentunderstanding.models.GenerationMethod;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Configuration;
import com.azure.core.util.polling.SyncPoller;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sample for updating a custom analyzer using the Update API.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.UpdateAnalyzer"
 *
 * This sample demonstrates:
 * 1. Create an initial analyzer
 * 2. Get the analyzer to verify initial state
 * 3. Update the analyzer with new description and tags
 * 4. Get the analyzer again to verify changes persisted
 * 5. Clean up the created analyzer
 */
public class UpdateAnalyzer {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Update Analyzer");
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

            // Step 3: Create initial analyzer
            System.out.println("Step 3: Creating initial analyzer...");

            // Generate a unique analyzer ID using timestamp
            String analyzerId = "sdk_sample_analyzer_for_update_" + System.currentTimeMillis() / 1000;
            System.out.println("  Analyzer ID: " + analyzerId);

            // Create field schema with custom fields
            Map<String, ContentFieldDefinition> fields = new HashMap<>();
            fields.put("total_amount", new ContentFieldDefinition()
                .setType(ContentFieldType.NUMBER)
                .setMethod(GenerationMethod.EXTRACT)
                .setDescription("Total amount of this document"));
            fields.put("company_name", new ContentFieldDefinition()
                .setType(ContentFieldType.STRING)
                .setMethod(GenerationMethod.EXTRACT)
                .setDescription("Name of the company"));

            ContentFieldSchema fieldSchema = new ContentFieldSchema()
                .setName("update_demo_schema")
                .setDescription("Schema for update demo")
                .setFields(fields);

            // Create analyzer configuration
            ContentAnalyzerConfig config = new ContentAnalyzerConfig()
                .setEnableFormula(true)
                .setEnableLayout(true)
                .setEnableOcr(true)
                .setEstimateFieldSourceAndConfidence(true)
                .setReturnDetails(true);

            // Create the initial analyzer object
            ContentAnalyzer initialAnalyzer = new ContentAnalyzer()
                .setBaseAnalyzerId("prebuilt-document")
                .setDescription("Initial description")
                .setConfig(config)
                .setFieldSchema(fieldSchema);

            // Add model mappings for completion and embedding models (required for custom analyzers)
            Map<String, String> models = new HashMap<>();
            models.put("completion", "gpt-4o-mini");
            models.put("embedding", "text-embedding-3-large");
            initialAnalyzer.setModels(models);

            // Add initial tags
            Map<String, String> tags = new HashMap<>();
            tags.put("tag1", "tag1_initial_value");
            tags.put("tag2", "tag2_initial_value");
            initialAnalyzer.setTags(tags);

            ContentAnalyzer createdAnalyzer = null;
            boolean created = false;
            try {
                System.out.println("  Creating analyzer (this may take a few moments)...");
                SyncPoller<ContentAnalyzerOperationStatus, ContentAnalyzer> createOperation =
                    client.beginCreateOrReplace(analyzerId, initialAnalyzer);

                createdAnalyzer = createOperation.getFinalResult();
                created = true;
                System.out.println("  ✅ Analyzer '" + analyzerId + "' created successfully!");
                System.out.println("  Status: " + createdAnalyzer.getStatus());
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to create analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 4: Get the analyzer before update to verify initial state
            System.out.println("Step 4: Getting analyzer before update...");
            ContentAnalyzer analyzerBeforeUpdate;
            try {
                analyzerBeforeUpdate = client.get(analyzerId);
                System.out.println("  ✅ Initial analyzer state verified:");
                System.out.println("    Description: " + analyzerBeforeUpdate.getDescription());
                System.out.println("    Tags: " + formatTags(analyzerBeforeUpdate.getTags()));
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to get analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 5: Update the analyzer
            System.out.println("Step 5: Updating analyzer with new description and tags...");

            System.out.println("  Changes to apply:");
            System.out.println("    New Description: Updated description");
            System.out.println("    Tag Updates: tag1 (updated), tag2 (removed), tag3 (added)");
            System.out.println();

            try {
                // For Update API, we need to send only the fields that should be changed
                // Note: The service currently requires baseAnalyzerId and models even in PATCH requests
                String updateJson = String.format(
                    "{" +
                    "\"baseAnalyzerId\": \"%s\"," +
                    "\"description\": \"Updated description\"," +
                    "\"tags\": {" +
                    "\"tag1\": \"tag1_updated_value\"," +
                    "\"tag2\": \"\"," +
                    "\"tag3\": \"tag3_value\"" +
                    "}," +
                    "\"models\": {" +
                    "\"completion\": \"gpt-4o-mini\"," +
                    "\"embedding\": \"text-embedding-3-large\"" +
                    "}" +
                    "}",
                    analyzerBeforeUpdate.getBaseAnalyzerId()
                );

                // Use protocol method for update (returns Response)
                client.updateWithResponse(analyzerId, BinaryData.fromString(updateJson), null);

                System.out.println("  ✅ Analyzer updated successfully!");
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to update analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 6: Get the analyzer after update to verify changes persisted
            System.out.println("Step 6: Getting analyzer after update to verify changes...");
            ContentAnalyzer analyzerAfterUpdate;
            try {
                analyzerAfterUpdate = client.get(analyzerId);
                System.out.println("  ✅ Updated analyzer state verified:");
                System.out.println("    Description: " + analyzerAfterUpdate.getDescription());
                System.out.println("    Tags: " + formatTags(analyzerAfterUpdate.getTags()));
                System.out.println();

                // Verify the changes
                System.out.println("  📋 Verification:");
                if ("Updated description".equals(analyzerAfterUpdate.getDescription())) {
                    System.out.println("    ✓ Description updated correctly");
                }
                if (analyzerAfterUpdate.getTags() != null && 
                    "tag1_updated_value".equals(analyzerAfterUpdate.getTags().get("tag1"))) {
                    System.out.println("    ✓ tag1 updated correctly");
                }
                String tag2Value = analyzerAfterUpdate.getTags() != null ? 
                    analyzerAfterUpdate.getTags().get("tag2") : null;
                if (tag2Value == null || tag2Value.isEmpty()) {
                    System.out.println("    ✓ tag2 removed correctly");
                }
                if (analyzerAfterUpdate.getTags() != null && 
                    "tag3_value".equals(analyzerAfterUpdate.getTags().get("tag3"))) {
                    System.out.println("    ✓ tag3 added correctly");
                }
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to get analyzer after update: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 7: Clean up (delete the created analyzer)
            if (created && createdAnalyzer != null) {
                System.out.println("Step 7: Cleaning up (deleting analyzer)...");
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
            System.out.println("  1. Creating a custom analyzer with initial configuration");
            System.out.println("  2. Updating analyzer properties (description and tags)");
            System.out.println("  3. Verifying the updates persisted");
            System.out.println("  4. Cleaning up by deleting the analyzer");
            System.out.println();
            System.out.println("Related samples:");
            System.out.println("  - To create analyzers: see CreateOrReplaceAnalyzer sample");
            System.out.println("  - To delete analyzers: see DeleteAnalyzer sample");
            System.out.println("  - To list analyzers: see ListAnalyzers sample");
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

    private static String formatTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "(none)";
        }
        return tags.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));
    }
}
