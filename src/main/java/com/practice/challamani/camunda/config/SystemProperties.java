package com.practice.challamani.camunda.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("workers-config")
public class SystemProperties {

    private List<Worker> workers  = new ArrayList<>();

    public static class Worker {

        private String topic;
        private Integer lockDuration;
        private String description;
        private String retries;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public Integer getLockDuration() {
            return lockDuration;
        }

        public void setLockDuration(Integer lockDuration) {
            this.lockDuration = lockDuration;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRetries() {
            return retries;
        }

        public void setRetries(String retries) {
            this.retries = retries;
        }
    }

    public Worker getWorkerConfigByTopic(String topic){
        return workers.stream().filter(worker -> StringUtils.equalsIgnoreCase(topic,worker.getTopic()))
                .findFirst().orElse(null);
    }
}
