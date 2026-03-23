# End-to-End BPMN Testing with Testcontainers

## Overview

This guide explains how to implement comprehensive end-to-end (E2E) BPMN process testing using **Camunda's process-test-spring** library with **Testcontainers** before deploying process definitions to an actual Camunda 8 engine.

Instead of manually testing BPMN workflows through the UI or relying on integration tests against live engines, testcontainer-based E2E testing allows you to:
- Automatically validate process flows in isolated test environments
- Catch deployment errors early in the development cycle
- Verify variable flows, conditional logic, and error handling
- Ensure process modifications don't break existing workflows

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  JUnit 5 Test Suite (@CamundaSpringProcessTest)        │
├─────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────┐  │
│  │  Spring Boot Test Context (ActiveProfiles)        │  │
│  │  - Loads test-specific application properties     │  │
│  │  - Disables production worker registration        │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Testcontainer (Camunda 8 Docker Image)           │  │
│  │  - Managed runtime (MANAGED mode)                 │  │
│  │  - Zeebe gRPC gateway + REST API                  │  │
│  │  - Isolated from production                       │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  CamundaClient + Job Worker Mocks                 │  │
│  │  - Deploy BPMN definitions                        │  │
│  │  - Start process instances                        │  │
│  │  - Mock external job workers                      │  │
│  │  - Assert process state                           │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Setup & Configuration

### 1. Maven Dependencies

Ensure your `pom.xml` includes:

```xml
<dependency>
    <groupId>io.camunda</groupId>
    <artifactId>camunda-process-test-spring</artifactId>
    <version>8.8.18</version>
    <scope>test</scope>
</dependency>
```

### 2. Test Profile Configuration

Create `src/test/resources/application-test.yaml`:

```yaml
camunda:
  process-test:
    runtime-mode: MANAGED                 # Managed testcontainer lifecycle
    camunda-docker-image-version: 8.8.0   # Camunda version to test against
    multi-tenancy-enabled: false
```

**Key Configuration Points:**
- `runtime-mode: MANAGED` — Testcontainer manages Camunda instance startup/shutdown
- `camunda-docker-image-version` — Pin to tested Camunda version (not latest)

---

## Writing BPMN Tests

### Test Class Setup

```java
@SpringBootTest
@CamundaSpringProcessTest
@ActiveProfiles("test")  // Loads application-test.yaml
class OrderProcessTest {

    @Autowired
    private CamundaClient client;

    @Autowired
    private CamundaProcessTestContext testContext;

    private void deployOrderProcess() {
        client.newDeployResourceCommand()
                .addResourceFromClasspath("order-processing.bpmn")
                .send()
                .join();
    }
    // ... test methods ...
}
```

**Annotations:**
- `@SpringBootTest` — Loads Spring application context
- `@CamundaSpringProcessTest` — Provisions Testcontainer + Camunda client
- `@ActiveProfiles("test")` — Applies test configuration

### Example: Success Path Test

```java
@Test
void shouldCompleteOrderProcessingSuccessPath() {
    // 1. Deploy the BPMN model
    deployOrderProcess();

    // 2. Create a process instance
    ProcessInstanceEvent instance = client.newCreateInstanceCommand()
            .bpmnProcessId("OrderProcessing")
            .latestVersion()
            .variables(Map.of(
                "customerId", "CUST-123",
                "orderAmount", 99.99
            ))
            .send()
            .join();

    // 3. Assert process started
    assertThat(instance).isCreated();

    // 4. Mock external service tasks (or job workers)
    testContext.mockJobWorker("checkOrderInformation")
            .thenComplete(Map.of("orderValid", true));

    testContext.mockJobWorker("orderQueue")
            .thenComplete();

    testContext.mockJobWorker("packingQueue")
            .thenComplete(Map.of("isQualityPassed", true));

    testContext.mockJobWorker("initiateDelivery")
            .thenComplete();

    // 5. Assert process completed successfully
    assertThat(instance).isCompleted();
}
```

### Example: Error/Branch Path Test

```java
@Test
void shouldFailOrderProcessingOnQualityCheck() {
    deployOrderProcess();

    ProcessInstanceEvent instance = client.newCreateInstanceCommand()
            .bpmnProcessId("OrderProcessing")
            .latestVersion()
            .send()
            .join();

    // Setup early steps
    testContext.mockJobWorker("checkOrderInformation")
            .thenComplete();
    testContext.mockJobWorker("orderQueue")
            .thenComplete();

    // Simulate quality failure — conditional gateway should route to end
    testContext.mockJobWorker("packingQueue")
            .thenComplete(Map.of("isQualityPassed", false));

    // Verify process ends without calling "initiateDelivery"
    assertThat(instance).isCompleted();
    // Optional: verify state to ensure correct branch was taken
}
```

### Testing Variable Flow

