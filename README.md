# Grid07 Core API and Guardrails

Spring Boot 3 backend for the Grid07 intern assignment. It stores posts, comments and likes in SQL, while Redis acts as the real-time guardrail layer for virality score, bot reply limits, bot-human cooldowns and notification batching.

## Tech Used

- Java 17
- Spring Boot 3.5
- Spring Web, Validation, Data JPA
- Redis with `StringRedisTemplate`
- MySQL by default, using password `Zakir@123`
- PostgreSQL profile also included because the PDF asks for Postgres

## Run Locally

Start Redis and your preferred database:

```powershell
docker compose up -d redis mysql
```

If Docker is not installed, start a local Redis-compatible server manually on port `6379` before testing the post/comment/like endpoints.

Run the app:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at:

```text
http://localhost:8080
```

To run with PostgreSQL instead of MySQL:

```powershell
docker compose up -d redis postgres
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Demo data is created on startup: users `alice`, `rohan`, and `bot-1` to `bot-200`. This makes the 200-bot spam test easy to run without manually creating actors.

## Main Endpoints

```text
POST /api/posts
POST /api/posts/{postId}/comments
POST /api/posts/{postId}/like
GET  /api/posts/{postId}/virality
GET  /api/posts/{postId}/comments
POST /api/users
POST /api/bots
```

## Redis Guardrail Approach

The horizontal bot cap is protected with Redis `INCR` on:

```text
post:{id}:bot_count
```

`INCR` is atomic inside Redis. During concurrent requests, each request gets a different number. Only values `1` to `100` are allowed to insert a bot comment into the database. If the value is `101` or more, the service immediately runs `DECR` and returns HTTP `429 Too Many Requests`. Because the database write happens only after the Redis reservation succeeds, the DB cannot cross 100 bot comments for a post.

The cooldown cap uses Redis `SETNX` with TTL:

```text
cooldown:bot_{id}:human_{id}
```

If the key already exists, the bot-human interaction is rejected with HTTP `429`. The key expires after 10 minutes.

The vertical cap is checked before saving a comment. Any comment deeper than level 20 is rejected.

Virality is stored in Redis at:

```text
post:{id}:virality_score
```

Score changes:

- Bot reply: `+1`
- Human like: `+20`
- Human comment: `+50`

## Notification Batching

For bot comments on a human user's post, the first notification in a 15-minute window is logged immediately. Extra notifications are pushed into:

```text
user:{id}:pending_notifs
```

A scheduled sweeper runs every 5 minutes and logs one summary notification for each user with pending messages.

## Postman

Import `Grid07-Core-API.postman_collection.json` into Postman. The collection sets `postId` and `commentId` variables automatically after creating a post/comment.

## Notes

The app is stateless. Counters, cooldowns and pending notifications are stored only in Redis, never in Java memory.
