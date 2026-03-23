package com.practice.challamani.camunda;


import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static io.camunda.process.test.api.CamundaAssert.assertThat;

@SpringBootTest
@CamundaSpringProcessTest
@ActiveProfiles("test")
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

    @Test
    void shouldCompleteOrderProcessingSuccessPath() {
        deployOrderProcess();

        // 1. Start
        ProcessInstanceEvent instance = client.newCreateInstanceCommand()
                .bpmnProcessId("OrderProcessing")
                .latestVersion()
                .send().join();

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
    void shouldFailOrderProcessing() {
        deployOrderProcess();

        ProcessInstanceEvent instance = client.newCreateInstanceCommand()
                .bpmnProcessId("OrderProcessing")
                .latestVersion()
                .send().join();

        testContext.mockJobWorker("checkOrderInformation")
                .thenComplete();
        testContext.mockJobWorker("orderQueue").thenComplete();
        assertThat(instance).isActive();

        // Mock Quality check FAILURE
        testContext.mockJobWorker("packingQueue")
                .thenComplete(Map.of("isQualityPassed", false));

        // Assert it went straight to End and skipped "Initiate a delivery"
        assertThat(instance).isCompleted();
    }
}