# DemoBank_v1 — Reliability & Failure Simulation Platform

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

## 5. Core Banking Endpoints (Test-Only)

---

All endpoints are accessible without auth in testing mode. Default values are shown where known and can be overridden via query params or request body.

5.1 Create Account  
POST /account/create_account

Request Body

| Field        | Type   | Description |
| ------------ | ------ | ----------- |
| account_name | string | Required    |
| account_type | string | Required    |

5.2 Deposit  
POST /transact/deposit

Request Body

| Field          | Type   | Description |
| -------------- | ------ | ----------- |
| deposit_amount | string | Required    |
| account_id     | int    | Required    |

5.3 Withdraw  
POST /transact/withdraw

Request Body

| Field             | Type   | Description |
| ----------------- | ------ | ----------- |
| withdrawal_amount | string | Required    |
| account_id        | string | Required    |

5.4 Payment  
POST /transact/payment

Request Body

| Field          | Type   | Description |
| -------------- | ------ | ----------- |
| beneficiary    | string | Required    |
| account_number | string | Required    |
| account_id     | string | Required    |
| reference      | string | Required    |
| payment_amount | string | Required    |

5.5 Transfer  
POST /transact/transfer

Request Body

| Field         | Type   | Description |
| ------------- | ------ | ----------- |
| sourceAccount | string | Required    |
| targetAccount | string | Required    |
| amount        | string | Required    |

## 6. Loan

---

6.1 Calculate Loan  
POST /loan/calculate

Request Body

| Field      | Type   | Description    |
| ---------- | ------ | -------------- |
| customerId | string | Required       |
| principal  | number | Default: 10000 |
| rate       | number | Default: 7.5   |
| years      | number | Default: 5     |

6.2 Simulate OOM via Loan Calculations  
POST /loan/simulate-oom?count={n}

| Parameter | Type | Default | Description                                                       |
| --------- | ---- | ------- | ----------------------------------------------------------------- |
| count     | int  | 10000   | Number of loan computations stored in memory; change via ?count=N |

6.3 Cleanup Loan Cache  
DELETE /loan/cleanup

## 7. OOM & Memory Leak Simulations

---

7.1 Upload File (unsafe memory load)  
POST /oom/upload-file — multipart file; entire file read into memory. Avoid very large files unless intentional.

7.2 Queue Memory Leak  
GET /oom/queue-leak?chunks={n}

| Parameter | Type | Default | Description                                                               |
| --------- | ---- | ------- | ------------------------------------------------------------------------- |
| chunks    | int  | 1000    | Number of byte[] chunks added to an in-memory queue; change via ?chunks=N |

7.3 Session Cache Leak  
GET /oom/cache-leak?sessions={n}

| Parameter | Type | Default | Description                                                                           |
| --------- | ---- | ------- | ------------------------------------------------------------------------------------- |
| sessions  | int  | 10000   | Number of session-like objects inserted into an in-memory map; change via ?sessions=N |

7.4 Simulated Logins (static map growth)  
POST /oom/simulate-login?users={n}

| Parameter | Type | Default | Description                                                   |
| --------- | ---- | ------- | ------------------------------------------------------------- |
| users     | int  | 1000    | Number of simulated sessions to populate; change via ?users=N |

7.5 Account Transaction History (OOM variant)  
POST /oom/account_transaction_history — same body as account transaction history; implementation may build large structures.

7.6 Export Transactions as CSV  
POST /oom/export-transactions-csv

Request Body

| Field      | Type   | Description |
| ---------- | ------ | ----------- |
| account_id | string | Required    |

7.7 Thread Leak  
GET /oom/thread-leak?count={n}

| Parameter | Type | Default | Description                                                            |
| --------- | ---- | ------- | ---------------------------------------------------------------------- |
| count     | int  | 100     | Number of threads spawned that sleep indefinitely; change via ?count=N |

7.8 Multi-Threaded OOM  
GET /oom/multi-thread-oom — composite concurrent leak tasks.

7.9 Cleanup Memory and Threads  
DELETE /oom/cleanup

## 8. Ephemeral Storage / Disk Simulations

---

8.1 Safe Disk Fill  
GET /ephimeral/safe-fill?maxFiles={n}

| Parameter | Type | Default | Description                                            |
| --------- | ---- | ------- | ------------------------------------------------------ |
| maxFiles  | int  | (none)  | Number of small temp files to create; pass ?maxFiles=N |

8.2 Cleanup Files  
DELETE /ephimeral/cleanup

8.3 Log Growth Simulation  
GET /ephimeral/log-growth?lines={n}

| Parameter | Type | Default | Description                                  |
| --------- | ---- | ------- | -------------------------------------------- |
| lines     | int  | (none)  | Number of log lines to append; pass ?lines=N |

8.4 Cleanup Logs  
DELETE /ephimeral/cleanup-logs

## 9. Database Pool Exhaustion Simulations

---

9.1 Exhaust DB Connections  
POST /dbpool/exhaust_db?threads={n}

