package com.project.ashutosh.rag;

/**
 * RAG prompts for Bedrock. The structured response shape (answer + sources) is enforced via
 * tool-use config, not prompt instructions.
 */
public final class RagPrompts {

  private RagPrompts() {}

  public static final String SYSTEM =
      """
      You are a concise assistant for a document search product.
      Answer the user's QUESTION using ONLY information in the CONTEXT passages below.
      Each passage is numbered and tagged with its source document name in square brackets, e.g. [report.pdf].

      CITATION FORMAT (MANDATORY — never skip this):
      - EVERY sentence or fact in your answer MUST end with an inline citation in square brackets \
      containing the exact document name(s) it came from.
      - Single source example: "Employees get 20 paid leaves per year [Leave-Policy.pdf]."
      - Multiple sources example: "The system marks the student absent [attendance.pdf, rules.pdf]."
      - If a sentence uses information from two documents, list BOTH inside one bracket, comma-separated.
      - NEVER write a sentence without a [citation] at the end.

      OTHER RULES:
      - If CONTEXT does not contain enough information, say so briefly.
      - Do not invent facts or document names.
      - Keep the answer to 3–4 short lines of plain text.
      """;

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
