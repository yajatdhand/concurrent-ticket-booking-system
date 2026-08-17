# Concurrent Ticket Booking System

A Spring Boot ticket booking system built to understand and demonstrate **database-level concurrency control** in a real-world booking scenario.

The project implements the same ticket-booking operation using both:

- **Optimistic Locking** using JPA `@Version`
- **Pessimistic Locking** using JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)`

The project was built primarily as a hands-on exploration of race conditions, transactions, Hibernate locking, optimistic concurrency control, and clean REST API error handling.

---

## Problem Statement

Consider a movie ticket booking system where multiple users attempt to book the same seat concurrently.

A naive implementation might perform:

1. Read the seat
2. Check if the seat is `AVAILABLE`
3. Mark it as `BOOKED`
4. Save the seat

Under concurrent requests, this can result in a race condition:

```text
T1                         T2
│                          │
├── Read seat → AVAILABLE  │
│                          ├── Read seat → AVAILABLE
│                          │
├── Check AVAILABLE        ├── Check AVAILABLE
│                          │
├── BOOKED                 ├── BOOKED
│                          │
├── Save                   ├── Save
│                          │
└── Success                └── Success
```

Both transactions can observe the seat as available and attempt to book it.

The purpose of this project is to solve this concurrency problem using database-backed concurrency control.

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA
- Hibernate
- PostgreSQL
- Maven
- REST APIs

---

## Architecture

The project follows a simple layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Hibernate / JPA
    ↓
PostgreSQL
```

### Layers

**Controller**
- Handles HTTP requests and responses.
- Delegates business operations to the service.
- Contains only the happy-path response construction.

**Service**
- Contains booking business logic.
- Defines the transaction boundary using `@Transactional`.
- Provides separate optimistic and pessimistic booking flows.

**Repository**
- Uses Spring Data JPA.
- Provides derived queries.
- Defines the pessimistic locking query using `@Lock`.

**Global Exception Handler**
- Centralizes business and persistence exception handling.
- Converts exceptions into appropriate HTTP responses.

---

## Domain Model

### Show

A movie show contains:

- `showId`
- `movieName`
- `showTime`

### Seat

A seat contains:

- `seatId`
- `seatNumber`
- `bookingStatus`
- `show`
- `version`

Relationship:

```text
Show
  │
  └── 1 ───────── * ── Seat
```

The relationship is represented using:

```java
@ManyToOne
@JoinColumn(name = "show_id")
private Show show;
```

`show_id` is the actual database column storing the foreign key to the `show` table.

---

# API Endpoints

## 1. Optimistic Booking

```http
POST /ticket-booking-service/book-ticket-optimistic
```

Request:

```json
{
  "showId": 1,
  "seatId": 1
}
```

Successful response:

```http
201 Created
```

```json
{
  "seatNumber": "A10",
  "movieName": "Avengers",
  "status": "BOOKED"
}
```

---

## 2. Pessimistic Booking

```http
POST /ticket-booking-service/book-ticket-pessimistic
```

Request:

```json
{
  "showId": 1,
  "seatId": 1
}
```

Successful response:

```http
201 Created
```

```json
{
  "seatNumber": "A10",
  "movieName": "Avengers",
  "status": "BOOKED"
}
```

The business operation is intentionally the same; the difference is how concurrent access to the seat is controlled.

---

# Optimistic Locking

Optimistic locking is implemented using JPA's `@Version`.

```java
@Version
private Integer version;
```

Hibernate maintains this version automatically.

For example:

```text
Seat 3
status  = AVAILABLE
version = 0
```

Two transactions can initially read version `0`:

```text
T1 → version 0
T2 → version 0
```

Both can modify their in-memory entities.

When Hibernate flushes the changes, it generates a version-aware update conceptually similar to:

```sql
UPDATE seat
SET booking_status = 'BOOKED',
    version = 1
WHERE seat_id = 3
  AND version = 0;
```

If T1's update succeeds:

```text
version 0 → version 1
```

T2 still has the old version:

```text
version = 0
```

Therefore T2 effectively executes:

```sql
UPDATE seat
SET booking_status = 'BOOKED',
    version = 1
WHERE seat_id = 3
  AND version = 0;
```

But the database now contains:

```text
version = 1
```

