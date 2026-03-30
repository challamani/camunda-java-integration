package com.practice.challamani.camunda.controller;

import com.practice.challamani.camunda.dto.PublishMessageRequest;
import com.practice.challamani.camunda.dto.PublishMessageResponse;
import com.practice.challamani.camunda.dto.StartProcessRequest;
import com.practice.challamani.camunda.dto.StartProcessResponse;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * REST Controller for Camunda Process and Message operations
 * Provides endpoints to start processes and publish messages
 */
@RestController
@RequestMapping("/api/processes")
@Slf4j
@RequiredArgsConstructor
public class ProcessController {


    private final CamundaClient camundaClient;

    /**
     * Start a process instance with the given process definition ID and variables
     *
     * @param request StartProcessRequest containing processDefinitionId and variables
     * @return ResponseEntity with StartProcessResponse containing processInstanceKey and status
     */
    @PostMapping("/start")
    public ResponseEntity<StartProcessResponse> startProcess(@RequestBody StartProcessRequest request) {
        try {
            log.info("Starting process: {} with variables: {}", request.getProcessDefinitionId(), request.getVariables());

            var processInstanceEvent = camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId(request.getProcessDefinitionId())
                    .latestVersion()
                    .variables(request.getVariables())
                    .send()
                    .join();

            var response = StartProcessResponse.builder()
                    .processInstanceKey(processInstanceEvent.getProcessInstanceKey())
                    .processDefinitionId(processInstanceEvent.getBpmnProcessId())
                    .processDefinitionVersion(processInstanceEvent.getVersion())
                    .status("ACTIVE")
                    .build();

            log.info("Process started successfully with instance key: {}", processInstanceEvent.getProcessInstanceKey());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to start process: {}", request.getProcessDefinitionId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Publish a message to correlate with a process instance
     *
     * @param request PublishMessageRequest containing messageName, correlationKey, and variables
     * @return ResponseEntity with PublishMessageResponse containing message name and status
     */
    @PostMapping("/publish-message")
    public ResponseEntity<PublishMessageResponse> publishMessage(@RequestBody PublishMessageRequest request) {
        try {
            log.info("Publishing message: {} with correlation key: {}", request.getMessageName(), request.getCorrelationKey());

            var commandStep1 = camundaClient.newPublishMessageCommand()
                    .messageName(request.getMessageName())
                    .correlationKey(request.getCorrelationKey());

            // Add variables if provided
            var commandStep2 = commandStep1;
            if (request.getVariables() != null && !request.getVariables().isEmpty()) {
                commandStep2 = commandStep1.variables(request.getVariables());
            }

            // Add timeToLive if provided
            if (request.getTimeToLive() != null && request.getTimeToLive() > 0) {
                commandStep2 = commandStep2.timeToLive(Duration.ofMillis(request.getTimeToLive()));
            }

            commandStep2.send().join();

            var response = PublishMessageResponse.builder()
                    .messageName(request.getMessageName())
                    .correlationKey(request.getCorrelationKey())
                    .status("PUBLISHED")
                    .message("Message published successfully")
                    .build();

            log.info("Message published successfully: {}", request.getMessageName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to publish message: {}", request.getMessageName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
