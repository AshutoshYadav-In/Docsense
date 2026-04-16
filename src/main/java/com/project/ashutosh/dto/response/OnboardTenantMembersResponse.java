package com.project.ashutosh.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardTenantMembersResponse {

  private int added;
  private int skippedAlreadyMember;
  private List<String> emailsNotFound = new ArrayList<>();
}
