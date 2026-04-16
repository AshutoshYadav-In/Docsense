package com.project.ashutosh.service;

import ch.qos.logback.core.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ashutosh.dto.event.DocumentIngestionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;

@Service
public class StepFunctionIngestionService {

  @Value("${aws.stepfunctions.state-machine-arn:}")
  public String stateMachineArn;

  private final SfnClient sfnClient;
  private final ObjectMapper objectMapper;

  public StepFunctionIngestionService(SfnClient sfnClient, ObjectMapper objectMapper) {
    this.sfnClient = sfnClient;
    this.objectMapper = objectMapper;
  }

  public void publish(DocumentIngestionEvent event) {
    if (StringUtil.isNullOrEmpty(stateMachineArn)) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Step Functions state machine ARN not configured");
    }
    String input = toJson(event);
    StartExecutionRequest request = StartExecutionRequest.builder().stateMachineArn(stateMachineArn)
        .name(event.referenceId().toString()).input(input).build();
    sfnClient.startExecution(request);
  }

  private String toJson(DocumentIngestionEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize ingestion event", e);
    }
  }
}
