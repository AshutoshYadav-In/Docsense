package com.project.ashutosh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ashutosh.dto.ChunkSearchHit;
import com.project.ashutosh.dto.RagAnswer;
import com.project.ashutosh.rag.RagPrompts;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AnyToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

@Service
public class RagBedrockService {

  private static final Logger log = LoggerFactory.getLogger(RagBedrockService.class);

  private static final String TOOL_NAME = "rag_answer";

  private static final Document TOOL_SCHEMA = Document.mapBuilder()
      .putString("type", "object")
      .putDocument("properties", Document.mapBuilder()
          .putDocument("answer", Document.mapBuilder()
              .putString("type", "string")
              .putString("description",
                  "A concise 3-4 line plain-text answer based only on the provided context. "
                      + "EVERY sentence MUST end with an inline citation in square brackets, e.g. "
                      + "'Employees get 20 leaves [Leave-Policy.pdf].' or "
                      + "'Students are marked absent [attendance.pdf, rules.pdf].' "
                      + "Never write a sentence without [docname] at the end.")
              .build())
          .putDocument("sources", Document.mapBuilder()
              .putString("type", "array")
              .putString("description",
                  "Unique document names (from the [brackets] in context) whose content was "
                      + "actually used in the answer. No duplicates.")
              .putDocument("items", Document.mapBuilder()
                  .putString("type", "string")
                  .build())
              .build())
          .build())
      .putDocument("required", Document.fromList(List.of(
          Document.fromString("answer"),
          Document.fromString("sources"))))
      .build();

  /**
   * Uses {@code toolChoice = any} instead of forcing a specific tool name. Some models (e.g. GLM)
   * support tool use but ignore {@code toolChoice.tool} (specific); {@code any} forces the model
   * to invoke a tool without naming it — since we only define one, it will use {@code rag_answer}.
   */
  private static final ToolConfiguration TOOL_CONFIG = ToolConfiguration.builder()
      .tools(Tool.fromToolSpec(ToolSpecification.builder()
          .name(TOOL_NAME)
          .description("You MUST call this tool to return your answer. Provide the RAG answer "
              + "(with inline [docname] citations) and the source document names used.")
          .inputSchema(ToolInputSchema.fromJson(TOOL_SCHEMA))
          .build()))
      .toolChoice(ToolChoice.fromAny(AnyToolChoice.builder().build()))
      .build();

  private final BedrockRuntimeClient bedrockRuntimeClient;
  private final ObjectMapper objectMapper;

  @Value("${aws.bedrock.model-id}")
  private String modelId;

  @Value("${aws.bedrock.max-output-tokens:400}")
  private int maxOutputTokens;

  public RagBedrockService(BedrockRuntimeClient bedrockRuntimeClient, ObjectMapper objectMapper) {
    this.bedrockRuntimeClient = bedrockRuntimeClient;
    this.objectMapper = objectMapper;
  }

  public RagAnswer answerFromChunks(String question, List<ChunkSearchHit> hits) {
    if (hits.isEmpty()) {
      return new RagAnswer(
          "No passages matched your query above the relevance threshold. "
              + "Try different wording or check that documents are indexed.",
          List.of());
    }

    String contextBlock = buildNumberedContext(hits);
    String userMessage = RagPrompts.buildUserMessage(question, contextBlock);

    ConverseRequest request = ConverseRequest.builder().modelId(modelId)
        .system(SystemContentBlock.fromText(RagPrompts.SYSTEM))
        .messages(Message.builder().role(ConversationRole.USER).content(ContentBlock.fromText(userMessage)).build())
        .toolConfig(TOOL_CONFIG)
        .inferenceConfig(InferenceConfiguration.builder().maxTokens(maxOutputTokens).temperature(0.2f).build()).build();

    try {
      ConverseResponse response = bedrockRuntimeClient.converse(request);
      return extractResult(response);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Bedrock generation failed: " + e.getMessage(), e);
    }
  }

  private RagAnswer extractResult(ConverseResponse response) {
    if (response == null || response.output() == null || response.output().message() == null) {
      return new RagAnswer("", List.of());
    }

    List<ContentBlock> blocks = response.output().message().content();
    for (ContentBlock block : blocks) {

      ToolUseBlock toolUse;
      try {
        toolUse = block.toolUse();
      } catch (Exception e) {
        continue;
      }

      if (toolUse != null) {
        if (TOOL_NAME.equals(toolUse.name()) && toolUse.input() != null) {
          try {
            return objectMapper.convertValue(toolUse.input().unwrap(), RagAnswer.class);
          } catch (IllegalArgumentException e) {
            log.warn("Failed to deserialize tool input into RagAnswer", e);
          }
        }
      }
    }

    log.warn("No tool_use block found; stopReason={}, blockCount={}, blockTypes={}", response.stopReasonAsString(),
        blocks.size(), blocks.stream().map(b -> String.valueOf(b.type())).toList());
    return new RagAnswer("", List.of());
  }

  private static String buildNumberedContext(List<ChunkSearchHit> hits) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < hits.size(); i++) {
      ChunkSearchHit h = hits.get(i);
      String body = h.text() == null ? "" : h.text().replace("\r", " ").trim();
      sb.append(i + 1)
          .append(". [")
          .append(h.fileName() == null || h.fileName().isBlank() ? "doc" : h.fileName())
          .append("] ")
          .append(body)
          .append("\n\n");
    }
    return sb.toString();
  }
}
