import { describe, it, expect, beforeEach, vi } from 'vitest'
import { AI_CHAT_SINCE_KEY, readChatSince, markNewChat, turnsSince } from '../aiChat'
import type { AiConversationHistoryItem } from '@/services/aiApi'

const turn = (id: number, createdAt: string): AiConversationHistoryItem => ({
  conversationId: id,
  userMessage: `u${id}`,
  aiResponse: `a${id}`,
  conversationType: 'GENERAL_CHAT',
  tasksCreated: 0,
  createdAt,
})

// API returns newest-first
const history: AiConversationHistoryItem[] = [
  turn(4, '2026-03-01T12:10:00Z'),
  turn(3, '2026-03-01T12:00:00Z'),
  turn(2, '2026-02-01T09:00:00Z'),
  turn(1, '2026-01-01T08:00:00Z'),
]

describe('turnsSince', () => {
  it('returns the whole history (oldest-first) when there is no marker', () => {
    expect(turnsSince(history, null).map((t) => t.conversationId)).toEqual([1, 2, 3, 4])
  })

  it('keeps only turns at or after the marker', () => {
    const since = new Date('2026-03-01T11:59:00Z').getTime()
    expect(turnsSince(history, since).map((t) => t.conversationId)).toEqual([3, 4])
  })

  it('is inclusive of a turn exactly on the marker', () => {
    const since = new Date('2026-03-01T12:00:00Z').getTime()
    expect(turnsSince(history, since).map((t) => t.conversationId)).toEqual([3, 4])
  })

  it('returns nothing when the marker is after every turn', () => {
    const since = new Date('2027-01-01T00:00:00Z').getTime()
    expect(turnsSince(history, since)).toEqual([])
  })

  it('does not mutate the input array', () => {
    const copy = [...history]
    turnsSince(history, null)
    expect(history).toEqual(copy)
  })

  it('treats NaN like no marker', () => {
    expect(turnsSince(history, NaN)).toHaveLength(4)
  })
})

describe('readChatSince / markNewChat', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns null when nothing was stored', () => {
    expect(readChatSince()).toBeNull()
  })

  it('round-trips a timestamp', () => {
    markNewChat(1_700_000_000_000)
    expect(readChatSince()).toBe(1_700_000_000_000)
    expect(localStorage.getItem(AI_CHAT_SINCE_KEY)).toBe('1700000000000')
  })

  it('defaults to the current time', () => {
    const before = Date.now()
    markNewChat()
    const stored = readChatSince()!
    expect(stored).toBeGreaterThanOrEqual(before)
  })

  it('returns null for a corrupted stored value', () => {
    localStorage.setItem(AI_CHAT_SINCE_KEY, 'not-a-number')
    expect(readChatSince()).toBeNull()
  })

  it('survives localStorage throwing', () => {
    const spy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('denied')
    })
    expect(readChatSince()).toBeNull()
    spy.mockRestore()

    const setSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('denied')
    })
    expect(() => markNewChat()).not.toThrow()
    setSpy.mockRestore()
  })
})
