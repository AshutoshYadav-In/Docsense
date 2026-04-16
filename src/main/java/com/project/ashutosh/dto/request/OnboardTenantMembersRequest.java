package com.project.ashutosh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardTenantMembersRequest {

  @NotNull
  @NotEmpty
  private Set<@NotBlank String> emails = new LinkedHashSet<>();
}
