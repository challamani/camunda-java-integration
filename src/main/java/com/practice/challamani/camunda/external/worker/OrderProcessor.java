package com.practice.challamani.camunda.external.worker;

import com.practice.challamani.camunda.config.SystemProperties;
import com.practice.challamani.camunda.external.AbstractTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Slf4j
@Service("order-processor")
public class OrderProcessor extends AbstractTaskHandler {

    private final static String TOPIC="orderQueue";
    private final TaskExecutor taskExecutor;
    private final SystemProperties.Worker workerConfig;

    @Autowired
    public OrderProcessor(SystemProperties systemProperties,@Qualifier("workerTaskExecutor") TaskExecutor taskExecutor) {
        super(systemProperties.getWorkerConfigByTopic(TOPIC));
        this.taskExecutor =  taskExecutor;
        this.workerConfig = systemProperties.getWorkerConfigByTopic(TOPIC);
    }

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log.info("executing the service task for businessKey {}",externalTask.getBusinessKey());
        taskExecutor.execute(new OrderProcessingWorker(externalTask,externalTaskService,getClass().getName(),workerConfig.getRetries()));
    }
}
