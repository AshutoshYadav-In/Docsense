package com.project.ashutosh.service;

import com.project.ashutosh.dto.ChunkSearchHit;
import com.project.ashutosh.rag.RagPrompts;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

@Service
public class RagBedrockService {

  private final BedrockRuntimeClient bedrockRuntimeClient;

  @Value("${aws.bedrock.model-id}")
  private String modelId;

  @Value("${aws.bedrock.max-output-tokens:400}")
  private int maxOutputTokens;

  public RagBedrockService(BedrockRuntimeClient bedrockRuntimeClient) {
    this.bedrockRuntimeClient = bedrockRuntimeClient;
  }

  /**
   * Calls Bedrock with the user question and retrieved chunk texts. If {@code hits} is empty,
   * returns a short message without calling the model.
   */
  public String answerFromChunks(String question, List<ChunkSearchHit> hits) {
    if (hits.isEmpty()) {
      return "No passages matched your query above the relevance threshold. Try different wording "
          + "or check that documents are indexed.";
    }
    String contextBlock = buildNumberedContext(hits);
    String userMessage = RagPrompts.buildUserMessage(question, contextBlock);

    ConverseRequest request = ConverseRequest.builder().modelId(modelId)
        .system(SystemContentBlock.fromText(RagPrompts.SYSTEM))
        .messages(Message.builder().role(ConversationRole.USER).content(ContentBlock.fromText(userMessage)).build())
        .inferenceConfig(InferenceConfiguration.builder().maxTokens(maxOutputTokens).temperature(0.2f).build()).build();

    try {
      ConverseResponse response = bedrockRuntimeClient.converse(request);
      return extractAssistantText(response);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Bedrock generation failed: " + e.getMessage(), e);
    }
  }

  private static String buildNumberedContext(List<ChunkSearchHit> hits) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < hits.size(); i++) {
      ChunkSearchHit h = hits.get(i);
      String body = h.text() == null ? "" : h.text().replace("\r", " ").trim();
      sb.append(i + 1).append(". [").append(h.fileName() == null || h.fileName().isBlank() ? "doc" : h.fileName())
          .append("] ").append(body).append("\n\n");
    }
    return sb.toString();
  }

  private static String extractAssistantText(ConverseResponse response) {
    if (response == null || response.output() == null || response.output().message() == null) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (ContentBlock block : response.output().message().content()) {
      if (block.text() != null && !block.text().isBlank()) {
        if (out.length() > 0) {
          out.append('\n');
        }
        out.append(block.text());
      }
    }
    return out.toString().trim();
  }
}
