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
import com.azure.core.util.Configuration;
import com.azure.core.util.polling.SyncPoller;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample for deleting a custom analyzer using the Delete API.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.DeleteAnalyzer"
 *
 * This sample demonstrates:
 * 1. Authenticate with Azure AI Content Understanding
 * 2. Create a custom analyzer (for deletion demo)
 * 3. Delete the analyzer using the delete API
 */
public class DeleteAnalyzer {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Delete Analyzer");
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

            // Step 3: Create a temporary analyzer for deletion demo
            System.out.println("Step 3: Creating temporary analyzer for deletion demo...");

            // Generate a unique analyzer ID using timestamp
            // Note: Analyzer IDs cannot contain hyphens
            String analyzerId = "sdk_sample_analyzer_to_delete_" + System.currentTimeMillis() / 1000;
            System.out.println("  Analyzer ID: " + analyzerId);

            // Create field schema with a demo field
            Map<String, ContentFieldDefinition> fields = new HashMap<>();
            fields.put("demo_field", new ContentFieldDefinition()
                .setType(ContentFieldType.STRING)
                .setMethod(GenerationMethod.EXTRACT)
                .setDescription("Demo field for deletion"));

            ContentFieldSchema fieldSchema = new ContentFieldSchema()
                .setName("demo_schema")
                .setDescription("Schema for deletion demo")
                .setFields(fields);

            // Create analyzer configuration
            ContentAnalyzerConfig config = new ContentAnalyzerConfig()
                .setReturnDetails(true);

            // Create the temporary analyzer object
            ContentAnalyzer tempAnalyzer = new ContentAnalyzer()
                .setBaseAnalyzerId("prebuilt-document")
                .setDescription("Temporary analyzer for deletion demo")
                .setConfig(config)
                .setFieldSchema(fieldSchema);

            // Add required model mappings
            Map<String, String> models = new HashMap<>();
            models.put("completion", "gpt-4o-mini");
            models.put("embedding", "text-embedding-3-large");
            tempAnalyzer.setModels(models);

            try {
                SyncPoller<ContentAnalyzerOperationStatus, ContentAnalyzer> operation =
                    client.beginCreateOrReplace(analyzerId, tempAnalyzer);

                ContentAnalyzer createdAnalyzer = operation.getFinalResult();
                System.out.println("  ✅ Analyzer '" + analyzerId + "' created successfully!");
                System.out.println("  Status: " + createdAnalyzer.getStatus());
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to create analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 4: Delete the analyzer
            System.out.println("Step 4: Deleting the analyzer...");
            try {
                client.delete(analyzerId);
                System.out.println("  ✅ Analyzer '" + analyzerId + "' deleted successfully!");
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to delete analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            System.out.println("=============================================================");
            System.out.println("✓ Sample completed successfully");
            System.out.println("=============================================================");
            System.out.println();
            System.out.println("This sample demonstrated:");
            System.out.println("  1. Creating a temporary custom analyzer");
            System.out.println("  2. Deleting the analyzer using the Delete API");
            System.out.println();
            System.out.println("Related samples:");
            System.out.println("  - To create analyzers: see CreateOrReplaceAnalyzer sample");
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
}
