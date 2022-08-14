package com.practice.challamani.camunda.external;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.web.client.RestTemplate;
import com.practice.challamani.camunda.config.ApplicationContextHolder;

import java.util.Map;

/**
 * The AbstractWorker contains general camunda-service task handling functionality,
 */

public class AbstractWorker {

    protected ExternalTask externalTask;
    protected ExternalTaskService externalTaskService;
    private String workerId;
    protected Integer retryAttempts;

    public AbstractWorker(ExternalTask externalTask, ExternalTaskService externalTaskService, String workerId, Integer retryAttempts) {
        this.externalTask = externalTask;
        this.externalTaskService = externalTaskService;
        this.workerId = workerId;
        this.retryAttempts = retryAttempts;
    }


    protected RestTemplate getRestTemplate(){
        return ApplicationContextHolder.getContext().getBean(RestTemplate.class);
    }

    protected boolean completeServiceTask(Map<String,Object> serviceTaskVariables) {
        try {

            if (serviceTaskVariables != null) {
                externalTaskService.complete(this.externalTask, serviceTaskVariables);
            } else {
                externalTaskService.complete(this.externalTask);
            }
            return true;
        } catch (Exception e) {
            throw e;
        }
    }

    protected boolean taskErrorHandling(String errorMessage, String errorDetails, int retries, long retryTimeout) {
        try {
            externalTaskService.handleFailure(this.externalTask, errorMessage, errorDetails, retries, retryTimeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean unlockServiceTask() {
        try {
            externalTaskService.unlock(this.externalTask);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean extendLock(Long milliSec) {
        try {
            externalTaskService.extendLock(this.externalTask, milliSec);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean handleBPMNError(String message) {
        try {
            externalTaskService.handleBpmnError(this.externalTask, message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
