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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample for retrieving an analyzer using the Get API.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.GetAnalyzer"
 *
 * This sample demonstrates:
 * 1. Authenticate with Azure AI Content Understanding
 * 2. Create a custom analyzer (for retrieval demo)
 * 3. Retrieve the analyzer using the get API
 * 4. Display the analyzer properties
 * 5. Clean up by deleting the analyzer
 */
public class GetAnalyzer {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Get Analyzer");
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

            // Step 3: Create a temporary analyzer for retrieval demo
            System.out.println("Step 3: Creating temporary analyzer for retrieval demo...");

            // Generate a unique analyzer ID using timestamp
            String analyzerId = "sdk_sample_analyzer_to_retrieve_" + System.currentTimeMillis() / 1000;
            System.out.println("  Analyzer ID: " + analyzerId);

            // Create field schema with a demo field
            Map<String, ContentFieldDefinition> fields = new HashMap<>();
            fields.put("demo_field", new ContentFieldDefinition()
                .setType(ContentFieldType.STRING)
                .setMethod(GenerationMethod.EXTRACT)
                .setDescription("Demo field for retrieval"));

            ContentFieldSchema fieldSchema = new ContentFieldSchema()
                .setName("retrieval_schema")
                .setDescription("Schema for retrieval demo")
                .setFields(fields);

            // Create analyzer configuration
            ContentAnalyzerConfig config = new ContentAnalyzerConfig()
                .setReturnDetails(true);

            // Create the temporary analyzer object
            ContentAnalyzer tempAnalyzer = new ContentAnalyzer()
                .setBaseAnalyzerId("prebuilt-document")
                .setDescription("Custom analyzer for retrieval demo")
                .setConfig(config)
                .setFieldSchema(fieldSchema);

            // Add required model mappings
            Map<String, String> models = new HashMap<>();
            models.put("completion", "gpt-4o-mini");
            models.put("embedding", "text-embedding-3-large");
            tempAnalyzer.setModels(models);

            boolean created = false;
            try {
                System.out.println("  Creating analyzer (this may take a few moments)...");
                SyncPoller<ContentAnalyzerOperationStatus, ContentAnalyzer> operation =
                    client.beginCreateOrReplace(analyzerId, tempAnalyzer);

                operation.getFinalResult();
                created = true;
                System.out.println("  ✅ Analyzer '" + analyzerId + "' created successfully!");
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to create analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 4: Retrieve the analyzer
            System.out.println("Step 4: Retrieving the analyzer...");
            ContentAnalyzer retrievedAnalyzer;
            try {
                retrievedAnalyzer = client.get(analyzerId);
                System.out.println("  ✅ Analyzer '" + analyzerId + "' retrieved successfully!");
                System.out.println();
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to retrieve analyzer: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            }

            // Step 5: Display analyzer properties
            System.out.println("Step 5: Displaying analyzer properties...");
            System.out.println("=============================================================");
            System.out.println("  Analyzer ID: " + retrievedAnalyzer.getAnalyzerId());
            System.out.println("  Description: " + retrievedAnalyzer.getDescription());
            System.out.println("  Status: " + retrievedAnalyzer.getStatus());
            System.out.println("  Base Analyzer: " + retrievedAnalyzer.getBaseAnalyzerId());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            if (retrievedAnalyzer.getCreatedAt() != null) {
                System.out.println("  Created at: " + retrievedAnalyzer.getCreatedAt().format(formatter) + " UTC");
            }
            if (retrievedAnalyzer.getLastModifiedAt() != null) {
                System.out.println("  Last modified: " + retrievedAnalyzer.getLastModifiedAt().format(formatter) + " UTC");
            }

            if (retrievedAnalyzer.getFieldSchema() != null) {
                ContentFieldSchema schema = retrievedAnalyzer.getFieldSchema();
                System.out.println("  Field Schema:");
                System.out.println("    Name: " + schema.getName());
                System.out.println("    Description: " + schema.getDescription());
                System.out.println("    Fields: " + (schema.getFields() != null ? schema.getFields().size() : 0));

                if (schema.getFields() != null) {
                    for (Map.Entry<String, ContentFieldDefinition> field : schema.getFields().entrySet()) {
                        System.out.println("      - " + field.getKey() + ": " + 
                            field.getValue().getType() + " (" + field.getValue().getMethod() + ")");
                    }
                }
            }

            if (retrievedAnalyzer.getModels() != null && !retrievedAnalyzer.getModels().isEmpty()) {
                System.out.println("  Models:");
                for (Map.Entry<String, String> model : retrievedAnalyzer.getModels().entrySet()) {
                    System.out.println("    " + model.getKey() + ": " + model.getValue());
                }
            }

            if (retrievedAnalyzer.getTags() != null && !retrievedAnalyzer.getTags().isEmpty()) {
                StringBuilder tagsBuilder = new StringBuilder();
                for (Map.Entry<String, String> tag : retrievedAnalyzer.getTags().entrySet()) {
                    if (tagsBuilder.length() > 0) {
                        tagsBuilder.append(", ");
                    }
                    tagsBuilder.append(tag.getKey()).append("=").append(tag.getValue());
                }
                System.out.println("  Tags: " + tagsBuilder.toString());
            }

            System.out.println("=============================================================");
            System.out.println();

            // Step 6: Clean up (delete the analyzer)
            if (created) {
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
            System.out.println("  1. Creating a temporary custom analyzer");
            System.out.println("  2. Retrieving the analyzer using the Get API");
            System.out.println("  3. Displaying analyzer properties and configuration");
            System.out.println("  4. Cleaning up by deleting the analyzer");
            System.out.println();
            System.out.println("Related samples:");
            System.out.println("  - To create analyzers: see CreateOrReplaceAnalyzer sample");
            System.out.println("  - To list analyzers: see ListAnalyzers sample");
            System.out.println("  - To delete analyzers: see DeleteAnalyzer sample");
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