```java
@Test
void shouldPropagateVariablesToDownstream() {
    deployOrderProcess();

    ProcessInstanceEvent instance = client.newCreateInstanceCommand()
            .bpmnProcessId("OrderProcessing")
            .latestVersion()
            .variables(Map.of(
                "priority", "HIGH",
                "shippingAddress", "123 Main St"
            ))
            .send()
            .join();

    testContext.mockJobWorker("orderQueue")
            .thenComplete(Map.of(
                "shippingCost", 15.99,
                "estimatedDelivery", "2026-03-30"
            ));

    // Verify downstream tasks receive merged variables
    assertThat(instance).isActive(); // Can inspect via REST if needed
}
```

---

## Best Practices

### 1. **One Deployment Per Test File (or Use `@BeforeEach`)**

```java
@BeforeEach
void setupOrderProcess() {
    deployOrderProcess();
    // This runs before each test method
}

@Test
void testScenario1() { /* ... */ }

@Test
void testScenario2() { /* ... */ }
```

Alternatively, deploy once and reuse:

```java
private static ProcessDefinition orderProcessDef;

@BeforeAll
static void deployAll() {
    // Deploy all BPMN once per test class
}
```

### 2. **Test Naming Convention**

Use descriptive names that clarify the scenario:

```java
@Test
void shouldCompleteOrderProcessingSuccessPath() { }

@Test
void shouldFailOrderProcessingOnQualityCheckFailure() { }

@Test
void shouldTimeoutIfDeliveryNotInitiatedWithin24Hours() { }

@Test
void shouldPropagateCustomerDetailsToAllDownstreamTasks() { }
```

### 3. **Separate Happy/Sad/Edge Cases**

```java
class OrderProcessTest {
    // ✅ Happy path
    @Test
    void testSuccessScenario() { }

    // ❌ Sad path / expected failures
    @Test
    void testQualityCheckFailure() { }

    // ⚠️ Edge cases
    @Test
    void testLargeOrderAmount() { }

    @Test
    void testZeroQuantity() { }
}
```

### 4. **Use Assertions from CamundaAssert**

```java
// ✅ Good
assertThat(instance).isCreated();
assertThat(instance).isActive();
assertThat(instance).isCompleted();

// ❌ Avoid generic assertions
assertTrue(instance != null);
```
---


## Example: Complete Test Suite

```java
@SpringBootTest
@CamundaSpringProcessTest
@ActiveProfiles("test")
class OrderProcessTest {

    @Autowired
    private CamundaClient client;

    @Autowired
    private CamundaProcessTestContext testContext;

    @BeforeEach
    void setUp() {
        client.newDeployResourceCommand()
                .addResourceFromClasspath("order-processing.bpmn")
                .send()
                .join();
    }

    @Test
    void shouldCompleteOrderProcessingSuccessPath() {
        ProcessInstanceEvent instance = client.newCreateInstanceCommand()
                .bpmnProcessId("OrderProcessing")
                .latestVersion()
                .variables(Map.of(
                    "customerId", "CUST-001",
                    "orderAmount", 150.00
                ))
                .send()
                .join();

        assertThat(instance).isCreated();

        testContext.mockJobWorker("checkOrderInformation")
                .thenComplete(Map.of("orderValid", true));

        testContext.mockJobWorker("orderQueue")
                .thenComplete();

        testContext.mockJobWorker("packingQueue")
                .thenComplete(Map.of("isQualityPassed", true));

        testContext.mockJobWorker("initiateDelivery")
                .thenComplete();

        assertThat(instance).isCompleted();
    }

    @Test
    void shouldFailOrderProcessingOnQualityCheckFailure() {
        ProcessInstanceEvent instance = client.newCreateInstanceCommand()
                .bpmnProcessId("OrderProcessing")
                .latestVersion()
                .send()
                .join();

        testContext.mockJobWorker("checkOrderInformation")
                .thenComplete();
        testContext.mockJobWorker("orderQueue")
                .thenComplete();
        testContext.mockJobWorker("packingQueue")
                .thenComplete(Map.of("isQualityPassed", false));

        assertThat(instance).isCompleted();
    }
}
```

---

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Test BPMN Definitions
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run BPMN tests
        run: mvn -Dtest=*ProcessTest test
      - name: Upload coverage
        uses: codecov/codecov-action@v2
        if: always()
```

---

## Summary

TestContainer-based E2E BPMN testing provides:

1. ✅ **Fast feedback** on BPMN logic without manual testing
2. ✅ **Regression prevention** when processes are modified
3. ✅ **Isolated environments** that don't interfere with dev/staging/prod
4. ✅ **Deterministic behavior** via mocked external services
5. ✅ **Living documentation** that demonstrates process flows
6. ✅ **CI/CD integration** for automated validation on every commit
7. ✅ **Cost savings** by reducing manual testing and production incidents

By following the patterns in this guide, you can confidently deploy BPMN process definitions with high confidence that they will work as intended in production.

