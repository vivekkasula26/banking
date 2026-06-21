# DemoBank_v1

## 1. Overview

---

This repository is a Spring Boot + Maven testing platform for system reliability, chaos testing, and observability dataset generation for AI/ML models. It intentionally exposes endpoints that simulate faults (memory leaks, CPU storms, thread leaks, disk pressure, DB pool exhaustion, network/TLS failures, WebSocket load, etc.). Authentication exists in the code but is bypassed in testing mode so endpoints are available without real JWTs.

Run all simulations in isolated environments only (local VM, disposable container). Do not expose to public networks.

## 2. Quick start

---

Prerequisites

- Java 11+ (or the JDK version configured in the project)
- Maven (mvn)
- A relational DB (Postgres / MySQL / H2) configured in `src/main/resources/application.properties` or via environment variables

Build and run

```bash
# from repo root
mvn clean package

# or run with Maven plugin (dev)
mvn spring-boot:run
```

If you see import errors for `org.springframework.web.socket`, add Spring WebSocket to `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-websocket</artifactId>
</dependency>
```

## 3. Database: seed data and notes

---

Many simulation endpoints expect realistic DB data. Populate at least: users, accounts, transaction_history. Transaction history should be large (example: 30,000+ rows) to reproduce realistic memory/CPU pressure.

Suggested seed SQL (Postgres / generic)

```sql
CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, email VARCHAR(255), name VARCHAR(255), password VARCHAR(255));
CREATE TABLE IF NOT EXISTS accounts (id SERIAL PRIMARY KEY, user_id INT REFERENCES users(id), account_name VARCHAR(255), balance BIGINT);
CREATE TABLE IF NOT EXISTS transaction_history (id SERIAL PRIMARY KEY, account_id INT REFERENCES accounts(id), amount BIGINT, type VARCHAR(50), created_at TIMESTAMP DEFAULT now());

INSERT INTO users (email,name,password) VALUES ('test1@example.com','Test One','pass'),('test2@example.com','Test Two','pass');
INSERT INTO accounts (user_id,account_name,balance) VALUES (1,'Checking',100000),(1,'Savings',250000),(2,'Checking',50000);

-- populate many transaction rows (example 30k)
INSERT INTO transaction_history (account_id, amount, type)
SELECT 1 + (random()*2)::int, floor(random()*100000), 'DEPOSIT'
FROM generate_series(1, 30000);
```

Important DB notes

- Some code hardcodes `account_id = 1`. Ensure the first account exists or update callers.
- Seed counts and sizes control realism of simulations.

## 4. Safety & monitoring

---

- Run only in isolated environments.
- Limit JVM heap to reproduce OOM quickly.
- Use testing tools like Jmeter to popoulate http traffic.
- Use cleanup endpoints and DB truncation after tests.

## 5. Chaos Testing API Reference

---

This application is purpose-built for **chaos testing**. All 28 APIs simulate various failure scenarios including memory leaks, CPU throttling, network failures, database exhaustion, and WebSocket load conditions.

**Testing approach:** For high-volume concurrent load testing, use **JMeter** on the following endpoint groups:

- **OOM APIs** (queue-leak, cache-leak, multi-thread-oom)
- **CPU Throttling APIs** (calculate_primes, analyze_transactions_repo)
- **Database Pool APIs** (exhaust_db, multi-service-load)
- **WebSocket APIs** (multi-socket, flood-messages)

### OOM (Out of Memory)

| #   | API Name                      | Method | Endpoint                                  |
| --- | ----------------------------- | ------ | ----------------------------------------- |
| 1   | oom-transaction-history       | POST   | `{{url}}/oom/account_transaction_history` |
| 2   | oom-in-mem-csv                | POST   | `{{url}}/oom/export-transactions-csv`     |
| 3   | oom-queue-leak                | GET    | `{{url}}/oom/queue-leak`                  |
| 4   | oom-multi-thread              | GET    | `{{url}}/oom/multi-thread-oom`            |
| 5   | oom-simulate-login-many-users | POST   | `{{url}}/oom/simulate-login`              |
| 6   | oom-cache-leak                | GET    | `{{url}}/oom/cache-leak`                  |
| 7   | oom-upload-big-files          | POST   | `{{url}}/oom/upload-file`                 |
| 8   | oom-cleanup                   | DELETE | `{{url}}/oom/cleanup`                     |

