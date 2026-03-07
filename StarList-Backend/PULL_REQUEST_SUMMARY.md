# StarList Backend - JPA Model Layer Implementation

## Overview
Implemented a complete, production-ready persistence layer for the StarList PostgreSQL database using Spring Data JPA, Hibernate, and Lombok. This includes 8 entities, 8 enums, comprehensive validation, and proper cascade relationships.

## What Was Done

### 1. Core Entities Created (8 total)

#### User (`User.java`)
- Central entity for user account management
- **Cognito Integration**: Uses `cognitoUserId` (no password hash stored - Cognito handles auth)
- **Immutable Email**: `@Column(updatable = false)` prevents email changes
- **Galaxy Progression**: Tracks `currentGalaxyCycle`, `galaxyResetDate`, `totalCoins`, `lifetimeCoinsEarned`
- **Relationships**: Owns 6 collections with `cascade = CascadeType.ALL, orphanRemoval = true` for complete data cleanup on deletion
- **Indexes**: `galaxy_reset_date`, `last_login`

#### Task (`Task.java`)
- Individual to-do items with gamification rewards
- **DifficultyLevel Enum**: Type-safe enum (EASY/MEDIUM/HARD/VERY_HARD/EXTREME) replaces Integer validation
- **Coin Rewards**: `@Min(0)` validation on `coinReward` and `coinPenalty`
- **Soft Delete**: `deletedAt` timestamp enables 24-hour restore window before permanent deletion
- **Status Tracking**: PENDING/COMPLETED/EXPIRED/DELETED states
- **AI Integration**: References `AiConversation` for tasks created by assistant
- **Indexes**: `user_id`, `due_date`, `status`, `deleted_at`, `ai_conversation_id`

#### Habit (`Habit.java`)
- Recurring habits with streak and completion tracking
- **DifficultyLevel Enum**: Type-safe difficulty mapping (same as Task)
- **Streak Validation**: `@Min(0)` ensures `currentStreak`, `bestStreak`, `totalCompletions` cannot be negative
- **Soft Delete**: `deletedAt` preserves habit history for statistics and analytics
- **Frequency**: DAILY/WEEKLY/CUSTOM scheduling options
- **Active State**: `isActive` flag for paused/archived habits
- **Completions**: One-to-many collection with cascade
- **Indexes**: `user_id`, `frequency`, `deleted_at`

#### HabitCompletion (`HabitCompletion.java`)
- Records individual habit completions (diary entries)
- **Unique Constraint**: `UNIQUE(habit_id, completed_date)` ensures one completion per habit per day
- **Streak Snapshot**: Captures `streakAtCompletion` for accurate historical data
- **Indexes**: `user_id`, `habit_id`, `completed_date`

#### ItemCatalog (`ItemCatalog.java`)
- Master catalog of purchasable galaxy items
- **BigDecimal Positioning**: `precision=6, scale=2` for accurate UI placement (prevents Float/Double rounding errors)
  - `positionX`, `positionY` (0-100% coordinates)
  - `scale` (size multiplier)
  - `rotation` (0-360 degrees)
- **Coin Validation**: `@Min(0)` on `costCoins` and `unlockRequirement`
- **Item Types**: STAR/PLANET/NEBULA/ASTEROID/COMET
- **Rarity Levels**: COMMON/RARE/EPIC/LEGENDARY
- **Availability Control**: `isAvailable` flag for temporary item disabling
- **Indexes**: `item_type`, `is_available`, `unlock_requirement`

#### GalaxyItem (`GalaxyItem.java`)
- Tracks user purchases and item placement in personal galaxy
- **Purchase History**: `purchaseDate` (immutable) and `timesSelected` for favorites tracking
- **Active State**: `isActive` set to false during monthly reset (preserves history)
- **Indexes**: `user_id`, `catalog_item_id`, `purchase_date`

