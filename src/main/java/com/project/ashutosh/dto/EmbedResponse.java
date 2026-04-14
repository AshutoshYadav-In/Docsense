package com.project.ashutosh.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedResponse {

  private String chunkText;
  private List<Float> embedding;

  public static EmbedResponse of(String chunkText, float[] embedding) {
    List<Float> list = new ArrayList<>(embedding.length);
    for (float v : embedding) {
      list.add(v);
    }
    return new EmbedResponse(chunkText, list);
  }
}
