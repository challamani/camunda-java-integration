package com.practice.challamani.camunda.external.worker;


import com.practice.challamani.camunda.external.AbstractWorker;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OrderProcessingWorker extends AbstractWorker implements Runnable {

    public OrderProcessingWorker(ExternalTask externalTask,
                                 ExternalTaskService externalTaskService,
                                 String workerId, Integer retryAttempts) {
        super(externalTask, externalTaskService, workerId, retryAttempts);
    }

    @Override
    public void run() {
        log.info("OrderProcessingWorker {} {}", externalTask.getBusinessKey(), externalTask.getWorkerId());
        Map<String, Object> map = new HashMap<>();
        map.put("orderStatus","Confirmed");
        completeTask(map);
    }
}
