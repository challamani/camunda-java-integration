package com.practice.challamani.camunda.worker;


import com.practice.challamani.camunda.external.worker.AbstractTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.practice.challamani.camunda.config.ApplicationContextHolder;
import com.practice.challamani.camunda.worker.model.Worker;

import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class WorkerRegistrar {

    private String camundaBaseUrl;
    private Integer maxTasks;
    private Long asyncResponseTimeout;
    private List<String> workers;

    @Autowired
    private ApplicationContextHolder applicationContextHolder;

    private List<ExternalTaskClient> clients = new ArrayList<>();

    @PostConstruct
    private void init() {
        try {
            for (String worker : workers) {
                AbstractTaskHandler abstractTaskHandler = (AbstractTaskHandler) ApplicationContextHolder.getContext().getBean(worker);
                register(abstractTaskHandler, abstractTaskHandler.getTopicName(), abstractTaskHandler.getDuration());
            }
        } catch (Exception e) {

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

