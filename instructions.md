I need you to design (not implement) a two-phase web application 
for a university department information system with RAG-based Q&A.
Produce a complete technical design document. 
Do not write any application code — only the design.

## Context

A web app for university students and staff to browse department 
information and ask questions about it. 
Multi-tenant from day one. The system is designed to serve 
multiple universities, each with multiple departments.
At launch only AUEB CS is populated with data, but the 
architecture must treat this as one tenant among many 
from the start — not an afterthought.

The hierarchy is:
University → Department → Section → Item → Detail

Examples of future tenants:
- AUEB / Statistics
- AUEB / Marketing  
- Another university / CS

Each university+department combination is an independent 
tenant with its own data files, its own Lucene index 
partition, and its own RAG vector store partition.
Adding a new department means dropping in a new set of 
JSON files — no code changes.

**Scale:** 10-15 users, ~20 documents, light query volume.
**Budget:** $0 — free tiers only.

## Stack (decided, do not change)

- Backend: Spring Boot 3 (Java 21), single fat JAR
- Search: Apache Lucene (embedded, no external server)
- RAG pipeline: LangChain4j
- LLM: Groq API (Llama 3.1 70B) via LangChain4j Groq client
- Embeddings: LangChain4j + HuggingFace Inference API 
  (intfloat/multilingual-e5-small)
- Vector store: LangChain4j in-memory store, 
  persisted to disk as JSON on shutdown, loaded on startup
- Frontend: Next.js (TypeScript)
- Hosting: Fly.io free tier 
  (2 machines: one frontend, one backend)
- Local dev: Docker Compose

## Two phases — design both together, clearly separated

### Phase 1 — Hierarchical information browser

Users navigate a tree of structured department data:

Department → section → item → detail

For CS department the tree is:
- Courses
  - Browse by semester (Α΄ through Η΄)
  - Browse by type (Mandatory / Core Elective / Elective)
  - Browse by study cycle (8 cycles: Data Science, Cybersecurity, etc.)
  - Course detail: code, name, ECTS, semester, type,
    prerequisites, instructor, short description, eclass URL
- Faculty
  - Browse by research sector (3 sectors)
  - Faculty member detail: name, rank, sector, courses taught
- Graduation Requirements
  - Structured display of all graduation rules
  - ECTS calculator: user inputs courses passed, 
    app shows remaining ECTS gap and what is still needed
- Academic Calendar
  - Semester dates, exam periods, holidays

Data source: a set of JSON files (one per section) 
loaded by the backend at startup. 
The JSON files are exported from the spreadsheet 
the admin maintains.
No database in phase 1 — JSON files on the filesystem, 
loaded into memory at startup.

### Phase 2 — Fuzzy search + RAG chatbot

Added to the same Spring Boot application, same frontend.

**Fuzzy search:**
- Apache Lucene index built over all structured data at startup
- Handles Greek text (correct analyzer choice for Greek)
- Fuzzy matching so "αλγοριθμοι" matches "Αλγόριθμοι", 
  student abbreviations and typos still find the right course
- Search endpoint returns ranked results with type 
  (course / faculty / requirement) and a snippet

**RAG chatbot:**
- User asks a free-text question in Greek or English
- Query is embedded via HuggingFace Inference API
- Vector search over embedded chunks of the structured data
- Top retrieved chunks assembled into context
- Groq API called with context + question + conversation history
- Answer streamed back to frontend
- Sources cited in the answer (which document/section each 
  chunk came from)

**Data chunking for RAG:**
- Same JSON files used in phase 1 are also the source for RAG
- Each course is one chunk, each faculty member is one chunk,
  each graduation rule is one chunk
- Chunks are embedded at startup and held in the 
  LangChain4j in-memory vector store
- Vector store serialized to disk so embeddings survive restarts
  without re-calling the HuggingFace API

## What the design document must cover

1. **System architecture diagram** (ASCII)
   - All components, how they connect
   - What runs on Fly.io, what is external API
   - Clear separation of phase 1 and phase 2 components
   - Data flow for: hierarchical browse, fuzzy search, RAG query

2. **Project file and folder structure**
   - Full tree for both frontend/ and backend/
   - One-line description of every file
   - Java package structure for the Spring Boot app
   - No boilerplate, only files that actually need to exist

