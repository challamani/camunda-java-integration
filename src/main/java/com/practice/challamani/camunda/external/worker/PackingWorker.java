package com.practice.challamani.camunda.external.worker;

import com.practice.challamani.camunda.external.AbstractWorker;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PackingWorker extends AbstractWorker implements Runnable {

    public PackingWorker(ExternalTask externalTask,
                                 ExternalTaskService externalTaskService,
                                 String workerId, Integer retryAttempts) {
        super(externalTask, externalTaskService, workerId, retryAttempts);
    }

    @Override
    public void run() {
        log.info("PackingWorker {} {}", externalTask.getBusinessKey(), externalTask.getWorkerId());
        Map<String, Object> map = new HashMap<>();
        map.put("isQualityPassed",true);
        completeTask(map);
    }
}
