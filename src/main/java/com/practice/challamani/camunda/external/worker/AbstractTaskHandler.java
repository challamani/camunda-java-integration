package com.practice.challamani.camunda.external.worker;


import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AbstractCTaskHandler implements Camunda's ExternalTaskHandler
 */

public abstract class AbstractTaskHandler implements ExternalTaskHandler {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTaskHandler.class);
	private String topicName;
	private Integer duration;

	public AbstractTaskHandler(String topicName, Integer duration) {
		super();
		this.topicName = topicName;
		this.duration = duration;
        logger.info("topic {} duration {}",topicName,duration);
	}

	public String getTopicName() {
		return topicName;
	}

	public Integer getDuration() {
		return duration;
	}

}
