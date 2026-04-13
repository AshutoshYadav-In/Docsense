package com.project.ashutosh.dao;

import com.project.ashutosh.entity.TenantMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantMemberDao extends JpaRepository<TenantMember, Long>, TenantMemberDaoCustom {}
