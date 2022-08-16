package com.practice.challamani.camunda.external.worker;

import com.practice.challamani.camunda.config.SystemProperties;
import com.practice.challamani.camunda.external.AbstractTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("order-processor")
public class OrderProcessor extends AbstractTaskHandler {

    private final static String TOPIC="orderQueue";

    @Autowired
    public OrderProcessor(SystemProperties systemProperties) {
        super(systemProperties.getWorkerConfigByTopic(TOPIC));
    }

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log.info("executing the service task {}",externalTask.getBusinessKey());
    }
}
