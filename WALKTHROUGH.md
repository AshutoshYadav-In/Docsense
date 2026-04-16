# Docsense -- Demo Walkthrough

This guide walks you through the full user flow: from creating an account to asking questions
about your uploaded documents. Every step includes the exact curl command you can copy-paste.

Make sure the application is running locally on `http://localhost:8080` before you start.
See [README.md](README.md) for setup instructions.

---

## Step 1: Register a user

Create your first user account. This is the only step that does not require any token.

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "SecurePass123",
    "name": "Alice"
  }'
```

You will get back a JWT token in the response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiresInMs": 3600000
}
```

Save the `token` value -- you will need it for every subsequent request.

---

## Step 2: Log in (optional, for future sessions)

If your token expires, log in again to get a fresh one:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "SecurePass123"
  }'
```

Same response shape as register. Use the new token going forward.

---

## Step 3: Create a tenant (workspace)

A tenant is a workspace that groups users and documents together. Create one:

```bash
curl -s -X POST http://localhost:8080/api/tenants/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "name": "My Research Team"
  }'
```

Response:

```json
{
  "id": 1,
  "referenceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "My Research Team"
}
```

Save the `referenceId` -- this is your **tenant ID** used in the `X-Tenant-Id` header
for all tenant-scoped operations.

---

## Step 4: List your tenants

Confirm you can see the tenant you just created:

```bash
curl -s -X GET http://localhost:8080/api/tenants \
  -H "Authorization: Bearer <your-token>"
```

Returns an array of all tenants you belong to.

---

## Step 5: Add another user to the tenant (optional)

If you registered a second user (say `bob@example.com`), you can add them to the tenant:

```bash
curl -s -X POST http://localhost:8080/api/tenants/members \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -H "X-Tenant-Id: <tenant-referenceId>" \
  -d '{
    "emails": ["bob@example.com"]
  }'
```

Response:

```json
{
  "added": 1,
  "skippedAlreadyMember": 0,
  "emailsNotFound": []
}
```

---

## Step 6: Upload a document

Now upload a file (PDF, text, etc.) to your tenant. The system will store it in S3 and
kick off an async ingestion pipeline that chunks the document, generates embeddings, and
indexes them into OpenSearch.

```bash
curl -s -X POST http://localhost:8080/api/tenants/files \
  -H "Authorization: Bearer <your-token>" \
  -H "X-Tenant-Id: <tenant-referenceId>" \
  -F "file=@/path/to/your/document.pdf"
```

Response:

```json
{
  "referenceId": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "folderName": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "fileName": "document.pdf",
  "bucket": "your-s3-bucket",
  "s3Key": "tenants/<tenant-id>/f1e2.../document.pdf",
  "s3FolderPrefix": "tenants/<tenant-id>/f1e2.../"
}
```

The upload itself is instant. The chunking, embedding, and indexing happen asynchronously
via AWS Step Functions. Wait a minute or two for the pipeline to finish before searching.

---

## Step 7: Search your documents

This is the main feature. Ask a natural-language question and the system will:
1. Embed your question using the local model
2. Find the most relevant document chunks via vector search in OpenSearch
3. Send those chunks + your question to AWS Bedrock
4. Return an AI-generated answer with source citations

```bash
curl -s -X POST http://localhost:8080/api/tenants/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -H "X-Tenant-Id: <tenant-referenceId>" \
  -d '{
    "query": "What are the main findings in the document?"
  }'
```

Response:

```json
{
  "query": "What are the main findings in the document?",
  "answer": "Based on the uploaded document, the main findings are...",
  "sources": ["document.pdf"]
}
```

The `sources` field tells you which uploaded documents the AI used to generate its answer.

---

## Full flow at a glance

```
Register/Login
      |
      v
  Get JWT token
      |
      v
  Create a tenant ───> Save the tenant referenceId
      |
      v
  Upload documents ───> Async pipeline: chunk -> embed -> index
      |
      (wait ~1-2 min)
      |
      v
  Search with questions ───> AI answer + source citations
```

---

## Tips

- **Token expired?** Just call `/api/auth/login` again to get a fresh JWT.
- **Multiple documents?** Upload as many files as you like. They all get indexed
  under the same tenant and search queries will find relevant chunks across all of them.
- **Different tenants?** Documents are isolated per tenant. A search in Tenant A
  will never return results from Tenant B.
- **Ingestion not working?** Check that your Step Functions state machine is set up
  and that the internal API credentials (`client_secret` in Secrets Manager) match
  what the state machine uses to call `/api/internal/embed` and `/api/internal/bulk-insert`.
- **Empty search results?** The ingestion pipeline may still be running. Wait a minute
  and try again. Also check that `opensearch.search.min-relevance-score` is not set
  too high for your content.
