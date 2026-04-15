-- Adds nullable chunk count; filled when ingestion completes.
ALTER TABLE document_jobs
ADD COLUMN IF NOT EXISTS number_of_chunks INTEGER;
