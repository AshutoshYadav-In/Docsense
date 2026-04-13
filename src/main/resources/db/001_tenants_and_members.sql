-- Tenants: internal surrogate id + external reference_id (sent in X-Tenant-Id header).
-- Run manually against PostgreSQL (spring.jpa.hibernate.ddl-auto=none).

CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY,
  reference_id UUID NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenants_reference_id ON tenants (reference_id);

CREATE TABLE IF NOT EXISTS tenant_members (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  UNIQUE (tenant_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_tenant_members_user ON tenant_members (user_id);
CREATE INDEX IF NOT EXISTS idx_tenant_members_tenant ON tenant_members (tenant_id);
