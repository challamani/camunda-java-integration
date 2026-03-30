# BPMN Testing Guide (Testcontainers + Remote Engine)

## Overview

This project supports two BPMN testing styles:

- `OrderProcessTest` — managed Testcontainers runtime, starts a Camunda Docker container automatically
- `OrderProcessRemoteTest` — remote runtime, connects to an already running Camunda 8 engine

Both validate the same BPMN (`order-process-v2.bpmn`) and message correlation behavior.

## When to Use Which

| | `OrderProcessTest` | `OrderProcessRemoteTest` |
|---|---|---|
| Profile | `test-managed` | `test-remote` |
| Runtime mode | `MANAGED` | `REMOTE` |
| Docker required | ✅ Yes | ❌ No |
| Camunda engine required | auto-managed | ✅ Must be running |
| Best for | CI/CD, full isolation | Local dev with engine already running |

## Prerequisites

### Testcontainers (`test-managed`)
- Docker must be installed and running
- No manual engine setup needed — container starts automatically

### Remote (`test-remote`)
- A Camunda 8 engine must be running and reachable **before** tests start
- Default endpoints (from `application.yaml`):
  - gRPC: `http://localhost:26500`
  - REST: `http://localhost:8080`
- Start a local engine via [Camunda 8 Run](https://docs.camunda.io/docs/self-managed/setup/deploy/local/c8run/):
  ```bash
  cd c8run-8* && ./start.sh
  ```

## Dependencies

Single test dependency covers both approaches in `pom.xml`:

```xml
<dependency>
    <groupId>io.camunda</groupId>
    <artifactId>camunda-process-test-spring</artifactId>
    <version>8.8.18</version>
    <scope>test</scope>
</dependency>
```

Supported runtime modes in this version: `MANAGED` and `REMOTE`.

## Configuration Profiles

### 1) Testcontainers profile

`src/test/resources/application-test-managed.yaml`

```yaml
camunda:
  process-test:
    runtime-mode: MANAGED
    camunda-docker-image-version: 8.8.0
    multi-tenancy-enabled: false

client-config:
  enabled: false
```

### 2) Remote profile

`src/test/resources/application-test-remote.yaml`

```yaml
camunda:
  process-test:
    runtime-mode: REMOTE
    multi-tenancy-enabled: false

client-config:
  enabled: false
```

> **Note:** In `REMOTE` mode, `CamundaClient` connects to the engine configured in `application.yaml` (`grpc-address`, `rest-address`). Ensure those endpoints are reachable before running the tests.

## Test Classes

- `src/test/java/com/practice/challamani/camunda/OrderProcessTest.java`
  - Profile: `test-managed`
  - Starts a Camunda 8 Docker container via Testcontainers

- `src/test/java/com/practice/challamani/camunda/OrderProcessRemoteTest.java`
  - Profile: `test-remote`
  - Connects to a pre-running Camunda 8 engine — **engine must be started manually**

## Process Notes for Both Approaches

- Process ID: `OrderProcessDefinition_v2`
- Message name: `Message_Confirmation`
- Correlation key expression: `=orderId`
- Worker types: `inventoryAllocation`, `packingQueue`, `deliveryQueue`
- Failed quality path routes to user task `Activity_1nid25r` (Manual Review)

## BPMN Coverage Report

After running tests, Camunda generates a coverage report at:

```
target/coverage-report/report.html
```

Open it in a browser to visualise which BPMN elements and sequence flows were exercised by each test.

### Current Coverage — `OrderProcessDefinition_v2`

| Scope | Coverage |
|---|---|
| Overall (all tests) | **93.75%** (15 / 16 elements) |
| `shouldCompleteOrderProcessingSuccessPath` | 81.25% |
| `shouldStayActiveAtManualReviewWhenQualityFails` | 68.75% |

#### Covered elements (combined)

| Element | Type | Covered by |
|---|---|---|
| `StartEvent_1` | Start Event | Both tests |
| `Activity_0dnqpva` | Receive Task — Confirm Order | Both tests |
| `Activity_0gw8k50` | Service Task — Inventory Allocation | Both tests |
| `Activity_1g0gkk3` | Service Task — Quality & Packing | Both tests |
| `Gateway_04bjqao` | Exclusive Gateway | Both tests |
| `Activity_0gf6y5x` | Send Task — Initiate Delivery | Success path |
| `Event_1jog9qi` | End Event | Success path |
| `Activity_1nid25r` | User Task — Manual Review | Failure path |

#### Covered sequence flows (combined)

`Flow_02c2wt0` → `Flow_07wxlji` → `Flow_0inlpdr` → `Flow_0mqinnp` → `Flow_0h1s7qv` (quality pass) → `Flow_0iyj71e` → `Flow_1aswyhx` (quality fail)

#### ⚠️ Uncovered element

| Element | Type | Reason |
|---|---|---|
| `Flow_1ur84l7` | Sequence Flow — Manual Review → Quality & Packing | No test completes the Manual Review user task to trigger the retry loop |

To reach 100% coverage, add a test that completes the Manual Review user task and re-enters the Quality & Packing service task.

## About Service Task Mocking

Real job workers (`OrderProcessHandler`) are disabled in both test profiles via `@Profile("!test-managed & !test-remote")`, so `mockJobWorker(...)` drives all service-task completions deterministically in both test classes.

| Profile | Workers Active | Mocking |
|---|---|---|
| `test-managed` | ❌ disabled | ✅ `mockJobWorker(...)` required |
| `test-remote` | ❌ disabled | ✅ `mockJobWorker(...)` required |
