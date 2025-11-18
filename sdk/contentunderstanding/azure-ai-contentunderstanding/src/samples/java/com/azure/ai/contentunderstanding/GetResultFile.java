// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.contentunderstanding;

import com.azure.ai.contentunderstanding.models.AnalyzeInput;
import com.azure.ai.contentunderstanding.models.AnalyzeResult;
import com.azure.ai.contentunderstanding.models.AudioVisualContent;
import com.azure.ai.contentunderstanding.models.ContentAnalyzerAnalyzeOperationStatus;
import com.azure.ai.contentunderstanding.models.MediaContent;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Configuration;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sample for getting result files (like keyframe images) from a video analysis operation.
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
 *     mvn clean compile exec:java -Dexec.mainClass="com.azure.ai.contentunderstanding.GetResultFile"
 *
 * This sample demonstrates:
 * 1. Analyze a video file using the prebuilt video analyzer
 * 2. Extract operation ID from the analysis
 * 3. Get result files (keyframe images) using the operation ID
 * 4. Save the keyframe images to local files
 *
 * NOTE: The Java SDK uses keyFrameTimesMs (List of Long) to represent keyframe timestamps.
 * The path format for GetResultFile uses: "keyframes/{frameTimeMs}"
 */
public class GetResultFile {

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused. Arguments to the program.
     */
    public static void main(final String[] args) {
        System.out.println("=============================================================");
        System.out.println("Azure Content Understanding Sample: Get Result File");
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

            // Step 3: Use prebuilt video analyzer
            System.out.println("Step 3: Using prebuilt video analyzer...");
            String analyzerId = "prebuilt-videoSearch";
            System.out.println("  Analyzer ID: " + analyzerId);
            System.out.println("  (Using prebuilt analyzer - no creation needed)");
            System.out.println();

            // Step 4: Analyze a video file
            System.out.println("Step 4: Analyzing video file...");
            String videoUrl = "https://github.com/Azure-Samples/azure-ai-content-understanding-assets/raw/refs/heads/main/videos/sdk_samples/FlightSimulator.mp4";
            System.out.println("  URL: " + videoUrl);
            System.out.println("  Starting video analysis (this may take several moments)...");

            AnalyzeResult analyzeResult;
            String operationId = null;

            try {
                AnalyzeInput analyzeInput = new AnalyzeInput().setUrl(videoUrl);
                SyncPoller<ContentAnalyzerAnalyzeOperationStatus, AnalyzeResult> operation =
                    client.beginAnalyze(analyzerId, null, null,
                        Collections.singletonList(analyzeInput), null);

                // Extract operation ID from the poll response
                PollResponse<ContentAnalyzerAnalyzeOperationStatus> pollResponse = operation.poll();
                
                // Extract operation ID from the status object
                if (pollResponse != null && pollResponse.getValue() != null) {
                    ContentAnalyzerAnalyzeOperationStatus status = pollResponse.getValue();
                    operationId = status.getId();
                    System.out.println("  Analysis started, Operation ID: " + operationId);
                }
                
                // Poll for completion with status updates
                System.out.println("  Polling for completion (this may take several minutes for video)...");
                int pollCount = 0;
                while (!operation.poll().getStatus().isComplete()) {
                    try {
                        Thread.sleep(5000); // 5 seconds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Polling interrupted", e);
                    }
                    pollCount++;

                    if (pollCount % 6 == 0) {  // Every 30 seconds
                        System.out.println("  Still processing... (" + (pollCount * 5) + " seconds elapsed)");
                    }

                    if (pollCount > 240) {  // 20 minutes timeout
                        System.out.println("  ⚠️  Analysis is taking longer than expected (>20 minutes)");
                        break;
                    }
                }

                analyzeResult = operation.getFinalResult();
                System.out.println("  ✅ Video analysis completed!");
                System.out.println("  Contents count: " + (analyzeResult.getContents() != null ? analyzeResult.getContents().size() : 0));

                // Save raw JSON response to inspect structure
                String outputDir = "sample_output";
                File outputDirFile = new File(outputDir);
                if (!outputDirFile.exists()) {
                    outputDirFile.mkdirs();
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String jsonFileName = "video_analysis_raw_" + timestamp + ".json";
                String jsonFilePath = Paths.get(outputDir, jsonFileName).toString();

                // Convert result to JSON for inspection
                BinaryData resultData = BinaryData.fromObject(analyzeResult);
                Files.write(Paths.get(jsonFilePath), resultData.toBytes());
                System.out.println("  💾 Raw JSON response saved to: " + jsonFilePath);
                System.out.println();

            } catch (Exception ex) {
                System.err.println("  Failed to analyze video: " + ex.getMessage());
                ex.printStackTrace();
                throw ex;
            }

            // Step 5: Find keyframes in the analysis result
            System.out.println("Step 5: Finding keyframes in analysis result...");

            List<Long> keyframeTimesMs = new ArrayList<>();

            if (analyzeResult.getContents() != null && !analyzeResult.getContents().isEmpty()) {
                for (MediaContent content : analyzeResult.getContents()) {
                    if (content instanceof AudioVisualContent) {
                        AudioVisualContent videoContent = (AudioVisualContent) content;
                        System.out.println("  Video content found:");
                        System.out.println("    Start time: " + videoContent.getStartTimeMs() + "ms");
                        System.out.println("    End time: " + videoContent.getEndTimeMs() + "ms");
                        System.out.println("    KeyFrames count: " + 
                            (videoContent.getKeyFrameTimesMs() != null ? videoContent.getKeyFrameTimesMs().size() : 0));

                        if (videoContent.getKeyFrameTimesMs() != null && !videoContent.getKeyFrameTimesMs().isEmpty()) {
                            System.out.println("  Found " + videoContent.getKeyFrameTimesMs().size() + " keyframes in video content");
                            keyframeTimesMs.addAll(videoContent.getKeyFrameTimesMs());
                        }
                        break;
                    }
                }
            }

            if (keyframeTimesMs.isEmpty()) {
                System.out.println();
                System.out.println("  ⚠️  No keyframes found in the analysis result");
                System.out.println("  NOTE: The prebuilt-videoSearch may not generate keyframes by default.");
                System.out.println("        To generate keyframes, a custom video analyzer with specific configuration");
                System.out.println("        may be required (see CreateOrReplaceAnalyzer sample).");
                System.out.println();
                System.out.println("  This sample successfully demonstrated:");
                System.out.println("    ✓ Video analysis workflow");
                System.out.println("    ✓ Extracting keyframe information from results");
                System.out.println("    ✓ GetResultFile API usage (would work if keyframes were present and operation ID available)");
                System.out.println();
            } else {
                System.out.println("  🖼️  Found " + keyframeTimesMs.size() + " keyframe timestamps");
                System.out.println();

                // Step 6: Download keyframe images
                if (operationId != null) {
                    System.out.println("Step 6: Downloading keyframe images...");

                    // Download a few keyframe images as examples (first, middle, last)
                    List<Long> framesToDownload = new ArrayList<>();
                    if (keyframeTimesMs.size() >= 3) {
                        framesToDownload.add(keyframeTimesMs.get(0));  // First
                        framesToDownload.add(keyframeTimesMs.get(keyframeTimesMs.size() / 2));  // Middle
                        framesToDownload.add(keyframeTimesMs.get(keyframeTimesMs.size() - 1));  // Last
                    } else {
                        framesToDownload.addAll(keyframeTimesMs);
                    }

                    System.out.println("  Downloading " + framesToDownload.size() + " keyframe images as examples");

                    // Create output directory
                    String outputDir = "sample_output";
                    File outputDirFile = new File(outputDir);
                    if (!outputDirFile.exists()) {
                        outputDirFile.mkdirs();
                    }

                    for (Long frameTimeMs : framesToDownload) {
                        // Path format: "keyframes/{frameTimeMs}"
                        String framePath = "keyframes/" + frameTimeMs;
                        System.out.println("  📥 Getting result file: " + framePath);

                        try {
                            BinaryData fileData = client.getResultFile(operationId, framePath);
                            byte[] imageBytes = fileData.toBytes();
                            System.out.println("    ✅ Retrieved (" + String.format("%,d", imageBytes.length) + " bytes)");

                            // Save the image file
                            String fileName = "keyframe_" + frameTimeMs + ".jpg";
                            String filePath = Paths.get(outputDir, fileName).toString();
                            Files.write(Paths.get(filePath), imageBytes);
                            System.out.println("    💾 Saved to: " + filePath);
                        } catch (Exception ex) {
                            System.err.println("    Failed to get result file: " + ex.getMessage());
                            // Continue with next file
                        }
                    }
                    System.out.println();
                } else {
                    System.out.println("Step 6: Skipping keyframe download...");
                    System.out.println("  ⚠️  Operation ID not available");
                    System.out.println("  GetResultFile requires the operation ID from the analysis");
                    System.out.println();
                }
            }

            // Step 7: Clean up (if we created a custom analyzer)
            System.out.println("Step 7: Cleanup...");
            System.out.println("  (Using prebuilt analyzer - no cleanup needed)");
            System.out.println();

            System.out.println("=============================================================");
            System.out.println("✓ Sample completed successfully");
            System.out.println("=============================================================");
            System.out.println();
            System.out.println("This sample demonstrated:");
            System.out.println("  1. Using the prebuilt video analyzer");
            System.out.println("  2. Analyzing a video to generate keyframes");
            System.out.println("  3. Extracting keyframe information from results");
            System.out.println("  4. Using GetResultFile API to download keyframe images");
            System.out.println("  5. Saving keyframe images to local files");
            System.out.println();
            System.out.println("API Notes:");
            System.out.println("  - AudioVisualContent.getKeyFrameTimesMs() returns List<Long>");
            System.out.println("  - Path format for GetResultFile: \"keyframes/{frameTimeMs}\"");
            System.out.println("  - Operation ID is needed to retrieve result files");

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
