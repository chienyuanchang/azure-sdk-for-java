# Content Understanding SDK Tests

This directory contains unit and integration tests for the Azure Content Understanding Java SDK.

## Quick Start

### 1. Prerequisites
- Java 11 or later
- Maven 3.6+
- Azure subscription with Content Understanding resource (for RECORD/LIVE modes)

### 2. Run Tests with Your Credentials

```bash
# Linux/macOS
export AZURE_CONTENT_UNDERSTANDING_ENDPOINT="https://your-resource.cognitiveservices.azure.com/"
export AZURE_CONTENT_UNDERSTANDING_KEY="your-api-key"
export AZURE_TEST_MODE=RECORD
cd ..
mvn test

# Windows (PowerShell)
$env:AZURE_CONTENT_UNDERSTANDING_ENDPOINT = "https://your-resource.cognitiveservices.azure.com/"
$env:AZURE_CONTENT_UNDERSTANDING_KEY = "your-api-key"
$env:AZURE_TEST_MODE = "RECORD"
cd ..
mvn test
```

### 3. Get Your Credentials

1. Go to [Azure Portal](https://portal.azure.com)
2. Find your Content Understanding resource
3. Go to "Keys and Endpoint" section
4. Copy Endpoint and Key 1

## Test Organization

### Test Classes
- **ContentUnderstandingClientTest.java** - Tests for synchronous client
- **ContentUnderstandingAsyncClientTest.java** - Tests for asynchronous client
- **ContentUnderstandingTestBase.java** - Base class for all tests
- **TestUtils.java** - Test utilities and test data

### Test Data
- **resources/sample_invoice.pdf** - Sample PDF document for testing

## Documentation

- **TEST_EXECUTION_GUIDE.md** - Complete guide for all test modes
- **TEST_STATUS_AND_RESOLUTION.md** - Troubleshooting and setup help
- **TESTING.md** - Testing patterns and architecture

## Test Modes

| Mode | Credentials Needed | CI/CD | Speed |
|------|-------------------|-------|-------|
| PLAYBACK | No | ✓ (with recordings) | Fast |
| RECORD | Yes (API Key) | ✗ | Slow |
| LIVE | Yes (API Key) | ✗ | Slow |

**→ See TEST_EXECUTION_GUIDE.md for detailed instructions**

## Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=ContentUnderstandingClientTest

# Specific test method
mvn test -Dtest=ContentUnderstandingClientTest#analyzeDocumentFromBytes

# With logging
AZURE_LOG_LEVEL=verbose mvn test
```

## Troubleshooting

### "Tenant mismatch" Error
- Check your API key is from the correct Azure subscription
- Use `az login` to verify your Azure CLI identity

### "Status 400" Error  
- Verify endpoint URL is correct (should end with `/`)
- Verify API key hasn't expired
- Check key has Read permissions

### Tests won't run
- Ensure Maven is installed: `mvn -v`
- Check Java version: `java -version` (need 11+)
- Try compiling first: `mvn test-compile`

**→ See TEST_STATUS_AND_RESOLUTION.md for more help**

## Contributing

When adding new tests:
1. Follow existing test patterns in ContentUnderstandingClientTest.java
2. Add test data to TestUtils if needed
3. Update TestUtils.getTestParameters() for new parameter combinations
4. Document test purpose in JavaDoc
5. Run tests with your credentials before committing

## Need Help?

1. Read TEST_EXECUTION_GUIDE.md for setup instructions
2. Check TEST_STATUS_AND_RESOLUTION.md for common issues
3. Review test code comments for implementation details
4. Check [Azure Content Understanding docs](https://learn.microsoft.com/azure/ai-services/document-intelligence/overview)