so the update affects zero rows.

Hibernate detects the unexpected row count and raises an optimistic-locking exception.

The application handles this as:

```http
409 Conflict
```

---

## Optimistic Locking Flow

```text
T1                              T2
│                               │
├── Read version 0              ├── Read version 0
│                               │
├── Modify entity               ├── Modify entity
│                               │
├── Flush                       │
│   UPDATE WHERE version = 0    │
│                               │
├── Success                     │
│   version → 1                 │
│                               │
│                               ├── Flush
│                               │   UPDATE WHERE version = 0
│                               │
│                               └── 0 rows affected
│                                   ↓
│                               Optimistic locking failure
```

### Important Point

Optimistic locking does **not** require a separate query to check the version before updating.

The version check is part of the `UPDATE` itself.

---

# Pessimistic Locking

The pessimistic implementation uses:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Seat> findPessimisticBySeatIdAndShow_ShowId(
        Long seatId,
        Long showId
);
```

Hibernate generates a database locking query similar to:

```sql
SELECT ...
FROM seat
WHERE seat_id = ?
  AND show_id = ?
FOR UPDATE;
```

Depending on the database and Hibernate dialect, the exact SQL locking clause may differ.

The important behavior is:

```text
T1
 ↓
SELECT ... FOR UPDATE
 ↓
Row lock acquired
 ↓
Business logic
 ↓
UPDATE
 ↓
COMMIT
 ↓
Lock released
```

If T2 attempts a conflicting row lock while T1 holds the lock:

```text
T2
 ↓
SELECT ... FOR UPDATE
 ↓
Waits
```

T2 cannot acquire the conflicting lock until T1's transaction completes.

---

## Why `@Transactional` Is Important

The pessimistic lock is not held merely because the SELECT query returned.

The row lock is associated with the database transaction.

Therefore:

```java
@Transactional
public Seat bookTicketPessimistically(...) {
    ...
}
```

creates the transaction boundary.

Conceptually:

```text
BEGIN TRANSACTION
       ↓
SELECT ... FOR UPDATE
       ↓
Row lock acquired
       ↓
Business logic
       ↓
UPDATE
       ↓
COMMIT
       ↓
Row lock released
```

This is why the transaction belongs around the service-level business operation rather than only around the repository query.

---

# Optimistic vs Pessimistic Locking

| | Optimistic Locking | Pessimistic Locking |
|---|---|---|
| Initial read | No row lock | Acquires row lock |
| Concurrent reads | Allowed | Conflicting operations may wait |
| Conflict detection | During update | Prevented/serialized using lock |
| Main JPA mechanism | `@Version` | `@Lock(PESSIMISTIC_WRITE)` |
| Waiting | Generally no lock waiting | Conflicting operations may wait |
| Conflict handling | Application handles conflict/retry | Database coordinates access |
| Best suited for | Relatively rare conflicts | Cases where coordination before modification is important |

Neither strategy is universally better.

The appropriate choice depends on:

- Frequency of conflicts
- Cost of a conflict
- Transaction duration
- Number of concurrent requests
- Acceptable latency
- Retry behavior
- Whether an atomic database operation can solve the invariant more efficiently

---

# Transaction and Hibernate Flush

One important concept explored in this project is the difference between:

```text
Changing the Java entity
        ≠
Executing SQL UPDATE
        ≠
Committing the transaction
```

For example:

```java
entity.setBookingStatus(BookingStatus.BOOKED);
return entity;
```

The managed entity can become dirty in Hibernate's persistence context.

Hibernate later flushes the pending state:

```text
Service method
      ↓
return entity
      ↓
@Transactional proxy
      ↓
Hibernate flush
      ↓
SQL UPDATE
      ↓
COMMIT
      ↓
Controller receives result
      ↓
HTTP response
```

Therefore, returning an entity from the service does **not** by itself mean that the database transaction has already committed.

If the optimistic-locking `UPDATE` fails during flush, the transaction can fail and the controller will not receive a successful result.

---

# Global Exception Handling

The project uses:

```java
@RestControllerAdvice
public class GlobalExceptionHandler
```

Business-specific exceptions are allowed to propagate from the service instead of being handled inside every controller.

The flow is:

```text
Controller
    ↓
Service
    ↓
Exception
    ↓