| Parameter | Type | Default | Description                                                  |
| --------- | ---- | ------- | ------------------------------------------------------------ |
| threads   | int  | 50      | Concurrent threads performing DB work; change via ?threads=N |

9.2 DB Exhaustion + OOM  
POST /dbpool/exhaust_db_and_OOM

9.3 Multi-Service DB Load  
POST /dbpool/multi-service-load

9.4 Service-Based Load Simulation  
POST /dbpool/LoadSimulationService?threadsPerService={n}

| Parameter         | Type | Default | Description                                              |
| ----------------- | ---- | ------- | -------------------------------------------------------- |
| threadsPerService | int  | (none)  | Threads per simulated service; pass ?threadsPerService=N |

## 10. API Rate Limit / Quota Simulations

---

10.1 GitHub Sequential Rate Limit  
GET /rate-limit/github-rate-limit?count={n}

| Parameter | Type | Default | Description                                        |
| --------- | ---- | ------- | -------------------------------------------------- |
| count     | int  | (none)  | Number of sequential requests; change via ?count=N |

10.2 GitHub Concurrent Spam with Retry  
GET /rate-limit/github-spam-retry?threads={n}

| Parameter | Type | Default | Description                               |
| --------- | ---- | ------- | ----------------------------------------- |
| threads   | int  | (none)  | Concurrent threads; change via ?threads=N |

## 11. TLS / DNS Failure Simulations

---

11.1 Expired Certificate  
GET /tls-dns/expired-cert

11.2 Wrong Domain Lookup  
GET /tls-dns/wrong-domain

11.3 Certificate Mismatch  
GET /tls-dns/cert-mismatch

## 12. Network Timeout Simulations

---

12.1 External API with No Timeout  
GET /network/external_api

12.2 Slow Streaming Response  
GET /network/slow_stream

## 13. CPU Throttling and Heavy Computation

---

13.1 Prime Calculation  
GET /cpu/calculate_primes?count={n}

| Parameter | Type | Default | Description                                              |
| --------- | ---- | ------- | -------------------------------------------------------- |
| count     | int  | 1000    | Number of prime checks / work units; change via ?count=N |

13.2 Transaction Repository Analysis  
GET /cpu/analyze_transactions_repo?iterations={n}

| Parameter  | Type | Default | Description                                             |
| ---------- | ---- | ------- | ------------------------------------------------------- |
| iterations | int  | (none)  | Number of analysis iterations; change via ?iterations=N |

Notes: CPU stress can also be injected by enabling busy loops, recursive hashing, or heavy math in service methods.

## 14. WebSocket Simulations

---

14.1 Idle WebSocket  
GET /ws-sim/idle-socket?durationSeconds={n}

| Parameter       | Type | Default | Description                                                |
| --------------- | ---- | ------- | ---------------------------------------------------------- |
| durationSeconds | int  | (none)  | Seconds to keep socket idle; change via ?durationSeconds=N |

14.2 Multiple WebSockets  
GET /ws-sim/multi-socket?count={n}

| Parameter | Type | Default | Description                                      |
| --------- | ---- | ------- | ------------------------------------------------ |
| count     | int  | (none)  | Number of simulated sockets; change via ?count=N |

14.3 Heartbeat Disabled  
GET /ws-sim/heartbeat-disabled

14.4 WebSocket Flooding  
GET /ws-sim/flood-messages

15. Coordinated failure scenario example

---

FraudDetectionService loads large model per request + LoanService caches computations + CustomerProfileService keeps sessions alive.

Steps:

1. Seed DB with many users/accounts and 30k+ transaction_history rows.
2. POST /loan/simulate-oom?count=20000
3. Simultaneously call fraud detection endpoint that allocates large model bytes.
4. Spawn /oom/simulate-login?users=... to create many session objects.

Result: memory + CPU pressure can produce OOM and severe slowdowns.

16. Cleanup & restore

---

Use cleanup endpoints and DB scripts:

- DELETE /loan/cleanup
- DELETE /oom/cleanup
- DELETE /ephimeral/cleanup
- Truncate or restore DB tables via SQL scripts

17. Example curl commands

---

Calculate a loan (override defaults)

```bash
curl -s -X POST http://localhost:8080/loan/calculate \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-x","principal":50000,"rate":8.5,"years":10}'
```

Simulate loan OOM

```bash
curl -s -X POST "http://localhost:8080/loan/simulate-oom?count=20000"
```

Upload file (loads into memory)

```bash
curl -s -X POST http://localhost:8080/oom/upload-file \
  -F "file=@/path/to/large-file.bin"
```

18. Development tips & next steps

---

- Edit `src/main/resources/application.properties` or use env vars for DB and port.
- If `account_id = 1` errors occur, create that account or update code/tests.
- I can add `db/init_seed.sql` in `db/`, generate an OpenAPI spec, or add Docker/Docker Compose files for an isolated test environment.

## License & disclaimer

This tooling is for controlled internal testing only. The author is not responsible for misuse. Run in contained environments.
