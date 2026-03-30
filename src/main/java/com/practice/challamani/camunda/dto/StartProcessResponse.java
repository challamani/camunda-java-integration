package com.practice.challamani.camunda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for process instance creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartProcessResponse {

    @JsonProperty("processInstanceKey")
    private Long processInstanceKey;

    @JsonProperty("processDefinitionId")
    private String processDefinitionId;

    @JsonProperty("processDefinitionVersion")
    private Integer processDefinitionVersion;

    @JsonProperty("status")
    private String status;
}

