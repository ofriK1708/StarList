import { describe, it, expect, vi, beforeEach } from 'vitest'
import { tasksApi } from '../taskApi'
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

describe('tasksApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getTasks GETs /tasks and returns response.data', async () => {
    const rows = [{ taskId: 1, title: 'A' }]
    mockApi.get.mockResolvedValue({ data: rows })

    const result = await tasksApi.getTasks()

    expect(mockApi.get).toHaveBeenCalledWith('/tasks')
    expect(result).toBe(rows)
  })

  it('createTask POSTs the payload to /tasks', async () => {
    const payload = { title: 'New', difficultyLevel: 'MEDIUM' as const, durationMinutes: 30 }
    mockApi.post.mockResolvedValue({ data: { taskId: 9, ...payload } })

    const result = await tasksApi.createTask(payload)

    expect(mockApi.post).toHaveBeenCalledWith('/tasks', payload)
    expect(result).toEqual({ taskId: 9, ...payload })
  })

  it('updateTask PUTs a partial payload to /tasks/:id', async () => {
    const patch = { title: 'Renamed', durationMinutes: 0 }
    mockApi.put.mockResolvedValue({ data: { taskId: 3, ...patch } })

    const result = await tasksApi.updateTask(3, patch)

    expect(mockApi.put).toHaveBeenCalledWith('/tasks/3', patch)
    expect(result).toEqual({ taskId: 3, ...patch })
  })

  it('completeTask POSTs to /tasks/:id/complete', async () => {
    mockApi.post.mockResolvedValue({ data: { taskId: 5, coinsEarned: 10, newTotalCoins: 110 } })

    const result = await tasksApi.completeTask(5)

    expect(mockApi.post).toHaveBeenCalledWith('/tasks/5/complete')
    expect(result.coinsEarned).toBe(10)
  })

  it('deleteTask DELETEs /tasks/:id and resolves void', async () => {
    mockApi.delete.mockResolvedValue({ data: undefined })

    await expect(tasksApi.deleteTask(7)).resolves.toBeUndefined()
    expect(mockApi.delete).toHaveBeenCalledWith('/tasks/7')
  })
})
