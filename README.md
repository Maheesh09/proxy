# ProxyMaze

A real-world backend monitoring system for proxy servers built with **Spring Boot 3 + Java 21**.

## Architecture

```
REST API → Service Layer → DataStore (ConcurrentHashMap)
                ↑
    MonitoringScheduler (background thread, every N seconds)
                ↓
    ProxyProbeService (real HTTP via OkHttp)
                ↓
    AlertService (lifecycle: ACTIVE → RESOLVED → NEW ACTIVE)
                ↓
    WebhookDeliveryService (retry + exactly-once delivery)
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /health | Health check |
| GET | /config | Get current config |
| POST | /config | Update config (dynamically restarts scheduler) |
| POST | /proxies | Add proxy URLs to monitor |
| GET | /proxies | List all proxies with live status |
| GET | /proxies/{id} | Get single proxy |
| GET | /proxies/{id}/history | Check history |
| DELETE | /proxies | Clear all proxies |
| DELETE | /proxies/{id} | Remove one proxy |
| GET | /alerts | List all alerts |
| GET | /alerts/active | Get current active alert |
| POST | /webhooks | Register webhook receiver |
| GET | /webhooks | List webhooks |
| DELETE | /webhooks/{id} | Remove webhook |
| POST | /integrations | Register Slack/Discord integration |
| GET | /integrations | List integrations |
| DELETE | /integrations/{id} | Remove integration |
| GET | /metrics | System metrics |

## Running Locally

```bash
mvn clean package -DskipTests
java -jar target/proxymaze-1.0.0.jar
```

## Docker

```bash
docker build -t proxymaze .
docker run -p 8080:8080 proxymaze
```

## Key Design Decisions

- **Background monitoring**: `ScheduledExecutorService` runs continuously, never tied to API requests
- **Dynamic config**: changing `check_interval_seconds` immediately restarts the scheduler
- **Alert lifecycle**: single ACTIVE alert max; new breach after resolution = new `alert_id`
- **Webhook retries**: exponential backoff (1s→30s cap) for 5xx errors; delivered-key set prevents duplicates
- **Thread safety**: `ConcurrentHashMap`, `CopyOnWriteArrayList`, `AtomicInteger` throughout
- **Real HTTP probes**: OkHttp with configurable timeout; 2xx = UP, anything else = DOWN
