# Deploy TalentForge Backend

This backend is ready to deploy as a Docker service.

## 1) Pre-deploy validation

From `talentforge/`:

```powershell
./mvnw.cmd test
```

If the app is already running locally on `8080`, run endpoint smoke tests:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-endpoints.ps1 -BaseUrl http://localhost:8080/api/v1 -CurlMaxTimeSeconds 10
```

## 2) Required infrastructure

- PostgreSQL database (required)
- Ollama (optional; app falls back gracefully when unavailable)

## 3) Required environment variables

Minimum required:

- `DB_URL` (example: `jdbc:postgresql://<host>:5432/talentforge`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (strong, unique value)

Recommended:

- `FRONTEND_PUBLIC_BASE_URL`
- `OLLAMA_BASE_URL` (if AI is enabled)
- `OLLAMA_MODEL` (default: `llama3.2:latest`)
- `AI_BIAS_TIMEOUT_MS` (default: `8000`)
- `AI_BIAS_MAX_RETRIES` (default: `1`)

Optional payment config:

- `PAYSTACK_SECRET_KEY`
- `PAYSTACK_WEBHOOK_SECRET`
- `PAYSTACK_CALLBACK_URL`

Use `.env.example` as the full variable reference.

## 4) Deploy on any Docker host

The repo includes a production Dockerfile at `talentforge/Dockerfile`.

Build and run locally:

```bash
docker build -t talentforge-backend .
docker run --env-file .env -p 8080:8080 talentforge-backend
```

Health check endpoint:

- `GET /actuator/health`

## 5) Docker Compose option

To run backend + postgres + ollama together from `talentforge/`:

```bash
docker compose --profile app up -d --build
```

This starts:

- `talentforge-backend` (port `8080`)
- `talentforge-postgres` (port `5432`)
- `talentforge-ollama` (port `11434`)
