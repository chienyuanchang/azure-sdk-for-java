// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeInput;
import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.ai.contentunderstanding.models.ArrayField;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerAnalyzeOperationStatus;
import com.azure.ai.contentunderstanding.models.ContentField;
import com.azure.ai.contentunderstanding.models.MediaContent;
import com.azure.ai.contentunderstanding.models.NumberField;
import com.azure.ai.contentunderstanding.models.ObjectField;
import com.azure.ai.contentunderstanding.models.StringField;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Configuration;
import com.azure.core.util.polling.SyncPoller;
import com.azure.json.JsonProviders;
import com.azure.json.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Sample demonstrating how to analyze an invoice from a URL using the prebuilt-invoice analyzer.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.AnalyzeUrlPrebuiltInvoice"
 *
 * This sample demonstrates:
 * 1. Authenticate with Azure AI Content Understanding
 * 2. Analyze an invoice from a remote URL using the prebuilt-invoice analyzer
 * 3. Save the complete analysis result to JSON file
 * 4. Show examples of extracting different field types (string, number, object, array)
 */
public class AnalyzeUrlPrebuiltInvoice {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Prebuilt Invoice");
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

            // Step 3: Analyze invoice
            analyzeInvoice(client);

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

    /**
     * Analyzes an invoice from a URL using the prebuilt-invoice analyzer.
     *
     * @param client The Content Understanding client.
     */
    private static void analyzeInvoice(ContentUnderstandingClient client) {
        String fileUrl = "https://github.com/Azure-Samples/azure-ai-content-understanding-python/raw/refs/heads/main/data/invoice.pdf";

        System.out.println("Step 3: Analyzing invoice from URL...");
        System.out.println("  URL: " + fileUrl);
        System.out.println("  Analyzer: prebuilt-invoice");
        System.out.println("  Analyzing...");

        AnalyzeResult result;
        try {
            AnalyzeInput analyzeInput = new AnalyzeInput().setUrl(fileUrl);
            SyncPoller<ContentAnalyzerAnalyzeOperationStatus, AnalyzeResult> operation =
                client.beginAnalyze("prebuilt-invoice", null, null,
                    Collections.singletonList(analyzeInput), null);
            result = operation.getFinalResult();
            System.out.println("  Analysis completed successfully");
            System.out.println();
        } catch (Exception ex) {
            System.err.println("  Failed to analyze invoice: " + ex.getMessage());
            throw new RuntimeException(ex);
        }

        System.out.println("Step 4: Displaying invoice analysis result...");
        System.out.println("=============================================================");

        // A PDF file has only one content element even if it contains multiple pages
        if (result.getContents() == null || result.getContents().isEmpty()) {
            System.out.println("(No content returned from analysis)");
            return;
        }

        MediaContent content = result.getContents().get(0);

        if (content.getFields() == null || content.getFields().isEmpty()) {
            System.out.println("No fields found in the analysis result");
            return;
        }

        System.out.println();
        System.out.println("📋 Sample Field Extractions:");
        System.out.println("----------------------------------------");

        // Example 1: Simple string fields
        String customerName = getStringFieldValue(content.getFields(), "CustomerName");
        String invoiceDate = getStringFieldValue(content.getFields(), "InvoiceDate");

        System.out.println("Customer Name: " + (customerName != null ? customerName : "(None)"));
        System.out.println("Invoice Date: " + (invoiceDate != null ? invoiceDate : "(None)"));

        // Example 1b: Currency field (TotalAmount is an object with Amount and CurrencyCode)
        ContentField totalAmountField = content.getFields().get("TotalAmount");
        if (totalAmountField instanceof ObjectField) {
            ObjectField totalAmountObj = (ObjectField) totalAmountField;
            Map<String, ContentField> totalAmountFields = totalAmountObj.getValueObject();
            Double amount = getNumberFieldValue(totalAmountFields, "Amount");
            String currency = getStringFieldValue(totalAmountFields, "CurrencyCode");
            System.out.println("Invoice Total: " + (currency != null ? currency : "$") + 
                (amount != null ? String.format("%.2f", amount) : "(None)"));
        } else {
            System.out.println("Invoice Total: (Not found)");
        }

        // Example 2: Array field (LineItems)
        System.out.println();
        System.out.println("🛒 Invoice Line Items (Array):");
        ContentField itemsField = content.getFields().get("LineItems");
        if (itemsField instanceof ArrayField) {
            ArrayField arrayField = (ArrayField) itemsField;
            List<ContentField> items = arrayField.getValueArray();
            if (items != null && !items.isEmpty()) {
                for (int i = 0; i < items.size(); i++) {
                    ContentField item = items.get(i);
                    if (item instanceof ObjectField) {
                        ObjectField objectField = (ObjectField) item;
                        Map<String, ContentField> itemFields = objectField.getValueObject();
                        if (itemFields != null) {
                            System.out.println("  Item " + (i + 1) + ":");

                            // Extract common item fields
                            String description = getStringFieldValue(itemFields, "Description");
                            Double quantity = getNumberFieldValue(itemFields, "Quantity");

                            System.out.println("    Description: " + (description != null ? description : "N/A"));
                            System.out.println("    Quantity: " + (quantity != null ? quantity.toString() : "N/A"));

                            // UnitPrice and Amount are currency objects with Amount and CurrencyCode sub-fields
                            ContentField unitPriceField = itemFields.get("UnitPrice");
                            if (unitPriceField instanceof ObjectField) {
                                ObjectField unitPriceObj = (ObjectField) unitPriceField;
                                Map<String, ContentField> unitPriceFields = unitPriceObj.getValueObject();
                                Double unitAmount = getNumberFieldValue(unitPriceFields, "Amount");
                                String unitCurrency = getStringFieldValue(unitPriceFields, "CurrencyCode");
                                System.out.println("    Unit Price: " + (unitCurrency != null ? unitCurrency : "$") + 
                                    (unitAmount != null ? String.format("%.2f", unitAmount) : "N/A"));
                            }

                            ContentField amountField = itemFields.get("Amount");
                            if (amountField instanceof ObjectField) {
                                ObjectField amountObj = (ObjectField) amountField;
                                Map<String, ContentField> amountFields = amountObj.getValueObject();
                                Double itemAmount = getNumberFieldValue(amountFields, "Amount");
                                String itemCurrency = getStringFieldValue(amountFields, "CurrencyCode");
                                System.out.println("    Total Price: " + (itemCurrency != null ? itemCurrency : "$") + 
                                    (itemAmount != null ? String.format("%.2f", itemAmount) : "N/A"));
                            }
                        }
                    } else {
                        System.out.println("  Item " + (i + 1) + ": No item object found");
                    }
                }
            } else {
                System.out.println("  No items found");
            }
        } else {
            System.out.println("  No items found");
        }

        System.out.println();
        System.out.println("📄 Total fields extracted: " + content.getFields().size());

        // Save the full result to JSON for detailed inspection
        System.out.println();
        System.out.println("Step 5: Saving analysis result to JSON...");
        saveResultToJson(result, "content_analyzers_analyze_url_prebuilt_invoice");
        System.out.println("✅ Invoice fields saved to JSON file for detailed inspection");
        System.out.println();
    }

