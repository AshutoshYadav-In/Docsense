-- Upgrade path: if you previously ran 001_organizations_and_members.sql, run this once.
-- If you use 001_tenants_and_members.sql on a fresh DB, skip this file.

ALTER TABLE IF EXISTS organizations RENAME TO tenants;
ALTER TABLE IF EXISTS organization_members RENAME TO tenant_members;

ALTER TABLE tenant_members RENAME COLUMN organization_id TO tenant_id;

ALTER TABLE tenant_members DROP COLUMN IF EXISTS role;

DROP INDEX IF EXISTS idx_organizations_reference_id;
DROP INDEX IF EXISTS idx_organization_members_user;
DROP INDEX IF EXISTS idx_organization_members_org;

CREATE INDEX IF NOT EXISTS idx_tenants_reference_id ON tenants (reference_id);
CREATE INDEX IF NOT EXISTS idx_tenant_members_user ON tenant_members (user_id);
CREATE INDEX IF NOT EXISTS idx_tenant_members_tenant ON tenant_members (tenant_id);
