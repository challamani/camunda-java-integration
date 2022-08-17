package com.practice.challamani.camunda.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConfigurationProperties(prefix = "workers-config", ignoreInvalidFields = true)
public class SystemProperties {

    private Integer noOfWorkers;
    private Boolean async;
    private List<Worker> workers;

    public List<Worker> getWorkers() {
        return workers;
    }

    public static class Worker {

        private String topic;
        private Integer lockDuration;
        private String name;
        private Integer retries;

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getRetries() {
            return retries;
        }

        public void setRetries(Integer retries) {
            this.retries = retries;
        }

        @Override
        public String toString() {
            return "Worker{" +
                    "topic='" + topic + '\'' +
                    ", lockDuration=" + lockDuration +
                    ", name='" + name + '\'' +
                    ", retries=" + retries +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SystemProperties{" +
                "noOfWorkers=" + noOfWorkers +
                ", async=" + async +
                ", workers=" + workers +
                '}';
    }

    public Integer getNoOfWorkers() {
        return noOfWorkers;
    }

    public void setNoOfWorkers(Integer noOfWorkers) {
        this.noOfWorkers = noOfWorkers;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public Worker getWorkerConfigByTopic(String topic){

        log.info("list of workers {}",workers);
        return workers.stream().filter(worker -> StringUtils.equalsIgnoreCase(topic,worker.getTopic()))
                .findFirst().orElse(null);
    }

    public void setWorkers(List<Worker> workers) {
        this.workers = workers;
    }
}
