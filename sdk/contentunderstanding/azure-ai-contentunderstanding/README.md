# Azure ContentUnderstanding client library for Java

Azure ContentUnderstanding client library for Java.

This package contains Microsoft Azure ContentUnderstanding client library.

## Documentation

Various documentation is available to help you get started

- [API reference documentation][docs]
- [Product documentation][product_documentation]

## Getting started

### Prerequisites

- [Java Development Kit (JDK)][jdk] with version 8 or above
- [Azure Subscription][azure_subscription]

### Adding the package to your product

[//]: # ({x-version-update-start;com.azure:azure-ai-contentunderstanding;current})
```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-ai-contentunderstanding</artifactId>
    <version>1.0.0-beta.1</version>
</dependency>
```
[//]: # ({x-version-update-end})

### Authentication

[Azure Identity][azure_identity] package provides the default implementation for authenticating the client.

## Key concepts

## Examples

### Analyze Binary Document

The [AnalyzeBinary][sample_analyze_binary] sample demonstrates how to analyze a PDF document using the Content Understanding SDK with the object model approach.

### Analyze Document from URL

The [AnalyzeUrl][sample_analyze_url] sample demonstrates how to analyze a document from a URL using the Content Understanding SDK with the prebuilt-documentSearch analyzer.

### Analyze Binary Document (Raw JSON)

The [AnalyzeBinaryRawJson][sample_analyze_binary_raw_json] sample demonstrates how to analyze a PDF document and access the raw JSON response. This approach is for demonstration purposes only - for production use, prefer the object model approach shown in AnalyzeBinary.

### Analyze Invoice from URL with Prebuilt Invoice Analyzer

The [AnalyzeUrlPrebuiltInvoice][sample_analyze_url_prebuilt_invoice] sample demonstrates how to analyze an invoice from a URL using the prebuilt-invoice analyzer. This sample shows how to extract structured fields from invoices including customer information, line items, and totals.

### Create or Replace Custom Analyzer

The [CreateOrReplaceAnalyzer][sample_create_or_replace_analyzer] sample demonstrates how to create a custom analyzer with field schema using the CreateOrReplace API. This sample shows how to define custom fields, create an analyzer, use it to analyze a document, and clean up by deleting the analyzer.

### Delete Custom Analyzer

The [DeleteAnalyzer][sample_delete_analyzer] sample demonstrates how to delete a custom analyzer using the Delete API. This sample shows how to create a temporary analyzer and then delete it using the delete operation.

### Get Analyzer

The [GetAnalyzer][sample_get_analyzer] sample demonstrates how to retrieve an analyzer using the Get API. This sample shows how to create a temporary analyzer, retrieve it to display its properties and configuration, and clean up by deleting the analyzer.

### Get Result File

The [GetResultFile][sample_get_result_file] sample demonstrates how to get result files (like keyframe images) from a video analysis operation. This sample shows how to analyze a video file, extract the operation ID, retrieve keyframe images using the GetResultFile API, and save them to local files.

### List Analyzers

The [ListAnalyzers][sample_list_analyzers] sample demonstrates how to list all available content analyzers in a Content Understanding resource. This sample shows how to retrieve both prebuilt and custom analyzers, display their properties, and categorize them by type.

### Update Analyzer

The [UpdateAnalyzer][sample_update_analyzer] sample demonstrates how to update a custom analyzer using the Update API. This sample shows how to create an analyzer, update its properties (description and tags), verify the changes persisted, and clean up by deleting the analyzer.

```java com.azure.ai.contentunderstanding.readme
```

### Service API versions

The client library targets the latest service API version by default.
The service client builder accepts an optional service API version parameter to specify which API version to communicate.

#### Select a service API version

You have the flexibility to explicitly select a supported service API version when initializing a service client via the service client builder.
This ensures that the client can communicate with services using the specified API version.

When selecting an API version, it is important to verify that there are no breaking changes compared to the latest API version.
If there are significant differences, API calls may fail due to incompatibility.

Always ensure that the chosen API version is fully supported and operational for your specific use case and that it aligns with the service's versioning policy.

## Troubleshooting

## Next steps

## Contributing

For details on contributing to this repository, see the [contributing guide](https://github.com/Azure/azure-sdk-for-java/blob/main/CONTRIBUTING.md).

1. Fork it
1. Create your feature branch (`git checkout -b my-new-feature`)
1. Commit your changes (`git commit -am 'Add some feature'`)
1. Push to the branch (`git push origin my-new-feature`)
1. Create new Pull Request

<!-- LINKS -->
[product_documentation]: https://azure.microsoft.com/services/
[docs]: https://azure.github.io/azure-sdk-for-java/
[jdk]: https://learn.microsoft.com/azure/developer/java/fundamentals/
[azure_subscription]: https://azure.microsoft.com/free/
[azure_identity]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/identity/azure-identity
[sample_analyze_binary]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/AnalyzeBinary.java
[sample_analyze_url]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/AnalyzeUrl.java
[sample_analyze_binary_raw_json]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/AnalyzeBinaryRawJson.java
[sample_analyze_url_prebuilt_invoice]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/AnalyzeUrlPrebuiltInvoice.java
[sample_create_or_replace_analyzer]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/CreateOrReplaceAnalyzer.java
[sample_delete_analyzer]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/DeleteAnalyzer.java
[sample_get_analyzer]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/GetAnalyzer.java
[sample_get_result_file]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/GetResultFile.java
[sample_list_analyzers]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/ListAnalyzers.java
[sample_update_analyzer]: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/contentunderstanding/azure-ai-contentunderstanding/src/samples/java/com/azure/ai/contentunderstanding/UpdateAnalyzer.java
