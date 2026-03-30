
# Camunda 8 and Spring Boot 3.x integration examples

This repo contains a sample Spring Boot application that demonstrates how to integrate with Camunda 8 using the Zeebe client. It includes examples of external service tasks and receive tasks, showcasing how to interact with the Camunda engine from a Spring Boot application.

## Prerequisites

### version

- Default build/runtime: **Java 21**
- Optional fallback: **Java 17** via Maven profile `java17`
- Camunda 8 (Zeebe) instance running locally or remotely
- Spring Boot 3.x application with Zeebe client dependency


## Start Camunda 8 instance

Download the latest - https://docs.camunda.io/docs/self-managed/quickstart/developer-quickstart/c8run/
Run:
```bash 
  cd c8run-8* && ./start.sh
```

## Install Camunda Modeler

Install [Camunda Modeler](https://camunda.com/download/modeler/)


## Exploring the Camunda 8 Rest APIs to deploy and start the process

- Deploy the BPMN process definition using Camunda Rest API

Request:

```bash
  curl -X POST http://localhost:8080/v2/deployments \
    -H "Content-Type: multipart/form-data" \
    -F "resources=@./src/main/resources/order-process-v2.bpmn"
 ```

Response:

```json
{
  "tenantId": "<default>",
  "deploymentKey": "2251799813693661",
  "deployments": [
    {
      "processDefinition": {
        "processDefinitionId": "OrderProcessDefinition_v2",
        "processDefinitionVersion": 1,
        "resourceName": "order-process-v2.bpmn",
        "tenantId": "<default>",
        "processDefinitionKey": "2251799813693662"
      }
    }
  ]
}
```

- Start a process using curl command

```shell
curl -X POST http://localhost:8080/v2/process-instances \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionId": "OrderProcessDefinition_v2",
    "variables": {
      "orderId": "ORDER-001"
    }
  }'
```

Response:

```json
{
  "processDefinitionId": "OrderProcessDefinition_v2",
  "processDefinitionVersion": 1,
  "tenantId": "<default>",
  "variables": {},
  "processDefinitionKey": "2251799813693662",
  "processInstanceKey": "2251799813695002",
  "tags": []
}
```

## Process REST Endpoints

### Start Process Instance

**Endpoint:** `POST /api/processes/start`

Start a new process instance with variables using the Java Client:

```bash
curl -X POST http://localhost:8082/api/processes/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionId": "OrderProcessDefinition_v2",
    "variables": {
      "orderId": "ORDER-001"
    }
  }'
```

**Response:**
```json
{
  "processInstanceKey": 2251799813695002,
  "processDefinitionId": "OrderProcessDefinition_v2",
  "processDefinitionVersion": 1,
  "status": "ACTIVE"
}
```

### Publish Message

**Endpoint:** `POST /api/processes/publish-message`

Publish a message to correlate with process instances using the Java Client:

```bash
curl -X POST http://localhost:8082/api/processes/publish-message \
  -H "Content-Type: application/json" \
  -d '{
    "messageName": "Message_Confirmation",
    "correlationKey": "ORDER-001",
    "variables": {
      "approvedBy": "admin"
    },
    "timeToLive": 300000
  }'
```

**Response:**
```json
{
  "messageName": "Message_Confirmation",
  "correlationKey": "ORDER-001",
  "status": "PUBLISHED",
  "message": "Message published successfully"
}
```

## Process Flow

1. Start process via `/api/processes/start` → Process waits at Receive Task
2. External service tasks execute via Job Workers (`inventoryAllocation`, `packingQueue`, `deliveryQueue`)
3. Publish message via `/api/processes/publish-message` → Token advances and service tasks execute
4. On quality check success → Process completes
5. On quality check failure → Manual Review user task remains active

**Note:** Service task execution relies on active Job Workers (see `OrderProcessHandler.java`). No mocking required for Job Workers in deployment.

![camunda-process-screenshot.png](src/main/resources/camunda-process-screenshot.png)

