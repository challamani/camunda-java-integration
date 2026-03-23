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

#client-config:
#  enabled: false                          # Disable Camunda 7 workers during tests
```

**Key Configuration Points:**
- `runtime-mode: MANAGED` — Testcontainer manages Camunda instance startup/shutdown
- `camunda-docker-image-version` — Pin to tested Camunda version (not latest)
- `client-config.enabled: false` — Prevents production worker beans from auto-registering

### 3. Conditional Bean Registration

Guard production components to avoid instantiation during tests:

```java
@Component
@ConditionalOnProperty(
    prefix = "client-config", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class WorkerRegistrar {
    // External task client registration logic
    // Enabled in production, disabled in @ActiveProfiles("test")
}
```

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

## BPMN Model Requirements for Testing

### Zeebe-Compatible Service Tasks

For testcontainers with Camunda 8, all external tasks must use Zeebe syntax:

```xml
<bpmn:serviceTask id="Activity_CheckOrder" name="Check Order">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="checkOrderInformation" retries="3"/>
  </bpmn:extensionElements>
  <bpmn:incoming>Flow_1</bpmn:incoming>
  <bpmn:outgoing>Flow_2</bpmn:outgoing>
</bpmn:serviceTask>
```

**Do NOT use Camunda 7 syntax** (which may work in legacy engines but won't activate in Zeebe):
```xml
<!-- ❌ WRONG for Camunda 8 -->
<serviceTask camunda:type="external" camunda:topic="orderTopic" />
```

### Conditional Expressions

Use Zeebe's feel expression language:

```xml
<bpmn:sequenceFlow id="Flow_Success" name="True" 
                   sourceRef="Gateway_Check" 
                   targetRef="Activity_Process">
  <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
    = isQualityPassed
  </bpmn:conditionExpression>
</bpmn:sequenceFlow>

<bpmn:sequenceFlow id="Flow_Failure" name="False" 
                   sourceRef="Gateway_Check" 
                   targetRef="Event_End">
  <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
    = not(isQualityPassed)
  </bpmn:conditionExpression>
</bpmn:sequenceFlow>
```

---

## Benefits of Testcontainer-Based E2E Testing

### 1. **Early Validation of Process Logic**
   - Catch BPMN syntax errors, undefined variables, and bad expressions before production deployment
   - Example: A typo in a conditional expression is caught in test, not after deployment

### 2. **Regression Prevention**
   - Modifications to a BPMN model are immediately validated against test suite
   - If a change breaks an existing flow, tests fail fast
   - Developers gain confidence when refactoring processes

### 3. **Isolated Testing Environment**
   - Tests run in ephemeral Docker containers — no pollution of development/staging engines
   - Each test gets a clean Zeebe instance
   - No cleanup burden; containers are destroyed after tests complete

### 4. **Fast Feedback Loop**
   - No manual UI testing required for basic happy/sad paths
   - Testcontainers start/stop quickly (typically ~10–20s per test)
   - CI/CD pipelines can run full suite in < 1 minute

### 5. **Deterministic Behavior**
   - Mock job workers guarantee timing — no flaky waiting for external services
   - Conditional branching can be thoroughly exercised (success path, error path, timeout path)
   - Variables are predictable and auditable

### 6. **Documentation via Tests**
   - Tests serve as executable documentation of process behavior
   - New team members can read tests to understand expected workflows
   - Acceptance criteria are codified as assertions

### 7. **Cost Reduction**
   - No need to maintain separate test Camunda instances
   - Reduced manual testing labor
   - Fewer production incidents due to unvalidated process changes

### 8. **Integration with CI/CD**
   - Tests run automatically on pull requests
   - No manual approval step for BPMN changes
   - Deployment gates can be automated (e.g., "block merge if test fails")

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

### 5. **Mock External Services, Don't Call Real Ones**

```java
// ✅ Correct: mock the job worker
testContext.mockJobWorker("paymentService")
        .thenComplete(Map.of("transactionId", "TX-123"));

// ❌ Wrong: test tries to call actual payment API
PaymentAPI.charge(order.getAmount()); // Don't do this
```

### 6. **Test at Multiple Levels**

| Level | Scope | Example |
|-------|-------|---------|
| **Unit** | Individual task handlers | Test order validation logic in isolation |
| **Integration** | BPMN + mock services | Test "Order → Payment → Shipping" flow |
| **E2E** | Full process with testcontainer | Test entire process lifecycle |

---

## Troubleshooting

### Issue: "No enum constant CamundaProcessTestRuntimeMode.testcontainers"

**Solution:** Update `application-test.yaml` to use a valid enum:

```yaml
camunda:
  process-test:
    runtime-mode: MANAGED  # Use MANAGED or REMOTE, not testcontainers
```

### Issue: "Expected to complete user task but no user task is available"

**Solution:** Use `zeebe:taskDefinition` for service tasks, not legacy user tasks:

```xml
<!-- ✅ Correct for testing -->
<bpmn:serviceTask id="Activity_CheckOrder">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="checkOrder" retries="3"/>
  </bpmn:extensionElements>
</bpmn:serviceTask>

<!-- ❌ Avoid legacy user tasks -->
<bpmn:userTask id="Activity_CheckOrder" />
```

### Issue: "Connection refused to localhost:8080"

**Solution:** Ensure Camunda 7 external workers are disabled in tests:

```yaml
# application-test.yaml
client-config:
  enabled: false
```

And in Java:

```java
@ConditionalOnProperty(
    prefix = "client-config",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class WorkerRegistrar { }
```

### Issue: Tests are slow

**Solution:**
- Increase testcontainer startup timeout in GitHub Actions or CI environment
- Use `@Testcontainers` with explicit image pinning to avoid pulling latest each run
- Batch related tests into single test class to reuse container

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

Testcontainer-based E2E BPMN testing provides:

1. ✅ **Fast feedback** on BPMN logic without manual testing
2. ✅ **Regression prevention** when processes are modified
3. ✅ **Isolated environments** that don't interfere with dev/staging/prod
4. ✅ **Deterministic behavior** via mocked external services
5. ✅ **Living documentation** that demonstrates process flows
6. ✅ **CI/CD integration** for automated validation on every commit
7. ✅ **Cost savings** by reducing manual testing and production incidents

By following the patterns in this guide, you can confidently deploy BPMN process definitions with high confidence that they will work as intended in production.

