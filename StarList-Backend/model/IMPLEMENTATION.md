# StarList Model Layer Implementation

## Overview
Complete JPA persistence model for PostgreSQL database with 8 entities, 7 enums, proper validation, and cascade relationships.

## Entities Created

### 1. **User** (`User.java`)
- Central entity representing a user account
- **Key Fields:**
  - `email` - immutable, unique identifier (Cognito-linked)
  - `cognitoUserId` - Cognito sub (unique)
  - `displayName` - user-chosen name
  - `totalCoins` - monthly resetting balance
  - `lifetimeCoinsEarned` - lifetime statistics
  - `currentGalaxyCycle` - galaxy progression counter
  - `galaxyResetDate` - last monthly reset date
- **Relationships:**
  - `@OneToMany` tasks, habits, habitCompletions, galaxyItems, coinTransactions, aiConversations
  - All with `cascade = CascadeType.ALL, orphanRemoval = true` for complete cleanup on user deletion
- **Indexes:** `galaxy_reset_date`, `last_login`

### 2. **Task** (`Task.java`)
- Individual to-do items with reward system
- **Key Changes from Discussion:**
  - `difficultyLevel` is now `DifficultyLevel` enum (not Integer) - type-safe
  - Added `@Min(0)` to `coinReward` and `coinPenalty`
  - Supports soft-delete via `deletedAt` timestamp (24-hour restore window)
- **Fields:**
  - `title`, `description`, `difficultyLevel` (EASY/MEDIUM/HARD/VERY_HARD/EXTREME)
  - `durationMinutes`, `coinReward`, `coinPenalty`
  - `status` (PENDING/COMPLETED/EXPIRED/DELETED)
  - `dueDate`, `completedAt`, `deletedAt`
  - `createdByAi`, `aiConversation` reference
- **Indexes:** `user_id`, `due_date`, `status`, `deleted_at`, `ai_conversation_id`

### 3. **Habit** (`Habit.java`)
- Recurring habits with streak tracking
- **Key Changes:**
  - `difficultyLevel` now `DifficultyLevel` enum
  - Added `@Min(0)` to `coinReward`, `coinPenalty`, `currentStreak`, `bestStreak`, `totalCompletions`
  - Soft-delete via `deletedAt` (permanent history preservation)
- **Fields:**
  - `title`, `description`, `frequency` (DAILY/WEEKLY/CUSTOM)
  - `difficultyLevel`, `coinReward`, `coinPenalty`
  - `currentStreak`, `bestStreak`, `totalCompletions`, `lastCompletedDate`
  - `isActive`, `deletedAt`
  - `completions` collection (@OneToMany with cascade)
- **Indexes:** `user_id`, `frequency`, `deleted_at`

### 4. **HabitCompletion** (`HabitCompletion.java`)
- Records of individual habit completions (diary entries)
- **Constraints:**
  - **Unique** on `(habit_id, completed_date)` - one completion per habit per day
- **Fields:**
  - `habit`, `user`, `completedDate`, `coinsEarned`, `streakAtCompletion`
- **Indexes:** `user_id`, `habit_id`, `completed_date`

### 5. **ItemCatalog** (`ItemCatalog.java`)
- Master list of purchasable galaxy items
- **Key Changes:**
  - Added `@Min(0)` to `costCoins` and `unlockRequirement`
  - `scale`, `positionX`, `positionY`, `rotation` use `BigDecimal` for precise positioning
- **Fields:**
  - `itemName` (unique), `itemType` (enum), `description`
  - `costCoins`, `rarity` (COMMON/RARE/EPIC/LEGENDARY)
  - `imageUrl`, `positionX`, `positionY`, `scale`, `rotation`
  - `unlockRequirement`, `isAvailable`
- **Indexes:** `type`, `is_available`, `unlock_requirement`

### 6. **GalaxyItem** (`GalaxyItem.java`)
- Tracks user purchases and placement in galaxy
- **Fields:**
  - `user`, `catalogItem`, `purchaseDate`, `timesSelected`, `isActive`
- **Indexes:** `user_id`, `catalog_item_id`, `purchase_date`

### 7. **CoinTransaction** (`CoinTransaction.java`)
- Complete audit log of all coin movements
- **Fields:**
  - `user`, `amount` (positive = earned, negative = spent)
  - `transactionType` (TASK_COMPLETION/HABIT_COMPLETION/ITEM_PURCHASE/STREAK_PENALTY/DEADLINE_PENALTY)
  - `referenceType` (TASK/HABIT/GALAXY_ITEM), `referenceId`
  - `description`
- **Indexes:** `user_id`, `transaction_type`, `created_at`

### 8. **AiConversation** (`AiConversation.java`)
- Chat history with AI assistant
- **Key Fix:**
  - Removed `cascade = CascadeType.ALL` from tasks collection
  - Now tasks persist independently after conversation ends (no orphan removal)
- **Fields:**
  - `user`, `conversationType` (TASK_CREATION/STUDY_PLAN/HABIT_SUGGESTION/GENERAL_CHAT)
  - `userMessage`, `aiResponse` (TEXT columns)
  - `tasksCreated`
  - `tasks` collection (read-only reference)
- **Indexes:** `user_id`, `conversation_type`, `created_at`

## Enums Created

1. **TaskStatus** - PENDING, COMPLETED, EXPIRED, DELETED
2. **HabitFrequency** - DAILY, WEEKLY, CUSTOM
3. **DifficultyLevel** - EASY(1), MEDIUM(2), HARD(3), VERY_HARD(4), EXTREME(5)
   - New! With numeric level for coin calculations
4. **ItemType** - STAR, PLANET, NEBULA, ASTEROID, COMET
5. **RarityLevel** - COMMON, RARE, EPIC, LEGENDARY
6. **TransactionType** - TASK_COMPLETION, HABIT_COMPLETION, ITEM_PURCHASE, STREAK_PENALTY, DEADLINE_PENALTY
7. **ReferenceType** - TASK, HABIT, GALAXY_ITEM
8. **ConversationType** - TASK_CREATION, STUDY_PLAN, HABIT_SUGGESTION, GENERAL_CHAT

## Configuration

### application-dev.yml
- PostgreSQL: localhost:5432
- **ddl-auto: update** - auto-creates/modifies schema
- Default credentials if env vars not set

### application-test.yml
- H2 in-memory database
- **ddl-auto: create-drop** - fresh DB per test run
- PostgreSQL-compatible mode

### application-prod.yml
- PostgreSQL via env variables (required)
- **ddl-auto: validate** - strict schema validation only
- No modifications to production schema

All profiles use **UTC timezone** for timestamps.

## Dependencies

**model/pom.xml additions:**
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- postgresql (runtime)
- h2 (test)

**repository/pom.xml additions:**
- spring-boot-starter-data-jpa
- Dependency on model module

## Build Status

✅ **All modules compile successfully**
- Java 25 compatible
- Lombok annotation processing working
- No compilation errors or warnings (except Unsafe deprecation)

## Next Steps

1. Create Spring Data JPA repositories in `repository` module
2. Implement service layer for business logic (coin calculations, streak updates, monthly reset)
3. Add REST controllers in `controller` module
4. Implement login flow with Cognito integration and monthly galaxy reset check
5. Add scheduled tasks for 24-hour task cleanup and galaxy reset automation

