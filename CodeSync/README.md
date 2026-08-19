# CodeSync

Real-time collaborative coding platform built with Spring Boot and WebSockets.

## Run locally

```bash
cd CodeSync
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080), create a room, then open the same room ID in another tab to test live sync.

## Code execution engine (Piston) — required for the Run button

Running code (the ▶ Run button / `POST /execute`) does **not** execute anything on
this server directly. It forwards the request to
[Piston](https://github.com/engineer-man/piston), a sandboxed code-execution
engine, at the URL configured by `codesync.piston.base-url`
(`src/main/resources/application.properties`, default
`http://localhost:2000/api/v2`).

**If Run fails with "Execution service unavailable, please try again", Piston
isn't running yet.** Start it with Docker:

```bash
# from the repo root (where docker-compose.yml lives)
docker compose up -d
```

The first request for a given language can be slow/fail until that language's
runtime is installed in Piston. Install the ones you need:

```bash
# list available packages
curl http://localhost:2000/api/v2/packages

# install the ones you want to support, e.g.
curl -X POST http://localhost:2000/api/v2/packages -H "Content-Type: application/json" \
  -d '{"language": "python", "version": "3.12.0"}'
curl -X POST http://localhost:2000/api/v2/packages -H "Content-Type: application/json" \
  -d '{"language": "java", "version": "15.0.2"}'
curl -X POST http://localhost:2000/api/v2/packages -H "Content-Type: application/json" \
  -d '{"language": "node", "version": "18.15.0"}'
```

(Exact available versions come back from the `GET /api/v2/packages` call above —
use whatever version string it lists.) Once Piston is up and the languages you
need are installed, restart the Spring Boot app (or wait up to an hour — runtime
versions are cached) and Run will work.

Verify Piston directly:

```bash
curl http://localhost:2000/api/v2/runtimes
```

If that doesn't return JSON, Piston itself isn't reachable — check
`docker compose ps` / `docker logs piston_api`, and confirm nothing else is
bound to port 2000.

## Fix IDE red errors (Cursor / VS Code)

If files show red underlines but `mvn test` passes, the Java language server is not synced with Maven.

1. Install **Extension Pack for Java** (recommended in `.vscode/extensions.json`).
2. Open the workspace folder `CodeSync` (parent folder that contains the inner `CodeSync/` Maven project).
3. Run command palette: **Java: Clean Java Language Server Workspace** → Reload.
4. Run command palette: **Java: Force Java Compilation** → Full.

The Maven project lives at `CodeSync/CodeSync/pom.xml`. Settings in `.vscode/settings.json` point the Java importer to that path.

## Verify backend

```bash
cd CodeSync
./mvnw test
```

All tests should pass, including REST and WebSocket integration tests.

## Frontend (React + Monaco)

The React app lives in `frontend/` and proxies API/WebSocket calls to the backend during development.

```bash
# Terminal 1 — backend
cd CodeSync
./mvnw spring-boot:run

# Terminal 2 — frontend
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173), create a room, then open the same room ID in another tab to test live sync with syntax highlighting.

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/room/create` | Creates a room and returns `{ roomId, code }` |
| GET | `/room/{roomId}` | Returns room state; `404` if not found |
| WS | `/code?roomId={id}` | Real-time code sync |

### WebSocket message format

```json
{ "type": "CODE_UPDATE", "content": "your code here" }
```

Server responses:

- `SYNC` — sent once when a client joins (current room code)
- `CODE_UPDATE` — broadcast to other clients when someone edits
- `ERROR` — invalid message

## Project structure

```
src/main/java/com/codesync/
├── CodeSyncApplication.java      # Entry point
├── config/                       # Spring configuration
│   └── WebSocketConfig.java
├── controller/                   # REST endpoints
│   └── RoomController.java
├── service/                      # Business logic
│   └── RoomService.java
├── model/                        # Domain entities
│   └── Room.java
├── dto/                          # WebSocket / API payloads
│   ├── CodeMessage.java
│   └── MessageType.java
├── websocket/                    # Real-time handlers
│   └── CodeSocketHandler.java
└── exception/                    # Errors + global handler
    ├── RoomNotFoundException.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.properties
└── static/index.html             # Demo UI (replace with React later)
```

## What was fixed

- WebSocket handler is now a Spring bean and uses `RoomService`
- New joiners receive existing code via `SYNC`
- Edits are persisted in memory and broadcast to other users
- Thread-safe session and room storage
- Proper `roomId` query parsing
- Empty WebSocket room cleanup on disconnect
- Longer, collision-resistant room IDs (8 chars)
- REST `404` for missing rooms
- Structured JSON protocol instead of raw text
- SLF4J logging instead of `System.out`
- Package aligned to `com.codesync`
- Removed Lombok (plain getters/setters — fixes IDE false errors without Lombok plugin)

## Recommended next improvements

| Priority | Improvement | Why |
|----------|-------------|-----|
| High | React + Monaco Editor frontend | Better UX, syntax highlighting, separate SPA deploy |
| High | Redis / DB persistence | Survive restarts and scale horizontally |
| Medium | User presence (join/leave, cursors) | Core collab experience |
| Medium | Operational Transform or CRDT | Conflict-free concurrent edits |
| Medium | Room passwords / auth | Control who can join |
| Low | Rate limiting + max message size | Protect against abuse |
| Low | Docker + CI pipeline | Easier deployment |

## Suggested future layout (full product)

```
codesync/
├── CodeSync/         # Spring Boot backend
├── frontend/         # React + Monaco (Vite)
├── docker-compose.yml
└── README.md
```

Keep backend layers as they are now: **controller → service → model**, with **websocket** and **dto** as separate concerns. Add `repository/` when you introduce a database.
