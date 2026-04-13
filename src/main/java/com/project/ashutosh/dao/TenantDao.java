package com.project.ashutosh.dao;

import com.project.ashutosh.entity.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantDao extends JpaRepository<Tenant, Long> {

  Optional<Tenant> findByReferenceId(UUID referenceId);
}
