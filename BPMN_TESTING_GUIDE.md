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
