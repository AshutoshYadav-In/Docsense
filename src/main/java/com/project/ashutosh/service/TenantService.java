package com.project.ashutosh.service;

import ch.qos.logback.core.util.StringUtil;
import com.project.ashutosh.dao.TenantDao;
import com.project.ashutosh.dao.TenantMemberDao;
import com.project.ashutosh.dao.UserDao;
import com.project.ashutosh.dto.CreateTenantRequest;
import com.project.ashutosh.dto.OnboardTenantMembersResponse;
import com.project.ashutosh.dto.TenantResponse;
import com.project.ashutosh.entity.Tenant;
import com.project.ashutosh.entity.TenantMember;
import com.project.ashutosh.entity.User;
import com.project.ashutosh.security.CurrentUserIdProvider;
import com.project.ashutosh.tenant.TenantContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TenantService {

  private final TenantDao tenantDao;
  private final TenantMemberDao tenantMemberDao;
  private final UserDao userDao;
  private final TenantContext tenantContext;
  private final CurrentUserIdProvider currentUserIdProvider;

  public TenantService(
      TenantDao tenantDao,
      TenantMemberDao tenantMemberDao,
      UserDao userDao,
      TenantContext tenantContext,
      CurrentUserIdProvider currentUserIdProvider) {
    this.tenantDao = tenantDao;
    this.tenantMemberDao = tenantMemberDao;
    this.userDao = userDao;
    this.tenantContext = tenantContext;
    this.currentUserIdProvider = currentUserIdProvider;
  }

  public TenantResponse createTenant(CreateTenantRequest request) {
    if (StringUtil.isNullOrEmpty(request.getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
    }
    long requesterUserId = currentUserIdProvider.requireUserId();
    Tenant tenant = new Tenant();
    tenant.setReferenceId(UUID.randomUUID());
    tenant.setName(request.getName());
    Tenant saved = tenantDao.save(tenant);

    User requester = userDao.findById(requesterUserId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requester user not found"));
    TenantMember membership = new TenantMember();
    membership.setTenantId(saved.getId());
    membership.setUserId(requester.getId());
    tenantMemberDao.save(membership);

    return toResponse(saved);
  }

  public List<TenantResponse> listTenantsForCurrentUser() {
    long userId = currentUserIdProvider.requireUserId();
    return tenantMemberDao.findTenantsByUserId(userId).stream().map(this::toResponse).toList();
  }

  public OnboardTenantMembersResponse onboardEmails(Set<String> emails) {
    Long tenantInternalId = tenantContext.getTenantId();
    if (tenantInternalId == null) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Tenant context missing after tenant filter");
    }
    if (CollectionUtils.isEmpty(emails)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emails required");
    }

    List<User> users = userDao.findByEmailIn(emails);
    Map<String, User> userByEmail =
        users.stream().collect(Collectors.toMap(User::getEmail, u -> u, (a, b) -> a));

    List<String> notFound = new ArrayList<>();
    for (String email : emails) {
      if (!userByEmail.containsKey(email)) {
        notFound.add(email);
      }
    }

    if (CollectionUtils.isEmpty(users)) {
      return new OnboardTenantMembersResponse(0, 0, notFound);
    }

    List<Long> candidateIds = users.stream().map(User::getId).toList();
    Set<Long> alreadyMemberIds =
        new HashSet<>(
            tenantMemberDao.findMemberUserIdsByTenantIdAndUserIdIn(tenantInternalId, candidateIds));

    List<TenantMember> toSave = new ArrayList<>();
    int skipped = 0;
    for (User u : users) {
      if (alreadyMemberIds.contains(u.getId())) {
        skipped++;
        continue;
      }
      TenantMember row = new TenantMember();
      row.setTenantId(tenantInternalId);
      row.setUserId(u.getId());
      toSave.add(row);
    }
    tenantMemberDao.saveAll(toSave);

    return new OnboardTenantMembersResponse(toSave.size(), skipped, notFound);
  }

  private TenantResponse toResponse(Tenant t) {
    return new TenantResponse(t.getId(), t.getReferenceId(), t.getName());
  }
}