### CPU Throttling

| #   | API Name             | Method | Endpoint                                     |
| --- | -------------------- | ------ | -------------------------------------------- |
| 9   | calculate_primes     | GET    | `{{url}}/cpu/calculate_primes`               |
| 10  | analyze_transactions | GET    | `{{url}}/cpu/analyze_transactions_repo`      |
| 11  | cpu-report-threaded  | GET    | `{{url}}/cpu/cpu-report-threaded`            |
| 12  | CPU usage metrics    | GET    | `{{url}}/actuator/metrics/process.cpu.usage` |

### TLS / DNS

| #   | API Name             | Method | Endpoint                        |
| --- | -------------------- | ------ | ------------------------------- |
| 13  | expired-certificate  | GET    | `/tls-dns/expired-cert`         |
| 14  | wrong-domain         | GET    | `{{url}}/tls-dns/wrong-domain`  |
| 15  | certificate-mismatch | GET    | `{{url}}/tls-dns/cert-mismatch` |

### Network Timeout

| #   | API Name     | Method | Endpoint                       |
| --- | ------------ | ------ | ------------------------------ |
| 16  | external-api | GET    | `{{url}}/network/external_api` |
| 17  | slow-stream  | GET    | `{{url}}/network/slow_stream`  |
| 18  | retry-storm  | GET    | `{{url}}/network/retry_storm`  |

### API Quota / Rate Limit

| #   | API Name         | Method | Endpoint                               |
| --- | ---------------- | ------ | -------------------------------------- |
| 19  | rate-limit-retry | GET    | `{{url}}/rate-limit/github-spam-retry` |
| 20  | rate-limit       | GET    | `{{url}}/rate-limit/github-rate-limit` |

### Ephemeral Storage

| #   | API Name  | Method | Endpoint                      |
| --- | --------- | ------ | ----------------------------- |
| 21  | safe-fill | GET    | `{{url}}/ephimeral/safe-fill` |

### Database Pool Exhaustion

| #   | API Name           | Method | Endpoint                            |
| --- | ------------------ | ------ | ----------------------------------- |
| 22  | exhaust_db         | POST   | `{{url}}/dbpool/exhaust_db`         |
| 23  | exhaust_db_and_oom | POST   | `{{url}}/dbpool/exhaust_db_and_OOM` |
| 24  | multi-service-load | POST   | `{{url}}/dbpool/multi-service-load` |

### WebSocket Simulation

| #   | API Name           | Method | Endpoint                            |
| --- | ------------------ | ------ | ----------------------------------- |
| 25  | idle-socket        | GET    | `{{url}}/ws-sim/idle-socket`        |
| 26  | multi-socket       | GET    | `{{url}}/ws-sim/multi-socket`       |
| 27  | heartbeat-disabled | GET    | `{{url}}/ws-sim/heartbeat-disabled` |
| 28  | flood-messages     | GET    | `{{url}}/ws-sim/flood-messages`     |

---

**Total: 28 APIs across 8 categories** covering memory, CPU, network, storage, database, and WebSocket failure scenarios.

### Quick Testing Examples

Load test with curl:

```bash
# OOM: Queue leak
curl -s -X GET "http://localhost:8080/oom/queue-leak?chunks=1000"

# CPU: Calculate primes
curl -s -X GET "http://localhost:8080/cpu/calculate_primes?count=5000"

# DB: Exhaust connections
curl -s -X POST "http://localhost:8080/dbpool/exhaust_db?threads=50"

# Cleanup
curl -s -X DELETE "http://localhost:8080/oom/cleanup"
```

For sustained load and monitoring:

```bash
#  Use JMeter to simulate 100 concurrent users
```

## 6. Development tips & next steps

---

- Edit `src/main/resources/application.properties` or use env vars for DB and port.
- If `account_id = 1` errors occur, create that account or update code/tests.

## License & disclaimer

This tooling is for controlled internal testing only. The author is not responsible for misuse. Run in contained environments.