3. **Data design**
   - JSON schema for each data file 
     (courses, faculty, requirements, calendar)
   - How the JSON structure maps to Lucene documents (phase 2)
   - How the JSON structure maps to RAG chunks (phase 2)
   - Vector store persistence format (what gets serialized to disk)

4. **API contract**
   Every endpoint: method, path, auth, 
   request shape, response shape, error cases.
   
Phase 1 endpoints:
- GET  /api/universities
- GET  /api/universities/{uni}/departments
- GET  /api/universities/{uni}/departments/{dept}/courses
- GET  /api/universities/{uni}/departments/{dept}/courses/{code}
- GET  /api/universities/{uni}/departments/{dept}/faculty
- GET  /api/universities/{uni}/departments/{dept}/requirements
- GET  /api/universities/{uni}/departments/{dept}/calendar
- POST /api/universities/{uni}/departments/{dept}/ects-calculator

Phase 2 endpoints:
- GET  /api/search?q=...&uni=...&dept=...
- POST /api/universities/{uni}/departments/{dept}/chat
- GET  /api/chat/{sessionId}/history

5. **Phase 1 — hierarchical browse design**
   - How JSON files are loaded and held in memory
   - How the ECTS calculator logic works 
     (input: list of passed course codes → 
      output: total ECTS, remaining by level, 
      missing mandatory courses, cycle completion status)
   - Frontend navigation design 
     (how the tree is traversed, URL structure, 
      what a course detail page shows)

6. **Phase 2 — Lucene fuzzy search design**
   - Which Lucene analyzer to use for Greek text
   - Index structure (fields per document type)
   - Query construction for fuzzy matching
   - How search results are ranked and returned
   - When the index is built (startup) and 
     whether it needs to be rebuilt on data change

7. **Phase 2 — RAG pipeline design**
   - Chunking strategy (one item per chunk, 
     what fields compose the chunk text)
   - Embedding strategy (batch size, 
     HuggingFace free tier rate limit handling)
   - Vector store: in-memory during runtime, 
     persistence/reload strategy across restarts
   - Retrieval: top-k value, similarity threshold, 
     metadata filtering by department
   - Prompt design: system prompt for Greek/English 
     mixed queries, how context chunks are inserted, 
     how sources are cited
   - Conversation history: how many turns kept, 
     how passed to Groq, session storage strategy 
     (in-memory map keyed by sessionId)
   - Streaming: how Groq streaming response is 
     forwarded to the Next.js frontend (SSE or chunked)

8. **Frontend design**
   - Page structure and routing (Next.js App Router)
   - Phase 1 UI: department picker, 
     navigation tree, course/faculty/requirement detail views,
     ECTS calculator form
   - Phase 2 UI: search bar with live results dropdown,
     chat panel (can be opened from any page),
     source citations display below each answer
   - How frontend calls backend in production 
     (env var for backend URL, CORS config)

9. **Fly.io deployment design**
   - fly.toml for backend (memory, CPU, health check path, 
     persistent volume mount for vector store + JSON data files)
   - fly.toml for frontend
   - All environment variables and secrets needed 
     (GROQ_API_KEY, HF_API_KEY, etc.)
   - How to update data files in production 
     (fly ssh or volume mount approach)

10. **Docker Compose design for local dev**
    - Services, ports, volumes, env vars
    - How to seed local JSON data files
    - How to match production environment

11. **Phase transition plan**
    - Exactly what is added in phase 2 vs phase 1
    - What does not change between phases
    - Order of implementation within each phase

12. **Key design decisions and trade-offs**
    - Why in-memory + disk persistence over a proper DB at this scale
    - Lucene embedded vs external search service
    - Groq rate limit risks and mitigation strategy
    - HuggingFace free tier embedding limits and 
      how the startup embedding strategy handles them
    - What breaks first if usage grows, 
      and what the upgrade path looks like

## Output format

A single structured markdown document.
Headers for each section above.
Tables for API contracts, JSON schemas, and data mappings.
ASCII diagrams for architecture and data flows.
No application code — pseudocode or plain English 
descriptions only where logic needs explaining.
Explicitly flag every decision that has a meaningful 
alternative with a one-line note on why this choice was made.