#### CoinTransaction (`CoinTransaction.java`)
- Complete audit log of all coin earnings and spending
- **Transaction Types**: TASK_COMPLETION/HABIT_COMPLETION/ITEM_PURCHASE/STREAK_PENALTY/DEADLINE_PENALTY
- **Reference Tracking**: Links to related Task/Habit/GalaxyItem via `referenceType` and `referenceId`
- **Flexible Amount**: Positive = earned, Negative = spent (no @Min constraint for legitimate negative values)
- **Indexes**: `user_id`, `transaction_type`, `created_at`

#### AiConversation (`AiConversation.java`)
- Logs all interactions with AI assistant
- **Conversation Types**: TASK_CREATION/STUDY_PLAN/HABIT_SUGGESTION/GENERAL_CHAT
- **Task Persistence**: Removed `cascade = CascadeType.ALL` - tasks persist independently after conversation ends
- **Conversation Types**: MESSAGE storage for full chat history
- **Indexes**: `user_id`, `conversation_type`, `created_at`

### 2. Enums Created (8 total)

1. **TaskStatus** - PENDING, COMPLETED, EXPIRED, DELETED
2. **HabitFrequency** - DAILY, WEEKLY, CUSTOM
3. **DifficultyLevel** ✨ NEW - EASY(1), MEDIUM(2), HARD(3), VERY_HARD(4), EXTREME(5)
   - Replaces Integer validation with type-safe enum
   - Includes `getLevel()` method for numeric calculations
4. **ItemType** - STAR, PLANET, NEBULA, ASTEROID, COMET
5. **RarityLevel** - COMMON, RARE, EPIC, LEGENDARY
6. **TransactionType** - TASK_COMPLETION, HABIT_COMPLETION, ITEM_PURCHASE, STREAK_PENALTY, DEADLINE_PENALTY
7. **ReferenceType** - TASK, HABIT, GALAXY_ITEM
8. **ConversationType** - TASK_CREATION, STUDY_PLAN, HABIT_SUGGESTION, GENERAL_CHAT

All enums use `@Enumerated(EnumType.STRING)` for readable database storage.

### 3. Database Configuration

#### application-dev.yml
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/starlist
  username: postgres (default, overridable via env)
  password: postgres (default, overridable via env)
jpa:
  ddl-auto: update  # Auto-creates/updates schema
  timezone: UTC
```
**Use**: Local development with auto-schema generation

#### application-test.yml
```yaml
datasource:
  url: jdbc:h2:mem:starlist_test (in-memory)
  driver: H2
jpa:
  ddl-auto: create-drop  # Fresh DB per test run
  timezone: UTC
```
**Use**: Unit/integration tests with H2 database (PostgreSQL-compatible mode)

#### application-prod.yml
```yaml
datasource:
  url: ${DB_URL}           # Required env variable
  username: ${DB_USERNAME} # Required env variable
  password: ${DB_PASSWORD} # Required env variable
jpa:
  ddl-auto: validate  # Strict validation only, no modifications
  timezone: UTC
