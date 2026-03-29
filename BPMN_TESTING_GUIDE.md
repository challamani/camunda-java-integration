# End-to-End BPMN Testing with Testcontainers

## Overview

This guide explains how to implement end-to-end (E2E) BPMN process testing using **Camunda's process-test-spring** library with **Testcontainers** before deploying to a Camunda 8 engine.

Instead of manually testing BPMN workflows through the UI or relying on live-engine integration tests, testcontainer-based E2E testing allows you to:
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
│  │  CamundaClient + Spring Job Workers               │  │
│  │  - Deploy BPMN definitions                        │  │
│  │  - Start process instances                        │  │
│  │  - Workers poll and complete service tasks        │  │
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
    private CamundaProcessTestContext testContext; // Optional for mock-worker scenarios

    private static final String PROCESS_ID = "Process_0rcqim1";
    private static final String MESSAGE_NAME = "Message_Confirmation";

    private void deployOrderProcess() {
        client.newDeployResourceCommand()
                .addResourceFromClasspath("order-process-v2.bpmn")
                .send()
                .join();
    }

    private ProcessInstanceEvent startOrderProcess(String orderId) {
        return client.newCreateInstanceCommand()
                .bpmnProcessId(PROCESS_ID)
                .latestVersion()
                .variables(Map.of("orderId", orderId))
                .send()
                .join();
    }

    private void publishOrderConfirmation(String orderId) {
        client.newPublishMessageCommand()
                .messageName(MESSAGE_NAME)
                .correlationKey(orderId)
                .variables(Map.of("orderId", orderId))
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
    deployOrderProcess();

    ProcessInstanceEvent instance = startOrderProcess("ORDER-001");

    testContext.mockJobWorker("inventoryAllocation")
            .thenComplete(Map.of("IS_INVENTORY_ALLOCATED", true));
    testContext.mockJobWorker("packingQueue")
            .thenComplete(Map.of("IS_QUALITY_PASSED", true));
    testContext.mockJobWorker("deliveryQueue")
            .thenComplete(Map.of("READY_TO_DELIVERY", true));

    // Receive task is released by message correlation on orderId
    publishOrderConfirmation("ORDER-001");

    assertThat(instance).isCreated();
    assertThat(instance).isCompleted();
}
```

### Example: Failed Quality Branch

```java
@Test
void shouldStayActiveWhenQualityCheckFails() {
    deployOrderProcess();

    ProcessInstanceEvent instance = startOrderProcess("TEST-FAIL-001");

    testContext.mockJobWorker("inventoryAllocation")
            .thenComplete(Map.of("IS_INVENTORY_ALLOCATED", true));
    testContext.mockJobWorker("packingQueue")
            .thenComplete(Map.of("IS_QUALITY_PASSED", false));

    publishOrderConfirmation("TEST-FAIL-001");

    // On failure, process routes to Manual Review user task and remains active
    assertThat(instance).isActive();
    assertThat(instance).hasActiveElements("Activity_1nid25r");
}
```

### Notes for This BPMN

- Start event goes to a **receive task** (`Confirm Order`), so tests must publish `Message_Confirmation`.
- Correlation key is `orderId` (message subscription uses `=orderId`).
- Worker types are `inventoryAllocation`, `packingQueue`, and `deliveryQueue`.
- Gateway branch depends on `IS_QUALITY_PASSED`.
- If production workers are disabled in tests (for example with `@Profile("!test")`), service-task mocks are required via `mockJobWorker(...)`.

### Optional: Deterministic Task Control

```java
@Test
void shouldUseMockWorkersWhenYouNeedStrictStepControl() {
    deployOrderProcess();

    ProcessInstanceEvent instance = startOrderProcess("ORDER-MOCK-001");

    publishOrderConfirmation("ORDER-MOCK-001");

    // Use mock workers only when deterministic control is needed.
    testContext.mockJobWorker("inventoryAllocation")
            .thenComplete(Map.of("IS_INVENTORY_ALLOCATED", true));
    testContext.mockJobWorker("packingQueue")
            .thenComplete(Map.of("IS_QUALITY_PASSED", true));
    testContext.mockJobWorker("deliveryQueue").thenComplete();

    assertThat(instance).isCompleted();
}
```

---
