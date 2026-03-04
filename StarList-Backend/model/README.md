# StarList Model Module

This module contains JPA entities and enums for the StarList PostgreSQL schema.

## Implemented tables

- `users`
- `tasks`
- `habits`
- `habit_completions`
- `item_catalog`
- `galaxy_items`
- `coin_transactions`
- `ai_conversations`

## Notes

- Authentication password storage is not modeled because Cognito is planned.
- `users.email` is immutable with `@Column(updatable = false)`.
- Task and habit soft-delete support exists via nullable `deletedAt`.
- `habit_completions` enforces one completion per habit per day using a unique constraint.
- Enum columns are stored as strings (`EnumType.STRING`).