    /**
     * Helper method to extract string value from a ContentField.
     *
     * @param fields The map of fields.
     * @param fieldName The name of the field to extract.
     * @return The extracted string value or null if not found.
     */
    private static String getStringFieldValue(Map<String, ContentField> fields, String fieldName) {
        if (fields == null || !fields.containsKey(fieldName)) {
            return null;
        }
        ContentField field = fields.get(fieldName);
        if (field instanceof StringField) {
            return ((StringField) field).getValueString();
        }
        return null;
    }

    /**
     * Helper method to extract number value from a ContentField.
     *
     * @param fields The map of fields.
     * @param fieldName The name of the field to extract.
     * @return The extracted number value or null if not found.
     */
    private static Double getNumberFieldValue(Map<String, ContentField> fields, String fieldName) {
        if (fields == null || !fields.containsKey(fieldName)) {
            return null;
        }
        ContentField field = fields.get(fieldName);
        if (field instanceof NumberField) {
            return ((NumberField) field).getValueNumber();
        }
        return null;
    }

    /**
     * Save the analysis result to a JSON file.
     *
     * @param result The analysis result.
     * @param filenamePrefix The prefix for the output filename.
     */
    private static void saveResultToJson(AnalyzeResult result, String filenamePrefix) {
        String outputDir = "sample_output";
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = filenamePrefix + "_" + timestamp + ".json";
        String outputPath = new File(outputDir, filename).getPath();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             JsonWriter jsonWriter = JsonProviders.createWriter(outputStream)) {
            
            result.toJson(jsonWriter);
            jsonWriter.flush();
            
            String json = outputStream.toString(StandardCharsets.UTF_8.name());
            
            // Write to file
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
                fileOutputStream.write(json.getBytes(StandardCharsets.UTF_8));
            }
            
            System.out.println("  💾 Analysis result saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("  Failed to save result to JSON: " + e.getMessage());
        }
    }
}
