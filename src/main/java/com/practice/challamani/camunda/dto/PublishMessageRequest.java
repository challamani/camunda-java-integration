package com.practice.challamani.camunda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for publishing a message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishMessageRequest {

    @JsonProperty("messageName")
    private String messageName;

    @JsonProperty("correlationKey")
    private String correlationKey;

    @JsonProperty("variables")
    private Map<String, Object> variables;

    @JsonProperty("timeToLive")
    private Long timeToLive;
}

