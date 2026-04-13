-- Add email when upgrading an existing `users` table (password column may already exist).
-- Option A — empty table
-- ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE NOT NULL;

-- Option B — existing rows: add nullable, backfill, then NOT NULL + UNIQUE
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
-- UPDATE users SET email = 'user' || id || '@example.com' WHERE email IS NULL;
-- ALTER TABLE users ALTER COLUMN email SET NOT NULL;
-- CREATE UNIQUE INDEX IF NOT EXISTS users_email_key ON users (email);
