package com.practice.challamani.camunda;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@Slf4j
@EnableScheduling
public class Application implements ApplicationRunner {

	public static void main(String[] args)  {
		SpringApplication.run(Application.class,args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
	}
}