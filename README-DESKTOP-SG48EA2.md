# Department Information System

A multi-tenant university information platform for browsing department data, understanding requirements, and asking questions in natural language.

The first live tenant is AUEB Computer Science, but the architecture is designed from day one for many universities and departments, each with its own data, search index, and RAG space.

Status: Phase 1 is live and working. Phase 2 adds fuzzy search and a RAG-based chatbot.

Live demo: https://deptinfo-aueb-web.fly.dev

## What we offer

This project is a modern digital front door for a department:

- Browse structured academic information by hierarchy
  - University → Department → Section → Item → Detail
- Explore courses, faculty, graduation rules, and the academic calendar
- Use a graduation ECTS calculator to see what remains to complete a program
- Search across course and academic content with typo-tolerant fuzzy matching
- Ask questions in Greek or English through a chatbot grounded in department data
- Support multiple tenants without changing application logic

In short, we are building a reliable, low-cost, department-aware information system that helps students and staff find what they need quickly and accurately.

## How it works

The system is split into a public web experience and a private backend data layer.

1. The frontend is a Next.js application that presents the department information in a familiar, navigable structure.
2. The backend is a Spring Boot API that loads validated JSON data at startup and serves it read-only.
3. The same data is also indexed for search and embedded for retrieval-augmented generation (RAG).
4. Users can browse the structure directly, or ask free-form questions that are answered with supported sources from the tenant's data.
5. The app runs in Docker locally and is deployed on Fly.io in production.

This gives a clean separation between:

- human-friendly browsing
- structured API access
- indexing and search
- AI-powered Q&A grounded in the department's official data

## Architecture at a glance

```text
Browser
  |
  v
Next.js frontend
  |  (HTTPS, public app)
  |-- proxies /api/* to the private backend
  v
Spring Boot backend
  |-- loads tenant JSON data from disk
  |-- validates data before serving it
  |-- exposes REST endpoints for browse + calculator
  |-- builds Lucene index for fuzzy search
  |-- creates embeddings and vector store for RAG
  |
  +--> Data files (tenant JSON)
  +--> Lucene index (embedded)
  +--> Vector store (in-memory + local persistence)
  |
  +--> Groq API (LLM chat)
  +--> HuggingFace Inference API (embeddings)
```

The backend is not exposed publicly. The frontend sits in front of it, which keeps admin operations and internal APIs off the public internet.

## Product phases

### Phase 1 — Hierarchical information browser

The current, live experience focuses on browsing and understanding a department's academic structure.

Included features:

- Department navigation tree
- Course browsing by level, semester, and study cycle
- Faculty directory and details
- Graduation requirements and rule breakdown
- ECTS calculator for remaining credits and graduation readiness
- Academic calendar and semester information

### Phase 2 — Search and assistant

Planned expansion for deeper discovery and natural-language support.

Included features:

- Fuzzy, typo-tolerant search across structured academic data
- Lucene-based indexing for Greek text and common student mistakes
- RAG chatbot for answering questions from department data
- Source-aware answers with citations to the relevant department content
- Session-based chat history

## Multi-tenant design

This is not a single-department app disguised as a platform.

From the start, the architecture supports:

- University → Department structure
- Independent tenant data folders
- Separate search indexes per tenant
- Separate vector-store partitions per tenant
- No code changes required when adding a new department

Example tenants:

- AUEB / Computer Science
- AUEB / Statistics
- AUEB / Marketing
- Another university / CS

Each new tenant is just another data set in the configured folder structure.

## Key technologies

- Frontend: Next.js + TypeScript
- Backend: Spring Boot 3 + Java 21
- Search: Apache Lucene
- AI: LangChain4j
- LLM: Groq API
- Embeddings: Hugging Face Inference API
- Deployment: Fly.io
- Local development: Docker Compose

## Repository layout

```text
chat-data/
  README.md
  DESIGN.md
  DEPLOY.md
  instructions.md
  docker-compose.yml
  docker-compose.dev.yml
  .env.example
  data-seed/
    tenants/
      aueb/
        cs/
          tenant.json
          courses.json
          faculty.json
          requirements.json
          calendar.json
          DATA-NOTES.md
  backend/
    src/
    pom.xml
    Dockerfile
  frontend/
    src/
    package.json
  deploy/
  docs/
    architecture.png
    architecture.svg
    design-detailed-archive.md
```

## Quick start

### Run locally with Docker

```powershell
# from the project root
docker compose up --build
```

Then open:

- Frontend: http://localhost:3000
- Backend: http://localhost:8080

### Run the app without Docker

See the instructions in the project docs and deployment notes for the environment-specific Java and Node setup.

## Documentation

This project has layered documentation to match the product maturity:

- [DESIGN.md](./DESIGN.md) — short architecture and design overview
- [docs/design-detailed-archive.md](./docs/design-detailed-archive.md) — detailed technical design archive
- [DEPLOY.md](./DEPLOY.md) — deployment and production update steps
- [docs/architecture.svg](./docs/architecture.svg) — editable architecture diagram
- [docs/architecture.png](./docs/architecture.png) — exported architecture image

## Why this project matters

The goal is simple but important:

- make academic information easy to browse
- reduce confusion and manual lookup
- support students and staff with a trustworthy department portal
- do it in a way that scales across multiple universities and departments without rewriting the system

This makes the project useful as a digital service layer for a university, not just a one-off website.

## Notes

- The app loads JSON data into memory for fast read access.
- Data validation is built in so bad or inconsistent content fails loudly rather than silently reaching the public interface.
- Phase 2 is designed to be additive: it extends the same product without breaking the Phase 1 experience.
- The primary live tenant is AUEB CS; additional tenants are supported through the data-driven architecture.