@RestControllerAdvice
    ↓
HTTP status + JSON body
```

### Seat Not Found

```text
SeatNotFoundException
        ↓
404 Not Found
```

Example:

```json
{
  "message": "Seat not found!"
}
```

### Seat Already Booked

```text
SeatAlreadyBookedException
        ↓
409 Conflict
```

Example:

```json
{
  "message": "Seat is already booked!"
}
```

### Optimistic Locking Conflict

```text
ObjectOptimisticLockingFailureException
        ↓
409 Conflict
```

---

# Why 409 Conflict?

A `409 Conflict` is appropriate for the optimistic-locking failure because:

- The requested seat exists.
- The request itself is structurally valid.
- Another concurrent request modified the same resource.
- The requested operation conflicts with the resource's current state.

The client can potentially reload the resource and retry.

---

# Spring Data JPA Derived Queries

The repository contains:

```java
Optional<Seat> findBySeatIdAndShow_ShowId(
        Long seatId,
        Long showId
);
```

Because `Seat` contains:

```java
private Show show;
```

and `Show` contains:

```java
private Long showId;
```

Spring Data navigates the entity relationship through:

```text
Seat → show → showId
```

The `_` makes the property traversal explicit:

```text
Show_ShowId
```

The pessimistic method is:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Seat> findPessimisticBySeatIdAndShow_ShowId(
        Long seatId,
        Long showId
);
```

The text before `By` is a descriptive method subject. The query criteria begin after `By`.

---

# Project Structure

```text
src/main/java/com/org/ram/ticketbookingsystem
│
├── controller
│   ├── GlobalExceptionHandler.java
│   └── TicketBookingController.java
│
├── dto
│   ├── BookTicketRequest.java
│   ├── BookTicketResponse.java
│   └── BookingErrorResponse.java
│
├── entity
│   ├── Seat.java
│   └── Show.java
│
├── exceptions
│   ├── SeatAlreadyBookedException.java
│   └── SeatNotFoundException.java
│
├── model
│   └── BookingStatus.java
│
├── repository
│   └── SeatRepository.java
│
├── service
│   └── TicketBookingService.java
│
└── serviceImpl
    └── TicketBookingServiceImpl.java
```

---

# Configuration

Database credentials are supplied through environment variables rather than being hardcoded:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Set the following environment variables before running the application:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/your_database"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"
```

Do not commit real database credentials to the repository.

---

# Running the Application

### Prerequisites

- Java 21
- PostgreSQL
- Maven (optional because the project includes Maven Wrapper)

### Using Maven Wrapper

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

On Windows:

```cmd
mvnw.cmd spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

---

# Testing the APIs

Example request:

```bash
curl -X POST http://localhost:8080/ticket-booking-service/book-ticket-optimistic \
  -H "Content-Type: application/json" \
  -d '{
    "showId": 1,
    "seatId": 1
  }'
```

For pessimistic locking:

```bash
curl -X POST http://localhost:8080/ticket-booking-service/book-ticket-pessimistic \
  -H "Content-Type: application/json" \
  -d '{
    "showId": 1,
    "seatId": 1
  }'
```

---

# What This Project Demonstrates

This project was built to gain practical understanding of:

- Race conditions
- Concurrent database updates
- Transactions
- `@Transactional`
- Database row locking
- `SELECT ... FOR UPDATE`
- JPA pessimistic locking
- JPA optimistic locking
- `@Version`
- Hibernate dirty checking
- Hibernate flushing
- Transaction commit and rollback
- Spring transactional proxies
- Spring Data JPA derived queries
- Entity relationships
- Global exception handling
- REST API error responses
- HTTP status codes
- Optimistic locking conflict handling
- Choosing between optimistic and pessimistic concurrency control

---

# Key Takeaway

The core lesson of this project is that concurrency control should not be treated as simply a Java-thread problem.

For a horizontally scalable application:

```text
Multiple application threads
        ↓
Multiple JVMs / application instances
        ↓
Shared database
        ↓
Database-level concurrency control
```

Java synchronization such as `synchronized` can coordinate threads inside one JVM, but it does not coordinate independent application instances.

For shared database state, mechanisms such as transactions, pessimistic locks, optimistic versioning, and atomic database operations provide stronger concurrency guarantees.
