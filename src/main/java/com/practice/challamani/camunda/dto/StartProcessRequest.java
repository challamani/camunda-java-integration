package com.practice.challamani.camunda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for starting a process instance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartProcessRequest {

    @JsonProperty("processDefinitionId")
    private String processDefinitionId;

    @JsonProperty("variables")
    private Map<String, Object> variables;
}

