# Camunda Java Integration - external service task implementation.

In this repository you can find some sample external worker implementation using **camunda-external-task-client**, 
this spring-boot application act as a client by subscribing to set of topics (Camunda's external service tasks), this client continuously poll to camunda with backoff strategy to process active instances. 

Camunda Rest API - `FetchAndLock` backoff strategy based integration.

## Java version

- Default build/runtime: **Java 21**
- Optional fallback: **Java 17** via Maven profile `java17`


## External service-task example (Start order processing workflow)

1. Start camunda instance in your local using docker image

    ```shell
        docker pull camunda/camunda-bpm-platform:latest
        docker run -d --name camunda -p 8080:8080 camunda/camunda-bpm-platform:latest
    ```

2. Install Camunda Modeler
    [Camunda Modeler](https://camunda.com/download/modeler/)

3. Deploy BPMN file using modeler - ```resource/order-processing.bpmn```

4. Deploy the process definition using Camunda Rest API

    ```shell
        curl -X POST http://localhost:8080/engine-rest/deployment/create \
          --user demo:demo \
          -H "Content-Type: multipart/form-data" \
          -F "deployment-name=order-processing.bpmn" \
          -F "data=@./src/main/resources/order-processing.bpmn"
    ```

5. Start a process instance using Camunda UI - tasklist option.


## Receive task example

1. Deploy and Start the process using Camunda modeler or Curl command as mentioned above.
2. Process waits at the `Receive Task` until it receives the message with name `SimpleNotifyRef` and business key `Example-9001`, you can trigger the message using below curl command or using Camunda UI - Message option.

3.
```shell
curl -X POST http://localhost:8080/engine-rest/message \
     --user demo:demo \
     -H "Content-Type: application/json" \
     -d '{
          "messageName": "SimpleNotifyRef",
          "businessKey": "Example-90001",
          "resultEnabled": true
         }'
```

*Refer below links for more information*

[Camunda Docker Image](https://hub.docker.com/r/camunda/camunda-bpm-platform/)

[Camunda Rest API](https://docs.camunda.org/manual/latest/reference/rest/)

[Fetch and Lock external task](https://docs.camunda.org/manual/latest/reference/rest/external-task/fetch/)

<h4>***NOTE: Implementation partially completed.***</h4>


![camunda-process-screenshot.png](src/main/resources/camunda-process-screenshot.png)

