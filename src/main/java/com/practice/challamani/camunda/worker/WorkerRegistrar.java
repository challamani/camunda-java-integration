package com.practice.challamani.camunda.worker;


import com.practice.challamani.camunda.external.AbstractTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.practice.challamani.camunda.config.ApplicationContextHolder;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConfigurationProperties(prefix = "client-config", ignoreInvalidFields = true)
public class WorkerRegistrar {

    private String camundaBaseUrl;
    private Integer maxTasks;
    private Integer asyncResponseTimeout;
    private String workersEnabled;

    @Autowired
    private ApplicationContextHolder applicationContextHolder;

    private List<ExternalTaskClient> clients = new ArrayList<>();

    @PostConstruct
    private void init() {
        try {
            log.info("camundaBaseUrl {} maxTasks {} asyncResponseTimeout {} workersEnabled {}"
            ,camundaBaseUrl,maxTasks,asyncResponseTimeout,workersEnabled);

            for (String worker : workersEnabled.split(",")) {
                AbstractTaskHandler abstractTaskHandler = (AbstractTaskHandler) ApplicationContextHolder.getContext().getBean(worker);
                register(abstractTaskHandler, abstractTaskHandler.getTopicName(), abstractTaskHandler.getDuration());
            }
        } catch (Exception e) {
            log.error("worker registration failed {}",e);
        }
    }


    private void register(AbstractTaskHandler abstractTaskHandler, String topicName, Integer lockDuration) {
        ExternalTaskClient client = ExternalTaskClient.create().baseUrl(camundaBaseUrl)
                .maxTasks(maxTasks)
                .asyncResponseTimeout(asyncResponseTimeout)
                .build();

        client.subscribe(topicName)
                .lockDuration(lockDuration)
                .handler(abstractTaskHandler)
                .open();
        clients.add(client);
    }

    public List<ExternalTaskClient> getClients(){
        return clients;
    }

    public String getCamundaBaseUrl() {
        return camundaBaseUrl;
    }

    public void setCamundaBaseUrl(String camundaBaseUrl) {
        this.camundaBaseUrl = camundaBaseUrl;
    }

    public Integer getMaxTasks() {
        return maxTasks;
    }

    public void setMaxTasks(Integer maxTasks) {
        this.maxTasks = maxTasks;
    }

    public Integer getAsyncResponseTimeout() {
        return asyncResponseTimeout;
    }

    public void setAsyncResponseTimeout(Integer asyncResponseTimeout) {
        this.asyncResponseTimeout = asyncResponseTimeout;
    }

    public String getWorkersEnabled() {
        return workersEnabled;
    }

    public void setWorkersEnabled(String workersEnabled) {
        this.workersEnabled = workersEnabled;
    }

}

