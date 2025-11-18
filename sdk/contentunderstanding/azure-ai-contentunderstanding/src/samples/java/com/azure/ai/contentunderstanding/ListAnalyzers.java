// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.ContentAnalyzer;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sample for listing all available content analyzers.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.ListAnalyzers"
 */
public class ListAnalyzers {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: List Analyzers");
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
            System.out.println("  Client created successfully");
            System.out.println();

            // Step 3: List all available analyzers
            System.out.println("Step 3: Listing all available analyzers...");
            List<ContentAnalyzer> analyzers = new ArrayList<>();

            try {
                PagedIterable<ContentAnalyzer> pagedAnalyzers = client.list();
                for (ContentAnalyzer analyzer : pagedAnalyzers) {
                    analyzers.add(analyzer);
                }

                System.out.println("  Found " + analyzers.size() + " analyzer(s)");
            } catch (HttpResponseException ex) {
                System.err.println("  Failed to list analyzers: " + ex.getMessage());
                System.err.println("  Status: " + ex.getResponse().getStatusCode());
                throw ex;
            } catch (Exception ex) {
                System.err.println("  Unexpected error while listing analyzers: " + ex.getClass().getName());
                System.err.println("  Message: " + ex.getMessage());
                if (ex.getCause() != null) {
                    System.err.println("  Cause: " + ex.getCause().getMessage());
                }
                throw ex;
            }

            System.out.println();

            // Step 4: Display summary
            System.out.println("Step 4: Summary...");
            System.out.println("  Total analyzers: " + analyzers.size());

            long prebuiltCount = analyzers.stream()
                .filter(a -> a.getAnalyzerId() != null && a.getAnalyzerId().startsWith("prebuilt-"))
                .count();
            long customCount = analyzers.stream()
                .filter(a -> a.getAnalyzerId() == null || !a.getAnalyzerId().startsWith("prebuilt-"))
                .count();
            System.out.println("  Prebuilt analyzers: " + prebuiltCount);
            System.out.println("  Custom analyzers: " + customCount);
            System.out.println();

            // Step 5: Display detailed information about each analyzer
            if (!analyzers.isEmpty()) {
                System.out.println("Step 5: Displaying analyzer details...");
                System.out.println("=============================================================");
                System.out.println();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < analyzers.size(); i++) {
                    ContentAnalyzer analyzer = analyzers.get(i);
                    System.out.println("Analyzer " + (i + 1) + ":");
                    System.out.println("  ID: " + analyzer.getAnalyzerId());
                    System.out.println("  Description: " + (analyzer.getDescription() != null ? analyzer.getDescription() : "(none)"));
                    System.out.println("  Status: " + analyzer.getStatus());

                    if (analyzer.getCreatedAt() != null) {
                        System.out.println("  Created at: " + analyzer.getCreatedAt().format(formatter) + " UTC");
                    }
                    if (analyzer.getLastModifiedAt() != null) {
                        System.out.println("  Last modified: " + analyzer.getLastModifiedAt().format(formatter) + " UTC");
                    }

                    // Check if it's a prebuilt analyzer
                    if (analyzer.getAnalyzerId() != null && analyzer.getAnalyzerId().startsWith("prebuilt-")) {
                        System.out.println("  Type: Prebuilt analyzer");
                    } else {
                        System.out.println("  Type: Custom analyzer");
                    }

                    // Show tags if available
                    if (analyzer.getTags() != null && !analyzer.getTags().isEmpty()) {
                        StringBuilder tagsBuilder = new StringBuilder();
                        for (Map.Entry<String, String> tag : analyzer.getTags().entrySet()) {
                            if (tagsBuilder.length() > 0) {
                                tagsBuilder.append(", ");
                            }
                            tagsBuilder.append(tag.getKey()).append("=").append(tag.getValue());
                        }
                        System.out.println("  Tags: " + tagsBuilder.toString());
                    }

                    System.out.println();
                }
            } else {
                System.out.println("No analyzers found in this Content Understanding resource.");
                System.out.println();
            }

            System.out.println("=============================================================");
            System.out.println("✓ Sample completed successfully");
            System.out.println("=============================================================");
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
