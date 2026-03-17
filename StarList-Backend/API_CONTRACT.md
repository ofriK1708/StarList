# StarList API Contract

> Base URL (dev): `http://localhost:8080`
> All timestamps are UTC `Instant` (ISO-8601). All dates are `LocalDate` (`YYYY-MM-DD`).
> Authentication is planned via AWS Cognito. For now, the **caller's user ID is passed as a request header** `X-User-Id: <Long>` where required.

---

## Table of Contents

1. [Common Conventions](#common-conventions)
2. [Enums Reference](#enums-reference)
3. [Error Responses](#error-responses)
4. [Users API](#users-api)
5. [Tasks API](#tasks-api)
6. [Habits API](#habits-api)
7. [Health Check](#health-check)

---

## Common Conventions

### Headers

| Header         | Type               | Required On                           | Description                |
|----------------|--------------------|---------------------------------------|----------------------------|
| `Content-Type` | `application/json` | POST / PUT                            | Request body format        |
| `X-User-Id`    | `Long`             | Task & Habit endpoints (create, list) | Identifies the acting user |

### Date/Time Formats

| Type        | Format              | Example                  |
|-------------|---------------------|--------------------------|
| `Instant`   | ISO-8601 UTC string | `"2026-03-17T10:30:00Z"` |
| `LocalDate` | ISO-8601 date       | `"2026-03-17"`           |

---

## Enums Reference

All enum values are serialized as **uppercase strings**.

### `DifficultyLevel`
```
NONE | EASY | MEDIUM | HARD | VERY_HARD | EXTREME
```
Internally maps to levels 0–5. Determines coin rewards/penalties.

### `TaskStatus`
```
PENDING | COMPLETED | EXPIRED | DELETED
```

### `HabitFrequency`
```
DAILY | WEEKLY | CUSTOM
```

### `TransactionType`
```
TASK_COMPLETION | HABIT_COMPLETION | ITEM_PURCHASE | STREAK_PENALTY | DEADLINE_PENALTY
```

### `ReferenceType`
```
TASK | HABIT | GALAXY_ITEM
```

### `ItemType`
```
STAR | PLANET | NEBULA | ASTEROID | COMET
```

### `RarityLevel`
```
COMMON | RARE | EPIC | LEGENDARY
```

### `ConversationType`
```
TASK_CREATION | STUDY_PLAN | HABIT_SUGGESTION | GENERAL_CHAT
```

### `CompletionStatus`
```
DONE | MISSED | NA
```
Per-day habit completion state embedded in `GET /habits` and `GET /habits/{habitId}` responses.
- `DONE` — the day has passed, the habit existed, and a completion record exists
- `MISSED` — the day has passed, the habit existed, and no completion record exists
- `NA` — the day is today or in the future, OR before the habit was created

---

## Error Responses

All errors return the following JSON body:

```json
{
  "detail": "User already exists with the same email",
  "instance": "/users",
  "status": 409,
  "title": "User already exists"
}
```

### HTTP Status Codes

| Status            | Trigger                                                                                              |
|-------------------|------------------------------------------------------------------------------------------------------|
| `400 Bad Request` | Validation failure (`@NotBlank`, `@Email`, `@NotNull`, `@Positive`, `@Future` violated)              |
| `404 Not Found`   | `UserNotFoundException`, `TaskNotFoundException`, `HabitNotFoundException`                           |
| `409 Conflict`    | `UserAlreadyExistsException`, `TaskAlreadyCompletedException`, `HabitAlreadyCompletedTodayException` |

### Validation Error Body (400)

When request body validation fails, the `message` field contains a map of field names to error descriptions:

```json
{
  "detail": "Validation failed with 1 failures. Check the 'failures' field for more information",
  "instance": "/habits",
  "status": 400,
  "title": "Invalid request, you doofus!",
  "failures": [
    {
      "message": "title must not be blank",
      "field": "title"
    }
  ]
}
```

---

## Users API

### Create User

```
POST /users
Content-Type: application/json
```

**Request Body — `CreateUserRequest`**

| Field           | Type     | Required | Constraints           | Description               |
|-----------------|----------|----------|-----------------------|---------------------------|
| `email`         | `String` | Yes      | `@NotBlank`, `@Email` | Unique user email         |
| `cognitoUserId` | `String` | Yes      | `@NotBlank`           | AWS Cognito sub / user ID |
| `displayName`   | `String` | Yes      | `@NotBlank`           | Display name shown in UI  |

```json
{
  "email": "alice@example.com",
  "cognitoUserId": "us-east-1:abc-123",
  "displayName": "Alice"
}
```

**Response — `201 Created` — `UserResponse`**

| Field                 | Type        | Description                               |
|-----------------------|-------------|-------------------------------------------|
| `id`                  | `Long`      | Internal database ID                      |
| `email`               | `String`    | User email                                |
| `displayName`         | `String`    | Display name                              |
| `totalCoins`          | `Integer`   | Current coin balance (resets monthly)     |
| `lifetimeCoinsEarned` | `Integer`   | All-time coins earned (never resets)      |
| `currentGalaxyCycle`  | `Integer`   | Current galaxy cycle number (starts at 1) |
| `galaxyResetDate`     | `LocalDate` | Date when galaxy/coins will next reset    |
| `createdAt`           | `Instant`   | Account creation timestamp                |

```json
{
  "id": 1,
  "email": "alice@example.com",
  "displayName": "Alice",
  "totalCoins": 0,
  "lifetimeCoinsEarned": 0,
  "currentGalaxyCycle": 1,
  "galaxyResetDate": "2026-04-17",
  "createdAt": "2026-03-17T10:00:00Z"
}
```

**Errors:** `400` (validation), `409` (email or cognitoUserId already exists)

---

### Get User

```
GET /users/{userId}
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `userId` | `Long` | User's database ID |

**Response — `200 OK` — `UserResponse`** *(same shape as Create User response above)*

**Errors:** `404` (user not found)

---

### Update User

```
PUT /users/{userId}
Content-Type: application/json
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `userId` | `Long` | User's database ID |

**Request Body — `UpdateUserRequest`**

| Field         | Type     | Required | Constraints | Description      |
|---------------|----------|----------|-------------|------------------|
| `displayName` | `String` | Yes      | `@NotBlank` | New display name |

```json
{
  "displayName": "Alice Wonderland"
}
```

**Response — `200 OK` — `UserResponse`** *(same shape as Create User response above)*

**Errors:** `400` (validation), `404` (user not found)

---

### Delete User

```
DELETE /users/{userId}
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `userId` | `Long` | User's database ID |

Deletes the user and **all owned data** (tasks, habits, completions, transactions, galaxy items, conversations) via cascade.

**Response — `204 No Content`** *(empty body)*

**Errors:** `404` (user not found)

---

## Tasks API

> Tasks support soft-delete: deleted tasks get `status: DELETED` and a `deletedAt` timestamp; they are not physically removed.
> Optimistic locking is used on completion to prevent race conditions.

### Create Task

```
POST /tasks
Content-Type: application/json
X-User-Id: <userId>
```

**Request Body — `AddTaskRequest`**

| Field             | Type              | Required | Constraints    | Description                |
|-------------------|-------------------|----------|----------------|----------------------------|
| `title`           | `String`          | Yes      | `@NotBlank`    | Task title                 |
| `description`     | `String`          | No       | max 2000 chars | Optional details           |
| `difficultyLevel` | `DifficultyLevel` | Yes      | `@NotNull`     | One of the enum values     |
| `durationMinutes` | `Integer`         | No       | `@Positive`    | Estimated duration         |
| `dueDate`         | `Instant`         | No       | `@Future`      | Must be a future timestamp |

```json
{
  "title": "Read chapter 5",
  "description": "Physics textbook chapter on thermodynamics",
  "difficultyLevel": "MEDIUM",
  "durationMinutes": 45,
  "dueDate": "2026-03-20T18:00:00Z"
}
```

**Response — `201 Created` — `AddTaskResponse`**

| Field             | Type              | Description                              |
|-------------------|-------------------|------------------------------------------|
| `taskId`          | `Long`            | Task database ID                         |
| `title`           | `String`          | Task title                               |
| `description`     | `String`          | Optional description                     |
| `difficultyLevel` | `DifficultyLevel` | Difficulty enum value                    |
| `durationMinutes` | `Integer`         | Estimated duration (nullable)            |
| `coinReward`      | `Integer`         | Coins earned on completion               |
| `coinPenalty`     | `Integer`         | Coins lost if expired/deleted (nullable) |
| `status`          | `TaskStatus`      | Always `PENDING` on creation             |
| `dueDate`         | `Instant`         | Due date (nullable)                      |
| `createdAt`       | `Instant`         | Creation timestamp                       |

```json
{
  "taskId": 101,
  "title": "Read chapter 5",
  "description": "Physics textbook chapter on thermodynamics",
  "difficultyLevel": "MEDIUM",
  "durationMinutes": 45,
  "coinReward": 20,
  "coinPenalty": 10,
  "status": "PENDING",
  "dueDate": "2026-03-20T18:00:00Z",
  "createdAt": "2026-03-17T10:00:00Z"
}
```

**Errors:** `400` (validation), `404` (user not found from header)

---

### Get Task

```
GET /tasks/{taskId}
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `taskId` | `Long` | Task's database ID |

**Response — `200 OK` — `TaskResponse`**

Same shape as `AddTaskResponse` above.

**Errors:** `404` (task not found)

---

### List Tasks for User

```
GET /tasks
X-User-Id: <userId>
```

Returns all non-deleted tasks belonging to the user.

**Response — `200 OK` — `List<TaskResponse>`**

```json
[
  {
    "taskId": 101,
    "title": "Read chapter 5",
    "description": "Physics textbook chapter on thermodynamics",
    "difficultyLevel": "MEDIUM",
    "durationMinutes": 45,
    "coinReward": 20,
    "coinPenalty": 10,
    "status": "PENDING",
    "dueDate": "2026-03-20T18:00:00Z",
    "createdAt": "2026-03-17T10:00:00Z"
  }
]
```

**Errors:** `404` (user not found from header)

---

### Update Task

```
PUT /tasks/{taskId}
Content-Type: application/json
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `taskId` | `Long` | Task's database ID |

**Request Body — `UpdateTaskRequest`**

All fields are **optional** — only the fields you include (non-null) are applied. Omitted fields keep their current values.

> **Partial-update semantics:** `null` (or omitted) → keep current value.
> To clear `dueDate` or `durationMinutes`, use the dedicated DELETE endpoints below.
> To clear `description`, send `""`.

| Field             | Type              | Required | Constraints           | Description                                                                   |
|-------------------|-------------------|----------|-----------------------|-------------------------------------------------------------------------------|
| `title`           | `String`          | No       | —                     | New title; omit to keep current                                               |
| `description`     | `String`          | No       | max 2000 chars        | New description; omit to keep current; send `""` to clear                     |
| `difficultyLevel` | `DifficultyLevel` | No       | —                     | New difficulty; recalculates `coinReward`/`coinPenalty`; omit to keep current |
| `durationMinutes` | `Integer`         | No       | must be positive      | New estimated duration; omit to keep current                                  |
| `dueDate`         | `Instant`         | No       | must be in the future | New due date; omit to keep current                                            |

| Scenario                                             | Example                                 |
|------------------------------------------------------|-----------------------------------------|
| Only extend the due date — everything else unchanged | `{ "dueDate": "2026-04-01T18:00:00Z" }` |
| Only bump the difficulty                             | `{ "difficultyLevel": "HARD" }`         |


**Response — `200 OK` — `TaskResponse`**

Same shape as `AddTaskResponse`.

**Errors:** `400` (validation), `404` (task not found)

---

### Clear Task Due Date

```
DELETE /tasks/{taskId}/due-date
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `taskId` | `Long` | Task's database ID |

Sets `dueDate` to `null` on the task.

**Response — `200 OK` — `TaskResponse`** *(same shape as `AddTaskResponse`; `dueDate` will be `null`)*

**Errors:** `404` (task not found)

---

### Clear Task Duration

```
DELETE /tasks/{taskId}/duration
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `taskId` | `Long` | Task's database ID |

Sets `durationMinutes` to `null` on the task.

**Response — `200 OK` — `TaskResponse`** *(same shape as `AddTaskResponse`; `durationMinutes` will be `null`)*

**Errors:** `404` (task not found)

---

### Complete Task

```
POST /tasks/{taskId}/complete
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `taskId` | `Long` | Task's database ID |

Marks the task as `COMPLETED` and awards coins to the user. Uses optimistic locking — concurrent requests for the same task will result in one succeeding and the other receiving `409`.

**Response — `200 OK` — `MarkTaskDoneResponse`**

| Field           | Type      | Description                       |
|-----------------|-----------|-----------------------------------|
| `taskId`        | `Long`    | Completed task ID                 |
| `coinsEarned`   | `Integer` | Coins awarded for this completion |
| `newTotalCoins` | `Integer` | User's updated coin balance       |

```json
{
  "taskId": 101,
  "coinsEarned": 20,
  "newTotalCoins": 120
}
```

**Errors:** `404` (task not found), `409` (task already completed)

---

### Delete Task

```
DELETE /tasks/{taskId}
```

**Path Parameters**

| Param    | Type   | Description        |
|----------|--------|--------------------|
| `taskId` | `Long` | Task's database ID |

Soft-deletes the task: sets `status = DELETED` and records `deletedAt`. Task is not returned in list queries after deletion.

**Response — `204 No Content`** *(empty body)*

**Errors:** `404` (task not found)

---

## Habits API

> Habits track streaks. A habit can only be completed **once per day** (enforced by a DB unique constraint on `habit_id + completed_date`).
> Like tasks, habits support soft-delete.

### Create Habit

```
POST /habits
Content-Type: application/json
X-User-Id: <userId>
```

**Request Body — `AddHabitRequest`**

| Field             | Type              | Required | Constraints    | Description                    |
|-------------------|-------------------|----------|----------------|--------------------------------|
| `title`           | `String`          | Yes      | `@NotBlank`    | Habit title                    |
| `description`     | `String`          | No       | max 2000 chars | Optional details               |
| `frequency`       | `HabitFrequency`  | Yes      | `@NotNull`     | `DAILY`, `WEEKLY`, or `CUSTOM` |
| `difficultyLevel` | `DifficultyLevel` | Yes      | `@NotNull`     | Determines coin reward         |

```json
{
  "title": "Morning run",
  "description": "Run at least 3km",
  "frequency": "DAILY",
  "difficultyLevel": "EASY"
}
```

**Response — `201 Created` — `AddHabitResponse`**

| Field               | Type              | Description                               |
|---------------------|-------------------|-------------------------------------------|
| `habitId`           | `Long`            | Habit database ID                         |
| `title`             | `String`          | Habit title                               |
| `description`       | `String`          | Optional description                      |
| `frequency`         | `HabitFrequency`  | Frequency enum value                      |
| `difficultyLevel`   | `DifficultyLevel` | Difficulty enum value                     |
| `coinReward`        | `Integer`         | Coins earned per completion               |
| `coinPenalty`       | `Integer`         | Coins lost on missed streak (nullable)    |
| `currentStreak`     | `Integer`         | Current consecutive completion streak     |
| `bestStreak`        | `Integer`         | All-time best streak                      |
| `totalCompletions`  | `Integer`         | Total number of times completed           |
| `lastCompletedDate` | `LocalDate`       | Date of last completion (nullable)        |
| `createdAt`         | `Instant`         | Creation timestamp                        |
| `isActive`          | `Boolean`         | Whether the habit is active (not deleted) |
```json
{
  "habitId": 55,
  "title": "Morning run",
  "description": "Run at least 3km",
  "frequency": "DAILY",
  "difficultyLevel": "EASY",
  "coinReward": 10,
  "coinPenalty": 5,
  "currentStreak": 0,
  "bestStreak": 0,
  "totalCompletions": 0,
  "lastCompletedDate": null,
  "createdAt": "2026-03-17T10:00:00Z",
  "isActive": true
}
```

**Errors:** `400` (validation), `404` (user not found from header)

---

### Get Habit

```
GET /habits/{habitId}?year=&month=
```

**Path Parameters**

| Param     | Type   | Description         |
|-----------|--------|---------------------|
| `habitId` | `Long` | Habit's database ID |

**Query Parameters**

| Param   | Type      | Required | Constraints | Description                                   |
|---------|-----------|----------|-------------|-----------------------------------------------|
| `year`  | `Integer` | No       | `>= 2000`   | Year of the month to display; defaults to now |
| `month` | `Integer` | No       | `1–12`      | Month number to display; defaults to now      |

If only one of `year`/`month` is provided, both are ignored and the current month is used.

**Response — `200 OK` — `HabitResponse`**

Same shape as `AddHabitResponse` above, plus:

| Field              | Type                     | Description                                                                  |
|--------------------|--------------------------|------------------------------------------------------------------------------|
| `monthCompletions` | `List<CompletionStatus>` | One entry per day of the requested month (index 0 = day 1). See enum values. |

**Errors:** `400` (invalid `year`/`month`), `404` (habit not found)

---

### List Habits for User

```
GET /habits?year=&month=
X-User-Id: <userId>
```

Returns all active (non-deleted) habits belonging to the user.

**Query Parameters**

| Param   | Type      | Required | Constraints | Description                                   |
|---------|-----------|----------|-------------|-----------------------------------------------|
| `year`  | `Integer` | No       | `>= 2000`   | Year of the month to display; defaults to now |
| `month` | `Integer` | No       | `1–12`      | Month number to display; defaults to now      |

If only one of `year`/`month` is provided, both are ignored and the current month is used.

**Response — `200 OK` — `List<HabitResponse>`**

Array of `HabitResponse` objects (same shape as `AddHabitResponse`), each including `monthCompletions`. Completions for all habits are fetched in a single DB query.

**Errors:** `400` (invalid `year`/`month`), `404` (user not found from header)

---

### Update Habit

```
PUT /habits/{habitId}
Content-Type: application/json
```

**Path Parameters**

| Param     | Type   | Description         |
|-----------|--------|---------------------|
| `habitId` | `Long` | Habit's database ID |

**Request Body — `UpdateHabitRequest`**

All fields are **optional** — only the fields you include (non-null) are applied. Omitted fields keep their current values.

> **Partial-update semantics:** `null` (or omitted) → keep current value.
> To clear `description`, send `""`.

| Field             | Type              | Required | Constraints    | Description                                                                   |
|-------------------|-------------------|----------|----------------|-------------------------------------------------------------------------------|
| `title`           | `String`          | No       | —              | New title; omit to keep current                                               |
| `description`     | `String`          | No       | max 2000 chars | New description; omit to keep current; send `""` to clear                     |
| `frequency`       | `HabitFrequency`  | No       | —              | New frequency; omit to keep current                                           |
| `difficultyLevel` | `DifficultyLevel` | No       | —              | New difficulty; recalculates `coinReward`/`coinPenalty`; omit to keep current |

**Response — `200 OK` — `HabitResponse`**

Same shape as `HabitResponse` (`monthCompletions` is `null`).

**Errors:** `400` (validation), `404` (habit not found)

---

### Complete Habit (for today)

```
POST /habits/{habitId}/complete
```

**Path Parameters**

| Param     | Type   | Description         |
|-----------|--------|---------------------|
| `habitId` | `Long` | Habit's database ID |

Marks the habit as completed for **today's date** (server-side UTC date). Awards coins and updates streak counters. Each habit can only be completed once per calendar day — a second call on the same day returns `409`.

**Response — `200 OK` — `MarkHabitDoneResponse`**

| Field           | Type      | Description                         |
|-----------------|-----------|-------------------------------------|
| `habitId`       | `Long`    | Completed habit ID                  |
| `coinsEarned`   | `Integer` | Coins awarded for this completion   |
| `newTotalCoins` | `Integer` | User's updated coin balance         |
| `currentStreak` | `Integer` | Updated current streak count        |
| `bestStreak`    | `Integer` | Updated best streak (if new record) |

```json
{
  "habitId": 55,
  "coinsEarned": 10,
  "newTotalCoins": 130,
  "currentStreak": 5,
  "bestStreak": 10
}
```

**Errors:** `404` (habit not found), `409` (already completed today)

---

### Delete Habit

```
DELETE /habits/{habitId}
```

**Path Parameters**

| Param     | Type   | Description         |
|-----------|--------|---------------------|
| `habitId` | `Long` | Habit's database ID |

Soft-deletes the habit: sets `isActive = false` and records `deletedAt`. Habit is not returned in list queries after deletion.

**Response — `204 No Content`** *(empty body)*

**Errors:** `404` (habit not found)

---

## Health Check

```
GET /
```

Simple liveness probe. No auth required.

**Response — `200 OK`**

```
starList is alive and well!
```

---

## Domain Notes for Frontend

### Coin System
- `totalCoins` — the user's spendable balance; **resets at the start of each galaxy cycle** (`galaxyResetDate`)
- `lifetimeCoinsEarned` — never resets; useful for leaderboards or unlocks
- Coin amounts (`coinReward`, `coinPenalty`) are computed server-side based on `difficultyLevel` — the frontend does not set them

### Galaxy Cycle
- Each cycle represents a monthly period
- When the cycle resets, `totalCoins` goes to 0 and `currentGalaxyCycle` increments
- `galaxyResetDate` tells the frontend when the next reset occurs

### Streak Logic (Habits)
- `currentStreak` increments each time the habit is completed without missing a day (or week, for `WEEKLY`)
- Missing a period resets `currentStreak` to 0
- `bestStreak` is the historical maximum of `currentStreak`

### Soft Deletes
- Tasks: deleted tasks have `status: DELETED`; they are excluded from list responses
- Habits: deleted habits have `isActive: false`; they are excluded from list responses

### Optimistic Locking on Task Completion
- The backend uses optimistic locking to prevent double-completion of a task
- If two requests complete the same task concurrently, one will receive `409 Conflict`
