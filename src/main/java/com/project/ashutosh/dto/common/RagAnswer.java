package com.project.ashutosh.dto.common;

import java.util.List;

/** Parsed AI response: the textual answer and the document names the model actually cited. */
public record RagAnswer(String answer, List<String> sources) {}
