# Camunda Java Integration - external service task implementation.

In this repository you can find some sample external worker implementation using **camunda-external-task-client**, 
this spring-boot application act as a client by subscribing to set of topics (Camunda's external service tasks), this client continuously poll to camunda with backoff strategy to progress active instances. 

Camunda Rest API - FetchAndLock backoff strategy based integration.

1. Start camunda instance in your local using docker image

```shell
docker pull camunda/camunda-bpm-platform:latest
docker run -d --name camunda -p 8080:8080 camunda/camunda-bpm-platform:latest
```

2. Install Camunda Modeler
[Camunda Modeler](https://camunda.com/download/modeler/)

3. Deploy bpmn file using modeler - ```resource/order-processing.bpmn```


###Refer below links for more information
[Camunda Docker Image](https://hub.docker.com/r/camunda/camunda-bpm-platform/)

[Camunda Rest API](https://docs.camunda.org/manual/latest/reference/rest/)

[Fetch and Lock external task](https://docs.camunda.org/manual/latest/reference/rest/external-task/fetch/)

<h4>***NOTE: Implementation partially completed.***</h4>


![Image](https://github.com/challamani/camunda-java-integration/blob/main/src/main/resources/camunda-process-screenshot.png)

