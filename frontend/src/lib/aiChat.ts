import type { AiConversationHistoryItem } from '@/services/aiApi'

/**
 * The AI backend persists every exchange as a standalone row and has no notion
 * of a conversation thread, so "New Chat" is remembered on the client as the
 * moment it was pressed. Everything here keeps the chat screen showing only the
 * turns that belong to the current chat after a page refresh.
 */
export const AI_CHAT_SINCE_KEY = 'starlist.aiChatSince'

/** Reads the "current chat started at" marker (epoch ms), or null when unset/unavailable. */
export function readChatSince(): number | null {
  try {
    const raw = localStorage.getItem(AI_CHAT_SINCE_KEY)
    if (!raw) return null
    const n = Number(raw)
    return Number.isFinite(n) ? n : null
  } catch {
    return null
  }
}

/** Records that a fresh chat started now, so a later refresh won't replay older turns. */
export function markNewChat(now: number = Date.now()): void {
  try {
    localStorage.setItem(AI_CHAT_SINCE_KEY, String(now))
  } catch {
    /* storage unavailable — we simply fall back to showing the full history */
  }
}

/**
 * Given the full history (newest-first, as the API returns it) and the marker,
 * returns just the current chat's turns in chronological (oldest-first) order.
 * A null marker means "never started a new chat" → return everything.
 */
export function turnsSince(
  history: AiConversationHistoryItem[],
  sinceMs: number | null,
): AiConversationHistoryItem[] {
  const chronological = [...history].reverse()
  if (sinceMs == null || Number.isNaN(sinceMs)) return chronological
  return chronological.filter((t) => new Date(t.createdAt).getTime() >= sinceMs)
}
