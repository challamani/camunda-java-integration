package com.practice.challamani.camunda.worker.model;

public class Worker {

    private String workerId;
    private String topic;
    private Integer lockDuration;
    private String description;
    private String beanReference;

    public Worker() {
    }

    public String getBeanReference() {
        return beanReference;
    }

    public void setBeanReference(String beanReference) {
        this.beanReference = beanReference;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

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
}
