-- Document upload jobs (async processing correlation). Run manually (spring.jpa.hibernate.ddl-auto=none).

CREATE TABLE IF NOT EXISTS document_jobs (
  id BIGSERIAL PRIMARY KEY,
  reference_id UUID NOT NULL UNIQUE,
  creator_id BIGINT NOT NULL REFERENCES users (id),
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_document_jobs_creator ON document_jobs (creator_id);
