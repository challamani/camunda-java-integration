package com.practice.challamani.camunda.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "worker-executor")
public class ThreadConfig {

	private Integer corePoolSize;
	private Integer maxPoolSize;
    private Integer poolCapacity;
	private String name;

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public void setPoolCapacity(Integer poolCapacity) {
        this.poolCapacity = poolCapacity;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Bean("workerTaskExecutor")
    public TaskExecutor createTaskExecutor() {
        log.info("thread-pool configuration min {} max {} capacity {}",corePoolSize,maxPoolSize,poolCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(poolCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix(name);
        executor.initialize();
        return executor;
    }
}