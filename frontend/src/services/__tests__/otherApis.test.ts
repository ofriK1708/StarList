import { describe, it, expect, vi, beforeEach } from 'vitest'
import { storeApi } from '../storeApi'
import { usersApi } from '../usersApi'
import { achievementsApi } from '../achievementsApi'
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

describe('storeApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getAvailableItems GETs /store/items', async () => {
    mockApi.get.mockResolvedValue({ data: [] })
    await storeApi.getAvailableItems()
    expect(mockApi.get).toHaveBeenCalledWith('/store/items')
  })

  it('getMyItems GETs /store/my-items', async () => {
    mockApi.get.mockResolvedValue({ data: [] })
    await storeApi.getMyItems()
    expect(mockApi.get).toHaveBeenCalledWith('/store/my-items')
  })

  it('buyItem POSTs to /store/buy/:id', async () => {
    mockApi.post.mockResolvedValue({ data: { newTotalCoins: 5 } })
    const res = await storeApi.buyItem(3)
    expect(mockApi.post).toHaveBeenCalledWith('/store/buy/3')
    expect(res.newTotalCoins).toBe(5)
  })
})

describe('usersApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('createUser POSTs to /users', async () => {
    const body = { email: 'a@b.c', cognitoUserId: 'sub-1', displayName: 'A' }
    mockApi.post.mockResolvedValue({ data: { id: 1, ...body } })
    await usersApi.createUser(body)
    expect(mockApi.post).toHaveBeenCalledWith('/users', body)
  })

  it('updateUser PUTs to /users/me', async () => {
    mockApi.put.mockResolvedValue({ data: { id: 1, displayName: 'B' } })
    await usersApi.updateUser({ displayName: 'B' })
    expect(mockApi.put).toHaveBeenCalledWith('/users/me', { displayName: 'B' })
  })

  it('deleteUser DELETEs /users/me', async () => {
    mockApi.delete.mockResolvedValue({ data: undefined })
    await usersApi.deleteUser()
    expect(mockApi.delete).toHaveBeenCalledWith('/users/me')
  })

  it('getUserByCognitoSub GETs /users/cognito/:sub', async () => {
    mockApi.get.mockResolvedValue({ data: { id: 1 } })
    await usersApi.getUserByCognitoSub('abc-123')
    expect(mockApi.get).toHaveBeenCalledWith('/users/cognito/abc-123')
  })
})

describe('achievementsApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getMyAchievements GETs /achievements', async () => {
    mockApi.get.mockResolvedValue({ data: [] })
    await achievementsApi.getMyAchievements()
    expect(mockApi.get).toHaveBeenCalledWith('/achievements')
  })
})
