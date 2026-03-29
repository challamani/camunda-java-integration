package com.practice.challamani.camunda.workers;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@Profile("!test")
public class OrderProcessHandler {

    private static final String VAR_PROCESSED_AT = "processedAt";
    private static final String VAR_ORDER_ID = "orderId";

    @JobWorker(type = "inventoryAllocation")
    public Map<String, Object> processInventory(final ActivatedJob job) {
        logStep(job, "Allocating inventory");

        return createResponse(Map.of("IS_INVENTORY_ALLOCATED", true));
    }

    @JobWorker(type = "packingQueue")
    public Map<String, Object> processPacking(final ActivatedJob job) {
        logStep(job, "Packing order");

        // Use getVariableAsType for safer type handling
        String orderId = job.getVariable(VAR_ORDER_ID).toString();
        boolean isQualityPassed = !orderId.startsWith("TEST");

        return createResponse(Map.of("IS_QUALITY_PASSED", isQualityPassed));
    }

    @JobWorker(type = "deliveryQueue")
    public Map<String, Object> processDelivery(final ActivatedJob job) {
        logStep(job, "Initiating delivery");

        return createResponse(Map.of("READY_TO_DELIVERY", true));
    }

    private Map<String, Object> createResponse(Map<String, Object> businessVariables) {
        Map<String, Object> response = new HashMap<>(businessVariables);
        response.put(VAR_PROCESSED_AT, OffsetDateTime.now().toString());
        return response;
    }

    private void logStep(ActivatedJob job, String message) {
        log.info("{} | JobKey: {} | ProcessInstance: {}",
                message, job.getKey(), job.getProcessInstanceKey());
    }
}