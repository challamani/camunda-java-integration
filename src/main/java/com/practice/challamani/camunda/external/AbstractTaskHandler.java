package com.practice.challamani.camunda.external;


import com.practice.challamani.camunda.config.SystemProperties;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.springframework.stereotype.Service;

/**
 * The AbstractTaskHandler implements Camunda's ExternalTaskHandler
 */

@Slf4j
@Service
public abstract class AbstractTaskHandler implements ExternalTaskHandler {

	private String topicName;
	private Integer duration;

	public AbstractTaskHandler(SystemProperties.Worker worker) {
		super();
		this.topicName = worker.getTopic();
		this.duration = worker.getLockDuration();
        log.info("topic {} duration {}",topicName,duration);
	}

	public String getTopicName() {
		return topicName;
	}

	public Integer getDuration() {
		return duration;
	}

}
