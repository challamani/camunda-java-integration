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

    private static final String PROCESS_ID = "Process_0rcqim1";
    private static final String MESSAGE_NAME = "Message_Confirmation";

    @Autowired
    private CamundaClient client;

    @Autowired
    private CamundaProcessTestContext testContext;

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

    @Test
    void shouldCompleteOrderProcessingSuccessPath() {
        deployOrderProcess();

        ProcessInstanceEvent instance = startOrderProcess("ORDER-001");
        publishOrderConfirmation("ORDER-001");

        assertThat(instance).isCreated();
        assertThat(instance).isCompleted();
    }

    @Test
    void shouldStayActiveWhenQualityCheckFails() {
        deployOrderProcess();

        ProcessInstanceEvent instance = startOrderProcess("TEST-FAIL-001");
        publishOrderConfirmation("TEST-FAIL-001");
        // On failed quality check, the token moves to Manual Review and remains active.
        assertThat(instance).isActive();
        assertThat(instance).hasActiveElements("Activity_1nid25r");
    }
}

