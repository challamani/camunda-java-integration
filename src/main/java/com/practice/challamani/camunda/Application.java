package com.practice.challamani.camunda;

import io.camunda.client.annotation.Deployment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Slf4j
@SpringBootApplication
@Deployment(resources = "classpath:order-process-v2.bpmn")
public class Application{
	public static void main(String[] args)  {
		SpringApplication.run(Application.class,args);
	}
}