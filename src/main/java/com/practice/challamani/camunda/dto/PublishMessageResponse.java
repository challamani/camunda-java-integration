package com.practice.challamani.camunda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishMessageResponse {

    @JsonProperty("messageName")
    private String messageName;

    @JsonProperty("correlationKey")
    private String correlationKey;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;
}

