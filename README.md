# Docsense

| Repository | Description |
|------------|-------------|
| **Docsense** (this repo) | Spring Boot API — multi-tenant RAG platform with local embeddings, OpenSearch vector store, and AWS Bedrock for answers |
| **[Docsense-serverless](https://github.com/AshutoshYadav-In/Docsense-serverless)** | AWS Step Functions pipeline — async document ingestion (chunk, embed, bulk-insert vectors) |

Docsense is a multi-tenant RAG (Retrieval-Augmented Generation) platform built with Spring Boot.
Users upload documents, the system chunks and embeds them locally using a DJL PyTorch model,
stores the vectors in Amazon OpenSearch, and answers natural-language questions using AWS Bedrock.

> **Local development only.** The application runs an embedding model (all-MiniLM-L6-v2) in-process
> via DJL + PyTorch. Deploying this to a cloud VM or container will require a machine with enough
> CPU/RAM to run inference, and **the combined cost of OpenSearch, Bedrock, S3, Step Functions, RDS,
> and a compute instance large enough for local model inference will be significant.** Use this
> project for learning and local experimentation.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **Java** | 24+ | Required by `pom.xml` (`<java.version>24</java.version>`) |
| **Maven** | 3.9+ | Or use the included `./mvnw` wrapper |
| **PostgreSQL** | 14+ | Local or RDS; connection details go into Secrets Manager |
| **AWS Account** | Free tier is **not** enough | S3, Secrets Manager, OpenSearch, Step Functions, Bedrock |
| **AWS CLI** | v2 | Configured with a named profile or default credentials |

---

## AWS services used

| Service | Purpose |
|---------|---------|
| **Secrets Manager** | Stores DB credentials, JWT secret, internal client secret, OpenSearch URI |
| **S3** | Tenant-scoped document storage |
| **OpenSearch Service** | k-NN vector index for chunk embeddings |
| **Step Functions** | Async document ingestion pipeline (chunk + embed + index) |
| **Bedrock** | LLM for RAG answer generation after vector search |

---

## Setup

### 1. Clone and copy the example config

```bash
git clone <repo-url> && cd Docsense
cp src/main/resources/application.ex.properties src/main/resources/application.properties
```

Open `application.properties` and fill in your real values. The file is git-ignored.

### 2. Create the AWS Secrets Manager secret

Store a JSON object under the secret name you set in `aws.secret.name`:

```json
{
  "jwt_secret": {
    "secret": "<base64-encoded HS256 key, at least 256 bits>",
    "access_token_ttl_ms": 3600000
  },
  "database_credentials": {
    "url": "jdbc:postgresql://localhost:5432/docsense",
    "username": "docsense_app",
    "password": "<your-db-password>"
  },
  "client_secret": {
    "client_id": "internal-service",
    "client_token": "<a-long-random-token-for-internal-api>"
  },
  "elasticsearch_credentials": {
    "uri": "https://search-your-domain.ap-south-1.es.amazonaws.com"
  }
}
```

### 3. Create the PostgreSQL database and tables

```bash
createdb docsense
```

Run the migration scripts **in order** against the database:

```bash
psql -d docsense -f src/main/resources/db/001_tenants_and_members.sql
psql -d docsense -f src/main/resources/db/002_rename_organizations_to_tenants.sql
psql -d docsense -f src/main/resources/db/003_document_jobs.sql
psql -d docsense -f src/main/resources/db/004_document_jobs_number_of_chunks.sql
```

The `users` table must exist before `001` (it is referenced by foreign keys).
Create it first if your schema does not already have it:

```sql
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255),
  password VARCHAR(255) NOT NULL
);
```

### 4. Create the OpenSearch index

Apply the index template to your OpenSearch domain:

```bash
curl -X PUT "https://<your-opensearch-domain>/vector-store" \
  -H "Content-Type: application/json" \
  -d @src/main/resources/opensearch/document-chunks-index-template.json
```

If you use IAM-signed requests, use `awscurl` or the OpenSearch dashboard instead.

### 5. Configure AWS credentials

Either set the `aws.profile` property to a named profile, or leave it blank
to use the default credential chain (environment variables, instance profile, etc.):

```bash
# Option A: named profile
aws configure --profile your-profile

# Option B: environment variables
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=ap-south-1
```

### 6. Build and run

```bash
./mvnw clean compile
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="--enable-native-access=ALL-UNNAMED"
```

The `--enable-native-access` flag is required because DJL/PyTorch loads native libraries on Java 24+.

On **first startup**, the embedding model (~80 MB) is downloaded and cached locally. Subsequent
starts are faster.

The application starts on `http://localhost:8080` by default.

### 7. Run tests

```bash
./mvnw test
```

---

## API overview

All endpoints under `/api/**` (except auth) require a valid `Authorization: Bearer <jwt>` header.
Tenant-scoped endpoints additionally require an `X-Tenant-Id: <uuid>` header.

### Auth (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login; returns JWT |

### Users (JWT required, X-Tenant-Id required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/{id}` | Get user by id |
| DELETE | `/api/users/{id}` | Delete user |

### Tenants (JWT required)

| Method | Path | Headers | Description |
|--------|------|---------|-------------|
| POST | `/api/tenants/create` | JWT only | Create a new tenant |
| GET | `/api/tenants` | JWT only | List tenants for current user |
| POST | `/api/tenants/members` | JWT + X-Tenant-Id | Onboard members by email |

### Documents (JWT + X-Tenant-Id required)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tenants/files` | Upload a document (multipart) |

### Search (JWT + X-Tenant-Id required)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tenants/search` | RAG search across tenant documents |

### Internal (X-Client-Id + X-Client-Token, no JWT)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/internal/embed` | Batch text embedding |
| POST | `/api/internal/bulk-insert` | Bulk index chunks into OpenSearch |

---

## Project structure

```
com.project.ashutosh/
  config/        Spring @Configuration beans (security, DB, S3, Bedrock, etc.)
  controller/    REST controllers
  dao/           JPA data access (Criteria API repositories)
  dto/
    request/     Inbound request payloads
    response/    Outbound response payloads
    event/       Async pipeline events (Step Functions)
    common/      Shared domain shapes (ChunkSearchHit, RagAnswer, etc.)
  entity/        JPA entities (User, Tenant, TenantMember, DocumentJob)
  enums/         Enums (DocumentJobStatus)
  rag/           RAG prompt templates
  secret/        Secrets Manager deserialization POJOs
  security/      Filters, JWT, path rules, auth helpers
  service/       Business logic
  storage/       S3 key construction
  tenant/        Thread-local tenant context
  web/           MVC interceptors
```

---

## Key configuration reference

| Property | Description |
|----------|-------------|
| `aws.region` | AWS region for all services |
| `aws.secret.name` | Secrets Manager secret name (JSON with DB, JWT, client, OpenSearch creds) |
| `aws.profile` | (Optional) AWS CLI named profile |
| `aws.s3.bucket` | S3 bucket for document uploads |
| `aws.stepfunctions.state-machine-arn` | Step Functions state machine ARN |
| `aws.bedrock.model-id` | Bedrock model/inference profile for RAG answers |
| `aws.bedrock.max-output-tokens` | Max tokens for Bedrock response |
| `model.url` | DJL model URI (defaults to all-MiniLM-L6-v2) |
| `embedding.vector.dimensions` | Vector dimensions (384 for MiniLM) |
| `opensearch.index-name` | OpenSearch index name |
| `opensearch.search.top-k` | Max search results returned |
| `opensearch.search.min-relevance-score` | Cosine similarity floor (0-1) |
| `opensearch.search.knn-neighbors` | k-NN neighbor pool size |
