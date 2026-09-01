import { describe, it, expect, vi, beforeEach } from 'vitest'
import { habitsApi } from '../habitsApi'
import api from '../api'

vi.mock('../api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  setAuthToken: vi.fn(),
}))

const mockApi = api as unknown as {
  get: ReturnType<typeof vi.fn>
  post: ReturnType<typeof vi.fn>
  put: ReturnType<typeof vi.fn>
  delete: ReturnType<typeof vi.fn>
}

describe('habitsApi', () => {
  beforeEach(() => vi.clearAllMocks())

  describe('getHabits', () => {
    it('GETs /habits with no params when year/month omitted', async () => {
      mockApi.get.mockResolvedValue({ data: [] })
      await habitsApi.getHabits()
      expect(mockApi.get).toHaveBeenCalledWith('/habits', { params: {} })
    })

    it('passes year and month as query params when both given', async () => {
      mockApi.get.mockResolvedValue({ data: [] })
      await habitsApi.getHabits(2026, 9)
      expect(mockApi.get).toHaveBeenCalledWith('/habits', { params: { year: 2026, month: 9 } })
    })

    it('normalizes id -> habitId when backend returns "id"', async () => {
      mockApi.get.mockResolvedValue({ data: [{ id: 42, title: 'Run' }] })
      const [h] = await habitsApi.getHabits()
      expect(h.habitId).toBe(42)
    })

    it('keeps habitId when backend already returns habitId', async () => {
      mockApi.get.mockResolvedValue({ data: [{ habitId: 7, id: 999, title: 'Read' }] })
      const [h] = await habitsApi.getHabits()
      expect(h.habitId).toBe(7)
    })
  })

  it('createHabit POSTs to /habits and normalizes the id', async () => {
    const payload = { title: 'Meditate', frequency: 'DAILY' as const, difficultyLevel: 'EASY' as const }
    mockApi.post.mockResolvedValue({ data: { id: 11, ...payload } })

    const result = await habitsApi.createHabit(payload)

    expect(mockApi.post).toHaveBeenCalledWith('/habits', payload)
    expect(result.habitId).toBe(11)
  })

  it('updateHabit PUTs to /habits/:id and normalizes the id', async () => {
    mockApi.put.mockResolvedValue({ data: { id: 4, title: 'Renamed' } })

    const result = await habitsApi.updateHabit(4, { title: 'Renamed' })

    expect(mockApi.put).toHaveBeenCalledWith('/habits/4', { title: 'Renamed' })
    expect(result.habitId).toBe(4)
  })

  it('completeHabit POSTs to /habits/:id/complete', async () => {
    mockApi.post.mockResolvedValue({ data: { habitId: 2, coinsEarned: 5, newTotalCoins: 55, currentStreak: 3, bestStreak: 9 } })

    const result = await habitsApi.completeHabit(2)

    expect(mockApi.post).toHaveBeenCalledWith('/habits/2/complete')
    expect(result.currentStreak).toBe(3)
  })

  it('deleteHabit DELETEs /habits/:id', async () => {
    mockApi.delete.mockResolvedValue({ data: undefined })
    await habitsApi.deleteHabit(6)
    expect(mockApi.delete).toHaveBeenCalledWith('/habits/6')
  })
})
