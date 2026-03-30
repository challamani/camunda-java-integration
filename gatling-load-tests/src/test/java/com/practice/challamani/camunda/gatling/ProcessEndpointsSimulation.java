package com.practice.challamani.camunda.gatling;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ProcessEndpointsSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8082");
    private static final String PROCESS_DEFINITION_ID = System.getProperty("processDefinitionId", "OrderProcessDefinition_v2");
    private static final String MESSAGE_NAME = System.getProperty("messageName", "Message_Confirmation");

    private static final int USERS = Integer.getInteger("users", 10);
    private static final int RAMP_SECONDS = Integer.getInteger("rampSeconds", 5);
    private static final long PAUSE_MILLIS = Long.getLong("pauseMillis", 50L);
    private static final long TIME_TO_LIVE_MILLIS = Long.getLong("timeToLiveMillis", 300000L);

    private static final String START_PROCESS_BODY = """
            {
              "processDefinitionId": "%s",
              "variables": {
                "orderId": "#{orderId}"
              }
            }
            """.formatted(PROCESS_DEFINITION_ID);

    private static final String PUBLISH_MESSAGE_BODY = """
            {
              "messageName": "%s",
              "correlationKey": "#{orderId}",
              "variables": {
                "orderId": "#{orderId}",
                "approvedBy": "#{approvedBy}",
                "processInstanceKey": "#{processInstanceKey}"
              },
              "timeToLive": %d
            }
            """.formatted(MESSAGE_NAME, TIME_TO_LIVE_MILLIS);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final ScenarioBuilder processFlowScenario = scenario("Start process then publish message")
            .feed(Stream.generate(() -> Map.<String, Object>of(
                    "orderId", "ORDER-" + UUID.randomUUID(),
                    "approvedBy", "gatling"
            )).iterator())
            .exec(http("start-process")
                    .post("/api/processes/start")
                    .body(StringBody(START_PROCESS_BODY))
                    .asJson()
                    .check(status().is(201))
                    .check(jsonPath("$.processInstanceKey").saveAs("processInstanceKey")))
            .pause(Duration.ofMillis(PAUSE_MILLIS))
            .exec(http("publish-message")
                    .post("/api/processes/publish-message")
                    .body(StringBody(PUBLISH_MESSAGE_BODY))
                    .asJson()
                    .check(status().is(200)));

    public ProcessEndpointsSimulation() {
        OpenInjectionStep injectionStep = RAMP_SECONDS > 0
                ? rampUsers(USERS).during(Duration.ofSeconds(RAMP_SECONDS))
                : atOnceUsers(USERS);

        setUp(processFlowScenario.injectOpen(injectionStep)).protocols(httpProtocol);
    }
}
