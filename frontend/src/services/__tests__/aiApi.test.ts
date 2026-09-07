import { describe, it, expect, vi, beforeEach } from 'vitest'
import { aiApi } from '../aiApi'
import api from '../api'

vi.mock('../api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  setAuthToken: vi.fn(),
}))

const mockApi = api as unknown as {
  get: ReturnType<typeof vi.fn>
  post: ReturnType<typeof vi.fn>
}

describe('aiApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('sendMessage POSTs the full request body to /ai/chat', async () => {
    const req = { message: 'hi', newConversation: true, userTimezone: 'Asia/Jerusalem' }
    mockApi.post.mockResolvedValue({ data: { conversationId: 1, aiMessage: 'hello', conversationType: 'GENERAL_CHAT', tasksCreated: 0, habitsCreated: 0, createdAt: 'x' } })

    const result = await aiApi.sendMessage(req)

    expect(mockApi.post).toHaveBeenCalledWith('/ai/chat', req)
    expect(result.aiMessage).toBe('hello')
  })

  it('forwards newConversation:false unchanged', async () => {
    mockApi.post.mockResolvedValue({ data: {} })
    await aiApi.sendMessage({ message: 'x', newConversation: false })
    expect(mockApi.post).toHaveBeenCalledWith('/ai/chat', { message: 'x', newConversation: false })
  })

  it('getDailyBriefing GETs /ai/briefing', async () => {
    mockApi.get.mockResolvedValue({ data: { aiMessage: 'brief' } })
    const result = await aiApi.getDailyBriefing()
    expect(mockApi.get).toHaveBeenCalledWith('/ai/briefing')
    expect(result.aiMessage).toBe('brief')
  })

  it('getHistory GETs /ai/history and returns the array', async () => {
    const history = [{ conversationId: 1, userMessage: 'u', aiResponse: 'a', conversationType: 'GENERAL_CHAT', tasksCreated: 0, createdAt: 'x' }]
    mockApi.get.mockResolvedValue({ data: history })
    const result = await aiApi.getHistory()
    expect(mockApi.get).toHaveBeenCalledWith('/ai/history')
    expect(result).toBe(history)
  })
})
