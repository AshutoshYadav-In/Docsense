package com.project.ashutosh.rag;

/**
 * Default RAG prompts. For production, tighten style/guardrails and keep CONTEXT structured
 * (numbered passages work well with Claude on Bedrock).
 *
 * <p><b>How many chunks?</b> Typical RAG uses <b>3–5</b> retrieved passages: fewer miss nuance; more
 * adds noise and tokens. This app uses the same limit as vector search ({@code opensearch.search.top-k},
 * default 5).
 */
public final class RagPrompts {

  private RagPrompts() {}

  /**
   * System instruction: answer only from context, short output. Pair with a user message that
   * contains QUESTION + numbered CONTEXT block.
   */
  public static final String SYSTEM =
      """
      You are a concise assistant for a document search product.
      Answer the user's QUESTION using ONLY information in the CONTEXT passages below.
      If CONTEXT does not contain enough information, say briefly that it is not stated in the provided passages.
      Do not invent facts or sources.
      Reply in at most 3–4 short lines of plain text (no markdown headings, no bullet lists unless essential).
      """;

  /**
   * Builds the user message: repeats the question and appends numbered passages from retrieval.
   */
  public static String buildUserMessage(String question, String numberedContextBlock) {
    return """
        QUESTION:
        %s

        CONTEXT (numbered passages from search; use only this material):
        %s
        """
        .formatted(question, numberedContextBlock);
  }
}