```
**Use**: Production deployment (requires all env vars, zero schema modifications)

### 4. Maven Dependencies Added

**model/pom.xml:**
- `spring-boot-starter-data-jpa` - JPA/Hibernate support
- `spring-boot-starter-validation` - Jakarta Bean Validation
- `postgresql` (runtime) - PostgreSQL JDBC driver
- `h2` (test) - H2 in-memory database for testing

**repository/pom.xml:**
- `spring-boot-starter-data-jpa` - Repository support
- `com.starlist:model` dependency - Links to entities

### 5. Key Design Decisions

#### Cognito Authentication
- No `password_hash` field in User entity
- Uses `cognitoUserId` (Cognito `sub` claim) as unique identifier
- Email immutable with `@Column(updatable = false)`
- Cognito handles password security, StarList only stores user profile

#### Soft Deletes Strategy
- **Tasks**: `deletedAt` timestamp (24-hour restore window)
  - Daily cleanup job deletes records where `deletedAt < now() - 24 hours`
  - User can restore within 24 hours
- **Habits**: `deletedAt` timestamp (permanent history preservation)
  - Keeps habit completion records for statistics
  - User can view historical habit data
- Implementation: Service layer filters out deleted items in queries

#### Cascade Relationships
- User owns all child collections with `cascade = CascadeType.ALL, orphanRemoval = true`
- Deleting a user automatically deletes all their tasks, habits, transactions, etc.
- Exception: AiConversation → Tasks (no cascade) - tasks persist independently
- Exception: Task → AiConversation (no cascade) - conversation can be archived without deleting tasks

#### Type-Safe Enums
- `DifficultyLevel` enum replaces Integer validation
- Stored as readable strings in database (e.g., "HARD" not "3")
- All status/type fields use enums for type safety

#### Indexing Strategy
- User FK indexes on all child tables for fast lookups
- Status/type column indexes for WHERE clauses
- Date indexes on frequently queried timestamps (`due_date`, `completed_date`, `created_at`)
- Composite unique constraint on `habit_completions(habit_id, completed_date)`

#### Precision Positioning
- `BigDecimal` with `precision=6, scale=2` for galaxy item positioning
- Prevents Float/Double rounding errors on UI coordinates
- Database stores exact values (e.g., 50.20 stays 50.20, not 50.20000000002)

#### Validation Layers
- **Application Level**: Jakarta Bean Validation (`@Min`, `@Max`, `@NotBlank`, `@Email`)
- **Database Level**: Constraints on column definitions + unique constraints
- **Business Logic Level**: Service layer enforces additional rules (e.g., streak calculations)

### 6. Build Verification

```
BUILD SUCCESS
- model module: COMPILED (16 source files)
- repository module: COMPILED
- All Java modules: COMPILED
- Total time: 2.8 seconds
- Warnings: Only Lombok Unsafe deprecation (planned Java removal, not critical)
```

## Technical Highlights

✅ **Java 25 Compatible** - All generics, records, and modern syntax supported
✅ **Lombok Integration** - `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
✅ **Hibernate Auto-DDL** - Schema auto-generated from entities (dev/test profiles)
✅ **UTC Timestamps** - All dates stored/retrieved in UTC for consistency
✅ **Immutable Creation Dates** - `@Column(updatable = false)` on `createdAt` fields
✅ **Default Values** - `@PrePersist` hooks set sensible defaults
✅ **Type Safety** - Enums instead of magic numbers/strings
✅ **Lazy Loading** - `@ManyToOne(fetch = FetchType.LAZY)` for performance
✅ **Referential Integrity** - Foreign keys with proper cascade behavior

## Repository Structure

```
model/
├── src/main/java/model/
│   ├── *.java (8 entity classes)
│   └── enums/ (8 enum classes)
├── README.md
├── IMPLEMENTATION.md
└── pom.xml
repository/
├── pom.xml (updated with model dependency)
└── src/main/java/repository/
    └── [Ready for Spring Data repositories]
app/
└── src/main/resources/
    ├── application.yml (updated with profiles)
    ├── application-dev.yml (updated with PostgreSQL config)
    ├── application-test.yml (updated with H2 config)
    └── application-prod.yml (updated with production config)
```

## What's Ready to Build Next

1. **Spring Data Repositories** - JPA repository interfaces for each entity
2. **Service Layer** - Business logic for coin calculations, streak updates, monthly resets
3. **REST Controllers** - API endpoints for CRUD operations
4. **Cognito Integration** - Login flow with JWT validation
5. **Scheduled Tasks** - Cleanup jobs (24-hour task deletion, monthly galaxy reset)
6. **Validation Services** - Business rule enforcement

## Files Modified/Created

- ✅ Created: 15 Java entity and enum files
- ✅ Created: 2 documentation files (README.md, IMPLEMENTATION.md)
- ✅ Modified: model/pom.xml (added dependencies)
- ✅ Modified: repository/pom.xml (added model dependency)
- ✅ Modified: app configuration files (3 YAML profiles with database config)

## Conclusion

The StarList backend now has a robust, validated, and well-documented data model ready for service layer implementation. All entities follow JPA best practices with:

- Type-safe enums for domain concepts
- Proper validation at multiple levels
- Efficient indexes for common queries
- Cascade behavior for data integrity
- Clear separation of concerns (entities, validation, configuration)
- PostgreSQL and H2 support with automatic schema generation

The foundation is solid and ready to build application logic on top! 🚀

