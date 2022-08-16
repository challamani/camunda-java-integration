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

@Component
@Slf4j
@ConfigurationProperties(prefix = "client-config")
public class WorkerRegistrar {

    private String camundaBaseUrl;
    private Integer maxTasks;
    private Long asyncResponseTimeout;
    private String workersEnabled;

    @Autowired
    private ApplicationContextHolder applicationContextHolder;

    private List<ExternalTaskClient> clients = new ArrayList<>();

    @PostConstruct
    private void init() {
        try {
            log.info("");
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
}

